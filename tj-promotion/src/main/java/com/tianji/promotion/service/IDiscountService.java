package com.tianji.promotion.service;

import com.tianji.api.dto.promotion.CouponDiscountDTO;
import com.tianji.api.dto.promotion.OrderCourseDTO;

import java.util.List;

/**
 * @author hazard
 * @version 1.0
 * @description 折扣服务的
 * @date 2025/8/11 20:28
 */
public interface IDiscountService {
    List<CouponDiscountDTO> findDiscountSolution(List<OrderCourseDTO> order);

}
