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
package ch.unibas.medizin.universe.statemachine.config.configurers;

import java.util.function.Function;

import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import ch.unibas.medizin.universe.statemachine.StateContext;
import ch.unibas.medizin.universe.statemachine.action.Action;
import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineTransitionBuilder;
import ch.unibas.medizin.universe.statemachine.guard.Guard;
import ch.unibas.medizin.universe.statemachine.guard.SpelExpressionGuard;
import ch.unibas.medizin.universe.statemachine.transition.TransitionKind;

import reactor.core.publisher.Mono;

/**
 * Default implementation of a {@link ExternalTransitionConfigurer}.
 *
 * @author Janne Valkealahti
 *
 * @param <S> the type of state
 * @param <E> the type of event
 */
public class DefaultExternalTransitionConfigurer<S, E> extends AbstractTransitionConfigurer<S, E>
		implements ExternalTransitionConfigurer<S, E> {

	@Override
	public void configure(StateMachineTransitionBuilder<S, E> builder) throws Exception {
		builder.addTransition(getSource(), getTarget(), getState(), getEvent(), getPeriod(), getCount(), getActions(), getGuard(), TransitionKind.EXTERNAL,
				getName());
	}

	@Override
	public ExternalTransitionConfigurer<S, E> source(S source) {
		setSource(source);
		return this;
	}

	@Override
	public ExternalTransitionConfigurer<S, E> target(S target) {
		setTarget(target);
		return this;
	}

	@Override
	public ExternalTransitionConfigurer<S, E> state(S state) {
		setState(state);
		return this;
	}

	@Override
	public ExternalTransitionConfigurer<S, E> event(E event) {
		setEvent(event);
		return this;
	}

	@Override
	public ExternalTransitionConfigurer<S, E> timer(long period) {
		setPeriod(period);
		return this;
	}

	@Override
	public ExternalTransitionConfigurer<S, E> timerOnce(long period) {
		setPeriod(period);
		setCount(1);
		return this;
	}

	@Override
	public ExternalTransitionConfigurer<S, E> action(Action<S, E> action) {
		return action(action, null);
	}

	@Override
	public ExternalTransitionConfigurer<S, E> action(Action<S, E> action, Action<S, E> error) {
		addAction(action, error);
		return this;
	}

	@Override
	public ExternalTransitionConfigurer<S, E> actionFunction(Function<StateContext<S, E>, Mono<Void>> action) {
		addActionFunction(action);
		return this;
	}

	@Override
	public ExternalTransitionConfigurer<S, E> guard(Guard<S, E> guard) {
		setGuard(guard);
		return this;
	}

	@Override
	public ExternalTransitionConfigurer<S, E> guardExpression(String expression) {
		SpelExpressionParser parser = new SpelExpressionParser(
				new SpelParserConfiguration(SpelCompilerMode.MIXED, null));
		setGuard(new SpelExpressionGuard<>(parser.parseExpression(expression)));
		return this;
	}

	@Override
	public ExternalTransitionConfigurer<S, E> name(String name) {
		setName(name);
		return this;
	}

}
