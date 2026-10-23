package com.claim.platform.intake.interfaces.web.converter;

import com.claim.platform.intake.domain.model.person.AccidentPerson;
import com.claim.platform.intake.domain.model.person.Applicant;
import com.claim.platform.intake.domain.model.person.MainInsured;
import com.claim.platform.intake.domain.model.person.Payee;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * 将 HTTP 请求中的人员 JSON 转换为领域人员模型，避免接口层依赖持久化对象。
 *
 * @author wangguangwu
 */
@Component
public class PersonRequestConverter {

    private final ObjectMapper objectMapper;

    public PersonRequestConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 转换申请人 JSON。
     *
     * @param node 申请人 JSON 节点
     * @return 领域申请人；节点为空时返回 null
     */
    public Applicant toApplicant(JsonNode node) {
        return convert(node, Applicant.class);
    }

    /**
     * 转换出险人 JSON。
     *
     * @param node 出险人 JSON 节点
     * @return 领域出险人；节点为空时返回 null
     */
    public AccidentPerson toAccidentPerson(JsonNode node) {
        return convert(node, AccidentPerson.class);
    }

    /**
     * 转换主被保险人 JSON。
     *
     * @param node 主被保险人 JSON 节点
     * @return 领域主被保险人；节点为空时返回 null
     */
    public MainInsured toMainInsured(JsonNode node) {
        return convert(node, MainInsured.class);
    }

    /**
     * 转换领款人 JSON。
     *
     * @param node 领款人 JSON 节点
     * @return 领域领款人；节点为空时返回 null
     */
    public Payee toPayee(JsonNode node) {
        return convert(node, Payee.class);
    }

    private <T> T convert(JsonNode node, Class<T> targetType) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return objectMapper.treeToValue(node, targetType);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("人员 JSON 内容不合法", exception);
        }
    }
}
