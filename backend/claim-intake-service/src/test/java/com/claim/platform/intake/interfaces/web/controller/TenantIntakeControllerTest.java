package com.claim.platform.intake.interfaces.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.claim.platform.intake.application.command.SubmitIntakeCommand;
import com.claim.platform.intake.application.result.ClaimIntakeResult;
import com.claim.platform.intake.application.service.ClaimIntakeCommandService;
import com.claim.platform.intake.domain.model.IntakeStatus;
import com.claim.platform.intake.interfaces.tenant.core.context.TenantContext;
import com.claim.platform.intake.interfaces.tenant.core.context.TenantContextHolder;
import com.claim.platform.intake.interfaces.tenant.core.exception.IntakeCodeMappingException;
import com.claim.platform.intake.interfaces.tenant.core.exception.IntakePayloadParseException;
import com.claim.platform.intake.interfaces.tenant.core.exception.IntakeRequestValidationException;
import com.claim.platform.intake.interfaces.tenant.core.exception.TenantProtocolNotSupportedException;
import com.claim.platform.intake.interfaces.tenant.core.pipeline.IntakeConvertPipeline;
import com.claim.platform.intake.interfaces.web.context.GatewayRequestContextResolver;
import com.claim.platform.intake.interfaces.web.context.TenantIntakeContextResolver;
import com.claim.platform.intake.interfaces.web.exception.TenantContextMissingException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(TenantIntakeController.class)
class TenantIntakeControllerTest {

    private static final TenantContext CONTEXT = new TenantContext("reference", "v1", "trace-001");
    private static final SubmitIntakeCommand COMMAND = new SubmitIntakeCommand("reference", "trace-001",
            "EXT-001", "张三", null, null, null, null, "{}");
    private static final ClaimIntakeResult RESULT = new ClaimIntakeResult("SIGN001",
            IntakeStatus.IMAGE_NOT_UPLOAD);

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private TenantIntakeContextResolver contextResolver;
    @MockBean
    private IntakeConvertPipeline convertPipeline;
    @MockBean
    private ClaimIntakeCommandService commandService;

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldReturnStableEnvelopeForConvertedPayload() throws Exception {
        stubSuccessPath();

        mockMvc.perform(intakeRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.traceId").value("trace-001"))
                .andExpect(jsonPath("$.data.signNo").value("SIGN001"));
    }

    @Test
    void shouldClearTenantContextAfterPipeline() throws Exception {
        stubSuccessPath();
        AtomicReference<Optional<TenantContext>> captured = new AtomicReference<>();
        given(convertPipeline.execute(anyString())).willAnswer(invocation -> {
            captured.set(TenantContextHolder.get());
            return COMMAND;
        });

        mockMvc.perform(intakeRequest()).andExpect(status().isOk());

        assertThat(captured.get()).contains(CONTEXT);
        assertThat(TenantContextHolder.get()).isEmpty();
    }

    @Test
    void shouldClearTenantContextWhenPipelineFails() throws Exception {
        stubContext();
        given(convertPipeline.execute(anyString())).willAnswer(invocation -> {
            assertThat(TenantContextHolder.get()).contains(CONTEXT);
            throw new IntakePayloadParseException("payload");
        });

        mockMvc.perform(intakeRequest()).andExpect(status().isBadRequest());

        assertThat(TenantContextHolder.get()).isEmpty();
        verifyNoInteractions(commandService);
    }

    @Test
    void shouldMapParseFailure() throws Exception {
        stubContext();
        given(convertPipeline.execute(anyString())).willThrow(new IntakePayloadParseException("applicant"));

        mockMvc.perform(intakeRequest())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.message").value("请求报文格式不合法"))
                .andExpect(jsonPath("$.traceId").value("trace-001"));
        verifyNoInteractions(commandService);
    }

    @Test
    void shouldMapValidationFailure() throws Exception {
        stubContext();
        given(convertPipeline.execute(anyString()))
                .willThrow(new IntakeRequestValidationException("applicant.name"));

        mockMvc.perform(intakeRequest())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"));
        verifyNoInteractions(commandService);
    }

    @Test
    void shouldMapCodeMappingFailure() throws Exception {
        stubContext();
        given(convertPipeline.execute(anyString()))
                .willThrow(new IntakeCodeMappingException("applicant.certificateType"));

        mockMvc.perform(intakeRequest())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"));
        verifyNoInteractions(commandService);
    }

    @Test
    void shouldMapUnsupportedProtocol() throws Exception {
        stubContext();
        given(convertPipeline.execute(anyString()))
                .willThrow(new TenantProtocolNotSupportedException("reference", "v2"));

        mockMvc.perform(intakeRequest())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TENANT_PROTOCOL_NOT_SUPPORTED"))
                .andExpect(jsonPath("$.message").value("租户协议不支持"));
        verifyNoInteractions(commandService);
    }

    @Test
    void shouldMapMissingTenantContext() throws Exception {
        given(contextResolver.resolve(any())).willThrow(new TenantContextMissingException());

        mockMvc.perform(intakeRequest())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TENANT_CONTEXT_MISSING"));
        verifyNoInteractions(convertPipeline, commandService);
    }

    @Test
    void shouldMapInvalidProtocolVersionHeader() throws Exception {
        given(contextResolver.resolve(any()))
                .willThrow(new IllegalArgumentException("协议版本头格式非法"));

        mockMvc.perform(intakeRequest())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"));
        verifyNoInteractions(convertPipeline, commandService);
    }

    private void stubSuccessPath() {
        stubContext();
        given(convertPipeline.execute(anyString())).willReturn(COMMAND);
        given(commandService.submit(any())).willReturn(RESULT);
    }

    private void stubContext() {
        given(contextResolver.resolve(any())).willAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            request.setAttribute(GatewayRequestContextResolver.TRACE_ATTRIBUTE, CONTEXT.traceId());
            return CONTEXT;
        });
    }

    private MockHttpServletRequestBuilder intakeRequest() {
        return post("/api/v1/tenant-intakes")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Claim-Tenant", "reference")
                .header(TenantIntakeContextResolver.PROTOCOL_VERSION_HEADER, "v1")
                .content("{}");
    }
}
