package com.tianji.promotion.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.promotion.domain.query.CodeQuery;

import java.util.List;

/**
 * <p>
 * 兑换码 服务类
 * </p>
 *
 * @author hazard
 * @since 2025-07-28
 */
public interface IExchangeCodeService extends IService<ExchangeCode> {

    void asyncGenerateCode(Coupon coupon);

    PageDTO<ExchangeCode> pageQueryCodes(CodeQuery query);

    boolean updateExchangeMark(long serialNum, boolean b);
}
