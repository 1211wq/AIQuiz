package com.ssu.aiQuiz.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ssu.aiQuiz.common.ErrorCode;
import com.ssu.aiQuiz.constant.CommonConstant;
import com.ssu.aiQuiz.exception.ThrowUtils;
import com.ssu.aiQuiz.model.dto.question.QuestionQueryRequest;
import com.ssu.aiQuiz.model.entity.App;
import com.ssu.aiQuiz.model.entity.Question;
import com.ssu.aiQuiz.mapper.QuestionMapper;
import com.ssu.aiQuiz.model.entity.User;
import com.ssu.aiQuiz.model.vo.QuestionVO;
import com.ssu.aiQuiz.model.vo.UserVO;
import com.ssu.aiQuiz.service.IAppService;
import com.ssu.aiQuiz.service.IQuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ssu.aiQuiz.service.UserService;
import com.ssu.aiQuiz.utils.SqlUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 题目 服务实现类
 * </p>
 *
 * @author ssu
 * @since 2024-09-05
 */
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements IQuestionService {

    @Resource
    private IAppService appService;
    @Resource
    private UserService userService;

    /**
     * 数据校验
     *
     * @param question 题目
     * @param add      是否为创建数据
     */
    @Override
    public void validQuestion(Question question, boolean add) {
        ThrowUtils.throwIf(question == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String questionContent = question.getQuestionContent();
        Long appId = question.getAppId();
        // 创建数据时，参数不能为空
        if (add) {
            // 补充校验规则
            ThrowUtils.throwIf(StringUtils.isBlank(questionContent), ErrorCode.PARAMS_ERROR, "题目内容不能为空");
            ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "appId 非法");
        }
        // 修改数据时，有参数则校验
        // 补充校验规则
        if (appId != null) {
            App app = appService.getById(appId);
            ThrowUtils.throwIf(app == null, ErrorCode.PARAMS_ERROR, "应用不存在");
        }
    }

    @Override
    public QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if (questionQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = questionQueryRequest.getId();
        String questionContent = questionQueryRequest.getQuestionContent();
        Long appId = questionQueryRequest.getAppId();
        Long userId = questionQueryRequest.getUserId();
        Long notId = questionQueryRequest.getNotId();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();

        // 补充需要的查询条件
        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(questionContent), "questionContent", questionContent);
        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(appId), "appId", appId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取题目封装
     *
     * @param question 题目
     * @param request
     * @return
     */
    @Override
    public QuestionVO getQuestionVO(Question question, HttpServletRequest request) {
        QuestionVO questionVO = QuestionVO.objToVo(question);

        // 关联查询用户
        Long userId = questionVO.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        questionVO.setUser(userVO);

        return questionVO;
    }

    /**
     * 分页获取题目封装
     *
     * @param questionPage
     * @param request
     * @return
     */
    @Override
    public Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request) {
        List<Question> questionList = questionPage.getRecords();
        Page<QuestionVO> questionVOPage = new Page<>(questionPage.getCurrent(), questionPage.getSize());
        if (CollUtil.isEmpty(questionList)) {
            return questionVOPage;
        }

        List<QuestionVO> questionVOList = questionList.stream().map(question -> {
            return QuestionVO.objToVo(question);
        }).collect(Collectors.toList());

        // 关联查询用户
        Set<Long> userIdSet = questionList.stream().map(Question::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> map = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
        questionVOList.forEach(questionVO -> {
            Long userId = questionVO.getUserId();
            User user = null;
            if (userId != null && userId > 0) {
                user = map.get(userId).get(0);
            }
            questionVO.setUser(userService.getUserVO(user));
        });
        questionVOPage.setRecords(questionVOList);
        return questionVOPage;
    }



}
