package com.scf.common.exception;

/**
 * 错误码接口定义
 * 各个微服务可实现此接口定义自己的错误码枚举
 */
public interface IErrorCode {
    /**
     * 获取错误码
     * @return 错误码字符串
     */
    String getCode();

    /**
     * 获取错误信息
     * @return 错误描述
     */
    String getMessage();
}
