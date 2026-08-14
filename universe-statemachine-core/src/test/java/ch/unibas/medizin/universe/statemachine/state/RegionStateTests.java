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
package ch.unibas.medizin.universe.statemachine.state;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import ch.unibas.medizin.universe.statemachine.AbstractStateMachineTests;
import ch.unibas.medizin.universe.statemachine.ObjectStateMachine;
import ch.unibas.medizin.universe.statemachine.region.Region;
import ch.unibas.medizin.universe.statemachine.transition.DefaultExternalTransition;
import ch.unibas.medizin.universe.statemachine.transition.Transition;
import ch.unibas.medizin.universe.statemachine.trigger.EventTrigger;

/**
 * Tests for states using a submachine.
 *
 * @author Janne Valkealahti
 *
 */
public class RegionStateTests extends AbstractStateMachineTests {

	@Test
	public void testSimpleRegionState() {
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

		DefaultExternalTransition<TestStates,TestEvents> transitionFromSIToS1 =
                new DefaultExternalTransition<>(stateSI, stateS1, null, TestEvents.E1, null, new EventTrigger<>(TestEvents.E1));

		DefaultExternalTransition<TestStates,TestEvents> transitionFromS1ToS2 =
                new DefaultExternalTransition<>(stateS1, stateS2, null, TestEvents.E2, null, new EventTrigger<>(TestEvents.E2));

		DefaultExternalTransition<TestStates,TestEvents> transitionFromS2ToS3 =
                new DefaultExternalTransition<>(stateS2, stateS3, null, TestEvents.E3, null, new EventTrigger<>(TestEvents.E3));

		transitions.add(transitionFromSIToS1);
		transitions.add(transitionFromS1ToS2);
		transitions.add(transitionFromS2ToS3);

		BeanFactory beanFactory = new DefaultListableBeanFactory();
		ObjectStateMachine<TestStates, TestEvents> machine = new ObjectStateMachine<>(states, transitions, stateSI);
		machine.setBeanFactory(beanFactory);
		machine.afterPropertiesSet();
		machine.start();

		Collection<Region<TestStates,TestEvents>> regions = new ArrayList<>();
		regions.add(machine);
		RegionState<TestStates,TestEvents> state = new RegionState<>(TestStates.S11, regions);

		assertThat(state.isSimple()).isFalse();
		assertThat(state.isComposite()).isTrue();
		assertThat(state.isOrthogonal()).isFalse();
		assertThat(state.isSubmachineState()).isFalse();

		assertThat(state.getIds()).containsOnly(TestStates.SI, TestStates.S11);



	}

}
