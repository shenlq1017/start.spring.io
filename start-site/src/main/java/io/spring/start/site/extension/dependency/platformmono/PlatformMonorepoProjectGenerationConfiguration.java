/*
 * Copyright 2012 - present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.start.site.extension.dependency.platformmono;

import io.spring.initializr.generator.buildsystem.maven.MavenBuildSystem;
import io.spring.initializr.generator.condition.ConditionalOnBuildSystem;
import io.spring.initializr.generator.condition.ConditionalOnRequestedDependency;
import io.spring.initializr.generator.project.ProjectDescription;
import io.spring.initializr.generator.project.ProjectGenerationConfiguration;
import io.spring.initializr.metadata.InitializrMetadata;

import org.springframework.context.annotation.Bean;

/**
 * Configuration for platform Monorepo Maven project generation.
 *
 * @author start.spring.io China ecosystem
 */
@ProjectGenerationConfiguration
@ConditionalOnRequestedDependency(PlatformMonorepoBuildCustomizer.DEPENDENCY_ID)
@ConditionalOnBuildSystem(MavenBuildSystem.ID)
class PlatformMonorepoProjectGenerationConfiguration {

	@Bean
	PlatformMonorepoBuildCustomizer platformMonorepoBuildCustomizer() {
		return new PlatformMonorepoBuildCustomizer();
	}

	@Bean
	PlatformMonorepoProjectContributor platformMonorepoProjectContributor(ProjectDescription description,
			InitializrMetadata metadata) {
		return new PlatformMonorepoProjectContributor(description, metadata);
	}

}
