package com.dlink.connector.http.constant;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;

/**
 * @author liuwenyi
 * @date 2023/2/22
 **/
public class OptionConstant {

    /**
     * 参数
     */
    public static final String IDENTIFIER = "http";

    public static final String HTTP_POST = "POST";

    public static final String HTTP_GET = "GET";

    public static final String SYMBOL = "&";

    public static final String QUESTION_MARK = "?";

    public static final String EQUAL_SIGN = "=";

    public static final ConfigOption<String> URL = ConfigOptions.key("url")
            .stringType().noDefaultValue().withDescription("the jdbc database url.");

    public static final ConfigOption<String> HEADERS = ConfigOptions.key("headers")
            .stringType().noDefaultValue().withDescription("the http header.");

    public static final ConfigOption<String> BODY = ConfigOptions.key("body")
            .stringType().noDefaultValue().withDescription("the http body params.");

    public static final ConfigOption<String> TYPE = ConfigOptions.key("type")
            .stringType().noDefaultValue().withDescription("the http type.");

    public static final ConfigOption<Integer> SLEEP_TIME = ConfigOptions.key("sleepTime")
            .intType().defaultValue(1000).withDescription("the http sleepTime.");

}
