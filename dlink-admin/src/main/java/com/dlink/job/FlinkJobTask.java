/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.dlink.job;

import com.dlink.alert.AlertMsg;
import com.dlink.common.result.Result;
import com.dlink.context.SpringContextUtils;
import com.dlink.daemon.task.DaemonTask;
import com.dlink.daemon.task.DaemonTaskConfig;
import com.dlink.model.JobInstance;
import com.dlink.model.JobRestart;
import com.dlink.model.JobStatus;
import com.dlink.model.Savepoints;
import com.dlink.service.JobRestartService;
import com.dlink.service.SavepointsService;
import com.dlink.service.TaskService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@DependsOn("springContextUtils")
public class FlinkJobTask implements DaemonTask {

    private static final Logger log = LoggerFactory.getLogger(FlinkJobTask.class);

    private DaemonTaskConfig config;
    public static final String TYPE = "jobInstance";
    private static TaskService taskService;
    private static SavepointsService savepointsService;

    private static JobRestartService jobRestartService;
    private long preDealTime;

    private long diffTime = 120;

    private int maxRestartNum = 6;

    static {
        taskService = SpringContextUtils.getBean("taskServiceImpl", TaskService.class);
        savepointsService = SpringContextUtils.getBean("savepointsServiceImpl", SavepointsService.class);
        jobRestartService = SpringContextUtils.getBean("jobRestartServiceImpl", JobRestartService.class);
    }

    @Override
    public DaemonTask setConfig(DaemonTaskConfig config) {
        this.config = config;
        return this;
    }

    @Override
    public String getType() {
        return TYPE;
    }

//    @Override
//    public void dealTask() {
//        long gap = System.currentTimeMillis() - this.preDealTime;
//        if (gap < FlinkTaskConstant.TIME_SLEEP) {
//            try {
//                Thread.sleep(FlinkTaskConstant.TIME_SLEEP);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//        preDealTime = System.currentTimeMillis();
//        JobInstance jobInstance = taskService.refreshJobInstance(config.getId(), false);
//        if ((!JobStatus.isDone(jobInstance.getStatus())) || (Asserts.isNotNull(jobInstance.getFinishTime())
//                && Duration.between(jobInstance.getFinishTime(), LocalDateTime.now()).toMinutes() < 1)) {
//            DefaultThreadPool.getInstance().execute(this);
//        } else {
//            taskService.handleJobDone(jobInstance);
//            FlinkJobTaskPool.getInstance().remove(config.getId().toString());
//        }
//    }

    @Override
    public void dealTask() {
        JobInstance jobInstance = taskService.refreshJobInstance(config.getId(), true);
        boolean isFail = Objects.equals(JobStatus.FAILED.getValue(), jobInstance.getStatus())
                || Objects.equals(JobStatus.FAILING.getValue(), jobInstance.getStatus());
        if (!isFail) {
            return;
        }
        JobRestart latest = jobRestartService.getLatestByTaskId(jobInstance.getTaskId());
        log.info("-------- ?????????????????? {}", jobInstance);
        // ??????
        jobInstance.setFailedRestartCount(Objects.isNull(latest) ? 0 : latest.getRestartNum());
        taskService.handleJobDone(jobInstance);
        FlinkJobTaskPool.getInstance().remove(config.getId().toString());

        // ??????
        Savepoints latestSavepoint = savepointsService.getLatestSavepointByTaskId(jobInstance.getTaskId());
        if (Objects.nonNull(latestSavepoint) && StringUtils.isNotBlank(latestSavepoint.getPath())) {

            log.info("?????????????????? checkpoint ??????,{},{}", jobInstance.getName(), latestSavepoint.getPath());
            Result result = taskService.reOnLineTask(jobInstance.getTaskId(), latestSavepoint.getPath());
            Integer code = result.getCode();
            AlertMsg msg = new AlertMsg();
            int laterRestartNum = Objects.isNull(latest) ? 1 : latest.getRestartNum() + 1;
            if (code != 0) {
                msg.setRemark("??????????????????");
            } else {
                msg.setRemark("??????????????????");
                JobResult datas = (JobResult) result.getDatas();
                jobInstance = taskService.refreshJobInstance(datas.getJobInstanceId(), true);
            }
            JobRestart save = new JobRestart();
            save.setUpdateTime(LocalDateTime.now());
            save.setTaskName(jobInstance.getName());
            if (Objects.nonNull(latest)) {
                long between = ChronoUnit.MINUTES.between(latest.getUpdateTime(), LocalDateTime.now());
                if (latest.getIsProhibit() == 1 && between <= diffTime) {
                    log.warn("?????????????????????????????????taskName = {} ,????????? {} ???????????????",
                            jobInstance.getName(), diffTime - between);
                    return;
                }
                save.setTaskId(latest.getTaskId());
                save.setCreateTime(latest.getCreateTime());
                if (laterRestartNum == maxRestartNum) {
                    msg.setRemark(String.format("???????????????????????? %s ???,????????????????????? %s ??????", maxRestartNum, diffTime));
                    save.setIsProhibit(1);
                    save.setRestartNum(1);
                }else {
                    save.setIsProhibit(0);
                    save.setId(latest.getId());
                    save.setRestartNum(laterRestartNum);
                }
            } else {
                save.setTaskId(jobInstance.getTaskId());
                save.setCreateTime(LocalDateTime.now());
                save.setRestartNum(1);
                save.setIsProhibit(0);
            }

            if (Objects.nonNull(save.getId())) {
                jobRestartService.updateById(save);
            } else {
                jobRestartService.save(save);
            }
            msg.setFailedRestartCount(laterRestartNum);
            taskService.handleJobDone(jobInstance, msg);
            log.info("{}", result.getMsg());
        } else {
            log.info("?????????????????????checkpoint???{},{}", jobInstance.getTaskId(), latestSavepoint);
        }
    }
}
