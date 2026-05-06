package com.scf.wms.application.service;

import com.scf.wms.interfaces.dto.OutboundCreateRequest;
import com.scf.wms.interfaces.dto.TaskActionRequest;
import com.scf.wms.interfaces.dto.TaskShipRequest;

import java.util.Map;

public interface WmsOutboundService {

    boolean createOutboundTask(OutboundCreateRequest request);

    Map<String, Object> listTasks(Map<String, String> query);

    Map<String, Object> getTask(String taskNo);

    Map<String, Object> getTaskByOrderNo(String orderNo);

    Map<String, Object> pickTask(String taskNo, TaskActionRequest request);

    Map<String, Object> shipTask(String taskNo, TaskShipRequest request);

    Map<String, Object> dashboard();
}
