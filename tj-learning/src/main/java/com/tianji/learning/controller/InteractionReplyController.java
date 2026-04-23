package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.service.IInteractionReplyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 互动问题的回答或评论 前端控制器
 * </p>
 *
 * @author hazard
 * @since 2025-07-14
 */
@Api(tags = "互动问题-回答或评论接口")
@RestController
@RequestMapping("/replies")
@RequiredArgsConstructor
public class InteractionReplyController {

    private final IInteractionReplyService replyService;


    @ApiOperation("提交答复或者评论")
    @PostMapping()
    public void addReply(@RequestBody ReplyDTO replyDTO) {
        replyService.addReply(replyDTO);
    }

    /**
     * 用户端 - 分页查询
     * @param replyPageQuery
     * @return
     */
    @ApiOperation("用户端 - 分页查询")
    @GetMapping("/page")
    public PageDTO<ReplyVO> page(ReplyPageQuery replyPageQuery) {
        return replyService.pageByQuery(replyPageQuery);
    }
}
