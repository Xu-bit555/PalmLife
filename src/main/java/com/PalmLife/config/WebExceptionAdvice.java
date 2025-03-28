package com.PalmLife.config;

import com.PalmLife.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;
import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器
 */

/**
 * @author CHEN
 * @date 2022/10/07
 */
@Slf4j
@RestControllerAdvice   // 全局异常处理注解
public class WebExceptionAdvice {

    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException e) {
        log.error(e.toString(), e);
        return Result.fail("服务器异常");
    }

    //专门处理请求异常
    @ExceptionHandler(value = {SQLIntegrityConstraintViolationException.class,MethodArgumentNotValidException.class, HttpMessageNotReadableException.class, ConstraintViolationException.class})
    @ResponseBody
    public Result ArgumentValidExceptionHandler(Exception ignored) {
        return Result.fail("请检查参数是否正确");
    }


    //专门处理请求参数异常
    @ExceptionHandler(value = {MissingServletRequestParameterException.class})
    @ResponseBody
    public Result MissingServletParameterExceptionHandler(Exception e) {
        return Result.fail("请核对参数，以免重新登录");
    }

}
