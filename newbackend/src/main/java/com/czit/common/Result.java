package com.czit.common;

import java.io.Serializable;
public class Result implements Serializable {
    /**
     * 状态码，例如 0-成功 -1-失败   40001-权限不足  50001-系统异常 ...
     */
    private Integer code;
    /**
     * 附加文本信息 ，例如：请求成功/请求失败/xxx异常
     */
    private String msg;
    /**
     * 返回到客户端的附加数据
     */
    private Object data;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    // ------------------------------------------------------------------
    // 静态工厂：统一在 Result 上构造响应，避免 Controller 里写 setCode/setMsg/setData
    // ------------------------------------------------------------------

    /** 成功且不带数据 */
    public static Result ok() {
        return of(0, "ok", null);
    }

    /** 成功并携带数据 */
    public static Result ok(Object data) {
        return of(0, "ok", data);
    }

    /** 业务失败 */
    public static Result fail(int code, String msg) {
        return of(code, msg, null);
    }

    public static Result of(int code, String msg, Object data) {
        Result result = new Result();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }

    @Override
    public String toString() {
        return "Result{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
    }
}
