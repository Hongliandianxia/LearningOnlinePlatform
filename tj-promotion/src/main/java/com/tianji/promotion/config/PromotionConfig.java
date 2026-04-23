package com.tianji.promotion.config;

import com.tianji.common.autoconfigure.xxljob.XxlJobProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author hazard
 * @version 1.0
 * @description
 * @date 2025/7/29 21:17
 */
@Slf4j
@Configuration
public class PromotionConfig {

    // 生成兑换码的线程池
    @Bean
    public Executor threadPoolOfGenerateExchangeCodeExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //1.核心线程池大小
        executor.setCorePoolSize(2);
        //2.最大线程数量
        executor.setMaxPoolSize(5);
        //3.队列大小
        executor.setQueueCapacity(200);
        //4.线程名称
        executor.setThreadNamePrefix("exchange-code-handler-");
        //5.拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //初始化
        executor.initialize();
        return executor;
    }

    //优惠券折扣计算线程池
    @Bean
    public Executor discountSolutionExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 1.核心线程池大小
        executor.setCorePoolSize(12);
        // 2.最大线程池大小
        executor.setMaxPoolSize(12);
        // 3.队列大小
        executor.setQueueCapacity(99999);
        // 4.线程名称
        executor.setThreadNamePrefix("discount-solution-calculator-");
        // 5.拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}