package com.tianji.remark.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.RabbitResourceHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tianji.common.constants.MqConstants.Exchange.LIKE_RECORD_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE;

/**
 * <p>
 * 点赞记录表 服务实现类
 * </p>
 *
 * @author hazard
 * @since 2025-07-18
 */
@Slf4j
//@Service
@RequiredArgsConstructor
public class LikedRecordServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {

     private final RabbitMqHelper rabbitMqHelper;
    @Override
    public void addLikedRecord(LikeRecordFormDTO formDTO) {
        //1.基于前端的数据，判断是点赞还是取消点赞
        boolean success=formDTO.getLiked() ? like(formDTO) : unlike(formDTO);
        //2.判断是否点赞成功
        if (!success) {
            return;
        }
        //3.统计点赞数量
        Integer likedTimes = lambdaQuery().eq(LikedRecord::getBizId, formDTO.getBizId()).count();
        //4.发送mq消息
        rabbitMqHelper.send(
                LIKE_RECORD_EXCHANGE,
                StringUtils.format(LIKED_TIMES_KEY_TEMPLATE,formDTO.getBizType()),
                //调用api下的dto的LikedTimesDTO全参构造方法
                LikedTimesDTO.of(formDTO.getBizId(), likedTimes)
                );
    }

    //取消点赞
    private boolean unlike(LikeRecordFormDTO formDTO) {
        return remove(new QueryWrapper<LikedRecord>().lambda()
                .eq(LikedRecord::getUserId, UserContext.getUser())
                .eq(LikedRecord::getBizId, formDTO.getBizId()));
    }

    //点赞
    private boolean like(LikeRecordFormDTO formDTO) {
        //1.查询点赞记录
        Integer count = lambdaQuery()
                .eq(LikedRecord::getUserId, UserContext.getUser())
                .eq(LikedRecord::getBizId, formDTO.getBizId())
                .eq(LikedRecord::getBizType, formDTO.getBizType())
                .count();

        //2.判断是否已经点赞
        if (count>0) {
            return false;
        }
        //3.保存点赞记录
        LikedRecord record = BeanUtils.copyProperties(formDTO, LikedRecord.class);
        record.setUserId(UserContext.getUser());
        save(record);
        return true;
    }

    @Override
    public Set<Long> isBizLiked(List<Long> bizIds) {
        //1.获取登录用户id
        Long userId = UserContext.getUser();
        //2.查询点赞状态
        List<LikedRecord> list = lambdaQuery()
                .in(LikedRecord::getBizId, bizIds)
                .eq(LikedRecord::getUserId, userId)
                .list();
        //3.返回结果
        return list.stream().map(LikedRecord::getBizId).collect(Collectors.toSet());
    }

    @Override
    public void readLikedTimesAndSendMessage(String bizType, int maxBizSize) {
    }
}

