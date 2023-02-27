package com.dlink.connector.http.factory;

import com.dlink.connector.http.constant.OptionConstant;
import com.dlink.connector.http.sink.HttpDynamicTableSink;
import com.dlink.connector.http.source.HttpDynamicTableSource;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.connector.format.DecodingFormat;
import org.apache.flink.table.connector.format.EncodingFormat;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.factories.DeserializationFormatFactory;
import org.apache.flink.table.factories.DynamicTableSinkFactory;
import org.apache.flink.table.factories.DynamicTableSourceFactory;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.table.factories.SerializationFormatFactory;
import org.apache.flink.table.types.DataType;

import java.util.HashSet;
import java.util.Set;

/**
 * @author liuwenyi
 * @date 2023/2/23
 **/
public class HttpDynamicTableFactory implements DynamicTableSinkFactory, DynamicTableSourceFactory {

    public HttpDynamicTableFactory() {
    }

    @Override
    public DynamicTableSink createDynamicTableSink(Context context) {
        FactoryUtil.TableFactoryHelper helper = FactoryUtil.createTableFactoryHelper(this, context);
        helper.validate();
        ReadableConfig options = helper.getOptions();
        EncodingFormat<SerializationSchema<RowData>> discoverEncodingFormat =
                helper.discoverEncodingFormat(SerializationFormatFactory.class, FactoryUtil.FORMAT);
        ResolvedSchema resolvedSchema = context.getCatalogTable().getResolvedSchema();
        return new HttpDynamicTableSink(discoverEncodingFormat, resolvedSchema, options);
    }

    @Override
    public DynamicTableSource createDynamicTableSource(Context context) {
        FactoryUtil.TableFactoryHelper helper = FactoryUtil.createTableFactoryHelper(this, context);
        helper.validate();
        ReadableConfig options = helper.getOptions();
        final DecodingFormat<DeserializationSchema<RowData>> decodingFormat = helper.discoverDecodingFormat(
                DeserializationFormatFactory.class,
                FactoryUtil.FORMAT);

        // derive the produced data type (excluding computed columns) from the catalog table
        final DataType producedDataType = context.getCatalogTable().getResolvedSchema().toPhysicalRowDataType();

        return new HttpDynamicTableSource(decodingFormat,producedDataType,options);
    }

    @Override
    public String factoryIdentifier() {
        return OptionConstant.IDENTIFIER;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        Set<ConfigOption<?>> options = new HashSet<>();
        options.add(OptionConstant.URL);
        options.add(OptionConstant.TYPE);
        return options;
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        Set<ConfigOption<?>> options = new HashSet<>();
        options.add(OptionConstant.HEADERS);
        options.add(OptionConstant.BODY);
        options.add(OptionConstant.SLEEP_TIME);
        options.add(FactoryUtil.FORMAT);
        return options;
    }
}
