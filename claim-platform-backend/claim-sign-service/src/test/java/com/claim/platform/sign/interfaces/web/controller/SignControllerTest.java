package com.claim.platform.sign.interfaces.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.claim.platform.sign.application.command.SignCommand;
import com.claim.platform.sign.application.handler.SignCommandHandler;
import com.claim.platform.sign.application.service.SignQueryService;
import com.claim.platform.sign.application.result.SignResult;
import com.claim.platform.sign.domain.exception.SignNotFoundException;
import com.claim.platform.sign.domain.model.SignStatus;
import com.claim.platform.sign.interfaces.web.context.GatewayRequestContext;
import com.claim.platform.sign.interfaces.web.context.GatewayRequestContextResolver;
import com.claim.platform.sign.interfaces.web.converter.PersonRequestConverter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SignController.class)
class SignControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private GatewayRequestContextResolver contextResolver;
    @MockBean
    private SignCommandHandler commandService;
    @MockBean
    private SignQueryService queryService;
    @MockBean
    private PersonRequestConverter personRequestConverter;

    @Test
    void shouldReturnStableEnvelopeWhenCreatingSign() throws Exception {
        given(contextResolver.resolve(any())).willReturn(new GatewayRequestContext("tenant-a", "trace-001"));
        given(commandService.submit(any())).willReturn(new SignResult("SIGN001",
                SignStatus.IMAGE_NOT_UPLOAD));

        mockMvc.perform(post("/api/v1/claim-intakes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalApplyNo\":\"EXT001\",\"applicantName\":\"张三\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.traceId").value("trace-001"))
                .andExpect(jsonPath("$.data.signNo").value("SIGN001"));

        ArgumentCaptor<SignCommand> captor = ArgumentCaptor.forClass(SignCommand.class);
        verify(commandService).submit(captor.capture());
        assertThat(captor.getValue().materials()).isEmpty();
    }

    @Test
    void shouldReturnNotFoundEnvelopeForSignOutsideCurrentTenant() throws Exception {
        given(contextResolver.resolve(any())).willReturn(new GatewayRequestContext("tenant-a", "trace-002"));
        given(queryService.get(eq("tenant-a"), eq("SIGN999"))).willThrow(new SignNotFoundException());

        mockMvc.perform(get("/api/v1/claim-intakes/SIGN999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CLAIM_INTAKE_NOT_FOUND"))
                .andExpect(jsonPath("$.traceId").value("trace-002"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
