package com.beeran.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beeran.backend.common.BaseResponse;
import com.beeran.backend.common.ErrorCode;
import com.beeran.backend.common.ResultUtils;
import com.beeran.backend.exception.BusisnessException;
import com.beeran.backend.model.domain.Team;
import com.beeran.backend.model.domain.User;
import com.beeran.backend.model.dto.TeamQuery;
import com.beeran.backend.model.request.TeamAddRequest;
import com.beeran.backend.model.request.TeamJoinRequest;
import com.beeran.backend.model.request.TeamUpdateRequest;
import com.beeran.backend.model.vo.TeamUserVO;
import com.beeran.backend.service.TeamService;
import com.beeran.backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;


/**
 * 队伍接口
 *
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

    @PostMapping("/add")
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

    @PostMapping("/delete")
    public BaseResponse<Long> delTeam(@RequestBody long id){
        if (id < 0){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR);
        }
        boolean save = teamService.removeById(id);
        if (!save){
            throw new BusisnessException(ErrorCode.SYSTEM_ERROR,"删除失败");
        }
        return ResultUtils.Success(true);
    }
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
        Team team = teamService.getById(id);
        if (team == null){
            throw new BusisnessException(ErrorCode.NULL_ERROR,"查询失败");
        }
        return ResultUtils.Success(true);
    }
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