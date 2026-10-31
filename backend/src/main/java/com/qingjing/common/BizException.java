package com.qingjing.common;

/**
 * 业务异常：由全局异常处理器转换为统一响应，HTTP 状态码仍保持 200，
 * 便于前端用同一套 response 拦截逻辑处理。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(ErrorCode.BAD_REQUEST, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}