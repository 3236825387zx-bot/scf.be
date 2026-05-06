package com.scf.oms.infrastructure.repository;

import com.scf.oms.domain.model.FulfillmentOrder;
import com.scf.oms.domain.repository.FulfillmentOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Slf4j
public class InMemoryFulfillmentOrderRepository implements FulfillmentOrderRepository {

    private final Map<String, FulfillmentOrder> store = new ConcurrentHashMap<>();
    private final Map<String, String> externalToId = new ConcurrentHashMap<>();

    @Override
    public synchronized void save(FulfillmentOrder order) {
        if (order == null) {
            log.warn("Attempted to save null order");
            return;
        }
        store.put(order.getOrderId(), order);
        if (order.getExternalOrderId() != null) {
            externalToId.put(order.getExternalOrderId(), order.getOrderId());
        }
    }

    @Override
    public Optional<FulfillmentOrder> findById(String orderId) {
        return orderId == null ? Optional.empty() : Optional.ofNullable(store.get(orderId));
    }

    @Override
    public Optional<FulfillmentOrder> findByExternalId(String externalId) {
        if (externalId == null) {
            return Optional.empty();
        }
        String id = externalToId.get(externalId);
        return id == null ? Optional.empty() : Optional.ofNullable(store.get(id));
    }

    @Override
    public List<FulfillmentOrder> findAll() {
        return new ArrayList<>(store.values());
    }
}
