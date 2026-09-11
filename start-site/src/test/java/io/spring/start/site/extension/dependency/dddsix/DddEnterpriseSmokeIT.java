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
import io.spring.initializr.metadata.InitializrMetadataBuilder;
import io.spring.start.site.support.GenerationRequestAttributes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DddEnterpriseSmokeIT {

	@AfterEach
	void clear() {
		GenerationRequestAttributes.clear();
	}

	@Test
	void smokeEnhancedUserToWorkspace() throws Exception {
		Path projectRoot = Path.of("/tmp/p8-smoke-demo");
		if (Files.exists(projectRoot)) {
			deleteRecursively(projectRoot);
		}
		Files.createDirectories(projectRoot);

		GenerationRequestAttributes.EntitySpec user = new GenerationRequestAttributes.EntitySpec("User", "sys_user",
				"postgresql", "mybatis-plus", "用户", true,
				List.of(new GenerationRequestAttributes.FieldSpec("username", "String", true, true, "用户名", true),
						new GenerationRequestAttributes.FieldSpec("email", "String", false, false, "邮箱", true),
						new GenerationRequestAttributes.FieldSpec("nickname", "String", false, false, "昵称", true)),
				GenerationRequestAttributes.ApiFlags.allCrud());
		GenerationRequestAttributes.set(new GenerationRequestAttributes("ddd-enhanced", "[]", List.of(user)));

		MutableProjectDescription description = new MutableProjectDescription();
		description.setGroupId("com.example");
		description.setArtifactId("demo-service");
		description.setVersion("0.0.1-SNAPSHOT");
		description.setPackageName("com.example.demo");
		description.setApplicationName("DemoServiceApplication");
		description.setDescription("P8 smoke");
		description.setPlatformVersion(Version.parse("4.1.1"));
		description.setLanguage(new JavaLanguage("21"));

		new DddSixModuleProjectContributor(description, InitializrMetadataBuilder.create().build())
			.contribute(projectRoot);

		assertThat(projectRoot.resolve("pom.xml")).exists();
		assertThat(projectRoot.resolve(
				"demo-service-application/src/main/java/com/example/demo/application/controller/UserController.java"))
			.exists();
		String api = Files.readString(projectRoot
			.resolve("demo-service-contract/src/main/java/com/example/demo/contract/constant/UserApiPath.java"));
		assertThat(api).contains("/demo/v1/users");
		String svc = Files.readString(projectRoot.resolve(
				"demo-service-application/src/main/java/com/example/demo/application/service/UserApplicationService.java"));
		assertThat(svc).doesNotContain("TODO");
	}

	private static void deleteRecursively(Path path) throws Exception {
		if (!Files.exists(path)) {
			return;
		}
		Files.walk(path).sorted((a, b) -> b.compareTo(a)).forEach((p) -> {
			try {
				Files.deleteIfExists(p);
			}
			catch (Exception ignored) {
			}
		});
	}

}
