package com.ssu.aiQuiz.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ssu.aiQuiz.model.dto.app.AppQueryRequest;
import com.ssu.aiQuiz.model.entity.App;
import com.ssu.aiQuiz.model.vo.AppVO;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 * 应用 服务类
 * </p>
 *
 * @author ssu
 * @since 2024-09-05
 */
public interface IAppService extends IService<App> {

    void validApp(App app, boolean add);

    QueryWrapper<App> getQueryWrapper(AppQueryRequest appQueryRequest);

    Page<AppVO> getAppVOPage(Page<App> appPage, HttpServletRequest request);
}
