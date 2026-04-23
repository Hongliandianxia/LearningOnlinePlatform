package com.tianji.promotion.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.domain.R;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.CollUtils;
import com.tianji.promotion.constans.PromotionConstants;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.query.CodeQuery;
import com.tianji.promotion.enums.ExchangeCodeStatus;
import com.tianji.promotion.mapper.ExchangeCodeMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.ultis.CodeUtil;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.tianji.promotion.constans.PromotionConstants.COUPON_CODE_MAP_KEY;
import static com.tianji.promotion.constans.PromotionConstants.COUPON_CODE_SERIAL_KEY;

/**
 * <p>
 * 兑换码 服务实现类
 * </p>
 *
 * @author hazard
 * @since 2025-07-28
 */
@Service
public class ExchangeCodeServiceImpl extends ServiceImpl<ExchangeCodeMapper, ExchangeCode> implements IExchangeCodeService {

    private final StringRedisTemplate redisTemplate;
    //使用构造函数直接注入StringRedisTemplate,并且进行key的绑定，因为都是使用同一个key
    private final BoundValueOperations<String, String> ops;
    public ExchangeCodeServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.ops = redisTemplate.boundValueOps(COUPON_CODE_SERIAL_KEY);
    }

    /**
     * 异步生成兑换码
     * @param coupon 优惠券
     */
    @Override
    @Transactional
    @Async("threadPoolOfGenerateExchangeCodeExecutor")
    public void asyncGenerateCode(Coupon coupon) {
        //获取发放数量
        Integer totalNum = coupon.getTotalNum();
        //1.获取redis自增序列号的最大值，减少redis的访问
        Long result = ops.increment(totalNum);
        if (result == null) {
            return;
        }
        int maxSerialNum = result.intValue();
        List<ExchangeCode> list=new ArrayList<>(totalNum);
        for (int serialNum=maxSerialNum-totalNum+1; serialNum<=maxSerialNum ;serialNum++) {
            //2.生成兑换码
            String code = CodeUtil.generateCode(serialNum, coupon.getId());
            ExchangeCode exchangeCode = new ExchangeCode();
            exchangeCode
                    .setCode(code)
                    .setId(serialNum)
                    .setExchangeTargetId(coupon.getId())
                    .setExpiredTime(coupon.getIssueEndTime());
            list.add(exchangeCode);
        }
        //3.保存兑换码到数据库
        saveBatch(list);
    }


    /**
     * 分页查询兑换码
     * @param query 查询参数
     * @return 分页结果
     */
    @Override
    public PageDTO<ExchangeCode> pageQueryCodes(CodeQuery query) {
        //1.获取查询参数
        Long couponId = query.getCouponId();
        Integer status = query.getStatus();
        //校验优惠券id
        if (couponId==null) {
            throw new BadRequestException("优惠券Id不能为空");
        }
        //校验兑换码状态
        if (status==3){
            throw new BadRequestException("兑换活动已过期");
        }
        //2.分页查询
        Page<ExchangeCode> page = lambdaQuery()
                .eq(ExchangeCode::getStatus, status)
                .eq(ExchangeCode::getExchangeTargetId, couponId)
                .page(query.toMpPage());

        List<ExchangeCode> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        //3.返回
        return PageDTO.of(page,records);
    }

    /**
     * 更新兑换码状态，使用redis的bitmap实现
     * @param serialNum 兑换码序列号
     * @param mark 是否更新
     * @return 是否更新成功
     */
    @Override
        public boolean updateExchangeMark(long serialNum, boolean mark) {
            Boolean boo = redisTemplate.opsForValue().setBit(COUPON_CODE_MAP_KEY, serialNum, mark);
            return boo != null && boo;
    }
}
