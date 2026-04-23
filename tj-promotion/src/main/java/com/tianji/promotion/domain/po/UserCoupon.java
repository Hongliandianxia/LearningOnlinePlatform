package com.tianji.promotion.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;

import com.tianji.promotion.enums.UserCouponStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息
 * </p>
 *
 * @author hazard
 * @since 2025-07-31
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_coupon")
public class UserCoupon implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户券id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 优惠券的拥有者
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 优惠券模板id
     */
    @TableField("coupon_id")
    private Long couponId;

    /**
     * 优惠券有效期开始时间
     */
    @TableField("term_begin_time")
    private LocalDateTime termBeginTime;

    /**
     * 优惠券有效期结束时间
     */
    @TableField("term_end_time")
    private LocalDateTime termEndTime;

    /**
     * 优惠券使用时间（核销时间）
     */
    @TableField("used_time")
    private LocalDateTime usedTime;

    /**
     * 优惠券状态，1：未使用，2：已使用，3：已失效
     */
    @TableField("status")
    private UserCouponStatus status;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;


}
