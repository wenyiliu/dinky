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

package com.dlink.init;

import com.dlink.assertion.Asserts;
import com.dlink.context.TenantContextHolder;
import com.dlink.daemon.constant.FlinkTaskConstant;
import com.dlink.daemon.task.DaemonTask;
import com.dlink.daemon.task.DaemonTaskConfig;
import com.dlink.function.pool.UdfCodePool;
import com.dlink.job.FlinkJobTask;
import com.dlink.model.JobInstance;
import com.dlink.model.Tenant;
import com.dlink.scheduler.client.ProjectClient;
import com.dlink.scheduler.config.DolphinSchedulerProperties;
import com.dlink.scheduler.exception.SchedulerException;
import com.dlink.scheduler.model.Project;
import com.dlink.service.JobInstanceService;
import com.dlink.service.SysConfigService;
import com.dlink.service.TaskService;
import com.dlink.service.TenantService;
import com.dlink.utils.UDFUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * SystemInit
 *
 * @author wenmo
 * @since 2021/11/18
 **/
@Component
@Order(value = 1)
public class SystemInit implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SystemInit.class);

    @Autowired
    private ProjectClient projectClient;
    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private JobInstanceService jobInstanceService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private DolphinSchedulerProperties dolphinSchedulerProperties;

    private static Project project;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<Tenant> tenants = tenantService.list();
        sysConfigService.initSysConfig();
        for (Tenant tenant : tenants) {
            taskService.initDefaultFlinkSQLEnv(tenant.getId());
        }
        initTaskMonitor();
        initDolphinScheduler();
        registerUDF();
    }

//    /**
//     * init task monitor
//     */
//    private void initTaskMonitor() {
//        List<JobInstance> jobInstances = jobInstanceService.listJobInstanceActive();
//        List<DaemonTaskConfig> configList = new ArrayList<>();
//        for (JobInstance jobInstance : jobInstances) {
//            configList.add(new DaemonTaskConfig(FlinkJobTask.TYPE, jobInstance.getId()));
//        }
//        log.info("Number of tasks started: " + configList.size());
//        DaemonFactory.start(configList);
//    }

    /**
     * init DolphinScheduler
     */
    private void initDolphinScheduler() {
        if (dolphinSchedulerProperties.isEnabled()) {
            try {
                project = projectClient.getDinkyProject();
                if (Asserts.isNull(project)) {
                    project = projectClient.createDinkyProject();
                }
            } catch (Exception e) {
                log.error("Error in DolphinScheduler: {}", e);
            }
        }
    }

    /**
     * get dolphinscheduler's project
     *
     * @return: com.dlink.scheduler.model.Project
     */
    public static Project getProject() {
        if (Asserts.isNull(project)) {
            throw new SchedulerException("Please complete the dolphinscheduler configuration.");
        }
        return project;
    }

    public void registerUDF() {
        // ??????admin?????? ??????????????????udf????????????????????????????????????
        TenantContextHolder.set(1);
        UdfCodePool
                .registerPool(taskService.getAllUDF().stream().map(UDFUtils::taskToUDF).collect(Collectors.toList()));
        TenantContextHolder.set(null);
    }

    public void initTaskMonitor() {

        Thread thread = new Thread(() -> {
            while (true) {
                doTaskMonitor();
                try {
                    Thread.sleep(FlinkTaskConstant.MAX_POLLING_GAP);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    log.info("next TaskMonitor");
                }
            }
        });
        thread.start();
    }

    private void doTaskMonitor() {
        List<JobInstance> jobInstances = jobInstanceService.listJobInstanceOnline();
        log.info("Number of tasks started: " + jobInstances.size());
        if (jobInstances.isEmpty()) {
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(jobInstances.size());
        for (JobInstance jobInstance : jobInstances) {
            try {
                DaemonTaskConfig taskConfig = new DaemonTaskConfig(FlinkJobTask.TYPE, jobInstance.getId());
                DaemonTask daemonTask = DaemonTask.build(taskConfig);
                daemonTask.dealTask();
            } catch (Exception e) {
                log.error("doTaskMonitor is error {}", e.getMessage());
            } finally {
                countDownLatch.countDown();
                log.info("countDownLatch current num is  {}", countDownLatch.getCount());
            }
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            log.error("countDownLatch is error {}", e.getMessage());
        }
    }

//    private DefaultThreadPool getDefaultThreadPool() {
//        List<JobInstance> jobInstances = jobInstanceService.listJobInstanceOnline();
//        List<DaemonTaskConfig> configList = new ArrayList<>();
//        for (JobInstance jobInstance : jobInstances) {
//            configList.add(new DaemonTaskConfig(FlinkJobTask.TYPE, jobInstance.getId()));
//        }
//        log.info("Number of tasks started: " + configList.size());
//        DefaultThreadPool defaultThreadPool = DefaultThreadPool.getInstance();
//        for (DaemonTaskConfig config : configList) {
//            DaemonTask daemonTask = DaemonTask.build(config);
//            daemonTask.dealTask();
//            defaultThreadPool.execute(daemonTask);
//        }
//        return defaultThreadPool;
//    }
}
