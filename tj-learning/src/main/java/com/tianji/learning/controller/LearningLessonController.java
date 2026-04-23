package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.vo.LearningLessonStatusVO;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.service.ILearningLessonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>
 * 学生课程表 前端控制器
 * </p>
 *
 * @author hazard
 * @since 2025-07-09
 */
@Slf4j
@Api(tags = "我的课表接口")
@RequiredArgsConstructor
@RestController
@RequestMapping("/lessons")
public class LearningLessonController {

    private final ILearningLessonService learningLessonService;


    @ApiOperation("分页查询我的课表")
    @GetMapping("/page")
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery pageQuery){

        log.info("分页查询我的课表");
        return learningLessonService.pageQuery(pageQuery);
    }

    @GetMapping("/now")
    @ApiOperation("查询我正在学习的课程")
    public LearningLessonVO queryMyCurrentLesson() {
        log.info("查询我正在学习的课程");
        return learningLessonService.queryMyCurrentLesson();
    }

    /**
     * 删除课程
     *
     * @param courseId 课程ID
     */
    @DeleteMapping("/{courseId}")
    @ApiOperation("删除课程")
    public void delete(@PathVariable("courseId") Long courseId) {
        log.info("删除课程：{}", courseId);
        learningLessonService.removeById(courseId);
    }

    /**
     * 验证课程是在有效期，在的话返回课程ID，反之返回 Null
     *
     * @param courseId 课程 ID
     * @return 课程ID
     */
    @GetMapping("/{courseId}/valid")
    @ApiOperation("验证课程是否在有效期")
    public Long isLessonValid(@PathVariable("courseId") Long courseId) {
        return learningLessonService.isLessonValid(courseId);
    }

    /**
     * 查询课表信息，获取相关课表信息
     *
     * @param courseId 课程ID
     * @return 课程状态信息
     */
    @GetMapping("/{courseId}")
    @ApiOperation("根据ID查询课程状态信息")
    public LearningLessonStatusVO statusInfo(@PathVariable("courseId") Long courseId) {
        return learningLessonService.statusInfo(courseId);
    }

    /**
     * 根据课程ID，统计报名人数
     *
     * @param courseId 课程ID
     * @return 报名人数
     */
    @GetMapping("/{courseId}/count")
    @ApiOperation(" 根据课程ID，统计报名人数")
    public Integer countLearningLessonByCourse(@PathVariable("courseId") Long courseId) {
        return learningLessonService.countLearningLessonByCourse(courseId);
    }


    /**
     * 创建学习计划
     * @param planDTO 学习计划
     */
    @ApiOperation("创建学习计划")
    @PostMapping("/plans")
    public void createPlan(@Valid @RequestBody LearningPlanDTO planDTO) {
        learningLessonService.createPlan(planDTO);
    }

    @ApiOperation("分页查询我的学习计划")
    @GetMapping("/plans")
    public LearningPlanPageVO queryMyPlans(PageQuery query){
        log.info("queryMyPlans query: {}", query);
        return learningLessonService.queryMyPlans(query);
    }



}
