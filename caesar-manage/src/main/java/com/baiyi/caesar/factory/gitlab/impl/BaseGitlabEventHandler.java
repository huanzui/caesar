package com.baiyi.caesar.factory.gitlab.impl;

import com.baiyi.caesar.builder.gitlab.GitlabSystemHookBuilder;
import com.baiyi.caesar.common.util.GitlabTokenUtils;
import com.baiyi.caesar.domain.generator.caesar.CsGitlabGroup;
import com.baiyi.caesar.domain.generator.caesar.CsGitlabInstance;
import com.baiyi.caesar.domain.generator.caesar.CsGitlabProject;
import com.baiyi.caesar.domain.generator.caesar.CsGitlabSystemHook;
import com.baiyi.caesar.domain.vo.gitlab.GitlabHooksVO;
import com.baiyi.caesar.facade.GitlabFacade;
import com.baiyi.caesar.factory.gitlab.GitlabEventHandlerFactory;
import com.baiyi.caesar.factory.gitlab.IGitlabEventHandler;
import com.baiyi.caesar.gitlab.handler.GitlabGroupHandler;
import com.baiyi.caesar.gitlab.handler.GitlabProjectHandler;
import com.baiyi.caesar.service.gitlab.CsGitlabGroupService;
import com.baiyi.caesar.service.gitlab.CsGitlabInstanceService;
import com.baiyi.caesar.service.gitlab.CsGitlabProjectService;
import com.baiyi.caesar.service.gitlab.CsGitlabSystemHookService;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabProject;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;

/**
 * @Author baiyi
 * @Date 2020/12/21 4:24 下午
 * @Version 1.0
 */
@Slf4j
@Component
public abstract class BaseGitlabEventHandler implements IGitlabEventHandler, InitializingBean {

    @Resource
    private GitlabProjectHandler gitlabProjectHandler;

    @Resource
    private GitlabGroupHandler gitlabGroupHandler;

    @Resource
    private CsGitlabInstanceService csGitlabInstanceService;

    @Resource
    protected CsGitlabProjectService csGitlabProjectService;

    @Resource
    protected CsGitlabGroupService csGitlabGroupService;

    @Resource
    protected CsGitlabSystemHookService csGitlabSystemHookService;

    @Resource
    protected GitlabFacade gitlabFacade;

    /**
     * 消费项目事件
     *
     * @param systemHook
     */
    protected void consumeProjectEvent(GitlabHooksVO.SystemHook systemHook) {
        CsGitlabInstance csGitlabInstance = getGitlabInstance();
        if (csGitlabInstance == null) return;
        CsGitlabSystemHook csGitlabSystemHook = recordSystemHook(csGitlabInstance, systemHook);
        GitlabProject gitlabProject = getProject(csGitlabInstance, systemHook.getProjectId());
        if (gitlabProject == null)
            return;
        updateProjectEvents(csGitlabInstance, gitlabProject);
        updateConsumed(csGitlabSystemHook);
    }

    private void updateConsumed(CsGitlabSystemHook csGitlabSystemHook) {
        csGitlabSystemHook.setIsConsumed(true);
        csGitlabSystemHookService.updateCsGitlabSystemHook(csGitlabSystemHook);
    }

    private CsGitlabSystemHook recordSystemHook(CsGitlabInstance csGitlabInstance, GitlabHooksVO.SystemHook systemHook) {
        CsGitlabSystemHook csGitlabSystemHook = GitlabSystemHookBuilder.build(csGitlabInstance, systemHook);
        csGitlabSystemHookService.addCsGitlabSystemHook(csGitlabSystemHook);
        return csGitlabSystemHook;
    }

    /**
     * 消费群组事件
     *
     * @param systemHook
     */
    protected void consumeGroupEvent(GitlabHooksVO.SystemHook systemHook) {
        CsGitlabInstance csGitlabInstance = getGitlabInstance();
        if (csGitlabInstance == null) return;
        CsGitlabSystemHook csGitlabSystemHook = recordSystemHook(csGitlabInstance, systemHook);
        GitlabGroup gitlabGroup = getGroup(csGitlabInstance, systemHook.getGroupId());
        if (gitlabGroup == null)
            return;
        updateGroupEvents(csGitlabInstance, gitlabGroup);
        updateConsumed(csGitlabSystemHook);
    }

    protected void updateProjectEvents(CsGitlabInstance csGitlabInstance, GitlabProject gitlabProject) {
        CsGitlabProject csGitlabProject = csGitlabProjectService.queryCsGitlabProjectByUniqueKey(csGitlabInstance.getId(), gitlabProject.getId());
        Map<Integer, CsGitlabProject> projectMap = Maps.newHashMap();
        projectMap.put(csGitlabProject.getProjectId(), csGitlabProject);
        gitlabFacade.saveGitlabProject(csGitlabInstance, gitlabProject, projectMap);
    }

    protected void updateGroupEvents(CsGitlabInstance csGitlabInstance, GitlabGroup gitlabGroup) {
        CsGitlabGroup csGitlabGroup = csGitlabGroupService.queryCsGitlabGroupByUniqueKey(csGitlabInstance.getId(), gitlabGroup.getId());
        Map<Integer, CsGitlabGroup> groupMap = Maps.newHashMap();
        groupMap.put(csGitlabGroup.getGroupId(), csGitlabGroup);
        gitlabFacade.saveGitlabGroup(csGitlabInstance, gitlabGroup, groupMap);
    }

    protected GitlabProject getProject(CsGitlabInstance csGitlabInstance, Integer projectId) {
        try {
            return gitlabProjectHandler.getProject(csGitlabInstance.getName(), projectId);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected GitlabGroup getGroup(CsGitlabInstance csGitlabInstance, Integer groupId) {
        try {
            return gitlabGroupHandler.getGroup(csGitlabInstance.getName(), groupId);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected CsGitlabInstance getGitlabInstance() {
        String token = GitlabTokenUtils.getToken();
        if (StringUtils.isEmpty(token))
            return null;
        return csGitlabInstanceService.queryCsGitlabInstanceByToken(token);
    }

    /**
     * 注册
     */
    @Override
    public void afterPropertiesSet() {
        GitlabEventHandlerFactory.register(this);
    }
}
