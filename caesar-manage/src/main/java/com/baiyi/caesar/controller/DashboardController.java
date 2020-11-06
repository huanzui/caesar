package com.baiyi.caesar.controller;

import com.baiyi.caesar.domain.HttpResult;
import com.baiyi.caesar.domain.vo.dashboard.DashboardVO;
import com.baiyi.caesar.facade.DashboardFacade;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Author baiyi
 * @Date 2020/11/5 3:11 下午
 * @Version 1.0
 */
@RestController
@RequestMapping("/dashboard")
@Api(tags = "仪表盘")
public class DashboardController {

    @Resource
    private DashboardFacade dashboardFacade;

    @ApiOperation(value = "查询顶部卡片")
    @GetMapping(value = "/top/card/query", produces = MediaType.APPLICATION_JSON_VALUE)
    public HttpResult<DashboardVO.TopCard> queryTopCard() {
        return new HttpResult<>(dashboardFacade.queryTopCard());
    }

    @ApiOperation(value = "查询最后任务")
    @GetMapping(value = "/latest/tasks/query", produces = MediaType.APPLICATION_JSON_VALUE)
    public HttpResult<DashboardVO.LatestTasks> queryLatestTasks() {
        return new HttpResult<>(dashboardFacade.queryLatestTasks());
    }
}
