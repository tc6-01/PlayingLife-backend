package com.beeran.backend.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求体
 *
 * @Author BeerAn
 */
@Data
public class TeamJoinRequest implements Serializable {


    /**
     * 队伍ID
     */
    private Long teamId;
    /**
     * 密码
     */
    private String password;


    private static final long serialVersionUID = 1L;

}
