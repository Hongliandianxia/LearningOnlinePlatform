package com.tianji.promotion.service.impl;

import com.tianji.api.dto.promotion.CouponDiscountDTO;
import com.tianji.api.dto.promotion.OrderCourseDTO;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.service.ICouponScopeService;
import com.tianji.promotion.service.IDiscountService;
import com.tianji.promotion.strategy.discount.Discount;
import com.tianji.promotion.strategy.discount.DiscountStrategy;
import com.tianji.promotion.ultis.PermuteUtil;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author hazard
 * @version 1.0
 * @description 折扣服务的
 * @date 2025/8/11 20:28
 */
@Slf4j
@Service
@RequiredArgsConstructor
class DiscountServiceImpl implements IDiscountService {

    private final Executor discountSolutionExecutor;
    private final UserCouponMapper userCouponMapper;
    private final ICouponScopeService scopeService;


    /**
     * 查询用户优惠券使用方案
     * @param orderCourses 订单课程
     * @return 优惠券使用方案
     */
    @Override
    public List<CouponDiscountDTO> findDiscountSolution(List<OrderCourseDTO> orderCourses) {
        // 1.查询我的所有可用优惠券
        List<Coupon> coupons = userCouponMapper.queryMyCoupons(UserContext.getUser());
        if (CollUtils.isEmpty(coupons)) {
            return CollUtils.emptyList();
        }
        // 2.初筛
        // 2.1.计算订单总价
        int totalAmount = orderCourses.stream().mapToInt(OrderCourseDTO::getPrice).sum();
        // 2.2.筛选可用券
        List<Coupon> availableCoupons = coupons.stream()
                .filter(c -> DiscountStrategy.getDiscount(c.getDiscountType()).canUse(totalAmount, c))
                .collect(Collectors.toList());
        if (CollUtils.isEmpty(availableCoupons)) {
            return CollUtils.emptyList();
        }
        // 3.排列组合出所有方案
        // 3.1.细筛（找出每一个优惠券的可用的课程，判断课程总价是否达到优惠券的使用需求）
        Map<Coupon, List<OrderCourseDTO>> availableCouponMap = findAvailableCoupon(availableCoupons, orderCourses);
        if (CollUtils.isEmpty(availableCouponMap)) {
            return CollUtils.emptyList();
        }
        // 3.2.排列组合
        availableCoupons = new ArrayList<>(availableCouponMap.keySet());
        List<List<Coupon>> solutions = PermuteUtil.permute(availableCoupons);
        // 3.3.添加单券的方案
        for (Coupon c : availableCoupons) {
            solutions.add(List.of(c));
        }

/*      // 4.计算方案的优惠明细  普通模式，采用循环遍历，速度较慢
        List<CouponDiscountDTO> list =
                Collections.synchronizedList(new ArrayList<>(solutions.size()));
        for (List<Coupon> solution : solutions) {
            list.add(calculateSolutionDiscount(availableCouponMap, orderCourses, solution));
        }*/
        // 4.计算方案的优惠明细 线程池模式, 使用同步List，避免出现线程安全问题，同步List机制
        List<CouponDiscountDTO> list = Collections.synchronizedList(new ArrayList<>(solutions.size()));
        // 4.1.定义闭锁
        CountDownLatch latch = new CountDownLatch(solutions.size());
        for (List<Coupon> solution : solutions) {
            // 4.2.异步计算
            CompletableFuture
                    .supplyAsync(
                            () -> calculateSolutionDiscount(availableCouponMap, orderCourses, solution),
                            discountSolutionExecutor
                    ).thenAccept(dto -> {
                        // 4.3.提交任务结果
                        list.add(dto);
                        latch.countDown();
                    });
        }
        // 4.4.等待运算结束
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("优惠方案计算被中断，{}", e.getMessage());
        }

        // 5.筛选最优解
        return findBestSolution(list);
    }


    /**
     *  细筛优惠券
     * @param coupons 优惠券列表
     * @param courses 订单课程列表
     * @return 筛选后的优惠券列表
     */
    private Map<Coupon, List<OrderCourseDTO>> findAvailableCoupon(
            List<Coupon> coupons, List<OrderCourseDTO> courses) {
        Map<Coupon, List<OrderCourseDTO>> map = new HashMap<>(coupons.size());
        for (Coupon coupon : coupons) {
            // 1.找出优惠券的可用的课程
            List<OrderCourseDTO> availableCourses = courses;
            if (coupon.getSpecific()) {
                // 1.1.限定了范围，查询券的可用范围
                List<CouponScope> scopes = scopeService.lambdaQuery().eq(CouponScope::getCouponId, coupon.getId()).list();
                // 1.2.获取范围对应的分类id
                Set<Long> scopeIds = scopes.stream().map(CouponScope::getBizId).collect(Collectors.toSet());
                // 1.3.筛选课程
                availableCourses = courses.stream()
                        .filter(c -> scopeIds.contains(c.getCateId())).collect(Collectors.toList());
            }
            if (CollUtils.isEmpty(availableCourses)) {
                // 没有任何可用课程，抛弃
                continue;
            }
            // 2.计算课程总价
            int totalAmount = availableCourses.stream().mapToInt(OrderCourseDTO::getPrice).sum();
            // 3.判断是否可用
            Discount discount = DiscountStrategy.getDiscount(coupon.getDiscountType());
            if (discount.canUse(totalAmount, coupon)) {
                map.put(coupon, availableCourses);
            }
        }
        return map;
    }


    /**
     * 根据优惠券方案计算优惠价格
     * @param couponMap  优惠明细映射
     * @param courses    可用课程
     * @param solution   优惠券方案
     */
    private CouponDiscountDTO calculateSolutionDiscount(
            Map<Coupon, List<OrderCourseDTO>> couponMap, List<OrderCourseDTO> courses, List<Coupon> solution) {
        // 1.初始化DTO
        CouponDiscountDTO dto = new CouponDiscountDTO();
        // 2.初始化折扣明细的映射,oc: 是 Lambda 表达式的参数名，代表流中的每个 OrderCourseDTO 对象
        Map<Long, Integer> detailMap = courses.stream().collect(Collectors.toMap(OrderCourseDTO::getId, oc -> 0));
        // 3.计算折扣
        for (Coupon coupon : solution) {
            // 3.1.获取优惠券限定范围对应的课程
            List<OrderCourseDTO> availableCourses = couponMap.get(coupon);
            // 3.2.计算课程总价(课程原价 - 折扣明细),第一次为原价，后续为优惠券优惠金额
            int totalAmount = availableCourses.stream()
                    .mapToInt(oc -> oc.getPrice() - detailMap.get(oc.getId())).sum();
            // 3.3.判断是否可用
            Discount discount = DiscountStrategy.getDiscount(coupon.getDiscountType());
            if (!discount.canUse(totalAmount, coupon)) {
                // 券不可用，跳过
                continue;
            }
            // 3.4.计算优惠金额
            int discountAmount = discount.calculateDiscount(totalAmount, coupon);
            // 3.5.计算优惠明细
            calculateDiscountDetails(detailMap, availableCourses, totalAmount, discountAmount);
            // 3.6.更新DTO数据
            dto.getIds().add(coupon.getCreater());
            dto.getRules().add(discount.getRule(coupon));
            dto.setDiscountAmount(discountAmount + dto.getDiscountAmount());
        }
        return dto;
    }


    /**
     * 计算优惠明细
     * @param detailMap       折扣明细映射
     * @param courses         课程列表
     * @param totalAmount     课程总价
     * @param discountAmount  优惠金额
     */
    private void calculateDiscountDetails(Map<Long, Integer> detailMap, List<OrderCourseDTO> courses,
                                          int totalAmount, int discountAmount) {
        int times = 0;
        //剩余折扣金额
        int remainDiscount = discountAmount;
        for (OrderCourseDTO course : courses) {
            // 更新课程已计算数量
            times++;
            int discount;
            // 判断是否是最后一个课程
            if (times == courses.size()) {
                // 是最后一个课程，总折扣金额 - 之前所有商品的折扣金额之和
                discount = remainDiscount;
            } else {
                // 计算折扣明细（课程价格在总价中占的比例，乘以总的折扣）
                discount = discountAmount * course.getPrice() / totalAmount;
                remainDiscount -= discount;
            }
            // 更新折扣明细
            detailMap.put(course.getId(), discount + detailMap.get(course.getId()));
        }
    }


    /**
     * 计算最佳优惠券折扣方案
     * @description 优惠券数量相同时，金额最高；金额相同时，用优惠券最少
     * @param list 可用优惠券集合
     */
    private List<CouponDiscountDTO> findBestSolution(List<CouponDiscountDTO> list) {
        // 1.准备Map记录最优解
        Map<String, CouponDiscountDTO> moreDiscountMap = new HashMap<>();
        Map<Integer, CouponDiscountDTO> lessCouponMap = new HashMap<>();
        // 2.遍历，筛选最优解
        for (CouponDiscountDTO solution : list) {
            // 2.1.计算当前方案的id组合
            String ids = solution.getIds().stream()
                    .sorted(Long::compare).map(String::valueOf).collect(Collectors.joining(","));
            // 2.2.比较用券相同时，优惠金额是否最大
            CouponDiscountDTO best = moreDiscountMap.get(ids);
            if (best != null && best.getDiscountAmount() >= solution.getDiscountAmount()) {
                // 当前方案优惠金额少，跳过
                continue;
            }
            // 2.3.比较金额相同时，用券数量是否最少
            best = lessCouponMap.get(solution.getDiscountAmount());
            int size = solution.getIds().size();
            if (size > 1 && best != null && best.getIds().size() <= size) {
                // 当前方案用券更多，放弃
                continue;
            }
            // 2.4.更新最优解
            moreDiscountMap.put(ids, solution);
            lessCouponMap.put(solution.getDiscountAmount(), solution);
        }
        // 3.求交集
        Collection<CouponDiscountDTO> bestSolutions = CollUtils
                .intersection(moreDiscountMap.values(), lessCouponMap.values());
        // 4.排序，按优惠金额降序
        return bestSolutions.stream()
                .sorted(Comparator.comparingInt(CouponDiscountDTO::getDiscountAmount).reversed())
                .collect(Collectors.toList());
    }


}
