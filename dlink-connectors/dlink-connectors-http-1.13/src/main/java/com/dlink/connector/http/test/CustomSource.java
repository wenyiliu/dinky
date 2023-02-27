package com.dlink.connector.http.test;

import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.util.UUID;

/**
 * @author liuwenyi
 * @date 2023/2/22
 **/
public class CustomSource implements SourceFunction<Tuple3<String, String, String>> {

    @Override
    public void run(SourceContext<Tuple3<String, String, String>> ctx) throws Exception {
        while (true) {
            long l = System.currentTimeMillis();
            Tuple3<String, String, String> tuple3 = Tuple3.of(UUID.randomUUID().toString(),
                    "localhost-localhost", String.valueOf(l));

            ctx.collect(tuple3);
            Thread.sleep(5000);
        }
    }

    @Override
    public void cancel() {

    }
}
