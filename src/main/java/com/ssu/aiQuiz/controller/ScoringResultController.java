package com.ssu.aiQuiz.controller;


import cn.hutool.core.bean.BeanUtil;
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
import com.ssu.aiQuiz.model.dto.scoringResult.ScoringResultAddRequest;
import com.ssu.aiQuiz.model.dto.scoringResult.ScoringResultEditRequest;
import com.ssu.aiQuiz.model.dto.scoringResult.ScoringResultQueryRequest;
import com.ssu.aiQuiz.model.dto.scoringResult.ScoringResultUpdateRequest;
import com.ssu.aiQuiz.model.entity.ScoringResult;
import com.ssu.aiQuiz.model.entity.User;
import com.ssu.aiQuiz.model.vo.ScoringResultVO;
import com.ssu.aiQuiz.service.IScoringResultService;
import com.ssu.aiQuiz.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>
 * 评分结果 前端控制器
 * </p>
 *
 * @author ssu
 * @since 2024-09-05
 */
@RestController
@RequestMapping("/scoring-result")
public class ScoringResultController {

    @Resource
    private IScoringResultService scoringResultService;

    @Resource
    private UserService userService;

    /**
     * 创建评分结果
     *
     * @param scoringResultAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addScoringResult(@RequestBody ScoringResultAddRequest scoringResultAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(scoringResultAddRequest == null, ErrorCode.PARAMS_ERROR);

        // 将dto转换为实体
        ScoringResult scoringResult = BeanUtil.copyProperties(scoringResultAddRequest, ScoringResult.class);
        String jsonStr = JSONUtil.toJsonStr(scoringResultAddRequest.getResultProp());
        scoringResult.setResultProp(jsonStr);

        // 数据校验
        scoringResultService.validScoringResult(scoringResult, true);

        // 填充默认值
        User loginUser = userService.getLoginUser(request);
        scoringResult.setUserId(loginUser.getId());

        // 写入数据库
        boolean result = scoringResultService.save(scoringResult);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 返回id
        Long newScoringResultId = scoringResult.getId();
        return ResultUtils.success(newScoringResultId);
    }

    /**
     * 删除评分结果
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteScoringResult(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        Long id = deleteRequest.getId();

        // 判断是否存在
        ScoringResult oldScoringResult = scoringResultService.getById(id);
        ThrowUtils.throwIf(oldScoringResult == null, ErrorCode.NOT_FOUND_ERROR);

        // 仅本人或管理员可删除
        if (!oldScoringResult.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 删除
        boolean result = scoringResultService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新评分结果（仅管理员可用）
     *
     * @param scoringResultUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateScoringResult(@RequestBody ScoringResultUpdateRequest scoringResultUpdateRequest) {
        if (scoringResultUpdateRequest == null || scoringResultUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将dto转换为实体
        ScoringResult scoringResult = BeanUtil.copyProperties(scoringResultUpdateRequest, ScoringResult.class);
        String jsonStr = JSONUtil.toJsonStr(scoringResultUpdateRequest.getResultProp());
        scoringResult.setResultProp(jsonStr);

        // 数据校验
        scoringResultService.validScoringResult(scoringResult, false);

        // 判断是否存在
        ScoringResult oldScoringResult = scoringResultService.getById(scoringResult.getId());
        ThrowUtils.throwIf(oldScoringResult == null, ErrorCode.NOT_FOUND_ERROR);

        // 更新
        boolean result = scoringResultService.updateById(scoringResult);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取评分结果（封装类）
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<ScoringResultVO> getScoringResultVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        // 查询数据库
        ScoringResult scoringResult = scoringResultService.getById(id);
        ThrowUtils.throwIf(scoringResult == null, ErrorCode.NOT_FOUND_ERROR);

        // 将查询结果封装
        ScoringResultVO scoringResultVO = scoringResultService.getScoringResultVO(scoringResult, request);
        return ResultUtils.success(scoringResultVO);
    }

    /**
     * 分页获取评分结果列表（仅管理员可用）
     *
     * @param scoringResultQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ScoringResult>> listScoringResultByPage(@RequestBody ScoringResultQueryRequest scoringResultQueryRequest) {
        long current = scoringResultQueryRequest.getCurrent();
        long size = scoringResultQueryRequest.getPageSize();

        // 查询数据库
        Page<ScoringResult> scoringResultPage = scoringResultService.page(new Page<>(current, size),
                scoringResultService.getQueryWrapper(scoringResultQueryRequest));
        return ResultUtils.success(scoringResultPage);
    }

    /**
     * 分页获取评分结果列表（封装类）
     *
     * @param scoringResultQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<ScoringResultVO>> listScoringResultVOByPage(@RequestBody ScoringResultQueryRequest scoringResultQueryRequest,
                                                                         HttpServletRequest request) {
        long current = scoringResultQueryRequest.getCurrent();
        long size = scoringResultQueryRequest.getPageSize();

        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        // 查询时数据库
        Page<ScoringResult> scoringResultPage = scoringResultService.page(new Page<>(current, size),
                scoringResultService.getQueryWrapper(scoringResultQueryRequest));
        // 将查询结果封装
        Page<ScoringResultVO> scoringResultVOPage = scoringResultService.getScoringResultVOPage(scoringResultPage, request);
        return ResultUtils.success(scoringResultVOPage);
    }

    /**
     * 分页获取当前登录用户创建的评分结果列表
     *
     * @param scoringResultQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<ScoringResultVO>> listMyScoringResultVOByPage(@RequestBody ScoringResultQueryRequest scoringResultQueryRequest,
                                                                           HttpServletRequest request) {
        ThrowUtils.throwIf(scoringResultQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = scoringResultQueryRequest.getCurrent();
        long size = scoringResultQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<ScoringResult> scoringResultPage = scoringResultService.page(new Page<>(current, size),
                scoringResultService.getQueryWrapper(scoringResultQueryRequest));
        // 获取封装类
        return ResultUtils.success(scoringResultService.getScoringResultVOPage(scoringResultPage, request));

    }

    /**
     * 编辑评分结果（给用户使用）
     *
     * @param scoringResultEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editScoringResult(@RequestBody ScoringResultEditRequest scoringResultEditRequest, HttpServletRequest request) {
        if (scoringResultEditRequest == null || scoringResultEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 将dto转换为实体
        ScoringResult scoringResult = BeanUtil.copyProperties(scoringResultEditRequest, ScoringResult.class);
        List<String> resultProp = scoringResultEditRequest.getResultProp();
        scoringResult.setResultProp(JSONUtil.toJsonStr(resultProp));

        // 校验数据
        scoringResultService.validScoringResult(scoringResult, false);
        User loginUser = userService.getLoginUser(request);

        // 判断是否存在
        Long id = scoringResultEditRequest.getId();
        ScoringResult oldScoringResult = scoringResultService.getById(id);
        ThrowUtils.throwIf(oldScoringResult == null, ErrorCode.NOT_FOUND_ERROR);

        // 仅本人或者管理员可进行操作
        if (!oldScoringResult.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 更新数据库
        boolean result = scoringResultService.updateById(scoringResult);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }
}
