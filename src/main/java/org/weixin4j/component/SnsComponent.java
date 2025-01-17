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
package org.weixin4j.component;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.weixin4j.model.sns.SnsAccessToken;
import org.weixin4j.http.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.apache.commons.lang.StringUtils;
import org.weixin4j.Configuration;
import org.weixin4j.model.sns.SnsUser;
import org.weixin4j.Weixin;
import org.weixin4j.WeixinException;
import org.weixin4j.http.HttpsClient;

/**
 * 网页授权获取用户基本信息
 *
 * @author yangqisheng
 * @since 0.1.0
 */
@Slf4j
public class SnsComponent extends AbstractComponent {

    /**
     * 默认授权请求URL
     */
    private String authorize_url = "https://open.weixin.qq.com/connect/oauth2/authorize";

    public static String getAccessTokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token";

    public static String getUserInfo = "https://api.weixin.qq.com/sns/userinfo";



    /**
     * 网页授权基础支持
     *
     */
    public SnsComponent() {
    }

    /**
     * 网页授权基础支持
     *
     * @param weixin 微信对象
     */
    public SnsComponent(Weixin weixin) {
        super(weixin);
    }

    /**
     * 开放的网页授权基础支持
     *
     * @param weixin 微信对象
     * @param authorize_url 第三方网页授权开发URL
     */
    public SnsComponent(Weixin weixin, String authorize_url) {
        super(weixin);
        this.authorize_url = authorize_url;
    }

    /**
     * 静默授权获取openid
     *
     * @param redirect_uri 授权后重定向的回调链接地址（不用编码）
     * @return 静默授权链接
     */
    public String getOAuth2CodeBaseUrl(String redirect_uri) {
        return getOAuth2CodeUrl(redirect_uri, "snsapi_base", "DEFAULT");
    }

    /**
     * 网页安全授权获取用户信息
     *
     * @param redirect_uri 授权后重定向的回调链接地址（不用编码）
     * @return 网页安全授权链接
     */
    public String getOAuth2CodeUserInfoUrl(String redirect_uri) {
        return getOAuth2CodeUrl(redirect_uri, "snsapi_userinfo", "DEFAULT");
    }

    /**
     * 获取授权链接
     *
     * @param redirect_uri 授权后重定向的回调链接地址（不用编码）
     * @param scope 应用授权作用域，snsapi_base
     * （不弹出授权页面，直接跳转，只能获取用户openid），snsapi_userinfo
     * （弹出授权页面，可通过openid拿到昵称、性别、所在地。并且， 即使在未关注的情况下，只要用户授权，也能获取其信息 ）
     * @param state 重定向后会带上state参数，开发者可以填写a-zA-Z0-9的参数值，最多128字节
     * @return 授权链接
     */
    public String getOAuth2CodeUrl(String redirect_uri, String scope, String state) {
        try {
            return authorize_url + "?appid=" + weixin.getAppId() + "&redirect_uri=" + URLEncoder.encode(redirect_uri, "UTF-8") + "&response_type=code&scope=" + scope + "&state=" + state + "&connect_redirect=1#wechat_redirect";
        } catch (UnsupportedEncodingException ex) {
            return "";
        }
    }

    /**
     * 获取微信用户OpenId
     *
     * @param code 仅能使用一次
     * @return 微信用户OpenId
     * @throws org.weixin4j.WeixinException 微信操作异常
     * @since 0.1.0
     */
    public String getOpenId(String code) throws WeixinException {
        SnsAccessToken snsAccessToken = getSnsOAuth2AccessToken(code);
        return snsAccessToken.getOpenid();
    }

    /**
     * 获取网页授权AccessToken
     *
     * @param code 换取身份唯一凭证
     * @return 网页授权AccessToken
     * @throws org.weixin4j.WeixinException 微信操作异常
     * @since 0.1.0
     */
    public SnsAccessToken getSnsOAuth2AccessToken(String appId, String secret, String code) throws WeixinException {
        if (StringUtils.isEmpty(code)) {
            throw new IllegalArgumentException("code can't be null or empty");
        }
        //拼接参数
        String param = "?appid=" + appId + "&secret=" + secret + "&code=" + code + "&grant_type=authorization_code";
        //创建请求对象
        HttpsClient http = new HttpsClient();
        //调用获取access_token接口
        Response res = http.get(getAccessTokenUrl + param);
        //根据请求结果判定，是否验证成功
        JSONObject jsonObj = res.asJSONObject();
        if (jsonObj == null) {
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("getSnsOAuth2AccessToken返回json:{}",jsonObj.toString());
        }
        Object errcode = jsonObj.get("errcode");
        if (errcode != null) {
            //返回异常信息
            throw new WeixinException(getCause(jsonObj.getIntValue("errcode")));
        }
        return new SnsAccessToken(jsonObj);
    }

    /**
     * 获取网页授权AccessToken
     *
     * @param code 换取身份唯一凭证
     * @return 网页授权AccessToken
     * @throws org.weixin4j.WeixinException 微信操作异常
     * @since 0.1.0
     */
    public SnsAccessToken getSnsOAuth2AccessToken(String code) throws WeixinException {
        if (StringUtils.isEmpty(code)) {
            throw new IllegalArgumentException("code can't be null or empty");
        }
        //拼接参数
        String param = "?appid=" + weixin.getAppId() + "&secret=" + weixin.getSecret() + "&code=" + code + "&grant_type=authorization_code";
        //创建请求对象
        HttpsClient http = new HttpsClient();
        //调用获取access_token接口
        Response res = http.get("https://api.weixin.qq.com/sns/oauth2/access_token" + param);
        //根据请求结果判定，是否验证成功
        JSONObject jsonObj = res.asJSONObject();
        if (jsonObj == null) {
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("getSnsOAuth2AccessToken返回json:" + jsonObj.toString());
        }
        Object errcode = jsonObj.get("errcode");
        if (errcode != null) {
            //返回异常信息
            throw new WeixinException(getCause(jsonObj.getIntValue("errcode")));
        }
        return new SnsAccessToken(jsonObj);
    }

    /**
     * 检验授权凭证（access_token）是否有效
     *
     * @param access_token 网页授权接口调用凭证
     * @param openid 用户的唯一标识
     * @return 可用返回true，否则返回false
     * @throws org.weixin4j.WeixinException 微信操作异常
     * @since 0.1.0
     */
    public boolean validateAccessToken(String access_token, String openid) throws WeixinException {
        if (StringUtils.isEmpty(access_token)) {
            throw new IllegalArgumentException("access_token can't be null or empty");
        }
        if (StringUtils.isEmpty(openid)) {
            throw new IllegalArgumentException("openid can't be null or empty");
        }
        //拼接参数
        String param = "?access_token=" + access_token + "&openid=" + openid;
        //创建请求对象
        HttpsClient http = new HttpsClient();
        //调用获取access_token接口
        Response res = http.get("https://api.weixin.qq.com/sns/auth" + param);
        //根据请求结果判定，是否验证成功
        JSONObject jsonObj = res.asJSONObject();
        if (jsonObj != null) {
            if (log.isDebugEnabled()) {
                log.debug("validateAccessToken返回json:" + jsonObj.toString());
            }
            return jsonObj.getIntValue("errcode") == 0;
        }
        return false;
    }

    /**
     * 刷新用户网页授权AccessToken
     *
     * @param refresh_token 用户刷新access_token
     * @return 刷新后的用户网页授权AccessToken
     * @throws org.weixin4j.WeixinException 微信操作异常
     * @since 0.1.0
     */
    public SnsAccessToken refreshToken(String refresh_token) throws WeixinException {
        if (StringUtils.isEmpty(refresh_token)) {
            throw new IllegalArgumentException("refresh_token can't be null or empty");
        }
        //拼接参数
        String param = "?appid=" + weixin.getAppId() + "&refresh_token=" + refresh_token + "&grant_type=refresh_token";
        //创建请求对象
        HttpsClient http = new HttpsClient();
        //调用获取access_token接口
        Response res = http.get("https://api.weixin.qq.com/sns/oauth2/refresh_token" + param);
        //根据请求结果判定，是否验证成功
        JSONObject jsonObj = res.asJSONObject();
        if (jsonObj != null) {
            if (log.isDebugEnabled()) {
                log.debug("refreshToken返回json:" + jsonObj.toString());
            }
            Object errcode = jsonObj.get("errcode");
            if (errcode != null) {
                //返回异常信息
                throw new WeixinException(getCause(jsonObj.getIntValue("errcode")));
            }
            //判断是否登录成功，并判断过期时间
            Object obj = jsonObj.get("access_token");
            //登录成功，设置accessToken和过期时间
            if (obj != null) {
                //设置凭证
                return new SnsAccessToken(jsonObj);
            }
        }
        return null;
    }

    /**
     * 拉取用户信息
     *
     * @param code 换取身份唯一凭证
     * @return 用户对象
     * @throws org.weixin4j.WeixinException 微信操作异常
     */
    public SnsUser getSnsUserByCode(String code) throws WeixinException {
        //默认简体中文
        return getSnsUserByCode(code, "zh_CN");
    }

    /**
     * 拉取用户信息
     *
     * @param code 换取身份唯一凭证
     * @param lang 国家地区语言版本 zh_CN 简体，zh_TW 繁体，en 英语
     * @return 网页授权用户对象
     * @throws org.weixin4j.WeixinException 微信操作异常
     */
    public SnsUser getSnsUserByCode(String code, String lang) throws WeixinException {
        if (StringUtils.isEmpty(code)) {
            throw new IllegalArgumentException("code can't be null or empty");
        }
        if (StringUtils.isEmpty(lang)) {
            throw new IllegalArgumentException("lang can't be null or empty");
        }
        SnsAccessToken snsAccessToken = getSnsOAuth2AccessToken(code);
        return getSnsUser(snsAccessToken.getAccess_token(), snsAccessToken.getOpenid(), lang);
    }

    /**
     * 拉取用户信息
     *
     * @param access_token 网页授权接口调用凭证
     * @param openid 用户的唯一标识
     * @param lang 国家地区语言版本，zh_CN 简体，zh_TW 繁体，en 英语
     * @return 用户对象
     * @throws org.weixin4j.WeixinException 微信操作异常
     */
    public SnsUser getSnsUser(String access_token, String openid, String lang) throws WeixinException {
        if (StringUtils.isEmpty(access_token)) {
            throw new IllegalArgumentException("access_token can't be null or empty");
        }
        if (StringUtils.isEmpty(openid)) {
            throw new IllegalArgumentException("openid can't be null or empty");
        }
        if (StringUtils.isEmpty(lang)) {
            throw new IllegalArgumentException("lang can't be null or empty");
        }
        SnsUser user = null;
        //拼接参数
        String param = "?access_token=" + access_token + "&openid=" + openid + "&lang=" + lang;
        //创建请求对象
        HttpsClient http = new HttpsClient();
        //调用获取access_token接口
        Response res = http.get(getUserInfo + param);
        //根据请求结果判定，是否验证成功
        JSONObject jsonObj = res.asJSONObject();
        if (jsonObj != null) {
            if (log.isDebugEnabled()) {
                log.debug("getSnsUser返回json:" + jsonObj.toString());
            }
            Object errcode = jsonObj.get("errcode");
            if (errcode != null) {
                //返回异常信息
                throw new WeixinException(getCause(jsonObj.getIntValue("errcode")));
            }
            //设置公众号信息
            user = JSONObject.toJavaObject(jsonObj, SnsUser.class);
        }
        return user;
    }
}
