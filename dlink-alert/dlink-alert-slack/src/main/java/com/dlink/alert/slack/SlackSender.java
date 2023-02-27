package com.dlink.alert.slack;

import com.dlink.alert.AlertResult;
import com.dlink.utils.JSONUtil;
import net.gpedro.integrations.slack.SlackApi;
import net.gpedro.integrations.slack.SlackMessage;
import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author liuwenyi
 * @date 2023/2/14
 **/
public class SlackSender {

    private static final Logger logger = LoggerFactory.getLogger(SlackSender.class);
    private final String url;
    private final String channel;

    SlackSender(Map<String, String> config) {
        url = config.get(SlackConstants.WEB_HOOK);
        channel = config.get(SlackConstants.CHANNEL);
    }

    public AlertResult send(String title, String content) {
        AlertResult alertResult;
        try {
            SlackApi slackApi = new SlackApi(this.url);
            SlackMessage slackMessage = new SlackMessage();
            slackMessage.setChannel(this.channel);
            String msg = this.genrateResultMsg(title, content);
            slackMessage.setText(msg);
            slackApi.call(slackMessage);
            alertResult = new AlertResult();
            alertResult.setSuccess(true);
            alertResult.setMessage("success");
        } catch (Exception e) {
            logger.info("send slack talk alert msg  exception : {}", e.getMessage());
            alertResult = new AlertResult();
            alertResult.setSuccess(false);
            alertResult.setMessage("send slack talk alert fail.");
        }
        return alertResult;
    }

    private String genrateResultMsg(String title, String content) {
        List<LinkedHashMap> mapSendResultItemsList = JSONUtil.toList(content, LinkedHashMap.class);
        if (null == mapSendResultItemsList || mapSendResultItemsList.isEmpty()) {
            logger.error("itemsList is null");
            throw new RuntimeException("itemsList is null");
        }
        StringBuilder builder = new StringBuilder();
        for (LinkedHashMap mapItems : mapSendResultItemsList) {
            Set<Map.Entry<String, Object>> entries = mapItems.entrySet();
            Iterator<Map.Entry<String, Object>> iterator = entries.iterator();
            StringBuilder t = new StringBuilder(String.format("\n%s %s\n", title, SlackConstants.MARKDOWN_ENTER));

            while (iterator.hasNext()) {
                Map.Entry<String, Object> entry = iterator.next();
                t.append(SlackConstants.MARKDOWN_QUOTE);
                t.append(entry.getKey()).append("：").append(entry.getValue());
                t.append(SlackConstants.MARKDOWN_ENTER);
            }
            builder.append(t);
        }
        byte[] byt = StringUtils.getBytesUtf8(builder.toString());
        return StringUtils.newStringUtf8(byt);
    }
}
