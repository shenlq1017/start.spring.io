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
import io.spring.start.site.support.GenerationRequestAttributes;

import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

/**
 * Rewrites a single-module Initializr project into a six-module DDD Maven layout.
 * <p>
 * Runs late so default contributors can create wrappers / {@code HELP.md}, then replaces
 * {@code pom.xml} and removes the single-module {@code src/} tree.
 *
 * @author start.spring.io China ecosystem
 */
class DddSixModuleProjectContributor implements ProjectContributor {

	private static final String TEMPLATE_ROOT = "/templates/ddd-six/";

	private final ProjectDescription description;

	private final InitializrMetadata metadata;

	DddSixModuleProjectContributor(ProjectDescription description, InitializrMetadata metadata) {
		this.description = description;
		this.metadata = metadata;
	}

	@Override
	public void contribute(Path projectRoot) throws IOException {
		Map<String, String> model = buildModel();
		String svc = model.get("svc");

		deleteRecursivelyIfExists(projectRoot.resolve("src"));

		write(projectRoot.resolve("pom.xml"), render("parent-pom.mustache", model));
		write(projectRoot.resolve("README-DDD.md"), render("readme.mustache", model));

		writeModulePom(projectRoot, svc, "contract", model);
		writeModulePom(projectRoot, svc, "feign-client", model);
		writeModulePom(projectRoot, svc, "domain", model);
		writeModulePom(projectRoot, svc, "infrastructure", model);
		writeModulePom(projectRoot, svc, "application", model);
		writeModulePom(projectRoot, svc, "bootstrap", model);

		String packagePath = model.get("packageName").replace('.', '/');
		String packageName = model.get("packageName");

		writePackageInfos(projectRoot, svc, packageName);

		Path bootstrapMain = projectRoot.resolve(svc + "-bootstrap/src/main/java/" + packagePath);
		Files.createDirectories(bootstrapMain);
		write(bootstrapMain.resolve(model.get("applicationName") + ".java"),
				render("application-main.mustache", model));

		Path bootstrapResources = projectRoot.resolve(svc + "-bootstrap/src/main/resources");
		Files.createDirectories(bootstrapResources);
		write(bootstrapResources.resolve("application.yml"), render("application-yml.mustache", model));
		Files.createDirectories(bootstrapResources.resolve("db/migration"));
		write(bootstrapResources.resolve("db/migration/.gitkeep"), "");

		Path infraResources = projectRoot.resolve(svc + "-infrastructure/src/main/resources/mapper");
		Files.createDirectories(infraResources);
		write(infraResources.resolve(".gitkeep"), "");

		Path bootstrapTest = projectRoot.resolve(svc + "-bootstrap/src/test/java/" + packagePath);
		Files.createDirectories(bootstrapTest);
		write(bootstrapTest.resolve("ModuleDependencyTest.java"), render("archunit-test.mustache", model));

		contributeCrudSlices(projectRoot, svc, packageName, model);
	}

	private void contributeCrudSlices(Path projectRoot, String svc, String packageName, Map<String, String> model)
			throws IOException {
		GenerationRequestAttributes attrs = GenerationRequestAttributes.get();
		try {
			var attrsHolder = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
			if (attrsHolder != null) {
				Object reqAttr = attrsHolder.getAttribute(GenerationRequestAttributes.REQUEST_ATTRIBUTE,
						org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
				if (reqAttr instanceof GenerationRequestAttributes fromRequest) {
					attrs = fromRequest;
				}
			}
		}
		catch (Exception ignored) {
			// non-web unit tests
		}
		String template = attrs.getTemplate();
		var entities = attrs.getEntities();
		if (!attrs.isDddEnhanced()) {
			return;
		}
		if (entities.isEmpty()) {
			entities = java.util.List.of(defaultUserEntity());
		}
		DddCrudSliceGenerator.generate(projectRoot, svc, packageName, entities);
		Path readme = projectRoot.resolve("README-DDD.md");
		if (Files.exists(readme)) {
			String effective = (template == null || template.isBlank()) ? "ddd-enhanced(default)" : template;
			String appendix = "\n\n## CRUD slices\nGenerated template=`" + effective + "` entities=" + entities.size()
					+ ".\n";
			boolean anySwagger = entities.stream().anyMatch(GenerationRequestAttributes.EntitySpec::swagger)
					|| entities.stream()
						.flatMap((e) -> e.fields().stream())
						.anyMatch(GenerationRequestAttributes.FieldSpec::swagger);
			if (anySwagger) {
				appendix += "OpenAPI `@Schema` annotations were emitted on DTOs — add `springdoc-openapi` (or Knife4j) if not already on the classpath.\n";
			}
			write(readme, Files.readString(readme) + appendix);
		}
	}

	private static GenerationRequestAttributes.EntitySpec defaultUserEntity() {
		return new GenerationRequestAttributes.EntitySpec("User", "sys_user", "postgresql", "mybatis-plus", "用户", true,
				java.util.List.of(
						new GenerationRequestAttributes.FieldSpec("username", "String", true, true, "用户名", true),
						new GenerationRequestAttributes.FieldSpec("email", "String", false, false, "邮箱", true),
						new GenerationRequestAttributes.FieldSpec("nickname", "String", false, false, "昵称", true)),
				GenerationRequestAttributes.ApiFlags.allCrud());
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	private void writeModulePom(Path projectRoot, String svc, String suffix, Map<String, String> model)
			throws IOException {
		Path moduleDir = projectRoot.resolve(svc + "-" + suffix);
		Files.createDirectories(moduleDir);
		String template = switch (suffix) {
			case "contract" -> "contract-pom.mustache";
			case "feign-client" -> "feign-client-pom.mustache";
			case "domain" -> "domain-pom.mustache";
			case "infrastructure" -> "infrastructure-pom.mustache";
			case "application" -> "application-pom.mustache";
			case "bootstrap" -> "bootstrap-pom.mustache";
			default -> throw new IllegalArgumentException(suffix);
		};
		write(moduleDir.resolve("pom.xml"), render(template, model));
	}

	private void writePackageInfos(Path projectRoot, String svc, String packageName) throws IOException {
		record Pkg(String moduleSuffix, String relativePackage, String doc) {
		}
		List<Pkg> packages = List.of(
				new Pkg("contract", "contract", "API contracts and DTOs (zero Spring runtime dependencies)"),
				new Pkg("contract", "contract.dto.request", "Request DTOs"),
				new Pkg("contract", "contract.dto.response", "Response DTOs"),
				new Pkg("contract", "contract.constant", "API path and service name constants"),
				new Pkg("contract", "contract.enums", "Shared contract enums"),
				new Pkg("feign-client", "feign", "Feign clients and fallback factories"),
				new Pkg("domain", "domain.model", "Domain aggregates and value objects"),
				new Pkg("domain", "domain.command", "Domain commands"),
				new Pkg("domain", "domain.query", "Domain queries"), new Pkg("domain", "domain.event", "Domain events"),
				new Pkg("domain", "domain.repository", "Domain repository ports"),
				new Pkg("domain", "domain.exception", "Domain exceptions"),
				new Pkg("domain", "domain.service", "Domain services"),
				new Pkg("infrastructure", "infrastructure.persistence.entity", "Persistence objects (PO)"),
				new Pkg("infrastructure", "infrastructure.persistence.mapper", "MyBatis mappers"),
				new Pkg("infrastructure", "infrastructure.persistence.repository", "Repository adapters"),
				new Pkg("infrastructure", "infrastructure.persistence.converter", "MapStruct converters"),
				new Pkg("infrastructure", "infrastructure.config", "Infrastructure Spring configuration"),
				new Pkg("application", "application.controller", "HTTP controllers"),
				new Pkg("application", "application.service", "Application services"),
				new Pkg("application", "application.assembler", "DTO assemblers"),
				new Pkg("application", "application.advice", "Exception advice / Problem Details"),
				new Pkg("application", "application.config", "Application-layer configuration"));

		for (Pkg pkg : packages) {
			String fullPkg = packageName + "." + pkg.relativePackage();
			Path dir = projectRoot
				.resolve(svc + "-" + pkg.moduleSuffix() + "/src/main/java/" + fullPkg.replace('.', '/'));
			Files.createDirectories(dir);
			Map<String, String> vars = Map.of("doc", pkg.doc(), "packageDecl", fullPkg);
			write(dir.resolve("package-info.java"), render("package-info.mustache", vars));
		}
	}

	private Map<String, String> buildModel() {
		String svc = this.description.getArtifactId();
		if (!StringUtils.hasText(svc)) {
			svc = "demo-service";
		}
		String packageName = this.description.getPackageName();
		if (!StringUtils.hasText(packageName)) {
			packageName = "com.example.demo";
		}
		String applicationName = this.description.getApplicationName();
		if (!StringUtils.hasText(applicationName)) {
			applicationName = "Application";
		}
		String bootVersion = this.description.getPlatformVersion().toString();
		String javaVersion = this.description.getLanguage().jvmVersion();
		String springCloudVersion = resolveSpringCloudVersion(this.description.getPlatformVersion());
		String dbName = svc.replace('-', '_') + "_db";
		String description = this.description.getDescription();
		if (!StringUtils.hasText(description)) {
			description = svc;
		}

		Map<String, String> model = new LinkedHashMap<>();
		model.put("groupId", this.description.getGroupId());
		model.put("svc", svc);
		model.put("version", this.description.getVersion());
		model.put("description", description);
		model.put("bootVersion", bootVersion);
		model.put("javaVersion", javaVersion);
		model.put("springCloudVersion", springCloudVersion);
		model.put("packageName", packageName);
		model.put("applicationName", applicationName);
		model.put("dbName", dbName);
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
			// fall through to default
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
		try (InputStream in = DddSixModuleProjectContributor.class.getResourceAsStream(path)) {
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
