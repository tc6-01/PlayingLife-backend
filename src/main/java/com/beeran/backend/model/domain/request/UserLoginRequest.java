package com.beeran.backend.model.domain.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求体
 *
 * @Author BeerAn
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 3345678765456787654L;

    private String userAccount;

    private String password;

}
