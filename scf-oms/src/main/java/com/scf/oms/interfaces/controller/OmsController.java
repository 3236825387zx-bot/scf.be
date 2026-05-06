package com.scf.oms.interfaces.controller;

import com.scf.oms.application.service.FulfillmentOrderService;
import com.scf.common.result.Result;
import com.scf.oms.domain.model.FulfillmentOrder;
import com.scf.oms.interfaces.dto.OrderCancelReq;
import com.scf.oms.interfaces.dto.OrderCreateReq;
import com.scf.common.exception.BusinessException;
import com.scf.oms.application.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 履约订单管理控制器
 * 提供订单的创建、取消、查询等接口
 * 说明：
 * - Controller 层负责协议/入参校验与异常边界处理（将异常转换为统一的 Result 返回）
 * - 业务错误在 Service 层以 BusinessException 抛出，Controller 在此捕获并返回带有 detail 的 Result
 */
@RestController
@RequestMapping("/api/oms/order")
@RequiredArgsConstructor
@Slf4j
public class OmsController {

    private final FulfillmentOrderService fulfillmentOrderApplicationService;

    /**
     * 提交履约订单
     *
     * @param req 订单创建请求参数
     * @return 统一包装的 Result，data 为生成的内部订单号
     */
    @PostMapping("/submit")
    public Result<String> submitOrder(@RequestBody OrderCreateReq req) {
        log.info("收到提交订单请求: externalOrderId={}", req.getExternalOrderId());
        try {
            String orderId = fulfillmentOrderApplicationService.createOrder(req);
            return Result.ok(orderId);
        } catch (BusinessException ex) {
            // 业务异常：返回明确的错误码、消息与 detail
            log.warn("创建订单业务异常, code={}, msg={}, detail={}", ex.getErrorCode().getCode(), ex.getMessage(), ex.getDetail());
            return Result.fail(ex.getErrorCode(), ex.getMessage(), ex.getDetail());
        } catch (Exception ex) {
            // 系统异常：记录堆栈并返回通用错误
            log.error("创建订单发生未知异常", ex);
            return Result.fail(ErrorCode.UNKNOWN_ERROR, ex.getMessage() == null ? ErrorCode.UNKNOWN_ERROR.getMessage() : ex.getMessage());
        }
    }

    /**
     * 取消订单
     *
     * @param req 订单取消请求参数
     * @return 是否取消成功
     */
    @PostMapping("/cancel")
    public Result<Boolean> cancelOrder(@RequestBody OrderCancelReq req) {
        log.info("收到取消订单请求: orderId={}, reason={}", req.getOrderId(), req.getReason());
        try {
            Boolean ok = fulfillmentOrderApplicationService.cancelOrder(req.getOrderId(), req.getReason());
            return Result.ok(ok);
        } catch (BusinessException ex) {
            log.warn("取消订单业务异常, code={}, msg={}, detail={}", ex.getErrorCode().getCode(), ex.getMessage(), ex.getDetail());
            return Result.fail(ex.getErrorCode(), ex.getMessage(), ex.getDetail());
        } catch (Exception ex) {
            log.error("取消订单发生未知异常", ex);
            return Result.fail(ErrorCode.UNKNOWN_ERROR, ex.getMessage() == null ? ErrorCode.UNKNOWN_ERROR.getMessage() : ex.getMessage());
        }
    }

    /**
     * 根据订单ID查询订单详情
     *
     * @param orderId 订单ID
     * @return 统一包装的 Result，data 为订单详情（若不存在，data 为 null）
     */
    @GetMapping("/{orderId}")
    public Result<FulfillmentOrder> getOrder(@PathVariable("orderId") String orderId) {
        log.info("收到查询订单请求: orderId={}", orderId);
        try {
            FulfillmentOrder order = fulfillmentOrderApplicationService.getOrder(orderId).orElse(null);
            return Result.ok(order);
        } catch (BusinessException ex) {
            log.warn("查询订单业务异常, code={}, msg={}, detail={}", ex.getErrorCode().getCode(), ex.getMessage(), ex.getDetail());
            return Result.fail(ex.getErrorCode(), ex.getMessage(), ex.getDetail());
        } catch (Exception ex) {
            log.error("查询订单发生未知异常", ex);
            return Result.fail(ErrorCode.UNKNOWN_ERROR, ex.getMessage() == null ? ErrorCode.UNKNOWN_ERROR.getMessage() : ex.getMessage());
        }
    }

    /**
     * 查询订单列表（示例/测试用，生产环境请考虑分页/筛选）
     *
     * @return 统一包装的 Result，data 为订单列表（可能为空列表）
     */
    @PostMapping("/page")
    public Result<List<FulfillmentOrder>> getOrders() {
        log.info("收到查询订单列表请求");
        try {
            List<FulfillmentOrder> orders = fulfillmentOrderApplicationService.getOrders();
            return Result.ok(orders);
        } catch (BusinessException ex) {
            log.warn("查询订单列表业务异常, code={}, msg={}, detail={}", ex.getErrorCode().getCode(), ex.getMessage(), ex.getDetail());
            return Result.fail(ex.getErrorCode(), ex.getMessage(), ex.getDetail());
        } catch (Exception ex) {
            log.error("查询订单列表发生未知异常", ex);
            return Result.fail(ErrorCode.UNKNOWN_ERROR, ex.getMessage() == null ? ErrorCode.UNKNOWN_ERROR.getMessage() : ex.getMessage());
        }
    }
}
