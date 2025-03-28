package com.PalmLife.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.PalmLife.entity.VoucherOrder;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface VoucherOrderMapper extends BaseMapper<VoucherOrder> {
        int save(VoucherOrder voucherOrder);
}
