package com.beeran.backend.model.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class UserVO implements Serializable {
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
     * 学号
     */
    private String planetCode;

    /**
     * 用户json标签
     */
    private String tags;
    /**
     * 用户简介
     */
    private String profile;

}
