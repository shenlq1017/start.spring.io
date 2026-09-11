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

package io.spring.start.site.extension.dependency.dddsix;

import io.spring.initializr.generator.buildsystem.Build;
import io.spring.initializr.generator.spring.build.BuildCustomizer;

import org.springframework.core.Ordered;

/**
 * Removes the {@code ddd-six-module} marker dependency from the build model (the layout
 * is produced by {@link DddSixModuleProjectContributor}).
 *
 * @author start.spring.io China ecosystem
 */
class DddSixModuleBuildCustomizer implements BuildCustomizer<Build> {

	static final String DEPENDENCY_ID = "ddd-six-module";

	@Override
	public void customize(Build build) {
		build.dependencies().remove(DEPENDENCY_ID);
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

}
