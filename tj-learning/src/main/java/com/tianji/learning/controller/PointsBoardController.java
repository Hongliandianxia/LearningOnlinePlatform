package com.tianji.learning.controller;


import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.service.IPointsBoardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 学霸天梯榜 前端控制器
 * </p>
 *
 * @author hazard
 * @since 2025-07-23
 */
@RestController
@RequestMapping("/boards")
@Api(tags = "积分排行榜接口")
@RequiredArgsConstructor
public class PointsBoardController {

    private final IPointsBoardService pointsBoardService;

    /**
     * 查询积分排行榜
     * @param query
     * @return
     */
    @GetMapping
    @ApiOperation("查询积分排行榜")
    public PointsBoardVO queryPointsBoard(PointsBoardQuery query) {
        return pointsBoardService.queryPointsBoard(query);
    }


}
