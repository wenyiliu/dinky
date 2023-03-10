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

import com.dlink.assertion.Asserts;
import com.dlink.common.result.ProTableResult;
import com.dlink.common.result.Result;
import com.dlink.model.Cluster;
import com.dlink.model.JobInstance;
import com.dlink.service.ClusterService;
import com.dlink.service.JobInstanceService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

/**
 * ClusterController
 *
 * @author wenmo
 * @since 2021/5/28 14:03
 **/
@Slf4j
@RestController
@RequestMapping("/api/cluster")
public class ClusterController {

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private JobInstanceService jobInstanceService;
    /**
     * ??????????????????
     */
    @PutMapping
    public Result saveOrUpdate(@RequestBody Cluster cluster) throws Exception {
        cluster.setAutoRegisters(false);
        Integer id = cluster.getId();
        clusterService.registersCluster(cluster);
        return Result.succeed(Asserts.isNotNull(id) ? "????????????" : "????????????");
    }

    /**
     * ???????????????
     */
    @PutMapping("/enable")
    public Result enableCluster(@RequestBody Cluster cluster) throws Exception {
        clusterService.enableCluster(cluster);
        return Result.succeed("????????????");
    }

    /**
     * ??????????????????
     */
    @PostMapping
    public ProTableResult<Cluster> listClusters(@RequestBody JsonNode para) {
        return clusterService.selectForProTable(para);
    }

    /**
     * ????????????
     */
    @DeleteMapping
    public Result deleteMul(@RequestBody JsonNode para) {
        if (para.size() > 0) {
            List<JobInstance> instances = jobInstanceService.listJobInstanceActive();
            Set<Integer> ids = instances.stream().map(JobInstance::getClusterId).collect(Collectors.toSet());
            List<String> error = new ArrayList<>();
            for (final JsonNode item : para) {
                Integer id = item.asInt();
                if (ids.contains(id) || !clusterService.removeById(id)) {
                    error.add(clusterService.getById(id).getName());
                }
            }
            if (error.size() == 0) {
                return Result.succeed("????????????");
            } else {
                if (para.size() > error.size()) {
                    return Result.succeed(
                            "????????????????????????" + error.toString() + "??????????????????" + error.size() + "????????????\n????????????????????????????????????????????????");
                } else {
                    return Result.succeed(error.toString() + "??????????????????" + error.size() + "????????????\n????????????????????????????????????????????????");
                }
            }
        } else {
            return Result.failed("???????????????????????????");
        }
    }

    /**
     * ????????????ID?????????
     */
    @PostMapping("/getOneById")
    public Result getOneById(@RequestBody Cluster cluster) throws Exception {
        cluster = clusterService.getById(cluster.getId());
        return Result.succeed(cluster, "????????????");
    }

    /**
     * ???????????????????????????
     */
    @GetMapping("/listEnabledAll")
    public Result listEnabledAll() {
        List<Cluster> clusters = clusterService.listEnabledAll();
        return Result.succeed(clusters, "????????????");
    }

    /**
     * ???????????????????????????
     */
    @GetMapping("/listSessionEnable")
    public Result listSessionEnable() {
        List<Cluster> clusters = clusterService.listSessionEnable();
        return Result.succeed(clusters, "????????????");
    }

    /**
     * ??????????????????
     */
    @PostMapping("/heartbeats")
    public Result heartbeat(@RequestBody JsonNode para) {
        List<Cluster> clusters = clusterService.listEnabledAll();
        for (int i = 0; i < clusters.size(); i++) {
            Cluster cluster = clusters.get(i);
            clusterService.registersCluster(cluster);
        }
        return Result.succeed("??????????????????");
    }

    /**
     * ??????????????????
     */
    @GetMapping("/clear")
    public Result clear() {
        return Result.succeed(clusterService.clearCluster(), "????????????");
    }

    /**
     * ????????????
     */
    @GetMapping("/killCluster")
    public Result killCluster(@RequestParam("id") Integer id) {
        clusterService.killCluster(id);
        return Result.succeed("Kill Cluster Succeed.");
    }

    /**
     * ????????????
     */
    @DeleteMapping("/killMulCluster")
    public Result killMulCluster(@RequestBody JsonNode para) {
        if (para.size() > 0) {
            for (final JsonNode item : para) {
                clusterService.killCluster(item.asInt());
            }
        }
        return Result.succeed("Kill cluster succeed.");
    }

    /**
     * ?????? Session ??????
     */
    @GetMapping("/deploySessionCluster")
    public Result deploySessionCluster(@RequestParam("id") Integer id) {
        return Result.succeed(clusterService.deploySessionCluster(id), "Deploy session cluster succeed.");
    }
}
