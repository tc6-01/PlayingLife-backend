package com.beeran.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.beeran.backend.model.domain.UserTeam;
import com.beeran.backend.service.UserTeamService;
import com.beeran.backend.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

/**
 *
 */
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService{

}




