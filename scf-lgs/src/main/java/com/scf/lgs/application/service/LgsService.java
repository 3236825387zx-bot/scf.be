package com.scf.lgs.application.service;

import java.util.List;
import java.util.Map;

public interface LgsService {
    List<Map<String, Object>> estimateRoute(Map<String, Object> request);

    Map<String, Object> listProviders(Map<String, String> query);

    Map<String, Object> getProvider(String providerCode);

    Map<String, Object> createProvider(Map<String, Object> request);

    Map<String, Object> updateProvider(String providerCode, Map<String, Object> request);

    Map<String, Object> updateProviderStatus(String providerCode, boolean enabled);

    Map<String, Object> listParcels(Map<String, String> query);

    Map<String, Object> getParcel(String parcelNo);

    Map<String, Object> deliverParcel(Map<String, Object> request);

    Map<String, Object> signParcel(String parcelNo, Map<String, Object> request);

    Map<String, Object> appendTracking(String parcelNo, Map<String, Object> request);

    Map<String, Object> dashboard();
}
