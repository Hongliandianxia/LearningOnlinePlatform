package com.tianji.promotion.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 优惠券作用范围信息
 * </p>
 *
 * @author hazard
 * @since 2025-07-28
 */
@Data
@EqualsAndHashCode(callSuper = false)
//@Accessors(chain = true) 链式调用，getter 和 setter 方法返回值的对象还是当前本身
@Accessors(chain = true)
@TableName("coupon_scope")
public class CouponScope implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 范围限定类型：1-分类
     */

    @TableField("type")
    private Integer type;

    /**
     * 优惠券id
     */
    @TableField("coupon_id")
    private Long couponId;

    /**
     * 优惠券作用范围的业务id，例如分类id
     */
    @TableField("biz_id")
    private Long bizId;


}
