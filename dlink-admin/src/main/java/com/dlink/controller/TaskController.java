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

package com.dlink.controller;

import com.dlink.common.result.ProTableResult;
import com.dlink.common.result.Result;
import com.dlink.dto.TaskRollbackVersionDTO;
import com.dlink.function.pool.UdfCodePool;
import com.dlink.job.JobResult;
import com.dlink.model.JobLifeCycle;
import com.dlink.model.JobStatus;
import com.dlink.model.Task;
import com.dlink.model.TaskOperatingSavepointSelect;
import com.dlink.service.TaskService;
import com.dlink.utils.TaskOneClickOperatingUtil;
import com.dlink.utils.UDFUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

/**
 * ?????? Controller
 *
 * @author wenmo
 * @since 2021-05-24
 */
@Slf4j
@RestController
@RequestMapping("/api/task")
public class TaskController {

    @Autowired
    private TaskService taskService;

    /**
     * ??????????????????
     */
    @PutMapping
    public Result saveOrUpdate(@RequestBody Task task) throws Exception {
        if (taskService.saveOrUpdateTask(task)) {
            return Result.succeed("????????????");
        } else {
            return Result.failed("????????????");
        }
    }

    /**
     * ??????????????????
     */
    @PostMapping
    public ProTableResult<Task> listTasks(@RequestBody JsonNode para) {
        return taskService.selectForProTable(para);
    }

    /**
     * ????????????
     */
    @DeleteMapping
    public Result deleteMul(@RequestBody JsonNode para) {
        if (para.size() > 0) {
            boolean isAdmin = false;
            List<Integer> error = new ArrayList<>();
            for (final JsonNode item : para) {
                Integer id = item.asInt();
                if (!taskService.removeById(id)) {
                    error.add(id);
                }
            }
            CompletableFuture.runAsync(() -> UdfCodePool.registerPool(
                    taskService.getAllUDF().stream().map(UDFUtils::taskToUDF).collect(Collectors.toList())));
            if (error.size() == 0 && !isAdmin) {
                return Result.succeed("????????????");
            } else {
                return Result.succeed("????????????????????????" + error.toString() + "??????????????????" + error.size() + "????????????");
            }
        } else {
            return Result.failed("???????????????????????????");
        }
    }

    /**
     * ????????????
     */
    @PostMapping(value = "/submit")
    public Result submit(@RequestBody JsonNode para) throws Exception {
        if (para.size() > 0) {
            List<JobResult> results = new ArrayList<>();
            List<Integer> error = new ArrayList<>();
            for (final JsonNode item : para) {
                Integer id = item.asInt();
                JobResult result = taskService.submitTask(id);
                if (!result.isSuccess()) {
                    error.add(id);
                }
                results.add(result);
            }
            if (error.size() == 0) {
                return Result.succeed(results, "????????????");
            } else {
                return Result.succeed(results, "????????????????????????" + error.toString() + "??????????????????" + error.size() + "????????????");
            }
        } else {
            return Result.failed("???????????????????????????");
        }
    }

    /**
     * ????????????ID?????????
     */
    @GetMapping
    public Result getOneById(@RequestParam Integer id) {
        Task task = taskService.getTaskInfoById(id);
        return Result.succeed(task, "????????????");
    }

    /**
     * ????????????????????? FlinkSQLEnv
     */
    @GetMapping(value = "/listFlinkSQLEnv")
    public Result listFlinkSQLEnv() {
        return Result.succeed(taskService.listFlinkSQLEnv(), "????????????");
    }

    /**
     * ?????? sql
     */
    @GetMapping(value = "/exportSql")
    public Result exportSql(@RequestParam Integer id) {
        return Result.succeed(taskService.exportSql(id), "????????????");
    }

    /**
     * ????????????
     */
    @GetMapping(value = "/releaseTask")
    public Result releaseTask(@RequestParam Integer id) {
        return taskService.releaseTask(id);
    }

    @PostMapping("/rollbackTask")
    public Result rollbackTask(@RequestBody TaskRollbackVersionDTO dto) throws Exception {

        return taskService.rollbackTask(dto);
    }

    /**
     * ????????????
     */
    @GetMapping(value = "/developTask")
    public Result developTask(@RequestParam Integer id) {
        return Result.succeed(taskService.developTask(id), "????????????");
    }

    /**
     * ????????????
     */
    @GetMapping(value = "/onLineTask")
    public Result onLineTask(@RequestParam Integer id) {
        return taskService.onLineTask(id);
    }

    /**
     * ????????????
     */
    @GetMapping(value = "/offLineTask")
    public Result offLineTask(@RequestParam Integer id, @RequestParam String type) {
        return taskService.offLineTask(id, type);
    }

    /**
     * ????????????
     */
    @GetMapping(value = "/cancelTask")
    public Result cancelTask(@RequestParam Integer id) {
        return taskService.cancelTask(id);
    }

    /**
     * ????????????
     */
    @GetMapping(value = "/recoveryTask")
    public Result recoveryTask(@RequestParam Integer id) {
        return Result.succeed(taskService.recoveryTask(id), "????????????");
    }

    /**
     * ????????????
     */
    @GetMapping(value = "/restartTask")
    public Result restartTask(@RequestParam Integer id, @RequestParam Boolean isOnLine) {
        if (isOnLine) {
            return taskService.reOnLineTask(id, null);
        } else {
            return Result.succeed(taskService.restartTask(id, null), "????????????");
        }
    }

    /**
     * ???????????????????????????
     */
    @GetMapping(value = "/selectSavePointRestartTask")
    public Result selectSavePointRestartTask(@RequestParam Integer id, @RequestParam Boolean isOnLine,
            @RequestParam String savePointPath) {
        if (isOnLine) {
            return taskService.reOnLineTask(id, savePointPath);
        } else {
            return Result.succeed(taskService.restartTask(id, savePointPath), "????????????");
        }
    }

    /**
     * ??????????????? API ?????????
     */
    @GetMapping(value = "/getTaskAPIAddress")
    public Result getTaskAPIAddress() {
        return Result.succeed(taskService.getTaskAPIAddress(), "????????????");
    }

    /**
     * ??????json
     */
    @GetMapping(value = "/exportJsonByTaskId")
    public Result exportJsonByTaskId(@RequestParam Integer id) {
        return Result.succeed(taskService.exportJsonByTaskId(id), "????????????");
    }

    /**
     * ??????json??????
     */
    @PostMapping(value = "/exportJsonByTaskIds")
    public Result exportJsonByTaskIds(@RequestBody JsonNode para) {
        return Result.succeed(taskService.exportJsonByTaskIds(para), "????????????");
    }

    /**
     * json????????????  ??????task
     */
    @PostMapping(value = "/uploadTaskJson")
    public Result uploadTaskJson(@RequestParam("file") MultipartFile file) throws Exception {
        return taskService.uploadTaskJson(file);
    }

    /**
     * ??????????????????
     *
     * @return
     */
    @GetMapping("/queryAllCatalogue")
    public Result queryAllCatalogue() {
        return taskService.queryAllCatalogue();
    }

    /**
     * ?????????????????????????????????
     *
     * @param operating
     * @return
     */
    @GetMapping("/queryOnClickOperatingTask")
    public Result<List<Task>> queryOnClickOperatingTask(@RequestParam("operating") Integer operating,
            @RequestParam("catalogueId") Integer catalogueId) {
        if (operating == null) {
            return Result.failed("???????????????");
        }
        switch (operating) {
            case 1:
                return taskService.queryOnLineTaskByDoneStatus(Arrays.asList(JobLifeCycle.RELEASE),
                        JobStatus.getAllDoneStatus(), true, catalogueId);
            case 2:
                return taskService.queryOnLineTaskByDoneStatus(Arrays.asList(JobLifeCycle.ONLINE),
                        Collections.singletonList(JobStatus.RUNNING), false, catalogueId);
            default:
                return Result.failed("???????????????");
        }
    }

    /**
     * ??????????????????
     *
     * @param operating
     * @return
     */
    @PostMapping("/onClickOperatingTask")
    public Result onClickOperatingTask(@RequestBody JsonNode operating) {
        if (operating == null || operating.get("operating") == null) {
            return Result.failed("???????????????");
        }
        switch (operating.get("operating").asInt()) {
            case 1:
                final JsonNode savepointSelect = operating.get("taskOperatingSavepointSelect");
                return TaskOneClickOperatingUtil.oneClickOnline(TaskOneClickOperatingUtil.parseJsonNode(operating),
                        TaskOperatingSavepointSelect
                                .valueByCode(savepointSelect == null ? 0 : savepointSelect.asInt()));
            case 2:
                return TaskOneClickOperatingUtil.onClickOffline(TaskOneClickOperatingUtil.parseJsonNode(operating));
            default:
                return Result.failed("???????????????");
        }
    }

    /**
     * ??????????????????????????????
     *
     * @return
     */
    @GetMapping("/queryOneClickOperatingTaskStatus")
    public Result queryOneClickOperatingTaskStatus() {
        return TaskOneClickOperatingUtil.queryOneClickOperatingTaskStatus();
    }

}
