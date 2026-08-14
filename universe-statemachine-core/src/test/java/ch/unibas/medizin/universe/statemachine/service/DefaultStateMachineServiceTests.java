/*
 * Copyright 2017-2020 the original author or authors.
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
package ch.unibas.medizin.universe.statemachine.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import ch.unibas.medizin.universe.statemachine.AbstractStateMachineTests;
import ch.unibas.medizin.universe.statemachine.StateMachine;
import ch.unibas.medizin.universe.statemachine.StateMachineSystemConstants;
import ch.unibas.medizin.universe.statemachine.TestUtils;
import ch.unibas.medizin.universe.statemachine.config.EnableStateMachineFactory;
import ch.unibas.medizin.universe.statemachine.config.EnumStateMachineConfigurerAdapter;
import ch.unibas.medizin.universe.statemachine.config.StateMachineFactory;
import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineStateConfigurer;
import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineTransitionConfigurer;

/**
 * Tests for {@link DefaultStateMachineService}.
 *
 * @author Janne Valkealahti
 *
 */
@SuppressWarnings("unchecked")
public class DefaultStateMachineServiceTests extends AbstractStateMachineTests {

	@Override
	protected AnnotationConfigApplicationContext buildContext() {
		return new AnnotationConfigApplicationContext();
	}

	@Test
	public void testAcquireNotStarted() {
		context.register(Config1.class);
		context.refresh();
		StateMachineFactory<TestStates, TestEvents> stateMachineFactory =
				context.getBean(StateMachineSystemConstants.DEFAULT_ID_STATEMACHINEFACTORY, StateMachineFactory.class);

		DefaultStateMachineService<TestStates, TestEvents> service = new DefaultStateMachineService<>(stateMachineFactory);
		StateMachine<TestStates,TestEvents> machine1 = service.acquireStateMachine("m1", false);
		assertThat(((Lifecycle)machine1).isRunning()).isFalse();
	}

	@Test
	public void testAcquireStarted() {
		context.register(Config1.class);
		context.refresh();
		StateMachineFactory<TestStates, TestEvents> stateMachineFactory =
				context.getBean(StateMachineSystemConstants.DEFAULT_ID_STATEMACHINEFACTORY, StateMachineFactory.class);

		DefaultStateMachineService<TestStates, TestEvents> service = new DefaultStateMachineService<>(stateMachineFactory);
		StateMachine<TestStates,TestEvents> machine1 = service.acquireStateMachine("m1", true);
		assertThat(((Lifecycle)machine1).isRunning()).isTrue();
	}

	@Test
	public void testReleaseStopMachine() {
		context.register(Config1.class);
		context.refresh();
		StateMachineFactory<TestStates, TestEvents> stateMachineFactory =
				context.getBean(StateMachineSystemConstants.DEFAULT_ID_STATEMACHINEFACTORY, StateMachineFactory.class);

		DefaultStateMachineService<TestStates, TestEvents> service = new DefaultStateMachineService<>(stateMachineFactory);
		StateMachine<TestStates,TestEvents> machine1 = service.acquireStateMachine("m1", true);
		assertThat(((Lifecycle)machine1).isRunning()).isTrue();
		service.releaseStateMachine("m1");
		assertThat(((Lifecycle)machine1).isRunning()).isFalse();
	}

	@Test
	public void testReleaseDoesNotStopMachine() {
		context.register(Config1.class);
		context.refresh();
		StateMachineFactory<TestStates, TestEvents> stateMachineFactory =
				context.getBean(StateMachineSystemConstants.DEFAULT_ID_STATEMACHINEFACTORY, StateMachineFactory.class);

		DefaultStateMachineService<TestStates, TestEvents> service = new DefaultStateMachineService<>(stateMachineFactory);
		StateMachine<TestStates,TestEvents> machine1 = service.acquireStateMachine("m1", true);
		assertThat(((Lifecycle)machine1).isRunning()).isTrue();
		service.releaseStateMachine("m1", false);
		assertThat(((Lifecycle)machine1).isRunning()).isTrue();
	}

	@Test
	public void testServiceStop() throws Exception {
		context.register(Config1.class);
		context.refresh();
		StateMachineFactory<TestStates, TestEvents> stateMachineFactory =
				context.getBean(StateMachineSystemConstants.DEFAULT_ID_STATEMACHINEFACTORY, StateMachineFactory.class);

		DefaultStateMachineService<TestStates, TestEvents> service = new DefaultStateMachineService<>(stateMachineFactory);
		StateMachine<TestStates,TestEvents> machine1 = service.acquireStateMachine("m1", false);
		StateMachine<TestStates,TestEvents> machine2 = service.acquireStateMachine("m2", false);
		assertThat(((Lifecycle)machine1).isRunning()).isFalse();
		assertThat(((Lifecycle)machine2).isRunning()).isFalse();
		Map<?, ?> machines = TestUtils.readField("machines", service);
		assertThat(machines).hasSize(2);
		service.destroy();
		assertThat(machines).isEmpty();
	}

	@Configuration
	@EnableStateMachineFactory
	static class Config1 extends EnumStateMachineConfigurerAdapter<TestStates, TestEvents> {

		@Override
		public void configure(StateMachineStateConfigurer<TestStates, TestEvents> states) throws Exception {
			states
				.withStates()
					.initial(TestStates.S1)
					.state(TestStates.S1)
					.state(TestStates.S2);
		}

		@Override
		public void configure(StateMachineTransitionConfigurer<TestStates, TestEvents> transitions) throws Exception {
			transitions
				.withExternal()
					.source(TestStates.S1)
					.target(TestStates.S2)
					.event(TestEvents.E1);
		}
	}
}
