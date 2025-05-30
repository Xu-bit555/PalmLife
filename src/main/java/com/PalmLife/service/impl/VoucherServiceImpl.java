package com.PalmLife.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.PalmLife.dto.Result;
import com.PalmLife.entity.SeckillVoucher;
import com.PalmLife.entity.Voucher;
import com.PalmLife.mapper.VoucherMapper;
import com.PalmLife.service.ISeckillVoucherService;
import com.PalmLife.service.IVoucherService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

import static com.PalmLife.utils.RedisConstants.SECKILL_STOCK_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 新增秒杀券
     * @param voucher 优惠券信息，包含秒杀信息
     * @return 优惠券id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result addSeckillVoucher(Voucher voucher) {
        //判断新增的优惠卷ID是否重复
        Boolean flag = seckillVoucherService.equals(
                new LambdaQueryWrapper<Voucher>()
                        .eq(Voucher::getId , voucher.getId()));
        if(flag == true){
            return Result.fail("优惠卷id重复");
        }
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);
        //保存秒杀库存的到redis
        stringRedisTemplate.opsForValue().set(SECKILL_STOCK_KEY + voucher.getId() , voucher.getStock().toString());
        return Result.ok(voucher.getId());
    }

    /**
     * 查询店铺的优惠券
     * @param shopId 店铺id
     * @return 优惠券列表
     */
    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        // 返回结果
        return Result.ok(vouchers);
    }


}
