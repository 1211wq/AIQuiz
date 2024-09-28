package com.ssu.aiQuiz.scoring;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ssu.aiQuiz.common.ErrorCode;
import com.ssu.aiQuiz.exception.ThrowUtils;
import com.ssu.aiQuiz.manager.AiManager;
import com.ssu.aiQuiz.model.dto.question.QuestionAnswerDTO;
import com.ssu.aiQuiz.model.dto.question.QuestionContentDTO;
import com.ssu.aiQuiz.model.entity.App;
import com.ssu.aiQuiz.model.entity.Question;
import com.ssu.aiQuiz.model.entity.UserAnswer;
import com.ssu.aiQuiz.model.vo.QuestionVO;
import com.ssu.aiQuiz.service.IQuestionService;
import com.ssu.aiQuiz.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@ScoringStrategyConfig(appType = 1, scoringStrategy = 1)
public class AiTestScoringStrategy implements ScoringStrategy{

    @Resource
    private IQuestionService questionService;

    @Resource
    private AiManager aiManager;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private UserService userService;

    private static final String AI_ANSWER_LOCK = "AI_ANSWER_LOCK";

    private final Cache<String, String> answerCacheMap =
            Caffeine.newBuilder().initialCapacity(1024)
                    .expireAfterAccess(5L, TimeUnit.MINUTES) // 过期时间
                    .build();


    private static final String SYSTEM_MESSAGE = "你是一位严谨的判题专家，我会给你如下信息：\n" +
            "应用名称，\n" +
            "【【【应用描述】】】，\n" +
            "题目和用户回答的列表：格式为 [{\"title\": \"题目\",\"answer\": \"用户回答\"}]\n" +
            "\n" +
            "请你根据上述信息，按照以下步骤来对用户进行评价：\n" +
            "1. 要求：需要给出一个明确的评价结果，包括评价名称（尽量简短）和评价描述（尽量详细，约为于 200 字）\n" +
            "2. 严格按照下面的 json 格式输出评价名称和评价描述\n" +
            "```\n" +
            "{\"resultName\": \"评价名称\", \"resultDesc\": \"评价描述\"}\n" +
            "```\n" +
            "3. 返回格式必须为 JSON 对象";

    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        Long appId = app.getId();

        // 优先从缓存获取答案结果
        String cacheKey = builderCacheKey(appId, choices);
        String answerCacheValue = answerCacheMap.getIfPresent(cacheKey);
        if (StrUtil.isNotBlank(answerCacheValue)) {
            // 构造UserAnswer对象返回
            UserAnswer userAnswer = JSONUtil.toBean(answerCacheValue, UserAnswer.class);
            userAnswer.setAppId(appId);
            userAnswer.setAppType(app.getAppType());
            userAnswer.setScoringStrategy(app.getScoringStrategy());
            userAnswer.setChoices(JSONUtil.toJsonStr(choices));
            // todo 生成随机Id
            userAnswer.setResultId(IdUtil.getSnowflakeNextId());
            return userAnswer;
        }
        // todo 加分布式锁
        // 定义锁
        RLock lock = redissonClient.getLock(AI_ANSWER_LOCK);

        try {
            // 锁竞争
            boolean res = lock.tryLock(3, 15, TimeUnit.SECONDS);
            if (!res) {
                return null;
            }
            // 抢到锁执行后续的业务
            // 获取问题列表
            Question question = questionService.lambdaQuery().eq(Question::getAppId, appId).one();
            ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR);
            QuestionVO questionVO = QuestionVO.objToVo(question);
            List<QuestionContentDTO> questionContentDTOList = questionVO.getQuestionContent();

            // 将选项key转换为题目 + 答案的形式
            List<QuestionAnswerDTO> questionAnswerDTOList = new ArrayList<>();
            for (int i = 0; i < questionContentDTOList.size(); i++) {
                String title = questionContentDTOList.get(i).getTitle();
                List<QuestionContentDTO.Option> options = questionContentDTOList.get(i).getOptions();
                for (QuestionContentDTO.Option option : options) {
                    if (choices.get(i).equals(option.getKey())) {
                        QuestionAnswerDTO questionAnswerDTO = new QuestionAnswerDTO();
                        questionAnswerDTO.setTitle(title);
                        questionAnswerDTO.setUserAnswer(option.getValue());
                        questionAnswerDTOList.add(questionAnswerDTO);
                    }
                }
            }

            // 编写用户prompt
            StringBuilder userMessage = new StringBuilder();
            userMessage.append(SYSTEM_MESSAGE).append("\n");
            userMessage.append(app.getAppName()).append("\n");
            userMessage.append(app.getAppDesc()).append("\n");
            String jsonStr = JSONUtil.toJsonStr(questionAnswerDTOList);
            userMessage.append(jsonStr);

            // AI判题
            String result = aiManager.doStableSyncRequest(userMessage.toString(), SYSTEM_MESSAGE);
            log.info("Ai回答：{}", result);

            // 截取有效json
            int start = result.indexOf("{");
            int end = result.lastIndexOf("}");
            String json = result.substring(start, end + 1);

            // 设置缓存
            answerCacheMap.put(cacheKey, json);

            // 构造UserAnswer对象返回
            UserAnswer userAnswer = JSONUtil.toBean(json, UserAnswer.class);
            userAnswer.setAppId(appId);
            userAnswer.setAppType(app.getAppType());
            userAnswer.setScoringStrategy(app.getScoringStrategy());
            userAnswer.setChoices(JSONUtil.toJsonStr(choices));
            // todo 生成随机Id
//        userAnswer.setResultId(IdUtil.getSnowflakeNextId());

            // todo 扩展，ai生成图片
//        userAnswer.setResultPicture(maxScoringResult.getResultPicture());

            return userAnswer;
        } finally {
            if (lock != null && lock.isLocked()) {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }

    public String builderCacheKey(Long appId, List<String> answer) {
        String md5Hex = DigestUtil.md5Hex(JSONUtil.toJsonStr(answer));
        return String.valueOf(appId) + ":" + md5Hex;
    }
}
