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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.spring.initializr.generator.project.ProjectDescription;
import io.spring.initializr.generator.project.contributor.ProjectContributor;
import io.spring.initializr.generator.version.Version;
import io.spring.initializr.metadata.BillOfMaterials;
import io.spring.initializr.metadata.InitializrMetadata;

import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

/**
 * Rewrites a single-module Initializr project into a platform Monorepo Maven layout
 * (common / starters / services / gateway / deploy).
 * <p>
 * Runs late so default contributors can create wrappers / {@code HELP.md}, then replaces
 * {@code pom.xml} and removes the single-module {@code src/} tree.
 *
 * @author start.spring.io China ecosystem
 */
class PlatformMonorepoProjectContributor implements ProjectContributor {

	private static final String TEMPLATE_ROOT = "/templates/platform-monorepo/";

	private final ProjectDescription description;

	private final InitializrMetadata metadata;

	PlatformMonorepoProjectContributor(ProjectDescription description, InitializrMetadata metadata) {
		this.description = description;
		this.metadata = metadata;
	}

	@Override
	public void contribute(Path projectRoot) throws IOException {
		Map<String, String> model = buildModel();

		deleteRecursivelyIfExists(projectRoot.resolve("src"));

		write(projectRoot.resolve("pom.xml"), render("parent-pom.mustache", model));
		write(projectRoot.resolve("README-PLATFORM.md"), render("readme.mustache", model));

		write(projectRoot.resolve("common/pom.xml"), render("common-aggregator-pom.mustache", model));
		writeCommonModule(projectRoot, "common-core", "common-core-pom.mustache", model);
		writeCommonModule(projectRoot, "common-web", "common-web-pom.mustache", model);
		writeCommonModule(projectRoot, "common-mybatis", "common-mybatis-pom.mustache", model);
		writeCommonModule(projectRoot, "common-redis", "common-redis-pom.mustache", model);
		writeCommonModule(projectRoot, "common-cloud", "common-cloud-pom.mustache", model);
		writeCommonModule(projectRoot, "common-test", "common-test-pom.mustache", model);

		write(projectRoot.resolve("starters/pom.xml"), render("starters-aggregator-pom.mustache", model));
		writeStarter(projectRoot, "web", model);
		writeStarter(projectRoot, "mybatis", model);
		writeStarter(projectRoot, "cloud", model);

		write(projectRoot.resolve("services/pom.xml"), render("services-aggregator-pom.mustache", model));
		write(projectRoot.resolve("services/README.md"), render("services-readme.mustache", model));

		write(projectRoot.resolve("gateway/pom.xml"), render("gateway-pom.mustache", model));
		String packagePath = model.get("packageName").replace('.', '/');
		Path gatewayPkg = projectRoot.resolve("gateway/src/main/java/" + packagePath + "/gateway");
		Files.createDirectories(gatewayPkg);
		write(gatewayPkg.resolve("package-info.java"), render("package-info.mustache", Map.of("doc",
				"Spring Cloud Gateway module stub", "packageDecl", model.get("packageName") + ".gateway")));

		Path deploy = projectRoot.resolve("deploy");
		Files.createDirectories(deploy);
		write(deploy.resolve("compose.yaml"), render("compose.mustache", model));

		writePackageInfos(projectRoot, model);
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	private void writeCommonModule(Path projectRoot, String module, String template, Map<String, String> model)
			throws IOException {
		Path dir = projectRoot.resolve("common/" + module);
		Files.createDirectories(dir);
		write(dir.resolve("pom.xml"), render(template, model));
	}

	private void writeStarter(Path projectRoot, String name, Map<String, String> model) throws IOException {
		String module = "common-" + name + "-starter";
		Path dir = projectRoot.resolve("starters/" + module);
		Files.createDirectories(dir);
		Map<String, String> starterModel = new LinkedHashMap<>(model);
		starterModel.put("starterName", name);
		starterModel.put("starterModule", module);
		starterModel.put("commonArtifact", "common-" + name);
		String autoConfigSimple = switch (name) {
			case "mybatis" -> "CommonMyBatisAutoConfiguration";
			case "web" -> "CommonWebAutoConfiguration";
			case "cloud" -> "CommonCloudAutoConfiguration";
			default -> "Common" + Character.toUpperCase(name.charAt(0)) + name.substring(1) + "AutoConfiguration";
		};
		starterModel.put("autoConfigSimple", autoConfigSimple);
		String starterPkg = model.get("packageName") + ".starter." + name;
		starterModel.put("starterPackage", starterPkg);
		starterModel.put("autoConfigFqn", starterPkg + "." + starterModel.get("autoConfigSimple"));

		write(dir.resolve("pom.xml"), render("starter-pom.mustache", starterModel));

		Path javaDir = dir.resolve("src/main/java/" + starterPkg.replace('.', '/'));
		Files.createDirectories(javaDir);
		write(javaDir.resolve(starterModel.get("autoConfigSimple") + ".java"),
				render("starter-autoconfig.mustache", starterModel));

		Path imports = dir.resolve(
				"src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");
		Files.createDirectories(imports.getParent());
		write(imports, starterModel.get("autoConfigFqn") + "\n");
	}

	private void writePackageInfos(Path projectRoot, Map<String, String> model) throws IOException {
		String base = model.get("packageName");
		record Pkg(String modulePath, String relativePackage, String doc) {
		}
		List<Pkg> packages = List.of(new Pkg("common/common-core", "common.core", "Platform shared core (JDK-only)"),
				new Pkg("common/common-core", "common.core.page", "Paging helpers"),
				new Pkg("common/common-core", "common.core.exception", "Business exception base types"),
				new Pkg("common/common-web", "common.web", "Shared web / Problem Details helpers"),
				new Pkg("common/common-mybatis", "common.mybatis", "MyBatis-Plus shared helpers"),
				new Pkg("common/common-mybatis", "common.mybatis.handler", "Type handlers"),
				new Pkg("common/common-redis", "common.redis", "Redis helpers"),
				new Pkg("common/common-cloud", "common.cloud", "Cloud / Feign helpers"),
				new Pkg("common/common-test", "common.test", "Shared test utilities"));

		for (Pkg pkg : packages) {
			String fullPkg = base + "." + pkg.relativePackage();
			Path dir = projectRoot.resolve(pkg.modulePath() + "/src/main/java/" + fullPkg.replace('.', '/'));
			Files.createDirectories(dir);
			write(dir.resolve("package-info.java"),
					render("package-info.mustache", Map.of("doc", pkg.doc(), "packageDecl", fullPkg)));
		}
	}

	private Map<String, String> buildModel() {
		String artifactId = this.description.getArtifactId();
		if (!StringUtils.hasText(artifactId)) {
			artifactId = "platform-parent";
		}
		String packageName = this.description.getPackageName();
		if (!StringUtils.hasText(packageName)) {
			packageName = "com.example.platform";
		}
		String bootVersion = this.description.getPlatformVersion().toString();
		String javaVersion = this.description.getLanguage().jvmVersion();
		String springCloudVersion = resolveSpringCloudVersion(this.description.getPlatformVersion());
		String descriptionText = this.description.getDescription();
		if (!StringUtils.hasText(descriptionText)) {
			descriptionText = artifactId;
		}

		Map<String, String> model = new LinkedHashMap<>();
		model.put("groupId", this.description.getGroupId());
		model.put("artifactId", artifactId);
		model.put("version", this.description.getVersion());
		model.put("description", descriptionText);
		model.put("bootVersion", bootVersion);
		model.put("javaVersion", javaVersion);
		model.put("springCloudVersion", springCloudVersion);
		model.put("packageName", packageName);
		return model;
	}

	private String resolveSpringCloudVersion(Version platformVersion) {
		try {
			BillOfMaterials bom = this.metadata.getConfiguration().getEnv().getBoms().get("spring-cloud");
			if (bom != null) {
				return bom.resolve(platformVersion).getVersion();
			}
		}
		catch (Exception ex) {
			// fall through
		}
		return "2025.1.3";
	}

	private static String render(String templateName, Map<String, String> model) {
		String template = readClasspath(TEMPLATE_ROOT + templateName);
		String result = template;
		for (Map.Entry<String, String> entry : model.entrySet()) {
			result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
		}
		return result;
	}

	private static String readClasspath(String path) {
		try (InputStream in = PlatformMonorepoProjectContributor.class.getResourceAsStream(path)) {
			if (in == null) {
				throw new IllegalStateException("Missing classpath resource: " + path);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static void write(Path path, String content) throws IOException {
		Path parent = path.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		Files.writeString(path, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
	}

	private static void deleteRecursivelyIfExists(Path path) throws IOException {
		if (!Files.exists(path)) {
			return;
		}
		Files.walkFileTree(path, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

}
