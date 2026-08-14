/*
 * Copyright 2015-2020 the original author or authors.
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
package ch.unibas.medizin.universe.statemachine.ensemble;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import ch.unibas.medizin.universe.statemachine.AbstractStateMachineTests;
import ch.unibas.medizin.universe.statemachine.StateMachine;
import ch.unibas.medizin.universe.statemachine.config.EnableStateMachine;
import ch.unibas.medizin.universe.statemachine.config.StateMachineConfigurerAdapter;
import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineStateConfigurer;
import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineTransitionConfigurer;

public class DistributedStateMachineTests extends AbstractStateMachineTests {

	@Override
	protected AnnotationConfigApplicationContext buildContext() {
		return new AnnotationConfigApplicationContext();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMachines() {
		context.register(Config1.class, Config2.class);
		context.refresh();
		StateMachine<String, String> machine1 =
				context.getBean("sm1", StateMachine.class);
		StateMachine<String, String> machine2 =
				context.getBean("sm2", StateMachine.class);

		StateMachineEnsemble<String, String> ensemble = new InMemoryStateMachineEnsemble<>();

		DistributedStateMachine<String, String> machine1s =
                new DistributedStateMachine<>(ensemble, machine1);

		DistributedStateMachine<String, String> machine2s =
                new DistributedStateMachine<>(ensemble, machine2);

		machine1s.afterPropertiesSet();
		machine2s.afterPropertiesSet();

		machine1s.start();
		machine2s.start();

		machine1s.sendEvent("E1");
		assertThat(machine1.getState().getIds()).containsOnly("S1");
		assertThat(machine2.getState().getIds()).containsOnly("S1");

		machine2s.sendEvent("E2");
		assertThat(machine1.getState().getIds()).containsOnly("S2");
		assertThat(machine2.getState().getIds()).containsOnly("S2");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testJoin() {
		context.register(Config1.class, Config2.class);
		context.refresh();
		StateMachine<String, String> machine1 =
				context.getBean("sm1", StateMachine.class);
		StateMachine<String, String> machine2 =
				context.getBean("sm2", StateMachine.class);

		StateMachineEnsemble<String, String> ensemble = new InMemoryStateMachineEnsemble<>();

		DistributedStateMachine<String, String> machine1s =
                new DistributedStateMachine<>(ensemble, machine1);
		machine1s.afterPropertiesSet();
		machine1s.start();

		machine1s.sendEvent("E1");
		assertThat(machine1.getState().getIds()).containsOnly("S1");

		DistributedStateMachine<String, String> machine2s =
                new DistributedStateMachine<>(ensemble, machine2);
		machine2s.afterPropertiesSet();
		machine2s.start();

		assertThat(machine2.getState().getIds()).containsOnly("S1");
	}

	@Configuration
	@EnableStateMachine(name = "sm1")
	static class Config1 extends SharedConfig {
	}

	@Configuration
	@EnableStateMachine(name = "sm2")
	static class Config2 extends SharedConfig {
	}

	static class SharedConfig extends StateMachineConfigurerAdapter<String, String> {

		@Override
		public void configure(StateMachineStateConfigurer<String, String> states) throws Exception {
			states
				.withStates()
					.initial("SI")
					.state("S1")
					.state("S2");
		}

		@Override
		public void configure(StateMachineTransitionConfigurer<String, String> transitions) throws Exception {
			transitions
				.withExternal()
					.source("SI")
					.target("S1")
					.event("E1")
					.and()
				.withExternal()
					.source("S1")
					.target("S2")
					.event("E2");
		}

	}

}
