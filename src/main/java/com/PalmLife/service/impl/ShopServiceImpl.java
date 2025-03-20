package com.PalmLife.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.PalmLife.utils.RedisData;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.PalmLife.dto.Result;
import com.PalmLife.entity.Shop;
import com.PalmLife.mapper.ShopMapper;
import com.PalmLife.service.IShopService;
import com.PalmLife.utils.CacheClient;
import com.PalmLife.utils.SystemConstants;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.PalmLife.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;

//    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);


    /**
     * 根据ID查询商店信息
     * @param id id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        //创建方法，使用空值解决缓存穿透
//        Shop shop = queryWithPassThrough(id);
        //使用Cache工具类，使用空值解决缓存穿透
//        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //互斥锁解决缓存击穿
//        Shop shop = queryWithMutex(id);
        //逻辑过期解决缓存击穿
//        Shop shop = queryWithLogicalExpire(id);
        //使用Cache工具类，逻辑过期解决缓存击穿
        Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    /**
     * 使用设置空值解决缓存穿透
     */
    /* private Shop queryWithPassThrough(Long id) {
        String shopKey = CACHE_SHOP_KEY + id;
        //从redis中查询
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
        //判断是否存在
        if (StringUtils.isNotEmpty(shopJson)) {
            //存在直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        //判断空值
        if ("".equals(shopJson)) {
            return null;
        }
        //不存在 查询数据库
        Shop shop = getById(id);
        if (shop == null) {
            //redis写入空值
            stringRedisTemplate.opsForValue().set(shopKey, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            //数据库不存在 返回错误
            return null;
        }
        //数据库存在 写入redis
        stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //返回
        return shop;
    }*/

    /**
     * 互斥锁解决缓存击穿
     *
     * @param id id
     * @return {@link Shop}
     */
    /*private Shop queryWithMutex(Long id) {
        String shopKey = CACHE_SHOP_KEY + id;
        //从redis中查询
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
        //判断是否存在
        if (StringUtils.isNotEmpty(shopJson)) {
            //存在直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        //如果缓存为空值，直接返回null
        if ("".equals(shopJson)) {
            return null;
        }
        //如果缓存中不存在，实现缓存重建
        //获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            //是否获取成功
            if (!isLock) {
                //获取失败 休眠并且重试当前方法
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            //成功获取锁 通过id查询数据库
            shop = getById(id);
            //模拟重建延时
            Thread.sleep(200);
            if (shop == null) {
                //如果数据库中数据为null，redis写入空值
                stringRedisTemplate.opsForValue().set(shopKey, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                //数据库不存在 返回错误
                return null;
            }
            //数据库存在 写入redis
            stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //释放互斥锁
            unLock(lockKey);
        }
        //返回
        return shop;
    }*/
    /**
     * 互斥锁解决缓存击穿辅助方法：获取锁
     *
     * @param key 关键
     * @return boolean
     */
    /*
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }*/
    /**
     * 互斥锁解决缓存击穿辅助方法：释放锁
     *
     * @param key 关键
     */
    /*
    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }*/


    /**
     * 逻辑过期解决缓存击穿
     *
     * @param id id
     * @return {@link Shop}
     */
    /*private Shop queryWithLogicalExpire(Long id) {
        String shopKey = CACHE_SHOP_KEY + id;
        //从redis中查询
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
        //判断是否存在
        if (StringUtils.isEmpty(shopJson)) {
            //不存在返回空
            return null;
        }
        //命中 反序列化：将redis中JSON格式数据转换为RedisDate自定义类型对象
        RedisDate redisDate = JSONUtil.toBean(shopJson, RedisDate.class);
        JSONObject jsonObject = (JSONObject) redisDate.getData();
        Shop shop = BeanUtil.toBean(jsonObject, Shop.class);
        LocalDateTime expireTime = redisDate.getExpireTime();
        //判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            //未过期 直接返回
            return shop;
        }
        //已过期
        //获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean flag = tryLock(lockKey);
        //是否获取锁成功
        if (flag) {
            //成功 异步重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    this.saveShopToRedis(id, 20L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放锁
                    unLock(lockKey);
                }
            });
        }
        //返回过期商铺信息
        return shop;
    }*/
    /**
     * 逻辑过期解决缓存击穿辅助方法：存入redis 携带逻辑过期时间
     */
    /*
    public void saveShopToRedis(Long id, Long expireSeconds) throws InterruptedException {
        //查询店铺数据
        Shop shop = getById(id);
        Thread.sleep(200);
        //封装逻辑过期
        RedisDate redisDate = new RedisDate();
        redisDate.setData(shop);
        //设置逻辑过期的目标时间
        redisDate.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        //将redisdata对象序列化为json字符串，存入到redis中
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisDate));
    }*/


    /**
     * 更新商铺信息
     * @param shop 商铺数据
     * @return 无
     */
    @Override
    @Transactional(rollbackFor = Exception.class)   //指定Exception异常时回滚
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("id不能为空");
        }
        //更新数据库
        updateById(shop);
        //删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }

    /**
     * 根据商铺类型分页查询商铺信息
     * @param typeId 商铺类型
     * @param current 页码
     * @return 商铺列表
     */
    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        //判断是否需要坐标查询
        if (x == null || y == null) {
            //不需要坐标查询
            Page<Shop> page = lambdaQuery()
                    .eq(Shop::getTypeId, typeId)
                    .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }
        //如果需要坐标查询，计算分页参数
        int from = (current - 1) * SystemConstants.MAX_PAGE_SIZE;
        int end = current * SystemConstants.MAX_PAGE_SIZE;
        //查询redis 距离排序 分页
        String key= SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo().search(
                key,    //Redis中存储GEO的Key
                GeoReference.fromCoordinate(x, y),//当前坐标的经纬度
                new Distance(5000), //查询距离5000米内的店铺
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()    //对于查询参数的配置
                        .includeDistance()  //查询在距离内
                        .limit(end) //查询的数量
        );
        //如果没有数据
        if (results == null){
            return Result.ok(Collections.emptyList());
        }

        //如果有数据，从中获取数据
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = results.getContent();
        //如果数据
        if (content.size() < from){
            //没有下一页
            return Result.ok();
        }
        //存放店铺名字，作为ID
        List<Long> ids = new ArrayList<>(content.size());
        //存店铺距离
        Map<String,Distance> distanceMap = new HashMap<>();
        //跳过前from个结果，对后续结果执行Stream流操作
        content.stream().skip(from).forEach(result->{
            //店铺id
            String shopId = result.getContent().getName();
            ids.add(Long.valueOf(shopId));
            //距离
            Distance distance = result.getDistance();
            distanceMap.put(shopId , distance);
        });

        //根据id（店铺名字）查询shop
        String join = StrUtil.join(",", ids);
        List<Shop> shopList = lambdaQuery()
                .in(Shop::getId, ids)   //查询这些店铺
                .last("order by field(id,"+join+")").list();    //按照id的顺序对结果升序排序

        for (Shop shop : shopList) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());  //对每个店铺设置距离
        }
        return Result.ok(shopList);
    }







}
