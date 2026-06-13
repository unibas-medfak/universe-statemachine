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
package ch.unibas.medizin.universe.statemachine.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.Message;
import ch.unibas.medizin.universe.statemachine.ExtendedState;
import ch.unibas.medizin.universe.statemachine.StateContext;
import ch.unibas.medizin.universe.statemachine.StateMachine;
import ch.unibas.medizin.universe.statemachine.action.Action;
import ch.unibas.medizin.universe.statemachine.action.Actions;
import ch.unibas.medizin.universe.statemachine.config.model.ChoiceData;
import ch.unibas.medizin.universe.statemachine.config.model.DefaultStateMachineModel;
import ch.unibas.medizin.universe.statemachine.config.model.EntryData;
import ch.unibas.medizin.universe.statemachine.config.model.ExitData;
import ch.unibas.medizin.universe.statemachine.config.model.HistoryData;
import ch.unibas.medizin.universe.statemachine.config.model.JunctionData;
import ch.unibas.medizin.universe.statemachine.config.model.MalformedConfigurationException;
import ch.unibas.medizin.universe.statemachine.config.model.StateData;
import ch.unibas.medizin.universe.statemachine.config.model.StateMachineModel;
import ch.unibas.medizin.universe.statemachine.config.model.StateMachineModelFactory;
import ch.unibas.medizin.universe.statemachine.config.model.TransitionData;
import ch.unibas.medizin.universe.statemachine.config.model.TransitionsData;
import ch.unibas.medizin.universe.statemachine.config.model.verifier.CompositeStateMachineModelVerifier;
import ch.unibas.medizin.universe.statemachine.config.model.verifier.StateMachineModelVerifier;
import ch.unibas.medizin.universe.statemachine.ensemble.DistributedStateMachine;
import ch.unibas.medizin.universe.statemachine.listener.StateMachineListener;
import ch.unibas.medizin.universe.statemachine.listener.StateMachineListenerAdapter;
import ch.unibas.medizin.universe.statemachine.monitor.StateMachineMonitor;
import ch.unibas.medizin.universe.statemachine.region.Region;
import ch.unibas.medizin.universe.statemachine.security.StateMachineSecurityInterceptor;
import ch.unibas.medizin.universe.statemachine.state.AbstractState;
import ch.unibas.medizin.universe.statemachine.state.ChoicePseudoState;
import ch.unibas.medizin.universe.statemachine.state.ChoicePseudoState.ChoiceStateData;
import ch.unibas.medizin.universe.statemachine.state.DefaultPseudoState;
import ch.unibas.medizin.universe.statemachine.state.EntryPseudoState;
import ch.unibas.medizin.universe.statemachine.state.ExitPseudoState;
import ch.unibas.medizin.universe.statemachine.state.ForkPseudoState;
import ch.unibas.medizin.universe.statemachine.state.HistoryPseudoState;
import ch.unibas.medizin.universe.statemachine.state.JoinPseudoState;
import ch.unibas.medizin.universe.statemachine.state.JoinPseudoState.JoinStateData;
import ch.unibas.medizin.universe.statemachine.state.JunctionPseudoState;
import ch.unibas.medizin.universe.statemachine.state.JunctionPseudoState.JunctionStateData;
import ch.unibas.medizin.universe.statemachine.state.PseudoState;
import ch.unibas.medizin.universe.statemachine.state.PseudoStateKind;
import ch.unibas.medizin.universe.statemachine.state.RegionState;
import ch.unibas.medizin.universe.statemachine.state.State;
import ch.unibas.medizin.universe.statemachine.state.StateHolder;
import ch.unibas.medizin.universe.statemachine.state.StateMachineState;
import ch.unibas.medizin.universe.statemachine.support.DefaultExtendedState;
import ch.unibas.medizin.universe.statemachine.support.LifecycleObjectSupport;
import ch.unibas.medizin.universe.statemachine.support.StateMachineInterceptor;
import ch.unibas.medizin.universe.statemachine.support.StateMachineInterceptorAdapter;
import ch.unibas.medizin.universe.statemachine.support.tree.Tree;
import ch.unibas.medizin.universe.statemachine.support.tree.Tree.Node;
import ch.unibas.medizin.universe.statemachine.support.tree.TreeTraverser;
import ch.unibas.medizin.universe.statemachine.transition.DefaultExternalTransition;
import ch.unibas.medizin.universe.statemachine.transition.DefaultInternalTransition;
import ch.unibas.medizin.universe.statemachine.transition.DefaultLocalTransition;
import ch.unibas.medizin.universe.statemachine.transition.InitialTransition;
import ch.unibas.medizin.universe.statemachine.transition.Transition;
import ch.unibas.medizin.universe.statemachine.transition.TransitionKind;
import ch.unibas.medizin.universe.statemachine.trigger.EventTrigger;
import ch.unibas.medizin.universe.statemachine.trigger.TimerTrigger;
import ch.unibas.medizin.universe.statemachine.trigger.Trigger;
import org.springframework.util.ObjectUtils;

import reactor.core.publisher.Mono;

/**
 * Base {@link StateMachineFactory} implementation building {@link StateMachine}s.
 *
 * @author Janne Valkealahti
 *
 * @param <S> the type of state
 * @param <E> the type of event
 */
public abstract class AbstractStateMachineFactory<S, E> extends LifecycleObjectSupport implements
		StateMachineFactory<S, E>, BeanNameAware {

	private final Log log = LogFactory.getLog(AbstractStateMachineFactory.class);

	private final StateMachineModel<S, E> defaultStateMachineModel;

	private final StateMachineModelFactory<S, E> stateMachineModelFactory;

	private Boolean contextEvents;

	private boolean handleAutostartup = false;

	private String beanName;

	private StateMachineMonitor<S, E> defaultStateMachineMonitor;

	/**
	 * Instantiates a new abstract state machine factory.
	 *
	 * @param defaultStateMachineModel the default state machine model
	 * @param stateMachineModelFactory the state machine model factory
	 */
	public AbstractStateMachineFactory(StateMachineModel<S, E> defaultStateMachineModel, StateMachineModelFactory<S, E> stateMachineModelFactory) {
		this.stateMachineModelFactory = stateMachineModelFactory;
		this.defaultStateMachineModel = defaultStateMachineModel;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	@Override
	public StateMachine<S, E> getStateMachine() {
		return getStateMachine(null, null);
	}

	@Override
	public StateMachine<S, E> getStateMachine(String machineId) {
		return getStateMachine(null, machineId);
	}

	@Override
	public StateMachine<S, E> getStateMachine(UUID uuid) {
		return getStateMachine(uuid, null);
	}

	/**
	 * Main constructor that create a {@link StateMachine}.
	 *
	 * @param uuid for internal usage. Can be null, in that case a random one will be generated.
	 * @param machineId represent a user Id, up to you to set what you want.
	 * @return a {@link StateMachine}
	 */
	@SuppressWarnings("unchecked")
	public StateMachine<S, E> getStateMachine(UUID uuid, String machineId) {
		ArrayList<StateMachine<S, E>> machines = new ArrayList<>();

		StateMachineModel<S, E> stateMachineModel = resolveStateMachineModel(machineId);
		if (stateMachineModel.getConfigurationData().isVerifierEnabled()) {
			StateMachineModelVerifier<S, E> verifier = stateMachineModel.getConfigurationData().getVerifier();
			if (verifier == null) {
				verifier = new CompositeStateMachineModelVerifier<>();
			}
			verifier.verify(stateMachineModel);
		}

		// shared
		DefaultExtendedState defaultExtendedState = new DefaultExtendedState();

		StateMachine<S, E> machine = null;

		// we store mappings from state id's to states which gets
		// created during the process. This is needed for transitions to
		// find a correct mappings because they use state id's, not actual
		// states.
		final Map<S, State<S, E>> stateMap = new HashMap<>();
		Deque<MachineStackItem<S, E>> regionStack = new ArrayDeque<>();
		Deque<StateData<S, E>> stateStack = new ArrayDeque<>();
		Map<Object, StateMachine<S, E>> machineMap = new HashMap<>();
		List<HolderListItem<S, E>> holderList = new ArrayList<>();

		Iterator<Node<StateData<S, E>>> iterator = buildStateDataIterator(stateMachineModel);
		while (iterator.hasNext()) {
			Node<StateData<S, E>> node = iterator.next();
			StateData<S, E> stateData = node.getData();
			StateData<S, E> peek = stateStack.isEmpty() ? null : stateStack.peekFirst();

			// simply push and continue
			if (stateStack.isEmpty()) {
				stateStack.addFirst(stateData);
				continue;
			}

			boolean stackContainsSameParent = false;
            for (StateData<S, E> sd : stateStack) {
                if (stateData != null && ObjectUtils.nullSafeEquals(stateData.getState(), sd.getParent())) {
                    stackContainsSameParent = true;
                    break;
                }
            }

			if (stateData != null && !stackContainsSameParent) {
				stateStack.addFirst(stateData);
				continue;
			}

			Collection<StateData<S, E>> stateDatas = popSameParents(stateStack);
			int initialCount = getInitialCount(stateDatas);
			Collection<Collection<StateData<S, E>>> regionsStateDatas = splitIntoRegions(stateDatas);
			Collection<TransitionData<S, E>> transitionsData = getTransitionData(iterator.hasNext(), stateDatas, stateMachineModel);

			if (initialCount > 1) {
				for (Collection<StateData<S, E>> regionStateDatas : regionsStateDatas) {
					// try to build reqion id's
					Object rId = regionStateDatas.iterator().next().getRegion();
					String mId = machineId != null ? machineId : stateMachineModel.getConfigurationData().getMachineId();
					mId = mId + "#" + (rId != null ? rId.toString() : "");

					machine = buildMachine(machineMap, stateMap, holderList, regionStateDatas, transitionsData,
							resolveBeanFactory(stateMachineModel), contextEvents, defaultExtendedState,
							stateMachineModel.getTransitionsData(), mId, null, stateMachineModel);
					regionStack.addFirst(new MachineStackItem<>(machine));
					machines.add(machine);
				}

				Collection<Region<S, E>> regions = new ArrayList<>();
				while (!regionStack.isEmpty()) {
					MachineStackItem<S, E> pop = regionStack.removeFirst();
					regions.add(pop.machine);
				}
				S parent = (S)peek.getParent();
				RegionState<S, E> rstate = buildRegionStateInternal(parent, regions, null,
						stateData != null ? stateData.getEntryActions() : null,
						stateData != null ? stateData.getExitActions() : null,
                        new DefaultPseudoState<>(PseudoStateKind.INITIAL), stateMachineModel);
				rstate.setRegionExecutionPolicy(stateMachineModel.getConfigurationData().getRegionExecutionPolicy());
				if (stateData != null) {
					stateMap.put(stateData.getState(), rstate);
				} else {
					// TODO: don't like that we create a last machine here
					Collection<State<S, E>> states = new ArrayList<>();
					states.add(rstate);
					Transition<S, E> initialTransition = new InitialTransition<>(rstate);
					StateMachine<S, E> m = buildStateMachineInternal(states, new ArrayList<>(), rstate, initialTransition,
							null, defaultExtendedState, null, contextEvents, resolveBeanFactory(stateMachineModel), beanName,
							machineId != null ? machineId : stateMachineModel.getConfigurationData().getMachineId(),
							uuid, stateMachineModel);
					machine = m;
					machines.add(m);
				}
			} else {
				machine = buildMachine(machineMap, stateMap, holderList, stateDatas, transitionsData,
						resolveBeanFactory(stateMachineModel), contextEvents, defaultExtendedState,
						stateMachineModel.getTransitionsData(), machineId, uuid, stateMachineModel);
				machines.add(machine);
				if (peek.isInitial() || (!peek.isInitial() && !machineMap.containsKey(peek.getParent()))) {
					machineMap.put(peek.getParent(), machine);
				}
			}

			if (stateData != null) {
				stateStack.addFirst(stateData);
			}
		}

		// setup autostart for top-level machine
		if (machine instanceof LifecycleObjectSupport) {
			((LifecycleObjectSupport)machine).setAutoStartup(stateMachineModel.getConfigurationData().isAutoStart());
		}

		// set top-level machine as relay
		final StateMachine<S, E> fmachine = machine;
		fmachine.getStateMachineAccessor().doWithAllRegions(function -> function.setRelay(fmachine));

		// add monitoring hooks
		final StateMachineMonitor<S, E> stateMachineMonitor = stateMachineModel.getConfigurationData().getStateMachineMonitor();
		if (stateMachineMonitor != null || defaultStateMachineMonitor != null) {
			fmachine.getStateMachineAccessor().doWithRegion(function -> {
				if (defaultStateMachineMonitor != null) {
					function.addStateMachineMonitor(defaultStateMachineMonitor);
				}
				if (stateMachineMonitor != null) {
					function.addStateMachineMonitor(stateMachineMonitor);
				}
			});
		}

		// set parent machines for each built machine
		for (Entry<Object, StateMachine<S, E>> mme : machineMap.entrySet()) {
			StateMachine<S, E> m = null;
			if (mme.getKey() != null) {
				Object sParent = null;
				for (StateData<S, E> sd : stateMachineModel.getStatesData().getStateData()) {
					if (ObjectUtils.nullSafeEquals(sd.getState(), mme.getKey())) {
						sParent = sd.getParent();
						break;
					}
				}
				m = machineMap.get(sParent);
			}
			final StateMachine<S, E> mm = m;
			mme.getValue().getStateMachineAccessor().doWithRegion(function -> function.setParentMachine(mm));
		}

		// init built machines
		for (StateMachine<S, E> m : machines) {
			((LifecycleObjectSupport)m).afterPropertiesSet();
		}


		// TODO: should error out if sec is enabled but spring-security is not in cp
		if (stateMachineModel.getConfigurationData().isSecurityEnabled()) {
			final StateMachineSecurityInterceptor<S, E> securityInterceptor = new StateMachineSecurityInterceptor<>(
                    stateMachineModel.getConfigurationData().getTransitionSecurityAccessDecisionManager(),
                    stateMachineModel.getConfigurationData().getEventSecurityAccessDecisionManager(),
                    stateMachineModel.getConfigurationData().getEventSecurityRule());
			log.info("Adding security interceptor " + securityInterceptor);
			fmachine.getStateMachineAccessor()
					.doWithAllRegions(function -> function.addStateMachineInterceptor(securityInterceptor));
		}

		// setup distributed state machine if needed.
		// we wrap previously build machine with a distributed
		// state machine and set it to use given ensemble.
		if (stateMachineModel.getConfigurationData().getStateMachineEnsemble() != null) {
			DistributedStateMachine<S, E> distributedStateMachine = new DistributedStateMachine<>(
                    stateMachineModel.getConfigurationData().getStateMachineEnsemble(), machine);
			distributedStateMachine.setAutoStartup(stateMachineModel.getConfigurationData().isAutoStart());
			distributedStateMachine.afterPropertiesSet();
			machine = distributedStateMachine;
		}

		for (StateMachineListener<S, E> listener : stateMachineModel.getConfigurationData().getStateMachineListeners()) {
			machine.addStateListener(listener);
		}

		List<StateMachineInterceptor<S,E>> interceptors = stateMachineModel.getConfigurationData().getStateMachineInterceptors();
		if (interceptors != null) {

			for (final StateMachineInterceptor<S, E> interceptor : interceptors) {
				// add persisting interceptor hooks to all regions
				RegionPersistingInterceptorAdapter<S, E> adapter = new RegionPersistingInterceptorAdapter<>(interceptor);
				machine.getStateMachineAccessor().doWithAllRegions(function -> function.addStateMachineInterceptor(adapter));
			}
		}

		// go through holders and fix state references which
		// were not known at a time holder was created
		for (HolderListItem<S, E> holderItem : holderList) {
			holderItem.value.setState(stateMap.get(holderItem.key));
		}

		return delegateAutoStartup(machine);
	}

	private static class RegionPersistingInterceptorAdapter<S, E> extends StateMachineInterceptorAdapter<S, E> {

		private final StateMachineInterceptor<S, E> interceptor;

		private RegionPersistingInterceptorAdapter(StateMachineInterceptor<S, E> interceptor) {
			this.interceptor = interceptor;
		}

        @Override
        public Message<E> preEvent(Message<E> message, StateMachine<S, E> stateMachine) {
            return interceptor.preEvent(message, stateMachine);
        }

        @Override
        public StateContext<S, E> preTransition(StateContext<S, E> stateContext) {
            return interceptor.preTransition(stateContext);
        }

        @Override
        public StateContext<S, E> postTransition(StateContext<S, E> stateContext) {
            return interceptor.postTransition(stateContext);
        }

        @Override
        public Exception stateMachineError(StateMachine<S, E> stateMachine, Exception exception) {
            return interceptor.stateMachineError(stateMachine, exception);
        }

		@Override
		public void preStateChange(State<S, E> state, Message<E> message, Transition<S, E> transition,
				StateMachine<S, E> stateMachine, StateMachine<S, E> rootStateMachine) {
			interceptor.preStateChange(state, message, transition, stateMachine, rootStateMachine);
		}

		@Override
		public void postStateChange(State<S, E> state, Message<E> message, Transition<S, E> transition,
				StateMachine<S, E> stateMachine, StateMachine<S, E> rootStateMachine) {
			interceptor.postStateChange(state, message, transition, stateMachine, rootStateMachine);
		}

	}

	/**
	 * Instructs this factory to handle auto-start flag manually
	 * by calling lifecycle start method.
	 *
	 * @param handleAutostartup the new handle autostartup
	 */
	public void setHandleAutostartup(boolean handleAutostartup) {
		this.handleAutostartup = handleAutostartup;
	}

	/**
	 * Instructs this factory to enable application context events.
	 *
	 * @param contextEvents the new context events enabled
	 */
	public void setContextEventsEnabled(Boolean contextEvents) {
		this.contextEvents = contextEvents;
	}

	/**
	 * Set state machine monitor.
	 *
	 * @param stateMachineMonitor the state machine monitor
	 */
	public void setStateMachineMonitor(StateMachineMonitor<S, E> stateMachineMonitor) {
		this.defaultStateMachineMonitor = stateMachineMonitor;
	}

	private StateMachine<S, E> delegateAutoStartup(StateMachine<S, E> delegate) {
		if (handleAutostartup && delegate instanceof SmartLifecycle smartLifecycle && smartLifecycle.isAutoStartup()) {
			AutostartListener<S, E> autostartListener = new AutostartListener<>();
			delegate.addStateListener(autostartListener);
			smartLifecycle.start();
			try {
				autostartListener.latch.await(30, TimeUnit.SECONDS);
			} catch (Exception e) {
				log.warn("Waited 30 seconds for machine to start as autostart was requested, machine may not be ready");
			} finally {
				delegate.removeStateListener(autostartListener);
			}
		}
		return delegate;
	}

	protected BeanFactory resolveBeanFactory(StateMachineModel<S, E> stateMachineModel) {
		if (stateMachineModel.getConfigurationData().getBeanFactory() != null) {
			return stateMachineModel.getConfigurationData().getBeanFactory();
		} else {
			return getBeanFactory();
		}
	}

	protected StateMachineModel<S, E> resolveStateMachineModel(String machineId) {
		if (stateMachineModelFactory == null) {
			return defaultStateMachineModel;
		} else {
			StateMachineModel<S, E> m = stateMachineModelFactory.build(machineId);
			if (m.getConfigurationData() == null) {
				// if model doesn't have explicit configuration data,
				// get it from default model
				return new DefaultStateMachineModel<>(defaultStateMachineModel.getConfigurationData(), m.getStatesData(),
						m.getTransitionsData());
			} else {
				return m;
			}
		}
	}

	private int getInitialCount(Collection<StateData<S, E>> stateDatas) {
		int count = 0;
		for (StateData<S, E> stateData : stateDatas) {
			if (stateData.isInitial()) {
				count++;
			}
		}
		return count;
	}

	private Collection<Collection<StateData<S, E>>> splitIntoRegions(Collection<StateData<S, E>> stateDatas) {
		Map<Object, Collection<StateData<S, E>>> map = new HashMap<>();
		for (StateData<S, E> stateData : stateDatas) {
			Collection<StateData<S, E>> c = map.get(stateData.getRegion());
			if (c == null) {
				c = new ArrayList<>();
			}
			c.add(stateData);
			map.put(stateData.getRegion(), c);
		}
		return map.values();
	}

	private Collection<TransitionData<S, E>> getTransitionData(boolean roots, Collection<StateData<S, E>> stateDatas, StateMachineModel<S, E> stateMachineModel) {
		if (roots) {
			return resolveTransitionData(stateMachineModel.getTransitionsData().getTransitions(), stateDatas);
		} else {
			return resolveTransitionData2(stateMachineModel.getTransitionsData().getTransitions());
		}
	}

	private static <S, E> Collection<StateData<S, E>> popSameParents(Deque<StateData<S, E>> stack) {
		Collection<StateData<S, E>> data = new ArrayList<>();
		Object parent = null;
		if (!stack.isEmpty()) {
			parent = stack.peekFirst().getParent();
		}
		while (!stack.isEmpty() && ObjectUtils.nullSafeEquals(parent, stack.peekFirst().getParent())) {
			data.add(stack.removeFirst());
		}
		return data;
	}


	private static class MachineStackItem<S, E> {

		final StateMachine<S, E> machine;

		private MachineStackItem(StateMachine<S, E> machine) {
			this.machine = machine;
		}

	}

	private Collection<TransitionData<S, E>> resolveTransitionData(Collection<TransitionData<S, E>> in, Collection<StateData<S, E>> stateDatas) {
		ArrayList<TransitionData<S, E>> out = new ArrayList<>();

		Collection<Object> states = new ArrayList<>();
		for (StateData<S, E> stateData : stateDatas) {
			states.add(stateData.getParent());
		}

		for (TransitionData<S, E> transitionData : in) {
			S state = transitionData.getState();
			if (state != null && states.contains(state)) {
				out.add(transitionData);
			}
		}

		return out;
	}

	private Collection<TransitionData<S, E>> resolveTransitionData2(Collection<TransitionData<S, E>> in) {
		ArrayList<TransitionData<S, E>> out = new ArrayList<>();
		for (TransitionData<S, E> transitionData : in) {
			if (transitionData.getState() == null) {
				out.add(transitionData);
			}
		}
		return out;
	}


	@SuppressWarnings("unchecked")
	private StateMachine<S, E> buildMachine(Map<Object, StateMachine<S, E>> machineMap, Map<S, State<S, E>> stateMap,
			List<HolderListItem<S, E>> holderList, Collection<StateData<S, E>> stateDatas,
			Collection<TransitionData<S, E>> transitionsData, BeanFactory beanFactory, Boolean contextEvents,
			DefaultExtendedState defaultExtendedState, TransitionsData<S, E> stateMachineTransitions, String machineId,
			UUID uuid, StateMachineModel<S, E> stateMachineModel) {
		State<S, E> state;
		State<S, E> initialState = null;
		PseudoState<S, E> historyState = null;
		Action<S, E> initialAction = null;
		Collection<State<S, E>> states = new ArrayList<>();

		// for now loop twice and build states for
		// non initial/end pseudostates last

		for (StateData<S, E> stateData : stateDatas) {
			StateMachine<S, E> stateMachine = machineMap.get(stateData.getState());
			if (stateMachine == null) {
				// get a submachine from state data if we didn't have
				// it already. stays null if we don't have one.
				stateMachine = stateData.getSubmachine();
				if (stateMachine == null && stateData.getSubmachineFactory() != null) {
					stateMachine = stateData.getSubmachineFactory().getStateMachine(machineId);
				}
			}
			state = stateMap.get(stateData.getState());
			if (state != null) {
				states.add(state);
				if (stateData.isInitial()) {
					initialState = state;
				}
				continue;
			}
			if (stateMachine != null) {
				PseudoState<S, E> pseudoState = null;
				if (stateData.isInitial()) {
					pseudoState = new DefaultPseudoState<>(PseudoStateKind.INITIAL);
				}
				StateMachineState<S, E> stateMachineState = new StateMachineState<>(stateData.getState(),
                        stateMachine, stateData.getDeferred(), stateData.getEntryActions(), stateData.getExitActions(),
                        pseudoState);
				stateMachineState
						.setStateDoActionPolicy(stateMachineModel.getConfigurationData().getStateDoActionPolicy());
				stateMachineState.setStateDoActionPolicyTimeout(
						stateMachineModel.getConfigurationData().getStateDoActionPolicyTimeout());
				state = stateMachineState;

				// TODO: below if/else doesn't feel right
				if (stateDatas.size() > 1 && stateData.isInitial()) {
					initialState = state;
					initialAction = stateData.getInitialAction();
				} else if (stateDatas.size() == 1) {
					initialState = state;
					initialAction = stateData.getInitialAction();
				}
				states.add(state);
			} else {
				PseudoState<S, E> pseudoState = null;
				if (stateData.isInitial()) {
					pseudoState = new DefaultPseudoState<>(PseudoStateKind.INITIAL);
				} else if (stateData.isEnd()) {
					pseudoState = new DefaultPseudoState<>(PseudoStateKind.END);
				} else if (stateData.getPseudoStateKind() == PseudoStateKind.HISTORY_SHALLOW) {
					continue;
				} else if (stateData.getPseudoStateKind() == PseudoStateKind.HISTORY_DEEP) {
					continue;
				} else if (stateData.getPseudoStateKind() == PseudoStateKind.JOIN) {
					continue;
				} else if (stateData.getPseudoStateKind() == PseudoStateKind.FORK) {
					continue;
				} else if (stateData.getPseudoStateKind() == PseudoStateKind.CHOICE) {
					continue;
				} else if (stateData.getPseudoStateKind() == PseudoStateKind.JUNCTION) {
					continue;
				} else if (stateData.getPseudoStateKind() == PseudoStateKind.ENTRY) {
					continue;
				} else if (stateData.getPseudoStateKind() == PseudoStateKind.EXIT) {
					continue;
				}
				state = buildStateInternal(stateData.getState(), stateData.getDeferred(), stateData.getEntryActions(),
						stateData.getExitActions(), stateData.getStateActions(), pseudoState, stateMachineModel);
				if (stateData.isInitial()) {
					initialState = state;
					initialAction = stateData.getInitialAction();
				}
				states.add(state);

			}
			stateMap.put(stateData.getState(), state);
		}

		for (StateData<S, E> stateData : stateDatas) {
			if (stateData.getPseudoStateKind() == PseudoStateKind.HISTORY_SHALLOW) {
				State<S, E> defaultState = null;
				S s = stateData.getState();
				Collection<HistoryData<S,E>> historys = stateMachineTransitions.getHistorys();
				if (historys == null) {
					throw new MalformedConfigurationException("No transitions for state " + s);
				}
				for (HistoryData<S,E> history : historys) {
					if (history.getSource().equals(s)) {
						defaultState = stateMap.get(history.getTarget());
					}
				}
				StateHolder<S, E> defaultStateHolder = new StateHolder<>(defaultState);
				StateHolder<S, E> containingStateHolder = new StateHolder<>(stateMap.get(stateData.getParent()));
				if (containingStateHolder.getState() == null) {
					holderList.add(new HolderListItem<>((S) stateData.getParent(), containingStateHolder));
				}
				PseudoState<S, E> pseudoState = new HistoryPseudoState<>(PseudoStateKind.HISTORY_SHALLOW, defaultStateHolder, containingStateHolder);
				state = buildStateInternal(stateData.getState(), stateData.getDeferred(), stateData.getEntryActions(),
						stateData.getExitActions(), stateData.getStateActions(), pseudoState, stateMachineModel);
				states.add(state);
				stateMap.put(stateData.getState(), state);
				historyState = pseudoState;
			} else if (stateData.getPseudoStateKind() == PseudoStateKind.HISTORY_DEEP) {
				State<S, E> defaultState = null;
				S s = stateData.getState();
				Collection<HistoryData<S,E>> historys = stateMachineTransitions.getHistorys();
				if (historys == null) {
					throw new MalformedConfigurationException("No transitions for state " + s);
				}
				for (HistoryData<S,E> history : historys) {
					if (history.getSource().equals(s)) {
						defaultState = stateMap.get(history.getTarget());
					}
				}
				StateHolder<S, E> defaultStateHolder = new StateHolder<>(defaultState);
				StateHolder<S, E> containingStateHolder = new StateHolder<>(stateMap.get(stateData.getParent()));
				if (containingStateHolder.getState() == null) {
					holderList.add(new HolderListItem<>((S) stateData.getParent(), containingStateHolder));
				}
				PseudoState<S, E> pseudoState = new HistoryPseudoState<>(PseudoStateKind.HISTORY_DEEP, defaultStateHolder, containingStateHolder);
				state = buildStateInternal(stateData.getState(), stateData.getDeferred(), stateData.getEntryActions(),
						stateData.getExitActions(), stateData.getStateActions(), pseudoState, stateMachineModel);
				states.add(state);
				stateMap.put(stateData.getState(), state);
				historyState = pseudoState;
			}

			if (stateData.getPseudoStateKind() == PseudoStateKind.CHOICE) {
				S s = stateData.getState();
				List<ChoiceData<S, E>> list = stateMachineTransitions.getChoices().get(s);
				if (list == null) {
					throw new MalformedConfigurationException("No transitions for state " + s);
				}
				List<ChoiceStateData<S, E>> choices = new ArrayList<>();
				for (ChoiceData<S, E> c : list) {
					StateHolder<S, E> holder = new StateHolder<>(stateMap.get(c.getTarget()));
					if (holder.getState() == null) {
						holderList.add(new HolderListItem<>(c.getTarget(), holder));
					}
					choices.add(new ChoiceStateData<>(holder, c.getGuard(), Actions.from(c.getActions())));
				}
				PseudoState<S, E> pseudoState = new ChoicePseudoState<>(choices);
				state = buildStateInternal(stateData.getState(), stateData.getDeferred(), stateData.getEntryActions(),
						stateData.getExitActions(), stateData.getStateActions(), pseudoState, stateMachineModel);
				states.add(state);
				stateMap.put(stateData.getState(), state);
			} else if (stateData.getPseudoStateKind() == PseudoStateKind.JUNCTION) {
				S s = stateData.getState();
				List<JunctionData<S, E>> list = stateMachineTransitions.getJunctions().get(s);
				List<JunctionStateData<S, E>> junctions = new ArrayList<>();
				if (list == null) {
					throw new MalformedConfigurationException("No transitions for state " + s);
				}
				for (JunctionData<S, E> c : list) {
					StateHolder<S, E> holder = new StateHolder<>(stateMap.get(c.getTarget()));
					if (holder.getState() == null) {
						holderList.add(new HolderListItem<>(c.getTarget(), holder));
					}
					junctions.add(new JunctionStateData<>(holder, c.getGuard(), Actions.from(c.getActions())));
				}
				PseudoState<S, E> pseudoState = new JunctionPseudoState<>(junctions);
				state = buildStateInternal(stateData.getState(), stateData.getDeferred(), stateData.getEntryActions(),
						stateData.getExitActions(), stateData.getStateActions(), pseudoState, stateMachineModel);
				states.add(state);
				stateMap.put(stateData.getState(), state);
			} else if (stateData.getPseudoStateKind() == PseudoStateKind.ENTRY) {
				S s = stateData.getState();
				Collection<EntryData<S, E>> entrys = stateMachineTransitions.getEntrys();
				if (entrys == null) {
					throw new MalformedConfigurationException("No transitions for state " + s);
				}
				for (EntryData<S, E> entry : entrys) {
					if (s.equals(entry.getSource())) {
						PseudoState<S, E> pseudoState = new EntryPseudoState<>(stateMap.get(entry.getTarget()));
						state = buildStateInternal(stateData.getState(), stateData.getDeferred(), stateData.getEntryActions(),
								stateData.getExitActions(), stateData.getStateActions(), pseudoState, stateMachineModel);
						states.add(state);
						stateMap.put(stateData.getState(), state);
						break;
					}
				}
			} else if (stateData.getPseudoStateKind() == PseudoStateKind.EXIT) {
				S s = stateData.getState();
				Collection<ExitData<S, E>> exits = stateMachineTransitions.getExits();
				if (exits == null) {
					throw new MalformedConfigurationException("No transitions for state " + s);
				}
				for (ExitData<S, E> entry : exits) {
					if (s.equals(entry.getSource())) {
						StateHolder<S, E> holder = new StateHolder<>(stateMap.get(entry.getTarget()));
						if (holder.getState() == null) {
							holderList.add(new HolderListItem<>(entry.getTarget(), holder));
						}
						PseudoState<S, E> pseudoState = new ExitPseudoState<>(holder);
						state = buildStateInternal(stateData.getState(), stateData.getDeferred(), stateData.getEntryActions(),
								stateData.getExitActions(), stateData.getStateActions(), pseudoState, stateMachineModel);
						states.add(state);
						stateMap.put(stateData.getState(), state);
						break;
					}
				}
			} else if (stateData.getPseudoStateKind() == PseudoStateKind.FORK) {
				S s = stateData.getState();
				List<S> list = stateMachineTransitions.getForks().get(s);
				List<State<S, E>> forks = new ArrayList<>();
				if (list == null) {
					throw new MalformedConfigurationException("No transitions for state " + s);
				}
				for (S fs : list) {
					forks.add(stateMap.get(fs));
				}
				PseudoState<S, E> pseudoState = new ForkPseudoState<>(forks);
				state = buildStateInternal(stateData.getState(), stateData.getDeferred(), stateData.getEntryActions(),
						stateData.getExitActions(), stateData.getStateActions(), pseudoState, stateMachineModel);
				states.add(state);
				stateMap.put(stateData.getState(), state);
			} else if (stateData.getPseudoStateKind() == PseudoStateKind.JOIN) {
				S s = stateData.getState();
				List<S> list = stateMachineTransitions.getJoins().get(s);
				if (list == null) {
					throw new MalformedConfigurationException("No transitions for state " + s);
				}
				List<List<State<S, E>>> joins = new ArrayList<>();

				// if join source is an orthogonal state, assume we're joining by region end states
				// and actually use list of states from each region to support case where one region
				// defines multiple end states.
				if (list.size() == 1) {
					State<S, E> ss1 = stateMap.get(list.getFirst());
					if (ss1 instanceof RegionState) {
						Collection<Region<S, E>> regions = ((RegionState<S, E>)ss1).getRegions();
						for (Region<S, E> r : regions) {
							List<State<S, E>> j = new ArrayList<>();
							Collection<State<S, E>> ss2 = r.getStates();
							for (State<S, E> ss3 : ss2) {
								if (ss3.getPseudoState() != null && ss3.getPseudoState().getKind() == PseudoStateKind.END) {
									j.add(ss3);
								}
							}
							joins.add(j);
						}
					}
				} else {
					for (S fs : list) {
						joins.add(Collections.singletonList(stateMap.get(fs)));
					}
				}

				List<JoinStateData<S, E>> joinTargets = new ArrayList<>();
				Collection<TransitionData<S, E>> transitions = stateMachineTransitions.getTransitions();
				for (TransitionData<S, E> tt : transitions) {
					if (Objects.equals(tt.getSource(), s)) {
						StateHolder<S, E> holder = new StateHolder<>(stateMap.get(tt.getTarget()));
						if (holder.getState() == null) {
							holderList.add(new HolderListItem<>(tt.getTarget(), holder));
						}
						joinTargets.add(new JoinStateData<>(holder, tt.getGuard()));
					}
				}
				JoinPseudoState<S, E> pseudoState = new JoinPseudoState<>(joins, joinTargets);

				state = buildStateInternal(stateData.getState(), stateData.getDeferred(), stateData.getEntryActions(),
						stateData.getExitActions(), stateData.getStateActions(), pseudoState, stateMachineModel);
				states.add(state);
				stateMap.put(stateData.getState(), state);
			}
		}

		Collection<Transition<S, E>> transitions = new ArrayList<>();
		for (TransitionData<S, E> transitionData : transitionsData) {
			S source = transitionData.getSource();
			S target = transitionData.getTarget();
			E event = transitionData.getEvent();
			Long period = transitionData.getPeriod();
			Integer count = transitionData.getCount();

			Trigger<S, E> trigger = null;
			if (event != null) {
				trigger = new EventTrigger<>(event);
			} else if (period != null) {
				TimerTrigger<S, E> t = new TimerTrigger<>(period, count != null ? count : 0);
				if (beanFactory != null) {
					t.setBeanFactory(beanFactory);
				}
				trigger = t;
				((AbstractState<S, E>)stateMap.get(source)).getTriggers().add(trigger);
			}

			if (transitionData.getKind() == TransitionKind.EXTERNAL) {
				// TODO can we do this?
				if (stateMap.get(source) == null || stateMap.get(target) == null) {
					continue;
				}
				DefaultExternalTransition<S, E> transition = new DefaultExternalTransition<>(stateMap.get(source),
                        stateMap.get(target), transitionData.getActions(), event, transitionData.getGuard(), trigger,
                        transitionData.getSecurityRule(), transitionData.getName());
				transitions.add(transition);

			} else if (transitionData.getKind() == TransitionKind.LOCAL) {
				// TODO can we do this?
				if (stateMap.get(source) == null || stateMap.get(target) == null) {
					continue;
				}
				DefaultLocalTransition<S, E> transition = new DefaultLocalTransition<>(stateMap.get(source),
                        stateMap.get(target), transitionData.getActions(), event, transitionData.getGuard(), trigger,
                        transitionData.getSecurityRule(), transitionData.getName());
				transitions.add(transition);
			} else if (transitionData.getKind() == TransitionKind.INTERNAL) {
				DefaultInternalTransition<S, E> transition = new DefaultInternalTransition<>(stateMap.get(source),
                        transitionData.getActions(), event, transitionData.getGuard(), trigger,
                        transitionData.getSecurityRule(), transitionData.getName());
				transitions.add(transition);
			}
		}

		if (stateMachineTransitions.getJoins() != null) {
			for (Entry<S, List<S>> entry : stateMachineTransitions.getJoins().entrySet()) {
				if (stateMap.get(entry.getKey()) != null) {
					List<S> entryList = entry.getValue();
					for (S entryState : entryList) {
						State<S, E> source = stateMap.get(entryState);
						if (source != null && !source.isOrthogonal()) {
							State<S, E> target = stateMap.get(entry.getKey());
							DefaultExternalTransition<S, E> transition = new DefaultExternalTransition<>(
                                    source, target, null, null, null, null, null);
							transitions.add(transition);
						}
					}
				}
			}
		}

		Transition<S, E> initialTransition = new InitialTransition<>(initialState, Actions.from(initialAction));
		StateMachine<S, E> machine = buildStateMachineInternal(states, transitions, initialState, initialTransition,
				null, defaultExtendedState, historyState, contextEvents, beanFactory,
				beanName, machineId != null ? machineId : stateMachineModel.getConfigurationData().getMachineId(), uuid, stateMachineModel);
		return machine;
	}

	protected abstract StateMachine<S, E> buildStateMachineInternal(Collection<State<S, E>> states,
			Collection<Transition<S, E>> transitions, State<S, E> initialState, Transition<S, E> initialTransition,
			Message<E> initialEvent, ExtendedState extendedState, PseudoState<S, E> historyState,
			Boolean contextEventsEnabled, BeanFactory beanFactory, String beanName, String machineId, UUID uuid,
			StateMachineModel<S, E> stateMachineModel);

	protected abstract State<S, E> buildStateInternal(S id, Collection<E> deferred,
			Collection<Function<StateContext<S, E>, Mono<Void>>> entryActions,
			Collection<Function<StateContext<S, E>, Mono<Void>>> exitActions,
			Collection<Function<StateContext<S, E>, Mono<Void>>> stateActions, PseudoState<S, E> pseudoState,
			StateMachineModel<S, E> stateMachineModel);

	private Iterator<Node<StateData<S, E>>> buildStateDataIterator(StateMachineModel<S, E> stateMachineModel) {
		Tree<StateData<S, E>> tree = new Tree<>();
		treeAdd(tree, stateMachineModel.getStatesData().getStateData());
		return new TreeTraverser<Node<StateData<S, E>>>() {
			@Override
			public Iterable<Node<StateData<S, E>>> children(Node<StateData<S, E>> root) {
				return root.getChildren();
			}
		}.postOrderTraversal(tree.getRoot()).iterator();
	}

	private void treeAdd(Tree<StateData<S, E>> tree, Collection<StateData<S, E>> stateDatas) {
		// recursive call due to possible submachine data ref
		if (stateDatas == null) {
			return;
		}
		for (StateData<S, E> stateData : stateDatas) {
			tree.add(stateData, stateData.getState(), stateData.getParent());
			treeAdd(tree, stateData.getSubmachineStateData());
		}
	}

	protected abstract RegionState<S, E> buildRegionStateInternal(S id, Collection<Region<S, E>> regions,
			Collection<E> deferred, Collection<Function<StateContext<S, E>, Mono<Void>>> entryActions,
			Collection<Function<StateContext<S, E>, Mono<Void>>> exitActions, PseudoState<S, E> pseudoState,
			StateMachineModel<S, E> stateMachineModel);

	/**
	 * Simple utility listener waiting machine to get started if
	 * autostart was requestes. Needed for machine to be ready
	 * if async executor is used.
	 */
	private static class AutostartListener<S, E> extends StateMachineListenerAdapter<S, E> {
		final CountDownLatch latch = new CountDownLatch(1);

		@Override
		public void stateMachineStarted(StateMachine<S, E> stateMachine) {
			latch.countDown();
		}
	}

	private static class HolderListItem<S, E> {
		final S key;
		final StateHolder<S, E> value;

		private HolderListItem(S key, StateHolder<S, E> value) {
			this.key = key;
			this.value = value;
		}
	}
}
