package com.tianji.learning.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.SectionType;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import com.tianji.learning.utils.LearningRecordDelayTaskHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.login.CredentialNotFoundException;
import java.util.List;

/**
 * <p>
 * 学习记录表 服务实现类
 * </p>
 *
 * @author hazard
 * @since 2025-07-10
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LearningRecordServiceImpl extends ServiceImpl<LearningRecordMapper, LearningRecord> implements ILearningRecordService {

    private final LearningRecordDelayTaskHandler taskHandler;

    private final ILearningLessonService learningLessonService;

    private final CourseClient courseClient;

    /**
     * 查询指定课程的课程学习进度
     * @param courseId 课程id
     * @return 课程学习进度
     */
    @Override
    public LearningLessonDTO queryLearningRecordByCourse(Long courseId) {

        //1.获取用户信息
        Long userId = UserContext.getUser();

        //2.查询课表信息
        LearningLesson lesson = learningLessonService.queryByCourseIdAndUserId(courseId, userId);
        //3.如果课表不存在，返回null
        if (lesson == null) {
            return null;
        }
        //4.查询学习记录信息 select * from learning_record where lesson_id =#{lessonId}
        List<LearningRecord> records = lambdaQuery()
                .eq(LearningRecord::getLessonId, lesson.getId())
                .list();
        List<LearningRecordDTO> learningRecordList = BeanUtils.copyList(records, LearningRecordDTO.class);
        //5.封装成DTO返回
        LearningLessonDTO dto = new LearningLessonDTO();
        dto.setId(lesson.getId());
        dto.setLatestSectionId(lesson.getLatestSectionId());
        dto.setRecords(learningRecordList);
        return dto;
    }

    /**
     * 添加学习记录
     * @param recordFormDto 记录信息
     */
    @Transactional
    @Override
    public void addLearningRecord (LearningRecordFormDTO recordFormDto) {

        //1.获取用户信息
        Long userId = UserContext.getUser();
        //2.处理学习记录
        boolean finished=false;
        if (recordFormDto.getSectionType()== SectionType.VIDEO) {
            //2.1处理视频记录
            log.info("处理视频记录");
            finished =handleVideoRecord(recordFormDto, userId);
        }else {
            //2.2处理考试记录
            finished = handleExamRecord(recordFormDto, userId);
        }
        if (!finished) {
            //无新学习完的小节，无需更新课表中的学习进度
            return;
        }

        //3.处理课表数据
        handleLearningLessonChanges(recordFormDto);
    }

    //处理课表数据
    private void handleLearningLessonChanges(LearningRecordFormDTO recordFormDto) {
        //1.查询课表
        LearningLesson lesson= learningLessonService.getById(recordFormDto.getLessonId());
        if (lesson==null) {
            throw new BizIllegalException("课程不存在，无法更新学习记录!");
        }
        //2.判断是否有新学习小节
        boolean allLearned=false;
        //3.有新完成的课程小节
        CourseFullInfoDTO cInfo= courseClient.getCourseInfoById(lesson.getCourseId(), false, false);
        if (cInfo==null) {
            throw new BizIllegalException("课程不存在，无法更新学习记录!");
        }
        //4.判断课表是否已经完全学习完成
        allLearned=lesson.getLearnedSections()+1>=cInfo.getSectionNum();
        //5.更新课表(课程学习完成状态(第一次学，非第一次学、最新小节ID、最新学习时间、已学习小节数))
        learningLessonService.lambdaUpdate()
                .set(lesson.getLearnedSections()==0,LearningLesson::getStatus, LessonStatus.LEARNING.getValue())
                .set(allLearned,LearningLesson::getStatus, LessonStatus.FINISHED.getValue())
                .setSql("learned_sections=learned_sections+1")
                .eq(LearningLesson::getId, lesson.getId())
                .update();
    }

    //处理考试记录
    private boolean handleExamRecord(LearningRecordFormDTO recordFormDto, Long userId) {
        //将DTO转换为PO数据
        LearningRecord record =BeanUtils.copyBean(recordFormDto, LearningRecord.class);
        //填充PO数据
        record.setUserId(userId);
        record.setFinished(true);
        record.setCreateTime(recordFormDto.getCommitTime());
        //写入数据库
        boolean success=save(record);
        if (!success) {
            throw new DbException("新增考试的记录失败!");
        }
        //考试只要提交就是完成
        return true;
    }

    //处理视频记录
    private boolean handleVideoRecord(LearningRecordFormDTO recordFormDto, Long userId) {
        //1.查询是否有旧学习记录
        LearningRecord oldRecord =queryOldRecord(recordFormDto.getSectionId(),recordFormDto.getLessonId());
        //2.判断是否存在旧数据
        if (oldRecord==null) {
            //3.不存在则新增,只有第一次观看才会执行，低频操作
            //将DTO转换为PO数据
            LearningRecord record =BeanUtils.copyBean(recordFormDto, LearningRecord.class);
            //填充PO数据
            record.setUserId(userId);
            record.setFinished(true);
            record.setCreateTime(recordFormDto.getCommitTime());
            //写入数据库
            boolean success=save(record);
            if (!success) {
                throw new DbException("新增学习的记录失败!");
            }
            return false;
        }
        //4.存在则更新
        //4.1.判断是否是第一次完成
        boolean finished = !oldRecord.getFinished() && recordFormDto.getMoment() <<1 >= recordFormDto.getDuration();
        if (!finished) {
            //非第一次完成，写入缓存并添加记录到延迟队列
            LearningRecord record =BeanUtils.copyBean(recordFormDto, LearningRecord.class);
            record.setId(oldRecord.getId());
            record.setFinished(oldRecord.getFinished());
            taskHandler.addLearningRecordTask(record);

            return false;

        }

        //4.2.更新数据
        boolean success = lambdaUpdate()
                .set(LearningRecord::getMoment, recordFormDto.getMoment())
                .set(LearningRecord::getFinished, true)
                .set(LearningRecord::getFinishTime, recordFormDto.getCommitTime())
                .eq(LearningRecord::getId, oldRecord.getId())
                .update();
        if (!success) {
            throw new DbException("更新学习记录失败！");
        }
        //4.3清理缓存
        taskHandler.cleanRecordCache(recordFormDto.getLessonId(), recordFormDto.getSectionId());
        return true;
    }

    //查询旧学习记录
    private LearningRecord queryOldRecord(Long sectionId, Long lessonId) {
        //1.查询缓存
        log.info("查询缓存学习记录");
        LearningRecord record = taskHandler.readRecordCache(lessonId, sectionId);
        //2命中缓存，直接返回
        log.info("命中缓存");
        if (record != null) {
            return record;
        }
        //3.未命中缓存，查询数据库
        log.info("未命中缓存");
        record=lambdaQuery()
                .eq(LearningRecord::getLessonId,lessonId)
                .eq(LearningRecord::getSectionId, sectionId)
                .one();
        //4.写入缓存
        log.info("写入学习记录到缓存");
        taskHandler.writeRecordCache(record);
        return record;
    }

}
