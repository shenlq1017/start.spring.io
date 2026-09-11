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

import java.nio.file.Files;
import java.nio.file.Path;

import io.spring.initializr.generator.buildsystem.Dependency;
import io.spring.initializr.generator.language.java.JavaLanguage;
import io.spring.initializr.generator.project.MutableProjectDescription;
import io.spring.initializr.generator.version.Version;
import io.spring.initializr.metadata.InitializrMetadata;
import io.spring.initializr.metadata.InitializrMetadataBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PlatformMonorepoProjectContributor} (no Spring context /
 * network).
 *
 * @author start.spring.io China ecosystem
 */
class PlatformMonorepoProjectContributorTests {

	@Test
	void contributesPlatformMonorepoTree(@TempDir Path projectRoot) throws Exception {
		MutableProjectDescription description = new MutableProjectDescription();
		description.setGroupId("com.example");
		description.setArtifactId("platform-parent");
		description.setVersion("0.0.1-SNAPSHOT");
		description.setPackageName("com.example.platform");
		description.setApplicationName("PlatformApplication");
		description.setDescription("Platform monorepo");
		description.setPlatformVersion(Version.parse("4.1.1"));
		description.setLanguage(new JavaLanguage("21"));

		InitializrMetadata metadata = InitializrMetadataBuilder.create().build();
		PlatformMonorepoProjectContributor contributor = new PlatformMonorepoProjectContributor(description, metadata);

		Path legacySrc = projectRoot.resolve("src/main/java/com/example/platform");
		Files.createDirectories(legacySrc);
		Files.writeString(legacySrc.resolve("PlatformApplication.java"), "class PlatformApplication {}");
		Files.writeString(projectRoot.resolve("pom.xml"), "<project></project>");

		contributor.contribute(projectRoot);

		assertThat(projectRoot.resolve("src")).doesNotExist();
		assertThat(projectRoot.resolve("pom.xml")).exists();
		String parentPom = Files.readString(projectRoot.resolve("pom.xml"));
		assertThat(parentPom).contains("<packaging>pom</packaging>")
			.contains("<module>common</module>")
			.contains("<module>starters</module>")
			.contains("<module>services</module>")
			.contains("<module>gateway</module>")
			.contains("spring-boot-dependencies")
			.contains("com.example");

		assertThat(projectRoot.resolve("common/pom.xml")).exists();
		for (String mod : new String[] { "common-core", "common-web", "common-mybatis", "common-redis", "common-cloud",
				"common-test" }) {
			assertThat(projectRoot.resolve("common/" + mod + "/pom.xml")).exists();
		}
		assertThat(projectRoot.resolve("starters/pom.xml")).exists();
		for (String mod : new String[] { "common-web-starter", "common-mybatis-starter", "common-cloud-starter" }) {
			assertThat(projectRoot.resolve("starters/" + mod + "/pom.xml")).exists();
		}
		assertThat(projectRoot.resolve(
				"starters/common-web-starter/src/main/java/com/example/platform/starter/web/CommonWebAutoConfiguration.java"))
			.exists();
		assertThat(projectRoot.resolve(
				"starters/common-web-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"))
			.exists();
		assertThat(projectRoot.resolve("services/pom.xml")).exists();
		assertThat(projectRoot.resolve("services/README.md")).exists();
		assertThat(projectRoot.resolve("gateway/pom.xml")).exists();
		assertThat(projectRoot.resolve("gateway/src/main/java/com/example/platform/gateway/package-info.java"))
			.exists();
		assertThat(projectRoot.resolve("deploy/compose.yaml")).exists();
		assertThat(projectRoot.resolve("README-PLATFORM.md")).exists();
		assertThat(projectRoot
			.resolve("common/common-core/src/main/java/com/example/platform/common/core/package-info.java")).exists();
	}

	@Test
	void platformRemovesDddSixWhenBothRequested() {
		MutableProjectDescription description = new MutableProjectDescription();
		description.addDependency("platform-monorepo",
				Dependency.withCoordinates("org.springframework.boot", "spring-boot").build());
		description.addDependency("ddd-six-module",
				Dependency.withCoordinates("org.springframework.boot", "spring-boot").build());

		new PlatformMonorepoProjectDescriptionCustomizer().customize(description);

		assertThat(description.getRequestedDependencies()).containsKey("platform-monorepo");
		assertThat(description.getRequestedDependencies()).doesNotContainKey("ddd-six-module");
	}

}
