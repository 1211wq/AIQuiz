package com.ssu.aiQuiz.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ssu.aiQuiz.annotation.AuthCheck;
import com.ssu.aiQuiz.common.BaseResponse;
import com.ssu.aiQuiz.common.DeleteRequest;
import com.ssu.aiQuiz.common.ErrorCode;
import com.ssu.aiQuiz.common.ResultUtils;
import com.ssu.aiQuiz.constant.UserConstant;
import com.ssu.aiQuiz.exception.BusinessException;
import com.ssu.aiQuiz.exception.ThrowUtils;
import com.ssu.aiQuiz.manager.AiManager;
import com.ssu.aiQuiz.model.dto.question.*;
import com.ssu.aiQuiz.model.entity.App;
import com.ssu.aiQuiz.model.entity.Question;
import com.ssu.aiQuiz.model.entity.User;
import com.ssu.aiQuiz.model.enums.AppTypeEnum;
import com.ssu.aiQuiz.model.vo.QuestionVO;
import com.ssu.aiQuiz.service.IAppService;
import com.ssu.aiQuiz.service.IQuestionService;
import com.ssu.aiQuiz.service.UserService;
import com.zhipu.oapi.service.v4.model.ModelData;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * 题目 前端控制器
 * </p>
 *
 * @author ssu
 * @since 2024-09-05
 */
@RestController
@RequestMapping("/question")
@Slf4j
public class QuestionController {

    @Resource
    private IQuestionService questionService;

    @Resource
    private UserService userService;

    @Resource
    private IAppService appService;

    @Resource
    private AiManager aiManager;

    /**
     * 创建题目
     *
     * @param questionAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> createQuestion(@RequestBody QuestionAddRequest questionAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(questionAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 将dto转换为实体类
        Question question = BeanUtil.copyProperties(questionAddRequest, Question.class);
        String jsonStr = JSONUtil.toJsonStr(questionAddRequest.getQuestionContent());
        question.setQuestionContent(jsonStr);

        // 数据校验
        questionService.validQuestion(question, true);

        // 填充默认值
        User loginUser = userService.getLoginUser(request);
        question.setUserId(loginUser.getId());

        // 写入数据库
        boolean result = questionService.save(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 返回写入的数据id
        return ResultUtils.success(question.getId());
    }

    /**
     * 删除题目
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteQuestion(DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        Long id = deleteRequest.getId();

        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);

        // 仅本人或者管理员可删除
        if (!oldQuestion.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 删除
        boolean result = questionService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新题目（仅管理员可用）
     *
     * @param questionUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        if (questionUpdateRequest == null || questionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 将dto转换为实体类
        Question question = BeanUtil.copyProperties(questionUpdateRequest, Question.class);
        String jsonStr = JSONUtil.toJsonStr(questionUpdateRequest.getQuestionContent());
        question.setQuestionContent(jsonStr);

        // 数据校验
        questionService.validQuestion(question, false);

        // 判断是否存在
        Question oldQuestion = questionService.getById(question.getId());
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);

        // 更新
        boolean result = questionService.updateById(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取题目（封装类）
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<QuestionVO> getQuestionVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        // 查询数据库
        Question question = questionService.getById(id);
        ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR);

        // 获取封装类
        QuestionVO questionVO = questionService.getQuestionVO(question, request);
        return ResultUtils.success(questionVO);
    }

    /**
     * 分页获取题目列表（仅管理员可用）
     *
     * @param questionQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Question>> listQuestionByPage(@RequestBody QuestionQueryRequest questionQueryRequest) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 查询数据库
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionPage);
    }

    /**
     * 分页获取题目列表（封装类）
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                               HttpServletRequest request) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 分页获取当前登录用户创建的题目列表
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listMyQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(questionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        questionQueryRequest.setUserId(loginUser.getId());
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 编辑题目（用户使用）
     *
     * @param questionEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editQuestion(@RequestBody QuestionEditRequest questionEditRequest, HttpServletRequest request) {
        if (questionEditRequest == null || questionEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 转换为实体类
        Question question = BeanUtil.copyProperties(questionEditRequest, Question.class);

        // 将题目信息实体类转换为json
        String jsonStr = JSONUtil.toJsonStr(questionEditRequest.getQuestionContent());
        question.setQuestionContent(jsonStr);

        // 数据校验
        questionService.validQuestion(question, false);

        // 判断是否存在
        Question oldQuestion = questionService.getById(questionEditRequest.getId());
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);

        // 进本人或者管理员可操作
        User loginUser = userService.getLoginUser(request);
        if (!Objects.equals(oldQuestion.getUserId(), loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 更新数据
        boolean result = questionService.updateById(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // region AI生成题目功能

    private static final String GENERATE_QUESTION_SYSTEM_MESSAGE = "你是一位严谨的出题专家，我会给你如下信息：\n" +
            "应用名称，\n" +
            "【【【应用描述】】】，\n" +
            "应用类别，\n" +
            "要生成的题目数，\n" +
            "每个题目的选项数\n\n" +
            "请你根据上述信息，按照以下步骤来出题：\n" +
            "\n" +
            "1.要求：题目和选项尽可能地短，题目不要包含序号，每题的选项数以我提供的为主，题目不能重复\n" +
            "2.严格按照下面的 JSON 格式输出题目和选项\n" +
            "[{\"title\":\"题目标题\",\"options\":[{\"value\":\"选项内容\",\"key\":\"A\"},{\"value\":\"\",\"key\":\"B\"}]}]\n" +
            "title 是题目，options 是选项，每个选项的 key 按照英文字母序（比如 A、B、C、D）以此类推，value 是选项内容，根据应用的类型，result为测评类映射的结果，score为得分类的映射结果，你需要为每一个选项都添加一个映射结果，方便后续的评分\n" +
            "3.检查题目是否包含序号，若包含序号则去除序号\n" +
            "4.返回的题目列表格式必须为 JSON 数组";

    /**
     * 生成题目的用户提示词
     *
     * @param app
     * @param questionNumber
     * @param optionNumber
     * @return
     */
    private String getGenerateQuestionUserMessage(App app, int questionNumber, int optionNumber) {
        StringBuilder userMessage = new StringBuilder();
        userMessage.append(GENERATE_QUESTION_SYSTEM_MESSAGE).append("\n");
        userMessage.append(app.getAppName()).append(",\n");
        userMessage.append(app.getAppDesc()).append(",\n");
        userMessage.append(AppTypeEnum.getEnumByValue(app.getAppType()).getDesc() + "类").append(",\n");
        userMessage.append(questionNumber).append(",\n");
        userMessage.append(optionNumber);
        return userMessage.toString();
    }

    @PostMapping("/ai_generate")
    public BaseResponse<List<QuestionContentDTO>> aiGenerateQuestion(@RequestBody AiGenerateQuestionRequest aiGenerateQuestionRequest) {
        ThrowUtils.throwIf(aiGenerateQuestionRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取参数
        Long appId = aiGenerateQuestionRequest.getAppId();
        int questionNumber = aiGenerateQuestionRequest.getQuestionNumber();
        int optionNumber = aiGenerateQuestionRequest.getOptionNumber();
        // 获取应用信息
        App app = appService.getById(appId);
        // 判断应用是否为null
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 生成提示词
        String userMessage = getGenerateQuestionUserMessage(app, questionNumber, optionNumber);
        // ai生成
        String result = aiManager.doSyncRequest(userMessage, GENERATE_QUESTION_SYSTEM_MESSAGE);
        log.info("ai生成题目：{}", result);
        int start = result.indexOf("[");
        int end = result.lastIndexOf("]");
        String json = result.substring(start, end + 1);
        List<QuestionContentDTO> questionContentDTOList = JSONUtil.toList(json, QuestionContentDTO.class);
        return ResultUtils.success(questionContentDTOList);
    }


    @GetMapping("/ai_generate/sse")
    public SseEmitter aiGenerateSseQuestion(AiGenerateQuestionRequest aiGenerateQuestionRequest) {
        ThrowUtils.throwIf(aiGenerateQuestionRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取参数
        Long appId = aiGenerateQuestionRequest.getAppId();
        int questionNumber = aiGenerateQuestionRequest.getQuestionNumber();
        int optionNumber = aiGenerateQuestionRequest.getOptionNumber();
        // 获取应用信息
        App app = appService.getById(appId);
        // 判断应用是否为null
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 生成提示词
        String userMessage = getGenerateQuestionUserMessage(app, questionNumber, optionNumber);
        // sse连接对象
        SseEmitter sseEmitter = new SseEmitter(0L);
        // AI生成，sse返回
        Flowable<ModelData> modelDataFlowable = aiManager.doStreamRequest(userMessage, GENERATE_QUESTION_SYSTEM_MESSAGE);
        // 括号匹配计数器
        AtomicInteger counter = new AtomicInteger(0);
        StringBuffer stringBuffer = new StringBuffer();
        modelDataFlowable
                .observeOn(Schedulers.io())
                .map(modelData -> modelData.getChoices().get(0).getDelta().getContent())
                .map(message -> message.replaceAll("\\s", ""))
                .filter(StrUtil::isNotBlank)
                .flatMap(message -> {
                    List<Character> characterList = new ArrayList<>();
                    for (char c : message.toCharArray()) {
                        characterList.add(c);
                    }
                    return Flowable.fromIterable(characterList);
                })
                .doOnNext(c -> {
                    if (c == '{') {
                        counter.addAndGet(1);
                    }
                    if (counter.get() > 0) {
                        stringBuffer.append(c);
                    }
                    if (c == '}') {
                        counter.addAndGet(-1);
                        if (counter.get() == 0) {
                            // 拼接完成，返回给前端
                            sseEmitter.send(JSONUtil.toJsonStr(stringBuffer.toString()));
                            stringBuffer.setLength(0);
                        }
                    }
                })
                .doOnError((error) -> log.error("sse error", error))
                .doOnComplete(sseEmitter::complete)
                .subscribe();
        return sseEmitter;
    }

    // endregion


}