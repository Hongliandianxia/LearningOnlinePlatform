package com.tianji.learning.controller;


import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.service.ILearningRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 学习记录表 前端控制器
 * </p>
 *
 * @author hazard
 * @since 2025-07-10
 */
@Api(tags = "学习记录相关接口")
@RequiredArgsConstructor
@RestController
@RequestMapping("/learning-records")
public class LearningRecordController {

    private final ILearningRecordService learningRecordService;

    /**
     * 查询当前用户指定课程的学习进度
     * @param courseId 课程id
     * @return 课表信息、学习记录及进度信息
     */
    @ApiOperation("查询当前用户指定课程的学习进度")
    @GetMapping("/course/{courseId}")
    public LearningLessonDTO queryLearningRecordByCourse(@ApiParam (value = "课程id",defaultValue = "2")
                                                             @PathVariable("courseId")
                                                             Long courseId){
        return learningRecordService.queryLearningRecordByCourse(courseId);
    }

    /**
     * 添加学习记录
     * @param recordFormDto 记录信息
     */
    @PostMapping()
    @ApiOperation("添加学习记录")
    public void addLearningRecord(@RequestBody LearningRecordFormDTO recordFormDto) {

        learningRecordService.addLearningRecord(recordFormDto);
    }

}
