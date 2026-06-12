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
package ch.unibas.medizin.universe.statemachine.config.model.verifier;

import java.util.Iterator;

import ch.unibas.medizin.universe.statemachine.config.model.MalformedConfigurationException;
import ch.unibas.medizin.universe.statemachine.config.model.StateData;
import ch.unibas.medizin.universe.statemachine.config.model.StateMachineModel;
import ch.unibas.medizin.universe.statemachine.config.model.StatesData;
import ch.unibas.medizin.universe.statemachine.support.tree.Tree;
import ch.unibas.medizin.universe.statemachine.support.tree.Tree.Node;
import ch.unibas.medizin.universe.statemachine.support.tree.TreeTraverser;

/**
 * {@link StateMachineModelVerifier} which verifies a base model structure
 * like existence of initial states, etc.
 *
 * @author Janne Valkealahti
 *
 * @param <S> the type of state
 * @param <E> the type of event
 */
public class BaseStructureVerifier<S, E> implements StateMachineModelVerifier<S, E> {

	@Override
	public void verify(StateMachineModel<S, E> model) {
		// verify that we have transitions
		if (model.getTransitionsData().getTransitions().isEmpty()) {
			MalformedConfigurationException exception = new MalformedConfigurationException("Must have at least one transition");
			throw exception;
		}
		// verify that we have initial state
		Iterator<Node<StateData<S, E>>> iterator = buildStateDataIterator(model.getStatesData());
		while (iterator.hasNext()) {
			Node<StateData<S, E>> node = iterator.next();
			if (node.getData() == null) {
				boolean initialStateFound = false;
				for (Node<StateData<S, E>> n : node.getChildren()) {
					StateData<S, E> data = n.getData();
					if (data.isInitial()) {
						initialStateFound = true;
					}
				}
				if (!initialStateFound) {
					MalformedConfigurationException exception = new MalformedConfigurationException("Initial state not set");
					for (Node<StateData<S, E>> c : node.getChildren()) {
						exception.addTrace(c.getData().toString());
					}
					throw exception;
				}
			}
		}
	}

	private Iterator<Node<StateData<S, E>>> buildStateDataIterator(StatesData<S, E> stateMachineStates) {
		Tree<StateData<S, E>> tree = new Tree<>();

		for (StateData<S, E> stateData : stateMachineStates.getStateData()) {
			Object id = stateData.getState();
			Object parent = stateData.getParent();
			tree.add(stateData, id, parent);
		}

		TreeTraverser<Node<StateData<S, E>>> traverser = new TreeTraverser<>() {
            @Override
            public Iterable<Node<StateData<S, E>>> children(Node<StateData<S, E>> root) {
                return root.getChildren();
            }
        };

		Iterable<Node<StateData<S, E>>> postOrderTraversal = traverser.postOrderTraversal(tree.getRoot());
		Iterator<Node<StateData<S, E>>> iterator = postOrderTraversal.iterator();
		return iterator;
	}
}
