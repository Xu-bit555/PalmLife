package com.PalmLife.controller;


import cn.hutool.core.bean.BeanUtil;
import com.PalmLife.dto.LoginFormDTO;
import com.PalmLife.dto.Result;
import com.PalmLife.dto.UserDTO;
import com.PalmLife.entity.User;
import com.PalmLife.entity.UserInfo;
import com.PalmLife.service.IUserInfoService;
import com.PalmLife.service.IUserService;
import com.PalmLife.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        // 发送短信验证码并保存验证码
        return userService.sendCode(phone,session);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){
        // 实现登录功能
        return userService.login(loginForm,session);
    }

    /**
     * 签到
     * @return
     */
    @PostMapping("/sign")
    public Result sign(){
        return userService.sign();
    }

    /**
     * 获取签到次数
     * @return
     */
    @GetMapping("/sign/count")
    public Result signCount(){
        return userService.signCount();
    }



    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(){
        return Result.ok(userService.logout());
    }


    /**
     * 获取当前登录的用户
     * @return
     */
    @GetMapping("/me")
    public Result me(){
        //  获取当前登录的用户并返回
        return Result.ok(UserHolder.getUser());
    }


    /**
     * 根据 id 查询个人见解
     * @param userId
     * @return
     */
    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }



    /**
     * 根据用户id查询用户信息
     * @param userId
     * @return
     */
    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id")Long userId){
        User user = userService.getById(userId);
        if (user==null){
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        return Result.ok(userDTO);
    }
}
