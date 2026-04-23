package com.tianji.promotion.controller;


import com.tianji.api.dto.promotion.CouponDiscountDTO;
import com.tianji.api.dto.promotion.OrderCouponDTO;
import com.tianji.api.dto.promotion.OrderCourseDTO;
import com.tianji.promotion.service.IDiscountService;
import com.tianji.promotion.service.IUserCouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.A;
import org.springframework.web.bind.annotation.*;

import javax.naming.directory.InitialDirContext;
import java.util.List;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 前端控制器
 * </p>
 *
 * @author hazard
 * @since 2025-07-31
 */
@RestController
@RequestMapping("/user-coupons")
@RequiredArgsConstructor
@Api(tags = "用户优惠券接口")
public class UserCouponController {
    private final IUserCouponService userCouponService;
    private final IDiscountService discountService;

    /**
     * 用户领取优惠券
     * @param couponId 优惠券id
     */
    @PostMapping("/{couponId}/receive")
    @ApiOperation("用户领取优惠券")
    public void userReceiveCoupon(@PathVariable("couponId") Long couponId) {
        userCouponService.userReceiveCoupon(couponId);
    }

    /**
     * 兑换码兑换优惠券
     * @param code 兑换码
     */
    @PostMapping("/{code}/exchange")
    @ApiOperation("兑换码兑换优惠券")
    public void userExchangeCoupon(@PathVariable("code") String code) {
        userCouponService.userExchangeCoupon(code);
    }

    @ApiOperation("查询用户优惠券使用方案")
    @PostMapping("/available")
    public List<CouponDiscountDTO> findDiscountSolution(@RequestBody List<OrderCourseDTO>  order) {
        return discountService.findDiscountSolution(order);
    }
}
