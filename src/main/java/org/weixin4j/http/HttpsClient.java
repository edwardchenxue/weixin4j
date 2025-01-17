/*
 * 微信公众平台(JAVA) SDK
 *
 * Copyright (c) 2014, Ansitech Network Technology Co.,Ltd All rights reserved.
 * 
 * http://www.weixin4j.org/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.weixin4j.http;

import com.alibaba.fastjson.JSONException;
import lombok.extern.slf4j.Slf4j;
import org.weixin4j.model.media.Attachment;
import com.alibaba.fastjson.JSONObject;
import java.io.BufferedInputStream;
import org.weixin4j.Configuration;
import org.weixin4j.WeixinException;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * 请求微信平台及响应的客户端类
 *
 * <p>
 * 每一次请求即对应一个<tt>HttpsClient</tt>，
 * 每次登陆产生一个<tt>OAuth</tt>用户连接,使用<tt>OAuthToken</tt>
 * 可以不用重复向微信平台发送登陆请求，在没有过期时间内，可继续请求。</p>
 *
 * @author yangqisheng
 * @since 0.0.1
 */
@Slf4j
public class HttpsClient implements java.io.Serializable {

    private static final int OK = 200;  // OK: Success!
    private static final int CONNECTION_TIMEOUT = Configuration.getConnectionTimeout();
    private static final int READ_TIMEOUT = Configuration.getReadTimeout();
    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final String GET = "GET";
    private static final String POST = "POST";

    public HttpsClient() {
    }

    /**
     * Post JSON数据
     *
     * 默认https方式
     *
     * @param url 提交地址
     * @param json JSON数据
     * @return 输出流对象
     * @throws org.weixin4j.WeixinException 微信操作异常
     */
    public Response post(String url, JSONObject json) throws WeixinException {
        //将JSON数据转换为String字符串
        String jsonString = json == null ? null : json.toString();
        if (log.isDebugEnabled()) {
            log.debug("URL POST 数据：" + jsonString);
        }
        //提交数据
        return httpRequest(url, POST, jsonString, false, null, null, null);
    }

    /**
     * Get 请求
     *
     * 默认https方式
     *
     * @param url 请求地址
     * @return 输出流对象
     * @throws org.weixin4j.WeixinException 微信操作异常
     */
    public Response get(String url) throws WeixinException {
        return httpRequest(url, GET, null, false, null, null, null);
    }

    /**
     * Post XML格式数据
     *
     * @param url 提交地址
     * @param xml XML数据
     * @return 输出流对象
     * @throws org.weixin4j.WeixinException 微信操作异常
     */
    public Response postXml(String url, String xml) throws WeixinException {
        return httpRequest(url, POST, xml, false, null, null, null);
    }

    /**
     * Post XML格式数据
     *
     * @param url 提交地址
     * @param xml XML数据
     * @param partnerId 商户ID
     * @param certPath 证书地址
     * @param certSecret 证书密钥
     * @return 输出流对象
     * @throws org.weixin4j.WeixinException 微信操作异常
     */
    public Response postXml(String url, String xml, String partnerId, String certPath, String certSecret) throws WeixinException {
        return httpRequest(url, POST, xml, true, partnerId, certPath, certSecret);
    }

    /**
     * 通过http协议请求url
     *
     * 实现方法中会根据url判断地址是https还是http
     *
     * @param url 提交地址
     * @param method 提交方式
     * @param postData 提交数据
     * @return 响应流
     * @throws org.weixin4j.WeixinException 微信操作异常
     */
    private Response httpRequest(String url, String method, String postData, boolean needCert, String partnerId, String certPath, String certSecret)
            throws WeixinException {
        Response res;
        OutputStream output;
        HttpURLConnection httpURLConnection;
        try {
            //判断schema
            if(url.startsWith("https:")) {
                //创建https请求连接
                httpURLConnection = getHttpsURLConnection(url);
                //设置Header信息，包括https证书
                setHttpsHeader((HttpsURLConnection) httpURLConnection, needCert, partnerId, certPath, certSecret);
            } else {
                //创建http请求连接
                httpURLConnection = getHttpURLConnection(url);
            }
            setHttpHeader(httpURLConnection, method);
            //判断是否需要提交数据
            if (method.equals(POST) && null != postData) {
                byte[] bytes = postData.getBytes(DEFAULT_CHARSET);
                httpURLConnection.setRequestProperty("Content-Length", Integer.toString(bytes.length));
                httpURLConnection.connect();
                output = httpURLConnection.getOutputStream();
                output.write(bytes);
                output.flush();
                output.close();
            } else {
                //开始连接
                httpURLConnection.connect();
            }
            //创建输出对象
            res = new Response(httpURLConnection);
            //获取响应代码
            if (res.getStatus() == OK) {
                return res;
            }
        } catch (NoSuchAlgorithmException ex) {
            throw new WeixinException(ex.getMessage(), ex);
        } catch (KeyManagementException ex) {
            throw new WeixinException(ex.getMessage(), ex);
        } catch (NoSuchProviderException ex) {
            throw new WeixinException(ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new WeixinException(ex.getMessage(), ex);
        } catch (KeyStoreException ex) {
            throw new WeixinException(ex.getMessage(), ex);
        } catch (CertificateException ex) {
            throw new WeixinException(ex.getMessage(), ex);
        } catch (UnrecoverableKeyException ex) {
            throw new WeixinException(ex.getMessage(), ex);
        }
        return res;
    }

    /**
     * 获取https请求连接
     *
     * @param url 连接地址
     * @return https连接对象
     * @throws IOException IO异常
     */
    private HttpsURLConnection getHttpsURLConnection(String url) throws IOException {
        URL urlGet = new URL(url);
        //创建https请求
        HttpsURLConnection httpsUrlConnection = (HttpsURLConnection) urlGet.openConnection();
        return httpsUrlConnection;
    }

    /**
     * 获取http请求连接
     *
     * @param url 连接地址
     * @return http连接对象
     * @throws IOException IO异常
     */
    private HttpURLConnection getHttpURLConnection(String url) throws IOException {
        URL urlGet = new URL(url);
        //创建https请求
        HttpURLConnection httpURLConnection = (HttpURLConnection) urlGet.openConnection();
        return httpURLConnection;
    }

    private void setHttpsHeader(HttpsURLConnection httpsUrlConnection, boolean needCert, String partnerId, String certPath, String certSecret)
            throws NoSuchAlgorithmException, KeyManagementException, NoSuchProviderException,
            IOException, KeyStoreException, CertificateException, UnrecoverableKeyException {
        //不需要维修证书，则使用默认证书
        if (!needCert) {
            //创建https请求证书
            TrustManager[] tm = {new MyX509TrustManager()};
            //创建证书上下文对象
            SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
            //初始化证书信息
            sslContext.init(null, tm, new java.security.SecureRandom());
            // 从上述SSLContext对象中得到SSLSocketFactory对象  
            SSLSocketFactory ssf = sslContext.getSocketFactory();
            //设置ssl证书
            httpsUrlConnection.setSSLSocketFactory(ssf);
        } else {
            //指定读取证书格式为PKCS12
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            //读取本机存放的PKCS12证书文件
            FileInputStream instream = new FileInputStream(new File(certPath));
            try {
                //指定PKCS12的密码
                keyStore.load(instream, partnerId.toCharArray());
            } finally {
                instream.close();
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keyStore, certSecret.toCharArray());
            //创建管理jks密钥库的x509密钥管理器，用来管理密钥，需要key的密码  
            SSLContext sslContext = SSLContext.getInstance("TLSv1");
            // 构造SSL环境，指定SSL版本为3.0，也可以使用TLSv1，但是SSLv3更加常用。  
            sslContext.init(kmf.getKeyManagers(), null, null);
            // 从上述SSLContext对象中得到SSLSocketFactory对象  
            SSLSocketFactory ssf = sslContext.getSocketFactory();
            //设置ssl证书
            httpsUrlConnection.setSSLSocketFactory(ssf);
        }
    }


    private void setHttpHeader(HttpURLConnection httpURLConnection, String method) throws IOException {
        //设置header信息
        httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        //设置User-Agent信息
        httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.146 Safari/537.36");
        //设置可接受信息
        httpURLConnection.setDoOutput(true);
        //设置可输入信息
        httpURLConnection.setDoInput(true);
        //设置请求方式
        httpURLConnection.setRequestMethod(method);
        //设置连接超时时间
        if (CONNECTION_TIMEOUT > 0) {
            httpURLConnection.setConnectTimeout(CONNECTION_TIMEOUT);
        } else {
            //默认10秒超时
            httpURLConnection.setConnectTimeout(10000);
        }
        //设置请求超时
        if (READ_TIMEOUT > 0) {
            httpURLConnection.setReadTimeout(READ_TIMEOUT);
        } else {
            //默认10秒超时
            httpURLConnection.setReadTimeout(10000);
        }
        //设置编码
        httpURLConnection.setRequestProperty("Charsert", "UTF-8");
    }

    /**
     * 上传文件
     *
     * @param url 上传地址
     * @param file 上传文件对象
     * @return 服务器上传响应结果
     * @throws org.weixin4j.WeixinException 微信操作异常
     */
    public String uploadHttps(String url, File file) throws WeixinException {
        HttpsURLConnection https = null;
        StringBuffer bufferRes = new StringBuffer();
        try {
            // 定义数据分隔线 
            String BOUNDARY = "----WebKitFormBoundaryiDGnV9zdZA1eM1yL";
            //创建https请求连接
            https = getHttpsURLConnection(url);
            //设置header和ssl证书
            setHttpsHeader(https, false, null, null, null);
            setHttpHeader(https, POST);
            //不缓存
            https.setUseCaches(false);
            //保持连接
            https.setRequestProperty("connection", "Keep-Alive");
            //设置文档类型
            https.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);

            //定义输出流
            OutputStream out = null;
            //定义输入流
            DataInputStream dataInputStream;
            try {
                out = new DataOutputStream(https.getOutputStream());
                byte[] end_data = ("\r\n--" + BOUNDARY + "--\r\n").getBytes();// 定义最后数据分隔线  
                StringBuilder sb = new StringBuilder();
                sb.append("--");
                sb.append(BOUNDARY);
                sb.append("\r\n");
                sb.append("Content-Disposition: form-data;name=\"media\";filename=\"").append(file.getName()).append("\"\r\n");
                sb.append("Content-Type:application/octet-stream\r\n\r\n");
                byte[] data = sb.toString().getBytes();
                out.write(data);
                //读取文件流
                dataInputStream = new DataInputStream(new FileInputStream(file));
                int bytes;
                byte[] bufferOut = new byte[1024];
                while ((bytes = dataInputStream.read(bufferOut)) != -1) {
                    out.write(bufferOut, 0, bytes);
                }
                out.write("\r\n".getBytes()); //多个文件时，二个文件之间加入这个  
                dataInputStream.close();
                out.write(end_data);
                out.flush();
            } finally {
                if (out != null) {
                    out.close();
                }
            }

            // 定义BufferedReader输入流来读取URL的响应  
            InputStream ins = null;
            try {
                ins = https.getInputStream();
                BufferedReader read = new BufferedReader(new InputStreamReader(ins, "UTF-8"));
                String valueString;
                bufferRes = new StringBuffer();
                while ((valueString = read.readLine()) != null) {
                    bufferRes.append(valueString);
                }
            } finally {
                if (ins != null) {
                    ins.close();
                }
            }
        } catch (IOException ex) {
            throw new WeixinException(ex.getMessage(), ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new WeixinException(ex.getMessage(), ex);
        } catch (KeyManagementException ex) {
            throw new WeixinException(ex.getMessage(), ex);
        } catch (NoSuchProviderException ex) {
            throw new WeixinException(ex.getMessage(), ex);
        } catch (KeyStoreException ex) {
            throw new WeixinException(ex.getMessage(), ex);
        } catch (CertificateException ex) {
            throw new WeixinException(ex.getMessage(), ex);
        } catch (UnrecoverableKeyException ex) {
            throw new WeixinException(ex.getMessage(), ex);
        } finally {
            if (https != null) {
                // 关闭连接
                https.disconnect();
            }
        }
        return bufferRes.toString();
    }

    /**
     * 下载附件
     *
     * @param url 附件地址
     * @return 附件对象
     * @throws org.weixin4j.WeixinException 微信操作异常
     */
    public Attachment downloadHttps(String url) throws WeixinException {
        //定义下载附件对象
        Attachment attachment = null;
        HttpsURLConnection https;
        try {
            //创建https请求连接
            https = getHttpsURLConnection(url);
            //设置header和ssl证书
            setHttpsHeader(https, false, null, null, null);
            setHttpHeader(https,POST);
            //不缓存
            https.setUseCaches(false);
            //保持连接
            https.setRequestProperty("connection", "Keep-Alive");

            //获取输入流
            InputStream in = https.getInputStream();

            //初始化返回附件对象
            attachment = new Attachment();
            String contentType = https.getContentType();
            //出现错误时，返回错误消息
            if (contentType.contains("text/plain")) {
                // 定义BufferedReader输入流来读取URL的响应  
                BufferedReader read = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                String valueString;
                StringBuilder bufferRes = new StringBuilder();
                while ((valueString = read.readLine()) != null) {
                    bufferRes.append(valueString);
                }
                String textString = bufferRes.toString();
                if (textString.contains("video_url")) {
                    try {
                        JSONObject result = JSONObject.parseObject(textString);
                        if (result.containsKey("errcode") && result.getIntValue("errcode") != 0) {
                            attachment.setError(result.getString("errmsg"));
                        } else if (result.containsKey("video_url")) {
                            //发起get请求获取视频流
                            HttpClient httpClient = new HttpClient();
                            return httpClient.download(result.getString("video_url"));
                        } else {
                            //未知格式
                            attachment.setError(textString);
                        }
                    } catch (JSONException ex) {
                        attachment.setError(textString);
                    }
                } else {
                    attachment.setError(textString);
                }
            } else if (contentType.contains("application/json")) {
                // 定义BufferedReader输入流来读取URL的响应  
                BufferedReader read = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                String valueString;
                StringBuilder bufferRes = new StringBuilder();
                while ((valueString = read.readLine()) != null) {
                    bufferRes.append(valueString);
                }
                String jsonString = bufferRes.toString();
                JSONObject result = JSONObject.parseObject(jsonString);
                if (result.containsKey("errcode") && result.getIntValue("errcode") != 0) {
                    attachment.setError(result.getString("errmsg"));
                } else if (result.containsKey("video_url")) {
                    //发起get请求获取视频流
                    HttpClient httpClient = new HttpClient();
                    return httpClient.download(result.getString("video_url"));
                } else {
                    //未知格式
                    attachment.setError(jsonString);
                }
            } else {
                BufferedInputStream bis = new BufferedInputStream(in);
                String ds = https.getHeaderField("Content-disposition");
                String fullName = ds.substring(ds.indexOf("filename=\"") + 10, ds.length() - 1);
                String relName = fullName.substring(0, fullName.lastIndexOf("."));
                String suffix = fullName.substring(relName.length() + 1);

                attachment.setFullName(fullName);
                attachment.setFileName(relName);
                attachment.setSuffix(suffix);
                attachment.setContentLength(https.getHeaderField("Content-Length"));
                attachment.setContentType(https.getHeaderField("Content-Type"));

                attachment.setFileStream(bis);
            }
        } catch (IOException ex) {
            throw new WeixinException(ex.getMessage(), ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new WeixinException(ex.getMessage(), ex);
        } catch (KeyManagementException ex) {
            throw new WeixinException(ex.getMessage(), ex);
        } catch (NoSuchProviderException ex) {
            throw new WeixinException(ex.getMessage(), ex);
        } catch (KeyStoreException ex) {
            throw new WeixinException(ex.getMessage(), ex);
        } catch (CertificateException ex) {
            throw new WeixinException(ex.getMessage(), ex);
        } catch (UnrecoverableKeyException ex) {
            throw new WeixinException(ex.getMessage(), ex);
        } finally {
        }
        return attachment;
    }
}
