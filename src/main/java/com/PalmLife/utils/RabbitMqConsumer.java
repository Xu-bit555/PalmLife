package com.PalmLife.utils;


import com.PalmLife.entity.SeckillVoucher;
import com.PalmLife.entity.VoucherOrder;
import com.PalmLife.mapper.VoucherOrderMapper;
import com.PalmLife.service.ISeckillVoucherService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rabbitmq.client.Channel;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;


/**
 * RabbitMQ消费者
 */
@Service
@RabbitListener(queues = {"order1"})
public class RabbitMqConsumer {

    @Resource
    private RedissonClient redissonClient;
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private VoucherOrderMapper voucherOrderMapper;
    @Resource
    private RedisTemplate redisTemplate;


    @RabbitHandler
    public void consumer(VoucherOrder voucherOrder, Channel channel, Message message) throws IOException {
        try {

            //判断是否已经有订单
            VoucherOrder voucherOrder1 = voucherOrderMapper.selectById(voucherOrder.getId());
            if(voucherOrder1 == null){
                System.out.println("禁止重复下单");
                return;
            }
            //下单
            Long userId = voucherOrder.getUserId();
            //创建锁对象（兜底）
            RLock lock = redissonClient.getLock("lock:order:" + userId);
            //获取锁
            boolean isLock = lock.tryLock();
            //判断是否获取锁成功
            if (!isLock) {
                //获取失败,返回错误或者重试
                throw new RuntimeException("发送未知错误");
            }
            //获取锁成功，执行创建订单操作
            try {
                this.createVoucherOrder(voucherOrder);
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                //deliveryTag 标识符处理的是哪个消息。true代表批量确定消息，false代表单独确认消息
                redisTemplate.delete("seckill:order:" + voucherOrder.getVoucherId());
            } finally {
                //释放锁
                lock.unlock();
            }

            //ack确认消息
        } catch (Exception e) {
            e.printStackTrace();
            // 如果处理消息失败，可以选择重新入队或者拒绝消息
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
            //deliveryTag 标识 代表处理的消息，true代表将处理失败的消息放到队列尾部等待其他消费者处理。false代表消息被丢弃不会重新放入队列中
        }
    }

    /**\
     * 执行创建订单存入数据库中
     * @param voucherOrder
     */
    @Transactional(rollbackFor = Exception.class)
    public void createVoucherOrder(VoucherOrder voucherOrder) {

        //扣减库存
        boolean isSuccess = seckillVoucherService.update(
                new LambdaUpdateWrapper<SeckillVoucher>()
                        .eq(SeckillVoucher::getVoucherId, voucherOrder.getVoucherId())
                        .gt(SeckillVoucher::getStock, 0)
                        .setSql("stock = stock-1"));
        if(isSuccess){
            //创建订单
            int rows = voucherOrderMapper.save(voucherOrder);
            if (rows <= 0) {
                throw new RuntimeException("订单创建失败");
            }
        }
    }



}
