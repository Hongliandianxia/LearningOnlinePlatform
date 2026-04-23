package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;

/**
 * <p>
 * 互动提问的问题表 服务类
 * </p>
 *
 * @author hazard
 * @since 2025-07-14
 */
public interface IInteractionQuestionService extends IService<InteractionQuestion> {

    void saveQuestion(QuestionFormDTO questionDTO);

    PageDTO<QuestionVO> pageQueryQuestions(QuestionPageQuery query);

    QuestionVO queryQuestionsById(Long id);

    PageDTO<QuestionAdminVO> pageQueryQuestionsForAdmin(QuestionAdminPageQuery query)

            ;

    void deleteById(Long id);

    void hiddenQuestion(Long id, Integer hidden);

}
