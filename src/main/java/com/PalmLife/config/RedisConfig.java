package com.PalmLife.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis工具类的配置类
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        //配置Redis连接工厂
        redisTemplate.setConnectionFactory(factory);
        //使用 StringRedisSerializer 作为键的序列化器
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(keySerializer);  //用于将键（Key）序列化为字符串。
        redisTemplate.setHashKeySerializer(keySerializer);//确保所有哈希键在存储到 Redis 时都以字符串形式存储。


        //调用 afterPropertiesSet 方法，完成 RedisTemplate 的初始化。
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }


}
