package com.dlink.service.impl;

import com.dlink.db.service.impl.SuperServiceImpl;
import com.dlink.mapper.JobRestartMapper;
import com.dlink.model.JobRestart;
import com.dlink.service.JobRestartService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author liuwenyi
 * @date 2023/2/20
 **/
@Service
public class JobRestartServiceImpl extends SuperServiceImpl<JobRestartMapper, JobRestart> implements JobRestartService {

    @Override
    public boolean saveJobRestart(JobRestart jobRestart) {
        if (Objects.isNull(jobRestart)) {
            return false;
        }
        jobRestart.setUpdateTime(LocalDateTime.now());
        return this.save(jobRestart);
    }

    @Override
    public JobRestart getLatestByTaskId(Integer taskId) {
        if (Objects.isNull(taskId)) {
            return null;
        }
        return baseMapper.selectLatestByTaskId(taskId);
    }


}
