package com.tianji.remark.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.constants.RedisConstants;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;

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
@Service
@RequiredArgsConstructor
public class LikedRecordServiceRedisImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {

    private final StringRedisTemplate redisTemplate;
    private final RabbitMqHelper rabbitMqHelper;
    @Override
    public void addLikedRecord(LikeRecordFormDTO formDTO) {
        //1.基于前端的数据，判断是点赞还是取消点赞
        boolean success=formDTO.getLiked() ? like(formDTO) : unlike(formDTO);
        //2.判断是否点赞成功
        if (!success) {
            return;
        }
        //3.统计点赞数量,
        Long likedTimes = redisTemplate.opsForSet()
                .size(RedisConstants.LIKES_BIZ_KEY_PREFIX + formDTO.getBizId());
        if (likedTimes==null) {
            //点赞数量为空直接返回
            return;
        }
        //4.缓存点赞数
        redisTemplate.opsForZSet().add(
                RedisConstants.LIKES_TIMES_KEY_PREFIX + formDTO.getBizType(),
                formDTO.getBizId().toString(),
                likedTimes);
    }

    //取消点赞
    private boolean unlike(LikeRecordFormDTO formDTO) {
        //1.获取登录用户id
        Long userId = UserContext.getUser();
        //2.获取key
        String key = RedisConstants.LIKES_BIZ_KEY_PREFIX + formDTO.getBizId();
        //3.执行srem命令
        Long result = redisTemplate.opsForSet().remove(key, userId.toString());
        return result != null && result >0;
    }

    //点赞
    private boolean like(LikeRecordFormDTO formDTO) {
        //1.获取登录用户id
        Long userId = UserContext.getUser();
        //2.获取key
        String key = RedisConstants.LIKES_BIZ_KEY_PREFIX + formDTO.getBizId();
        //3.执行sadd命令
        Long result = redisTemplate.opsForSet().add(key, userId.toString());
        return result != null && result >0;
    }

    /**
     * 批量查询点赞状态
     * @param bizIds 业务id列表
     * @return 业务id对应的点赞状态
     */
    @Override
    public Set<Long> isBizLiked(List<Long> bizIds) {
        //1.获取登录用户id
        Long userId = UserContext.getUser();
        //2.查询点赞状态
        //逐一执行，T=redis执行命令时间和网络延迟时间总和
/*        for (Long bizId : bizIds) {
            String key = RedisConstants.LIKES_BIZ_KEY_PREFIX + bizId;
            redisTemplate.opsForSet().isMember(key,userId.toString());
        }*/

        /*
        采用管道方式批处理redis命令,匿名内部类，返回值类型为Object，与原数据一一对应
        Redis中提供了一个功能，可以在一次请求中执行多个命令，实现批处理效果。这个功能就是Pipelined
         */
        List<Object> objects = redisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                StringRedisConnection src = (StringRedisConnection) connection;
                for (Long bizId : bizIds) {
                    String key = RedisConstants.LIKES_BIZ_KEY_PREFIX + bizId;
                    src.sIsMember(key, userId.toString());
                }
                return null;
            }
        });
        //3.返回结果
        Set<Long> set = new HashSet<>();
        for (int i = 0; i < objects.size(); i++) {
            Boolean b = (Boolean) objects.get(i);
            if (b) {
                set.add(bizIds.get(i));
            }
        }

        //stream收集数据结果
/*        return IntStream.range(0, objects.size()) // 创建从0到集合size的流
                .filter(i -> (boolean) objects.get(i)) // 遍历每个元素，保留结果为true的角标i
                .mapToObj(bizIds::get)// 用角标i取bizIds中的对应数据，就是点赞过的id
                .collect(Collectors.toSet());// 收集*/
        return set;
    }

    //读取redis中的点赞信息
    @Override
    public void readLikedTimesAndSendMessage(String bizType, int maxBizSize) {
        // 1.读取并移除Redis中缓存的点赞总数
        String key = RedisConstants.LIKES_TIMES_KEY_PREFIX + bizType;
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().popMin(key, maxBizSize);
        if (CollUtils.isEmpty(tuples)) {
            return;
        }
        // 2.数据转换
        List<LikedTimesDTO> list = new ArrayList<>(tuples.size());
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String bizId = tuple.getValue();
            Double likedTimes = tuple.getScore();
            if (bizId == null || likedTimes == null) {
                continue;
            }
            list.add(LikedTimesDTO.of(Long.valueOf(bizId), likedTimes.intValue()));
        }
        // 3.发送MQ消息
        rabbitMqHelper.send(
                LIKE_RECORD_EXCHANGE,
                StringUtils.format(LIKED_TIMES_KEY_TEMPLATE, bizType),
                list);
    }
}

