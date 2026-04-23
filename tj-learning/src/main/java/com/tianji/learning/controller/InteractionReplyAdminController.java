package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.service.IInteractionReplyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author hazard
 */
@Api(tags = "管理员回答相关问题")
@Slf4j
@RestController
@RequestMapping("/admin/replies")
@RequiredArgsConstructor
public class InteractionReplyAdminController {

    private final IInteractionReplyService iInteractionReplyService;

    /**
     * 用户端 - 分页查询
     * @param replyPageQuery
     * @return
     */
    @ApiOperation("管理端 - 分页查询")
    @GetMapping("/page")
    public PageDTO<ReplyVO> page(ReplyPageQuery replyPageQuery) {
        return iInteractionReplyService.pageAdminByQuery(replyPageQuery);
    }

}