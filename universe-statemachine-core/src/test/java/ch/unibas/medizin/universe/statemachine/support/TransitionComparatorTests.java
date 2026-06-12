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
package ch.unibas.medizin.universe.statemachine.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;

import ch.unibas.medizin.universe.statemachine.support.TransitionComparator;
import org.junit.jupiter.api.Test;
import ch.unibas.medizin.universe.statemachine.AbstractStateMachineTests.TestEvents;
import ch.unibas.medizin.universe.statemachine.AbstractStateMachineTests.TestStates;
import ch.unibas.medizin.universe.statemachine.ObjectStateMachine;
import ch.unibas.medizin.universe.statemachine.state.DefaultPseudoState;
import ch.unibas.medizin.universe.statemachine.state.EnumState;
import ch.unibas.medizin.universe.statemachine.state.PseudoState;
import ch.unibas.medizin.universe.statemachine.state.PseudoStateKind;
import ch.unibas.medizin.universe.statemachine.state.State;
import ch.unibas.medizin.universe.statemachine.state.StateMachineState;
import ch.unibas.medizin.universe.statemachine.transition.DefaultExternalTransition;
import ch.unibas.medizin.universe.statemachine.transition.Transition;
import ch.unibas.medizin.universe.statemachine.transition.TransitionConflictPolicy;
import ch.unibas.medizin.universe.statemachine.trigger.EventTrigger;

/**
 * Tests for {@link TransitionComparator}.
 *
 * @author Janne Valkealahti
 *
 */
public class TransitionComparatorTests {

	@Test
	public void testCompareWithParentAndChild() {
		PseudoState<TestStates, TestEvents> pseudoState = new DefaultPseudoState<>(PseudoStateKind.INITIAL);

		State<TestStates, TestEvents> stateS111 = new EnumState<>(TestStates.S111, null, null, null, pseudoState);

		// submachine 11
		Collection<State<TestStates, TestEvents>> substates111 = new ArrayList<>();
		substates111.add(stateS111);
		Collection<Transition<TestStates, TestEvents>> subtransitions111 = new ArrayList<>();
		ObjectStateMachine<TestStates, TestEvents> submachine11 = new ObjectStateMachine<>(substates111,
                subtransitions111, stateS111);

		// submachine 1
		StateMachineState<TestStates, TestEvents> stateS11 = new StateMachineState<>(TestStates.S11, submachine11,
                null, null, null, pseudoState);

		Collection<State<TestStates, TestEvents>> substates11 = new ArrayList<>();
		substates11.add(stateS11);
		Collection<Transition<TestStates, TestEvents>> subtransitions11 = new ArrayList<>();
		ObjectStateMachine<TestStates, TestEvents> submachine1 = new ObjectStateMachine<>(substates11,
                subtransitions11, stateS11);

		// machine
		StateMachineState<TestStates, TestEvents> stateS1 = new StateMachineState<>(TestStates.S1, submachine1, null,
                null, null, pseudoState);

		DefaultExternalTransition<TestStates, TestEvents> transitionFromS111ToS1 = new DefaultExternalTransition<>(
                stateS111, stateS1, null, TestEvents.E1, null, new EventTrigger<>(TestEvents.E1));
		DefaultExternalTransition<TestStates, TestEvents> transitionFromS11ToS1 = new DefaultExternalTransition<>(
                stateS11, stateS1, null, TestEvents.E1, null, new EventTrigger<>(TestEvents.E1));

		TransitionComparator<TestStates, TestEvents> comparator = new TransitionComparator<>(null);
		assertThat(comparator.compare(transitionFromS111ToS1, transitionFromS11ToS1)).isEqualTo(-1);
		assertThat(comparator.compare(transitionFromS11ToS1, transitionFromS111ToS1)).isEqualTo(1);
		assertThat(comparator.compare(transitionFromS111ToS1, transitionFromS111ToS1)).isZero();
		assertThat(comparator.compare(transitionFromS11ToS1, transitionFromS11ToS1)).isZero();

		comparator = new TransitionComparator<>(TransitionConflictPolicy.CHILD);
		assertThat(comparator.compare(transitionFromS111ToS1, transitionFromS11ToS1)).isEqualTo(-1);
		assertThat(comparator.compare(transitionFromS11ToS1, transitionFromS111ToS1)).isEqualTo(1);
		assertThat(comparator.compare(transitionFromS111ToS1, transitionFromS111ToS1)).isZero();
		assertThat(comparator.compare(transitionFromS11ToS1, transitionFromS11ToS1)).isZero();

		comparator = new TransitionComparator<>(TransitionConflictPolicy.PARENT);
		assertThat(comparator.compare(transitionFromS111ToS1, transitionFromS11ToS1)).isEqualTo(1);
		assertThat(comparator.compare(transitionFromS11ToS1, transitionFromS111ToS1)).isEqualTo(-1);
		assertThat(comparator.compare(transitionFromS111ToS1, transitionFromS111ToS1)).isZero();
		assertThat(comparator.compare(transitionFromS11ToS1, transitionFromS11ToS1)).isZero();
	}
}
