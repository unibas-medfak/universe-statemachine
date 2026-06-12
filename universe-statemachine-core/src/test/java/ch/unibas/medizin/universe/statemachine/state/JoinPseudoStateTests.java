/*
 * Copyright 2019-2020 the original author or authors.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ch.unibas.medizin.universe.statemachine.state.JoinPseudoState;
import ch.unibas.medizin.universe.statemachine.state.ObjectState;
import ch.unibas.medizin.universe.statemachine.state.State;
import org.junit.jupiter.api.Test;
import ch.unibas.medizin.universe.statemachine.TestUtils;

public class JoinPseudoStateTests {

	@SuppressWarnings("unchecked")
	@Test
	public void testResetWithSingleStates() throws Exception {
		ObjectState<String, String> s1 = new ObjectState<>("S1");
		ObjectState<String, String> s2 = new ObjectState<>("S2");

		List<List<State<String, String>>> joins = new ArrayList<>();
		joins.add(Collections.singletonList(s1));
		joins.add(Collections.singletonList(s2));

		JoinPseudoState<String,String> pseudoState = new JoinPseudoState<>(joins, Collections.emptyList());

		Object obj1 = TestUtils.readField("tracker", pseudoState);
		Object obj2 = TestUtils.readField("track", obj1);

		List<List<State<String, String>>> track = (List<List<State<String, String>>>) obj2;
		assertThat(track).hasSize(2);
		assertThat(track.get(0)).hasSize(1);
		assertThat(track.get(1)).hasSize(1);

		pseudoState.reset(Arrays.asList("S1"));
		assertThat(track).hasSize(1);
		assertThat(track.getFirst()).hasSize(1);

		pseudoState.reset(Arrays.asList("S1", "S2"));
		assertThat(track).isEmpty();

		pseudoState.reset(Collections.emptyList());
		assertThat(track).hasSize(2);
		assertThat(track.get(0)).hasSize(1);
		assertThat(track.get(1)).hasSize(1);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testResetWithMultipleStates() throws Exception {
		ObjectState<String, String> s11 = new ObjectState<>("S11");
		ObjectState<String, String> s12 = new ObjectState<>("S12");
		ObjectState<String, String> s21 = new ObjectState<>("S21");
		ObjectState<String, String> s22 = new ObjectState<>("S22");

		List<List<State<String, String>>> joins = new ArrayList<>();
		joins.add(Arrays.asList(s11, s12));
		joins.add(Arrays.asList(s21, s22));

		JoinPseudoState<String,String> pseudoState = new JoinPseudoState<>(joins, Collections.emptyList());

		Object obj1 = TestUtils.readField("tracker", pseudoState);
		Object obj2 = TestUtils.readField("track", obj1);

		List<List<State<String, String>>> track = (List<List<State<String, String>>>) obj2;
		assertThat(track).hasSize(2);
		assertThat(track.get(0)).hasSize(2);
		assertThat(track.get(1)).hasSize(2);

		pseudoState.reset(Arrays.asList("S11"));
		assertThat(track).hasSize(1);
		assertThat(track.getFirst()).hasSize(2);

		pseudoState.reset(Arrays.asList("S11", "S21"));
		assertThat(track).isEmpty();

		pseudoState.reset(Collections.emptyList());
		assertThat(track).hasSize(2);
		assertThat(track.get(0)).hasSize(2);
		assertThat(track.get(1)).hasSize(2);
	}
}
