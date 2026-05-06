package com.scf.oms.domain.repository;

import com.scf.oms.domain.model.FulfillmentOrder;
import java.util.List;
import java.util.Optional;

/**
 * 履约主单仓储接口（领域层）
 *
 * 说明：
 * - 定义聚合根 FulfillmentOrder 的持久化操作。具体的存储实现由基础设施层提供（如数据库实现或内存实现）。
 * - 接口仅暴露领域所需的最小方法，避免泄露技术细节。
 */
public interface FulfillmentOrderRepository {

    /**
     * 保存或更新履约订单聚合根。
     *
     * @param order 要保存的聚合根实例（不能为 null）
     */
    void save(FulfillmentOrder order);

    /**
     * 根据内部订单 ID 查询履约订单。
     *
     * @param orderId 内部订单ID
     * @return Optional 包装的聚合根，如果不存在则为空
     */
    Optional<FulfillmentOrder> findById(String orderId);

    /**
     * 根据外部系统订单 ID 查询履约订单（用于幂等校验）。
     *
     * @param externalId 外部订单ID（幂等键）
     * @return Optional 包装的聚合根，如果不存在则为空
     */
    Optional<FulfillmentOrder> findByExternalId(String externalId);

    /**
     * 查询所有履约订单（仅用于示例/内存实现及测试场景）。
     * 生产环境中若数据量大，请使用分页接口替代。
     *
     * @return 所有履约订单的列表（实现应返回空列表而非 null）
     */
    List<FulfillmentOrder> findAll();
}
