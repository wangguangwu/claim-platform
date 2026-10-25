package com.claim.platform.sign.domain.interactionlog.repository;

import com.claim.platform.sign.domain.interactionlog.model.InteractionLog;

/** Appends sanitized interaction audits without changing Sign lifecycle facts.
 * @author wangguangwu
 */
public interface InteractionLogRepository { void append(InteractionLog interactionLog); }
