package com.baiyi.caesar.facade;

import com.baiyi.caesar.domain.BusinessWrapper;
import com.baiyi.caesar.domain.DataTable;
import com.baiyi.caesar.domain.param.application.CdJobParam;
import com.baiyi.caesar.domain.param.application.CiJobParam;
import com.baiyi.caesar.domain.param.jenkins.JenkinsInstanceParam;
import com.baiyi.caesar.domain.param.jenkins.JobTplParam;
import com.baiyi.caesar.domain.vo.application.CdJobVO;
import com.baiyi.caesar.domain.vo.application.CiJobVO;
import com.baiyi.caesar.domain.vo.jenkins.JenkinsInstanceVO;
import com.baiyi.caesar.domain.vo.jenkins.JenkinsJobVO;
import com.baiyi.caesar.domain.vo.jenkins.JobTplVO;

import java.util.List;

/**
 * @Author baiyi
 * @Date 2020/7/17 5:48 下午
 * @Version 1.0
 */
public interface JenkinsFacade {

    DataTable<JenkinsInstanceVO.Instance> queryJenkinsInstancePage(JenkinsInstanceParam.JenkinsInstancePageQuery pageQuery);

    BusinessWrapper<Boolean>  setJenkinsInstanceActive(int id);

    BusinessWrapper<Boolean> addJenkinsInstance(JenkinsInstanceVO.Instance instance);

    BusinessWrapper<Boolean> updateJenkinsInstance(JenkinsInstanceVO.Instance instance);

    BusinessWrapper<Boolean> deleteJenkinsInstanceById(int id);

    DataTable<JobTplVO.JobTpl> queryJobTplPage(JobTplParam.JobTplPageQuery pageQuery);

    BusinessWrapper<Boolean> addJobTpl(JobTplVO.JobTpl jobTpl);

    BusinessWrapper<Boolean> updateJobTpl(JobTplVO.JobTpl jobTpl);

    BusinessWrapper<Boolean> writeJobTpl(JobTplVO.JobTpl jobTpl);

    BusinessWrapper<Boolean> deleteJobTplById(int id);

    List<JenkinsJobVO.Job> queryJobTplByInstanceId(int instanceId);

    BusinessWrapper<Boolean> readJobTplById(int id);

    DataTable<CiJobVO.CiJob> queryCiJobTplPage(CiJobParam.CiJobTplPageQuery pageQuery);

    DataTable<CdJobVO.CdJob> queryCdJobTplPage(CdJobParam.CdJobTplPageQuery pageQuery);

    BusinessWrapper<Boolean> upgradeCiJobTplByJobId(int jobId);

    BusinessWrapper<Boolean> upgradeCdJobTplByJobId(int jobId);
}
