package com.beeran.backend.model.domain;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * table
 * @TableName user
 */
@TableName(value ="user")
@Data
public class User implements Serializable {
    /**
     * id

     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户昵称
     */
    private String username;

    /**
     * 账号
     */
    private String _account;

    /**
     * 头像地址
     */
    private String avatar_url;

    /**
     * 性别
     */
    private Byte gender;

    /**
     * 密码
     */
    private String _password;

    /**
     * 电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 创建时间
     */
    private Date create_time;

    /**
     * 更新时间
     */
    private Date update_time;

    /**
     * 是否有效
     */
    private Integer _status;

    /**
     * 用户权限
     * 0 -普通用户
     * 1 -管理员
     */
    private Integer role;


    /**
     * 是否删除
     */
    @TableLogic
    private Byte is_delete;

    /**
     * 星球编号
     */
    private String planetCode;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}