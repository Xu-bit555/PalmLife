package com.PalmLife.utils;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class BloomFilterConfig {

    @Bean
    public BloomFilter<Long> bloomFilter() {
        return BloomFilter.create(Funnels.longFunnel(), 1500, 0.01);
    }
}