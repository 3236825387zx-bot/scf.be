package com.scf.oms.application.enums;

import com.scf.common.exception.IErrorCode;

public enum ErrorCode implements IErrorCode {
    SUCCESS("0", "success"),
    UNKNOWN_ERROR("900500", "system error"),
    VALIDATION_ERROR("900001", "invalid request"),
    BUSINESS_ERROR("900001", "business validation failed"),
    NOT_FOUND("900404", "resource not found"),
    USER_NOT_FOUND("100100", "user does not exist"),
    PASSWORD_INCORRECT("100101", "password is incorrect"),
    USER_DISABLED("100102", "user is disabled"),
    REGISTER_INFO_INCOMPLETE("100200", "registration fields are incomplete"),
    PASSWORDS_NOT_MATCH("100201", "passwords do not match"),
    USER_ALREADY_EXISTS("100202", "user already exists"),
    UPSTREAM_ORDER_NOT_FOUND("200100", "upstream order not found"),
    UPSTREAM_ORDER_ALREADY_DISPATCHED("200101", "upstream order already dispatched"),
    INVENTORY_LOCK_FAILED("200102", "inventory lock failed"),
    WMS_DISPATCH_FAILED("200103", "wms dispatch failed"),
    OMS_ORDER_NOT_FOUND("300100", "oms order not found"),
    RULE_NAME_REQUIRED("300200", "ruleName is required"),
    RULE_CONDITIONS_REQUIRED("300201", "conditions are required"),
    RULE_PRIORITY_INVALID("300202", "priority is invalid"),
    SPLIT_MERGE_SOURCE_REQUIRED("300300", "sourceOrderNos are required"),
    MERGE_REQUIRES_TWO_ORDERS("300301", "merge requires at least two source orders"),
    SPLIT_MERGE_STRATEGY_REQUIRED("300302", "strategy is required"),
    TARGET_WAREHOUSE_REQUIRED("300303", "targetWarehouse is required"),
    SPLIT_MERGE_REASON_REQUIRED("300304", "reason is required"),
    SPLIT_MERGE_REQUEST_NOT_FOUND("300400", "split/merge request not found"),
    SPLIT_MERGE_REQUEST_STATUS_INVALID("300401", "only pending request can be executed"),
    SPLIT_MERGE_CANCEL_NOT_FOUND("300500", "split/merge request not found"),
    SPLIT_MERGE_CANCEL_STATUS_INVALID("300501", "only pending request can be cancelled"),
    NO_AVAILABLE_WAREHOUSE("300600", "no available warehouse");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
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
