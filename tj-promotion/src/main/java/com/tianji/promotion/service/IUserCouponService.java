package com.tianji.promotion.service;

import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.UserCoupon;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务类
 * </p>
 *
 * @author hazard
 * @since 2025-07-31
 */
public interface IUserCouponService extends IService<UserCoupon> {

    void userReceiveCoupon(Long couponId);

    void userExchangeCoupon(String code);

    void checkAndCreateUserCoupon(Coupon coupon, Long userId, Long serialNum);
}
