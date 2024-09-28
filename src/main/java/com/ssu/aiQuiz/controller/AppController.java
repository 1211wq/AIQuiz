package com.ssu.aiQuiz.controller;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ssu.aiQuiz.annotation.AuthCheck;
import com.ssu.aiQuiz.common.*;
import com.ssu.aiQuiz.constant.UserConstant;
import com.ssu.aiQuiz.exception.BusinessException;
import com.ssu.aiQuiz.exception.ThrowUtils;
import com.ssu.aiQuiz.model.dto.app.AppAddRequest;
import com.ssu.aiQuiz.model.dto.app.AppEditRequest;
import com.ssu.aiQuiz.model.dto.app.AppQueryRequest;
import com.ssu.aiQuiz.model.dto.app.AppUpdateRequest;
import com.ssu.aiQuiz.model.entity.App;
import com.ssu.aiQuiz.model.entity.User;
import com.ssu.aiQuiz.model.enums.ReviewStatusEnum;
import com.ssu.aiQuiz.model.vo.AppVO;
import com.ssu.aiQuiz.model.vo.UserVO;
import com.ssu.aiQuiz.service.IAppService;
import com.ssu.aiQuiz.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * <p>
 * 应用 前端控制器
 * </p>
 *
 * @author ssu
 * @since 2024-09-05
 */
@RestController
@RequestMapping("/app")
@Slf4j
public class AppController {

    @Resource
    private IAppService appService;

    @Resource
    private UserService userService;

    /**
     * 创建应用
     *
     * @param appAddRequest 应用信息
     * @param request       请求信息
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);

        // dto转换po
        App app = BeanUtil.copyProperties(appAddRequest, App.class);

        // 数据校验
        appService.validApp(app, true);

        // 填充默认值
        User loginUser = userService.getLoginUser(request);
        app.setUserId(loginUser.getId());
        app.setReviewStatus(ReviewStatusEnum.REVIEWING.getValue());

        // 写入数据库
        boolean result = appService.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 返回新写入的数据
        Long newAppId = app.getId();
        return ResultUtils.success(newAppId);
    }

    /**
     * 删除应用
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        Long id = deleteRequest.getId();

        // 判断是否存在
        App oldId = appService.getById(id);
        ThrowUtils.throwIf(oldId == null, ErrorCode.NOT_FOUND_ERROR);

        // 本人或管理员可删除
        if (!oldId.getUserId().equals(user.getId()) && !user.getUserRole().equals("admin")) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 写入数据库
        boolean result = appService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新应用（仅管理员可用）
     *
     * @param appUpdateRequest 应用信息
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest) {
        if (appUpdateRequest == null || appUpdateRequest.getId() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // dto转换为实体
        App app = BeanUtil.copyProperties(appUpdateRequest, App.class);

        // 数据校验
        appService.validApp(app, false);

        // 判断是否存在
        Long id = appUpdateRequest.getId();
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);

        // 写入数据库
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据id获取应用
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 转换为封装类
        UserVO userVO = userService.getUserVO(userService.getById(app.getUserId()));
        AppVO appVO = AppVO.objToVo(app);
        appVO.setUser(userVO);
        return ResultUtils.success(appVO);
    }

    /**
     * 分页获取应用列表（仅管理员可用）
     *
     * @param appQueryRequest 查询信息
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<App>> listAppByPage(@RequestBody AppQueryRequest appQueryRequest) {
        long current = appQueryRequest.getCurrent();
        long size = appQueryRequest.getPageSize();
        // 查询数据库
        Page<App> appPage = appService.page(new Page<>(current, size), appService.getQueryWrapper(appQueryRequest));
        return ResultUtils.success(appPage);
    }

    /**
     * 分页查询获取应用列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<AppVO>> listAppVOByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        long current = appQueryRequest.getCurrent();
        long size = appQueryRequest.getPageSize();

        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        // 只能查看过审的应用
        appQueryRequest.setReviewStatus(ReviewStatusEnum.PASS.getValue());

        // 查询数据库
        Page<App> appPage = appService.page(new Page<>(current, size),
                appService.getQueryWrapper(appQueryRequest));
        return ResultUtils.success(appService.getAppVOPage(appPage, request));
    }

    /**
     * 分页获取当前用户创建的应用列表
     *
     * @param appQueryRequest 查询信息
     * @param request
     * @return
     */
    @PostMapping("/my//list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);

        // 补充查询条件，只查当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        appQueryRequest.setUserId(loginUser.getId());
        long current = appQueryRequest.getCurrent();
        long size = appQueryRequest.getPageSize();

        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        // 查询数据库
        Page<App> appPage = appService.page(new Page<>(current, size),
                appService.getQueryWrapper(appQueryRequest));
        return ResultUtils.success(appService.getAppVOPage(appPage, request));
    }

    /**
     * 编辑应用（给用户使用）
     *
     * @param appEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editApp(@RequestBody AppEditRequest appEditRequest, HttpServletRequest request) {
        if (appEditRequest == null || appEditRequest.getId() <= 0) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }

        // 将dto转换为实体类
        App app = BeanUtil.copyProperties(appEditRequest, App.class);

        // 校验数据
        appService.validApp(app, false);
        User loginUser = userService.getLoginUser(request);

        // 判断应用是否存在
        Long id = appEditRequest.getId();
        App oldId = appService.getById(id);
        ThrowUtils.throwIf(oldId == null, ErrorCode.NOT_FOUND_ERROR);

        // 只有当前用户或者管理员才能进行操作
        if (!oldId.getUserId().equals(loginUser.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 重置审核状态
        app.setReviewStatus(ReviewStatusEnum.REVIEWING.getValue());

        // 更新数据
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);
        return ResultUtils.success(true);
    }


    /**
     * 应用审核
     *
     * @param reviewRequest
     * @param request
     * @return
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doAppReview(@RequestBody ReviewRequest reviewRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(reviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = reviewRequest.getId();
        Integer reviewStatus = reviewRequest.getReviewStatus();

        // 校验
        ReviewStatusEnum reviewStatusEnum = ReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id == null || reviewStatusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 判断审核应用是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);

        // 已是该状态
        if (oldApp.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }

        // 更新审核状态
        User loginUser = userService.getLoginUser(request);
        App app = new App();
        app.setId(id);
        app.setReviewStatus(reviewStatus);
        app.setReviewTime(new Date());
        app.setReviewId(loginUser.getId());
        app.setReviewMessage(reviewRequest.getReviewMessage());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);
        return ResultUtils.success(true);
    }


}

