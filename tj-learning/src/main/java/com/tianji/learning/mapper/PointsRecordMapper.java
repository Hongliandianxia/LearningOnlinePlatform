package com.tianji.learning.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.tianji.learning.domain.po.PointsRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 Mapper 接口
 * </p>
 *
 * @author hazard
 * @since 2025-07-23
 */
public interface PointsRecordMapper extends BaseMapper<PointsRecord> {

    //(@Param(Constants.WRAPPER)  import org.apache.ibatis.annotations.Param;必须用这个注解
    //新增积分，利用mp的组合查询,将mp的wrapper查到的数据充当where条件
    @Select("select sum(points) from points_record ${ew.customSqlSegment}")
    Integer queryUserPointsByTypeAndDate(@Param(Constants.WRAPPER) QueryWrapper<PointsRecord> wrapper);


    //查询今日积分详情，也是利用mp组合查询
/*    @Select("SELECT type, SUM(points) AS points FROM points_record ${ew.customSqlSegment} GROUP BY type")
    List<PointsRecord> queryUserPointsByDate(@Param(Constants.WRAPPER) QueryWrapper<PointsRecord> wrapper);*/

    @Select("SELECT type, SUM(points) AS points FROM points_record ${ew.customSqlSegment} GROUP BY type")
    List<PointsRecord> queryUserPointsByDate(@Param(Constants.WRAPPER) QueryWrapper<PointsRecord> wrapper);
}
