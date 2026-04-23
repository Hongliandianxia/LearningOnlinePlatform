package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.vo.LearningLessonStatusVO;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;

import java.util.List;

/**
 * <p>
 * 学生课程表 服务类
 * </p>
 *
 * @author hazard
 * @since 2025-07-09
 */
public interface ILearningLessonService extends IService<LearningLesson> {

    //添加课程
    void addUserLesson(Long userId, List<Long> courseIds);


    PageDTO<LearningLessonVO> pageQuery(PageQuery pageQuery);

    LearningLessonVO queryMyCurrentLesson();

    void deleteUserLesson(Long userId, List<Long> courseIds);

    Long isLessonValid(Long courseId);

    LearningLessonStatusVO statusInfo(Long courseId);

    Integer countLearningLessonByCourse(Long courseId);

    LearningLesson queryByCourseIdAndUserId(Long courseId, Long userId);

    void createPlan(LearningPlanDTO planDTO);

    LearningPlanPageVO queryMyPlans(PageQuery query);


}
