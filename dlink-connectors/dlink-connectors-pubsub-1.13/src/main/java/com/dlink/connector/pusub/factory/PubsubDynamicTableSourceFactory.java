package com.dlink.connector.pusub.factory;

import com.dlink.connector.pusub.constant.OptionConstant;
import com.dlink.connector.pusub.source.PubsubDynamicTableSource;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.connector.format.DecodingFormat;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.factories.DeserializationFormatFactory;
import org.apache.flink.table.factories.DynamicTableSourceFactory;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.table.types.DataType;

import java.util.HashSet;
import java.util.Set;

import static org.apache.flink.table.factories.FactoryUtil.createTableFactoryHelper;

/**
 * @author liuwenyi
 * @date 2023/2/22
 **/
public class PubsubDynamicTableSourceFactory implements DynamicTableSourceFactory {

    @Override
    public DynamicTableSource createDynamicTableSource(Context context) {

        //内置工具类校验传入参数
        FactoryUtil.TableFactoryHelper helper = createTableFactoryHelper(this, context);
        helper.validate();
        // 获取有效参数
        final ReadableConfig options = helper.getOptions();

        final DecodingFormat<DeserializationSchema<RowData>> decodingFormat = helper.discoverDecodingFormat(
                DeserializationFormatFactory.class,
                FactoryUtil.FORMAT);


        // 获取元数据信息
//        ResolvedSchema resolvedSchema = context.getCatalogTable().getResolvedSchema();

//        TableSchema schema = context.getCatalogTable().getSchema();
        final DataType producedDataType =
                context.getCatalogTable().getResolvedSchema().toPhysicalRowDataType();
        // 创建并且返回一个动态表源
        return new PubsubDynamicTableSource(options, producedDataType, decodingFormat);
    }

    /**
     * 指定工厂类的标识符，该标识符就是建表时必须填写的connector参数的值
     *
     * @return String
     */
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
        options.add(OptionConstant.USEPUBSUB_SUB);
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
        options.add(OptionConstant.USEPUBSUB_TOPICID);

        return options;
    }


}
