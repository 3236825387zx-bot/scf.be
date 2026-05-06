package com.scf.wms.interfaces.controller;

import com.scf.common.exception.BusinessException;
import com.scf.wms.application.enums.WmsErrorCode;
import com.scf.wms.application.service.WmsOutboundService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WmsOutboundController.class)
@Import(GlobalExceptionHandler.class)
class WmsOutboundControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WmsOutboundService wmsOutboundService;

    @Test
    void listTasksReturnsWrappedPayload() throws Exception {
        when(wmsOutboundService.listTasks(any())).thenReturn(Map.of(
                "list", List.of(Map.of("taskNo", "WT1", "status", "created")),
                "pageNo", 1,
                "pageSize", 1,
                "total", 1
        ));

        mockMvc.perform(get("/api/wms/outbound/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].taskNo").value("WT1"));
    }

    @Test
    void shipTaskReturnsBusinessErrorPayload() throws Exception {
        when(wmsOutboundService.shipTask(eq("WT404"), any())).thenThrow(
                new BusinessException(WmsErrorCode.OUTBOUND_TASK_NOT_FOUND, "outbound task not found: WT404", Map.of("taskNo", "WT404"))
        );

        mockMvc.perform(post("/api/wms/outbound/tasks/WT404/ship")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operator": "shipper"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("4004"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.taskNo").value("WT404"));
    }
}
