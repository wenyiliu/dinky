package com.dlink.connector.http.source;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.format.DecodingFormat;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.connector.source.ScanTableSource;
import org.apache.flink.table.connector.source.SourceFunctionProvider;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;

/**
 * @author liuwenyi
 * @date 2023/2/23
 **/
public class HttpDynamicTableSource implements ScanTableSource {

    private DecodingFormat<DeserializationSchema<RowData>> decodingFormat;

    private DataType producedDataType;

    private ReadableConfig readableConfig;

    public HttpDynamicTableSource() {
    }

    public HttpDynamicTableSource(DecodingFormat<DeserializationSchema<RowData>> decodingFormat,
                                  DataType producedDataType, ReadableConfig readableConfig) {
        this.decodingFormat = decodingFormat;
        this.producedDataType = producedDataType;
        this.readableConfig = readableConfig;
    }

    @Override
    public ChangelogMode getChangelogMode() {
        return decodingFormat.getChangelogMode();
    }

    @Override
    public ScanRuntimeProvider getScanRuntimeProvider(ScanContext runtimeProviderContext) {
        DeserializationSchema<RowData> runtimeDecoder = decodingFormat.createRuntimeDecoder(runtimeProviderContext,
                producedDataType);
        HttpSourceFunction httpSourceFunction = new HttpSourceFunction(readableConfig, runtimeDecoder);
        return SourceFunctionProvider.of(httpSourceFunction,true);
    }

    @Override
    public DynamicTableSource copy() {
        return new HttpDynamicTableSource(decodingFormat, producedDataType, readableConfig);
    }

    @Override
    public String asSummaryString() {
        return "http source";
    }
}
