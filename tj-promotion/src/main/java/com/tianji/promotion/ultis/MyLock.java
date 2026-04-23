package com.tianji.promotion.ultis;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * @author hazard
 * @version 1.0
 * @description 自定义注解
 * @date 2025/8/6 21:06
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MyLock {

    String name();

    long waitTime() default 1;

    //redisson自带看门狗机制，执行传参会导致看门狗机制失效，只有设置为-1才会开启看门狗机制
    long leaseTime() default -1;

    TimeUnit unit() default TimeUnit.SECONDS;
    //锁类型,默认可重入锁,方便工厂模式改造
    LockType lockType() default LockType.RE_ENTRANT_LOCK;

    LockStrategy lockStrategy() default LockStrategy.FAIL_AFTER_READY_TIMEOUT;
}
