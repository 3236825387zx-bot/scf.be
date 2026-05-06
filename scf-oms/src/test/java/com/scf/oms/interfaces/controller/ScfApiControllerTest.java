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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScfApiController.class)
@Import(GlobalExceptionHandler.class)
class ScfApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScfFacadeService scfFacadeService;

    @Test
    void workspaceEndpointReturnsWrappedPayload() throws Exception {
        when(scfFacadeService.omsWorkspace(any())).thenReturn(Map.of(
                "statusOptions", List.of(Map.of("label", "Created", "value", 10)),
                "logisticsProviders", List.of(),
                "warehouseOptions", List.of(),
                "orders", List.of(Map.of("orderNo", "FO1001")),
                "details", Map.of("1", Map.of("base", Map.of("orderNo", "FO1001")))
        ));

        mockMvc.perform(get("/api/scf/oms/workspace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.orders[0].orderNo").value("FO1001"));
    }

    @Test
    void interceptEndpointReturnsWrappedPayload() throws Exception {
        when(scfFacadeService.submitLgsIntercept(any())).thenReturn(Map.of(
                "orderId", "FO1001",
                "channel", "LGS Gateway",
                "result", "Success",
                "detail", "accepted"
        ));

        mockMvc.perform(post("/api/scf/lgs/intercepts")
                        .contentType("application/json")
                        .content("""
                                {
                                  "orderId": "FO1001",
                                  "providerCode": "SF",
                                  "reason": "Customer refund",
                                  "window": "Allow Intercept"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.orderId").value("FO1001"))
                .andExpect(jsonPath("$.data.result").value("Success"));
    }

    @Test
    void legacyInterceptEndpointRemainsAvailable() throws Exception {
        when(scfFacadeService.submitLgsIntercept(any())).thenReturn(Map.of(
                "orderId", "FO1001",
                "result", "Success"
        ));

        mockMvc.perform(post("/api/scf/lgs/callbacks/intercept")
                        .contentType("application/json")
                        .content("{\"orderId\":\"FO1001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value("FO1001"));
    }

    @Test
    void usersEndpointReturnsWrappedPayload() throws Exception {
        when(scfFacadeService.users()).thenReturn(List.of(Map.of(
                "id", "user-1001",
                "username", "operator"
        )));

        mockMvc.perform(get("/api/scf/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data[0].username").value("operator"));
    }

    @Test
    void exceptionEndpointsReturnContractShape() throws Exception {
        when(scfFacadeService.omsExceptions(any())).thenReturn(Map.of(
                "stats", List.of(Map.of("label", "Exception Tickets", "value", "1")),
                "tickets", List.of(Map.of("ticketNo", "EX8001", "orderNo", "FO202603220001"))
        ));
        when(scfFacadeService.omsExceptionDetail(eq(8001L))).thenReturn(Map.of(
                "ticketNo", "EX8001",
                "orderNo", "FO202603220001",
                "monitorSteps", List.of(Map.of("key", "oms", "label", "OMS accepted request", "state", "done")),
                "availableActions", Map.of("canReleaseInventory", true),
                "compensationLogs", List.of()
        ));

        mockMvc.perform(get("/api/scf/oms/exceptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stats[0].label").value("Exception Tickets"))
                .andExpect(jsonPath("$.data.tickets[0].ticketNo").value("EX8001"));

        mockMvc.perform(get("/api/scf/oms/exceptions/8001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticketNo").value("EX8001"))
                .andExpect(jsonPath("$.data.availableActions.canReleaseInventory").value(true));
    }

    @Test
    void ledgerAndAdjustmentEndpointsReturnContractShape() throws Exception {
        when(scfFacadeService.iscLedger()).thenReturn(Map.of(
                "stats", List.of(Map.of("label", "Global ATP", "value", "100")),
                "skuRows", List.of(Map.of("sku", "SKU-1")),
                "lockRows", List.of(Map.of("orderId", "FO1"))
        ));
        when(scfFacadeService.iscAdjustment()).thenReturn(Map.of(
                "form", Map.of("requestNo", "ADJ-1", "requestType", "manual_adjustment"),
                "ledgerRows", List.of(Map.of("skuCode", "SKU-1"))
        ));

        mockMvc.perform(get("/api/scf/isc/ledger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stats[0].label").value("Global ATP"))
                .andExpect(jsonPath("$.data.skuRows[0].sku").value("SKU-1"))
                .andExpect(jsonPath("$.data.lockRows[0].orderId").value("FO1"));

        mockMvc.perform(get("/api/scf/isc/adjustments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.form.requestNo").value("ADJ-1"))
                .andExpect(jsonPath("$.data.ledgerRows[0].skuCode").value("SKU-1"));
    }

    @Test
    void documentedAliasEndpointsRemainAvailable() throws Exception {
        when(scfFacadeService.lgsDeliveries(any())).thenReturn(Map.of(
                "rows", List.of(Map.of("parcelNo", "LP1001")),
                "carrierOptions", List.of(),
                "statusOptions", List.of(),
                "watchList", List.of()
        ));
        when(scfFacadeService.lgsCallbacks()).thenReturn(Map.of(
                "rows", List.of(Map.of("orderId", "FO1001")),
                "actionForm", Map.of("window", "15m"),
                "windowOptions", List.of(Map.of("label", "15 minutes", "value", "15m"))
        ));
        when(scfFacadeService.splitMergeRequests(any())).thenReturn(Map.of(
                "records", List.of(Map.of("requestNo", "SM1001")),
                "options", Map.of("statuses", List.of())
        ));

        mockMvc.perform(get("/api/scf/lgs/delivery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rows[0].parcelNo").value("LP1001"));

        mockMvc.perform(get("/api/scf/lgs/callback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actionForm.window").value("15m"));

        mockMvc.perform(get("/api/scf/oms/split-merge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].requestNo").value("SM1001"));
    }

    @Test
    void newWmsEndpointsReturnWrappedPayload() throws Exception {
        when(scfFacadeService.wmsWaveDetail("W-20391")).thenReturn(Map.of("waveId", "W-20391"));
        when(scfFacadeService.updateWmsPackingOrder(eq(7301L), any())).thenReturn(Map.of("packageNo", "PK202603180021"));

        mockMvc.perform(get("/api/scf/wms/taskhall/W-20391"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.waveId").value("W-20391"));

        mockMvc.perform(put("/api/scf/wms/packing/7301")
                        .contentType("application/json")
                        .content("{\"packageStatus\":\"packed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.packageNo").value("PK202603180021"));
    }
}
