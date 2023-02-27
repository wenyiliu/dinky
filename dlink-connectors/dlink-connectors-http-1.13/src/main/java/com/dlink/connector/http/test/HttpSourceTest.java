package com.dlink.connector.http.test;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.math.BigDecimal;

/**
 * @author liuwenyi
 * @date 2023/2/23
 **/
public class HttpSourceTest {
    private static String dropSql = "drop table if exists http_source_table";
    private static String sql = "CREATE TABLE http_source_table (\n" +
            "    `code` BIGINT,\n" +
            "    `data` ARRAY<ROW <id BIGINT,`timestamp` BIGINT, `name` STRING>>,\n" +
            "    `message` STRING\n" +
            ") WITH (\n" +
            "    'connector' = 'http',\n" +
            "    'type' = 'post',\n" +
            "    'format' = 'json',\n" +
            "    'url' = 'http://172.31.0.92:8091/list_01',\n" +
            "    'body' = '{\"start_index\":\"1\",\"end_index\":\"10\"}'\n" +
            ")";

    private static String getSql ="CREATE TABLE http_source_table (\n" +
            "    `code` BIGINT,\n" +
            "    `data` ROW <id BIGINT,`timestamp` BIGINT, `name` STRING>,\n" +
            "    `message` STRING\n" +
            ") WITH (\n" +
            "    'connector' = 'http',\n" +
            "    'type' = 'get',\n" +
            "    'format' = 'json',\n" +
            "    'url' = 'http://172.31.0.92:8091/list_02?id=2',\n" +
            "    'body' = '{\"start_index\":\"1\",\"end_index\":\"10\"}'\n" +
            ")";

    public static void main(String[] args) throws Exception {

        EnvironmentSettings fsSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment streamTableEnvironment = StreamTableEnvironment.create(env, fsSettings);

        streamTableEnvironment.executeSql(dropSql);
        streamTableEnvironment.executeSql(getSql);
        Table table = streamTableEnvironment.sqlQuery("select * from http_source_table");
        streamTableEnvironment.toDataStream(table).print();

        env.execute();
    }
}
