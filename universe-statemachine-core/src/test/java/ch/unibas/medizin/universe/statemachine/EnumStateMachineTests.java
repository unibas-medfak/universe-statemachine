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
package ch.unibas.medizin.universe.statemachine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

import ch.unibas.medizin.universe.statemachine.ObjectStateMachine;
import ch.unibas.medizin.universe.statemachine.StateContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.messaging.support.MessageBuilder;
import ch.unibas.medizin.universe.statemachine.action.Action;
import ch.unibas.medizin.universe.statemachine.action.Actions;
import ch.unibas.medizin.universe.statemachine.state.DefaultPseudoState;
import ch.unibas.medizin.universe.statemachine.state.EnumState;
import ch.unibas.medizin.universe.statemachine.state.PseudoState;
import ch.unibas.medizin.universe.statemachine.state.PseudoStateKind;
import ch.unibas.medizin.universe.statemachine.state.State;
import ch.unibas.medizin.universe.statemachine.transition.DefaultExternalTransition;
import ch.unibas.medizin.universe.statemachine.transition.DefaultInternalTransition;
import ch.unibas.medizin.universe.statemachine.transition.Transition;
import ch.unibas.medizin.universe.statemachine.trigger.EventTrigger;

import reactor.core.publisher.Mono;

public class EnumStateMachineTests extends AbstractStateMachineTests {

	@Test
	public void testSimpleStateSwitch() {
		PseudoState<TestStates,TestEvents> pseudoState = new DefaultPseudoState<>(PseudoStateKind.INITIAL);
		State<TestStates,TestEvents> stateSI = new EnumState<>(TestStates.SI, pseudoState);
		State<TestStates,TestEvents> stateS1 = new EnumState<>(TestStates.S1);
		State<TestStates,TestEvents> stateS2 = new EnumState<>(TestStates.S2);
		State<TestStates,TestEvents> stateS3 = new EnumState<>(TestStates.S3);

		Collection<State<TestStates,TestEvents>> states = new ArrayList<>();
		states.add(stateSI);
		states.add(stateS1);
		states.add(stateS2);
		states.add(stateS3);

		Collection<Transition<TestStates,TestEvents>> transitions = new ArrayList<>();

		Collection<Function<StateContext<TestStates, TestEvents>, Mono<Void>>> actionsFromSIToS1 = new ArrayList<>();
		actionsFromSIToS1.add(Actions.from(new LoggingAction("actionsFromSIToS1")));
		DefaultExternalTransition<TestStates,TestEvents> transitionFromSIToS1 =
                new DefaultExternalTransition<>(stateSI, stateS1, actionsFromSIToS1, TestEvents.E1, null, new EventTrigger<>(TestEvents.E1));

		Collection<Function<StateContext<TestStates, TestEvents>, Mono<Void>>> actionsFromS1ToS2 = new ArrayList<>();
		actionsFromS1ToS2.add(Actions.from(new LoggingAction("actionsFromS1ToS2")));
		DefaultExternalTransition<TestStates,TestEvents> transitionFromS1ToS2 =
                new DefaultExternalTransition<>(stateS1, stateS2, actionsFromS1ToS2, TestEvents.E2, null, new EventTrigger<>(TestEvents.E2));

		Collection<Function<StateContext<TestStates, TestEvents>, Mono<Void>>> actionsFromS2ToS3 = new ArrayList<>();
		actionsFromS1ToS2.add(Actions.from(new LoggingAction("actionsFromS2ToS3")));
		DefaultExternalTransition<TestStates,TestEvents> transitionFromS2ToS3 =
                new DefaultExternalTransition<>(stateS2, stateS3, actionsFromS2ToS3, TestEvents.E3, null, new EventTrigger<>(TestEvents.E3));

		transitions.add(transitionFromSIToS1);
		transitions.add(transitionFromS1ToS2);
		transitions.add(transitionFromS2ToS3);

		BeanFactory beanFactory = new DefaultListableBeanFactory();
		ObjectStateMachine<TestStates, TestEvents> machine = new ObjectStateMachine<>(states, transitions, stateSI);
		machine.setBeanFactory(beanFactory);
		machine.afterPropertiesSet();
		machine.start();

		State<TestStates,TestEvents> initialState = machine.getInitialState();
		assertThat(initialState).isEqualTo(stateSI);

		State<TestStates,TestEvents> state = machine.getState();
		assertThat(state).isEqualTo(stateSI);

		machine.sendEvent(MessageBuilder.withPayload(TestEvents.E1).build());
		state = machine.getState();
		assertThat(state).isEqualTo(stateS1);

		machine.sendEvent(MessageBuilder.withPayload(TestEvents.E2).build());
		state = machine.getState();
		assertThat(state).isEqualTo(stateS2);

		// not processed
		machine.sendEvent(MessageBuilder.withPayload(TestEvents.E1).build());
		state = machine.getState();
		assertThat(state).isEqualTo(stateS2);

		machine.sendEvent(MessageBuilder.withPayload(TestEvents.E3).build());
		state = machine.getState();
		assertThat(state).isEqualTo(stateS3);
	}

	@Test
	public void testDeferredEvents() {
		PseudoState<TestStates,TestEvents> pseudoState = new DefaultPseudoState<>(PseudoStateKind.INITIAL);

		Collection<TestEvents> deferred = new ArrayList<>();
		deferred.add(TestEvents.E2);
		deferred.add(TestEvents.E3);

		// states
		State<TestStates,TestEvents> stateSI = new EnumState<>(TestStates.SI, deferred, null, null, pseudoState);
		State<TestStates,TestEvents> stateS1 = new EnumState<>(TestStates.S1);
		State<TestStates,TestEvents> stateS2 = new EnumState<>(TestStates.S2);
		State<TestStates,TestEvents> stateS3 = new EnumState<>(TestStates.S3);

		Collection<State<TestStates,TestEvents>> states = new ArrayList<>();
		states.add(stateSI);
		states.add(stateS1);
		states.add(stateS2);
		states.add(stateS3);

		// transitions
		Collection<Transition<TestStates,TestEvents>> transitions = new ArrayList<>();

		Collection<Function<StateContext<TestStates, TestEvents>, Mono<Void>>> actionsFromSIToS1 = new ArrayList<>();
		actionsFromSIToS1.add(Actions.from(new LoggingAction("actionsFromSIToS1")));
		DefaultExternalTransition<TestStates,TestEvents> transitionFromSIToS1 =
                new DefaultExternalTransition<>(stateSI, stateS1, actionsFromSIToS1, TestEvents.E1, null, new EventTrigger<>(TestEvents.E1));

		Collection<Function<StateContext<TestStates, TestEvents>, Mono<Void>>> actionsFromS1ToS2 = new ArrayList<>();
		actionsFromS1ToS2.add(Actions.from(new LoggingAction("actionsFromS1ToS2")));
		DefaultExternalTransition<TestStates,TestEvents> transitionFromS1ToS2 =
                new DefaultExternalTransition<>(stateS1, stateS2, actionsFromS1ToS2, TestEvents.E2, null, new EventTrigger<>(TestEvents.E2));

		Collection<Function<StateContext<TestStates, TestEvents>, Mono<Void>>> actionsFromS2ToS3 = new ArrayList<>();
		actionsFromS1ToS2.add(Actions.from(new LoggingAction("actionsFromS2ToS3")));
		DefaultExternalTransition<TestStates,TestEvents> transitionFromS2ToS3 =
                new DefaultExternalTransition<>(stateS2, stateS3, actionsFromS2ToS3, TestEvents.E3, null, new EventTrigger<>(TestEvents.E3));

		transitions.add(transitionFromSIToS1);
		transitions.add(transitionFromS1ToS2);
		transitions.add(transitionFromS2ToS3);

		// create machine
		BeanFactory beanFactory = new DefaultListableBeanFactory();
		ObjectStateMachine<TestStates, TestEvents> machine = new ObjectStateMachine<>(states, transitions, stateSI);
		machine.setBeanFactory(beanFactory);
		machine.afterPropertiesSet();
		machine.start();

		State<TestStates,TestEvents> initialState = machine.getInitialState();
		assertThat(initialState).isEqualTo(stateSI);

		State<TestStates,TestEvents> state = machine.getState();
		assertThat(state).isEqualTo(stateSI);


		machine.sendEvent(MessageBuilder.withPayload(TestEvents.E2).build());
		machine.sendEvent(MessageBuilder.withPayload(TestEvents.E3).build());
		state = machine.getState();
		assertThat(state).isEqualTo(stateSI);


		machine.sendEvent(MessageBuilder.withPayload(TestEvents.E1).build());
		state = machine.getState();
		assertThat(state).isEqualTo(stateS3);
	}

	@Test
	public void testInternalTransitions() {
		PseudoState<TestStates,TestEvents> pseudoState = new DefaultPseudoState<>(PseudoStateKind.INITIAL);
		State<TestStates,TestEvents> stateSI = new EnumState<>(TestStates.SI, pseudoState);

		Collection<State<TestStates,TestEvents>> states = new ArrayList<>();
		states.add(stateSI);

		Collection<Function<StateContext<TestStates, TestEvents>, Mono<Void>>> actionsInSI = new ArrayList<>();
		actionsInSI.add(Actions.from(new LoggingAction("actionsInSI")));
		DefaultInternalTransition<TestStates,TestEvents> transitionInternalSI =
                new DefaultInternalTransition<>(stateSI, actionsInSI, TestEvents.E1, null, new EventTrigger<>(TestEvents.E1));

		// transitions
		Collection<Transition<TestStates,TestEvents>> transitions = new ArrayList<>();
		transitions.add(transitionInternalSI);

		BeanFactory beanFactory = new DefaultListableBeanFactory();
		ObjectStateMachine<TestStates, TestEvents> machine = new ObjectStateMachine<>(states, transitions, stateSI);
		machine.setBeanFactory(beanFactory);
		machine.afterPropertiesSet();
		machine.start();

		machine.sendEvent(MessageBuilder.withPayload(TestEvents.E1).build());
	}

	private static class LoggingAction implements Action<TestStates, TestEvents> {

		private static final Log log = LogFactory.getLog(LoggingAction.class);

		private final String message;

		public LoggingAction(String message) {
			this.message = message;
		}

		@Override
		public void execute(StateContext<TestStates, TestEvents> context) {
			log.info("Hello from LoggingAction " + message);
		}

	}

}
