package com.PalmLife.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.PalmLife.dto.Result;
import com.PalmLife.entity.Voucher;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherService extends IService<Voucher> {

    Result queryVoucherOfShop(Long shopId);

    Result addSeckillVoucher(Voucher voucher);
}
