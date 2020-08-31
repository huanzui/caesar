package com.baiyi.caesar.dingtalk.impl;

import com.baiyi.caesar.common.base.NoticePhase;
import com.baiyi.caesar.common.base.NoticeType;
import com.baiyi.caesar.common.util.BeetlUtils;
import com.baiyi.caesar.dingtalk.DingtalkNotifyFactory;
import com.baiyi.caesar.dingtalk.IDingtalkNotify;
import com.baiyi.caesar.dingtalk.config.DingtalkConfig;
import com.baiyi.caesar.dingtalk.content.DingtalkContent;
import com.baiyi.caesar.dingtalk.handler.DingtalkHandler;
import com.baiyi.caesar.dingtalk.util.AtUserUtils;
import com.baiyi.caesar.domain.DataTable;
import com.baiyi.caesar.domain.generator.caesar.CsDingtalk;
import com.baiyi.caesar.domain.generator.caesar.CsDingtalkTemplate;
import com.baiyi.caesar.domain.generator.caesar.OcEnv;
import com.baiyi.caesar.domain.generator.caesar.OcUser;
import com.baiyi.caesar.domain.param.user.UserParam;
import com.baiyi.caesar.jenkins.context.BuildJobContext;
import com.baiyi.caesar.jenkins.context.DeploymentJobContext;
import com.baiyi.caesar.service.dingtalk.CsDingtalkService;
import com.baiyi.caesar.service.dingtalk.CsDingtalkTemplateService;
import com.baiyi.caesar.service.env.OcEnvService;
import com.baiyi.caesar.service.user.OcUserService;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @Author baiyi
 * @Date 2020/8/14 5:13 下午
 * @Version 1.0
 */
@Slf4j
public abstract class BaseDingtalkNotify implements IDingtalkNotify, InitializingBean {

    public static final String VERSION_NAME = "versionName";

    @Resource
    protected CsDingtalkTemplateService csDingtalkTemplateService;

    @Resource
    private OcEnvService ocEnvService;

    @Resource
    private OcUserService ocUserService;

    @Resource
    private DingtalkHandler dingtalkHandler;

    @Resource
    private DingtalkConfig dingtalkConfig;

    @Resource
    private CsDingtalkService csDingtalkService;

    @Resource
    private StringEncryptor stringEncryptor;

    @Override
    public void doNotify(int noticePhase, BuildJobContext context) {
        int noticeType = NoticeType.BUILD.getType();
        CsDingtalkTemplate csDingtalkTemplate = acqDingtalkTemplateByNoticeType(noticeType, noticePhase);
        if (csDingtalkTemplate == null) {
            log.error("钉钉通知模版未配置, buildId");
            return;
        }
        // 模版变量
        Map<String, Object> contentMap = acqTemplateContent(noticeType, noticePhase, context);
        try {
            CsDingtalk csDingtalk = csDingtalkService.queryCsDingtalkById(context.getCsCiJob().getDingtalkId());
            DingtalkContent dingtalkContent = DingtalkContent.builder()
                    .msg(renderTemplate(csDingtalkTemplate, contentMap))
                    .webHook(dingtalkConfig.getWebHook(stringEncryptor.decrypt(csDingtalk.getDingtalkToken())))
                    .build();
            dingtalkHandler.doNotify(dingtalkContent);
        } catch (IOException e) {
        }
    }

    @Override
    public void doNotify(int noticePhase, DeploymentJobContext context) {
        int noticeType = NoticeType.DEPLOYMENT.getType();
    }

    /**
     * 取模版内容
     *
     * @param noticeType
     * @param context
     * @return
     */
    protected Map<String, Object> acqTemplateContent(int noticeType, int noticePhase, BuildJobContext context) {
        Map<String, Object> contentMap = Maps.newHashMap();

        OcEnv ocEnv = ocEnvService.queryOcEnvByType(context.getCsCiJob().getEnvType());
        OcUser ocUser = ocUserService.queryOcUserByUsername(context.getJobBuild().getUsername());
        contentMap.put("applicationName", context.getCsApplication().getApplicationKey()); // 应用名称只显示key
        contentMap.put("jobName", context.getCsCiJob().getJobKey()); // 任务名称只显示key
        contentMap.put("buildPhase", noticePhase == NoticePhase.START.getType() ? "构建开始" : "构建结束");
        contentMap.put("envName", ocEnv != null ? ocEnv.getEnvName() : "default");
        contentMap.put("displayName", ocUser != null ? ocUser.getDisplayName() : context.getJobBuild().getUsername());
        String consolerUrl = Joiner.on("/").join(context.getJobBuild().getJobBuildUrl(), "console");
        contentMap.put("consoleUrl", consolerUrl); // console日志url
        contentMap.put("buildNumber", context.getJobBuild().getJobBuildNumber()); // 构建编号
        contentMap.put("branch", context.getJobBuild().getBranch()); // 构建分支
        contentMap.put("commit", context.getJobBuild().getCommit().substring(0, 7)); // 取7位commit
        contentMap.put("users", acqAtUsers(ocUser, context));
        return contentMap;
    }

    /**
     * 取通知用户
     *
     * @param ocUser
     * @param jobBuildContext
     * @return
     */
    private List<OcUser> acqAtUsers(OcUser ocUser, BuildJobContext jobBuildContext) {
        List<OcUser> users = Lists.newArrayList();
        if (jobBuildContext.getCsCiJob().getAtAll()) {
            UserParam.UserIncludeApplicationPageQuery pageQuery = new UserParam.UserIncludeApplicationPageQuery();
            pageQuery.setApplicationId(jobBuildContext.getCsApplication().getId());
            pageQuery.setPage(1);
            pageQuery.setLength(20);
            DataTable<OcUser> dataTable = ocUserService.queryApplicationIncludeUserParam(pageQuery);
            users = AtUserUtils.acqAtUsers(ocUser, dataTable.getData());
        } else {
            users.add(ocUser);
        }
        return users;
    }

    /**
     * 渲染模版
     *
     * @param csDingtalkTemplate
     * @param contentMap
     * @return
     * @throws IOException
     */
    private String renderTemplate(CsDingtalkTemplate csDingtalkTemplate, Map<String, Object> contentMap) throws IOException {
        return BeetlUtils.renderTemplate(csDingtalkTemplate.getNoticeTemplate(), contentMap);
    }

    private CsDingtalkTemplate acqDingtalkTemplateByNoticeType(int noticeType, int noticePhase) {
        return csDingtalkTemplateService.queryCsDingtalkTemplateByUniqueKey(getKey(), noticeType, noticePhase);
    }

    /**
     * 注册
     */
    @Override
    public void afterPropertiesSet() {
        DingtalkNotifyFactory.register(this);
    }
}