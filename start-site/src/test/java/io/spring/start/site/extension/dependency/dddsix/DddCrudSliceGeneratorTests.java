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
	void derivePrefixFromArtifact() {
		assertThat(DddCrudSliceGenerator.derivePrefix("demo-service", "com.example.demo")).isEqualTo("demo");
		assertThat(DddCrudSliceGenerator.derivePrefix("order-svc", "com.example.x")).isEqualTo("order");
		assertThat(DddCrudSliceGenerator.derivePrefix("system", "com.example.user")).isEqualTo("system");
	}

	@Test
	void enhancedTemplateGeneratesUserCrudFiles(@TempDir Path projectRoot) throws Exception {
		GenerationRequestAttributes.EntitySpec user = new GenerationRequestAttributes.EntitySpec("User", "sys_user",
				"postgresql", "mybatis-plus", "用户", true,
				List.of(new GenerationRequestAttributes.FieldSpec("username", "String", true, true, "用户名", true),
						new GenerationRequestAttributes.FieldSpec("email", "String", false, false, "邮箱", true)),
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
				"demo-service-application/src/main/java/com/example/demo/application/service/UserApplicationService.java"))
			.exists();
		assertThat(projectRoot.resolve(
				"demo-service-application/src/main/java/com/example/demo/application/service/impl/UserQueryServiceImpl.java"))
			.exists();
		assertThat(projectRoot.resolve(
				"demo-service-contract/src/main/java/com/example/demo/contract/dto/request/CreateUserRequest.java"))
			.exists();
		assertThat(projectRoot
			.resolve("demo-service-contract/src/main/java/com/example/demo/contract/common/page/PageResult.java"))
			.exists();
		assertThat(projectRoot.resolve(
				"demo-service-contract/src/main/java/com/example/demo/contract/common/id/SnowflakeIdGenerator.java"))
			.exists();
		assertThat(projectRoot.resolve("demo-service-bootstrap/src/main/resources/application-h2.yml")).exists();
		assertThat(projectRoot.resolve(
				"demo-service-infrastructure/src/main/java/com/example/demo/infrastructure/persistence/mapper/UserReadMapper.java"))
			.exists();
		assertThat(projectRoot.resolve("demo-service-infrastructure/src/main/resources/mapper/UserReadMapper.xml"))
			.exists();
		assertThat(projectRoot.resolve(
				"demo-service-application/src/test/java/com/example/demo/application/service/UserApplicationServiceTest.java"))
			.exists();

		String controller = Files.readString(projectRoot.resolve(
				"demo-service-application/src/main/java/com/example/demo/application/controller/UserController.java"));
		assertThat(controller).contains("DeleteMapping").contains("/import").contains("/export");
		assertThat(controller).contains("@Tag").contains("@Operation").contains("@Parameter");
		assertThat(controller).contains("@RequestMapping(UserApiPath.BASE)");
		assertThat(controller).contains("@Validated QueryUserRequest");
		assertThat(controller).doesNotContain("TODO").doesNotContain("UnsupportedOperationException");

		String apiPath = Files.readString(projectRoot
			.resolve("demo-service-contract/src/main/java/com/example/demo/contract/constant/UserApiPath.java"));
		assertThat(apiPath).contains("\"/demo/v1/users\"");

		assertThat(
				projectRoot.resolve("demo-service-domain/src/main/java/com/example/demo/domain/query/PageSlice.java"))
			.exists();

		String queryImpl = Files.readString(projectRoot.resolve(
				"demo-service-application/src/main/java/com/example/demo/application/service/impl/UserQueryServiceImpl.java"));
		assertThat(queryImpl).contains("UserReadMapper")
			.contains("selectSummaryPage")
			.contains("Page.of")
			.doesNotContain(".skip(");

		String appService = Files.readString(projectRoot.resolve(
				"demo-service-application/src/main/java/com/example/demo/application/service/UserApplicationService.java"));
		assertThat(appService).contains("repository.save")
			.contains("idGenerator.nextId()")
			.doesNotContain("UUID")
			.doesNotContain("TODO")
			.doesNotContain("UnsupportedOperationException");

		String repoImpl = Files.readString(projectRoot.resolve(
				"demo-service-infrastructure/src/main/java/com/example/demo/infrastructure/persistence/repository/UserRepositoryImpl.java"));
		assertThat(repoImpl).contains("insert").contains("toDomain").doesNotContain("TODO");
		assertThat(repoImpl).contains("Page.of").contains("selectPage").contains("findPage");

		String po = Files.readString(projectRoot.resolve(
				"demo-service-infrastructure/src/main/java/com/example/demo/infrastructure/persistence/entity/UserPO.java"));
		assertThat(po).contains("IdType.ASSIGN_ID").contains("Boolean deleted");

		String sql = Files.readString(projectRoot
			.resolve("demo-service-bootstrap/src/main/resources/db/migration/V1__01_create_sys_user.sql"));
		assertThat(sql).contains("BOOLEAN").contains("COMMENT ON").contains("FALSE");

		String queryReq = Files.readString(projectRoot.resolve(
				"demo-service-contract/src/main/java/com/example/demo/contract/dto/request/QueryUserRequest.java"));
		assertThat(queryReq).contains("@Min").contains("@Max");

		String statusEnum = Files.readString(projectRoot
			.resolve("demo-service-contract/src/main/java/com/example/demo/contract/enums/UserStatusEnum.java"));
		assertThat(statusEnum).contains("启用").contains("getDescription");

		String ie = Files.readString(projectRoot.resolve(
				"demo-service-application/src/main/java/com/example/demo/application/service/UserImportExportService.java"));
		assertThat(ie).contains("EasyExcel").contains("ExcelRow").contains("doReadSync");

		String createReq = Files.readString(projectRoot.resolve(
				"demo-service-contract/src/main/java/com/example/demo/contract/dto/request/CreateUserRequest.java"));
		assertThat(createReq).contains("@Schema").contains("用户名").contains("@NotBlank");

		String feign = Files.readString(projectRoot
			.resolve("demo-service-feign-client/src/main/java/com/example/demo/feign/UserFeignClient.java"));
		assertThat(feign).contains("page(").contains("create(").contains("update(").contains("delete(");

		assertThat(projectRoot
			.resolve("demo-service-bootstrap/src/main/java/com/example/demo/DemoServiceApplication.java")).exists();
		assertThat(projectRoot
			.resolve("demo-service-feign-client/src/main/java/com/example/demo/feign/UserFeignClient.java")).exists();
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
