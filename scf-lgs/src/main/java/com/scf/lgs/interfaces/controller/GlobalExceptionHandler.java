package com.scf.lgs.interfaces.controller;

import com.scf.common.exception.BusinessException;
import com.scf.common.result.Result;
import com.scf.lgs.application.enums.LgsErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public ResponseEntity<Result<Object>> handleBusiness(BusinessException ex) {
        log.warn("LGS business exception: code={}, message={}, detail={}",
                ex.getErrorCode().getCode(), ex.getMessage(), ex.getDetail());
        Result<Object> result = ex.getDetail() == null
                ? Result.fail(ex.getErrorCode(), ex.getMessage())
                : Result.fail(ex.getErrorCode(), ex.getMessage(), ex.getDetail());
        return ResponseEntity.ok(result);
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<Result<Object>> handleUnknown(Exception ex) {
        log.error("LGS unexpected exception", ex);
        return ResponseEntity.ok(Result.fail(LgsErrorCode.UNKNOWN_ERROR.getCode(), ex.getMessage() == null ? LgsErrorCode.UNKNOWN_ERROR.getMessage() : ex.getMessage()));
    }
}
