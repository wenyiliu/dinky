package com.dlink.connector.pusub;

import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.runtime.state.StateBackend;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.util.concurrent.TimeUnit;

/**
 * @author liuwenyi
 * @date 2023/2/9
 **/
public class SourceTest {

    private static String sql = "create table pubsub_test_source(device_id STRING,\n" +
            "app_version STRING,\n" +
            "os_version STRING,\n" +
            "ip STRING,\n" +
            "ip_country STRING,\n" +
            "imsi STRING,\n" +
            "local_time_zone STRING,\n" +
            "emulator STRING,\n" +
            "proxy STRING,\n" +
            "carrier STRING,\n" +
            "local_lang STRING,\n" +
            "event_id STRING,\n" +
            "root STRING,\n" +
            "os_type STRING,\n" +
            "imei STRING,\n" +
            "app_lang STRING,\n" +
            "user_agent STRING,\n" +
            "event_time STRING,\n" +
            "cid STRING)\n" +
            "with(\n" +
            "\t'connector' = 'pubsub',\n" +
            "\t'projectId' = 'bitmart-testing',\n" +
            "\t'pubsub' = 'bitmart-userinfo-collect-topic-sub',\n" +
            "\t'format' = 'json'\n" +
            ")";
    public static void main(String[] args) throws Exception {

        EnvironmentSettings fsSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(60000, CheckpointingMode.EXACTLY_ONCE);
        env.getConfig().setRestartStrategy(RestartStrategies.fixedDelayRestart(3, org.apache.flink.api.common.time.Time.of(3, TimeUnit.MINUTES)));
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(400000);
        env.getCheckpointConfig().setCheckpointTimeout(200000);
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(6);
        env.getCheckpointConfig().setTolerableCheckpointFailureNumber(2);
        env.getCheckpointConfig().enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
        StateBackend stateBackend = new FsStateBackend("file:///Users/liuwenyi/IdeaProjects/data-custom/data/ck");
        env.setStateBackend(stateBackend);
        StreamTableEnvironment streamTableEnvironment = StreamTableEnvironment.create(env, fsSettings);



        //创建表的执行环境
//        StreamTableEnvironment streamTableEnvironment = StreamTableEnvironment.create(env);

        streamTableEnvironment.executeSql(sql);
        Table table = streamTableEnvironment.sqlQuery("select * from pubsub_test_source");

        streamTableEnvironment.toDataStream(table).print();

        env.execute();
    }
}
