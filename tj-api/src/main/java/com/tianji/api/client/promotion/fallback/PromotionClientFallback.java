package com.tianji.api.client.promotion.fallback;

import com.tianji.api.client.promotion.PromotionClient;
import com.tianji.api.dto.promotion.CouponDiscountDTO;
import com.tianji.api.dto.promotion.OrderCourseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * @author hazard
 * @version 1.0
 * @description
 * @date 2025/10/13 21:06
 */
@Slf4j
public class PromotionClientFallback implements FallbackFactory<PromotionClient> {
    @Override
    public PromotionClient create(Throwable cause) {
        log.error("查询优惠券促销服务异常", cause);
        return order -> Collections.emptyList();
    }
}
