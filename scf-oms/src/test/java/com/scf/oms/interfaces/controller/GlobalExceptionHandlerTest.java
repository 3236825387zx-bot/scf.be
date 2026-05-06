package com.scf.oms.interfaces.controller;

import com.scf.common.exception.BusinessException;
import com.scf.oms.application.enums.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void businessExceptionIsConvertedToResultPayload() throws Exception {
        mockMvc.perform(get("/test/business").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("900404"))
                .andExpect(jsonPath("$.message").value("order not found"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.orderId").value("FO404"));
    }

    @Test
    void unknownExceptionIsConvertedToResultPayload() throws Exception {
        mockMvc.perform(get("/test/system").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("900500"))
                .andExpect(jsonPath("$.message").value("system boom"))
                .andExpect(jsonPath("$.success").value(false));
    }

    @RestController
    @RequestMapping("/test")
    static class ThrowingController {

        @GetMapping("/business")
        public void business() {
            throw new BusinessException(ErrorCode.NOT_FOUND, "order not found", Map.of("orderId", "FO404"));
        }

        @GetMapping("/system")
        public void system() {
            throw new RuntimeException("system boom");
        }
    }
}
