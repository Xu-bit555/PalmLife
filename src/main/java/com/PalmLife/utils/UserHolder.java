package com.PalmLife.utils;

import com.PalmLife.dto.UserDTO;

/**
 * ThreadLocal操作用户信息

 */
public class UserHolder {
    //创建UserDTO类型的ThreadLocal对象
    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();

    /**
     * 保存用户信息
     *
     */
    public static void saveUser(UserDTO user){
        tl.set(user);
    }


    /**
     * 获取用户信息
     * @return
     */
    public static UserDTO getUser(){
        return tl.get();
    }

    /**
     * 删除当前用户
     */
    public static void removeUser(){
        tl.remove();
    }
}
