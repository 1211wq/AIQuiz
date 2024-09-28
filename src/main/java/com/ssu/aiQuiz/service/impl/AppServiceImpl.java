package com.ssu.aiQuiz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ssu.aiQuiz.common.ErrorCode;
import com.ssu.aiQuiz.constant.CommonConstant;
import com.ssu.aiQuiz.exception.ThrowUtils;
import com.ssu.aiQuiz.mapper.AppMapper;
import com.ssu.aiQuiz.model.dto.app.AppQueryRequest;
import com.ssu.aiQuiz.model.entity.App;
import com.ssu.aiQuiz.model.entity.User;
import com.ssu.aiQuiz.model.enums.AppScoringStategyEnum;
import com.ssu.aiQuiz.model.enums.AppTypeEnum;
import com.ssu.aiQuiz.model.enums.ReviewStatusEnum;
import com.ssu.aiQuiz.model.vo.AppVO;
import com.ssu.aiQuiz.service.IAppService;
import com.ssu.aiQuiz.service.UserService;
import com.ssu.aiQuiz.utils.SqlUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 应用 服务实现类
 * </p>
 *
 * @author ssu
 * @since 2024-09-05
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements IAppService {

    @Resource
    private UserService userService;

    @Override
    public void validApp(App app, boolean add) {
        ThrowUtils.throwIf(app == null, ErrorCode.PARAMS_ERROR);

        //获取对象信息
        Long id = app.getId();
        String appName = app.getAppName();
        String appDesc = app.getAppDesc();
        Integer appType = app.getAppType();
        Integer scoringStrategy = app.getScoringStrategy();
        Integer reviewStatus = app.getReviewStatus();

        // 校验
        // 添加数据校验规则
        if (add) {

            // 校验规则
            ThrowUtils.throwIf(StringUtils.isBlank(appName), ErrorCode.PARAMS_ERROR, "应用名称不能为空");
            ThrowUtils.throwIf(StringUtils.isBlank(appDesc), ErrorCode.PARAMS_ERROR, "应用描述不能为空");
            AppTypeEnum appTypeEnum = AppTypeEnum.getEnumByValue(appType);
            ThrowUtils.throwIf(appTypeEnum == null, ErrorCode.PARAMS_ERROR, "应用类型非法");
            AppScoringStategyEnum scoringStategyEnum = AppScoringStategyEnum.getEnumByValue(scoringStrategy);
            ThrowUtils.throwIf(scoringStategyEnum == null, ErrorCode.PARAMS_ERROR, "计分策略非法");
        }

        // 修改数据校验规则
        if (StringUtils.isNotBlank(appName)) {
            ThrowUtils.throwIf(appName.length() > 80, ErrorCode.PARAMS_ERROR, "应用名称不能超过80个字符");
        }
        if (reviewStatus != null) {
            ReviewStatusEnum reviewStatusEnum = ReviewStatusEnum.getEnumByValue(reviewStatus);
            ThrowUtils.throwIf(reviewStatusEnum == null, ErrorCode.PARAMS_ERROR, "审核状态非法");
        }
    }

    @Override
    public QueryWrapper<App> getQueryWrapper(AppQueryRequest appQueryRequest) {
        QueryWrapper<App> queryWrapper = new QueryWrapper<>();
        if (appQueryRequest == null) {
            return queryWrapper;
        }
        // 获取查询条件
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String appDesc = appQueryRequest.getAppDesc();
        String appIcon = appQueryRequest.getAppIcon();
        Integer appType = appQueryRequest.getAppType();
        Integer scoringStrategy = appQueryRequest.getScoringStrategy();
        Integer reviewStatus = appQueryRequest.getReviewStatus();
        String reviewMessage = appQueryRequest.getReviewMessage();
        Long reviewerId = appQueryRequest.getReviewerId();
        Long userId = appQueryRequest.getUserId();
        Long notId = appQueryRequest.getNotId();
        String searchText = appQueryRequest.getSearchText();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();

        // 添加查询条件
        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like("appName",searchText).or().like("appDesc", searchText));
        }

        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(appName), "appName", appName);
        queryWrapper.like(StringUtils.isNotBlank(appDesc), "appDesc", appDesc);
        queryWrapper.like(StringUtils.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);

        // 精确查询
        queryWrapper.eq(StringUtils.isNotBlank(appIcon), "appIcon", appIcon);
        queryWrapper.ne(ObjectUtil.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(appType), "appType", appType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(scoringStrategy), "scoringStrategy", scoringStrategy);
        queryWrapper.eq(ObjectUtils.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjectUtils.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);

        // 排序
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取AppVO分页数据
     *
     * @param appPage
     * @param request
     * @return
     */
    @Override
    public Page<AppVO> getAppVOPage(Page<App> appPage, HttpServletRequest request) {
        List<App> appList = appPage.getRecords();
        Page<AppVO> appVOPage = new Page<>(appPage.getCurrent(), appPage.getSize(), appPage.getTotal());
        if (CollUtil.isEmpty(appList)) {
            return appVOPage;
        }

        // 对象列表转换为封装对象列表
        List<AppVO> appVOList = appList.stream().map(app -> {
            return AppVO.objToVo(app);
        }).collect(Collectors.toList());

        // 关联查询用户信息
        Set<Long> userIdSet = appList.stream().map(App::getId).collect(Collectors.toSet());
        Map<Long, List<User>> map = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
        appVOList.forEach(appVO -> {
            Long userId = appVO.getUserId();
            User user = null;
            if (map.containsKey(userId)) {
                user = map.get(userId).get(0);
            }
            appVO.setUser(userService.getUserVO(user));
        });
        appVOPage.setRecords(appVOList);
        return appVOPage;
    }
}
