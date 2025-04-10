package com.PalmLife;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类
 *
 * @author CHEN
 * @date 2022/10/07
 */
@MapperScan("com.PalmLife.mapper")
@SpringBootApplication
//@EnableLeafServer   //启动Leaf服务
public class PalmLifeApplication {

    public static void main(String[] args) {
        SpringApplication.run(PalmLifeApplication.class, args);
    }

}
