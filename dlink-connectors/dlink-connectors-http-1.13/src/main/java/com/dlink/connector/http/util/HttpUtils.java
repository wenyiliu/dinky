package com.dlink.connector.http.util;

import com.alibaba.fastjson.JSONObject;
import com.dlink.connector.http.constant.OptionConstant;
import lombok.Builder;
import lombok.Data;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * @author liuwenyi
 * @date 2023/2/23
 **/
public class HttpUtils {


    public static String stitchingParams(JSONObject jsonObject){
       return jsonObject.entrySet()
                .stream()
                .map(item -> item.getKey() + OptionConstant.EQUAL_SIGN + item.getValue())
                .collect(Collectors.joining(OptionConstant.SYMBOL));
    }
    public static byte[] sendPost(HttpParam params) throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(params.getUrl());
        httpPost.setEntity(new StringEntity(params.entity, "UTF-8"));
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json;charset=utf8");
        CloseableHttpResponse response = httpclient.execute(httpPost);
        int code = response.getStatusLine().getStatusCode();
        if (code != 200) {
            throw new Exception(response.toString());
        }
        String content = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        System.out.println(content);
        return content.getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] sendGet(HttpParam params) throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(params.url);
        httpGet.setHeader("Accept", "application/json");
        httpGet.setHeader("Content-type", "application/json;charset=utf8");
        CloseableHttpResponse response = httpclient.execute(httpGet);
        int code = response.getStatusLine().getStatusCode();
        if (code != 200) {
            throw new Exception(response.toString());
        }
        String content = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        return content.getBytes(StandardCharsets.UTF_8);
    }

    @Data
    @Builder
    public static class HttpParam {
        private String url;

        private String entity;
    }
}
