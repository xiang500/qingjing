package com.qingjing.common;

/**
 * 业务错误码常量。
 */
public final class ErrorCode {

    /** 参数校验失败 */
    public static final int BAD_REQUEST = 40000;
    /** 未登录 / 身份失效 */
    public static final int UNAUTHORIZED = 40100;
    /** 资源不存在 */
    public static final int NOT_FOUND = 40400;
    /** 服务器内部错误 */
    public static final int INTERNAL_ERROR = 50000;

    private ErrorCode() {
    }
}