/*
 * Copyright 2016 the original author or authors.
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
package ch.unibas.medizin.universe.statemachine.persist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ch.unibas.medizin.universe.statemachine.ExtendedState;
import ch.unibas.medizin.universe.statemachine.StateMachine;
import ch.unibas.medizin.universe.statemachine.StateMachineContext;
import ch.unibas.medizin.universe.statemachine.StateMachinePersist;
import ch.unibas.medizin.universe.statemachine.region.Region;
import ch.unibas.medizin.universe.statemachine.state.AbstractState;
import ch.unibas.medizin.universe.statemachine.state.HistoryPseudoState;
import ch.unibas.medizin.universe.statemachine.state.PseudoState;
import ch.unibas.medizin.universe.statemachine.state.State;
import ch.unibas.medizin.universe.statemachine.support.AbstractStateMachine;
import ch.unibas.medizin.universe.statemachine.support.DefaultExtendedState;
import ch.unibas.medizin.universe.statemachine.support.DefaultStateMachineContext;
import org.springframework.util.Assert;

/**
 * Base implementation of a {@link StateMachinePersister} easing persist
 * and restore operations with a {@link StateMachinePersist}.
 *
 * @author Janne Valkealahti
 *
 * @param <S> the type of state
 * @param <E> the type of event
 * @param <T> the type of context object
 */
public abstract class AbstractStateMachinePersister<S, E, T> implements StateMachinePersister<S, E, T> {

	private final StateMachinePersist<S, E, T> stateMachinePersist;

	/**
	 * Instantiates a new abstract state machine persister.
	 *
	 * @param stateMachinePersist the state machine persist
	 */
	public AbstractStateMachinePersister(StateMachinePersist<S, E, T> stateMachinePersist) {
		Assert.notNull(stateMachinePersist, "StateMachinePersist must be set");
		this.stateMachinePersist = stateMachinePersist;
	}

	@Override
	public final void persist(StateMachine<S, E> stateMachine, T contextObj) throws Exception {
		stateMachinePersist.write(buildStateMachineContext(stateMachine), contextObj);
	}

	@Override
	public final StateMachine<S, E> restore(StateMachine<S, E> stateMachine, T contextObj) throws Exception {
		final StateMachineContext<S, E> context = stateMachinePersist.read(contextObj);
		stateMachine.stopReactively().block();
		stateMachine.getStateMachineAccessor().doWithAllRegions(function -> function.resetStateMachineReactively(context).block());
		stateMachine.startReactively().block();
		return stateMachine;
	}

	protected StateMachineContext<S, E> buildStateMachineContext(StateMachine<S, E> stateMachine) {
		ExtendedState extendedState = new DefaultExtendedState();
		extendedState.getVariables().putAll(stateMachine.getExtendedState().getVariables());

		ArrayList<StateMachineContext<S, E>> childs = new ArrayList<StateMachineContext<S, E>>();
		S id = null;
		State<S, E> state = stateMachine.getState();
		if (state.isSubmachineState()) {
			StateMachine<S, E> submachine = ((AbstractState<S, E>) state).getSubmachine();
			id = submachine.getState().getId();
			childs.add(buildStateMachineContext(submachine));
		} else if (state.isOrthogonal()) {
			Collection<Region<S, E>> regions = ((AbstractState<S, E>)state).getRegions();
			for (Region<S, E> r : regions) {
				StateMachine<S, E> rsm = (StateMachine<S, E>) r;
				childs.add(buildStateMachineContext(rsm));
			}
			id = state.getId();
		} else {
			id = state.getId();
		}

		// building history state mappings
		Map<S, S> historyStates = new HashMap<S, S>();
		PseudoState<S, E> historyState = ((AbstractStateMachine<S, E>) stateMachine).getHistoryState();
		if (historyState != null && ((HistoryPseudoState<S, E>)historyState).getState() != null) {
			historyStates.put(null, ((HistoryPseudoState<S, E>) historyState).getState().getId());
		}
		Collection<State<S, E>> states = stateMachine.getStates();
		for (State<S, E> ss : states) {
			if (ss.isSubmachineState()) {
				StateMachine<S, E> submachine = ((AbstractState<S, E>) ss).getSubmachine();
				PseudoState<S, E> ps = ((AbstractStateMachine<S, E>) submachine).getHistoryState();
				if (ps != null) {
					State<S, E> pss = ((HistoryPseudoState<S, E>)ps).getState();
					if (pss != null) {
						historyStates.put(ss.getId(), pss.getId());
					}
				}
			}
		}
		return new DefaultStateMachineContext<S, E>(childs, id, null, null, extendedState, historyStates, stateMachine.getId());
	}
}
