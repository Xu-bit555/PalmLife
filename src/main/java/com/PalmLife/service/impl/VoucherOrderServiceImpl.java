package com.PalmLife.service.impl;

import com.PalmLife.dto.Result;
import com.PalmLife.dto.UserDTO;
import com.PalmLife.entity.SeckillVoucher;
import com.PalmLife.entity.VoucherOrder;
import com.PalmLife.mapper.VoucherOrderMapper;
import com.PalmLife.service.ISeckillVoucherService;
import com.PalmLife.service.IVoucherOrderService;
import com.PalmLife.utils.RedisIdWorker;
import com.PalmLife.utils.SnowflakeIdWorker;
import com.PalmLife.utils.UserHolder;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;


/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    /**
     * 生成分布式ID
     */
    @Resource
    private RedisIdWorker redisIdWorker;

    /**
     * 实现分布式锁
     */
    @Resource
    private RedissonClient redissonClient;

    /**
     * Redis的模版类
     */
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     *RabbitMQ的模版类
     */
    @Resource
    private RabbitTemplate rabbitTemplate;


    /**
     * 自己注入自己为了获取代理对象 @Lazy 延迟注入 避免形成循环依赖
     */
    @Resource
    @Lazy
    private IVoucherOrderService voucherOrderService;

    /**
     * 初始化lua脚本
     */
    private static final DefaultRedisScript<List> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();    //初始化脚本
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));   //脚本位置
        SECKILL_SCRIPT.setResultType(List.class);   //指定脚本返回类型
    }

    /**
     * 秒杀优惠券(执行lua脚本，将消息放入rabbitmq的消息队列)
     *
     * @param voucherId 券id
     * @return {@link Result}
     */
    @Override
    public Result seckillVoucher(Long voucherId) {
        //判断秒杀卷是否过期
//        SeckillVoucher seckillVoucher = new SeckillVoucher();


        //获取用户
        UserDTO user = UserHolder.getUser();
        //获取订单id
            //使用雪花算法
        Long orderId = SnowflakeIdWorker.nextId();
            //使用redis+时间戳
//        Long orderId = redisIdWorker.nextId("order");
        //执行lua脚本
        List<Object> res =stringRedisTemplate.execute(
                SECKILL_SCRIPT  //执行脚本
                , Collections.emptyList()
                , voucherId.toString()  //参数一：优惠卷ID
                , user.getId().toString()  //参数二：用户ID
                , orderId.toString()//参数三：订单ID
        );

        Long flag = (Long)res.get(0);
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setVoucherId( Long.valueOf((String) res.get(1)) );
        voucherOrder.setUserId( Long.valueOf((String)res.get(2))  );
        voucherOrder.setId( orderId );

        if (flag != 0) {
            //不为0 没有购买资格
            return Result.fail(flag == 1 ? "库存不足" : "禁止重复下单");
        }
        rabbitTemplate.convertAndSend("life", "order1", voucherOrder);


        return Result.ok(voucherOrder.getId());
    }




/**
 * 处理Redis消息队列
 */
    /**
     * 初始化线程池，完成ridis消息队列操作
     */
    //private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();
    /**
     * 处理消息队列中的消息
     */
   /*@PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(() -> {
            String queueName="stream.orders";
            while (true) {
                try {
                    //从消息队列中获取订单信息.
                    List<MapRecord<String, Object, Object>> list = redisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1")   //创建消费者组，指定消费者组（g1）和消费者名称（c1）。
                            , StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2))   //创建读取选项，指定读取数量（1）和阻塞时间（2秒）。
                            , StreamOffset.create(queueName, ReadOffset.lastConsumed()) //指定从流的末尾开始读取消息。
                    );

                    //判断消息时候获取成功
                    if (list == null || list.isEmpty()){
                        //获取失败 没有消息 继续循环
                        continue;
                    }
                    //获取成功 解析消息
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> values = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true); //将消息转换为voucherOrder实体类
                    //下单
                    handleVoucherOrder(voucherOrder);
                    //ack确认消息
                    redisTemplate.opsForStream().acknowledge(queueName ,"g1" , record.getId()); //向消息队列的g1消费组确认，返回消息id
                } catch (Exception e) {
                    e.printStackTrace();
                    handlePendingList();    //同样走到处理订单的方法
                }
            }
        });
    }
 */
    /**
     * 处理消息队列中的消息：重复方法
     */
   /*  private void handlePendingList() {
        String queueName="stream.orders";
        while (true){
            try {
                //从消息队列中获取订单信息
                List<MapRecord<String, Object, Object>> list = redisTemplate.opsForStream().read(
                        Consumer.from("g1", "c1")
                        , StreamReadOptions.empty().count(1)
                        , StreamOffset.create(queueName, ReadOffset.from("0"))
                );
                //判断消息时候获取成功
                if (list==null||list.isEmpty()){
                    //获取失败 没有消息 继续循环
                    break;
                }
                //获取成功 解析消息
                MapRecord<String, Object, Object> record = list.get(0);
                Map<Object, Object> values = record.getValue();
                VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
                //下单
                handleVoucherOrder(voucherOrder);
                //ack确认消息
                redisTemplate.opsForStream().acknowledge(queueName,"g1",record.getId());
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }
   */
    /**
     * 处理消息队列中消息辅助方法：执行创建订单操作
     * @param voucherOrder
     */
   /* private void handleVoucherOrder(VoucherOrder voucherOrder) {
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
        try {
            voucherOrderService.createVoucherOrder(voucherOrder);
        } finally {
            //释放锁
            lock.unlock();
        }
    }
   */
    /**
     * 执行创建订单操作辅助方法：创建优惠卷订单
     * @param voucherOrder 券订单
     */
   /* @Override
    @Transactional(rollbackFor = Exception.class)
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        //扣减库存
        boolean isSuccess = seckillVoucherService.update(
                new LambdaUpdateWrapper<SeckillVoucher>()
                        .eq(SeckillVoucher::getVoucherId, voucherOrder.getVoucherId())
                        .gt(SeckillVoucher::getStock, 0)
                        .setSql("stock=stock-1"));
        //创建订单
        this.save(voucherOrder);
    }
   */


/**
 * 存放Reids消息队列
  */
    /**
     * 秒杀优惠券(执行lua脚本，将消息放入redis的消息队列)
     *
     * @param voucherId 券id
     * @return {@link Result}
     */
    /**
    @Override
    public Result seckillVoucher(Long voucherId) {

        //获取用户
        UserDTO user = UserHolder.getUser();
        //获取订单id
        Long orderId = redisIdWorker.nextId("order");
        //执行lua脚本
        Long res = (Long) redisTemplate.execute(
                SECKILL_SCRIPT  //执行脚本
                , Collections.emptyList()   //无参数的脚本
                , voucherId.toString()  //参数一：优惠卷ID
                , user.getId().toString()   //参数二：用户ID
                , orderId.toString());  //参数三：订单ID
        //判断结果是否为0
        int r = res.intValue();
        if (r != 0) {
            //不为0 没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "禁止重复下单");
        }

        return Result.ok(orderId);
    }*/


/**
 * 使用阻塞队列处理秒杀优惠卷
  */
    /**
     * 初始化阻塞队列
     */
    //private static final BlockingQueue<VoucherOrder> orderTasks=new ArrayBlockingQueue<>(1024*1024);
    /**
     * 秒杀优惠券(异步)
     *
     * @param voucherId 券id
     * @return {@link Result}
     */
    /*@Override
    public Result seckillVoucher(Long voucherId) {
        //获取用户
        UserDTO user = UserHolder.getUser();
        //执行lua脚本
        Long res = stringRedisTemplate.execute(
                SECKILL_SCRIPT
                , Collections.emptyList()
                , voucherId.toString()
                ,user.getId().toString());
        //判断结果是否为0
        int r=res.intValue();
        if (r!=0){
            //不为0 没有购买资格
            return Result.fail(r==1?"库存不足":"禁止重复下单");
        }
        //为0有购买资格
        Long orderId = redisIdWorker.nextId("order");
        //创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(UserHolder.getUser().getId());
        voucherOrder.setId(orderId);
        //存入阻塞队列
        orderTasks.add(voucherOrder);
        //返回订单id
        return Result.ok(orderId);
    }*/


/**
 * 单体项目使用互斥锁秒杀优惠卷
  */
    /**
     * 秒杀优惠券
     *
     * @param voucherId 券id
     * @return {@link Result}
     */
    /*@Override
    public Result seckillVoucher(Long voucherId) {
        //查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        //判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            //秒杀尚未开始
            return Result.fail("秒杀尚未开始");
        }
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            //秒杀已经结束
            return Result.fail("秒杀已经结束");
        }
        //判断库存是否充足
        if (voucher.getStock() < 1) {
            //库存不足
            return Result.fail("库存不足");
        }
        Long userId = UserHolder.getUser().getId();
        //仅限单体应用使用
//        synchronized (userId.toString().intern()) {
//            //实现获取代理对象 比较复杂 我采用了自己注入自己的方式
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return voucherOrderService.getResult(voucherId);
//        }
        //创建锁对象
//        SimpleRedisLock simpleRedisLock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        //获取锁
//        boolean isLock = simpleRedisLock.tryLock(1200L);
        boolean isLock = lock.tryLock();
        //判断是否获取锁成功
        if (!isLock){
            //获取失败,返回错误或者重试
            return Result.fail("一人一单哦！");
        }
        try {
            return voucherOrderService.getResult(voucherId);
        } finally {
            //释放锁
            lock.unlock();
        }
    }*/


    /**
     * 得到结果
     *
     * @param voucherId 券id
     * @return {@link Result}
     */
    @Override
//    @NotNull
    @Transactional(rollbackFor = Exception.class)
    public Result getResult(Long voucherId) {
        //是否下单
        Long userId = UserHolder.getUser().getId();
        Long count = lambdaQuery()
                .eq(VoucherOrder::getVoucherId, voucherId)
                .eq(VoucherOrder::getUserId, userId)
                .count();
        if (count > 0) {
            return Result.fail("禁止重复购买");
        }
        //扣减库存
        boolean isSuccess = seckillVoucherService.update(
                new LambdaUpdateWrapper<SeckillVoucher>()
                        .eq(SeckillVoucher::getVoucherId, voucherId)
                        .gt(SeckillVoucher::getStock, 0)
                        .setSql("stock=stock-1"));
        if (!isSuccess) {
            //库存不足
            return Result.fail("库存不足");
        }
        //创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //为订单生成id
        Long orderId = redisIdWorker.nextId("order");
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(UserHolder.getUser().getId());
        voucherOrder.setId(orderId);
        this.save(voucherOrder);
        //返回订单id
        return Result.ok(orderId);
    }



}
