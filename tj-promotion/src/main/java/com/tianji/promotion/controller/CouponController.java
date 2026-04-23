package com.tianji.promotion.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.query.CodeQuery;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.service.ICouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.A;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;
import java.util.List;

/**
 * <p>
 * 优惠券管理 前端控制器
 * </p>
 *
 * @author hazard
 * @since 2025-07-28
 */
@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
@Api(tags = "优惠券管理接口")
public class CouponController {
    private final ICouponService couponService;

    @PostMapping
    @ApiOperation("添加优惠券")
    public void addCoupon(@RequestBody @Valid CouponFormDTO  couponFormDto) {
        couponService.addCoupon(couponFormDto);
    }

    @GetMapping("/page")
    @ApiOperation("分页查询优惠券")
    public PageDTO<CouponPageVO> pageQueryCoupons(CouponQuery query) {
        return couponService.pageQueryCoupons(query);
    }

    @ApiOperation("发放优惠券接口")
    @PutMapping("/{id}/issue")
    public void beginIssue(@RequestBody @Valid CouponIssueFormDTO dto) {
        couponService.beginIssue(dto);
    }

    @GetMapping("/{id}")
    @ApiOperation("根据id查询优惠券")
    public CouponDetailVO getCouponById(@PathVariable("id") Long id) {
        return couponService.queryCouponById(id);
    }

    @PutMapping("/{id}")
    @ApiOperation("编辑优惠券信息")
    public void updateCouponById(@RequestBody @Valid CouponFormDTO dto) {
        couponService.updateCoupon(dto);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除优惠券")
    public void deleteCouponById(@PathVariable("id") Long id) {
        couponService.deleteCouponById(id);
    }

    @PutMapping("/{id}/pause")
    @ApiOperation("暂停优惠券发放")
    public void pauseCouponById(@PathVariable("id") Long id) {
        couponService.pauseCouponById(id);
    }

    @GetMapping("/list")
    @ApiOperation("查询发放中的优惠券列表")
    public List<CouponVO> queryIssuingCoupons() {
        return couponService.queryIssuingCoupons();
    }
}
