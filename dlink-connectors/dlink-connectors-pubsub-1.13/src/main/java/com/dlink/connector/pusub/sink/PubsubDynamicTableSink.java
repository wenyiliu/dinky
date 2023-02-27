package com.dlink.connector.pusub.sink;

import com.dlink.connector.pusub.constant.OptionConstant;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.streaming.connectors.gcp.pubsub.PubSubSink;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.format.EncodingFormat;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.sink.SinkFunctionProvider;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;

import java.io.IOException;

/**
 * @author liuwenyi
 * @date 2023/2/22
 **/
public class PubsubDynamicTableSink implements DynamicTableSink {

    private ReadableConfig options;

    private DataType dataType;

    //    private DecodingFormat<DeserializationSchema<RowData>> decodingFormat;
    protected EncodingFormat<SerializationSchema<RowData>> encodingFormat;


//    protected WatermarkStrategy<RowData> watermarkStrategy;

    public PubsubDynamicTableSink(ReadableConfig options, DataType dataType,
                                  EncodingFormat<SerializationSchema<RowData>> encodingFormat) {
        this.options = options;
        this.dataType = dataType;
        this.encodingFormat = encodingFormat;
    }

    @Override
    public ChangelogMode getChangelogMode(ChangelogMode requestedMode) {
        return requestedMode;
    }

    @Override
    public DynamicTableSink.SinkRuntimeProvider getSinkRuntimeProvider(DynamicTableSink.Context context) {
        SerializationSchema<RowData> runtimeEncoder = encodingFormat.createRuntimeEncoder(context, dataType);

        PubSubSink<RowData> pubSubSink;
        try {
            pubSubSink = PubSubSink.newBuilder()
                    .withSerializationSchema(runtimeEncoder)
                    .withProjectName(options.get(OptionConstant.PUBSUB_PROJECTID))
                    .withTopicName(options.get(OptionConstant.USEPUBSUB_TOPICID))
                    .build();
            return SinkFunctionProvider.of(pubSubSink);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public DynamicTableSink copy() {
        return null;
    }

    @Override
    public String asSummaryString() {
        return "pubsub sink";
    }
}
