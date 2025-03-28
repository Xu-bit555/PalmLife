package com.PalmLife.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis工具类的配置类
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String,Object> redisTemplate = new RedisTemplate<>();
        //配置Redis连接工厂
        redisTemplate.setConnectionFactory(factory);

        //序列化器为 StringRedisSerializer
        redisTemplate.setKeySerializer(new StringRedisSerializer());  //用于将键（Key）序列化为字符串。
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());//确保所有哈希键在存储到 Redis 时都以字符串形式存储。



        //序列化器为 GenericJackson2JsonRedisSerializer
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer()); //确保所有值都以 JSON 格式存储。
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());//确保所有哈希值在存储到 Redis 时都以 JSON 格式存储。


        //调用 afterPropertiesSet 方法，完成 RedisTemplate 的初始化。
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

}





