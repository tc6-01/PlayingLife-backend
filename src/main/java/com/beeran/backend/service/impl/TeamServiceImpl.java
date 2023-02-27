package com.beeran.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.beeran.backend.common.ErrorCode;
import com.beeran.backend.exception.BusisnessException;
import com.beeran.backend.mapper.TeamMapper;
import com.beeran.backend.model.domain.Team;
import com.beeran.backend.model.domain.User;
import com.beeran.backend.model.domain.UserTeam;
import com.beeran.backend.model.dto.TeamQuery;
import com.beeran.backend.model.enums.TeamStatusEnums;
import com.beeran.backend.model.request.TeamJoinRequest;
import com.beeran.backend.model.request.TeamUpdateRequest;
import com.beeran.backend.model.vo.TeamUserVO;
import com.beeran.backend.model.vo.UserVO;
import com.beeran.backend.service.TeamService;
import com.beeran.backend.service.UserService;
import com.beeran.backend.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 *
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

    @Resource
    private UserTeamService userTeamService;
    @Resource
    private UserService userService;

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
            throw new BusisnessException(ErrorCode.NO_LOGIN, "未登录，无法创建队伍");
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
        if (maxNum < 1 || maxNum > 20) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR, "队伍人数不满足要求");
        }
        String name = team.getName();
        if (StringUtils.isBlank(name) || name.length() > 20) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR, "队伍标题不符合要求");
        }
        String description = team.getDescription();
        if (StringUtils.isBlank(description) || description.length() > 512) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR, "队伍描述不符合要求");
        }
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnums enumsByValue = TeamStatusEnums.getEnumsByValue(status);
        if (enumsByValue == null) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR, "队伍状态出错");
        }
        // 加密状态需要密码
        String password = team.getPassword();
        if (TeamStatusEnums.CRYPTO.equals(enumsByValue)) {
            if (StringUtils.isBlank(password) || password.length() < 32) {
                throw new BusisnessException(ErrorCode.PARAMS_ERROR, "密码设置错误");
            }
        }
        Date expireTime = team.getExpireTime();
        if (expireTime != null && new Date().after(expireTime)) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR, "队伍创建时间大于过期时间");
        }
        // TODO 设置锁---防止多机部署出现疯狂点击，创建出多个队伍
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userID);
        long count = this.count(queryWrapper);
        if (count >= 5) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR, "已创建队伍过多，无法创建");
        }
        team.setId(null);
        team.setUserId(userID);
        boolean result = this.save(team);
        Long teamId = team.getId();
        if (!result || teamId < 0) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        UserTeam userTeam = new UserTeam();
        userTeam.setTeamId(team.getId());
        userTeam.setUserId(userID);
        userTeam.setJoinTime(new Date());
        boolean save = userTeamService.save(userTeam);
        if (!save) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR, "插入用户队伍关系表失败");
        }
        return teamId;
    }

    @Override
    public List<TeamUserVO> ListTeams(TeamQuery teamQuery, boolean isAdmin) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        // 组合查询条件
        if (queryWrapper != null) {
            Long id = teamQuery.getId();
            if (id != null && id > 0) {
                queryWrapper.eq("id", id);
            }
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                queryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like("name", name);
            }
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }
            Integer maxNum = teamQuery.getMaxNum();
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("maxNum", maxNum);
            }
            Long userId = teamQuery.getUserId();
            if (userId != null && userId > 0) {
                queryWrapper.eq("userId", userId);
            }
            // 获取查询队伍的状态属性
            Integer status = teamQuery.getStatus();
            TeamStatusEnums teamStatusEnums = TeamStatusEnums.getEnumsByValue(status);
            if (teamStatusEnums == null) {
                teamStatusEnums = TeamStatusEnums.PUBLIC;
            }
            if (!isAdmin && !teamStatusEnums.equals(TeamStatusEnums.PUBLIC)) {
                throw new BusisnessException(ErrorCode.NO_AUTH);
            }
            queryWrapper.eq("status", teamStatusEnums.getValue());
        }
        // 不展示过期队伍
        queryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
        List<Team> list = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        // 关联查询用户信息
        // 1. 自己写 SQL
        // 查询队伍和创建人的信息
        // select * from team t left join user u on t.userId = u.id
        // 查询队伍和已加入队伍成员的信息
        // select *
        // from team t
        //         left join user_team ut on t.id = ut.teamId
        //         left join user u on ut.userId = u.id;


        List<TeamUserVO> teamUserVOList = new ArrayList<>();
        for (Team team : list) {
            Long userId = team.getUserId();
            if (userId == null) {
                return null;
            }
            User user = userService.getById(userId);
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO);
            if (user != null) {
                UserVO userVO = new UserVO();

                BeanUtils.copyProperties(user, userVO);
                teamUserVO.setCreateUser(userVO);
            }
            teamUserVOList.add(teamUserVO);
        }
        return teamUserVOList;
    }

    @Override

    public boolean updateTeam(TeamUpdateRequest teamUpdate, User loginUser) {

        if (teamUpdate == null) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = teamUpdate.getId();
        if (id == null || id < 0) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR);
        }
        Team oldTeam = this.getById(id);
        if (oldTeam == null) {
            throw new BusisnessException(ErrorCode.NULL_ERROR, "更新队伍不存在");
        }
        // 判断当前已登录用户是否为管理员或者创建用户

        if (oldTeam.getUserId() != loginUser.getId() && loginUser.getRole() != 1){
            throw new BusisnessException(ErrorCode.NO_AUTH,"只有管理员和创建用户可修改");
        }
        // TODO 确保状态与密码保持一致性
        // 如果公开状态，则清除密码； 如果为私有状态，密码必须添加
        TeamStatusEnums enumsByValue = TeamStatusEnums.getEnumsByValue(teamUpdate.getStatus());
        String updatePassword = teamUpdate.getPassword();
        if (!enumsByValue.equals(TeamStatusEnums.CRYPTO) && StringUtils.isNotBlank(updatePassword)){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR,"公开和私有状态下不允许设置密码");
        }
        if (enumsByValue.equals(TeamStatusEnums.CRYPTO)&& StringUtils.isBlank(updatePassword)){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR,"设置私有状态应该设置密码");
        }

        Team team = new Team();
        BeanUtils.copyProperties(teamUpdate, team);
        this.updateById(team);
        return true;
    }

    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        if (teamJoinRequest == null) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamJoinRequest.getTeamId();
        if (teamId == null || teamId < 0){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR,"队伍不存在");
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusisnessException(ErrorCode.NULL_ERROR,"加入队伍不存在");
        }
        if (team.getExpireTime() != null && new Date().after(team.getExpireTime())){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR,"加入队伍已过期不可加入");
        }
        // 用户加入队伍数量
        Long userId = loginUser.getId();
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userID", userId);
        long hasTeamNum = userTeamService.count(queryWrapper);
        if (hasTeamNum > 5){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR,"只能创建或加入5个队伍");
        }
        // 加入队伍已满
        QueryWrapper<UserTeam> wrapper = new QueryWrapper<>();
        wrapper.eq("teamId", teamId);
        long teamMemCount = userTeamService.count(wrapper);
        if (teamMemCount >= team.getMaxNum()){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR,"当前队伍人数已到达最大人数，不可加入");
        }
        // 不能重复加入已加入队伍
        wrapper.eq("userId", userId);
        long alreadyJoin = userTeamService.count(wrapper);
        if (alreadyJoin >= 1){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR,"您已经加入队伍，不用重复加入");
        }
        Integer status = team.getStatus();
        TeamStatusEnums enumsByValue = TeamStatusEnums.getEnumsByValue(status);
        if (enumsByValue.equals(TeamStatusEnums.PRIVATE) ){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR,"不能加入私有队伍");
        }
        String joinRequestPassword = teamJoinRequest.getPassword();
        if (enumsByValue.equals(TeamStatusEnums.CRYPTO)){
            if (StringUtils.isBlank(joinRequestPassword) || !joinRequestPassword.equals(team.getPassword())) {
                throw new BusisnessException(ErrorCode.PARAMS_ERROR,"密码不匹配");
            }
        }
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        return userTeamService.save(userTeam);

    }
}




