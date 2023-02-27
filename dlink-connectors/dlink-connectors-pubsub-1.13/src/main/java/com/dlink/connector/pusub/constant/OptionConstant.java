package com.dlink.connector.pusub.constant;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;

/**
 * @author liuwenyi
 * @date 2023/2/22
 **/
public class OptionConstant {

    /**
     * pubsub 参数
     */
    public static final ConfigOption<String> PUBSUB_PROJECTID =
            ConfigOptions.key("projectId")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("the pubsub projectId.");

    public static final ConfigOption<String> USEPUBSUB_SUB =
            ConfigOptions.key("pubsub")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("the pubsub sub.");

    public static final ConfigOption<String> USEPUBSUB_TOPICID =
            ConfigOptions.key("topicId")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("the pubsub topicId.");

    public static final ConfigOption<Integer> PUBSUB_MAXMESSAGESPERPULL =
            ConfigOptions.key("maxMessagesPerPull")
                    .intType()
                    .defaultValue(10)
                    .withDescription("the pubsub.maxMessagesPerPull.");

    public static final ConfigOption<Integer> PUBSUB_PERREQUESTTIMEOUT =
            ConfigOptions.key("perRequestTimeout")
                    .intType()
                    .defaultValue(5)
                    .withDescription("the pubsub.perRequestTimeout.");

    public static final ConfigOption<Integer> PUBSUB_RETRIES =
            ConfigOptions.key("retries")
                    .intType()
                    .defaultValue(5)
                    .withDescription("the pubsub.retries.");
}
