package com.claim.platform.sign.application.port;

import com.claim.platform.sign.domain.sign.model.SignNo;

/** Allocates globally unique platform Sign numbers outside the creation transaction.
 *
 * @author wangguangwu
 */
public interface SignNumberGenerator { SignNo next(); }
