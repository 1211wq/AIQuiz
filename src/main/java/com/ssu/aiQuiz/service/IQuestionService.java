package com.ssu.aiQuiz.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ssu.aiQuiz.model.dto.question.QuestionQueryRequest;
import com.ssu.aiQuiz.model.entity.Question;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ssu.aiQuiz.model.vo.QuestionVO;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 * 题目 服务类
 * </p>
 *
 * @author ssu
 * @since 2024-09-05
 */
public interface IQuestionService extends IService<Question> {

    /**
     * 校验数据
     *
     * @param question
     * @param add      对创建的数据进行校验
     */
    void validQuestion(Question question, boolean add);

    /**
     * 获取查询条件
     *
     * @param questionQueryRequest
     * @return
     */
    QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest);

    /**
     * 获取题目封装
     *
     * @param question 题目
     * @param request
     * @return
     */
    QuestionVO getQuestionVO(Question question, HttpServletRequest request);

    /**
     * 分页获取题目封装
     *
     * @param questionPage
     * @param request
     * @return
     */
    Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request);

}
