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

import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class RoleVoter implements AccessDecisionVoter<Object> {

	private String rolePrefix = "ROLE_";

	@Override
	public boolean supports(ConfigAttribute attribute) {
		String value = attribute.getAttribute();
		return value != null && value.startsWith(this.rolePrefix);
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return true;
	}

	@Override
	public int vote(Authentication authentication, Object object, Collection<ConfigAttribute> attributes) {
		int result = ACCESS_ABSTAIN;
		if (authentication == null) {
			return result;
		}
		for (ConfigAttribute attribute : attributes) {
			if (!supports(attribute)) {
				continue;
			}
			result = ACCESS_DENIED;
			String needed = attribute.getAttribute();
			for (GrantedAuthority authority : authentication.getAuthorities()) {
				if (needed.equals(authority.getAuthority())) {
					return ACCESS_GRANTED;
				}
			}
		}
		return result;
	}

	public void setRolePrefix(String rolePrefix) {
		this.rolePrefix = rolePrefix != null ? rolePrefix : "";
	}
}
