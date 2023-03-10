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
import com.dlink.constant.CommonConstant;
import com.dlink.metadata.driver.DriverPool;
import com.dlink.metadata.result.JdbcSelectResult;
import com.dlink.model.DataBase;
import com.dlink.model.QueryData;
import com.dlink.service.DataBaseService;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
 * DataBaseController
 *
 * @author wenmo
 * @since 2021/7/20 23:48
 */
@Slf4j
@RestController
@RequestMapping("/api/database")
public class DataBaseController {

    @Autowired
    private DataBaseService databaseService;
    private static Logger logger = LoggerFactory.getLogger(DataBaseController.class);

    /**
     * ??????????????????
     */
    @PutMapping
    public Result saveOrUpdate(@RequestBody DataBase database) {
        if (databaseService.saveOrUpdateDataBase(database)) {
            DriverPool.remove(database.getName());
            return Result.succeed("????????????");
        } else {
            return Result.failed("????????????");
        }
    }

    /**
     * ??????????????????
     */
    @PostMapping
    public ProTableResult<DataBase> listDataBases(@RequestBody JsonNode para) {
        return databaseService.selectForProTable(para);
    }

    /**
     * ????????????
     */
    @DeleteMapping
    public Result deleteMul(@RequestBody JsonNode para) {
        if (para.size() > 0) {
            List<Integer> error = new ArrayList<>();
            for (final JsonNode item : para) {
                Integer id = item.asInt();
                if (!databaseService.removeById(id)) {
                    error.add(id);
                }
            }
            if (error.size() == 0) {
                return Result.succeed("????????????");
            } else {
                return Result.succeed("????????????????????????" + error.toString() + "??????????????????" + error.size() + "????????????");
            }
        } else {
            return Result.failed("???????????????????????????");
        }
    }

    /**
     * ????????????ID?????????
     */
    @PostMapping("/getOneById")
    public Result getOneById(@RequestBody DataBase database) {
        database = databaseService.getById(database.getId());
        return Result.succeed(database, "????????????");
    }

    /**
     * ??????????????????????????????
     */
    @GetMapping("/listEnabledAll")
    public Result listEnabledAll() {
        List<DataBase> dataBases = databaseService.listEnabledAll();
        return Result.succeed(dataBases, "????????????");
    }

    /**
     * ????????????
     */
    @PostMapping("/testConnect")
    public Result testConnect(@RequestBody DataBase database) {
        String msg = databaseService.testConnect(database);
        boolean isHealthy = Asserts.isEquals(CommonConstant.HEALTHY, msg);
        if (isHealthy) {
            return Result.succeed("???????????????????????????!");
        } else {
            return Result.failed(msg);
        }
    }

    /**
     * ??????????????????
     */
    @PostMapping("/checkHeartBeats")
    public Result checkHeartBeats() {
        List<DataBase> dataBases = databaseService.listEnabledAll();
        for (int i = 0; i < dataBases.size(); i++) {
            DataBase dataBase = dataBases.get(i);
            databaseService.checkHeartBeat(dataBase);
            databaseService.updateById(dataBase);
        }
        return Result.succeed("??????????????????");
    }

    /**
     * ??????????????????ID
     */
    @GetMapping("/checkHeartBeatById")
    public Result checkHeartBeatById(@RequestParam Integer id) {
        DataBase dataBase = databaseService.getById(id);
        Asserts.checkNotNull(dataBase, "????????????????????????");
        databaseService.checkHeartBeat(dataBase);
        databaseService.updateById(dataBase);
        return Result.succeed(dataBase, "??????????????????");
    }

    /**
     * ?????????????????????
     */
    @Cacheable(cacheNames = "metadata_schema", key = "#id")
    @GetMapping("/getSchemasAndTables")
    public Result getSchemasAndTables(@RequestParam Integer id) {
        return Result.succeed(databaseService.getSchemasAndTables(id), "????????????");
    }

    /**
     * ???????????????????????????
     */
    @CacheEvict(cacheNames = "metadata_schema", key = "#id")
    @GetMapping("/unCacheSchemasAndTables")
    public Result unCacheSchemasAndTables(@RequestParam Integer id) {
        return Result.succeed("clear cache", "success");
    }

    /**
     * ?????????????????????????????????
     */
    @GetMapping("/listColumns")
    public Result listColumns(@RequestParam Integer id, @RequestParam String schemaName,
            @RequestParam String tableName) {
        return Result.succeed(databaseService.listColumns(id, schemaName, tableName), "????????????");
    }

    /**
     * ????????????????????????????????????
     */
    @PostMapping("/queryData")
    public Result queryData(@RequestBody QueryData queryData) {
        JdbcSelectResult jdbcSelectResult = databaseService.queryData(queryData);
        if (jdbcSelectResult.isSuccess()) {
            return Result.succeed(jdbcSelectResult, "????????????");
        } else {
            return Result.failed(jdbcSelectResult, "????????????");
        }
    }

    /**
     * ??????sql
     */
    @PostMapping("/execSql")
    public Result execSql(@RequestBody QueryData queryData) {
        JdbcSelectResult jdbcSelectResult = databaseService.execSql(queryData);
        if (jdbcSelectResult.isSuccess()) {
            return Result.succeed(jdbcSelectResult, "????????????");
        } else {
            return Result.failed(jdbcSelectResult, "????????????");
        }
    }

    /**
     * ?????? SqlGeneration
     */
    @GetMapping("/getSqlGeneration")
    public Result getSqlGeneration(@RequestParam Integer id, @RequestParam String schemaName,
            @RequestParam String tableName) {
        return Result.succeed(databaseService.getSqlGeneration(id, schemaName, tableName), "????????????");
    }

    /**
     * copyDatabase
     */
    @PostMapping("/copyDatabase")
    public Result copyDatabase(@RequestBody DataBase database) {
        if (databaseService.copyDatabase(database)) {
            return Result.succeed("????????????!");
        } else {
            return Result.failed("???????????????");
        }
    }
}
