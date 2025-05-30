package com.PalmLife.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.PalmLife.dto.Result;
import com.PalmLife.entity.ShopType;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IShopTypeService extends IService<ShopType> {

    /**
     * 获取商品类型列表
     *
     * @return {@link Result}
     */
    Result getTypeList();
}
