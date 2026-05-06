package com.scf.lgs.application.enums;

import com.scf.common.exception.IErrorCode;

public enum LgsErrorCode implements IErrorCode {
    PROVIDER_NOT_FOUND("500404", "logistics provider not found"),
    PROVIDER_ALREADY_EXISTS("500409", "logistics provider already exists"),
    PROVIDER_CODE_REQUIRED("500001", "providerCode is required"),
    PROVIDER_NAME_REQUIRED("500002", "providerName is required"),
    PARCEL_NOT_FOUND("500410", "parcel not found"),
    PARCEL_ALREADY_EXISTS("500411", "parcel already exists"),
    ORDER_NO_REQUIRED("500004", "orderNo is required"),
    UNKNOWN_ERROR("500500", "unknown error");

    private final String code;
    private final String message;

    LgsErrorCode(String code, String message) {
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
