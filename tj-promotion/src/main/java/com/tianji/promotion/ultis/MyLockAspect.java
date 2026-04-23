package com.tianji.promotion.ultis;

import com.tianji.common.exceptions.BizIllegalException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * @author hazard
 * @version 1.0
 * @description 切面,还需要配置成bean 直接使用component
 * @date 2025/8/6 21:09
 */
@Component
@Aspect
@RequiredArgsConstructor
public class MyLockAspect implements Ordered {
    private final LockFactory lockFactory;

    @Around("@annotation(myLock)")
    public Object tryLock(ProceedingJoinPoint joinPoint, MyLock myLock) throws Throwable {
        //1.创建锁对象
        RLock lock=lockFactory.getLock(myLock.lockType(),myLock.name());
/*      //获取锁类型，简单模式，
        switch (myLock.lockType()) {
            case FAIR_LOCK:
                lock = redissonClient.getFairLock(myLock.name());
                break;
            case READ_LOCK:
                lock = redissonClient.getReadWriteLock(myLock.name()).readLock();
                break;
            case WRITE_LOCk:
                lock = redissonClient.getReadWriteLock(myLock.name()).writeLock();
                break;
            case RE_ENTRANT_LOCK:
                lock = redissonClient.getLock(myLock.name());
            default:
                throw new RuntimeException("锁类型错误！");
        }*/
/*        //2.尝试获取锁
        boolean tryLock = lock.tryLock(myLock.waitTime(), myLock.leaseTime(), myLock.unit());
        //3.判断
        //获取锁失败,根据场景选择不同处理（策略模式）
        if (!tryLock) {
            throw new BizIllegalException("重复请求！");
        }*/
        //2.尝试获取锁
        boolean tryLock = myLock.lockStrategy().tryLock(lock, myLock);
        //3.判断
        if (!tryLock) {
            //获取锁失败 快速结束
            return null;
        }
        // 获取锁成功,执行原有业务
        try {
            return joinPoint.proceed();
        }finally {
            //4.释放锁
            lock.unlock();
        }
    }

    //优先级设置
    @Override
    public int getOrder() {
        //默认是int的最大值，优先级最低，数字越小，优先级越高
        return 0;
    }
}
