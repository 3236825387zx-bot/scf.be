package com.scf.oms.interfaces.controller;

import com.scf.common.exception.BusinessException;
import com.scf.oms.application.enums.ErrorCode;
import com.scf.oms.application.service.FulfillmentOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OmsController.class)
class OmsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FulfillmentOrderService fulfillmentOrderService;

    @Test
    void submitOrderReturnsBusinessErrorPayload() throws Exception {
        when(fulfillmentOrderService.createOrder(any())).thenThrow(
                new BusinessException(ErrorCode.VALIDATION_ERROR, "skuList must not be empty", Map.of("field", "skuList"))
        );

        mockMvc.perform(post("/api/oms/order/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalOrderId": "EXT-1",
                                  "receiverName": "Alice",
                                  "receiverPhone": "13800138000",
                                  "province": "Hubei",
                                  "city": "Wuhan",
                                  "district": "Hongshan",
                                  "detailAddress": "Optics Valley Road 188",
                                  "skuList": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("900001"))
                .andExpect(jsonPath("$.message").value("skuList must not be empty"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.field").value("skuList"));
    }

    @Test
    void cancelOrderReturnsUnknownErrorPayload() throws Exception {
        when(fulfillmentOrderService.cancelOrder(eq("FO1"), eq("manual cancel"))).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/api/oms/order/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "FO1",
                                  "reason": "manual cancel"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("900500"))
                .andExpect(jsonPath("$.message").value("boom"))
                .andExpect(jsonPath("$.success").value(false));
    }
}
