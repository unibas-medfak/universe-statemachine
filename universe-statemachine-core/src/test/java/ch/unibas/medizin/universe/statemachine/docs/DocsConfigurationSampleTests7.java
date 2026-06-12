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
package ch.unibas.medizin.universe.statemachine.docs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ch.unibas.medizin.universe.statemachine.config.EnableStateMachine;
import ch.unibas.medizin.universe.statemachine.config.StateMachineConfigurerAdapter;
import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineStateConfigurer;
import ch.unibas.medizin.universe.statemachine.config.builders.StateMachineTransitionConfigurer;
import ch.unibas.medizin.universe.statemachine.guard.Guard;

public class DocsConfigurationSampleTests7 {

// tag::snippetA[]
	@Configuration
	@EnableStateMachine
	public class Config1
			extends StateMachineConfigurerAdapter<String, String> {

		@Override
		public void configure(StateMachineStateConfigurer<String, String> states)
				throws Exception {
			states
				.withStates()
					.initial("S1")
					.state("S2");
		}

		@Override
		public void configure(StateMachineTransitionConfigurer<String, String> transitions)
				throws Exception {
			transitions
				.withExternal()
					.source("S1").target("S2").event("E1").guard(guard1(true))
					.and()
				.withExternal()
					.source("S1").target("S2").event("E2").guard(guard1(false))
					.and()
				.withExternal()
					.source("S1").target("S2").event("E3").guard(guard2(true))
					.and()
				.withExternal()
					.source("S1").target("S2").event("E4").guard(guard2(false));
		}

		@Bean
		public Guard<String, String> guard1(final boolean value) {
			return context -> value;
		}

		public Guard<String, String> guard2(final boolean value) {
			return context -> value;
		}
	}
// end::snippetA[]

}
