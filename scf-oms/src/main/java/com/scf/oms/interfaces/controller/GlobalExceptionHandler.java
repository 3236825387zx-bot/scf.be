package com.scf.oms.interfaces.controller;

import com.scf.common.result.Result;
import com.scf.common.exception.BusinessException;
import com.scf.oms.application.enums.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 全局异常处理器：将异常统一转换为 Result 返回给调用方（含中文注释）
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     *
     * @param ex 业务异常
     * @return 包含错误码和详情的统一响应
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public ResponseEntity<Result<Object>> handleBusiness(BusinessException ex) {
        log.warn("业务异常: code={}, message={}, detail={}", ex.getErrorCode().getCode(), ex.getMessage(), ex.getDetail());
        Result<Object> r;
        if (ex.getDetail() != null) {
            r = Result.fail(ex.getErrorCode(), ex.getMessage(), ex.getDetail());
        } else {
            r = Result.fail(ex.getErrorCode(), ex.getMessage());
        }
        return ResponseEntity.status(200).body(r);
    }

    /**
     * 处理未知系统异常
     *
     * @param ex 未知异常
     * @return 包含通用错误码的统一响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<Result<Object>> handleUnknown(Exception ex) {
        log.error("系统未知异常", ex);
        Result<Object> r = Result.fail(ErrorCode.UNKNOWN_ERROR.getCode(), ex.getMessage() == null ? ErrorCode.UNKNOWN_ERROR.getMessage() : ex.getMessage());
        return ResponseEntity.status(200).body(r);
    }
}
