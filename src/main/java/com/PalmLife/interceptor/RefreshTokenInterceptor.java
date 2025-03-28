package com.PalmLife.interceptor;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.PalmLife.dto.UserDTO;
import com.PalmLife.utils.RedisConstants;
import com.PalmLife.utils.UserHolder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 刷新令牌拦截器
 *
 * @author CHEN
 * @date 2022/10/07
 */
@Component
public class RefreshTokenInterceptor implements HandlerInterceptor {
    private final StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 进入servelt之前执行刷新令牌
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler){
        //从请求头中获取token
        String token = request.getHeader("authorization");
        if (StringUtils.isEmpty(token)) {
            //不存在token
            return true;
        }
        //从redis中获取用户
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY + token);
        //用户不存在
        if (userMap.isEmpty()) {
            return true;
        }
        //将redis提取出的userMap转换为UserDTO存入ThreadLocal
        UserHolder.saveUser(BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false));   //false作用忽略大小写
        //token续命
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        return true;
    }

    /**
     * 退出servelt之后清除ThreadLocal中的用户信息
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
