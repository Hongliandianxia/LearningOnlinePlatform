package com.tianji.promotion.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.query.CodeQuery;
import com.tianji.promotion.service.IExchangeCodeService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 兑换码 前端控制器
 * </p>
 *
 * @author hazard
 * @since 2025-07-28
 */
@Slf4j
@RestController
@RequestMapping("/codes")
@RequiredArgsConstructor
public class ExchangeCodeController {
    private final IExchangeCodeService exchangeCodeService;

    @GetMapping("/page")
    @ApiOperation("分页查询优惠券码")
    public PageDTO<ExchangeCode> pageQueryCouponCodes(CodeQuery query) {
        return exchangeCodeService.pageQueryCodes(query);

    }
}
