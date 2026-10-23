package com.claim.platform.sign.interfaces.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.claim.platform.sign.application.command.SignCommand;
import com.claim.platform.sign.application.exception.SignSubmissionInProgressException;
import com.claim.platform.sign.application.result.SignResult;
import com.claim.platform.sign.application.handler.SignCommandHandler;
import com.claim.platform.sign.domain.model.SignStatus;
import com.claim.platform.sign.interfaces.tenant.core.context.TenantContext;
import com.claim.platform.sign.interfaces.tenant.core.context.TenantContextHolder;
import com.claim.platform.sign.interfaces.tenant.core.exception.SignCodeMappingException;
import com.claim.platform.sign.interfaces.tenant.core.exception.SignPayloadParseException;
import com.claim.platform.sign.interfaces.tenant.core.exception.SignRequestValidationException;
import com.claim.platform.sign.interfaces.tenant.core.exception.TenantProtocolNotSupportedException;
import com.claim.platform.sign.interfaces.tenant.core.pipeline.SignConvertPipeline;
import com.claim.platform.sign.interfaces.web.SignWebContract;
import com.claim.platform.sign.interfaces.web.context.GatewayRequestContextResolver;
import com.claim.platform.sign.interfaces.web.context.TenantSignContextResolver;
import com.claim.platform.sign.interfaces.web.exception.TenantContextMissingException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
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

@WebMvcTest(TenantSignController.class)
class TenantSignControllerTest {

    private static final TenantContext CONTEXT = new TenantContext("reference", "v1", "trace-001");
    private static final SignCommand COMMAND = new SignCommand("reference", "trace-001",
            "EXT-001", "张三", null, null, null, null, List.of(), "{}");
    private static final SignResult RESULT = new SignResult("SIGN001",
            SignStatus.IMAGE_NOT_UPLOAD);

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private TenantSignContextResolver contextResolver;
    @MockBean
    private SignConvertPipeline convertPipeline;
    @MockBean
    private SignCommandHandler commandService;

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldReturnStableEnvelopeForConvertedPayload() throws Exception {
        stubSuccessPath();

        mockMvc.perform(signRequest())
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

        mockMvc.perform(signRequest()).andExpect(status().isOk());

        assertThat(captured.get()).contains(CONTEXT);
        assertThat(TenantContextHolder.get()).isEmpty();
    }

    @Test
    void shouldClearTenantContextWhenPipelineFails() throws Exception {
        stubContext();
        given(convertPipeline.execute(anyString())).willAnswer(invocation -> {
            assertThat(TenantContextHolder.get()).contains(CONTEXT);
            throw new SignPayloadParseException("payload");
        });

        mockMvc.perform(signRequest()).andExpect(status().isBadRequest());

        assertThat(TenantContextHolder.get()).isEmpty();
        verifyNoInteractions(commandService);
    }

    @Test
    void shouldMapParseFailure() throws Exception {
        stubContext();
        given(convertPipeline.execute(anyString())).willThrow(new SignPayloadParseException("applicant"));

        mockMvc.perform(signRequest())
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
                .willThrow(new SignRequestValidationException("applicant.name"));

        mockMvc.perform(signRequest())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"));
        verifyNoInteractions(commandService);
    }

    @Test
    void shouldMapCodeMappingFailure() throws Exception {
        stubContext();
        given(convertPipeline.execute(anyString()))
                .willThrow(new SignCodeMappingException("applicant.certificateType"));

        mockMvc.perform(signRequest())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"));
        verifyNoInteractions(commandService);
    }

    @Test
    void shouldMapUnsupportedProtocol() throws Exception {
        stubContext();
        given(convertPipeline.execute(anyString()))
                .willThrow(new TenantProtocolNotSupportedException("reference", "v2"));

        mockMvc.perform(signRequest())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TENANT_PROTOCOL_NOT_SUPPORTED"))
                .andExpect(jsonPath("$.message").value("租户协议不支持"));
        verifyNoInteractions(commandService);
    }

    @Test
    void shouldMapMissingTenantContext() throws Exception {
        given(contextResolver.resolve(any())).willThrow(new TenantContextMissingException());

        mockMvc.perform(signRequest())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TENANT_CONTEXT_MISSING"));
        verifyNoInteractions(convertPipeline, commandService);
    }

    @Test
    void shouldMapSubmissionInProgressConflict() throws Exception {
        stubContext();
        given(convertPipeline.execute(anyString())).willReturn(COMMAND);
        given(commandService.submit(any())).willThrow(new SignSubmissionInProgressException());

        mockMvc.perform(signRequest())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INTAKE_SUBMISSION_IN_PROGRESS"))
                .andExpect(jsonPath("$.message").value("申请正在处理中，请稍后重试"))
                .andExpect(jsonPath("$.traceId").value("trace-001"));
    }

    @Test
    void shouldMapInvalidProtocolVersionHeader() throws Exception {
        given(contextResolver.resolve(any()))
                .willThrow(new IllegalArgumentException("协议版本头格式非法"));

        mockMvc.perform(signRequest())
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

    private MockHttpServletRequestBuilder signRequest() {
        return post("/api/v1/tenant-intakes")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Claim-Tenant", "reference")
                .header(SignWebContract.Headers.PROTOCOL_VERSION, "v1")
                .content("{}");
    }
}
