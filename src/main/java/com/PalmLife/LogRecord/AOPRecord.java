package com.PalmLife.LogRecord;



import com.PalmLife.dto.UserDTO;
import com.PalmLife.entity.LogEntity;
import com.PalmLife.entity.User;
import com.PalmLife.service.LogService;
import com.PalmLife.utils.UserHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;


import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * AOP实现记录日志功能
 */
@Aspect
@Component
public class AOPRecord {

    @Resource
    private LogService logService;

    @Around("@annotation(com.PalmLife.LogRecord.AOPLog)")
    public Object logRecord(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        long beginTime = System.currentTimeMillis();
        Object result = proceedingJoinPoint.proceed();
        long time = System.currentTimeMillis() - beginTime;

        saveLog(proceedingJoinPoint,time);

        return result;
    }

    public void saveLog(ProceedingJoinPoint joinPoint, long time){
        MethodSignature signature = (MethodSignature) joinPoint.getSignature(); //获取方法签名
        Method method = signature.getMethod();  //获取被拦截的方法对象

        LogEntity sysLog = new LogEntity();

        //注解上的操作类型内容记录
        AOPLog aopLog = method.getAnnotation(AOPLog.class);
        if(aopLog != null){
            sysLog.setDescription(aopLog.value());
        }

        //当前操作用户名
        UserDTO user = UserHolder.getUser();
        String name = user.getNickName();
        sysLog.setUserName(name);


        //请求的方法名
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = signature.getName();
        sysLog.setMethod(className + "." + methodName + "()");

        //请求的参数
        Object[] args = joinPoint.getArgs();
        try{
            String params = Arrays.toString(args);
            sysLog.setParams(params);
        }catch (Exception e){
            throw new RuntimeException();
        }


        logService.save(sysLog);
    }


}
