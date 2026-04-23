package com.tianji.learning.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动提问的问题表 服务实现类
 * </p>
 *
 * @author hazard
 * @since 2025-07-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {

    private final InteractionReplyMapper replyMapper;
    private final UserClient userClient;
    private final CourseClient courseClient;
    private final SearchClient searchClient;
    private final CatalogueClient catalogueClient;
    private final CategoryCache categoryCache;

    /**
     * 新增问题
     * @param questionDTO  问题
     */
    @Override
    @Transactional
    public void saveQuestion(QuestionFormDTO questionDTO) {
        // 1.获取登录用户
        Long userId = UserContext.getUser();
        // 2.数据转换
        InteractionQuestion question = BeanUtils.toBean(questionDTO, InteractionQuestion.class);
        // 3.补充数据
        question.setUserId(userId);
        // 4.保存问题
        save(question);
    }


    /**
     * 分页查询问题
     * @param query 查询参数
     * @return 分页数据
     */
    @Override
    public PageDTO<QuestionVO> pageQueryQuestions(QuestionPageQuery query) {
        //1.参数校验，避免数据库数据全被查出来
        Long courseId = query.getCourseId();
        Long sectionId = query.getSectionId();
        if (courseId == null && sectionId == null) {
            throw new BadRequestException("课程id或小节id不能同时为空");
        }

        //2.分页查询,三个过滤条件+隐含字段(指定返回字段,排除description）
        //.select(InteractionQuestion.class, info -> !info.getProperty().equals("description"))
        Page<InteractionQuestion> page = lambdaQuery()
                .select(InteractionQuestion.class, info -> !"description".equals(info.getProperty()))
                .eq(query.getOnlyMine() != null , InteractionQuestion::getUserId, UserContext.getUser())
                .eq(courseId != null, InteractionQuestion::getCourseId, courseId)
                .eq(sectionId != null, InteractionQuestion::getSectionId, sectionId)
                .eq(InteractionQuestion::getHidden, false)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        //转为List，判空
        List<InteractionQuestion> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }

        //3.根据id查询提问者和最近一次回答的信息,不同问题的提问者可能为同一个人
        Set<Long> userIds = new HashSet<>();
        Set<Long> answerIds = new HashSet<>();

        //3.1 获取问题当中的提问者id和最近一次回答的id
        for (InteractionQuestion question : records) {
            //非匿名提问
            if (!question.getAnonymity()){
                userIds.add(question.getUserId());
            }
            answerIds.add(question.getLatestAnswerId());
        }

        //3.2 根据最近一次回答的ids查询最近一次回答的内容,没有回答则map为空,map以id为key，回答为value
        answerIds.remove(null);
        Map<Long, InteractionReply> rMap = new HashMap<>(answerIds.size());
        if (!CollUtils.isEmpty(answerIds)) {
            //回答不为空时才查询
            List<InteractionReply> replies = replyMapper.selectBatchIds(answerIds);
            for (InteractionReply reply : replies) {
                rMap.put(reply.getId(), reply);
                //非匿名回答，将回答者id加入set
                if (!reply.getAnonymity()){
                    userIds.add(reply.getUserId());
                }
            }
        }

        //3.3 根据提问者id查询提问者信息(用户微服务）也是使用map，以id为key，回答为value
        userIds.remove(null);
        Map<Long, UserDTO> uMap = new HashMap<>(userIds.size());
        //Map<Long, UserDTO> uMap = new HashMap<>(questionIds.size()); //手动创建map
        if (!CollUtils.isEmpty(userIds)) {
            List<UserDTO> users = userClient.queryUserByIds(userIds);
            //stream流创建map(id,user)
            uMap = users.stream()
                    .collect(Collectors.toMap(UserDTO::getId, u -> u));
        }

        //4.封装vo
        List<QuestionVO> voList = new ArrayList<>(records.size());
        for (InteractionQuestion question : records) {
            //4.1 po -> vo
            QuestionVO vo = BeanUtils.toBean(question, QuestionVO.class);
            voList.add(vo);

            //4.2 设置提问者信息
            if (!question.getAnonymity()){
                UserDTO userDTO = uMap.get(question.getUserId());
                if (userDTO != null) {
                    vo.setUserName(userDTO.getName());
                    vo.setUserIcon(userDTO.getIcon());
                }
            }

            //4.3 设置最近一次回答信息
            InteractionReply reply = rMap.get(question.getLatestAnswerId());
            if (reply!=null) {
                vo.setLatestReplyContent(reply.getContent());
                if (!reply.getAnonymity()){
                    UserDTO user = uMap.get(reply.getUserId());
                    vo.setLatestReplyUser(user.getName());
                }
            }
        }
        return PageDTO.of(page, voList);
    }

    /**
     * 根据id查询问题
     * @param id 问题id
     * @return  问题
     */
    @Override
    public QuestionVO queryQuestionsById(Long id) {
        //1.根据id查询
        InteractionQuestion question = getById(id);
        //2.数据校验
        if (question==null || question.getHidden()) {
            return null;
        }
        //3.查询提问者信息
        UserDTO user=null;
        if (!question.getAnonymity()){
            user = userClient.queryUserById(question.getUserId());
        }
        //4.封装vo
        QuestionVO vo = BeanUtils.toBean(question, QuestionVO.class);
        if (user!=null){
            vo.setUserName(user.getName());
            vo.setUserIcon(user.getIcon());
        }
        return vo;
    }

    /**
     * 管理端分页查询问题
     * @param query 查询参数
     * @return 问题
     */
    @Override
    public PageDTO<QuestionAdminVO> pageQueryQuestionsForAdmin(QuestionAdminPageQuery query) {

            // 1.处理课程名称，得到课程id
            List<Long> courseIds = null;
            if (StringUtils.isNotBlank(query.getCourseName())) {
                courseIds = searchClient.queryCoursesIdByName(query.getCourseName());
                if (CollUtils.isEmpty(courseIds)) {
                    return PageDTO.empty(0L, 0L);
                }
            }
            // 2.分页查询
            Integer status = query.getStatus();
            LocalDateTime begin = query.getBeginTime();
            LocalDateTime end = query.getEndTime();
            Page<InteractionQuestion> page = lambdaQuery()
                    .in(courseIds != null, InteractionQuestion::getCourseId, courseIds)
                    .eq(status != null, InteractionQuestion::getStatus, status)
                    .gt(begin != null, InteractionQuestion::getCreateTime, begin)
                    .lt(end != null, InteractionQuestion::getCreateTime, end)
                    .page(query.toMpPageDefaultSortByCreateTimeDesc());
            List<InteractionQuestion> records = page.getRecords();
            if (CollUtils.isEmpty(records)) {
                return PageDTO.empty(page);
            }

            // 3.准备VO需要的数据：用户数据、课程数据、章节数据
            Set<Long> userIds = new HashSet<>();
            Set<Long> cIds = new HashSet<>();
            Set<Long> cataIds = new HashSet<>();
            // 3.1.获取各种数据的id集合
            for (InteractionQuestion q : records) {
                userIds.add(q.getUserId());
                cIds.add(q.getCourseId());
                cataIds.add(q.getChapterId());
                cataIds.add(q.getSectionId());
            }
            // 3.2.根据id查询用户
            List<UserDTO> users = userClient.queryUserByIds(userIds);
            Map<Long, UserDTO> userMap = new HashMap<>(users.size());
            if (CollUtils.isNotEmpty(users)) {
                userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
            }

            // 3.3.根据id查询课程
            List<CourseSimpleInfoDTO> cInfos = courseClient.getSimpleInfoList(cIds);
            Map<Long, CourseSimpleInfoDTO> cInfoMap = new HashMap<>(cInfos.size());
            if (CollUtils.isNotEmpty(cInfos)) {
                cInfoMap = cInfos.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
            }

            // 3.4.根据id查询章节
            List<CataSimpleInfoDTO> catas = catalogueClient.batchQueryCatalogue(cataIds);
            Map<Long, String> cataMap = new HashMap<>(catas.size());
            if (CollUtils.isNotEmpty(catas)) {
                cataMap = catas.stream()
                        .collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
            }


            // 4.封装VO
            List<QuestionAdminVO> voList = new ArrayList<>(records.size());
            for (InteractionQuestion q : records) {
                // 4.1.将PO转VO，属性拷贝
                QuestionAdminVO vo = BeanUtils.copyBean(q, QuestionAdminVO.class);
                voList.add(vo);
                // 4.2.用户信息
                UserDTO user = userMap.get(q.getUserId());
                if (user != null) {
                    vo.setUserName(user.getName());
                }
                // 4.3.课程信息以及分类信息
                CourseSimpleInfoDTO cInfo = cInfoMap.get(q.getCourseId());
                if (cInfo != null) {
                    vo.setCourseName(cInfo.getName());
                    vo.setCategoryName(categoryCache.getCategoryNames(cInfo.getCategoryIds()));
                }
                // 4.4.章节信息
                vo.setChapterName(cataMap.getOrDefault(q.getChapterId(), ""));
                vo.setSectionName(cataMap.getOrDefault(q.getSectionId(), ""));
            }
            return PageDTO.of(page, voList);
    }

    /**
     * 删除问题
     * @param id 问题 ID
     */
    @Override
    public void deleteById(Long id) {
        InteractionQuestion question = getById(id);
        if (ObjectUtil.isNull(question)) {
            throw new BadRequestException("查询问题详情不存在");
        }
        Long userId = UserContext.getUser();
        if (!userId.equals(question.getUserId())) {
            throw new BadRequestException("您不能删除别人的问题");
        }
        //根据问题ID删除所有的评论数据
        LambdaQueryWrapper<InteractionReply> replyLambdaQueryWrapper =
                new LambdaQueryWrapper<InteractionReply>().eq(InteractionReply::getQuestionId, question.getId());
        replyMapper.delete(replyLambdaQueryWrapper);
        //删除问题
        boolean removeQuestion = removeById(question.getId());
        log.info("用户：{}，删除问题详情：{}", userId, removeQuestion);
    }

    /**
     * 显示或者隐藏问题
     * @param id     问题 ID
     * @param hidden 隐藏结果
     */
    @Override
    public void hiddenQuestion(Long id, Integer hidden) {
        InteractionQuestion question = getById(id);
        if (ObjectUtil.isNull(question)) {
            throw new BadRequestException("查询问题详情不存在");
        }
        question.setHidden(1 == hidden ? Boolean.TRUE : Boolean.FALSE);
        updateById(question);
    }
}
