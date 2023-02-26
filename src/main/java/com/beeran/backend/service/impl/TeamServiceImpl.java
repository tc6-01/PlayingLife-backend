package com.beeran.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.beeran.backend.common.ErrorCode;
import com.beeran.backend.exception.BusisnessException;
import com.beeran.backend.mapper.TeamMapper;
import com.beeran.backend.model.domain.Team;
import com.beeran.backend.model.domain.User;
import com.beeran.backend.model.domain.UserTeam;
import com.beeran.backend.model.enums.TeamStatusEnums;
import com.beeran.backend.service.TeamService;
import com.beeran.backend.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Optional;

/**
 *
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService{

    @Resource
    private UserTeamService userTeamService;
    @Override
    // 添加事务
    @Transactional(rollbackFor = Exception.class)
    public long addTeam(Team team, User loginUser) {
        //请求参数是否为空
        if (team == null) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR);
        }
        // 用户是否登录
        if (loginUser == null) {
            throw new BusisnessException(ErrorCode.NO_LOGIN,"未登录，无法创建队伍");
        }
        final Long userID = loginUser.getId();
        /**
         * 校验队伍信息
         * 1. 队伍人数 > 1 且 <= 20
         * 2. 队伍标题 <= 20
         * 3. 描述 <= 512
         * 4. status 是否公开（int）不传默认为 0（公开）
         * 5. 如果 status 是加密状态，一定要有密码，且密码 <= 32
         * 6. 超时时间 > 当前时间
         * 7. 校验用户最多创建 5 个队伍
         */
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if (maxNum < 1 || maxNum > 20 ){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR,"队伍人数不满足要求");
        }
        String name = team.getName();
        if (StringUtils.isBlank(name) || name.length() > 20){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR,"队伍标题不符合要求");
        }
        String description = team.getDescription();
        if (StringUtils.isBlank(description) || description.length() > 512){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR,"队伍描述不符合要求");
        }
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnums enumsByValue = TeamStatusEnums.getEnumsByValue(status);
        if (enumsByValue == null) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR,"队伍状态出错");
        }
        // 加密状态需要密码
        String password = team.getPassword();
        if( TeamStatusEnums.CRYPTO.equals(enumsByValue)){
            if (StringUtils.isBlank(password) || password.length() < 32) {
                throw new BusisnessException(ErrorCode.PARAMS_ERROR,"密码设置错误");
            }
        }
        Date expireTime = team.getExpireTime();
        if (new Date().after(expireTime)){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR,"队伍创建时间大于过期时间");
        }
        // TODO 设置锁---防止多机部署出现疯狂点击，创建出多个队伍
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId",userID);
        long count = this.count(queryWrapper);
        if (count >= 5){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR,"已创建队伍过多，无法创建");
        }
        team.setUserId(null);
        boolean result = this.save(team);
        Long teamId = team.getId();
        if (!result || teamId < 0) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR,"创建队伍失败");
        }
        UserTeam userTeam = new UserTeam();
        userTeam.setTeamId(team.getId());
        userTeam.setUserId(userID);
        userTeam.setJoinTime(new Date());
        boolean save = userTeamService.save(userTeam);
        if (!save){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR,"插入用户队伍关系表失败");
        }
        return teamId;
    }
}




