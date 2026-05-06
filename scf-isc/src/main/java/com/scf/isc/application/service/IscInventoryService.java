package com.scf.isc.application.service;

import java.util.List;
import java.util.Map;

public interface IscInventoryService {

    List<Map<String, Object>> queryAtp(Map<String, Object> req);

    Boolean lockStock(Map<String, Object> req);

    Boolean unlockStock(Map<String, Object> req);

    Map<String, Object> ledger();

    Map<String, Object> adjustment();

    Map<String, Object> alerts();
}
