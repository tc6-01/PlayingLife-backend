package com.beeran.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beeran.backend.common.BaseResponse;
import com.beeran.backend.common.ErrorCode;
import com.beeran.backend.common.ResultUtils;
import com.beeran.backend.exception.BusisnessException;
import com.beeran.backend.model.domain.Team;
import com.beeran.backend.model.domain.User;
import com.beeran.backend.model.domain.UserTeam;
import com.beeran.backend.model.dto.TeamQuery;
import com.beeran.backend.model.request.TeamAddRequest;
import com.beeran.backend.model.request.TeamJoinRequest;
import com.beeran.backend.model.request.TeamUpdateRequest;
import com.beeran.backend.model.vo.TeamUserVO;
import com.beeran.backend.model.vo.TeamVO;
import com.beeran.backend.service.TeamService;
import com.beeran.backend.service.UserService;
import com.beeran.backend.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 队伍接口
 * @Author BeerAn
 */
@RestController
@RequestMapping("/team")
@CrossOrigin
@Slf4j
public class TeamController {
    @Resource
    private UserService userService;
    @Resource
    private TeamService teamService;
    @Resource
    private UserTeamService userTeamService;

    /**
     * 添加队伍
     * @param teamAddRequest
     * @param request
     * @return
     */
    @PostMapping("/addTeam")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        if (teamAddRequest == null) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);
        long teamId = teamService.addTeam(team, loginUser);
        return ResultUtils.Success(teamId);
    }

    /**
     * 退出队伍
     * @param teamId
     * @param request
     * @return
     */
    @GetMapping("/exitTeam")
    public BaseResponse<Boolean> exitTeam(long teamId, HttpServletRequest request){
        if (teamId < 0){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean save = teamService.exitTeam(teamId,loginUser);
        if (!save){
            throw new BusisnessException(ErrorCode.SYSTEM_ERROR,"退出失败");
        }
        return ResultUtils.Success(true);
    }

    /**
     * 解散队伍
     * @param teamId
     * @param request
     * @return
     */
    @GetMapping("/deleteTeam")
    public BaseResponse<Boolean> delTeam(long teamId, HttpServletRequest request){
        if (teamId < 0){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.delTeam(teamId,loginUser);
        if (!result){
            throw new BusisnessException(ErrorCode.SYSTEM_ERROR,"解散队伍失败");
        }
        return ResultUtils.Success(true);
    }
    /**
     * 更新队伍信息
     * @param teamUpdate
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Long> updateTeam(@RequestBody TeamUpdateRequest teamUpdate, HttpServletRequest request){
        if (teamUpdate == null){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.updateTeam(teamUpdate, loginUser);
        if (!result){
            throw new BusisnessException(ErrorCode.SYSTEM_ERROR,"更新失败");
        }
        return ResultUtils.Success(true);
    }

    /**
     * 用户加入队伍
     * @param teamJoinRequest
     * @param request
     * @return
     */
    @PostMapping("/join")
    public BaseResponse<Long> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if (teamJoinRequest == null) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.joinTeam(teamJoinRequest, loginUser);
        if (!result){
            throw new BusisnessException(ErrorCode.SYSTEM_ERROR,"更新失败");
        }
        return ResultUtils.Success(true);
    }
    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(long id){
        if (id < 0){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR);
        }
        TeamVO team = teamService.getTeamInfoById(id);
        if (team == null){
            throw new BusisnessException(ErrorCode.NULL_ERROR,"查询失败");
        }
        return ResultUtils.Success(team);
    }

    /**
     * 获取所有用户信息
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("/list")
    public BaseResponse<TeamUserVO> getTeams(TeamQuery teamQuery, HttpServletRequest request) {

        if (teamQuery == null){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR);
        }
        boolean isAdmin = userService.isAdmin(request);
        List<TeamUserVO> resultTeams = teamService.ListTeams(teamQuery, isAdmin);
        if (resultTeams == null){
            throw new BusisnessException(ErrorCode.NULL_ERROR,"查询失败");
        }
        // 判断当前用户是否已经加入展示的队伍
        // 获取展示队伍的teamID ，查询team_user关系表中的userID和TeamID对应
        resultTeams = teamService.correctsJoinProp(resultTeams, request);

        return ResultUtils.Success(resultTeams);
    }
    /**
     * 获取当前用户加入队伍
     * @param request
     * @return
     */
    @GetMapping("/listJoin")
    public BaseResponse<TeamUserVO> getJoinTeams(TeamQuery teamQuery, HttpServletRequest request) {

        if (teamQuery == null){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId());
        List<UserTeam> list = userTeamService.list(queryWrapper);
        List<Long> idList = new ArrayList<>(list.stream().collect(Collectors.groupingBy(UserTeam::getTeamId)).keySet());
        teamQuery.setIdList(idList);
        List<TeamUserVO> resultTeams = teamService.ListTeams(teamQuery, true);
        if (resultTeams == null){
            throw new BusisnessException(ErrorCode.NULL_ERROR,"查询失败");
        }
        resultTeams = teamService.correctsJoinProp(resultTeams, request);

        return ResultUtils.Success(resultTeams);
    }
    /**
     * 获取当前用户创建队伍
     * @param request
     * @return
     */
    @GetMapping("/listCreate")
    public BaseResponse<TeamUserVO> getCreateTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        teamQuery.setUserId(loginUser.getId());
        List<TeamUserVO> resultTeams = teamService.ListTeams(teamQuery, true);
        if (resultTeams == null){
            throw new BusisnessException(ErrorCode.NULL_ERROR,"查询失败");
        }
        resultTeams = teamService.correctsJoinProp(resultTeams, request);

        return ResultUtils.Success(resultTeams);
    }

    @GetMapping("/list/page")
    public BaseResponse<Team> getTeamsPage(TeamQuery teamQuery) {

        if (teamQuery == null){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        Page<Team> page = new Page<>(teamQuery.getPageSize(), teamQuery.getPageNum());
        Page<Team> resultPage = teamService.page(page, queryWrapper);
        if (resultPage == null){
            throw new BusisnessException(ErrorCode.NULL_ERROR,"查询失败");
        }
        return ResultUtils.Success(resultPage);
    }
}
