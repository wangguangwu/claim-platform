package com.claim.platform.application.domain.model;

import com.claim.platform.application.domain.exception.InvalidApplicationStatusException;

/**
 * 签收申请聚合根，维护签收编号、外部申请编号及材料登记引起的状态转换。
 *
 * @author wangguangwu
 */
public final class SignApplication {

    private final String tenantCode;
    private final String signNo;
    private final String externalApplyNo;
    private final String applicantName;
    private final String applicantInfo;
    private final String accidentPersonInfo;
    private final String mainInsuredInfo;
    private final String payeeInfo;
    private final String extJson;
    private SignApplicationStatus status;

    private SignApplication(String tenantCode, String signNo, String externalApplyNo, String applicantName,
                            String applicantInfo, String accidentPersonInfo, String mainInsuredInfo,
                            String payeeInfo, String extJson, SignApplicationStatus status) {
        this.tenantCode = tenantCode;
        this.signNo = signNo;
        this.externalApplyNo = externalApplyNo;
        this.applicantName = applicantName;
        this.applicantInfo = applicantInfo;
        this.accidentPersonInfo = accidentPersonInfo;
        this.mainInsuredInfo = mainInsuredInfo;
        this.payeeInfo = payeeInfo;
        this.extJson = extJson;
        this.status = status;
    }

    /**
     * 建立新的签收申请，所有新申请均从待上传材料状态开始。
     *
     * @param tenantCode 当前受信任租户
     * @param signNo 平台签收编号
     * @param externalApplyNo 外部申请编号
     * @return 新申请聚合
     */
    public static SignApplication create(String tenantCode, String signNo, String externalApplyNo, String applicantName,
                                         String applicantInfo, String accidentPersonInfo, String mainInsuredInfo,
                                         String payeeInfo, String extJson) {
        return new SignApplication(tenantCode, signNo, externalApplyNo, applicantName, applicantInfo,
                accidentPersonInfo, mainInsuredInfo, payeeInfo, extJson, SignApplicationStatus.IMAGE_NOT_UPLOAD);
    }

    /**
     * 从持久化事实恢复聚合；状态转换仍由聚合方法校验。
     *
     * @return 已恢复的聚合
     */
    public static SignApplication restore(String tenantCode, String signNo, String externalApplyNo, String applicantName,
                                          String applicantInfo, String accidentPersonInfo, String mainInsuredInfo,
                                          String payeeInfo, String extJson, SignApplicationStatus status) {
        return new SignApplication(tenantCode, signNo, externalApplyNo, applicantName, applicantInfo,
                accidentPersonInfo, mainInsuredInfo, payeeInfo, extJson, status);
    }

    /**
     * 从不含基础设施属性的领域状态恢复聚合，供持久化适配器使用。
     *
     * @param state 已映射的领域状态
     * @return 已恢复的聚合
     */
    public static SignApplication restore(SignApplicationState state) {
        return restore(state.tenantCode(), state.signNo(), state.externalApplyNo(), state.applicantName(),
                state.applicantInfo(), state.accidentPersonInfo(), state.mainInsuredInfo(), state.payeeInfo(),
                state.extJson(), state.status());
    }

    /**
     * 登记材料并执行唯一允许由材料上传触发的状态转换。
     *
     * @throws InvalidApplicationStatusException 当前状态不能登记材料时抛出
     */
    public void registerMaterials() {
        if (status != SignApplicationStatus.IMAGE_NOT_UPLOAD && status != SignApplicationStatus.ISSUE_CASE) {
            throw new InvalidApplicationStatusException(status);
        }
        status = SignApplicationStatus.IMAGE_UPLOADED;
    }

    public String tenantCode() { return tenantCode; }
    public String signNo() { return signNo; }
    public String externalApplyNo() { return externalApplyNo; }
    public String applicantName() { return applicantName; }
    public String applicantInfo() { return applicantInfo; }
    public String accidentPersonInfo() { return accidentPersonInfo; }
    public String mainInsuredInfo() { return mainInsuredInfo; }
    public String payeeInfo() { return payeeInfo; }
    public String extJson() { return extJson; }
    public SignApplicationStatus status() { return status; }

    /**
     * 导出不变量校验后的领域状态，供基础设施层转换为持久化对象。
     *
     * @return 当前聚合的持久化领域状态
     */
    public SignApplicationState snapshot() {
        return new SignApplicationState(tenantCode, signNo, externalApplyNo, applicantName, applicantInfo,
                accidentPersonInfo, mainInsuredInfo, payeeInfo, extJson, status);
    }
}
