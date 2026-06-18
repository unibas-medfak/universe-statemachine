/*
 * Copyright 2015 the original author or authors.
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
import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineConfigurationConfigurer;
import ch.unibas.medizin.universe.statemachine.config.common.annotation.AnnotationConfigurerBuilder;
import ch.unibas.medizin.universe.statemachine.security.SecurityRule.ComparisonType;

/**
 * Base {@code ConfigConfigurer} interface for configuring generic config.
 *
 * @param <S> the type of state
 * @param <E> the type of event
 */
public interface SecurityConfigurer<S, E> extends
		AnnotationConfigurerBuilder<StateMachineConfigurationConfigurer<S, E>> {

	SecurityConfigurer<S, E> enabled(boolean enabled);

	SecurityConfigurer<S, E> transitionAuthorizationManager(AuthorizationManager<Object> authorizationManager);

	SecurityConfigurer<S, E> eventAuthorizationManager(AuthorizationManager<Object> authorizationManager);

	SecurityConfigurer<S, E> event(String attributes, ComparisonType match);

	SecurityConfigurer<S, E> event(String expression);

	SecurityConfigurer<S, E> transition(String attributes, ComparisonType match);

	SecurityConfigurer<S, E> transition(String expression);
}
