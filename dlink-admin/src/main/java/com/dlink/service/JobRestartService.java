package com.dlink.service;

import com.dlink.db.service.ISuperService;
import com.dlink.model.JobRestart;

/**
 * @author liuwenyi
 * @date 2023/2/20
 **/
public interface JobRestartService extends ISuperService<JobRestart> {

    boolean saveJobRestart(JobRestart jobRestart);


    JobRestart getLatestByTaskId(Integer taskId);
}
