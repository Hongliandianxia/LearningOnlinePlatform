package com.tianji.promotion.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponVO;

import java.util.List;

/**
 * <p>
 * 优惠券的管理 服务类
 * </p>
 *
 * @author hazard
 * @since 2025-07-28
 */
public interface ICouponService extends IService<Coupon> {

    void addCoupon(CouponFormDTO couponFormDto);

    PageDTO<CouponPageVO> pageQueryCoupons(CouponQuery query);

    void beginIssue(CouponIssueFormDTO dto);

    CouponDetailVO queryCouponById(Long id);

    void updateCoupon(CouponFormDTO dto);

    void deleteCouponById(Long id);

    void pauseCouponById(Long id);

    List<CouponVO> queryIssuingCoupons();
}
