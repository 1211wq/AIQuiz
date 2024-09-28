package com.ssu.aiQuiz.scoring;

import cn.hutool.json.JSONUtil;
import com.ssu.aiQuiz.model.dto.question.QuestionContentDTO;
import com.ssu.aiQuiz.model.entity.App;
import com.ssu.aiQuiz.model.entity.Question;
import com.ssu.aiQuiz.model.entity.ScoringResult;
import com.ssu.aiQuiz.model.entity.UserAnswer;
import com.ssu.aiQuiz.model.vo.QuestionVO;
import com.ssu.aiQuiz.service.IAppService;
import com.ssu.aiQuiz.service.IQuestionService;
import com.ssu.aiQuiz.service.IScoringResultService;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义测评类评分策略
 */
@ScoringStrategyConfig(appType = 1,scoringStrategy = 0)
public class CustomTestScoringStrategy implements ScoringStrategy {

    @Resource
    private IQuestionService questionService;

    @Resource
    private IScoringResultService scoringResultService;

    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        // 根据id获取题目和结果信息
        Long appId = app.getId();
        Question question = questionService.lambdaQuery().eq(Question::getAppId, appId).one();
        List<ScoringResult> scoringResultList = scoringResultService.lambdaQuery().eq(ScoringResult::getAppId, appId).list();

        // 统计用户的答案对应的属性个数
        Map<String, Integer> optionCount = new HashMap<>();

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
                        // 获取选项的result属性
                        String result = option.getResult();

                        // 如果result属性不在optionCount中，初始化为0
                        if (!optionCount.containsKey(result)) {
                            optionCount.put(result, 0);
                        }

                        // 在optionCount中增加计数
                        optionCount.put(result, optionCount.get(result) + 1);
                    }
                }
            }
        }

        // 遍历每种评分结果
        int maxScore = 0;
        ScoringResult maxScoringResult = scoringResultList.get(0);

        // 遍历评分结果列表
        for (ScoringResult scoringResult : scoringResultList) {
            List<String> resultProp = JSONUtil.toList(scoringResult.getResultProp(), String.class);
            // 计算当前评分结果的分数，[I, E] => [10, 5] => 15
            int score = resultProp.stream()
                    .mapToInt(prop -> optionCount.getOrDefault(prop, 0))
                    .sum();

            // 如果分数高于当前最高分数，更新最高分数和最高分数对应的评分结果
            if (score > maxScore) {
                maxScore = score;
                maxScoringResult = scoringResult;
            }
        }

        // 填充答案到UserAnswer返回
        UserAnswer userAnswer = new UserAnswer();
        userAnswer.setAppId(appId);
        userAnswer.setAppType(app.getAppType());
        userAnswer.setScoringStrategy(app.getScoringStrategy());
        userAnswer.setChoices(JSONUtil.toJsonStr(choices));
        userAnswer.setResultId(maxScoringResult.getId());
        userAnswer.setResultName(maxScoringResult.getResultName());
        userAnswer.setResultDesc(maxScoringResult.getResultDesc());
        userAnswer.setResultPicture(maxScoringResult.getResultPicture());
        return userAnswer;
    }
}
