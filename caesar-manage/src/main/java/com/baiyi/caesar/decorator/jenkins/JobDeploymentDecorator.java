package com.baiyi.caesar.decorator.jenkins;

import com.baiyi.caesar.common.base.BuildType;
import com.baiyi.caesar.common.util.BeanCopierUtils;
import com.baiyi.caesar.common.util.TimeAgoUtils;
import com.baiyi.caesar.common.util.TimeUtils;
import com.baiyi.caesar.decorator.application.JobEngineDecorator;
import com.baiyi.caesar.domain.generator.caesar.*;
import com.baiyi.caesar.domain.vo.application.JobEngineVO;
import com.baiyi.caesar.domain.vo.build.BuildExecutorVO;
import com.baiyi.caesar.domain.vo.build.CdJobBuildVO;
import com.baiyi.caesar.domain.vo.build.DeploymentServerVO;
import com.baiyi.caesar.domain.vo.server.ServerVO;
import com.baiyi.caesar.domain.vo.user.UserVO;
import com.baiyi.caesar.service.aliyun.CsOssBucketService;
import com.baiyi.caesar.service.jenkins.*;
import com.baiyi.caesar.service.server.OcServerService;
import com.baiyi.caesar.service.user.OcUserService;
import com.google.common.base.Joiner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author baiyi
 * @Date 2020/8/28 10:18 上午
 * @Version 1.0
 */
@Component
public class JobDeploymentDecorator {

    @Resource
    private CsJobEngineService csJobEngineService;

    @Resource
    private JobEngineDecorator jobEngineDecorator;

    @Resource
    private CsJobBuildArtifactService csJobBuildArtifactService;

    @Resource
    private JobBuildArtifactDecorator jobBuildArtifactDecorator;

    @Resource
    private CsCiJobService csCiJobService;

    @Resource
    private CsCdJobService csCdJobService;

    @Resource
    private CsOssBucketService ossBucketService;

    @Resource
    private OcUserService ocUserService;

    @Resource
    private CsJobBuildExecutorService csJobBuildExecutorService;

    @Resource
    private OcServerService ocServerService;

    @Resource
    private CsJobBuildServerService csJobBuildServerService;

    public CdJobBuildVO.JobBuild decorator(CsCdJobBuild csCdJobBuild, Integer extend) {
        return decorator(BeanCopierUtils.copyProperties(csCdJobBuild, CdJobBuildVO.JobBuild.class), extend);
    }

    public CdJobBuildVO.JobBuild decorator(CdJobBuildVO.JobBuild jobBuild, Integer extend) {
        if (extend == 1) {
            // 装饰工作引擎
            CsJobEngine csCiJobEngine = csJobEngineService.queryCsJobEngineById(jobBuild.getJobEngineId());
            if (csCiJobEngine != null) {
                JobEngineVO.JobEngine jobEngine = jobEngineDecorator.decorator(BeanCopierUtils.copyProperties(csCiJobEngine, JobEngineVO.JobEngine.class));
                jobBuild.setJobEngine(jobEngine);

                String jobBuildUrl = Joiner.on("/").join(jobEngine.getJobUrl(), jobBuild.getEngineBuildNumber());
                jobBuild.setJobBuildUrl(jobBuildUrl);
            }

            CsCiJob csCiJob = getCsCiJobByJobBuild(jobBuild);
            CsOssBucket csOssBucket = ossBucketService.queryCsOssBucketById(csCiJob.getOssBucketId());
            List<CsJobBuildArtifact> artifacts = csJobBuildArtifactService.queryCsJobBuildArtifactByBuildId(BuildType.DEPLOYMENT.getType(), jobBuild.getId());
            jobBuild.setArtifacts(jobBuildArtifactDecorator.decorator(artifacts, csOssBucket));

            if (jobBuild.getStartTime() != null && jobBuild.getEndTime() != null) {
                long buildTime = jobBuild.getEndTime().getTime() - jobBuild.getStartTime().getTime();
                jobBuild.setBuildTime(TimeUtils.acqBuildTime(buildTime));
            }

            jobBuild.setExecutors(getBuildExecutorByBuildId(jobBuild.getId()));

            List<CsJobBuildServer> csJobBuildServers = csJobBuildServerService.queryCsJobBuildServerByBuildId(BuildType.DEPLOYMENT.getType(), jobBuild.getId());
            jobBuild.setServers(BeanCopierUtils.copyListProperties(csJobBuildServers, DeploymentServerVO.BuildServer.class));
        }
        // Ago
        jobBuild.setAgo(TimeAgoUtils.format(jobBuild.getStartTime()));
        if (!StringUtils.isEmpty(jobBuild.getUsername())) {
            OcUser ocUser = ocUserService.queryOcUserByUsername(jobBuild.getUsername());
            if (ocUser != null) {
                ocUser.setPassword("");
                jobBuild.setUser(BeanCopierUtils.copyProperties(ocUser, UserVO.User.class));
            }
        }
        return jobBuild;
    }

    private CsCiJob getCsCiJobByJobBuild(CdJobBuildVO.JobBuild jobBuild) {
        CsCdJob csCdJob = csCdJobService.queryCsCdJobById(jobBuild.getCdJobId());
        return csCiJobService.queryCsCiJobById(csCdJob.getCiJobId());
    }

    public List<BuildExecutorVO.BuildExecutor> getBuildExecutorByBuildId(int buildId) {
        List<CsJobBuildExecutor> executors = csJobBuildExecutorService.queryCsJobBuildExecutorByBuildId(BuildType.DEPLOYMENT.getType(), buildId);
        return executors.stream().map(e -> {
                    BuildExecutorVO.BuildExecutor buildExecutor = BeanCopierUtils.copyProperties(e, BuildExecutorVO.BuildExecutor.class);
                    OcServer ocServer = ocServerService.queryOcServerByIp(buildExecutor.getPrivateIp());
                    if (ocServer != null)
                        buildExecutor.setServer(BeanCopierUtils.copyProperties(ocServer, ServerVO.Server.class));
                    return buildExecutor;
                }
        ).collect(Collectors.toList());
    }
}
