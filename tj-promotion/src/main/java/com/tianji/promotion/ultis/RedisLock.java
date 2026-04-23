package com.tianji.promotion.ultis;

import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.BooleanUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * @author hazard
 * @version 1.0
 * @description 简单分布式锁功能
 * @date 2025/8/3 17:39
 */
@RequiredArgsConstructor
public class RedisLock {

    private final String key;
    private final StringRedisTemplate stringRedisTemplate;
    public Boolean tryLock(long timeout, TimeUnit unit) {
        String value = Thread.currentThread().getName();
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
        return BooleanUtils.isTrue(success);
    }

    public void unlock() {
        stringRedisTemplate.delete(key);
    }
}
