package com.dlink.connector.http.source;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dlink.connector.http.constant.OptionConstant;
import com.dlink.connector.http.util.HttpUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.apache.flink.table.data.RowData;

import java.util.Locale;
import java.util.Objects;

/**
 * @author liuwenyi
 * @date 2023/2/23
 **/
public class HttpSourceFunction extends RichSourceFunction<RowData> implements ResultTypeQueryable<RowData> {

    private ReadableConfig readableConfig;

    private DeserializationSchema<RowData> deserializer;

    public HttpSourceFunction(ReadableConfig readableConfig, DeserializationSchema<RowData> deserializer) {
        this.readableConfig = readableConfig;
        this.deserializer = deserializer;
    }

    @Override
    public TypeInformation<RowData> getProducedType() {
        return deserializer.getProducedType();
    }

    @Override
    public void run(SourceContext<RowData> ctx) throws Exception {
        String url = readableConfig.get(OptionConstant.URL);
        HttpUtils.HttpParam param = HttpUtils.HttpParam.builder()
                .build();
        String type = readableConfig.get(OptionConstant.TYPE);
        type = StringUtils.isNotBlank(type) ? type.toUpperCase(Locale.ROOT) : type;
        String s = readableConfig.get(OptionConstant.BODY);
        if (StringUtils.isNotBlank(s)) {
            param.setEntity(s);
            if (Objects.equals(type, OptionConstant.HTTP_GET)) {
                JSONObject jsonObject = JSON.parseObject(s);
                String bodyParm = HttpUtils.stitchingParams(jsonObject);
                if (url.contains(OptionConstant.QUESTION_MARK)) {
                    url = url + OptionConstant.SYMBOL + bodyParm;
                } else {
                    url = url + OptionConstant.QUESTION_MARK + bodyParm;
                }
            }
        } else {
            param.setEntity("{}");
        }
        param.setUrl(url);
        while (true) {
            byte[] result = null;
            if (Objects.equals(type, OptionConstant.HTTP_GET)) {
                result = HttpUtils.sendGet(param);
            } else if (Objects.equals(type, OptionConstant.HTTP_POST)) {
                result = HttpUtils.sendPost(param);
            }
            if (Objects.nonNull(result)) {
                ctx.collect(deserializer.deserialize(result));
            }
            Integer sleepTime = readableConfig.get(OptionConstant.SLEEP_TIME);
            if (Objects.nonNull(sleepTime) && sleepTime > 0) {
                Thread.sleep(sleepTime);
            }
        }
    }

    @Override
    public void cancel() {

    }
}
