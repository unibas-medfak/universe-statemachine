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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;

/**
 * Minimal base class for statemachine's legacy vote-based security support.
 */
public abstract class AbstractAccessDecisionManager implements AccessDecisionManager {

	private final List<AccessDecisionVoter<?>> decisionVoters;

	private boolean allowIfAllAbstainDecisions;

	protected AbstractAccessDecisionManager(List<AccessDecisionVoter<?>> decisionVoters) {
		this.decisionVoters = new ArrayList<>(decisionVoters);
	}

	@Override
	public boolean supports(ConfigAttribute attribute) {
		for (AccessDecisionVoter<?> voter : this.decisionVoters) {
			if (voter.supports(attribute)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		for (AccessDecisionVoter<?> voter : this.decisionVoters) {
			if (voter.supports(clazz)) {
				return true;
			}
		}
		return false;
	}

	protected boolean isAllowIfAllAbstainDecisions() {
		return this.allowIfAllAbstainDecisions;
	}

	public void setAllowIfAllAbstainDecisions(boolean allowIfAllAbstainDecisions) {
		this.allowIfAllAbstainDecisions = allowIfAllAbstainDecisions;
	}

	protected List<AccessDecisionVoter<Object>> getSupportedVoters(Class<?> clazz,
			Collection<ConfigAttribute> attributes) {
		List<AccessDecisionVoter<Object>> supported = new ArrayList<>();
		for (AccessDecisionVoter<?> voter : this.decisionVoters) {
			if (!voter.supports(clazz)) {
				continue;
			}
			boolean supportsAnyAttribute = attributes.isEmpty();
			for (ConfigAttribute attribute : attributes) {
				if (voter.supports(attribute)) {
					supportsAnyAttribute = true;
					break;
				}
			}
			if (supportsAnyAttribute) {
				@SuppressWarnings("unchecked")
				AccessDecisionVoter<Object> cast = (AccessDecisionVoter<Object>) voter;
				supported.add(cast);
			}
		}
		return supported;
	}
}
