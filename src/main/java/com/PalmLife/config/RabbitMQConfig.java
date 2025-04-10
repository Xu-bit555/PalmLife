package com.PalmLife.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 */

@Configuration
public class RabbitMQConfig {
    @Value("${spring.rabbitmq.host}")
    private String host;
    @Value("${spring.rabbitmq.port}")
    private int port;
    @Value("${spring.rabbitmq.username}")
    private String username;
    @Value("${spring.rabbitmq.password}")
    private String password;
    @Value("${spring.rabbitmq.virtual-host}")
    private String virtualHost;

    public static final String QUEUE_NAME = "order1";
    public static final String EXCHANGE_NAME = "life";
    public static final String ROUNTING_KEY = "order1";



    @Bean   //RabbitTemplate 是 Spring AMQP 框架提供的一个高级抽象类
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory cachingConnectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate( cachingConnectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());

        return rabbitTemplate;
    }

    @Bean("cachingConnectionFactory")   //创建Spring AMQP提供的连接工厂，类型为CachingConnectionFactory，Bean指定的名称为cachingConnectionFactory
    public CachingConnectionFactory cachingConnectionFactory (){
        CachingConnectionFactory  cachingConnectionFactory  = new CachingConnectionFactory ();
        cachingConnectionFactory .setHost(host);
        cachingConnectionFactory .setPort(port);
        cachingConnectionFactory .setUsername(username);
        cachingConnectionFactory .setPassword(password);
        cachingConnectionFactory .setVirtualHost(virtualHost);

        return cachingConnectionFactory ;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(CachingConnectionFactory cachingConnectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(cachingConnectionFactory);
        factory.setConcurrentConsumers(5); // 设置同时启动的消费者数量
        factory.setMaxConcurrentConsumers(10); // 设置最大消费者数量
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }


    //实现JSON序列化
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.getJavaTypeMapper().addTrustedPackages("com.PalmLife.entity", "com.PalmLife.dto");
        return converter;
    }




    /**
     * 自定义消息队列
     * @return
     */
    @Bean
    public Queue rabbitmqQueue(){
        return new Queue(QUEUE_NAME,true,false,false);
    }
    //durable，默认false，消息是否持久化，即在无消费者消费时数据存储在内存还是磁盘中
    //exclusive，默认false，容i是否只能被当前创建的连接使用，并且关闭后自动删除
    //autoDelete，是否自动删除，为true时，当容器不再使用则会自动删除

    /**
     * 自定义交换机
     * @return
     */
    @Bean
    public DirectExchange rabbitmqDirectExchange(){
        return new DirectExchange(EXCHANGE_NAME,true,false);
    }

    /**
     * 关联绑定
     * @return
     */
    @Bean
    public Binding bindDirect(){
        return BindingBuilder.bind(rabbitmqQueue()).to(rabbitmqDirectExchange()).with(ROUNTING_KEY);
    }






}
