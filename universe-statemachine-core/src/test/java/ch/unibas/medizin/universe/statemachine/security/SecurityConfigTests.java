/*
 * Copyright 2015-2020 the original author or authors.
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
package ch.unibas.medizin.universe.statemachine.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ch.unibas.medizin.universe.statemachine.security.StateMachineSecurityInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import ch.unibas.medizin.universe.statemachine.AbstractStateMachineTests;
import ch.unibas.medizin.universe.statemachine.ObjectStateMachine;
import ch.unibas.medizin.universe.statemachine.StateMachineSystemConstants;
import ch.unibas.medizin.universe.statemachine.TestUtils;
import ch.unibas.medizin.universe.statemachine.config.EnableStateMachine;
import ch.unibas.medizin.universe.statemachine.config.StateMachineConfigurerAdapter;
import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineConfigurationConfigurer;
import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineStateConfigurer;
import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineTransitionConfigurer;
import ch.unibas.medizin.universe.statemachine.security.SecurityRule.ComparisonType;
import ch.unibas.medizin.universe.statemachine.support.StateMachineInterceptor;
import ch.unibas.medizin.universe.statemachine.support.StateMachineInterceptorList;
import ch.unibas.medizin.universe.statemachine.transition.Transition;

/**
 * Generic security config tests.
 *
 * @author Janne Valkealahti
 *
 */
public class SecurityConfigTests extends AbstractStateMachineTests {

	@Override
	protected AnnotationConfigApplicationContext buildContext() {
		return new AnnotationConfigApplicationContext();
	}

	@Test
	public void testSecurityEnabledWithTrue() throws Exception {
		context.register(Config1.class);
		context.refresh();
		assertThat(context.containsBean(StateMachineSystemConstants.DEFAULT_ID_STATEMACHINE)).isTrue();
		@SuppressWarnings("unchecked")
		ObjectStateMachine<String, String> machine =
				context.getBean(StateMachineSystemConstants.DEFAULT_ID_STATEMACHINE, ObjectStateMachine.class);
		assertThat(machine).isNotNull();

		StateMachineInterceptorList<?, ?> ilist = TestUtils.readField("interceptors", machine);
		List<StateMachineInterceptor<?, ?>> interceptors = TestUtils.readField("interceptors", ilist);
		assertThat(interceptors).isNotNull();
		assertThat(interceptors).hasSize(1);
		assertThat(interceptors.getFirst()).isInstanceOf(StateMachineSecurityInterceptor.class);
		Object adm = TestUtils.readField("transitionAuthorizationManager", interceptors.getFirst());
		assertThat(adm).isNull();
	}

	@Test
	public void testSecurityDisabledWithFalse() throws Exception {
		context.register(Config2.class);
		context.refresh();
		assertThat(context.containsBean(StateMachineSystemConstants.DEFAULT_ID_STATEMACHINE)).isTrue();
		@SuppressWarnings("unchecked")
		ObjectStateMachine<String, String> machine =
				context.getBean(StateMachineSystemConstants.DEFAULT_ID_STATEMACHINE, ObjectStateMachine.class);
		assertThat(machine).isNotNull();

		StateMachineInterceptorList<?, ?> ilist = TestUtils.readField("interceptors", machine);
		List<StateMachineInterceptor<?, ?>> interceptors = TestUtils.readField("interceptors", ilist);
		assertThat(interceptors).isNotNull();
		assertThat(interceptors).isEmpty();
	}

	@Test
	public void testSecurityEnabledWithJustWith() throws Exception {
		context.register(Config3.class);
		context.refresh();
		assertThat(context.containsBean(StateMachineSystemConstants.DEFAULT_ID_STATEMACHINE)).isTrue();
		@SuppressWarnings("unchecked")
		ObjectStateMachine<String, String> machine =
				context.getBean(StateMachineSystemConstants.DEFAULT_ID_STATEMACHINE, ObjectStateMachine.class);
		assertThat(machine).isNotNull();

		StateMachineInterceptorList<?, ?> ilist = TestUtils.readField("interceptors", machine);
		List<StateMachineInterceptor<?, ?>> interceptors = TestUtils.readField("interceptors", ilist);
		assertThat(interceptors).hasSize(1);
		assertThat(interceptors.getFirst()).isInstanceOf(StateMachineSecurityInterceptor.class);
		Object adm = TestUtils.readField("transitionAuthorizationManager", interceptors.getFirst());
		assertThat(adm).isNull();
	}

	@Test
	public void testSecurityDisabledNoSecurityConfigurer() throws Exception {
		context.register(Config4.class);
		context.refresh();
		assertThat(context.containsBean(StateMachineSystemConstants.DEFAULT_ID_STATEMACHINE)).isTrue();
		@SuppressWarnings("unchecked")
		ObjectStateMachine<String, String> machine =
				context.getBean(StateMachineSystemConstants.DEFAULT_ID_STATEMACHINE, ObjectStateMachine.class);
		assertThat(machine).isNotNull();

		StateMachineInterceptorList<?, ?> ilist = TestUtils.readField("interceptors", machine);
		List<StateMachineInterceptor<?, ?>> interceptors = TestUtils.readField("interceptors", ilist);
		assertThat(interceptors).isNotNull();
		assertThat(interceptors).isEmpty();
	}

	@Test
	public void testCustomAccessDecisionManager() throws Exception {
		context.register(Config5.class);
		context.refresh();
		assertThat(context.containsBean(StateMachineSystemConstants.DEFAULT_ID_STATEMACHINE)).isTrue();
		@SuppressWarnings("unchecked")
		ObjectStateMachine<String, String> machine =
				context.getBean(StateMachineSystemConstants.DEFAULT_ID_STATEMACHINE, ObjectStateMachine.class);
		assertThat(machine).isNotNull();

		StateMachineInterceptorList<?, ?> ilist = TestUtils.readField("interceptors", machine);
		List<StateMachineInterceptor<?, ?>> interceptors = TestUtils.readField("interceptors", ilist);
		assertThat(interceptors).isNotNull();
		assertThat(interceptors).hasSize(1);
		assertThat(interceptors.getFirst()).isInstanceOf(StateMachineSecurityInterceptor.class);
		Object adm = TestUtils.readField("transitionAuthorizationManager", interceptors.getFirst());
		assertThat(adm).isNotNull();
		assertThat(adm).isInstanceOf(MockAuthorizationManager.class);
	}

	@Test
	public void testTransitionExplicit() throws Exception {
		context.register(Config6.class);
		context.refresh();
		assertThat(context.containsBean(StateMachineSystemConstants.DEFAULT_ID_STATEMACHINE)).isTrue();
		@SuppressWarnings("unchecked")
		ObjectStateMachine<String, String> machine =
				context.getBean(StateMachineSystemConstants.DEFAULT_ID_STATEMACHINE, ObjectStateMachine.class);
		assertThat(machine).isNotNull();

		Transition<String, String> transition = machine.getTransitions().iterator().next();
		assertThat(transition.getSecurityRule()).isNotNull();
	}

	@Test
	public void testTransitionGlobal() throws Exception {
		context.register(Config8.class);
		context.refresh();
		assertThat(context.containsBean(StateMachineSystemConstants.DEFAULT_ID_STATEMACHINE)).isTrue();
		@SuppressWarnings("unchecked")
		ObjectStateMachine<String, String> machine =
				context.getBean(StateMachineSystemConstants.DEFAULT_ID_STATEMACHINE, ObjectStateMachine.class);
		assertThat(machine).isNotNull();

		Transition<String, String> transition = machine.getTransitions().iterator().next();
		assertThat(transition.getSecurityRule()).isNotNull();
	}

	@Test
	public void testEventRule() throws Exception {
		context.register(Config7.class);
		context.refresh();
		assertThat(context.containsBean(StateMachineSystemConstants.DEFAULT_ID_STATEMACHINE)).isTrue();
		@SuppressWarnings("unchecked")
		ObjectStateMachine<String, String> machine =
				context.getBean(StateMachineSystemConstants.DEFAULT_ID_STATEMACHINE, ObjectStateMachine.class);
		assertThat(machine).isNotNull();

		StateMachineInterceptorList<?, ?> ilist = TestUtils.readField("interceptors", machine);
		List<StateMachineInterceptor<?, ?>> interceptors = TestUtils.readField("interceptors", ilist);
		assertThat(interceptors).isNotNull();
		assertThat(interceptors).hasSize(1);
		assertThat(interceptors.getFirst()).isInstanceOf(StateMachineSecurityInterceptor.class);
		Object adm = TestUtils.readField("eventSecurityRule", interceptors.getFirst());
		assertThat(adm).isNotNull();
	}

	@Configuration
	@EnableStateMachine
	static class Config1 extends StateMachineConfigurerAdapter<String, String> {

		@Override
		public void configure(StateMachineConfigurationConfigurer<String, String> config)
				throws Exception {
			config
				.withSecurity()
					.enabled(true);
		}

		@Override
		public void configure(StateMachineStateConfigurer<String, String> states)
				throws Exception {
			states
				.withStates()
					.initial("S0")
					.state("S1");
		}

		@Override
		public void configure(StateMachineTransitionConfigurer<String, String> transitions)
				throws Exception {
			transitions
				.withExternal()
					.source("S0")
					.target("S1")
					.event("A");
		}

	}

	@Configuration
	@EnableStateMachine
	static class Config2 extends StateMachineConfigurerAdapter<String, String> {

		@Override
		public void configure(StateMachineConfigurationConfigurer<String, String> config)
				throws Exception {
			config
				.withSecurity()
					.enabled(false);
		}

		@Override
		public void configure(StateMachineStateConfigurer<String, String> states)
				throws Exception {
			states
				.withStates()
					.initial("S0")
					.state("S1");
		}

		@Override
		public void configure(StateMachineTransitionConfigurer<String, String> transitions)
				throws Exception {
			transitions
				.withExternal()
					.source("S0")
					.target("S1")
					.event("A");
		}

	}

	@Configuration
	@EnableStateMachine
	static class Config3 extends StateMachineConfigurerAdapter<String, String> {

		@Override
		public void configure(StateMachineConfigurationConfigurer<String, String> config)
				throws Exception {
			config
				.withSecurity();
		}

		@Override
		public void configure(StateMachineStateConfigurer<String, String> states)
				throws Exception {
			states
				.withStates()
					.initial("S0")
					.state("S1");
		}

		@Override
		public void configure(StateMachineTransitionConfigurer<String, String> transitions)
				throws Exception {
			transitions
				.withExternal()
					.source("S0")
					.target("S1")
					.event("A");
		}

	}

	@Configuration
	@EnableStateMachine
	static class Config4 extends StateMachineConfigurerAdapter<String, String> {

		@Override
		public void configure(StateMachineStateConfigurer<String, String> states)
				throws Exception {
			states
				.withStates()
					.initial("S0")
					.state("S1");
		}

		@Override
		public void configure(StateMachineTransitionConfigurer<String, String> transitions)
				throws Exception {
			transitions
				.withExternal()
					.source("S0")
					.target("S1")
					.event("A");
		}

	}

	@Configuration
	@EnableStateMachine
	static class Config5 extends StateMachineConfigurerAdapter<String, String> {

		@Override
		public void configure(StateMachineConfigurationConfigurer<String, String> config)
				throws Exception {
			config
				.withSecurity()
					.eventAuthorizationManager(new MockAuthorizationManager())
					.transitionAuthorizationManager(new MockAuthorizationManager());
		}

		@Override
		public void configure(StateMachineStateConfigurer<String, String> states)
				throws Exception {
			states
				.withStates()
					.initial("S0")
					.state("S1");
		}

		@Override
		public void configure(StateMachineTransitionConfigurer<String, String> transitions)
				throws Exception {
			transitions
				.withExternal()
					.source("S0")
					.target("S1")
					.event("A");
		}

	}

	@Configuration
	@EnableStateMachine
	static class Config6 extends StateMachineConfigurerAdapter<String, String> {

		@Override
		public void configure(StateMachineConfigurationConfigurer<String, String> config)
				throws Exception {
			config
				.withSecurity()
					.enabled(true);
		}

		@Override
		public void configure(StateMachineStateConfigurer<String, String> states)
				throws Exception {
			states
				.withStates()
					.initial("S0")
					.state("S1");
		}

		@Override
		public void configure(StateMachineTransitionConfigurer<String, String> transitions)
				throws Exception {
			transitions
				.withExternal()
					.source("S0")
					.target("S1")
					.event("A")
					.secured("expression")
					.secured("FOO", ComparisonType.ALL);
		}

	}

	@Configuration
	@EnableStateMachine
	static class Config7 extends StateMachineConfigurerAdapter<String, String> {

		@Override
		public void configure(StateMachineConfigurationConfigurer<String, String> config)
				throws Exception {
			config
				.withSecurity()
					.enabled(true)
					.event("expression")
					.event("FOO", ComparisonType.ALL);
		}

		@Override
		public void configure(StateMachineStateConfigurer<String, String> states)
				throws Exception {
			states
				.withStates()
					.initial("S0")
					.state("S1");
		}

		@Override
		public void configure(StateMachineTransitionConfigurer<String, String> transitions)
				throws Exception {
			transitions
				.withExternal()
					.source("S0")
					.target("S1")
					.event("A");
		}

	}

	@Configuration
	@EnableStateMachine
	static class Config8 extends StateMachineConfigurerAdapter<String, String> {

		@Override
		public void configure(StateMachineConfigurationConfigurer<String, String> config)
				throws Exception {
			config
				.withSecurity()
					.enabled(true)
					.transition("expression")
					.transition("FOO", ComparisonType.ALL);
		}

		@Override
		public void configure(StateMachineStateConfigurer<String, String> states)
				throws Exception {
			states
				.withStates()
					.initial("S0")
					.state("S1");
		}

		@Override
		public void configure(StateMachineTransitionConfigurer<String, String> transitions)
				throws Exception {
			transitions
				.withExternal()
					.source("S0")
					.target("S1")
					.event("A");
		}

	}

	private static class MockAuthorizationManager implements AuthorizationManager<Object> {

		@Override
		public AuthorizationResult authorize(java.util.function.Supplier<? extends org.springframework.security.core.Authentication> authentication, Object object) {
			return new AuthorizationDecision(true);
		}
	}

}
