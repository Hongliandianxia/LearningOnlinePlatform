package com.tianji.promotion.service.impl;


import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.enums.ExchangeCodeStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.mapper.UserCouponMapper;

import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.ultis.CodeUtil;
import com.tianji.promotion.ultis.MyLock;
import lombok.RequiredArgsConstructor;


import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务实现类
 * </p>
 *
 * @author hazard
 * @since 2025-07-31
 */
@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {
    private final CouponMapper couponMapper;
    private final IExchangeCodeService exchangeCodeService;
/*
    private final RedissonClient redissonClient;
*/


    /**
     * 用户手动领取优惠券
     * @param couponId 优惠券id
     */
    @Override
    public void userReceiveCoupon(Long couponId) {
        Long userId = UserContext.getUser();
        //1.查询优惠券
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在！");
        }
        //2.校验时间和库存
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getIssueBeginTime())) {
            throw new BadRequestException("优惠券未开始发放！");
        }
        if (now.isAfter(coupon.getIssueEndTime())) {
            throw new BadRequestException("优惠券发放已结束");
        }
        //3.校验库存
        if (coupon.getTotalNum() <= coupon.getIssueNum()) {
            throw new BadRequestException("优惠券已被领完！");
        }
        //4.校验领取限制 保证锁的是同一个用户
/*        //Redisson分布式锁
        String key="lock:coupon:uid"+userId;
        RLock lock = redissonClient.getLock(key);
        boolean isLock = lock.tryLock();
        if (!isLock) {
            //获取锁失败
            throw new BadRequestException("请勿重复操作！");
        }
        try {
            //获取锁成功，获取代理对象，写入数据库

        } finally {
            //释放锁
            lock.unlock();
        }
        */
        //使用代理接口调用 校验优惠券业务
        IUserCouponService proxy = (IUserCouponService)AopContext.currentProxy();
        proxy.checkAndCreateUserCoupon(coupon, userId,null);

    }


    /**
     * 用户使用兑换码兑换优惠券
     * @param code 兑换码
     */
    @Override
    public void userExchangeCoupon(String code) {
        // 1.校验并解析兑换码
        long serialNum = CodeUtil.parseCode(code);
        // 2.校验是否已经兑换, 这里直接执行setBit，通过返回值来判断是否兑换过
        boolean exchanged = exchangeCodeService.updateExchangeMark(serialNum, true);
        if (exchanged) {
            throw new BizIllegalException("兑换码已经被兑换过了！");
        }
        try {
            // 3.查询兑换码对应的优惠券id
            ExchangeCode exchangeCode = exchangeCodeService.getById(serialNum);
            if (exchangeCode == null) {
                throw new BizIllegalException("兑换码不存在！");
            }
            // 4.是否过期
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(exchangeCode.getExpiredTime())){
                throw new BizIllegalException("兑换码已经过期！");
            }
            // 5.校验并生成用户券
            // 5.1.查询优惠券
            Coupon coupon = couponMapper.selectById(exchangeCode.getExchangeTargetId());
            // 5.2.查询用户
            Long userId = UserContext.getUser();
            // 5.3.校验并生成用户券，更新兑换码状态
            IUserCouponService proxy = (IUserCouponService)AopContext.currentProxy();
            proxy.checkAndCreateUserCoupon(coupon, userId,serialNum);
        } catch (Exception e) {
            // 重置兑换的标记 0
            exchangeCodeService.updateExchangeMark(serialNum, false);
            throw e;
        }
    }


    /**
     * 校验信息以及保存优惠券领取记录
     * @param coupon 优惠券
     * @param userId 用户id
     */
    @Override
    @Transactional
    @MyLock(name = "lock:coupon:#{coupon.id}")
    public void checkAndCreateUserCoupon(Coupon coupon, Long userId,Long serialNum){
        // 1.校验每人限领数量
        // 1.1.统计当前用户对当前优惠券的已经领取的数量
        Integer count = lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, coupon.getId())
                .count();
        // 1.2.校验限领数量
        if(count != null && count >= coupon.getUserLimit()){
            throw new BadRequestException("您已超出领取限制！");
        }
        // 2.更新优惠券的已经发放的数量 + 1 前后数据库一致数量，防止并发
        int result = couponMapper.incrIssueNum(coupon.getId());
        if (result == 0) {
            throw new BizIllegalException("优惠券已领完！");
        }
        // 3.新增一个用户券
        saveUserCoupon(coupon, userId);
        /*throw new BizIllegalException("测试事务失效！");*/
        // 4.更新兑换码状态
        if (serialNum != null) {
            exchangeCodeService.lambdaUpdate()
                    .set(ExchangeCode::getUserId, userId)
                    .set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)
                    .eq(ExchangeCode::getId, serialNum)
                    .update();
        }
    }


    /**
     * 保存用户领取优惠券信息
     * @param coupon 优惠券
     * @param userId 用户id
     */
    private void saveUserCoupon(Coupon coupon, Long userId) {
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(coupon.getId());
        //获取优惠券指定的使用时间范围
        LocalDateTime beginTime = coupon.getTermBeginTime();
        LocalDateTime endTime = coupon.getTermEndTime();
        if (beginTime == null || endTime == null)
        {   //优惠券按天数计算
            beginTime = LocalDateTime.now();
            endTime = beginTime.plusDays(coupon.getTermDays());
        }
        userCoupon.setTermBeginTime(beginTime);
        userCoupon.setTermEndTime(endTime);
        save(userCoupon);
    }


}
