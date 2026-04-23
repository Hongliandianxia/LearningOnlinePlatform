package com.tianji.learning.service.impl;

import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.*;
import com.tianji.learning.constants.RedisConstants;


import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.mq.message.SignMessage;
import com.tianji.learning.service.ISignRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.connection.BitFieldSubCommands;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;

import java.util.List;

/**
 * @author hazard
 * @version 1.0
 * @description 签到记录
 * @date 2025/7/23 23:22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignRecordServiceImpl implements ISignRecordService {

    private final RabbitMqHelper mqHelper;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public SignResultVO sign() {
        //1.签到
        //key
        Long userId = UserContext.getUser();
        LocalDate now = LocalDate.now();
        String key = RedisConstants.SIGN_RECORD_KEY_PREFIX
                        + userId
                        + now.format(DateUtils.SIGN_DATE_SUFFIX_FORMATTER);

        int offset = now.getDayOfMonth() - 1;
        Boolean signed = stringRedisTemplate.opsForValue().setBit(key, offset, true);
        if (BooleanUtils.isTrue(signed)) {
            throw new BizIllegalException("不允许重复签到! ");
        }
        //2.计算是否连续签到
        int countDays=countSignDays(key,now.getDayOfMonth());
        //3.计算积分,签到7天以上有积分奖励
        int rewardPoints = 0;
        switch (countDays){
            case 7:
                 rewardPoints= 10;
                break;
            case 14:
                rewardPoints= 20;
                break;
            case 28:
                rewardPoints= 40;
                break;
        }
        //4.保存积分明细, 发送mq消息
        mqHelper.send(
                MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.SIGN_IN,
                SignMessage.of(userId, rewardPoints+1)
        );
        //5.返回结果
        SignResultVO vo = new SignResultVO();
        //连续签到天数
        vo.setSignDays(countDays);
        //连续签到奖励积分
        vo.setRewardPoints(0);
        log.info("签到成功：{}", vo);
        return vo;
    }

    /**
     * 获取当前用户在当前月份的总共签到次数
     * @param key redis key
     * @param dayOfMonth 当月天数
     * @return 签到次数
     */
    private int countSignDays(String key, int dayOfMonth) {
        //1.获取当前用户在当前月份的签到记录
        List<Long> bitField = stringRedisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create().get(
                        //encoding unsigned无符号，返回位数,偏移量为0
                        BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));
        if (CollUtils.isEmpty(bitField)) {
            return 0;
        }
        int num=bitField.get(0).intValue();
        //2.定义计数器
        int count = 0;
        //3.循环，与1做与运算，得到最后一个位数，如果是1，则计数器加1，0则结束循环
        while ((num & 1)==1){
            //4.计数器加1
            count++;
            //5.右移一位 num >>>= 1
            num = num >>>1;
        }
        return count;
    }

    /**
     *查询用户的每日签到状况
     * @return list
     */
    @Override

     public List<Byte> getSignRecord() {
        Long userId = UserContext.getUser();
        LocalDate now = LocalDate.now();
        String key = RedisConstants.SIGN_RECORD_KEY_PREFIX
                + userId
                + now.format(DateUtils.SIGN_DATE_SUFFIX_FORMATTER);

        int dayOfMonth = now.getDayOfMonth();
        //1.获取当前用户在当前月份的签到记录
        List<Long> bitField = stringRedisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create().get(
                        //encoding unsigned无符号，返回所有位数,偏移量为0
                        BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));

        //无签到，返回空集合

        if (CollUtils.isEmpty(bitField)) {
            return null;
        }
//        SignRecordVO vo = new SignRecordVO();
        List<Byte> list = new ArrayList<>();
        //转为10进制方便循环遍历每一天的签到情况
        int num = bitField.get(0).intValue();
        while (num > 0) {
            // 从高位到低位插入
            list.add((byte)(num & 1));
            num >>>= 1;
        }
        // 补全前面未签到的天数（如：1号~当前天数）
        for (int i = list.size(); i < dayOfMonth; i++) {
            list.add(0, (byte) 0);
        }

//        vo.setSignRecords(list);
        log.info("签到记录：{}", list);
        return list;

    }


}
