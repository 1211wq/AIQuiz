package com.ssu.aiQuiz.scoring;

import cn.hutool.json.JSONUtil;
import com.ssu.aiQuiz.model.dto.question.QuestionContentDTO;
import com.ssu.aiQuiz.model.entity.App;
import com.ssu.aiQuiz.model.entity.Question;
import com.ssu.aiQuiz.model.entity.ScoringResult;
import com.ssu.aiQuiz.model.entity.UserAnswer;
import com.ssu.aiQuiz.model.vo.QuestionVO;
import com.ssu.aiQuiz.service.IQuestionService;
import com.ssu.aiQuiz.service.IScoringResultService;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

/**
 * 自定义得分类评分策略
 */
@ScoringStrategyConfig(appType = 0,scoringStrategy = 0)
public class CustomScoreScoringStrategy implements ScoringStrategy {
    @Resource
    private IQuestionService questionService;

    @Resource
    private IScoringResultService scoringResultService;

    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        // 1. 根据id获取题目和结果信息
        Long appId = app.getId();
        Question question = questionService.lambdaQuery().eq(Question::getAppId, appId).one();
        List<ScoringResult> scoringResultList = scoringResultService.lambdaQuery().eq(ScoringResult::getAppId, appId).orderByDesc(ScoringResult::getResultScoreRange).list();

        // 2. 统计用户的总得分
        int totalScore = 0;
        QuestionVO questionVO = QuestionVO.objToVo(question);
        List<QuestionContentDTO> questionContent = questionVO.getQuestionContent();

        // 遍历题目列表
        for (QuestionContentDTO questionContentDTO : questionContent) {
            // 遍历答案列表
            for (String answer : choices) {
                // 遍历题目中的选项
                for (QuestionContentDTO.Option option : questionContentDTO.getOptions()) {
                    // 如果答案和选项的key匹配
                    if (option.getKey().equals(answer)) {
                        int score = Optional.of(option.getScore()).orElse(0);
                        totalScore += score;
                    }
                }
            }
        }

        // 3. 遍历得分结果，找到第一个用户分数大于得分范围的结果，作为最终结果
        ScoringResult maxScoringResult = scoringResultList.get(0);
        for (ScoringResult scoringResult : scoringResultList) {
            if (totalScore >= scoringResult.getResultScoreRange()) {
                maxScoringResult = scoringResult;
                break;
            }
        }

        // 4. 构造返回值，填充答案对象的属性
        UserAnswer userAnswer = new UserAnswer();
        userAnswer.setAppId(appId);
        userAnswer.setAppType(app.getAppType());
        userAnswer.setScoringStrategy(app.getScoringStrategy());
        userAnswer.setChoices(JSONUtil.toJsonStr(choices));
        userAnswer.setResultId(maxScoringResult.getId());
        userAnswer.setResultName(maxScoringResult.getResultName());
        userAnswer.setResultDesc(maxScoringResult.getResultDesc());
        userAnswer.setResultPicture(maxScoringResult.getResultPicture());
        userAnswer.setResultScore(totalScore);
        return userAnswer;
    }
}
