/*
 * Copyright 2016-2023 the original author or authors.
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
package org.springframework.statemachine.boot.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.statemachine.data.jpa.JpaRepositoryState;
import org.springframework.statemachine.data.jpa.JpaStateRepository;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for JPA repositories and Entity classes.
 */
@AutoConfiguration
@ConditionalOnClass(JpaStateRepository.class)
@ConditionalOnProperty(prefix = "spring.statemachine.data.jpa.repositories", name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigurationPackage(basePackageClasses = {JpaRepositoryState.class})
@EnableJpaRepositories(basePackageClasses = {JpaStateRepository.class})
public class StateMachineJpaRepositoriesAutoConfiguration {

}
