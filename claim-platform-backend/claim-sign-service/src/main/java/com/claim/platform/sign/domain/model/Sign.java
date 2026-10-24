package com.claim.platform.sign.domain.model;

import com.claim.platform.sign.domain.exception.ConcurrentSignModificationException;
import com.claim.platform.sign.domain.exception.InvalidSignStatusException;

/**
 * 理赔签收聚合根，维护签收编号、外部申请编号及材料登记引起的状态转换。
 * 人员明细不属于聚合状态，只以 {@code applicantName} 提供检索、以 {@code extJson} 原样透传。
 * <p>聚合不变量：
 * <ul>
 *   <li>标识：{@code tenantCode + signNo}；</li>
 *   <li>幂等键：{@code tenantCode + externalApplyNo}，重复提交回放既有签收事实，不得新建；</li>
 *   <li>并发策略：{@code version} 乐观锁，冲突时抛出 {@link ConcurrentSignModificationException}；</li>
 *   <li>合法状态转换：仅 {@link #registerMaterials()} 允许材料上传触发迁移。</li>
 * </ul>
 *
 * @author wangguangwu
 */
public final class Sign {

    private final String tenantCode;
    private final String signNo;
    private final String externalApplyNo;
    private final String applicantName;
    private final String extJson;
    private SignStatus status;

    private Sign(String tenantCode, String signNo, String externalApplyNo, String applicantName,
                        String extJson, SignStatus status) {
        this.tenantCode = tenantCode;
        this.signNo = signNo;
        this.externalApplyNo = externalApplyNo;
        this.applicantName = applicantName;
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
     * @param extJson         扩展信息 JSON 字符串
     * @return 新申请聚合
     */
    public static Sign create(String tenantCode, String signNo, String externalApplyNo, String applicantName,
                                     String extJson) {
        return new Sign(tenantCode, signNo, externalApplyNo, applicantName, extJson,
                SignStatus.IMAGE_NOT_UPLOAD);
    }

    /**
     * 从持久化事实恢复聚合；状态转换仍由聚合方法校验。
     *
     * @return 已恢复的聚合
     */
    public static Sign restore(String tenantCode, String signNo, String externalApplyNo, String applicantName,
                                      String extJson, SignStatus status) {
        return new Sign(tenantCode, signNo, externalApplyNo, applicantName, extJson, status);
    }

    /**
     * 从不含基础设施属性的领域状态恢复聚合，供持久化适配器使用。
     *
     * @param state 已映射的领域状态
     * @return 已恢复的聚合
     */
    public static Sign restore(SignState state) {
        return restore(state.tenantCode(), state.signNo(), state.externalApplyNo(), state.applicantName(),
                state.extJson(), state.status());
    }

    /**
     * 登记材料并执行唯一允许由材料上传触发的状态转换。
     *
     * @throws InvalidSignStatusException 当前状态不能登记材料时抛出
     */
    public void registerMaterials() {
        if (status != SignStatus.IMAGE_NOT_UPLOAD && status != SignStatus.ISSUE_CASE) {
            throw new InvalidSignStatusException(status);
        }
        status = SignStatus.IMAGE_UPLOADED;
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

    public String extJson() {
        return extJson;
    }

    public SignStatus status() {
        return status;
    }

    /**
     * 导出不变量校验后的领域状态，供基础设施层转换为持久化对象。
     *
     * @return 当前聚合的持久化领域状态
     */
    public SignState snapshot() {
        return new SignState(tenantCode, signNo, externalApplyNo, applicantName, extJson, status);
    }
}
