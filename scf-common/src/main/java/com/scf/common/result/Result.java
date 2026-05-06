package com.scf.common.result;

import com.scf.common.exception.IErrorCode;

import java.io.Serializable;
import java.util.Objects;

/**
 * 通用 API 响应封装类
 *
 * 约定：
 * - code = "0" 表示成功；非 "0" 表示失败
 * - message 为错误或提示信息
 * - success 为布尔值，表示是否成功
 * - data 为具体响应数据
 */
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private String code;
    private String message;
    private Boolean success;
    private T data;

    public Result() {}

    public Result(String code, String message, Boolean success, T data) {
        this.code = code;
        this.message = message;
        this.success = success;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>("0", "success", true, null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>("0", "success", true, data);
    }

    public static <T> Result<T> ok(String message, T data) {
        return new Result<>("0", message, true, data);
    }

    public static <T> Result<T> fail() {
        return new Result<>("-1", "error", false, null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>("-1", message, false, null);
    }

    public static <T> Result<T> fail(String code, String message) {
        return new Result<>(code, message, false, null);
    }

    public static <T> Result<T> fail(IErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), false, null);
    }

    public static <T> Result<T> fail(IErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message == null ? errorCode.getMessage() : message, false, null);
    }

    // 新增：允许携带 detail（例如 BusinessException 中的 detail）作为 data 返回
    @SuppressWarnings("unchecked")
    public static <T> Result<T> fail(IErrorCode errorCode, String message, Object detail) {
        return new Result<>(errorCode.getCode(), message == null ? errorCode.getMessage() : message, false, (T) detail);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Result)) return false;
        Result<?> result = (Result<?>) o;
        return Objects.equals(code, result.code) && Objects.equals(message, result.message) && Objects.equals(success, result.success) && Objects.equals(data, result.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, message, success, data);
    }

    @Override
    public String toString() {
        return "Result{" +
                "code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", success=" + success +
                ", data=" + data +
                '}';
    }
}
