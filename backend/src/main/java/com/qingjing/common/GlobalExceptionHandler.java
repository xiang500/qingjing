package com.qingjing.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：把任何异常收敛成 ApiResult，前端只需处理一种结构。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ApiResult<Void> handleBiz(BizException ex) {
        log.warn("业务异常: {}", ex.getMessage());
        return ApiResult.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ApiResult<Void> handleValidation(BindException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError == null
                ? "请求参数不合法"
                : fieldError.getField() + " " + fieldError.getDefaultMessage();
        return ApiResult.fail(ErrorCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResult<Void> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("请求体解析失败: {}", ex.getMessage());
        return ApiResult.fail(ErrorCode.BAD_REQUEST, "请求体格式不正确");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ApiResult<Void> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        log.warn("不支持的 Content-Type: {}", ex.getContentType());
        return ApiResult.fail(ErrorCode.BAD_REQUEST, "请求 Content-Type 需为 application/json");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResult<Void> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return ApiResult.fail(ErrorCode.BAD_REQUEST, "不支持的请求方法: " + ex.getMethod());
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleUnexpected(Exception ex) {
        log.error("未捕获异常", ex);
        return ApiResult.fail(ErrorCode.INTERNAL_ERROR, "服务开小差了，请稍后重试");
    }
}