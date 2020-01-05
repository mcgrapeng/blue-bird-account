package com.zhangpeng.account.core.aop;

import com.zhangpeng.account.core.utils.CookieUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Aspect
public class NoRepeatSubmitHandler {


    private final String BLUE_BIRD_JSESSIONID="BLUE-BIRD-TOKEN";

    @Autowired
    private RedisTemplate<String, Integer> template;


    @Around("execution(* com.zhangpeng..*Controller.*(..)) && @annotation(nrs)")
    public Object arround(ProceedingJoinPoint pjp, NoRepeatSubmit nrs) {
        ValueOperations<String, Integer> opsForValue = template.opsForValue();
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes.getRequest();

            String sessionId = CookieUtils.getCookieValue(request,BLUE_BIRD_JSESSIONID);
            String key = sessionId + "-" + request.getRequestURI();
            // 如果缓存中有这个url视为重复提交
            if (opsForValue.get(key) == null) {
                Object o = pjp.proceed();
                opsForValue.set(key, 0, 2, TimeUnit.SECONDS);
                return o;
            } else {
                log.error("重复提交的请求，已成功拦截！");
                return null;
            }
        } catch (Throwable e) {
            log.error("验证重复提交时出现未知异常!",e);
            return "{\"code\":-889,\"message\":\"验证重复提交时出现未知异常!\"}";
        }
    }
}
