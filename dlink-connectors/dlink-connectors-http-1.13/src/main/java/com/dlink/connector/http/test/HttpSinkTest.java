package com.dlink.connector.http.test;

import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.runtime.state.StateBackend;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.util.Arrays;

import static org.apache.flink.table.api.Expressions.$;

/**
 * @author liuwenyi
 * @date 2023/2/23
 **/
public class HttpSinkTest {
    private static String dropSql = "drop table if exists http_test_sink";
    private static String sql = "create table http_test_sink(device_id STRING, ip STRING, event_time " +
            "STRING) with" +
            "( 'connector' = 'http',\n" +
            "    'type' = 'post',\n" +
            "    'format' = 'json',\n" +
            "    'url' = 'http://192.168.124.8:8091/list_01',\n" +
            "    'body' = '{\"start_index\":\"1\",\"end_index\":\"10\"}'\n" +
            ")";

    public static void main(String[] args) throws Exception {
        EnvironmentSettings fsSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(60000);
        StateBackend stateBackend = new FsStateBackend("file:///Users/liuwenyi/IdeaProjects/data-custom/data/ck");
        env.setStateBackend(stateBackend);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, fsSettings);
        tableEnv.executeSql(dropSql);
        tableEnv.executeSql(sql);
//        DataStreamSource<String> dataStreamSource = env.addSource(new CustomSource());
//        Table table = tableEnv.fromDataStream(dataStreamSource);

//        TableResult tableResult = tableEnv.executeSql(String.format("insert into pubsub_test_sink " +
//                        "values ('%s','%s','%s')",
//                UUID.randomUUID(), "localhost-localhost", System.currentTimeMillis()));
//        tableResult.print();
//        TableResult execute = table.execute();
//        execute.print();
//        dataStreamSource.print();
//        env.execute();
        DataStreamSource<Tuple3<String, String, String>> dataStreamSource = env.addSource(new CustomSource());
        Table table = tableEnv.fromDataStream(dataStreamSource,
                $("f0"),
                $("f1"),
                $("f2"));
        String[] strings = tableEnv.listTables();
        System.out.println(Arrays.toString(strings));
        tableEnv.createTemporaryView("source_table", table);
//        tableEnv.executeSql("select f0,f1,f2 from source_table").print();
        tableEnv.executeSql("insert into http_test_sink select f0,f1,f2 from source_table").print();

        env.execute();
    }
}
