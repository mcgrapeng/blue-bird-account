package com.zhangpeng.account.core.controller.intercept;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zhangpeng.account.core.cache.RedisClientUtils;
import com.zhangpeng.account.core.controller.RES;
import com.zhangpeng.account.core.conts.Contant;
import com.zhangpeng.account.core.enums.ResultEnum;
import com.zhangpeng.account.core.utils.CookieUtils;
import com.zhangpeng.sso.api.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * session key 前缀
     */
    private static final String SHIRO_REDIS_SESSION_KEY_PREFIX = "blue.bird.shiro.redis.session";
    private static final String WX_REDIS_USER_SESSION_PREFIX = "wx.auth.user.redis_";
    private final String BLUE_BIRD_JSESSIONID="BLUE-BIRD-TOKEN";
    private static final String BLUE_BIRD_REQ_SOURCE="WX_PROGRAM";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //无论访问的地址是不是正确的，都进行登录验证，登录成功后的访问再进行分发，404的访问自然会进入到错误控制器中
        String aid = WebUtils.toHttp(request).getHeader("AID");
        String sessionId;
        if(aid.equalsIgnoreCase(BLUE_BIRD_REQ_SOURCE)){
            sessionId = WebUtils.toHttp(request).getHeader(BLUE_BIRD_JSESSIONID);
            if (StringUtils.isNotBlank(sessionId)) {

                String userStr = RedisClientUtils.get(WX_REDIS_USER_SESSION_PREFIX + sessionId);
                if(StringUtils.isNotBlank(userStr)){
                    User user = JSONObject.parseObject(userStr, User.class);
                    request.setAttribute(Contant.LOGIN_KEY,user);
                    return true;
                }
            }
        }else{
            sessionId = CookieUtils.getCookieValue(request,BLUE_BIRD_JSESSIONID);
            if (StringUtils.isNotBlank(sessionId)) {
                try {
                    //验证当前请求的session是否是已登录的session
                    Session session = (Session) redisTemplate.opsForValue().get(SHIRO_REDIS_SESSION_KEY_PREFIX + "_" +sessionId);
                    if (session != null && sessionId.equals(session.getId())) {
                        User user = (User) session.getAttribute(Contant.LOGIN_KEY);
                        request.setAttribute(Contant.LOGIN_KEY,user);
                        return true;
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        response(response);
        return false;
    }


    private void response(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=utf-8");
        try {
            response.getWriter().write(JSON.toJSONString(RES.of(ResultEnum.用户登陆过期.code, ResultEnum.用户登陆过期.name())));
        } catch (Exception e) {
           log.error(e.getMessage(),e);
        }
    }

}
