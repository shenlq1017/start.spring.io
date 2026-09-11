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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.spring.initializr.generator.language.java.JavaLanguage;
import io.spring.initializr.generator.project.MutableProjectDescription;
import io.spring.initializr.generator.version.Version;
import io.spring.initializr.metadata.InitializrMetadata;
import io.spring.initializr.metadata.InitializrMetadataBuilder;
import io.spring.start.site.support.GenerationRequestAttributes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for enhanced DDD CRUD slice generation.
 */
class DddCrudSliceGeneratorTests {

	@AfterEach
	void clear() {
		GenerationRequestAttributes.clear();
	}

	@Test
	void enhancedTemplateGeneratesUserCrudFiles(@TempDir Path projectRoot) throws Exception {
		GenerationRequestAttributes.EntitySpec user = new GenerationRequestAttributes.EntitySpec("User", "sys_user",
				"postgresql", "mybatis-plus", "用户",
				List.of(new GenerationRequestAttributes.FieldSpec("username", "String", true, true),
						new GenerationRequestAttributes.FieldSpec("email", "String", false, false)),
				new GenerationRequestAttributes.ApiFlags(true, true, true, true, true, true, true));
		GenerationRequestAttributes.set(new GenerationRequestAttributes("ddd-enhanced", "[]", List.of(user)));

		MutableProjectDescription description = new MutableProjectDescription();
		description.setGroupId("com.example");
		description.setArtifactId("demo-service");
		description.setVersion("0.0.1-SNAPSHOT");
		description.setPackageName("com.example.demo");
		description.setApplicationName("DemoServiceApplication");
		description.setDescription("Demo service");
		description.setPlatformVersion(Version.parse("4.1.1"));
		description.setLanguage(new JavaLanguage("21"));

		InitializrMetadata metadata = InitializrMetadataBuilder.create().build();
		DddSixModuleProjectContributor contributor = new DddSixModuleProjectContributor(description, metadata);
		contributor.contribute(projectRoot);

		assertThat(projectRoot
			.resolve("demo-service-bootstrap/src/main/resources/db/migration/V1__01_create_sys_user.sql")).exists();
		assertThat(projectRoot.resolve("demo-service-domain/src/main/java/com/example/demo/domain/model/User.java"))
			.exists();
		assertThat(projectRoot.resolve(
				"demo-service-application/src/main/java/com/example/demo/application/controller/UserController.java"))
			.exists();
		assertThat(projectRoot.resolve(
				"demo-service-application/src/main/java/com/example/demo/application/service/UserImportExportService.java"))
			.exists();
		assertThat(projectRoot.resolve(
				"demo-service-contract/src/main/java/com/example/demo/contract/dto/request/CreateUserRequest.java"))
			.exists();
		String controller = Files.readString(projectRoot.resolve(
				"demo-service-application/src/main/java/com/example/demo/application/controller/UserController.java"));
		assertThat(controller).contains("DeleteMapping").contains("/import").contains("/export");
	}

	@Test
	void standardTemplateSkipsCrud(@TempDir Path projectRoot) throws Exception {
		GenerationRequestAttributes.set(new GenerationRequestAttributes("ddd-standard", "", List.of()));
		MutableProjectDescription description = new MutableProjectDescription();
		description.setGroupId("com.example");
		description.setArtifactId("demo-service");
		description.setVersion("0.0.1-SNAPSHOT");
		description.setPackageName("com.example.demo");
		description.setApplicationName("DemoServiceApplication");
		description.setPlatformVersion(Version.parse("4.1.1"));
		description.setLanguage(new JavaLanguage("21"));
		InitializrMetadata metadata = InitializrMetadataBuilder.create().build();
		new DddSixModuleProjectContributor(description, metadata).contribute(projectRoot);
		assertThat(projectRoot.resolve("demo-service-domain/src/main/java/com/example/demo/domain/model/User.java"))
			.doesNotExist();
	}

}
