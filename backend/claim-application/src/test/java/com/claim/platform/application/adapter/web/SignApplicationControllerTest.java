package com.claim.platform.application.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.claim.platform.application.usecase.SignApplicationCommandService;
import com.claim.platform.application.usecase.SignApplicationQueryService;
import com.claim.platform.application.usecase.result.SignApplicationResult;
import com.claim.platform.application.domain.exception.SignApplicationNotFoundException;
import com.claim.platform.application.domain.model.SignApplicationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SignApplicationController.class)
class SignApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private GatewayRequestContextResolver contextResolver;
    @MockBean
    private SignApplicationCommandService commandService;
    @MockBean
    private SignApplicationQueryService queryService;

    @Test
    void shouldReturnStableEnvelopeWhenCreatingApplication() throws Exception {
        given(contextResolver.resolve(any())).willReturn(new GatewayRequestContext("tenant-a", "trace-001"));
        given(commandService.submit(any())).willReturn(new SignApplicationResult("SIGN001",
                SignApplicationStatus.IMAGE_NOT_UPLOAD));

        mockMvc.perform(post("/api/v1/sign-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalApplyNo\":\"EXT001\",\"applicantName\":\"张三\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.traceId").value("trace-001"))
                .andExpect(jsonPath("$.data.signNo").value("SIGN001"));
    }

    @Test
    void shouldReturnNotFoundEnvelopeForApplicationOutsideCurrentTenant() throws Exception {
        given(contextResolver.resolve(any())).willReturn(new GatewayRequestContext("tenant-a", "trace-002"));
        given(queryService.get(eq("tenant-a"), eq("SIGN999"))).willThrow(new SignApplicationNotFoundException());

        mockMvc.perform(get("/api/v1/sign-applications/SIGN999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SIGN_APPLICATION_NOT_FOUND"))
                .andExpect(jsonPath("$.traceId").value("trace-002"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
