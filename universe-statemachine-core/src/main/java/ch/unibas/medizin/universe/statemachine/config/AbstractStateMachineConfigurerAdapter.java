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
package ch.unibas.medizin.universe.statemachine.config;

import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineConfigBuilder;
import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineConfigurationBuilder;
import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineConfigurationConfigurer;
import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineConfigurer;
import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineModelBuilder;
import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineModelConfigurer;
import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineStateBuilder;
import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineStateConfigurer;
import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineTransitionBuilder;
import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineTransitionConfigurer;
import ch.unibas.medizin.universe.statemachine.config.common.annotation.AnnotationBuilder;
import ch.unibas.medizin.universe.statemachine.config.common.annotation.ObjectPostProcessor;

/**
 * Adapter base implementation for {@link StateMachineConfigurer}.
 *
 * @author Janne Valkealahti
 *
 * @param <S> the type of state
 * @param <E> the type of event
 */
public abstract class AbstractStateMachineConfigurerAdapter<S, E> implements StateMachineConfigurer<S, E> {

	private StateMachineModelBuilder<S, E> modelBuilder;
	private StateMachineTransitionBuilder<S, E> transitionBuilder;
	private StateMachineStateBuilder<S, E> stateBuilder;
	private StateMachineConfigurationBuilder<S, E> configurationBuilder;

	@Override
	public final void init(StateMachineConfigBuilder<S, E> config) throws Exception {
		config.setSharedObject(StateMachineModelBuilder.class, getStateMachineModelBuilder());
		config.setSharedObject(StateMachineTransitionBuilder.class, getStateMachineTransitionBuilder());
		config.setSharedObject(StateMachineStateBuilder.class, getStateMachineStateBuilder());
		config.setSharedObject(StateMachineConfigurationBuilder.class, getStateMachineConfigurationBuilder());
	}

	@Override
	public void configure(StateMachineConfigBuilder<S, E> config) throws Exception {
	}

	@Override
	public void configure(StateMachineModelConfigurer<S, E> model) throws Exception {
	}

	@Override
	public void configure(StateMachineConfigurationConfigurer<S, E> config) throws Exception {
	}

	@Override
	public void configure(StateMachineStateConfigurer<S, E> states) throws Exception {
	}

	@Override
	public void configure(StateMachineTransitionConfigurer<S, E> transitions) throws Exception {
	}

	@Override
	public boolean isAssignable(AnnotationBuilder<StateMachineConfig<S, E>> builder) {
		return builder instanceof StateMachineConfigBuilder;
	}

	protected final StateMachineModelBuilder<S, E> getStateMachineModelBuilder() throws Exception {
		if (modelBuilder != null) {
			return modelBuilder;
		}
		modelBuilder = new StateMachineModelBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
		configure(modelBuilder);
		return modelBuilder;
	}

	protected final StateMachineTransitionBuilder<S, E> getStateMachineTransitionBuilder() throws Exception {
		if (transitionBuilder != null) {
			return transitionBuilder;
		}
		transitionBuilder = new StateMachineTransitionBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
		configure(transitionBuilder);
		return transitionBuilder;
	}

	protected final StateMachineStateBuilder<S, E> getStateMachineStateBuilder() throws Exception {
		if (stateBuilder != null) {
			return stateBuilder;
		}
		stateBuilder = new StateMachineStateBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
		configure(stateBuilder);
		return stateBuilder;
	}

	protected final StateMachineConfigurationBuilder<S, E> getStateMachineConfigurationBuilder() throws Exception {
		if (configurationBuilder != null) {
			return configurationBuilder;
		}
		configurationBuilder = new StateMachineConfigurationBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
		configure(configurationBuilder);
		return configurationBuilder;
	}

}
