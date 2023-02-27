package com.dlink.connector.http.sink;

import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.format.EncodingFormat;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.sink.SinkFunctionProvider;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;

/**
 * @author liuwenyi
 * @date 2023/2/23
 **/
public class HttpDynamicTableSink implements DynamicTableSink {

    private EncodingFormat<SerializationSchema<RowData>> encodingFormat;

    private ResolvedSchema tableSchema;

    private ReadableConfig readableConfig;

    public HttpDynamicTableSink(EncodingFormat<SerializationSchema<RowData>> encodingFormat,
                                ResolvedSchema tableSchema, ReadableConfig readableConfig) {
        this.encodingFormat = encodingFormat;

        this.tableSchema = tableSchema;
        this.readableConfig = readableConfig;
    }

    @Override
    public ChangelogMode getChangelogMode(ChangelogMode requestedMode) {
        return requestedMode;
    }

    @Override
    public SinkRuntimeProvider getSinkRuntimeProvider(Context context) {
//        SerializationSchema<RowData> runtimeEncoder = encodingFormat.createRuntimeEncoder(context, dataType);
//        runtimeEncoder
        HttpSinkFunction httpSinkFunction = new HttpSinkFunction(
                tableSchema.getColumnDataTypes().toArray(new DataType[0]),
                tableSchema.getColumnNames().toArray(new String[0]),
                readableConfig
        );
        return SinkFunctionProvider.of(httpSinkFunction);
    }

    @Override
    public DynamicTableSink copy() {
        return null;
    }

    @Override
    public String asSummaryString() {
        return "HTTP Table Sink";
    }
}
