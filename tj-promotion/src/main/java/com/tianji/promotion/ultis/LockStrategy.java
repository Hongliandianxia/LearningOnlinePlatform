package com.tianji.promotion.ultis;

import com.tianji.common.exceptions.BizIllegalException;
import org.redisson.api.RLock;
/**
 * @author hazard
 * @version 1.0
 * @description 枚举策略
 * @date 2025/8/7 1:30
 */
public enum LockStrategy {
    // 快速结束（返回）
    SKIP_FAST(){
        @Override
        public boolean tryLock(RLock lock, MyLock prop) throws InterruptedException {
            return lock.tryLock(0, prop.leaseTime(), prop.unit());
        }
    },
    // 快速结束（抛出异常）
    FAIL_FAST(){
        @Override
        public boolean tryLock(RLock lock, MyLock prop) throws InterruptedException {
            boolean tryLock = lock.tryLock(0, prop.leaseTime(), prop.unit());
            if (!tryLock){
                throw new BizIllegalException("重复请求");
            }
            return true;
        }
    },
    //无限重试
    KEEP_TRYING(){
        @Override
        public boolean tryLock(RLock lock, MyLock prop) {
            //无限重试
            lock.lock(prop.leaseTime(), prop.unit());
            return true;
        }
    },
    //超时后返回
    SKIP_AFTER_READY_TIMEOUT(){
        @Override
        public boolean tryLock(RLock lock, MyLock prop) throws InterruptedException {
            return lock.tryLock(prop.waitTime(), prop.leaseTime(), prop.unit());
        }
    },
    //超时后抛异常
    FAIL_AFTER_READY_TIMEOUT(){
        @Override
        public boolean tryLock(RLock lock, MyLock prop) throws InterruptedException {
            boolean tryLock = lock.tryLock(prop.waitTime(), prop.leaseTime(), prop.unit());
            if (!tryLock) {
                throw new BizIllegalException("重复请求！");
            }
            return true;
        }

    },
    ;
    //枚举封装业务,抽象方法充当策略接口
    public abstract boolean tryLock(RLock lock,MyLock prop) throws Exception;

}
