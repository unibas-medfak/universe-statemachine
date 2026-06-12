/*
 * Copyright 2016-2020 the original author or authors.
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
package ch.unibas.medizin.universe.statemachine.action;

import static org.assertj.core.api.Assertions.assertThat;
import static ch.unibas.medizin.universe.statemachine.TestUtils.doSendEventAndConsumeAll;
import static ch.unibas.medizin.universe.statemachine.TestUtils.doStartAndAssert;
import static ch.unibas.medizin.universe.statemachine.TestUtils.resolveMachine;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import ch.unibas.medizin.universe.statemachine.action.Action;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ch.unibas.medizin.universe.statemachine.AbstractStateMachineTests;
import ch.unibas.medizin.universe.statemachine.StateContext;
import ch.unibas.medizin.universe.statemachine.StateMachine;
import ch.unibas.medizin.universe.statemachine.config.EnableStateMachine;
import ch.unibas.medizin.universe.statemachine.config.EnumStateMachineConfigurerAdapter;
import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineStateConfigurer;
import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineTransitionConfigurer;
import ch.unibas.medizin.universe.statemachine.listener.StateMachineListenerAdapter;
import ch.unibas.medizin.universe.statemachine.state.State;

public class ActionAndTimerTests extends AbstractStateMachineTests {

	@Test
	public void testExitActionWithTimerOnce() throws Exception {
		context.register(Config1.class);
		context.refresh();
		StateMachine<TestStates, TestEvents> machine = resolveMachine(context);
		TestTimerAction testTimerAction = context.getBean(TestTimerAction.class);
		TestExitAction testExitAction = context.getBean(TestExitAction.class);
		TestListener testListener = new TestListener();
		machine.addStateListener(testListener);
		doStartAndAssert(machine);
		assertThat(machine.getState().getIds()).containsOnly(TestStates.S1);
		doSendEventAndConsumeAll(machine, TestEvents.E1);
		assertThat(machine.getState().getIds()).containsOnly(TestStates.S2);

		assertThat(testTimerAction.latch.await(4, TimeUnit.SECONDS)).isTrue();
		assertThat(testTimerAction.e).isNull();

		// need to sleep for TimerTrigger not causing
		// next event to get handled with threads, thus
		// causing interrupt
		Thread.sleep(1000);

		doSendEventAndConsumeAll(machine, TestEvents.E2);
		assertThat(testListener.s3EnteredLatch.await(2, TimeUnit.SECONDS)).isTrue();
		assertThat(machine.getState().getIds()).containsOnly(TestStates.S3);
		assertThat(testExitAction.latch.await(2, TimeUnit.SECONDS)).isTrue();
		assertThat(testExitAction.e).isNull();
	}

	@Configuration
	@EnableStateMachine
	static class Config1 extends EnumStateMachineConfigurerAdapter<TestStates, TestEvents> {

		@Override
		public void configure(StateMachineStateConfigurer<TestStates, TestEvents> states) throws Exception {
			states
				.withStates()
					.initial(TestStates.S1)
					.state(TestStates.S2, null, testExitAction())
					.state(TestStates.S3);
		}

		@Override
		public void configure(StateMachineTransitionConfigurer<TestStates, TestEvents> transitions) throws Exception {
			transitions
				.withExternal()
					.source(TestStates.S1)
					.target(TestStates.S2)
					.event(TestEvents.E1)
					.and()
				.withExternal()
					.source(TestStates.S2)
					.target(TestStates.S3)
					.event(TestEvents.E2)
					.and()
				.withInternal()
					.source(TestStates.S2)
					.action(testTimerAction())
					.timerOnce(1000);
		}

		@Bean
		public TestExitAction testExitAction() {
			return new TestExitAction();
		}

		@Bean
		public TestTimerAction testTimerAction() {
			return new TestTimerAction();
		}
	}

	@Configuration
	@EnableStateMachine
	static class Config2 extends EnumStateMachineConfigurerAdapter<TestStates, TestEvents> {

		@Override
		public void configure(StateMachineStateConfigurer<TestStates, TestEvents> states) throws Exception {
			states
				.withStates()
					.initial(TestStates.S1)
					.state(TestStates.S2, null, testExitAction())
					.state(TestStates.S3);
		}

		@Override
		public void configure(StateMachineTransitionConfigurer<TestStates, TestEvents> transitions) throws Exception {
			transitions
				.withExternal()
					.source(TestStates.S1)
					.target(TestStates.S2)
					.event(TestEvents.E1)
					.and()
				.withExternal()
					.source(TestStates.S2)
					.target(TestStates.S3)
					.event(TestEvents.E2)
					.and()
				.withInternal()
					.source(TestStates.S2)
					.action(testTimerAction())
					.timerOnce(1000);
		}

		@Bean
		public TestExitAction testExitAction() {
			return new TestExitAction();
		}

		@Bean
		public TestTimerAction testTimerAction() {
			return new TestTimerAction();
		}
	}

	private static class TestListener extends StateMachineListenerAdapter<TestStates, TestEvents> {

		final CountDownLatch s3EnteredLatch = new CountDownLatch(1);

		@Override
		public void stateEntered(State<TestStates, TestEvents> state) {
			if (state.getId().equals(TestStates.S3)) {
				s3EnteredLatch.countDown();
			}
		}
	}

	private static class TestTimerAction implements Action<TestStates, TestEvents> {

		final CountDownLatch latch = new CountDownLatch(1);
		volatile Exception e;

		@Override
		public void execute(StateContext<TestStates, TestEvents> context) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				this.e = e;
			} finally {
				latch.countDown();
			}
		}
	}

	private static class TestExitAction implements Action<TestStates, TestEvents> {

		final CountDownLatch latch = new CountDownLatch(1);
		volatile Exception e;

		@Override
		public void execute(StateContext<TestStates, TestEvents> context) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				this.e = e;
			} finally {
				latch.countDown();
			}
		}
	}

	@Override
	protected AnnotationConfigApplicationContext buildContext() {
		return new AnnotationConfigApplicationContext();
	}
}
