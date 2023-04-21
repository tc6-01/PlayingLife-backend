package com.beeran.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.beeran.backend.model.domain.Team;
import com.beeran.backend.model.domain.User;
import com.beeran.backend.model.dto.TeamQuery;
import com.beeran.backend.model.request.TeamJoinRequest;
import com.beeran.backend.model.request.TeamUpdateRequest;
import com.beeran.backend.model.vo.TeamUserVO;
import com.beeran.backend.model.vo.TeamVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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

    /**
     * 查询队伍
     *
     * @param teamQuery
     * @return
     */
    List<TeamUserVO> ListTeams(TeamQuery teamQuery, boolean isAdmin);

    /**
     * 更新队伍
     * @param teamUpdate
     * @param loginUser
     * @return
     */
    boolean updateTeam(TeamUpdateRequest teamUpdate, User loginUser);

    /**
     * 用户加入队伍
     * @param teamJoinRequest
     * @param loginUser
     * @return
     */
    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    /**
     * 用户退出队伍
     * @param teamId
     * @param loginUser
     * @return
     */
    boolean exitTeam( long teamId, User loginUser);

    /**
     * 根据队伍Id查询队伍信息
     * @param id
     * @return TeamVO
     */
    TeamVO getTeamInfoById(long id);
    /**
     * 队长解散队伍
     * @param teamId
     * @param loginUser
     * @return
     */
    boolean delTeam(long teamId, User loginUser);


    List<TeamUserVO> correctsJoinProp(List<TeamUserVO> resultTeams, HttpServletRequest request);
}
