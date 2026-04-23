package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.service.IInteractionQuestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
@RequestMapping("/admin/questions")
public class InteractionAdminQuestionController {


    private final IInteractionQuestionService questionService;


    @ApiOperation("分页查询")
    @GetMapping("/admin/page")
    public PageDTO<QuestionAdminVO> pageQueryQuestionsForAdmin(QuestionAdminPageQuery query){

        return questionService.pageQueryQuestionsForAdmin(query);
    }

    @ApiOperation("根据id查询互动问题")
    @GetMapping("{id}")
    public QuestionVO queryQuestionsById(@PathVariable Long id ){
        return questionService.queryQuestionsById(id);
    }

    /**
     * 显示或者隐藏问题
     * @param id     问题 ID
     * @param hidden 隐藏结果
     */
    @ApiOperation("显示或者隐藏问题")
    @PutMapping("/{id}/hidden/{hidden}")
    public void hiddenQuestion(@PathVariable("id") Long id,
                               @PathVariable("hidden") Integer hidden) {
        questionService.hiddenQuestion(id, hidden);
    }
}
