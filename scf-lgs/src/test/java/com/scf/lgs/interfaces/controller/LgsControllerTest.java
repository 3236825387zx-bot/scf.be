package com.scf.lgs.interfaces.controller;

import com.scf.common.exception.BusinessException;
import com.scf.lgs.application.enums.LgsErrorCode;
import com.scf.lgs.application.service.LgsService;
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

@WebMvcTest(LgsController.class)
@Import(GlobalExceptionHandler.class)
class LgsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LgsService lgsService;

    @Test
    void listProvidersReturnsWrappedPayload() throws Exception {
        when(lgsService.listProviders(any())).thenReturn(Map.of(
                "list", List.of(Map.of("providerCode", "sf", "providerName", "SF Express")),
                "pageNo", 1,
                "pageSize", 10,
                "total", 1
        ));

        mockMvc.perform(get("/api/lgs/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].providerCode").value("sf"));
    }

    @Test
    void signParcelReturnsBusinessErrorPayload() throws Exception {
        when(lgsService.signParcel(eq("LP404"), any())).thenThrow(
                new BusinessException(LgsErrorCode.PARCEL_NOT_FOUND, "parcel not found: LP404", Map.of("parcelNo", "LP404"))
        );

        mockMvc.perform(post("/api/lgs/parcels/LP404/sign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("500410"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.parcelNo").value("LP404"));
    }
}
