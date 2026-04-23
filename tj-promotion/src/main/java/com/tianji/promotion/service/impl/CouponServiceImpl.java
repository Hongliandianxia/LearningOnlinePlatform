package com.tianji.promotion.service.impl;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponScopeVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.enums.ObtainType;
import com.tianji.promotion.enums.UserCouponStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.service.ICouponScopeService;
import com.tianji.promotion.service.ICouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tianji.promotion.enums.CouponStatus.ISSUING;
import static com.tianji.promotion.enums.CouponStatus.UN_ISSUE;

/**
 * <p>
 * 优惠券的规则信息 服务实现类
 * </p>
 *
 * @author hazard
 * @since 2025-07-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements ICouponService {
    private final ICouponScopeService scopeService;

    private final IExchangeCodeService codeService;

    private final IUserCouponService userCouponService;

    private final ICouponScopeService couponScopeService;

    /**
     * 新增优惠券，涉及到优惠券表和优惠券中间表信息，加事务
     *
     * @param couponFormDto 优惠券表单信息
     */
    @Override
    @Transactional
    public void addCoupon(CouponFormDTO couponFormDto) {

        //1.保存优惠券本身的信息
        //1.1 dto转换po
        Coupon coupon = BeanUtils.copyProperties(couponFormDto, Coupon.class);
        save(coupon);
        //2.保存关联表的信息
        //2.1判断优惠券是否使用范围限定
        if (!couponFormDto.getSpecific()) {
            return;
        }
        //2.2保存关联表信息
        List<Long> scopes = couponFormDto.getScopes();
        if (CollUtils.isEmpty(scopes)) {
            throw new BadRequestException("优惠券使用范围不能为空");
        }
        //scopes转化为po(CouponScope)
/*        List<CouponScope> scopeList = scopes.stream()
                .map(bizId -> new CouponScope().setBizId(bizId).setCouponId(coupon.getId()))
                .collect(Collectors.toList());//流式处理*/
        List<CouponScope> scopeList = new ArrayList<>();
        for (Long scope : scopes) {
            CouponScope scopePo = new CouponScope();
            scopePo.setBizId(scope);
            scopePo.setCouponId(coupon.getId());
            scopePo.setType(coupon.getType());
            scopeList.add(scopePo);
        }
        scopeService.saveBatch(scopeList);
    }

    /**
     * 分页查询优惠券
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @Override
    public PageDTO<CouponPageVO> pageQueryCoupons(CouponQuery query) {
        String name = query.getName();
        Integer status = query.getStatus();
        Integer type = query.getType();
        // 1.分页查询
        Page<Coupon> page = lambdaQuery()
                .like(StringUtils.isNotBlank(name), Coupon::getName, name)
                .eq(status != null, Coupon::getStatus, status)
                .eq(type != null, Coupon::getType, type)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        //2.数据转换
        List<Coupon> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        List<CouponPageVO> list = BeanUtils.copyList(records, CouponPageVO.class);
        return PageDTO.of(page, list);
    }


    /**
     * 发放优惠券
     *
     * @param dto 发放优惠券的参数
     */
    @Transactional
    @Override
    public void beginIssue(CouponIssueFormDTO dto) {
        // 1.查询优惠券
        Coupon coupon = getById(dto.getId());
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在！");
        }
        // 2.判断优惠券状态，是否是暂停或待发放,只有处于这两种状态的优惠券才能发放
        if (coupon.getStatus() != CouponStatus.DRAFT && coupon.getStatus() != CouponStatus.PAUSE) {
            throw new BizIllegalException("优惠券状态错误！");
        }
        // 3.判断是否是立刻发放
        LocalDateTime issueBeginTime = dto.getIssueBeginTime();
        LocalDateTime now = LocalDateTime.now();
        boolean isBegin = issueBeginTime == null || !issueBeginTime.isAfter(now);
        // 4.更新优惠券
        // 4.1.拷贝属性到PO
        Coupon c = BeanUtils.copyBean(dto, Coupon.class);
        // 4.2.更新状态
        if (isBegin) {
            c.setStatus(ISSUING);
            c.setIssueBeginTime(now);
        } else {
            c.setStatus(UN_ISSUE);
        }
        // 4.3.写入数据库
        updateById(c);

        //5.判断是否需要生成兑换码 获取方式为指定发放并且优惠券状态为待发放
        if (coupon.getObtainWay() == ObtainType.ISSUE && coupon.getStatus() == CouponStatus.DRAFT) {
            //5.1.异步生成兑换码
            //设置优惠券的结束发放时间
            coupon.setIssueEndTime(c.getIssueEndTime());
            codeService.asyncGenerateCode(coupon);
        }
    }

    /**
     * 根据id查询优惠券信息
     *
     * @param id id
     * @return CouponFormDTO
     */
    @Override
    public CouponDetailVO queryCouponById(Long id) {
        if (id == null) {
            return null;
        }
        Coupon coupon = lambdaQuery().eq(Coupon::getId, id).one();
        CouponDetailVO detailVO = BeanUtils.copyBean(coupon, CouponDetailVO.class);
        if (coupon.getSpecific()) {
            List<CouponScope> list = couponScopeService.lambdaQuery().eq(CouponScope::getCouponId, id).list();
            List<Long> scopeIdList = list.stream().map(CouponScope::getBizId).collect(Collectors.toList());
            List<CouponScopeVO> scopeVOList = new ArrayList<>(scopeIdList.size());
            for (Long l : scopeIdList) {
                CouponScopeVO couponScopeVO = new CouponScopeVO();
                couponScopeVO.setId(l);
                scopeVOList.add(couponScopeVO);
                detailVO.setScopes(scopeVOList);
                }
            }
        return detailVO;
    }



    /**
     * 修改优惠券信息
     * @param dto 修改的优惠券信息
     */
    @Override
    public void updateCoupon (CouponFormDTO dto){
        Long id = dto.getId();
        if (id == null) {
            throw new BadRequestException("优惠券id不能为空");
        }
        //查询数据库的优惠券状态，避免恶意修改
        Coupon couponOfDb = this.getById(id);
        if (couponOfDb.getStatus()!= CouponStatus.DRAFT) {
            throw new BizIllegalException("优惠券状态错误！");
        }

        Coupon coupon = BeanUtils.copyProperties(dto, Coupon.class);
        updateById(coupon);
        //2.保存关联表的信息
        //2.1判断优惠券是否使用范围限定
        if (!dto.getSpecific()) {
            return;
        }
        //2.2保存关联表信息
        List<Long> scopes = dto.getScopes();
        if (CollUtils.isEmpty(scopes)) {
            throw new BadRequestException("优惠券使用范围不能为空");
        }
        //scopes转化为po(CouponScope)
/*        List<CouponScope> scopeList = scopes.stream()
            .map(bizId -> new CouponScope().setBizId(bizId).setCouponId(coupon.getId()))
            .collect(Collectors.toList());//流式处理*/
        List<CouponScope> scopeList = new ArrayList<>();
        for (Long scope : scopes) {
            CouponScope scopePo = new CouponScope();
            scopePo.setBizId(scope);
            scopePo.setCouponId(coupon.getId());
            scopePo.setType(coupon.getType());
            scopeList.add(scopePo);
        }
        scopeService.updateBatchById(scopeList);
    }


    /**
     * 删除优惠券
     * @param id 优惠券id
     */
    @Override
    public void deleteCouponById(Long id) {
        Coupon coupon = lambdaQuery().eq(Coupon::getId, id).one();
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在");
        }
        if (coupon.getStatus() != CouponStatus.DRAFT) {
            throw new BizIllegalException("优惠券状态错误！");
        }
        //删除优惠券
        removeById(id);
    }


    /**
     * 暂停优惠券发放
     * @param id 优惠券id
     */
    @Override
    public void pauseCouponById(Long id) {
        Coupon coupon = lambdaQuery().eq(Coupon::getId, id).one();
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在");
        }
        if (coupon.getStatus() != CouponStatus.ISSUING) {
            throw new BizIllegalException("优惠券状态错误！");
        }
        coupon.setStatus(CouponStatus.PAUSE);
        updateById(coupon);
    }

    @Override
    public List<CouponVO> queryIssuingCoupons() {
        //1.查询发放中的优惠券列表
        List<Coupon> coupons = lambdaQuery()
                .eq(Coupon::getStatus, CouponStatus.ISSUING)
                .eq(Coupon::getObtainWay, ObtainType.PUBLIC)
                .list();
        if (CollUtils.isEmpty(coupons)) {
            return CollUtils.emptyList();
        }
        //2.查询用户已经领取的优惠券信息
        List<Long> couponIds = coupons.stream().map(Coupon::getId).collect(Collectors.toList());
        // 2.1.查询当前用户已经领取的优惠券的数据
        List<UserCoupon> userCoupons = userCouponService.lambdaQuery()
                .eq(UserCoupon::getUserId, UserContext.getUser())
                .in(UserCoupon::getCouponId, couponIds)
                .list();
        // 2.2.统计当前用户对优惠券的已经领取数量
        Map<Long, Long> issuedMap = userCoupons.stream()
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        // 2.3.统计当前用户对优惠券的已经领取并且未使用的数量
        Map<Long, Long> unusedMap = userCoupons.stream()
                .filter(uc -> uc.getStatus() == UserCouponStatus.UNUSED)
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));

        //3.封装VO返回
        List<CouponVO> list = new ArrayList<>(coupons.size());
        for (Coupon c : coupons) {
            CouponVO vo = BeanUtils.copyBean(c, CouponVO.class);
            list.add(vo);
            //3.2是否还可以领取
            vo.setAvailable(c.getIssueNum() < c.getTotalNum()
                    && issuedMap.getOrDefault(c.getId(), 0L) < c.getUserLimit());
            //3.3是否可以使用(用户存在领取了尚未使用的优惠券)
            vo.setReceived(unusedMap.getOrDefault(c.getId(), 0L) > 0);
            /*list.add(vo);*/
        }
        return list;
    }
}
