package com.claim.platform.intake.domain.model;

import com.claim.platform.intake.domain.exception.ConcurrentIntakeModificationException;
import com.claim.platform.intake.domain.exception.InvalidIntakeStatusException;
import com.claim.platform.intake.domain.model.person.AccidentPerson;
import com.claim.platform.intake.domain.model.person.Applicant;
import com.claim.platform.intake.domain.model.person.MainInsured;
import com.claim.platform.intake.domain.model.person.Payee;

/**
 * 理赔受理聚合根，维护签收编号、外部申请编号及材料登记引起的状态转换。
 * 四类人员以强类型 {@code Person} 子类型承载，状态恢复与快照导出均保持类型安全。
 * <p>聚合不变量：
 * <ul>
 *   <li>标识：{@code tenantCode + signNo}；</li>
 *   <li>幂等键：{@code tenantCode + externalApplyNo}，重复提交回放既有受理事实，不得新建；</li>
 *   <li>并发策略：{@code version} 乐观锁，冲突时抛出 {@link ConcurrentIntakeModificationException}；</li>
 *   <li>合法状态转换：仅 {@link #registerMaterials()} 允许材料上传触发迁移。</li>
 * </ul>
 *
 * @author wangguangwu
 */
public final class ClaimIntake {

    private final String tenantCode;
    private final String signNo;
    private final String externalApplyNo;
    private final String applicantName;
    private final Applicant applicant;
    private final AccidentPerson accidentPerson;
    private final MainInsured mainInsured;
    private final Payee payee;
    private final String extJson;
    private IntakeStatus status;

    private ClaimIntake(String tenantCode, String signNo, String externalApplyNo, String applicantName,
                        Applicant applicant, AccidentPerson accidentPerson, MainInsured mainInsured, Payee payee,
                        String extJson, IntakeStatus status) {
        this.tenantCode = tenantCode;
        this.signNo = signNo;
        this.externalApplyNo = externalApplyNo;
        this.applicantName = applicantName;
        this.applicant = applicant;
        this.accidentPerson = accidentPerson;
        this.mainInsured = mainInsured;
        this.payee = payee;
        this.extJson = extJson;
        this.status = status;
    }

    /**
     * 建立新的签收申请，所有新申请均从待上传材料状态开始。
     *
     * @param tenantCode      当前受信任租户
     * @param signNo          平台签收编号
     * @param externalApplyNo 外部申请编号
     * @param applicantName   申请人姓名（冗余存储用于检索）
     * @param applicant       申请人领域对象，可为 null
     * @param accidentPerson  出险人领域对象，可为 null
     * @param mainInsured     主被保险人领域对象，可为 null
     * @param payee           领款人领域对象，可为 null
     * @param extJson         扩展信息 JSON 字符串
     * @return 新申请聚合
     */
    public static ClaimIntake create(String tenantCode, String signNo, String externalApplyNo, String applicantName,
                                     Applicant applicant, AccidentPerson accidentPerson, MainInsured mainInsured,
                                     Payee payee, String extJson) {
        return new ClaimIntake(tenantCode, signNo, externalApplyNo, applicantName, applicant, accidentPerson,
                mainInsured, payee, extJson, IntakeStatus.IMAGE_NOT_UPLOAD);
    }

    /**
     * 从持久化事实恢复聚合；状态转换仍由聚合方法校验。
     *
     * @return 已恢复的聚合
     */
    public static ClaimIntake restore(String tenantCode, String signNo, String externalApplyNo, String applicantName,
                                      Applicant applicant, AccidentPerson accidentPerson, MainInsured mainInsured,
                                      Payee payee, String extJson, IntakeStatus status) {
        return new ClaimIntake(tenantCode, signNo, externalApplyNo, applicantName, applicant, accidentPerson,
                mainInsured, payee, extJson, status);
    }

    /**
     * 从不含基础设施属性的领域状态恢复聚合，供持久化适配器使用。
     *
     * @param state 已映射的领域状态
     * @return 已恢复的聚合
     */
    public static ClaimIntake restore(ClaimIntakeState state) {
        return restore(state.tenantCode(), state.signNo(), state.externalApplyNo(), state.applicantName(),
                state.applicant(), state.accidentPerson(), state.mainInsured(), state.payee(), state.extJson(),
                state.status());
    }

    /**
     * 登记材料并执行唯一允许由材料上传触发的状态转换。
     *
     * @throws InvalidIntakeStatusException 当前状态不能登记材料时抛出
     */
    public void registerMaterials() {
        if (status != IntakeStatus.IMAGE_NOT_UPLOAD && status != IntakeStatus.ISSUE_CASE) {
            throw new InvalidIntakeStatusException(status);
        }
        status = IntakeStatus.IMAGE_UPLOADED;
    }

    public String tenantCode() {
        return tenantCode;
    }

    public String signNo() {
        return signNo;
    }

    public String externalApplyNo() {
        return externalApplyNo;
    }

    public String applicantName() {
        return applicantName;
    }

    public Applicant applicant() {
        return applicant;
    }

    public AccidentPerson accidentPerson() {
        return accidentPerson;
    }

    public MainInsured mainInsured() {
        return mainInsured;
    }

    public Payee payee() {
        return payee;
    }

    public String extJson() {
        return extJson;
    }

    public IntakeStatus status() {
        return status;
    }

    /**
     * 导出不变量校验后的领域状态，供基础设施层转换为持久化对象。
     *
     * @return 当前聚合的持久化领域状态
     */
    public ClaimIntakeState snapshot() {
        return new ClaimIntakeState(tenantCode, signNo, externalApplyNo, applicantName, applicant,
                accidentPerson, mainInsured, payee, extJson, status);
    }
}
