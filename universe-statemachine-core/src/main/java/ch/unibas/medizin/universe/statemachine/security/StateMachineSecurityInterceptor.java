/*
 * Copyright 2015 the original author or authors.
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
package ch.unibas.medizin.universe.statemachine.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.messaging.Message;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.ExpressionUtils;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import ch.unibas.medizin.universe.statemachine.StateContext;
import ch.unibas.medizin.universe.statemachine.StateMachine;
import ch.unibas.medizin.universe.statemachine.support.StateMachineInterceptor;
import ch.unibas.medizin.universe.statemachine.support.StateMachineInterceptorAdapter;
import ch.unibas.medizin.universe.statemachine.transition.Transition;

/**
 * {@link StateMachineInterceptor} which can be registered into a {@link StateMachine}
 * in order to intercept various security-related checks.
 *
 * @param <S> the type of state
 * @param <E> the type of event
 */
public class StateMachineSecurityInterceptor<S, E> extends StateMachineInterceptorAdapter<S, E> {

	private AuthorizationManager<Object> transitionAuthorizationManager;
	private AuthorizationManager<Object> eventAuthorizationManager;
	private SecurityRule eventSecurityRule;

	private final ExpressionParser expressionParser = new SpelExpressionParser(
			new SpelParserConfiguration(SpelCompilerMode.OFF, null));

	public StateMachineSecurityInterceptor() {
		this(null, null);
	}

	public StateMachineSecurityInterceptor(AuthorizationManager<Object> transitionAuthorizationManager,
			AuthorizationManager<Object> eventAuthorizationManager) {
		this(transitionAuthorizationManager, eventAuthorizationManager, null);
	}

	public StateMachineSecurityInterceptor(AuthorizationManager<Object> transitionAuthorizationManager,
			AuthorizationManager<Object> eventAuthorizationManager, SecurityRule eventSecurityRule) {
		this.transitionAuthorizationManager = transitionAuthorizationManager;
		this.eventAuthorizationManager = eventAuthorizationManager;
		this.eventSecurityRule = eventSecurityRule;
	}

	@Override
	public Message<E> preEvent(Message<E> message, StateMachine<S, E> stateMachine) {
		if (eventSecurityRule != null) {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			AuthorizationManager<Object> manager = eventAuthorizationManager != null
					? eventAuthorizationManager
					: buildEventManager(eventSecurityRule);
			AuthorizationResult result = manager.authorize(() -> auth, message);
			if (result != null && !result.isGranted()) {
				throw new AccessDeniedException("Access is denied for event");
			}
		}
		return super.preEvent(message, stateMachine);
	}

	@Override
	public StateContext<S, E> preTransition(StateContext<S, E> stateContext) {
		Transition<S, E> transition = stateContext.getTransition();
		SecurityRule rule = transition.getSecurityRule();
		if (rule != null) {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			AuthorizationManager<Object> manager = transitionAuthorizationManager != null
					? transitionAuthorizationManager
					: buildTransitionManager(rule);
			AuthorizationResult result = manager.authorize(() -> auth, transition);
			if (result != null && !result.isGranted()) {
				throw new AccessDeniedException("Access is denied for transition");
			}
		}
		return super.preTransition(stateContext);
	}

	public void setEventAuthorizationManager(AuthorizationManager<Object> eventAuthorizationManager) {
		this.eventAuthorizationManager = eventAuthorizationManager;
	}

	public void setTransitionAuthorizationManager(AuthorizationManager<Object> transitionAuthorizationManager) {
		this.transitionAuthorizationManager = transitionAuthorizationManager;
	}

	public void setEventSecurityRule(SecurityRule eventSecurityRule) {
		this.eventSecurityRule = eventSecurityRule;
	}

	@SuppressWarnings("unchecked")
	private AuthorizationManager<Object> buildEventManager(SecurityRule rule) {
		List<AuthorizationManager<Object>> managers = new ArrayList<>();

		if (rule.getAttributes() != null) {
			for (String attr : rule.getAttributes()) {
				if (attr.startsWith("EVENT_")) {
					String eventName = attr.substring("EVENT_".length());
					managers.add((authSupplier, obj) -> {
						Message<E> msg = (Message<E>) obj;
						return new AuthorizationDecision(eventName.equals(String.valueOf(msg.getPayload())));
					});
				} else {
					managers.add(AuthorityAuthorizationManager.<Object>hasAuthority(attr)::authorize);
				}
			}
		}

		if (StringUtils.hasText(rule.getExpression())) {
			Expression expr = expressionParser.parseExpression(rule.getExpression());
			DefaultEventSecurityExpressionHandler<E> handler = new DefaultEventSecurityExpressionHandler<>();
			managers.add((authSupplier, obj) -> {
				Message<E> msg = (Message<E>) obj;
				EvaluationContext ctx = handler.createEvaluationContext(authSupplier.get(), msg);
				return new AuthorizationDecision(ExpressionUtils.evaluateAsBoolean(expr, ctx));
			});
		}

		return compose(managers, rule.getComparisonType());
	}

	@SuppressWarnings("unchecked")
	private AuthorizationManager<Object> buildTransitionManager(SecurityRule rule) {
		List<AuthorizationManager<Object>> managers = new ArrayList<>();

		if (rule.getAttributes() != null) {
			for (String attr : rule.getAttributes()) {
				if (attr.startsWith("TRANSITION_SOURCE_")) {
					String source = attr.substring("TRANSITION_SOURCE_".length());
					managers.add((authSupplier, obj) -> {
						Transition<S, E> tr = (Transition<S, E>) obj;
						return new AuthorizationDecision(source.equals(String.valueOf(tr.getSource().getId())));
					});
				} else if (attr.startsWith("TRANSITION_TARGET_")) {
					String target = attr.substring("TRANSITION_TARGET_".length());
					managers.add((authSupplier, obj) -> {
						Transition<S, E> tr = (Transition<S, E>) obj;
						return new AuthorizationDecision(target.equals(String.valueOf(tr.getTarget().getId())));
					});
				} else {
					managers.add(AuthorityAuthorizationManager.<Object>hasAuthority(attr)::authorize);
				}
			}
		}

		if (StringUtils.hasText(rule.getExpression())) {
			Expression expr = expressionParser.parseExpression(rule.getExpression());
			DefaultTransitionSecurityExpressionHandler handler = new DefaultTransitionSecurityExpressionHandler();
			managers.add((authSupplier, obj) -> {
				Transition<?, ?> tr = (Transition<?, ?>) obj;
				EvaluationContext ctx = handler.createEvaluationContext(authSupplier.get(), tr);
				return new AuthorizationDecision(ExpressionUtils.evaluateAsBoolean(expr, ctx));
			});
		}

		return compose(managers, rule.getComparisonType());
	}

	@SuppressWarnings("unchecked")
	private static <T> AuthorizationManager<T> compose(List<AuthorizationManager<T>> managers,
			SecurityRule.ComparisonType type) {
		if (managers.isEmpty()) {
			return (authSupplier, obj) -> new AuthorizationDecision(false);
		}
		AuthorizationManager<T>[] arr = managers.toArray(new AuthorizationManager[0]);
		return switch (type) {
			case ANY -> AuthorizationManagers.anyOf(arr);
			case ALL -> AuthorizationManagers.allOf(arr);
			case MAJORITY -> (authSupplier, obj) -> {
				int grant = 0, deny = 0;
				for (AuthorizationManager<T> m : managers) {
					AuthorizationResult r = m.authorize(authSupplier, obj);
					if (r != null && r.isGranted()) grant++;
					else if (r != null) deny++;
				}
				return new AuthorizationDecision(grant > deny);
			};
		};
	}

	@Override
	public String toString() {
		return "StateMachineSecurityInterceptor [transitionAuthorizationManager=" + transitionAuthorizationManager
				+ ", eventAuthorizationManager=" + eventAuthorizationManager
				+ ", eventSecurityRule=" + eventSecurityRule + "]";
	}
}
