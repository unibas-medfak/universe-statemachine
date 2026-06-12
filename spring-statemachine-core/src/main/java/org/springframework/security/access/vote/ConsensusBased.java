/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.security.access.vote;

import java.util.Collection;
import java.util.List;

import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;

public class ConsensusBased extends AbstractAccessDecisionManager {

	public ConsensusBased(List<AccessDecisionVoter<?>> decisionVoters) {
		super(decisionVoters);
	}

	@Override
	public void decide(Authentication authentication, Object object, Collection<ConfigAttribute> configAttributes)
			throws AccessDeniedException {
		int grant = 0;
		int deny = 0;
		for (AccessDecisionVoter<Object> voter : getSupportedVoters(object.getClass(), configAttributes)) {
			int result = voter.vote(authentication, object, configAttributes);
			if (result == AccessDecisionVoter.ACCESS_GRANTED) {
				grant++;
			}
			else if (result == AccessDecisionVoter.ACCESS_DENIED) {
				deny++;
			}
		}
		if (grant > deny) {
			return;
		}
		if (grant == deny && grant == 0 && isAllowIfAllAbstainDecisions()) {
			return;
		}
		throw new AccessDeniedException("Access is denied");
	}
}
