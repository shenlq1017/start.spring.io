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

import io.spring.initializr.generator.project.MutableProjectDescription;
import io.spring.initializr.generator.project.ProjectDescriptionCustomizer;

/**
 * When {@code platform-monorepo} is selected, removes {@code ddd-six-module} so the two
 * Project Structure generators do not both rewrite the tree.
 *
 * @author start.spring.io China ecosystem
 */
public class PlatformMonorepoProjectDescriptionCustomizer implements ProjectDescriptionCustomizer {

	@Override
	public void customize(MutableProjectDescription description) {
		if (description.getRequestedDependencies().containsKey(PlatformMonorepoBuildCustomizer.DEPENDENCY_ID)) {
			description.removeDependency("ddd-six-module");
		}
	}

}
