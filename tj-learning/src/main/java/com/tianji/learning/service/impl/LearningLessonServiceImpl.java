package com.tianji.learning.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.IdAndNumDTO;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.domain.vo.LearningLessonStatusVO;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.domain.vo.LearningPlanVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.PlanStatus;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 学生课程表 服务实现类
 * </p>
 *
 * @author hazard
 * @since 2025-07-09
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {

    private final CourseClient courseClient;
    private final CatalogueClient catalogueClient;
    private final LearningRecordMapper learningRecordMapper;

    /**
     * 批量新增课程
     * @param userId
     * @param courseIds
     */
    @Override
    @Transactional
    public void addUserLesson(Long userId, List<Long> courseIds) {
        //1.查询课程信息
        List<CourseSimpleInfoDTO> cInfoList = courseClient.getSimpleInfoList(courseIds);

        if (CollUtils.isEmpty(cInfoList)) {
            //2.判断课程信息是否为空
            log.warn("课程不存在：{}", courseIds);
            return;
        }
        //3.循环遍历出来课程数据
        List<LearningLesson> list=new ArrayList<>(cInfoList.size());

        for (CourseSimpleInfoDTO cInfo : cInfoList) {
            LearningLesson lesson=new LearningLesson();
            //获取过期时间, 接受到消息的时间上+月份(有效期)
            Integer validDuration = cInfo.getValidDuration();
            if (validDuration!=null && validDuration>0) {
                LocalDateTime now = LocalDateTime.now();
                lesson.setCreateTime(now);
                lesson.setExpireTime(now.plusMonths(validDuration));
            }
            //填充userId和courseId
            lesson.setUserId(userId);
            lesson.setCourseId(cInfo.getId());
            list.add(lesson);
        }
        //4.批量新增
        saveBatch(list);

    }

    /**
     * 分页查询课程
     * @param pageQuery
     * @return
     */

    @Override
    public PageDTO<LearningLessonVO> pageQuery(PageQuery pageQuery) {
        //1.获取登录用户
        Long userId = UserContext.getUser();
        //2.分页查询
        Page<LearningLesson> page =lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .page((pageQuery.toMpPage("latest_learn_time",false)));
        List<LearningLesson> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }

        //3.查询课程信息
        //3.1 获取课程id
        Set<Long> courseIds = records.stream().map(LearningLesson::getCourseId).collect(Collectors.toSet());
        //3.2 查询课程信息
        List<CourseSimpleInfoDTO> cInfoList = courseClient.getSimpleInfoList(courseIds);
        if (CollUtils.isEmpty(cInfoList)) {
            //课程信息不存在
            throw new BadRequestException("课程信息不存在!");
        }
        //3.3 转化课程信息为Map，方便遍历获取多条课程信息,以id为key，课程信息为value
        Map<Long, CourseSimpleInfoDTO> cInfoMap = cInfoList.stream()
                .collect(Collectors.toMap(CourseSimpleInfoDTO::getId, v -> v));

        //4.组装数据
        List<LearningLessonVO> list= new ArrayList<>(records.size());
        for (LearningLesson r : records) {
            //4.1. LearningLesson转化成VO
            LearningLessonVO vo = BeanUtils.copyBean(r, LearningLessonVO.class);

            CourseSimpleInfoDTO cInfo = cInfoMap.get(r.getCourseId());
            vo.setCourseName(cInfo.getName());
            vo.setCourseCoverUrl(cInfo.getCoverUrl());
            vo.setSections(cInfo.getSectionNum());
            list.add(vo);
        }
        return PageDTO.of(page, list);
    }


    /**
     * 查询当前正在学习的课程
     * @return
     */

    @Override
    public LearningLessonVO queryMyCurrentLesson() {
        // 1.获取当前登录的用户
        Long userId = UserContext.getUser();
        // 2.查询正在学习的课程 select * from xx where user_id = #{userId} AND status = 1 order by latest_learn_time limit 1
        LearningLesson lesson = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getStatus, LessonStatus.LEARNING.getValue())
                .orderByDesc(LearningLesson::getLatestLearnTime)
                .last("limit 1")
                .one();
        if (lesson == null) {
            return null;
        }
        // 3.拷贝PO基础属性到VO
        LearningLessonVO vo = BeanUtils.copyBean(lesson, LearningLessonVO.class);
        // 4.查询课程信息
        CourseFullInfoDTO cInfo = courseClient.getCourseInfoById(lesson.getCourseId(), false, false);
        if (cInfo == null) {
            throw new BadRequestException("课程不存在");
        }
        vo.setCourseName(cInfo.getName());
        vo.setCourseCoverUrl(cInfo.getCoverUrl());
        vo.setSections(cInfo.getSectionNum());
        // 5.统计课表中的课程数量 select count(1) from xxx where user_id = #{userId}
        Integer courseAmount = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .count();
        vo.setCourseAmount(courseAmount);
        // 6.查询小节信息
        List<CataSimpleInfoDTO> cataInfos =
                catalogueClient.batchQueryCatalogue(CollUtils.singletonList(lesson.getLatestSectionId()));
        if (!CollUtils.isEmpty(cataInfos)) {
            CataSimpleInfoDTO cataInfo = cataInfos.get(0);
            vo.setLatestSectionName(cataInfo.getName());
            vo.setLatestSectionIndex(cataInfo.getCIndex());
        }
        return vo;
    }


    /**
     * 删除课程表
     *
     * @param userId    用户ID
     * @param courseIds 课程ID集合
     */
    @Override
    public void deleteUserLesson(Long userId, List<Long> courseIds) {
        //根据课程ID查询课程详情
        LambdaQueryChainWrapper<LearningLesson> wrapper =
                lambdaQuery().eq(LearningLesson::getUserId, userId)
                        .in(LearningLesson::getCourseId, courseIds);
        //由于learning_lesson表中没有合适的逻辑删除字段，直接干掉数据
        remove(wrapper);
    }

    /**
     * 验证课程是在有效期，在的话返回课程ID，反之返回 Null
     *
     * @param courseId 课程 ID
     * @return
     */
    @Override
    public Long isLessonValid(Long courseId) {
        if (ObjectUtil.isNull(courseId)) {
            log.error("课程id为{}", courseId);
            return courseId;
        }
        Long userId = UserContext.getUser();
        LearningLesson lesson = lambdaQuery().eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId).one();
        if (ObjectUtil.isNull(lesson)) {
            log.error("该用户未拥有{}课程", lesson);
            return null;
        }
        LessonStatus status = lesson.getStatus();
        LocalDateTime expireTime = lesson.getExpireTime();
        LocalDateTime now = LocalDateTime.now();
        if (LessonStatus.EXPIRED.equalsValue(status.getValue()) || now.isAfter(expireTime)) {
            log.error("用户课程{}已过期", lesson);
            return null;
        }
        return lesson.getId();
    }

    /**
     * 查询课表信息，获取相关课表信息
     *
     * @param courseId 课程ID
     * @return
     */
    @Override
    public LearningLessonStatusVO statusInfo(Long courseId) {
        //1.获取当前登录的用户
        Long userId = UserContext.getUser();
        //2.根据用户ID和课程ID查询课程表
        LearningLesson lesson = lambdaQuery().eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId).one();
        //3.如果课程表不存在，返回null
        if (lesson == null) {
            return null;

        }
        return BeanUtils.copyBean(lesson, LearningLessonStatusVO.class);
    }

    /**
     * 根据课程ID，统计报名人数
     *
     * @param courseId 课程ID
     * @return
     */
    @Override
    public Integer countLearningLessonByCourse(Long courseId) {
        return lambdaQuery().eq(LearningLesson::getCourseId, courseId).count();
    }


    /**
     * 根据课程ID和用户ID查询课程表
     * @param courseId 课程ID
     * @param userId 用户ID
     * @return 课程表
     */
    @Override
    public LearningLesson queryByCourseIdAndUserId(Long courseId, Long userId) {
        return lambdaQuery().eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId).one();
    }

    @Override
    public void createPlan(LearningPlanDTO planDTO) {
        //1.获取当前登录的用户
        Long userId = UserContext.getUser();
        //2.根据课程ID和用户ID查询课程表
        LearningLesson lesson = queryByCourseIdAndUserId(planDTO.getCourseId(), userId);
        //3.如果课程表不存在，返回null
        if (lesson == null) {
            throw new BadRequestException("课程不存在");
        }
        //4.更新课表
        LearningLesson ls = new LearningLesson();
        ls.setId(lesson.getId());
        ls.setWeekFreq(planDTO.getFreq());
        if (lesson.getPlanStatus() == PlanStatus.NO_PLAN) {
            ls.setPlanStatus(PlanStatus.PLAN_RUNNING);
        }
        updateById(ls);

    }
    @Override
    public LearningPlanPageVO queryMyPlans(PageQuery query) {
        LearningPlanPageVO result = new LearningPlanPageVO();
        // 1.获取当前登录用户
        Long userId = UserContext.getUser();
        // 2.获取本周起始时间
        LocalDate now = LocalDate.now();
        LocalDateTime begin = DateUtils.getWeekBeginTime(now);
        LocalDateTime end = DateUtils.getWeekEndTime(now);
        // 3.查询总的统计数据
        // 3.1.本周总的已学习小节数量
        Integer weekFinished = learningRecordMapper.selectCount(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getFinished, true)
                .gt(LearningRecord::getFinishTime, begin)
                .lt(LearningRecord::getFinishTime, end)
        );
        result.setWeekFinished(weekFinished);
        // 3.2.本周总的计划学习小节数量
        Integer weekTotalPlan = getBaseMapper().queryTotalPlan(userId);
        result.setWeekTotalPlan(weekTotalPlan);
        // TODO 3.3.本周学习积分

        // 4.查询分页数据
        // 4.1.分页查询课表信息以及学习计划信息
        Page<LearningLesson> p = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getPlanStatus, PlanStatus.PLAN_RUNNING)
                .in(LearningLesson::getStatus, LessonStatus.NOT_BEGIN, LessonStatus.LEARNING)
                .page(query.toMpPage("latest_learn_time", false));
        List<LearningLesson> records = p.getRecords();
        if (CollUtils.isEmpty(records)) {
            return result.emptyPage(p);
        }
        // 4.2.查询课表对应的课程信息
        Map<Long, CourseSimpleInfoDTO> cMap = queryCourseSimpleInfoList(records);
        // 4.3.统计每一个课程本周已学习小节数量
        List<IdAndNumDTO> list = learningRecordMapper.countLearnedSections(userId, begin, end);
        Map<Long, Integer> countMap = IdAndNumDTO.toMap(list);
        // 4.4.组装数据VO
        List<LearningPlanVO> voList = new ArrayList<>(records.size());
        for (LearningLesson r : records) {
            // 4.4.1.拷贝基础属性到vo
            LearningPlanVO vo = BeanUtils.copyBean(r, LearningPlanVO.class);
            // 4.4.2.填充课程详细信息
            CourseSimpleInfoDTO cInfo = cMap.get(r.getCourseId());
            if (cInfo != null) {
                vo.setCourseName(cInfo.getName());
                vo.setSections(cInfo.getSectionNum());
            }
            // 4.4.3.每个课程的本周已学习小节数量
            vo.setWeekLearnedSections(countMap.getOrDefault(r.getId(), 0));
            voList.add(vo);
        }
        return result.pageInfo(p.getTotal(), p.getPages(), voList);
    }


    private Map<Long, CourseSimpleInfoDTO> queryCourseSimpleInfoList(List<LearningLesson> records) {
        // 3.1.获取课程id
        Set<Long> cIds = records.stream().map(LearningLesson::getCourseId).collect(Collectors.toSet());
        // 3.2.查询课程信息
        List<CourseSimpleInfoDTO> cInfoList = courseClient.getSimpleInfoList(cIds);
        if (CollUtils.isEmpty(cInfoList)) {
            // 课程不存在，无法添加
            throw new BadRequestException("课程信息不存在！");
        }
        // 3.3.把课程集合处理成Map，key是courseId，值是course本身
        Map<Long, CourseSimpleInfoDTO> cMap = cInfoList.stream()
                .collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        return cMap;
    }


}
