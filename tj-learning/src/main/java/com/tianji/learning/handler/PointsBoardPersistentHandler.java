package com.tianji.learning.handler;


import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.constants.TableNameConstants;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.tianji.learning.service.IPointsBoardService;
import com.tianji.learning.utils.TableInfoContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author hazard
 * @version 1.0
 * @description 定时任务，每月月初进行历史榜单持久化处理
 * @date 2025/7/27 21:04
 */
@Component
@RequiredArgsConstructor
public class PointsBoardPersistentHandler {
    private final StringRedisTemplate redisTemplate;

    private final IPointsBoardSeasonService seasonService;

    private final IPointsBoardService pointsBoardService;

    //cron表达式(秒 分 时 日 月 周) ?不指定
    //learning微服务可能部署多个，如何保证定时任务只创建一次（任务调度、编排）
//    @Scheduled(cron ="0 0 3 1 * ?")
    @XxlJob("createTableJob")
    public void createPointsBoardTableOfLastSeason(){
        //1.获取上个月时间
        LocalDateTime time = LocalDateTime.now().minusMonths(1);
        //2.查询赛季id
        Integer season = seasonService.querySeasonByTime(time);
        if(season == null){
            //赛季不存在
            return;
        }
        //3.创建表
        pointsBoardService.createPointsBoardTableBySeason(season);
    }
    @XxlJob("savePointsBoard2DB")
    public void savePointsBoard2Mysql(){
        //1.获取上个月时间
        LocalDateTime time = LocalDateTime.now().minusMonths(1);
        //2.计算动态表名
        //2.1查询赛季信息
        Integer season = seasonService.querySeasonByTime(time);
        //2.2存入ThreadLocal
        TableInfoContext.setInfo(TableNameConstants.POINTS_BOARD_TABLE_PREFIX +season);

        //2.查询榜单数据
        //2.1拼接key
        String key = RedisConstants.POINTS_RECORD_PREFIX +time.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        //2.2查询数据
        //xxl_job数据分页
        int index = XxlJobHelper.getShardIndex();
        int total = XxlJobHelper.getShardTotal();
        int pageNo = index +1;
        int pageSize = 500;
        while (true) {
            List<PointsBoard> boardList = pointsBoardService.queryCurrentSeasonList(key, pageNo, pageSize);
            if (CollUtils.isEmpty(boardList)) {
                break;
            }
            //4.持久化到数据库
            //4.1将rank写入id,用id代替排名rank
            boardList.forEach(b->{
                b.setId(b.getRank().longValue());
                b.setRank(null);
            });
            //4.2持久化
             pointsBoardService.saveBatch(boardList);
            //5.翻页,分片跨度
            pageNo+=total;
            TableInfoContext.remove();
        }
    }
    @XxlJob("clearPointsBoardFromRedis")
    public void clearPointsBoardFromRedis() {
        //不能直接删除，因为redis中排行榜为Big Key，采用unlink命令，异步操作删除指定key
        //1.获取上个月时间
        LocalDateTime time = LocalDateTime.now().minusMonths(1);
        //2.拼接key
        String key = RedisConstants.POINTS_RECORD_PREFIX +time.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        //3.删除,通用命令
        redisTemplate.unlink(key);
    }
}
