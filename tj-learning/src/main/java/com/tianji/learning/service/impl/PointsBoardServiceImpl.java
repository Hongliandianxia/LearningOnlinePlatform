package com.tianji.learning.service.impl;

import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.constants.TableNameConstants;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardItemVO;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.mapper.PointsBoardMapper;
import com.tianji.learning.service.IPointsBoardService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 学霸天梯榜 服务实现类
 * </p>
 *
 * @author hazard
 * @since 2025-07-23
 * @description 积分榜单服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointsBoardServiceImpl extends ServiceImpl<PointsBoardMapper, PointsBoard> implements IPointsBoardService {

    private final StringRedisTemplate stringRedisTemplate;

    private final UserClient userClient;

    /**
     *
     * 查询积分排行榜,可查当前赛季和历史赛季
     * @param query
     * @return
     */
    @Override
    public PointsBoardVO queryPointsBoard(PointsBoardQuery query) {
        //1.判断是否查询当前赛季,当前赛季实时榜单在redis中查询，历史赛季在mysql中查询
        Long season = query.getSeason();
        boolean isCurrent = season == null || season==0;
        //2.获取key
        LocalDateTime now = LocalDateTime.now();
        String key = RedisConstants.POINTS_RECORD_PREFIX+now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        //3.查询我的积分排名情况
        PointsBoard board=isCurrent?
                queryCurrentSeasonBoard(key):
                queryHistorySeasonBoard(season);

        //查询当前排行榜、历史榜单总体信息
        List<PointsBoard> list=isCurrent?
                queryCurrentSeasonList(key,query.getPageNo(),query.getPageSize()):
                queryHistorySeasonList(query);

        //3.封装返回
        PointsBoardVO vo=new PointsBoardVO();
        //构建我的积分和排名
        if (board!=null){
            log.info("查询到我的积分和排名");
            vo.setPoints(board.getPoints());
            vo.setRank(board.getRank());

        }
        if (CollUtils.isEmpty(list)){
            return vo;
        }
        //查询用户信息
        Set<Long> uIds = list.stream().map(PointsBoard::getUserId).collect(Collectors.toSet());
        List<UserDTO> users = userClient.queryUserByIds(uIds);
        Map<Long, String> userMap = new HashMap<>(uIds.size());
        //避免user为空时处理数据异常
        if (CollUtils.isNotEmpty(users)){
            userMap=users.stream().collect(Collectors.toMap(UserDTO::getId, UserDTO::getName));
        }
        //构建vo所需的List<PointsBoardItemVO>
        List<PointsBoardItemVO> items = new ArrayList<>(list.size());
        for (PointsBoard p : list) {
            PointsBoardItemVO v =new PointsBoardItemVO();
            v.setName(userMap.get(p.getUserId()));
            v.setPoints(p.getPoints());
            v.setRank(p.getRank());
            items.add(v);
        }
        vo.setBoardList(items);
        return vo;
    }


    //查询当前赛季
    private PointsBoard queryCurrentSeasonBoard(String key) {
        //1.绑定key
        BoundZSetOperations<String, String> ops = stringRedisTemplate.boundZSetOps(key);
        //2.查询
        String userId = UserContext.getUser().toString();
        Double points = ops.score(userId);
        Long rank = ops.reverseRank(userId);
        //3.封装返回
        PointsBoard pointsBoard = new PointsBoard();
        pointsBoard.setPoints(points==null ? 0 : points.intValue());
        pointsBoard.setRank(rank==null ? 0:rank.intValue()+1);
        return pointsBoard;
    }

    //查询历史榜单中我的积分和排名
    private PointsBoard queryHistorySeasonBoard(Long season) {
        return null;
    }

    @Override
    //查询当前赛季的积分排行榜
    public List<PointsBoard> queryCurrentSeasonList(String key, Integer pageNo, Integer pageSize) {
        int from = (pageNo - 1) * pageSize;
        int to = from + pageSize - 1;
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, from, to);
        //数据转换
        int rank=from+1;
        if (CollUtils.isEmpty(tuples)) {
            return CollUtils.emptyList();
        }
        List<PointsBoard> list = new ArrayList<>(tuples.size());
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String userId = tuple.getValue();
            Double points = tuple.getScore();
            if (userId == null || points == null){
                continue;
            }
            //构建积分排行榜,直接使用lombok的@Builder生成builder方法构建
  /*          PointsBoard board = PointsBoard.builder()
                    .userId(Long.valueOf(userId))
                    .points(points.intValue())
                    .rank(rank++)
                    .build();*/

            PointsBoard board =new PointsBoard();
            board.setUserId(Long.valueOf(userId));
            board.setPoints(points.intValue());
            board.setRank(rank++);
            list.add(board);
        }
        return list;
    }

    //查询历史榜单排名信息
    private List<PointsBoard> queryHistorySeasonList(PointsBoardQuery query) {
        return null;
    }

    /**
     *创建赛季积分榜单表
     * @param season 赛季
     */
    @Override
    public void createPointsBoardTableBySeason(Integer season) {
        getBaseMapper().createPointsBoardTable(TableNameConstants.POINTS_BOARD_TABLE_PREFIX +season);
    }
}
