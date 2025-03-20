package com.PalmLife.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * redisson配置
 *
 * @author CHEN
 * @date 2022/10/10
 */
@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient(){
        //配置
        Config config=new Config();
        config.useSingleServer()
                .setAddress("redis://192.168.163.133:6379")
                .setPassword("123456");
        //创建对并且返回
        return Redisson.create(config);
    }

}
