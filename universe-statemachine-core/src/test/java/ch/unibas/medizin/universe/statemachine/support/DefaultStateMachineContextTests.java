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
package ch.unibas.medizin.universe.statemachine.support;

import static org.assertj.core.api.Assertions.assertThat;

import ch.unibas.medizin.universe.statemachine.support.DefaultExtendedState;
import ch.unibas.medizin.universe.statemachine.support.DefaultStateMachineContext;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;


public class DefaultStateMachineContextTests {

	@Test
	public void testEquals() {
		DefaultStateMachineContext<String, String> c1 = new DefaultStateMachineContext<>(null, null, null,
                null);
		DefaultStateMachineContext<String, String> c2 = new DefaultStateMachineContext<>(null, null, null,
                null);
		assertThat(c1.equals(c2)).isTrue();

		DefaultStateMachineContext<String, String> c0 = new DefaultStateMachineContext<>(null, null, null,
                null);

		c1 = new DefaultStateMachineContext<>(Arrays.asList("x", "y"), Arrays.asList(c0), "s1", "e1",
                new HashMap<>(), new DefaultExtendedState(), new HashMap<>(), "id");
		c2 = new DefaultStateMachineContext<>(Arrays.asList("x", "y"), Arrays.asList(c0), "s1", "e1",
                new HashMap<>(), new DefaultExtendedState(), new HashMap<>(), "id");
		assertThat(c1.equals(c2)).isTrue();

		c2 = new DefaultStateMachineContext<>(Arrays.asList("d", "y"), Arrays.asList(c0), "s1", "e1",
                new HashMap<>(), new DefaultExtendedState(), new HashMap<>(), "id");
		assertThat(c1.equals(c2)).isFalse();
	}
}
