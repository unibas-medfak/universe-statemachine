/*
 * Copyright 2015-2016 the original author or authors.
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
package ch.unibas.medizin.universe.statemachine.config.configurers;

import org.springframework.security.authorization.AuthorizationManager;
import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineConfigurationBuilder;
import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineConfigurationConfigurer;
import ch.unibas.medizin.universe.statemachine.config.common.annotation.AnnotationConfigurerAdapter;
import ch.unibas.medizin.universe.statemachine.config.model.ConfigurationData;
import ch.unibas.medizin.universe.statemachine.security.SecurityRule;
import ch.unibas.medizin.universe.statemachine.security.SecurityRule.ComparisonType;

/**
 * Default implementation of a {@link SecurityConfigurer}.
 *
 * @param <S> the type of state
 * @param <E> the type of event
 */
public class DefaultSecurityConfigurer<S, E>
		extends AnnotationConfigurerAdapter<ConfigurationData<S, E>, StateMachineConfigurationConfigurer<S, E>, StateMachineConfigurationBuilder<S, E>>
		implements SecurityConfigurer<S, E> {

	private boolean enabled = true;
	private AuthorizationManager<Object> transitionAuthorizationManager;
	private AuthorizationManager<Object> eventAuthorizationManager;
	private SecurityRule eventSecurityRule;
	private SecurityRule transitionSecurityRule;

	@Override
	public void configure(StateMachineConfigurationBuilder<S, E> builder) throws Exception {
		if (enabled) {
			builder.setSecurityEnabled(true);
			builder.setTransitionAuthorizationManager(transitionAuthorizationManager);
			builder.setEventAuthorizationManager(eventAuthorizationManager);
			builder.setEventSecurityRule(eventSecurityRule);
			builder.setTransitionSecurityRule(transitionSecurityRule);
		}
	}

	@Override
	public SecurityConfigurer<S, E> enabled(boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	@Override
	public SecurityConfigurer<S, E> transitionAuthorizationManager(AuthorizationManager<Object> authorizationManager) {
		this.transitionAuthorizationManager = authorizationManager;
		return this;
	}

	@Override
	public SecurityConfigurer<S, E> eventAuthorizationManager(AuthorizationManager<Object> authorizationManager) {
		this.eventAuthorizationManager = authorizationManager;
		return this;
	}

	@Override
	public SecurityConfigurer<S, E> event(String attributes, ComparisonType match) {
		if (eventSecurityRule == null) {
			eventSecurityRule = new SecurityRule();
		}
		eventSecurityRule.setAttributes(SecurityRule.commaDelimitedListToSecurityAttributes(attributes));
		return this;
	}

	@Override
	public SecurityConfigurer<S, E> event(String expression) {
		if (eventSecurityRule == null) {
			eventSecurityRule = new SecurityRule();
		}
		eventSecurityRule.setExpression(expression);
		return this;
	}

	@Override
	public SecurityConfigurer<S, E> transition(String attributes, ComparisonType match) {
		if (transitionSecurityRule == null) {
			transitionSecurityRule = new SecurityRule();
		}
		transitionSecurityRule.setAttributes(SecurityRule.commaDelimitedListToSecurityAttributes(attributes));
		return this;
	}

	@Override
	public SecurityConfigurer<S, E> transition(String expression) {
		if (transitionSecurityRule == null) {
			transitionSecurityRule = new SecurityRule();
		}
		transitionSecurityRule.setExpression(expression);
		return this;
	}
}
