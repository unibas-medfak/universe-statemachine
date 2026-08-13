/*
 * Copyright 2015-2021 the original author or authors.
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
package ch.unibas.medizin.universe.statemachine.config.builders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import ch.unibas.medizin.universe.statemachine.StateContext;
import ch.unibas.medizin.universe.statemachine.config.common.annotation.AbstractConfiguredAnnotationBuilder;
import ch.unibas.medizin.universe.statemachine.config.common.annotation.AnnotationBuilder;
import ch.unibas.medizin.universe.statemachine.config.common.annotation.ObjectPostProcessor;
import ch.unibas.medizin.universe.statemachine.config.configurers.ChoiceTransitionConfigurer;
import ch.unibas.medizin.universe.statemachine.config.configurers.DefaultChoiceTransitionConfigurer;
import ch.unibas.medizin.universe.statemachine.config.configurers.DefaultEntryTransitionConfigurer;
import ch.unibas.medizin.universe.statemachine.config.configurers.DefaultExitTransitionConfigurer;
import ch.unibas.medizin.universe.statemachine.config.configurers.DefaultExternalTransitionConfigurer;
import ch.unibas.medizin.universe.statemachine.config.configurers.DefaultForkTransitionConfigurer;
import ch.unibas.medizin.universe.statemachine.config.configurers.DefaultHistoryTransitionConfigurer;
import ch.unibas.medizin.universe.statemachine.config.configurers.DefaultInternalTransitionConfigurer;
import ch.unibas.medizin.universe.statemachine.config.configurers.DefaultJoinTransitionConfigurer;
import ch.unibas.medizin.universe.statemachine.config.configurers.DefaultJunctionTransitionConfigurer;
import ch.unibas.medizin.universe.statemachine.config.configurers.DefaultLocalTransitionConfigurer;
import ch.unibas.medizin.universe.statemachine.config.configurers.EntryTransitionConfigurer;
import ch.unibas.medizin.universe.statemachine.config.configurers.ExitTransitionConfigurer;
import ch.unibas.medizin.universe.statemachine.config.configurers.ExternalTransitionConfigurer;
import ch.unibas.medizin.universe.statemachine.config.configurers.ForkTransitionConfigurer;
import ch.unibas.medizin.universe.statemachine.config.configurers.HistoryTransitionConfigurer;
import ch.unibas.medizin.universe.statemachine.config.configurers.InternalTransitionConfigurer;
import ch.unibas.medizin.universe.statemachine.config.configurers.JoinTransitionConfigurer;
import ch.unibas.medizin.universe.statemachine.config.configurers.JunctionTransitionConfigurer;
import ch.unibas.medizin.universe.statemachine.config.configurers.LocalTransitionConfigurer;
import ch.unibas.medizin.universe.statemachine.config.model.ChoiceData;
import ch.unibas.medizin.universe.statemachine.config.model.EntryData;
import ch.unibas.medizin.universe.statemachine.config.model.ExitData;
import ch.unibas.medizin.universe.statemachine.config.model.HistoryData;
import ch.unibas.medizin.universe.statemachine.config.model.JunctionData;
import ch.unibas.medizin.universe.statemachine.config.model.TransitionData;
import ch.unibas.medizin.universe.statemachine.config.model.TransitionsData;
import ch.unibas.medizin.universe.statemachine.transition.TransitionKind;

import reactor.core.publisher.Mono;

/**
 * {@link AnnotationBuilder} for {@link TransitionsData}.
 *
 * @author Janne Valkealahti
 *
 * @param <S> the type of state
 * @param <E> the type of event
 */
public class StateMachineTransitionBuilder<S, E>
		extends
		AbstractConfiguredAnnotationBuilder<TransitionsData<S, E>, StateMachineTransitionConfigurer<S, E>, StateMachineTransitionBuilder<S, E>>
		implements StateMachineTransitionConfigurer<S, E> {

	private final Collection<TransitionData<S, E>> transitionData = new ArrayList<>();
	private final Map<S, List<ChoiceData<S, E>>> choices = new HashMap<>();
	private final Map<S, List<JunctionData<S, E>>> junctions = new HashMap<>();
	private final Map<S, List<S>> forks = new HashMap<>();
	private final Map<S, List<S>> joins = new HashMap<>();
	private final Collection<EntryData<S, E>> entryData = new ArrayList<>();
	private final Collection<ExitData<S, E>> exitData = new ArrayList<>();
	private final Collection<HistoryData<S, E>> historyData = new ArrayList<>();

	/**
	 * Instantiates a new state machine transition builder.
	 */
	public StateMachineTransitionBuilder() {
		super();
	}

	/**
	 * Instantiates a new state machine transition builder.
	 *
	 * @param objectPostProcessor the object post processor
	 * @param allowConfigurersOfSameType the allow configurers of same type
	 */
	public StateMachineTransitionBuilder(ObjectPostProcessor<Object> objectPostProcessor,
			boolean allowConfigurersOfSameType) {
		super(objectPostProcessor, allowConfigurersOfSameType);
	}

	/**
	 * Instantiates a new state machine transition builder.
	 *
	 * @param objectPostProcessor the object post processor
	 */
	public StateMachineTransitionBuilder(ObjectPostProcessor<Object> objectPostProcessor) {
		super(objectPostProcessor);
	}

	@Override
	protected TransitionsData<S, E> performBuild() throws Exception {
		return new TransitionsData<>(transitionData, choices, junctions, forks, joins, entryData, exitData, historyData);
	}

	@Override
	public ExternalTransitionConfigurer<S, E> withExternal() throws Exception {
		return apply(new DefaultExternalTransitionConfigurer<>());
	}

	@Override
	public InternalTransitionConfigurer<S, E> withInternal() throws Exception {
		return apply(new DefaultInternalTransitionConfigurer<>());
	}

	@Override
	public LocalTransitionConfigurer<S, E> withLocal() throws Exception {
		return apply(new DefaultLocalTransitionConfigurer<>());
	}

	@Override
	public ChoiceTransitionConfigurer<S, E> withChoice() throws Exception {
		return apply(new DefaultChoiceTransitionConfigurer<>());
	}

	@Override
	public JunctionTransitionConfigurer<S, E> withJunction() throws Exception {
		return apply(new DefaultJunctionTransitionConfigurer<>());
	}

	@Override
	public ForkTransitionConfigurer<S, E> withFork() throws Exception {
		return apply(new DefaultForkTransitionConfigurer<>());
	}

	@Override
	public JoinTransitionConfigurer<S, E> withJoin() throws Exception {
		return apply(new DefaultJoinTransitionConfigurer<>());
	}

	@Override
	public EntryTransitionConfigurer<S, E> withEntry() throws Exception {
		return apply(new DefaultEntryTransitionConfigurer<>());
	}

	@Override
	public ExitTransitionConfigurer<S, E> withExit() throws Exception {
		return apply(new DefaultExitTransitionConfigurer<>());
	}

	@Override
	public HistoryTransitionConfigurer<S, E> withHistory() throws Exception {
		return apply(new DefaultHistoryTransitionConfigurer<>());
	}

	/**
	 * Adds the transition.
	 *
	 * @param source the source
	 * @param target the target
	 * @param state the state
	 * @param event the event
	 * @param period the period
	 * @param count the count
	 * @param actions the actions
	 * @param guard the guard
	 * @param kind the kind
	 * @param name the name
	 */
	public void addTransition(S source, S target, S state, E event, Long period, Integer count,
			Collection<Function<StateContext<S, E>, Mono<Void>>> actions,
			Function<StateContext<S, E>, Mono<Boolean>> guard, TransitionKind kind,
			String name) {
		transitionData.add(new TransitionData<>(source, target, state, event, period, count, actions, guard, kind, name));
	}

	/**
	 * Adds the choice.
	 *
	 * @param source the source
	 * @param choices the choices
	 */
	public void addChoice(S source, List<ChoiceData<S, E>> choices) {
		this.choices.put(source, choices);
	}

	/**
	 * Adds the junction.
	 *
	 * @param source the source
	 * @param junctions the junctions
	 */
	public void addJunction(S source, List<JunctionData<S, E>> junctions) {
		this.junctions.put(source, junctions);
	}

	/**
	 * Adds the entry.
	 *
	 * @param source the source
	 * @param target the target
	 */
	public void addEntry(S source, S target) {
		this.entryData.add(new EntryData<>(source, target));
	}

	/**
	 * Adds the exit.
	 *
	 * @param source the source
	 * @param target the target
	 */
	public void addExit(S source, S target) {
		this.exitData.add(new ExitData<>(source, target));
	}

	/**
	 * Adds the fork.
	 *
	 * @param source the source
	 * @param targets the targets
	 */
	public void addFork(S source, List<S> targets) {
		this.forks.put(source, targets);
	}

	/**
	 * Adds the join.
	 *
	 * @param target the target
	 * @param sources the sources
	 */
	public void addJoin(S target, List<S> sources) {
		this.joins.put(target, sources);
	}

	/**
	 * Adds the default history.
	 *
	 * @param source the source
	 * @param target the target
	 */
	public void addDefaultHistory(S source, S target) {
		this.historyData.add(new HistoryData<>(source, target));
	}
}
