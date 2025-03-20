package com.PalmLife.utils;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * redis ID生成器
 *
 * @author CHEN
 * @date 2022/10/09
 */
@Component
public class RedisIdWorker {
    /**
     * 初始时间戳
     */
    private static final Long BEGIN_TIMESTAMP = 1640995200L;
    /**
     * 序列号位数
     */
    private static final Integer COUNT_BITS = 32;

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 获取id
     *
     * @param keyPrefix 业务前缀
     * @return {@link Long}
     */
    public Long nextId(String keyPrefix) {
        //生成时间戳
        LocalDateTime now = LocalDateTime.now();
        //Unix 时间戳是从 1970年1月1日 00:00:00到当前时间经过的秒数。
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;
        //生成序列号
        //生成当前日期 精确到天
        String today = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // Redis 中存储的值进行自增操作
        Long count = redisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + today);
        //将左移后的 timestamp 与 count 进行按位或操作，生成最终的唯一时间戳。
        return timestamp << COUNT_BITS | count ;
    }

}
