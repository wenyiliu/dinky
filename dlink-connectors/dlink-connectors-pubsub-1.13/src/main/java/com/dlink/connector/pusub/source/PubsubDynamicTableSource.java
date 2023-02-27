package com.dlink.connector.pusub.source;

import com.dlink.connector.pusub.constant.OptionConstant;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.connectors.gcp.pubsub.PubSubSource;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.format.DecodingFormat;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.connector.source.ScanTableSource;
import org.apache.flink.table.connector.source.SourceFunctionProvider;
import org.apache.flink.table.connector.source.abilities.SupportsWatermarkPushDown;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;

import java.io.IOException;
import java.time.Duration;

/**
 * @author liuwenyi
 * @date 2023/2/22
 **/
public class PubsubDynamicTableSource implements ScanTableSource, SupportsWatermarkPushDown {

    private ReadableConfig options;

//    private TableSchema schema;

    private DataType dataType;

    private DecodingFormat<DeserializationSchema<RowData>> decodingFormat;

    protected WatermarkStrategy<RowData> watermarkStrategy;

//    public PubsubDynamicTableSource(ReadableConfig options, TableSchema schema,
//                                    DecodingFormat<DeserializationSchema<RowData>> decodingFormat) {
//        this.options = options;
//        this.schema = schema;
//        this.decodingFormat = decodingFormat;
//    }

    public PubsubDynamicTableSource(ReadableConfig options, DataType dataType,
                                    DecodingFormat<DeserializationSchema<RowData>> decodingFormat) {
        this.options = options;
        this.dataType = dataType;
        this.decodingFormat = decodingFormat;
    }


    @Override
    public ChangelogMode getChangelogMode() {
        return decodingFormat.getChangelogMode();
    }

    @Override
    public ScanRuntimeProvider getScanRuntimeProvider(ScanContext scanContext) {
        final DeserializationSchema<RowData> deserializer = decodingFormat.createRuntimeDecoder(
                scanContext,
                dataType);
        DeserializationSchema<RowData> messageout = new DeserializationSchema<RowData>() {
            private static final long serialVersionUID = 1L;

            @Override
            public TypeInformation<RowData> getProducedType() {
                return TypeInformation.of(RowData.class);
            }

            @Override
            public RowData deserialize(byte[] bytes) throws IOException {
                return deserializer.deserialize(bytes);
            }

            @Override
            public boolean isEndOfStream(RowData userPubsub) {
                return false;
            }
        };
        final SourceFunction<RowData> sourceFunction;
        try {
            sourceFunction = PubSubSource.newBuilder()
                    .withDeserializationSchema(messageout)
                    .withProjectName(options.get(OptionConstant.PUBSUB_PROJECTID))
                    .withSubscriptionName(options.get(OptionConstant.USEPUBSUB_SUB))
                    .withPubSubSubscriberFactory(options.get(OptionConstant.PUBSUB_MAXMESSAGESPERPULL),
                            Duration.ofSeconds(options.get(OptionConstant.PUBSUB_PERREQUESTTIMEOUT)),
                            options.get(OptionConstant.PUBSUB_RETRIES))
                    .build();
            return SourceFunctionProvider.of(sourceFunction, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public DynamicTableSource copy() {
        return new PubsubDynamicTableSource(options, dataType, decodingFormat);
    }

    @Override
    public String asSummaryString() {
        return "pubsub source table";
    }

    @Override
    public void applyWatermark(WatermarkStrategy<RowData> watermarkStrategy) {
        this.watermarkStrategy = watermarkStrategy;
    }
}
