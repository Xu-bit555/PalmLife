package com.PalmLife.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Log实体类
 */
@TableName("sys_log")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogEntity implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    @TableId(value = "record_id",type = IdType.AUTO)
    private Long id;

    //用户名
    @TableField("user_name")
    private String userName;

    //操作内容
    private String description;

    //请求方法
    private String method;

    //请求参数
    private String params;

    //执行时长(毫秒)
    @TableField("active_time")
    private String activeTime;

    //IP地址
    private String ip;
    //创建时间
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
