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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

import io.spring.start.site.support.GenerationRequestAttributes.ApiFlags;
import io.spring.start.site.support.GenerationRequestAttributes.EntitySpec;
import io.spring.start.site.support.GenerationRequestAttributes.FieldSpec;

/**
 * Generates spring-boot-gen style vertical CRUD slices into a DDD six-module tree.
 *
 * @author start.spring.io China ecosystem
 */
final class DddCrudSliceGenerator {

	private DddCrudSliceGenerator() {
	}

	static void generate(Path projectRoot, String svc, String packageName, List<EntitySpec> entities)
			throws IOException {
		if (entities == null || entities.isEmpty()) {
			return;
		}
		String packagePath = packageName.replace('.', '/');
		int idx = 1;
		for (EntitySpec entity : entities) {
			writeEntitySlice(projectRoot, svc, packageName, packagePath, entity, idx++);
		}
	}

	private static void writeEntitySlice(Path root, String svc, String pkg, String pkgPath, EntitySpec e, int flywayIdx)
			throws IOException {
		String entity = e.name();
		String lower = e.entityLower();
		String table = e.table();
		String desc = e.description();
		String resource = e.resource();
		ApiFlags apis = e.apis();
		List<FieldSpec> fields = e.fields();

		// SQL Flyway
		write(root.resolve(svc + "-bootstrap/src/main/resources/db/migration/V1__" + String.format("%02d", flywayIdx)
				+ "_create_" + table + ".sql"), sql(table, desc, fields));

		// Contract DTOs / constants / enums
		String cBase = svc + "-contract/src/main/java/" + pkgPath + "/contract";
		write(root.resolve(cBase + "/dto/request/Create" + entity + "Request.java"),
				createRequest(pkg, entity, desc, fields));
		write(root.resolve(cBase + "/dto/request/Update" + entity + "Request.java"),
				updateRequest(pkg, entity, desc, fields));
		write(root.resolve(cBase + "/dto/request/Query" + entity + "Request.java"), queryRequest(pkg, entity, desc));
		write(root.resolve(cBase + "/dto/response/" + entity + "DetailResponse.java"),
				detailResponse(pkg, entity, desc, fields));
		write(root.resolve(cBase + "/dto/response/" + entity + "SummaryResponse.java"),
				summaryResponse(pkg, entity, desc, fields));
		write(root.resolve(cBase + "/constant/" + entity + "ApiPath.java"), apiPath(pkg, entity, resource));
		write(root.resolve(cBase + "/enums/" + entity + "StatusEnum.java"), statusEnum(pkg, entity, desc));

		// Domain
		String dBase = svc + "-domain/src/main/java/" + pkgPath + "/domain";
		write(root.resolve(dBase + "/model/" + entity + ".java"), domainEntity(pkg, entity, desc, fields));
		write(root.resolve(dBase + "/model/" + entity + "Status.java"), domainStatus(pkg, entity));
		write(root.resolve(dBase + "/command/Create" + entity + "Command.java"), createCommand(pkg, entity, fields));
		write(root.resolve(dBase + "/command/Update" + entity + "Command.java"), updateCommand(pkg, entity, fields));
		write(root.resolve(dBase + "/repository/" + entity + "Repository.java"), repositoryPort(pkg, entity));
		write(root.resolve(dBase + "/exception/" + entity + "NotFoundException.java"), notFound(pkg, entity, desc));

		// Infrastructure
		String iBase = svc + "-infrastructure/src/main/java/" + pkgPath + "/infrastructure";
		write(root.resolve(iBase + "/persistence/entity/" + entity + "PO.java"), po(pkg, entity, table, fields));
		write(root.resolve(iBase + "/persistence/mapper/" + entity + "Mapper.java"), mapper(pkg, entity));
		write(root.resolve(iBase + "/persistence/repository/" + entity + "RepositoryImpl.java"),
				repositoryImpl(pkg, entity, lower));

		// Application
		String aBase = svc + "-application/src/main/java/" + pkgPath + "/application";
		write(root.resolve(aBase + "/service/" + entity + "ApplicationService.java"),
				appService(pkg, entity, lower, desc, apis));
		write(root.resolve(aBase + "/controller/" + entity + "Controller.java"),
				controller(pkg, entity, lower, desc, apis));
		if (apis.importApi() || apis.exportApi()) {
			write(root.resolve(aBase + "/service/" + entity + "ImportExportService.java"),
					importExportService(pkg, entity, lower, desc, apis));
		}
	}

	private static String sql(String table, String desc, List<FieldSpec> fields) {
		StringBuilder cols = new StringBuilder();
		for (FieldSpec f : fields) {
			cols.append("    ").append(pad(f.column(), 14)).append(f.sqlType());
			if (f.required()) {
				cols.append(" NOT NULL");
			}
			cols.append(",\n");
		}
		StringBuilder uniques = new StringBuilder();
		for (FieldSpec f : fields) {
			if (f.unique()) {
				uniques.append("CREATE UNIQUE INDEX uk_")
					.append(table)
					.append('_')
					.append(f.column())
					.append(" ON ")
					.append(table)
					.append(" (")
					.append(f.column())
					.append(");\n");
			}
		}
		return """
				-- %s
				CREATE TABLE %s (
				    id             VARCHAR(64)  PRIMARY KEY,
				%s    status         VARCHAR(32)  NOT NULL,
				    version        INT          NOT NULL DEFAULT 0,
				    deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
				    create_time    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
				    update_time    TIMESTAMPTZ
				);
				%sCREATE INDEX idx_%s_status ON %s (status);
				""".formatted(desc, table, cols, uniques, table, table);
	}

	private static String createRequest(String pkg, String entity, String desc, List<FieldSpec> fields) {
		return """
				package %s.contract.dto.request;

				%s
				/**
				 * 创建%s请求
				 */
				public record Create%sRequest(
				%s) {
				}
				""".formatted(pkg, importsForFields(fields), desc, entity, recordFields(fields, true));
	}

	private static String updateRequest(String pkg, String entity, String desc, List<FieldSpec> fields) {
		return """
				package %s.contract.dto.request;

				%s
				/**
				 * 更新%s请求
				 */
				public record Update%sRequest(
				%s) {
				}
				""".formatted(pkg, importsForFields(fields), desc, entity, recordFields(fields, false));
	}

	private static String queryRequest(String pkg, String entity, String desc) {
		return """
				package %s.contract.dto.request;

				/**
				 * 分页查询%s请求
				 */
				public record Query%sRequest(String keyword, Integer page, Integer size) {
					public int pageOrDefault() {
						return page == null || page < 1 ? 1 : page;
					}

					public int sizeOrDefault() {
						return size == null || size < 1 ? 20 : Math.min(size, 200);
					}
				}
				""".formatted(pkg, desc, entity);
	}

	private static String detailResponse(String pkg, String entity, String desc, List<FieldSpec> fields) {
		String extra = fields.stream()
			.map((f) -> "		" + f.type() + " " + f.name())
			.collect(Collectors.joining(",\n"));
		if (!extra.isEmpty()) {
			extra = ",\n" + extra;
		}
		return """
				package %s.contract.dto.response;

				%s
				/**
				 * %s详情
				 */
				public record %sDetailResponse(
						String id,
						String status%s,
						long version) {
				}
				""".formatted(pkg, importsForFields(fields), desc, entity, extra);
	}

	private static String summaryResponse(String pkg, String entity, String desc, List<FieldSpec> fields) {
		List<FieldSpec> brief = fields.stream().limit(3).toList();
		String extra = brief.stream()
			.map((f) -> "		" + f.type() + " " + f.name())
			.collect(Collectors.joining(",\n"));
		if (!extra.isEmpty()) {
			extra = ",\n" + extra;
		}
		return """
				package %s.contract.dto.response;

				%s
				/**
				 * %s摘要
				 */
				public record %sSummaryResponse(
						String id,
						String status%s) {
				}
				""".formatted(pkg, importsForFields(brief), desc, entity, extra);
	}

	private static String apiPath(String pkg, String entity, String resource) {
		return """
				package %s.contract.constant;

				/**
				 * %s API 路径常量
				 */
				public final class %sApiPath {

					public static final String BASE = "/api/v1/%s";

					private %sApiPath() {
					}

				}
				""".formatted(pkg, entity, entity, resource, entity);
	}

	private static String statusEnum(String pkg, String entity, String desc) {
		return """
				package %s.contract.enums;

				/**
				 * %s状态
				 */
				public enum %sStatusEnum {

					ACTIVE, DISABLED

				}
				""".formatted(pkg, desc, entity);
	}

	private static String domainEntity(String pkg, String entity, String desc, List<FieldSpec> fields) {
		String fieldDecls = fields.stream()
			.map((f) -> "	private " + f.type() + " " + f.name() + ";")
			.collect(Collectors.joining("\n"));
		String assigns = fields.stream()
			.map((f) -> "		entity." + f.name() + " = command." + f.name() + "();")
			.collect(Collectors.joining("\n"));
		String getters = fields.stream()
			.map((f) -> "	public " + f.type() + " get" + f.getter() + "() {\n		return " + f.name() + ";\n	}\n")
			.collect(Collectors.joining("\n"));
		String updateBody = fields.stream()
			.map((f) -> "		if (command." + f.name() + "() != null) {\n			this." + f.name() + " = command."
					+ f.name() + "();\n		}")
			.collect(Collectors.joining("\n"));
		return """
				package %s.domain.model;

				import %s.domain.command.Create%sCommand;
				import %s.domain.command.Update%sCommand;

				import java.time.OffsetDateTime;

				%s
				/**
				 * %s聚合根
				 */
				public class %s {

					private String id;
				%s
					private %sStatus status;
					private long version;
					private OffsetDateTime createTime;
					private OffsetDateTime updateTime;

					private %s() {
					}

					public static %s create(Create%sCommand command) {
						%s entity = new %s();
						entity.id = command.id();
				%s
						entity.status = %sStatus.ACTIVE;
						entity.createTime = OffsetDateTime.now();
						return entity;
					}

					public void update(Update%sCommand command) {
				%s
						this.updateTime = OffsetDateTime.now();
					}

					public String getId() {
						return id;
					}

				%s
					public %sStatus getStatus() {
						return status;
					}

					public long getVersion() {
						return version;
					}

					public OffsetDateTime getCreateTime() {
						return createTime;
					}

					public OffsetDateTime getUpdateTime() {
						return updateTime;
					}

				}
				""".formatted(pkg, pkg, entity, pkg, entity, importsForFields(fields), desc, entity, fieldDecls, entity,
				entity, entity, entity, entity, entity, assigns, entity, entity, updateBody, getters, entity);
	}

	private static String domainStatus(String pkg, String entity) {
		return """
				package %s.domain.model;

				/**
				 * %s 状态
				 */
				public enum %sStatus {

					ACTIVE, DISABLED

				}
				""".formatted(pkg, entity, entity);
	}

	private static String createCommand(String pkg, String entity, List<FieldSpec> fields) {
		String body = "		String id" + (fields.isEmpty() ? "" : ",\n"
				+ fields.stream().map((f) -> "		" + f.type() + " " + f.name()).collect(Collectors.joining(",\n")));
		return """
				package %s.domain.command;

				%s
				public record Create%sCommand(
				%s) {
				}
				""".formatted(pkg, importsForFields(fields), entity, body);
	}

	private static String updateCommand(String pkg, String entity, List<FieldSpec> fields) {
		return """
				package %s.domain.command;

				%s
				public record Update%sCommand(
				%s) {
				}
				""".formatted(pkg, importsForFields(fields), entity, recordFields(fields, false));
	}

	private static String repositoryPort(String pkg, String entity) {
		return """
				package %s.domain.repository;

				import %s.domain.model.%s;

				import java.util.Optional;

				public interface %sRepository {

					%s save(%s aggregate);

					Optional<%s> findById(String id);

					void deleteById(String id);

				}
				""".formatted(pkg, pkg, entity, entity, entity, entity, entity);
	}

	private static String notFound(String pkg, String entity, String desc) {
		return """
				package %s.domain.exception;

				public class %sNotFoundException extends RuntimeException {

					public %sNotFoundException(String id) {
						super("%s not found: " + id);
					}

				}
				""".formatted(pkg, entity, entity, desc);
	}

	private static String po(String pkg, String entity, String table, List<FieldSpec> fields) {
		String fieldDecls = fields.stream()
			.map((f) -> "	private " + f.type() + " " + f.name() + ";")
			.collect(Collectors.joining("\n"));
		return """
				package %s.infrastructure.persistence.entity;

				import com.baomidou.mybatisplus.annotation.TableId;
				import com.baomidou.mybatisplus.annotation.TableLogic;
				import com.baomidou.mybatisplus.annotation.TableName;
				import com.baomidou.mybatisplus.annotation.Version;

				import java.time.OffsetDateTime;

				%s
				/**
				 * %s 持久化对象
				 */
				@TableName("%s")
				public class %sPO {

					@TableId
					private String id;
				%s
					private String status;
					@Version
					private Long version;
					@TableLogic
					private Boolean deleted;
					private OffsetDateTime createTime;
					private OffsetDateTime updateTime;

					public String getId() {
						return id;
					}

					public void setId(String id) {
						this.id = id;
					}

					// remaining getters/setters omitted for brevity — fill via IDE or Lombok in real projects

				}
				""".formatted(pkg, importsForFields(fields), entity, table, entity, fieldDecls);
	}

	private static String mapper(String pkg, String entity) {
		return """
				package %s.infrastructure.persistence.mapper;

				import com.baomidou.mybatisplus.core.mapper.BaseMapper;
				import %s.infrastructure.persistence.entity.%sPO;

				import org.apache.ibatis.annotations.Mapper;

				@Mapper
				public interface %sMapper extends BaseMapper<%sPO> {
				}
				""".formatted(pkg, pkg, entity, entity, entity);
	}

	private static String repositoryImpl(String pkg, String entity, String lower) {
		return """
				package %s.infrastructure.persistence.repository;

				import %s.domain.model.%s;
				import %s.domain.repository.%sRepository;
				import %s.infrastructure.persistence.entity.%sPO;
				import %s.infrastructure.persistence.mapper.%sMapper;

				import java.util.Optional;

				import org.springframework.stereotype.Repository;

				@Repository
				public class %sRepositoryImpl implements %sRepository {

					private final %sMapper %sMapper;

					public %sRepositoryImpl(%sMapper %sMapper) {
						this.%sMapper = %sMapper;
					}

					@Override
					public %s save(%s aggregate) {
						// TODO map aggregate <-> PO
						return aggregate;
					}

					@Override
					public Optional<%s> findById(String id) {
						%sPO po = %sMapper.selectById(id);
						return Optional.empty(); // TODO map PO -> domain
					}

					@Override
					public void deleteById(String id) {
						%sMapper.deleteById(id);
					}

				}
				""".formatted(pkg, pkg, entity, pkg, entity, pkg, entity, pkg, entity, entity, entity, entity, lower,
				entity, entity, lower, lower, lower, entity, entity, entity, entity, lower, lower);
	}

	private static String appService(String pkg, String entity, String lower, String desc, ApiFlags apis) {
		StringBuilder methods = new StringBuilder();
		if (apis.create()) {
			methods.append("""

						public %sDetailResponse create(Create%sRequest request) {
							// TODO assemble command + persist
							throw new UnsupportedOperationException("TODO create %s");
						}
					""".formatted(entity, entity, desc));
		}
		if (apis.update()) {
			methods.append("""

						public %sDetailResponse update(String id, Update%sRequest request) {
							throw new UnsupportedOperationException("TODO update %s");
						}
					""".formatted(entity, entity, desc));
		}
		if (apis.delete()) {
			methods.append("""

						public void delete(String id) {
							repository.deleteById(id);
						}
					""");
		}
		if (apis.detail()) {
			methods.append("""

						public %sDetailResponse detail(String id) {
							throw new UnsupportedOperationException("TODO detail %s");
						}
					""".formatted(entity, desc));
		}
		if (apis.page()) {
			methods.append("""

						public Object page(Query%sRequest query) {
							throw new UnsupportedOperationException("TODO page %s");
						}
					""".formatted(entity, desc));
		}
		return """
				package %s.application.service;

				import %s.contract.dto.request.Create%sRequest;
				import %s.contract.dto.request.Query%sRequest;
				import %s.contract.dto.request.Update%sRequest;
				import %s.contract.dto.response.%sDetailResponse;
				import %s.domain.repository.%sRepository;

				import org.springframework.stereotype.Service;
				import org.springframework.transaction.annotation.Transactional;

				/**
				 * %s 应用服务
				 */
				@Service
				@Transactional
				public class %sApplicationService {

					private final %sRepository repository;

					public %sApplicationService(%sRepository repository) {
						this.repository = repository;
					}
				%s
				}
				""".formatted(pkg, pkg, entity, pkg, entity, pkg, entity, pkg, entity, pkg, entity, desc, entity,
				entity, entity, entity, methods);
	}

	private static String controller(String pkg, String entity, String lower, String desc, ApiFlags apis) {
		StringBuilder methods = new StringBuilder();
		StringBuilder imports = new StringBuilder();
		if (apis.page()) {
			methods.append("""

						@GetMapping
						public Object page(Query%sRequest query) {
							return %sApplicationService.page(query);
						}
					""".formatted(entity, lower));
		}
		if (apis.detail()) {
			methods.append("""

						@GetMapping("/{id}")
						public %sDetailResponse detail(@PathVariable String id) {
							return %sApplicationService.detail(id);
						}
					""".formatted(entity, lower));
		}
		if (apis.create()) {
			methods.append("""

						@PostMapping
						@ResponseStatus(HttpStatus.CREATED)
						public %sDetailResponse create(@RequestBody Create%sRequest request) {
							return %sApplicationService.create(request);
						}
					""".formatted(entity, entity, lower));
		}
		if (apis.update()) {
			methods.append("""

						@PutMapping("/{id}")
						public %sDetailResponse update(@PathVariable String id, @RequestBody Update%sRequest request) {
							return %sApplicationService.update(id, request);
						}
					""".formatted(entity, entity, lower));
		}
		if (apis.delete()) {
			methods.append("""

						@DeleteMapping("/{id}")
						@ResponseStatus(HttpStatus.NO_CONTENT)
						public void delete(@PathVariable String id) {
							%sApplicationService.delete(id);
						}
					""".formatted(lower));
		}
		if (apis.importApi() || apis.exportApi()) {
			methods.append("""

						// import/export stubs — see %sImportExportService
					""".formatted(entity));
			if (apis.importApi()) {
				methods.append("""

							@PostMapping("/import")
							public Object importData() {
								return importExportService.importData(null);
							}
						""");
			}
			if (apis.exportApi()) {
				methods.append("""

							@GetMapping("/export")
							public Object exportData() {
								return importExportService.exportData();
							}
						""");
			}
		}
		String ieField = (apis.importApi() || apis.exportApi())
				? "\n	private final " + entity + "ImportExportService importExportService;\n" : "";
		String ieCtor = (apis.importApi() || apis.exportApi())
				? ",\n			" + entity + "ImportExportService importExportService" : "";
		String ieAssign = (apis.importApi() || apis.exportApi())
				? "\n		this.importExportService = importExportService;" : "";
		String ieImport = (apis.importApi() || apis.exportApi())
				? "import " + pkg + ".application.service." + entity + "ImportExportService;\n" : "";
		return """
				package %s.application.controller;

				import %s.application.service.%sApplicationService;
				%simport %s.contract.constant.%sApiPath;
				import %s.contract.dto.request.Create%sRequest;
				import %s.contract.dto.request.Query%sRequest;
				import %s.contract.dto.request.Update%sRequest;
				import %s.contract.dto.response.%sDetailResponse;

				import org.springframework.http.HttpStatus;
				import org.springframework.web.bind.annotation.DeleteMapping;
				import org.springframework.web.bind.annotation.GetMapping;
				import org.springframework.web.bind.annotation.PathVariable;
				import org.springframework.web.bind.annotation.PostMapping;
				import org.springframework.web.bind.annotation.PutMapping;
				import org.springframework.web.bind.annotation.RequestBody;
				import org.springframework.web.bind.annotation.RequestMapping;
				import org.springframework.web.bind.annotation.ResponseStatus;
				import org.springframework.web.bind.annotation.RestController;

				/**
				 * %s接口
				 */
				@RestController
				@RequestMapping(%sApiPath.BASE)
				public class %sController {

					private final %sApplicationService %sApplicationService;%s

					public %sController(%sApplicationService %sApplicationService%s) {
						this.%sApplicationService = %sApplicationService;%s
					}
				%s
				}
				""".formatted(pkg, pkg, entity, ieImport, pkg, entity, pkg, entity, pkg, entity, pkg, entity, pkg,
				entity, desc, entity, entity, entity, lower, ieField, entity, entity, lower, ieCtor, lower, lower,
				ieAssign, methods);
	}

	private static String importExportService(String pkg, String entity, String lower, String desc, ApiFlags apis) {
		StringBuilder methods = new StringBuilder();
		if (apis.importApi()) {
			methods.append("""

						/**
						 * EasyExcel-oriented import stub.
						 */
						public Object importData(Object upload) {
							// TODO integrate EasyExcel read + batch create
							throw new UnsupportedOperationException("TODO import %s via EasyExcel");
						}
					""".formatted(desc));
		}
		if (apis.exportApi()) {
			methods.append("""

						/**
						 * EasyExcel-oriented export stub.
						 */
						public Object exportData() {
							// TODO integrate EasyExcel write
							throw new UnsupportedOperationException("TODO export %s via EasyExcel");
						}
					""".formatted(desc));
		}
		return """
				package %s.application.service;

				import org.springframework.stereotype.Service;

				/**
				 * %s 导入导出（EasyExcel 占位）
				 */
				@Service
				public class %sImportExportService {
				%s
				}
				""".formatted(pkg, desc, entity, methods);
	}

	private static String recordFields(List<FieldSpec> fields, boolean includeRequiredHints) {
		if (fields.isEmpty()) {
			return "		String placeholder";
		}
		return fields.stream().map((f) -> "		" + f.type() + " " + f.name()).collect(Collectors.joining(",\n"));
	}

	private static String importsForFields(List<FieldSpec> fields) {
		StringBuilder sb = new StringBuilder();
		boolean bigDecimal = fields.stream().anyMatch((f) -> "BigDecimal".equals(f.type()));
		boolean localDate = fields.stream().anyMatch((f) -> "LocalDate".equals(f.type()));
		boolean localDateTime = fields.stream().anyMatch((f) -> "LocalDateTime".equals(f.type()));
		if (bigDecimal) {
			sb.append("import java.math.BigDecimal;\n");
		}
		if (localDate) {
			sb.append("import java.time.LocalDate;\n");
		}
		if (localDateTime) {
			sb.append("import java.time.LocalDateTime;\n");
		}
		if (!sb.isEmpty()) {
			sb.append('\n');
		}
		return sb.toString();
	}

	private static String pad(String s, int n) {
		if (s.length() >= n) {
			return s;
		}
		return s + " ".repeat(n - s.length());
	}

	private static void write(Path path, String content) throws IOException {
		Path parent = path.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		Files.writeString(path, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
	}

}
