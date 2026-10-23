package com.claim.platform.sign.infrastructure.persistence.dataobject.person;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 领款人持久化对象，对应 {@code t_claim_intake.payee_info} JSON 列。
 *
 * @author wangguangwu
 */
@Data
@NoArgsConstructor
public class PayeeInfoDO extends PersonInfoDO {

    private String bankName;
    private String bankAccount;
    private String payeeRelation;
}
