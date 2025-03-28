package com.PalmLife.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置类
 */
@Configuration
public class ThreadPoolConfig {
    @Bean
    public ThreadPoolTaskExecutor threadPoolTaskExectour() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(50);    //最大线程数
        executor.setCorePoolSize(10);   //核心线程数
        executor.setQueueCapacity(500); //队列容量
        executor.setKeepAliveSeconds(30);   //线程空闲后的最大存活时间
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());    //拒绝策略
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
