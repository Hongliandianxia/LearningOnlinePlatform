package com.tianji.promotion.ultis;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Function;

import static com.tianji.promotion.ultis.LockType.*;

/**
 * @author hazard
 * @version 1.0
 * @description 锁工厂
 * @date 2025/8/6 22:39
 */
@Component
public class LockFactory {


    private final Map<LockType, Function<String , RLock>> lockHandlers;
    //使用构造方法初始化获取锁类型,函数式编程，引用，lambda表达式
    public LockFactory(RedissonClient redissonClient){
/*      不使用hashmap，避免hash运算等操作，提供效率，直接使用enumMap枚举map
        不需要指定hashmap大小，只需要指定key的类型（枚举类型）,根据枚举类型的项数判断map大小
        map大小固定，避免扩容，，提供效率
        可对枚举型进行编号，避免hash运算、取模操作，快速根据角标获取对应的锁对象
        */
        this.lockHandlers= new EnumMap<>(LockType.class);
        this.lockHandlers.put(RE_ENTRANT_LOCK, redissonClient::getLock);
        this.lockHandlers.put(FAIR_LOCK, redissonClient::getLock);
        this.lockHandlers.put(READ_LOCK,name -> redissonClient.getReadWriteLock(name).readLock());
        this.lockHandlers.put(WRITE_LOCk,name -> redissonClient.getReadWriteLock(name).writeLock());

    }

    /**
     * 获取锁  优化每一种锁类型对应一种操作，封装成键值对的map，达到键值映射的效果
     * @param lockType 锁类型
     * @param name 锁名称
     * @return RLock
     */
    public RLock getLock(LockType lockType,String name) {
        Function<String, RLock> lockHandler = lockHandlers.get(lockType);
        if(lockHandler==null){
            throw new RuntimeException("锁类型错误！");
        }
        //apply方法java8，将name作为参数传入，返回对应的锁对象(接收一个参数，返回一个结果)
        return lockHandler.apply(name);
/*    RLock lock=null;
    switch(myLock.lockType()) {
        case RE_ENTRANT_LOCK:
            lock = redissonClient.getLock(name);
            break;
        case FAIR_LOCK:
            lock = redissonClient.getFairLock(name);
            break;
        case READ_LOCK:
            lock = redissonClient.getReadWriteLock(name).readLock();
            break;
        case WRITE_LOCk:
            lock = redissonClient.getReadWriteLock(name).writeLock();
            break;
        default:
            throw new RuntimeException("锁类型错误！");
    }
    return lock;
    }*/
    }
}
