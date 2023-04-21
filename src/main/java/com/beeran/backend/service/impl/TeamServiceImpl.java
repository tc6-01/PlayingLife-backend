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
import com.beeran.backend.model.vo.TeamVO;
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
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

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
            if (StringUtils.isBlank(password) || password.length() > 32) {
                throw new BusisnessException(ErrorCode.PARAMS_ERROR, "密码设置错误");
            }
        }
        Date expireTime = team.getExpireTime();
        if (expireTime != null && new Date().after(expireTime)) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR, "队伍创建时间大于过期时间");
        }
        // TODO 设置锁---防止多机部署出现疯狂点击，创建出多个队伍
        synchronized (this) {
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
            List<Long> idList = teamQuery.getIdList();
            if (idList != null && idList.size() != 0){
                queryWrapper.in("id", idList);
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
            if (teamStatusEnums != null) {
//                if (!isAdmin && !teamStatusEnums.equals(TeamStatusEnums.PUBLIC)) {
//                    throw new BusisnessException(ErrorCode.NO_AUTH,"非管理员无法查看私有队伍");
//                }
                queryWrapper.eq("status", teamStatusEnums.getValue());
            }


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
            // 设置当前队伍已加入人数
           // 查询关系中记录数目，其中
            QueryWrapper<UserTeam> query =new QueryWrapper<>();
            query.eq("teamId", team.getId());
            long count = userTeamService.count(query);
            teamUserVO.setHasJoinNum(count);
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

        if ( !oldTeam.getUserId().equals(loginUser.getId()) && loginUser.getRole() != 1) {
            throw new BusisnessException(ErrorCode.NO_AUTH, "只有管理员和创建用户可修改");
        }
        // 如果公开状态，则清除密码； 如果为私有状态，密码必须添加
        TeamStatusEnums enumsByValue = TeamStatusEnums.getEnumsByValue(teamUpdate.getStatus());
        String updatePassword = teamUpdate.getPassword();
        if (!enumsByValue.equals(TeamStatusEnums.CRYPTO) && StringUtils.isNotBlank(updatePassword)) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR, "公开和私有状态下不允许设置密码");
        }
        if (enumsByValue.equals(TeamStatusEnums.CRYPTO) && StringUtils.isBlank(updatePassword)) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR, "设置私有状态应该设置密码");
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
        if (teamId == null || teamId < 0) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusisnessException(ErrorCode.NULL_ERROR, "查找队伍不存在");
        }
        if (team.getExpireTime() != null && new Date().after(team.getExpireTime())) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR, "加入队伍已过期不可加入");
        }
        // 用户加入队伍数量
        Long userId = loginUser.getId();
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userID", userId);
        long hasTeamNum = userTeamService.count(queryWrapper);
        if (hasTeamNum > 5) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR, "只能创建或加入5个队伍");
        }
        long teamMemCount = this.countTeamUsers(teamId);
        if (teamMemCount >= team.getMaxNum()) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR, "当前队伍人数已到达最大人数，不可加入");
        }

        Integer status = team.getStatus();
        String joinRequestPassword = teamJoinRequest.getPassword();
        TeamStatusEnums enumsByValue = TeamStatusEnums.getEnumsByValue(status);
        if (enumsByValue.equals(TeamStatusEnums.PRIVATE)) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR, "不能加入私有队伍");
        } else if (enumsByValue.equals(TeamStatusEnums.CRYPTO)) {
            if (StringUtils.isBlank(joinRequestPassword) || !joinRequestPassword.equals(team.getPassword())) {
                throw new BusisnessException(ErrorCode.PARAMS_ERROR, "密码不匹配");
            }
        } else {
            if (StringUtils.isNotBlank(joinRequestPassword)) {
                throw new BusisnessException(ErrorCode.PARAMS_ERROR, "加入公开队伍不需要使用密码");
            }
        }
        // 不能重复加入已加入队伍
        QueryWrapper<UserTeam> wrapper = new QueryWrapper<>();
        wrapper.eq("teamId", teamId);
        wrapper.eq("userId", userId);
        long alreadyJoin = userTeamService.count(wrapper);
        if (alreadyJoin >= 1) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR, "您已经加入队伍，不用重复加入");
        }
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        return userTeamService.save(userTeam);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean exitTeam(long teamId, User loginUser) {
        if (teamId < 0 || loginUser == null) {
            throw new BusisnessException(ErrorCode.NULL_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusisnessException(ErrorCode.NULL_ERROR, "未查询到该队伍");
        }
        Long userId = loginUser.getId();
        QueryWrapper<UserTeam> UserInTeamQueryWrapper = new QueryWrapper<>();
        UserInTeamQueryWrapper.eq("userID", userId);
        UserInTeamQueryWrapper.eq("teamID", teamId);
        long userInTeam = userTeamService.count(UserInTeamQueryWrapper);
        if (userInTeam < 1) {
            throw new BusisnessException(ErrorCode.NULL_ERROR, "用户未加入队伍，不可退出");
        }
        long teamMember = this.countTeamUsers(teamId);
        if (teamMember == 1) {
            // delete team
            this.removeById(teamId);
            // delete user-team relation
        } else {
            // 当前用户是否为队长
            if (team.getUserId().equals(userId)) {
                // 查询最早加入的两条用户
                QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                userTeamQueryWrapper.eq("teamId", teamId);
                userTeamQueryWrapper.last("order by id asc limit 2");
                List<UserTeam> list = userTeamService.list(userTeamQueryWrapper);
                if (CollectionUtils.isEmpty(list) || list.size() <= 1) {
                    throw new BusisnessException(ErrorCode.SYSTEM_ERROR, "获取最新插入用户异常");
                }
                UserTeam nextJoinUser = list.get(0);
                Long nextUserID = nextJoinUser.getUserId();
                // 更新旧的team的创建UserID
                team.setUserId(nextUserID);
                boolean result = this.updateById(team);
                if (!result) {
                    throw new BusisnessException(ErrorCode.SYSTEM_ERROR, "更新队伍队长失败");
                }
            }
        }
        // 移除用户关系
        return userTeamService.remove(UserInTeamQueryWrapper);
    }

    @Override
    public TeamVO getTeamInfoById(long id) {
        // 获取当前队伍
        // 去队伍用户表中查询当前队伍对应加入的用户，并放入返回结果中
        Team team = this.getById(id);
        // 校验队伍是否存在
        Long userId = team.getUserId();
        if (userId == null) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR,"队伍不存在");
        }
        TeamVO teamVO = new TeamVO();
        User createUser = userService.getById(userId);
        //脱敏信息
        if (createUser != null) {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(createUser, userVO);
            teamVO.setCreateUser(userVO);
        }
        BeanUtils.copyProperties(team, teamVO);
        // 加入用户信息
        QueryWrapper<UserTeam> wrapper = new QueryWrapper<>();
        wrapper.eq("teamId", id);
        List<UserTeam> userTeamList = userTeamService.list(wrapper);
        //获取加入队伍的用户id
        List<Long> userIdList = userTeamList.stream().map(UserTeam::getUserId).collect(Collectors.toList());
        //根据用户id查出详细的信息
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id", userIdList);
        List<User> userList = userService.list(userQueryWrapper);
        //用户脱敏
        List<UserVO> userVOList = new ArrayList<>();
        for (User user : userList) {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            userVOList.add(userVO);
        }
        teamVO.setUserJoinList(userVOList);
        teamVO.setHasJoinNum((long)userVOList.size());
        return teamVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delTeam(long teamId, User loginUser) {
        if (teamId < 0 || loginUser == null) {
            throw new BusisnessException(ErrorCode.NULL_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusisnessException(ErrorCode.NULL_ERROR, "未查询到该队伍");
        }
        Long userId = loginUser.getId();
        // judge current user is the caption
        if (userId.equals(team.getUserId())) {
            // 删除所有关系
            QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
            userTeamQueryWrapper.eq("teamId", teamId);
            boolean result = userTeamService.remove(userTeamQueryWrapper);
            if (!result) {
                throw new BusisnessException(ErrorCode.SYSTEM_ERROR, "删除队伍关系出错");
            }
            // 删除当前队伍
            return this.removeById(teamId);
        }
        throw new BusisnessException(ErrorCode.NO_AUTH, "当前用户不是队伍队长，不可解散队伍");
    }

    @Override
    public List<TeamUserVO> correctsJoinProp(List<TeamUserVO> resultTeams, HttpServletRequest request) {
        List<Long> idList = resultTeams.stream().map(TeamUserVO::getId).collect(Collectors.toList());
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        try {
            User loginUser = userService.getLoginUser(request);
            userTeamQueryWrapper.eq("userId", loginUser.getId());
            userTeamQueryWrapper.in("teamId", idList);
            List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
            Set<Long> hasJoinTeamSet = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
            resultTeams.forEach(team ->{
                boolean hasJoin = hasJoinTeamSet.contains(team.getId());
                team.setHasJoin(hasJoin);
            });
        }catch (Exception e){

        }
        return resultTeams;
    }

    /**
     * 查询队伍中用户数目
     *
     * @param teamId
     * @return
     */
    private long countTeamUsers(long teamId) {
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", teamId);
        return userTeamService.count(queryWrapper);
    }
}




