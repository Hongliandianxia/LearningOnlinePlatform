package com.tianji.learning.service;

import com.tianji.learning.domain.po.PointsBoard;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 学霸天梯榜 服务类
 * </p>
 *
 * @author hazard
 * @since 2025-07-23
 */
public interface IPointsBoardService extends IService<PointsBoard> {

    PointsBoardVO queryPointsBoard(PointsBoardQuery query);

    //查询当前赛季的积分排行榜
    List<PointsBoard> queryCurrentSeasonList(String key, Integer pageNo, Integer pageSize);

    void createPointsBoardTableBySeason(Integer season);
}
