package com.tianji.learning.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionReplyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动问题的回答或评论 服务实现类
 * </p>
 *
 * @author hazard
 * @since 2025-07-14
 */
@Service
@RequiredArgsConstructor
public class InteractionReplyServiceImpl extends ServiceImpl<InteractionReplyMapper, InteractionReply> implements IInteractionReplyService {

    private final UserClient userClient;

    /**
     * 新增评论或者回答
     * @param replyDTO 评论或回答数据
     */
    @Override
    public void addReply(ReplyDTO replyDTO) {
        InteractionReply interactionReply = BeanUtils.copyBean(replyDTO, InteractionReply.class);
        interactionReply.setReplyTimes(0);
        interactionReply.setLikedTimes(0);
        interactionReply.setUserId(UserContext.getUser());
        interactionReply.setCreateTime(LocalDateTime.now());
        interactionReply.setUpdateTime(LocalDateTime.now());
        save(interactionReply);
    }
    @Override
    public PageDTO<ReplyVO> pageByQuery(ReplyPageQuery replyPageQuery) {
        Long questionId = replyPageQuery.getQuestionId();
        Long answerId = replyPageQuery.getAnswerId();
        if (ObjectUtil.isNull(questionId) && ObjectUtil.isNull(answerId)) {
            throw new BadRequestException("问题ID和回复ID不能同时为空!");
        }
        Page<InteractionReply> page = lambdaQuery()
                .eq(ObjectUtil.isNotNull(questionId), InteractionReply::getQuestionId, questionId)
                .eq(ObjectUtil.isNotNull(answerId), InteractionReply::getAnswerId, answerId)
                .eq(InteractionReply::getHidden,Boolean.FALSE)
                .page(replyPageQuery.toMpPage("liked_times", true));
        List<InteractionReply> list = page.getRecords();
        //封装 用户数据
        HashSet<Long> userSet = new HashSet<>(20);
        for (InteractionReply reply : list) {
            userSet.add(reply.getUserId());
            userSet.add(reply.getTargetUserId());
        }
        Map<Long, UserDTO> userMap = userClient.queryUserByIds(userSet)
                .stream().collect(Collectors.toMap(UserDTO::getId, user -> user));
        ArrayList<ReplyVO> voList = new ArrayList<>(list.size());
        for (InteractionReply reply : list) {
            ReplyVO vo = BeanUtils.copyBean(reply, ReplyVO.class);
            //封装回答者相关数据
            if(!reply.getAnonymity()){
                Long userId = reply.getUserId();
                UserDTO userInfo = userMap.get(userId);
                if (ObjectUtil.isNotNull(userInfo)) {
                    vo.setUserName(userInfo.getName());
                    vo.setUserIcon(userInfo.getIcon());
                }
            }
            //封装被评论的相关数据
            Long targetUserId = reply.getTargetUserId();
            UserDTO targetUserInfo = userMap.get(targetUserId);
            if (ObjectUtil.isNotNull(targetUserInfo)) {
                vo.setTargetUserName(targetUserInfo.getName());
            }
            voList.add(vo);
        }
        return PageDTO.of(page, voList);
    }

    /**
     * 管理端 - 分页查询
     * @param replyPageQuery 查询参数
     * @return 分页数据
     */
    @Override
    public PageDTO<ReplyVO> pageAdminByQuery(ReplyPageQuery replyPageQuery) {
        Long questionId = replyPageQuery.getQuestionId();
        Long answerId = replyPageQuery.getAnswerId();
        if (ObjectUtil.isNull(questionId) && ObjectUtil.isNull(answerId)) {
            throw new BadRequestException("问题ID和回复ID不能同时为空!");
        }
        Page<InteractionReply> page = lambdaQuery()
                .eq(ObjectUtil.isNotNull(questionId), InteractionReply::getQuestionId, questionId)
                .eq(ObjectUtil.isNotNull(answerId), InteractionReply::getAnswerId, answerId)
                .page(replyPageQuery.toMpPage("liked_times", true));
        List<InteractionReply> list = page.getRecords();
        //封装 用户数据
        HashSet<Long> userSet = new HashSet<>(20);
        for (InteractionReply reply : list) {
            userSet.add(reply.getUserId());
            userSet.add(reply.getTargetUserId());
        }
        Map<Long, UserDTO> userMap = userClient.queryUserByIds(userSet)
                .stream().collect(Collectors.toMap(UserDTO::getId, user -> user));
        ArrayList<ReplyVO> voList = new ArrayList<>(list.size());
        for (InteractionReply reply : list) {
            ReplyVO vo = BeanUtils.copyBean(reply, ReplyVO.class);
            //封装回答者相关数据
            Long userId = reply.getUserId();
            UserDTO userInfo = userMap.get(userId);
            if (ObjectUtil.isNotNull(userInfo)) {
                vo.setUserName(userInfo.getName());
                vo.setUserIcon(userInfo.getIcon());
            }
            //封装被评论的相关数据
            Long targetUserId = reply.getTargetUserId();
            UserDTO targetUserInfo = userMap.get(targetUserId);
            if (ObjectUtil.isNotNull(targetUserInfo)) {
                vo.setTargetUserName(targetUserInfo.getName());
            }
            voList.add(vo);
        }
        return PageDTO.of(page, voList);
    }
}
