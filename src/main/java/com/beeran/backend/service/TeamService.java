package com.beeran.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.beeran.backend.model.domain.Team;
import com.beeran.backend.model.domain.User;

/**
 *
 */
public interface TeamService extends IService<Team> {

    /**
     * 已登录用户创建队伍
     * @param team
     * @param loginUser
     * @return
     */
    long addTeam(Team team, User loginUser);

}
