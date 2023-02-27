package com.dlink.connector.http.sink;

import com.alibaba.fastjson.JSONObject;
import com.dlink.connector.http.constant.OptionConstant;
import com.dlink.connector.http.util.HttpUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.DecimalType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.TimestampType;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

/**
 * @author liuwenyi
 * @date 2023/2/23
 **/
public class HttpSinkFunction extends RichSinkFunction<RowData> {

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private DataType[] dataTypes;

    private String[] fields;

    private ReadableConfig readableConfig;

    public HttpSinkFunction() {
    }

    public HttpSinkFunction(DataType[] dataTypes, String[] fields, ReadableConfig readableConfig) {
        this.dataTypes = dataTypes;
        this.fields = fields;
        this.readableConfig = readableConfig;
    }

    @Override
    public void invoke(RowData value, Context context) throws Exception {
        int length = dataTypes.length;
        JSONObject jsonObject = new JSONObject();
        for (int i = 0; i < length; i++) {
            Object convertionValue = typeConvertion(dataTypes[i].getLogicalType(), value, i);
            jsonObject.put(fields[i], convertionValue);
        }
        String type = readableConfig.get(OptionConstant.TYPE);
        String url = readableConfig.get(OptionConstant.URL);
        String body = readableConfig.get(OptionConstant.BODY);
        if (StringUtils.isNotBlank(body)){
            JSONObject object = JSONObject.parseObject(body);
            jsonObject.putAll(object);
        }
        type = StringUtils.isNotBlank(type) ? type.toUpperCase(Locale.ROOT) : type;
        HttpUtils.HttpParam param = HttpUtils.HttpParam.builder().build();
        if (Objects.equals(type, OptionConstant.HTTP_GET)) {
            String bodyParm = HttpUtils.stitchingParams(jsonObject);
            if (url.contains(OptionConstant.QUESTION_MARK)) {
                url = url + OptionConstant.SYMBOL + bodyParm;
            } else {
                url = url + OptionConstant.QUESTION_MARK + bodyParm;
            }
            param.setUrl(url);
            HttpUtils.sendGet(param);
        } else if (Objects.equals(type, OptionConstant.HTTP_POST)) {
            String entity = jsonObject.toJSONString();
            param.setUrl(url);
            param.setEntity(entity);
            HttpUtils.sendPost(param);
        }
        Integer sleepTime = readableConfig.get(OptionConstant.SLEEP_TIME);
        if (Objects.nonNull(sleepTime) && sleepTime > 0) {
            Thread.sleep(sleepTime);
        }
    }

    private Object typeConvertion(LogicalType type, RowData record, int pos) {
        if (record.isNullAt(pos)) {
            return null;
        } else if (record.isNullAt(pos)) {
            return null;
        } else {
            switch (type.getTypeRoot()) {
                case BOOLEAN:
                    return record.getBoolean(pos) ? 1L : 0L;
                case TINYINT:
                    return record.getByte(pos);
                case SMALLINT:
                    return record.getShort(pos);
                case INTEGER:
                    return record.getInt(pos);
                case BIGINT:
                    return record.getLong(pos);
                case FLOAT:
                    return record.getFloat(pos);
                case DOUBLE:
                    return record.getDouble(pos);
                case CHAR:
                case VARCHAR:
                    return record.getString(pos).toString();
                case DATE:
                    return this.dateFormatter.format(Date.valueOf(LocalDate.ofEpochDay(record.getInt(pos))));
                case TIMESTAMP_WITHOUT_TIME_ZONE:
                    int timestampPrecision = ((TimestampType) type).getPrecision();
                    return this.dateTimeFormatter.format(new Date(record.getTimestamp(pos, timestampPrecision)
                            .toTimestamp().getTime()));
                case DECIMAL:
                    int decimalPrecision = ((DecimalType) type).getPrecision();
                    int decimalScale = ((DecimalType) type).getScale();
                    return record.getDecimal(pos, decimalPrecision, decimalScale).toBigDecimal();

                default:
                    throw new UnsupportedOperationException("Unsupported type:" + type);
            }
        }
    }
}
