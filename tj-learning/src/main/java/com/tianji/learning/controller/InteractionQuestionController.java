package com.tianji.learning.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.dto.exam.QuestionDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.service.IInteractionQuestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>
 * 互动提问的问题表 前端控制器
 * </p>
 *
 * @author hazard
 * @since 2025-07-14
 */
@Api(tags = "互动提问-问题接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/questions")
public class InteractionQuestionController {


    private final IInteractionQuestionService questionService;

    @ApiOperation("新增提问")
    @PostMapping
    public void saveQuestion(@Valid @RequestBody QuestionFormDTO questionDTO){
        questionService.saveQuestion(questionDTO);
    }

    @ApiOperation("根据ID删除问题")
    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable("id") Long id) {
        questionService.deleteById(id);
    }


    @ApiOperation("分页查询")
    @GetMapping("/page")
    public PageDTO<QuestionVO> pageQueryQuestions(QuestionPageQuery  query){

        return questionService.pageQueryQuestions(query);
    }

    @ApiOperation("根据id查询互动问题")
    @GetMapping("{id}")
    public QuestionVO queryQuestionsById(@PathVariable Long id ){
        return questionService.queryQuestionsById(id);
    }

}
