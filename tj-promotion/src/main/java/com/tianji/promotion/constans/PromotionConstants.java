package com.tianji.promotion.constans;

/**
 * @author hazard
 * @version 1.0
 * @description
 * @date 2025/7/29 21:38
 */
public interface PromotionConstants {
    //共享同一个key,序列化id为redis自增id
    String COUPON_CODE_SERIAL_KEY = "coupon:code:serial";

    //利用bitmap判断优惠券是否被领取
    String COUPON_CODE_MAP_KEY = "coupon:code:map";
}
