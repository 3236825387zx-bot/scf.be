package com.scf.common.exception;

/**
 * 业务异常基类
 * 包含错误码接口和可选的详细信息
 */
public class BusinessException extends RuntimeException {
    private final IErrorCode errorCode;
    // 可选的详细信息（用于在 Controller 层或 Result.data 中携带更多错误上下文）
    private final Object detail;

    public BusinessException(IErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = null;
    }

    public BusinessException(IErrorCode errorCode, String message) {
        super(message == null ? errorCode.getMessage() : message);
        this.errorCode = errorCode;
        this.detail = null;
    }

    public BusinessException(IErrorCode errorCode, String message, Object detail) {
        super(message == null ? errorCode.getMessage() : message);
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public IErrorCode getErrorCode() {
        return errorCode;
    }

    public Object getDetail() {
        return detail;
    }
}
