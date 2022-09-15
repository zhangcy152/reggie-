package com.itheima.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器
 *  底层基于代理  代理controller 通过Aop  拦截到方法
 */
@ControllerAdvice(annotations = {RestController.class, Controller.class})
@ResponseBody  // 方法返回json数据
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 异常处理方法
     * @return
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public R<String> exceptionHandler(SQLIntegrityConstraintViolationException ex){
        log.error(ex.getMessage());

        //判断为哪种异常
        if (ex.getMessage().contains("Duplicate entry")){
            String[] split = ex.getMessage().split(" "); //根据空格分隔异常 数据在第三个
            String msg = split[2] + "已经存在!";
            return R.error(msg);
        }

        return R.error("网络繁忙,请稍后重试!");
    }


    /**
     * 异常处理方法  自定义业务异常
     * @return
     */
    @ExceptionHandler(CustomException.class)
    public R<String> exceptionHandler(CustomException ex){
        log.error(ex.getMessage());
        return R.error(ex.getMessage());
    }
}
