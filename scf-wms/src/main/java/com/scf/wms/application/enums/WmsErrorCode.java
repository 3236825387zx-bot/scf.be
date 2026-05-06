package com.scf.wms.application.enums;

import com.scf.common.exception.IErrorCode;

public enum WmsErrorCode implements IErrorCode {
    INVALID_REQUEST("4001", "invalid request"),
    ORDER_NOT_FOUND("4002", "oms order not found"),
    WAREHOUSE_NOT_FOUND("4003", "warehouse not found"),
    OUTBOUND_TASK_NOT_FOUND("4004", "outbound task not found"),
    INVALID_TASK_STATUS("4005", "invalid outbound task status");

    private final String code;
    private final String message;

    WmsErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
