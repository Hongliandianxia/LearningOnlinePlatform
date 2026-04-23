package com.tianji.learning.service.impl;

import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.mapper.PointsBoardSeasonMapper;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author hazard
 * @since 2025-07-23
 * @description 历史积分榜单赛季服务实现类
 */
@Service
public class PointsBoardSeasonServiceImpl extends ServiceImpl<PointsBoardSeasonMapper, PointsBoardSeason> implements IPointsBoardSeasonService {

    /**
     * 根据时间查询赛季
     * @param time
     * @return
     */
    @Override
    public Integer querySeasonByTime(LocalDateTime time) {
        //1.查询赛季，赛季开始时间<=time<=赛季结束时间
        Optional<PointsBoardSeason> optional = lambdaQuery()
                .le(PointsBoardSeason::getBeginTime, time)
                .ge(PointsBoardSeason::getEndTime, time)
                .oneOpt();
        //optional，java8新特性，该类提供了一种用于表示可选值而非空引用的类级别解决方案
        return optional.map(PointsBoardSeason::getId).orElse(null);
    }


}
