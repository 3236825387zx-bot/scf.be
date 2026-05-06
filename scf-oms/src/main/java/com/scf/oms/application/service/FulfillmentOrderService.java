package com.scf.oms.application.service;

import com.scf.oms.domain.model.FulfillmentOrder;
import com.scf.oms.interfaces.dto.OrderCreateReq;

import java.util.List;
import java.util.Optional;

/**
 * 履约主单应用服务接口（应用层）
 *
 * 责任：
 * - 提供领域用例级别的能力（创建订单、取消订单、查询订单等），
 *   将领域层的聚合与领域服务、仓储以及外部服务进行编排。
 * - 该接口位于应用层，目的是将用例逻辑与具体实现解耦，便于替换实现或进行单元测试。
 */
public interface FulfillmentOrderService {

    /**
     * 创建并调度履约订单。
     *
     * @param req 订单创建请求（外部订单号作为幂等键）
     * @return 内部生成的订单ID（String），若创建失败可抛出运行时异常
     */
    String createOrder(OrderCreateReq req);

    /**
     * 取消履约订单。
     *
     * @param orderId 内部订单ID
     * @param reason  取消原因（用于日志或下游通知）
     * @return true 表示取消成功
     */
    Boolean cancelOrder(String orderId, String reason);

    /**
     * 根据内部订单ID查询履约订单聚合根（返回 Optional）
     *
     * @param orderId 内部订单ID
     * @return Optional 包装的 FulfillmentOrder（不存在时为 Optional.empty()）
     */
    Optional<FulfillmentOrder> getOrder(String orderId);

    /**
     * 查询订单列表（示例/测试用，生产环境请考虑分页/筛选）。
     *
     * @return 所有履约订单的列表（实现可返回空列表）
     */
    List<FulfillmentOrder> getOrders();

}
