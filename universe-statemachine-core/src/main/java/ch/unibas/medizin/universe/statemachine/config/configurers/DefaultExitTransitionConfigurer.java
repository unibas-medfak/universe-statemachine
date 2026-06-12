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
package ch.unibas.medizin.universe.statemachine.config.configurers;

import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineTransitionBuilder;
import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineTransitionConfigurer;
import ch.unibas.medizin.universe.statemachine.config.common.annotation.AnnotationConfigurerAdapter;
import ch.unibas.medizin.universe.statemachine.config.model.TransitionsData;

/**
 * Default implementation of a {@link ExitTransitionConfigurer}.
 *
 * @author Janne Valkealahti
 *
 * @param <S> the type of state
 * @param <E> the type of event
 */
public class DefaultExitTransitionConfigurer<S, E>
		extends	AnnotationConfigurerAdapter<TransitionsData<S, E>, StateMachineTransitionConfigurer<S, E>, StateMachineTransitionBuilder<S, E>>
		implements ExitTransitionConfigurer<S, E> {

	private S source;
	private S target;

	@Override
	public void configure(StateMachineTransitionBuilder<S, E> builder) throws Exception {
		builder.addExit(source, target);
	}

	@Override
	public ExitTransitionConfigurer<S, E> source(S source) {
		this.source = source;
		return this;
	}

	@Override
	public ExitTransitionConfigurer<S, E> target(S target) {
		this.target = target;
		return this;
	}
}
