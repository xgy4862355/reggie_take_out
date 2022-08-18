package com.xgy.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器
 */

//指定拦截带RestController注解的Controller 和 普通Controller
@ControllerAdvice(annotations = {RestController.class, Controller.class})
@Slf4j
@ResponseBody//给前端返回Json数据
public class GlobalExceptionHandler {

    /**
     * 自定义SQLIntegrityConstraintViolationException异常处理方法
     * @param ex
     * @return
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public R<String> exceptionHandler(SQLIntegrityConstraintViolationException ex){
        log.error(ex.getMessage());

        if(ex.getMessage().contains("Duplicate entry")){
            String[] split = ex.getMessage().split(" ");
            String msg = "账号"+split[2] + "已存在！";
            return R.error(msg);
        }

        //在这里返回信息给前端
        return R.error("未知错误！");
    }


    /**
     * 自定义CustomException异常处理方法
     * @param ex
     * @return
     */
    @ExceptionHandler(CustomException.class)
    public R<String> exceptionHandler(CustomException ex){
        log.error(ex.getMessage());

        //在这里返回信息给前端
        return R.error(ex.getMessage());
    }
}
