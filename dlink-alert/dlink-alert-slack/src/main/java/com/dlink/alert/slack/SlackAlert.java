package com.dlink.alert.slack;

import com.dlink.alert.AbstractAlert;
import com.dlink.alert.AlertResult;

/**
 * @author liuwenyi
 * @date 2023/2/14
 **/
public class SlackAlert extends AbstractAlert {
    @Override
    public String getType() {
        return SlackConstants.TYPE;
    }

    @Override
    public AlertResult send(String title, String content) {
        SlackSender sender = new SlackSender(getConfig().getParam());
        return sender.send(title, content);
    }
}
