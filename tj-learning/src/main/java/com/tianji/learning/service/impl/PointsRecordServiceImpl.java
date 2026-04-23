package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mapper.PointsRecordMapper;
import com.tianji.learning.service.IPointsRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务实现类
 * 手写sql语句+mp组合查询
 * </p>
 *
 * @author hazard
 * @since 2025-07-23
 * @description 积分记录服务类
 */
@Service
@RequiredArgsConstructor
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements IPointsRecordService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 新增积分记录
     * @param userId
     * @param points
     * @param type
     */
    @Override
    public void addPointsRecord(Long userId, int points, PointsRecordType type) {
        LocalDateTime now = LocalDateTime.now();
        //1.判断是否有上限,realPoints为最终在数据库里面的积分
        int realPoints = points;
        int maxPoints = type.getMaxPoints();
        if (maxPoints>0) {
            //2.有上限

            LocalDateTime begin = DateUtils.getDayStartTime(now);
            LocalDateTime end = DateUtils.getDayEndTime(now);
            //2.1查询今日积分，Mybatis-Plus没有sum求和的方法，只能手写sql
            int currentPoints = queryUserPointsByTypeAndDate(userId, type, begin, end);
            //2.2判断当日积分是否超过上限
            if (currentPoints>=maxPoints) {
                //2.3超过上限
                return;
            }
            //2.4没超过,判断已得到积分加上新增积分是否超过上限
            if (currentPoints+points>maxPoints) {
                realPoints=maxPoints-currentPoints;
            }
        }
        //3.没有上限，直接保存积分记录
        PointsRecord pointsRecord=new PointsRecord();
        pointsRecord.setUserId(userId);
        pointsRecord.setType(type);
        pointsRecord.setPoints(realPoints);
        save(pointsRecord);

        //4.累加积分到redis的zset
        String key= RedisConstants.POINTS_RECORD_PREFIX+now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        redisTemplate.opsForZSet().incrementScore(key,userId.toString(),realPoints);


    }

    private int queryUserPointsByTypeAndDate(
            Long userId, PointsRecordType type, LocalDateTime begin, LocalDateTime end) {
        //利用mp组合查询
        QueryWrapper<PointsRecord> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .eq(PointsRecord::getUserId, userId)
                .eq(type!=null, PointsRecord::getType, type)
                .between(begin!=null&&end!=null, PointsRecord::getCreateTime,begin,end);

        //调用mapper，查询结果,如果where条件不成立查不到任何数据,null
        Integer points=getBaseMapper().queryUserPointsByTypeAndDate(wrapper);
        return points==null?0:points;
    }

    /**
     * 查询当日积分详细
     * @return List<PointsStatisticsVO>
     */
    @Override
    public List<PointsStatisticsVO> queryMyPointsToday() {
        // 1.获取用户
        Long userId = UserContext.getUser();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime begin = DateUtils.getDayStartTime(now);
        LocalDateTime end = DateUtils.getDayEndTime(now);
        // 3.构建查询条件
        QueryWrapper<PointsRecord> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .eq(PointsRecord::getUserId, userId)
                .between(PointsRecord::getCreateTime, begin, end);
        // 4.查询
        List<PointsRecord> list = getBaseMapper().queryUserPointsByDate(wrapper);
        if (CollUtils.isEmpty(list)) {
            return CollUtils.emptyList();
        }
        // 5.封装返回
        List<PointsStatisticsVO> vos = new ArrayList<>(list.size());
        for (PointsRecord p : list) {
            PointsStatisticsVO vo = new PointsStatisticsVO();
            vo.setType(p.getType().getDesc());
            vo.setMaxPoints(p.getType().getMaxPoints());
            vo.setPoints(p.getPoints());
            vos.add(vo);
        }
        return vos;
    }


}
