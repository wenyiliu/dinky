package com.dlink.connector.pusub.factory;

import com.dlink.connector.pusub.constant.OptionConstant;
import com.dlink.connector.pusub.sink.PubsubDynamicTableSink;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.connector.format.EncodingFormat;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.factories.DynamicTableSinkFactory;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.table.factories.SerializationFormatFactory;
import org.apache.flink.table.types.DataType;

import java.util.HashSet;
import java.util.Set;

import static org.apache.flink.table.factories.FactoryUtil.createTableFactoryHelper;

/**
 * @author liuwenyi
 * @date 2023/2/22
 **/
public class PubsubDynamicTableSinkFactory implements DynamicTableSinkFactory {

    @Override
    public DynamicTableSink createDynamicTableSink(Context context) {
        Tuple3<ReadableConfig, DataType, EncodingFormat<SerializationSchema<RowData>>> dynamicTable =
                this.createDynamicTable(context);
        return new PubsubDynamicTableSink(dynamicTable.f0, dynamicTable.f1, dynamicTable.f2);
    }


    public Tuple3<ReadableConfig, DataType, EncodingFormat<SerializationSchema<RowData>>> createDynamicTable(Context context) {
        FactoryUtil.TableFactoryHelper helper = createTableFactoryHelper(this, context);
        helper.validate();
        // 获取有效参数
        final ReadableConfig options = helper.getOptions();

        EncodingFormat<SerializationSchema<RowData>> encodingFormat =
                helper.discoverEncodingFormat(
                        SerializationFormatFactory.class, FactoryUtil.FORMAT);
        final DataType producedDataType =
                context.getCatalogTable().getResolvedSchema().toPhysicalRowDataType();
        return Tuple3.of(options, producedDataType, encodingFormat);
    }

    @Override
    public String factoryIdentifier() {
        return "pubsub";
    }

    /**
     * with里面非必须填写属性配置
     *
     * @return Set
     */
    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        final Set<ConfigOption<?>> options = new HashSet<>();
        options.add(OptionConstant.PUBSUB_PROJECTID);
        options.add(OptionConstant.USEPUBSUB_TOPICID);
        options.add(FactoryUtil.FORMAT);
        return options;
    }

    /**
     * with里面非必须填写属性配置
     *
     * @return Set
     */
    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        final Set<ConfigOption<?>> options = new HashSet<>();
        options.add(OptionConstant.PUBSUB_MAXMESSAGESPERPULL);
        options.add(OptionConstant.PUBSUB_PERREQUESTTIMEOUT);
        options.add(OptionConstant.PUBSUB_RETRIES);
        options.add(OptionConstant.USEPUBSUB_SUB);

        return options;
    }
}
