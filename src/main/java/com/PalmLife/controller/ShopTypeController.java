package com.PalmLife.controller;


import com.PalmLife.dto.Result;
import com.PalmLife.service.IShopTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    /**
     * 获取商品类型列表
     *
     * @return {@link Result}
     */
    @GetMapping("list")
    public Result queryTypeList() {
        return typeService.getTypeList();
    }
}
