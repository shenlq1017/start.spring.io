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

import io.spring.initializr.generator.language.java.JavaLanguage;
import io.spring.initializr.generator.project.MutableProjectDescription;
import io.spring.initializr.generator.version.Version;
import io.spring.initializr.metadata.InitializrMetadata;
import io.spring.initializr.metadata.InitializrMetadataBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DddSixModuleProjectContributor} (no Spring context / network).
 *
 * @author start.spring.io China ecosystem
 */
class DddSixModuleProjectContributorTests {

	@Test
	void contributesSixModuleTree(@TempDir Path projectRoot) throws Exception {
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

		// Simulate default single-module leftovers that should be removed
		Path legacySrc = projectRoot.resolve("src/main/java/com/example/demo");
		Files.createDirectories(legacySrc);
		Files.writeString(legacySrc.resolve("DemoServiceApplication.java"), "class DemoServiceApplication {}");
		Files.writeString(projectRoot.resolve("pom.xml"), "<project></project>");

		contributor.contribute(projectRoot);

		assertThat(projectRoot.resolve("src")).doesNotExist();
		assertThat(projectRoot.resolve("pom.xml")).exists();
		String parentPom = Files.readString(projectRoot.resolve("pom.xml"));
		assertThat(parentPom).contains("<packaging>pom</packaging>")
			.contains("<module>demo-service-contract</module>")
			.contains("<module>demo-service-bootstrap</module>")
			.contains("com.example");

		for (String suffix : new String[] { "contract", "feign-client", "domain", "infrastructure", "application",
				"bootstrap" }) {
			assertThat(projectRoot.resolve("demo-service-" + suffix + "/pom.xml")).exists();
		}

		assertThat(projectRoot
			.resolve("demo-service-bootstrap/src/main/java/com/example/demo/DemoServiceApplication.java")).exists();
		assertThat(projectRoot.resolve("demo-service-bootstrap/src/main/resources/application.yml")).exists();
		assertThat(
				projectRoot.resolve("demo-service-contract/src/main/java/com/example/demo/contract/package-info.java"))
			.exists();
		assertThat(projectRoot
			.resolve("demo-service-domain/src/main/java/com/example/demo/domain/model/package-info.java")).exists();
		assertThat(
				projectRoot.resolve("demo-service-bootstrap/src/test/java/com/example/demo/ModuleDependencyTest.java"))
			.exists();
		assertThat(projectRoot.resolve("README-DDD.md")).exists();
	}

}
