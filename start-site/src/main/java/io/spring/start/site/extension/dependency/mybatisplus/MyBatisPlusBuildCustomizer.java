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

package io.spring.start.site.extension.dependency.mybatisplus;

import io.spring.initializr.generator.buildsystem.Build;
import io.spring.initializr.generator.buildsystem.Dependency;
import io.spring.initializr.generator.spring.build.BuildCustomizer;

/**
 * When MyBatis-Plus is selected, remove classic MyBatis (Plus supersedes it) and add
 * {@code mybatis-plus-jsqlparser} for pagination plugin support.
 *
 * @author start.spring.io China ecosystem
 */
class MyBatisPlusBuildCustomizer implements BuildCustomizer<Build> {

	@Override
	public void customize(Build build) {
		if (build.dependencies().has("mybatis")) {
			build.dependencies().remove("mybatis");
		}
		if (build.dependencies().has("mybatis-test")) {
			build.dependencies().remove("mybatis-test");
		}
		build.dependencies()
			.add("mybatis-plus-jsqlparser",
					Dependency.withCoordinates("com.baomidou", "mybatis-plus-jsqlparser").build());
	}

}
