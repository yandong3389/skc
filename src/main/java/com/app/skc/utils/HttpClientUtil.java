package com.app.skc.utils;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HttpClientUtil {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);
    public static final int Format_KeyValue = 0;
    public static final int Format_Json = 1;

    /**
     * 创建HttpClient客户端
     *
     * @param isHttps
     * @return
     */
    public static CloseableHttpClient createClient(boolean isHttps) {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        if (isHttps) {
            try {
                SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                    // 信任所有
                    @Override
                    public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        return true;
                    }
                }).build();
                SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext,
                        SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                httpClientBuilder.setSSLSocketFactory(sslsf);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("创建HTTPS客户端异常");
            }
        }
        return httpClientBuilder.build();
    }

    /**
     * 调用Get接口
     *
     * @param url 接口地址
     * @return
     */
    public static String sendGet(String url) {
        return sendGet(url, createClient(false));
    }

    /**
     * 调用Get接口
     *
     * @param url        接口地址
     * @param httpClient
     * @return
     */
    public static String sendGet(String url, CloseableHttpClient httpClient) {
        if (url == null || "".equals(url)) {
            logger.error("接口地址为空");
            return null;
        }
        HttpGet request = null;
        try {
            request = new HttpGet(url);
            if (httpClient == null) {
                logger.error("HttpClient实例为空");
                return null;
            }
            CloseableHttpResponse response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() == 200) {
                return EntityUtils.toString(response.getEntity());
            }
        } catch (Exception e) {
            logger.error("访问接口失败，接口地址为：" + url);
        } finally {
            if (request != null)
                request.releaseConnection();
        }
        return null;
    }

    /**
     * 调用Post接口
     *
     * @param url        接口地址
     * @param parameters 参数
     * @param type       参数类型，0：键值对，1：json数据
     * @param httpClient
     * @return
     */
    public static String sendPost(String url, String parameters, int type, CloseableHttpClient httpClient) {
        if (url == null || "".equals(url)) {
            logger.error("接口地址为空");
            return null;
        }
        HttpPost request = null;
        try {
            request = new HttpPost(url);
            if (httpClient == null) {
                logger.error("HttpClient实例为空");
                return null;
            }
            StringEntity entity = new StringEntity(parameters, "UTF-8");
            if (type == Format_KeyValue) {
//                request.addHeader("Content-Type", "application/x-www-form-urlencoded");
                entity.setContentType("application/x-www-form-urlencoded");
            } else if (type == Format_Json) {
//                request.addHeader("Content-Type", "application/json");
                entity.setContentType("application/json");
            } else {
                logger.error("不支持的参数格式");
                return null;
            }
            request.setEntity(entity);
            CloseableHttpResponse response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() == 200) {
                return EntityUtils.toString(response.getEntity());
            }
        } catch (Exception e) {
            logger.error("访问接口失败，接口地址为：" + url);
        } finally {
            if (request != null)
                request.releaseConnection();
        }
        return null;
    }

    /**
     * 调用Post接口，参数为键值对方式
     *
     * @param url    接口地址
     * @param params 键值对参数
     * @return
     */
    public static String sendPostByKeyValue(String url, Map<String, String> params) {
        return sendPostByKeyValue(url, params, createClient(false));
    }

    /**
     * 调用Post接口，参数为键值对方式
     *
     * @param url        接口地址
     * @param params     键值对参数
     * @param httpClient
     * @return
     */
    public static String sendPostByKeyValue(String url, Map<String, String> params, CloseableHttpClient httpClient) {
        if (url == null || "".equals(url)) {
            logger.error("接口地址为空");
            return null;
        }
        HttpPost request = null;
        try {
            request = new HttpPost(url);
            if (httpClient == null) {
                logger.error("HttpClient实例为空");
                return null;
            }
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            request.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
            CloseableHttpResponse response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() == 200) {
                return EntityUtils.toString(response.getEntity());
            }
        } catch (Exception e) {
            logger.error("访问接口失败，接口地址为：" + url);
        } finally {
            if (request != null)
                request.releaseConnection();
        }
        return null;
    }

    /**
     * 调用Post接口，参数为JSON格式
     *
     * @param url    接口地址
     * @param params json数据
     * @return
     */
    public static String sendPostByJson(String url, String params) {
        return sendPost(url, params, Format_Json, createClient(false));
    }

    /**
     * 调用Post接口，参数为JSON格式
     *
     * @param url        接口地址
     * @param params     json数据
     * @param httpClient
     * @return
     */
    public static String sendPostByJson(String url, String params, CloseableHttpClient httpClient) {
        return sendPost(url, params, Format_Json, httpClient);
    }
}
