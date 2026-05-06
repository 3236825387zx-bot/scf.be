package com.scf.oms.interfaces.controller;

import com.scf.oms.application.service.ScfFacadeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
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

@WebMvcTest(OmsWorkspaceController.class)
@Import(GlobalExceptionHandler.class)
class OmsWorkspaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScfFacadeService scfFacadeService;

    @Test
    void ordersEndpointReturnsWrappedPayload() throws Exception {
        when(scfFacadeService.omsOrders(any())).thenReturn(Map.of(
                "list", List.of(Map.of("orderNo", "FO1001", "status", 40)),
                "pageNo", 1,
                "pageSize", 10,
                "total", 1
        ));

        mockMvc.perform(get("/api/oms/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].orderNo").value("FO1001"));
    }

    @Test
    void dispatchEndpointReturnsWrappedPayload() throws Exception {
        when(scfFacadeService.dispatchUpstreamOrder(eq(9001L))).thenReturn(Map.of(
                "result", Map.of("fulfillmentOrderNo", "FO9001")
        ));

        mockMvc.perform(post("/api/oms/upstream-orders/9001/dispatch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.result.fulfillmentOrderNo").value("FO9001"));
    }
}
