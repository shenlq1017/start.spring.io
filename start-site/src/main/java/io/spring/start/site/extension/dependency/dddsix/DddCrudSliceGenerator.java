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
import java.util.Locale;
import java.util.stream.Collectors;

import io.spring.start.site.support.GenerationRequestAttributes.ApiFlags;
import io.spring.start.site.support.GenerationRequestAttributes.EntitySpec;
import io.spring.start.site.support.GenerationRequestAttributes.FieldSpec;

/**
 * Generates DDD six-module vertical CRUD slices (guided by spring-boot-gen patterns: real
 * ApplicationService/QueryService, swagger on APIs, {@code /{prefix}/v1/{resource}}
 * paths, PageResult, Problem Details — adapted for Initializr emission).
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
		String prefix = derivePrefix(svc, packageName);
		writeShared(projectRoot, svc, packageName, packagePath, entities.get(0));
		int idx = 1;
		for (EntitySpec entity : entities) {
			writeEntitySlice(projectRoot, svc, packageName, packagePath, prefix, entity, idx++);
		}
	}

	/**
	 * Module/menu segment: artifact without {@code -service/-svc}, else last package
	 * segment.
	 * @param artifactId maven artifact id
	 * @param packageName base package name
	 * @return kebab-case prefix used in API paths
	 */
	static String derivePrefix(String artifactId, String packageName) {
		String s = (artifactId != null) ? artifactId.trim() : "";
		if (s.endsWith("-service")) {
			s = s.substring(0, s.length() - "-service".length());
		}
		else if (s.endsWith("-svc")) {
			s = s.substring(0, s.length() - "-svc".length());
		}
		if (!s.isBlank()) {
			return s.toLowerCase(Locale.ROOT).replace('_', '-');
		}
		String pkg = (packageName != null) ? packageName : "demo";
		int dot = pkg.lastIndexOf('.');
		return ((dot >= 0) ? pkg.substring(dot + 1) : pkg).toLowerCase(Locale.ROOT);
	}

	/**
	 * Plural resource segment in kebab-case.
	 * @param e entity spec
	 * @return resource path segment
	 */
	static String resourcePath(EntitySpec e) {
		String raw = e.resource();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			if (Character.isUpperCase(c) && i > 0) {
				sb.append('-');
			}
			sb.append(Character.toLowerCase(c));
		}
		return sb.toString();
	}

	private static void writeShared(Path root, String svc, String pkg, String pkgPath, EntitySpec first)
			throws IOException {
		// Module-local "common" under contract (no platform common-core required)
		String cBase = svc + "-contract/src/main/java/" + pkgPath + "/contract";
		write(root.resolve(cBase + "/common/page/PageResult.java"), pageResult(pkg));
		write(root.resolve(cBase + "/common/id/SnowflakeIdGenerator.java"), snowflakeIdGenerator(pkg));

		String dShared = svc + "-domain/src/main/java/" + pkgPath + "/domain";
		write(root.resolve(dShared + "/query/PageSlice.java"), pageSlice(pkg));
		write(root.resolve(dShared + "/exception/BusinessException.java"), businessException(pkg));

		String aBase = svc + "-application/src/main/java/" + pkgPath + "/application";
		write(root.resolve(aBase + "/advice/GlobalExceptionHandler.java"), globalAdvice(pkg, first));

		String iBase = svc + "-infrastructure/src/main/java/" + pkgPath + "/infrastructure";
		write(root.resolve(iBase + "/config/MyBatisPlusConfig.java"), mybatisConfig(pkg));

		Path bootstrapRes = root.resolve(svc + "-bootstrap/src/main/resources");
		Files.createDirectories(bootstrapRes);
		write(bootstrapRes.resolve("application-h2.yml"), h2ProfileYml());
		write(bootstrapRes.resolve("schema-h2.sql"),
				"-- H2 smoke schema (Flyway disabled on h2 profile; BOOLEAN/TIMESTAMP mapped from PG)\n");
	}

	private static void writeEntitySlice(Path root, String svc, String pkg, String pkgPath, String prefix, EntitySpec e,
			int flywayIdx) throws IOException {
		String entity = e.name();
		String lower = e.entityLower();
		String table = e.table();
		String desc = e.description();
		String resource = resourcePath(e);
		ApiFlags apis = e.apis();
		List<FieldSpec> fields = e.fields();
		boolean swagger = e.swagger();

		String ddl = sql(table, desc, fields);
		write(root.resolve(svc + "-bootstrap/src/main/resources/db/migration/V1__" + String.format("%02d", flywayIdx)
				+ "_create_" + table + ".sql"), ddl);
		Path h2Schema = root.resolve(svc + "-bootstrap/src/main/resources/schema-h2.sql");
		String h2Ddl = ddl.replace("TIMESTAMPTZ", "TIMESTAMP")
			.replace("NOW()", "CURRENT_TIMESTAMP")
			.replace("DEFAULT FALSE", "DEFAULT FALSE")
			.replace("DEFAULT TRUE", "DEFAULT TRUE");
		Files.writeString(h2Schema, h2Ddl + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE,
				StandardOpenOption.APPEND);

		String cBase = svc + "-contract/src/main/java/" + pkgPath + "/contract";
		write(root.resolve(cBase + "/dto/request/Create" + entity + "Request.java"),
				createRequest(pkg, entity, desc, fields, swagger));
		write(root.resolve(cBase + "/dto/request/Update" + entity + "Request.java"),
				updateRequest(pkg, entity, desc, fields, swagger));
		write(root.resolve(cBase + "/dto/request/Query" + entity + "Request.java"),
				queryRequest(pkg, entity, desc, swagger));
		write(root.resolve(cBase + "/dto/response/" + entity + "DetailResponse.java"),
				detailResponse(pkg, entity, desc, fields, swagger));
		write(root.resolve(cBase + "/dto/response/" + entity + "SummaryResponse.java"),
				summaryResponse(pkg, entity, desc, fields, swagger));
		write(root.resolve(cBase + "/constant/" + entity + "ApiPath.java"), apiPath(pkg, entity, prefix, resource));
		write(root.resolve(cBase + "/constant/" + entity + "ServiceName.java"), serviceName(pkg, entity, svc));
		write(root.resolve(cBase + "/enums/" + entity + "StatusEnum.java"), statusEnum(pkg, entity, desc));

		String fBase = svc + "-feign-client/src/main/java/" + pkgPath + "/feign";
		write(root.resolve(fBase + "/" + entity + "FeignClient.java"), feignClient(pkg, entity, desc, swagger));
		write(root.resolve(fBase + "/" + entity + "FeignFallbackFactory.java"), feignFallback(pkg, entity, desc));

		String dBase = svc + "-domain/src/main/java/" + pkgPath + "/domain";
		write(root.resolve(dBase + "/model/" + entity + ".java"), domainEntity(pkg, entity, desc, fields));
		write(root.resolve(dBase + "/model/" + entity + "Status.java"), domainStatus(pkg, entity));
		write(root.resolve(dBase + "/command/Create" + entity + "Command.java"), createCommand(pkg, entity, fields));
		write(root.resolve(dBase + "/command/Update" + entity + "Command.java"), updateCommand(pkg, entity, fields));
		write(root.resolve(dBase + "/query/" + entity + "Query.java"), domainQuery(pkg, entity));
		write(root.resolve(dBase + "/repository/" + entity + "Repository.java"), repositoryPort(pkg, entity));
		write(root.resolve(dBase + "/exception/" + entity + "NotFoundException.java"), notFound(pkg, entity, desc));

		String iBase = svc + "-infrastructure/src/main/java/" + pkgPath + "/infrastructure";
		write(root.resolve(iBase + "/persistence/entity/" + entity + "PO.java"), po(pkg, entity, table, fields));
		write(root.resolve(iBase + "/persistence/mapper/" + entity + "Mapper.java"), mapper(pkg, entity));
		write(root.resolve(iBase + "/persistence/mapper/" + entity + "ReadMapper.java"), readMapper(pkg, entity));
		write(root.resolve(svc + "-infrastructure/src/main/resources/mapper/" + entity + "ReadMapper.xml"),
				readMapperXml(pkg, entity, table, fields));
		write(root.resolve(iBase + "/persistence/converter/" + entity + "Converter.java"),
				converter(pkg, entity, fields));
		write(root.resolve(iBase + "/persistence/repository/" + entity + "RepositoryImpl.java"),
				repositoryImpl(pkg, entity, lower));

		String aBase = svc + "-application/src/main/java/" + pkgPath + "/application";
		write(root.resolve(aBase + "/assembler/" + entity + "Assembler.java"), assembler(pkg, entity, fields));
		write(root.resolve(aBase + "/service/" + entity + "ApplicationService.java"),
				appService(pkg, entity, lower, desc, apis));
		write(root.resolve(aBase + "/service/" + entity + "QueryService.java"), queryService(pkg, entity));
		write(root.resolve(aBase + "/service/impl/" + entity + "QueryServiceImpl.java"),
				queryServiceImpl(pkg, entity, lower, desc));
		write(root.resolve(aBase + "/controller/" + entity + "Controller.java"),
				controller(pkg, entity, lower, desc, apis, swagger));
		if (apis.importApi() || apis.exportApi()) {
			write(root.resolve(aBase + "/service/" + entity + "ImportExportService.java"),
					importExportService(pkg, entity, desc, apis, fields));
		}
		if (apis.create()) {
			write(root.resolve(svc + "-application/src/test/java/" + pkgPath + "/application/service/" + entity
					+ "ApplicationServiceTest.java"), applicationServiceTest(pkg, entity, lower, fields));
		}
	}

	// ----- shared -----

	private static String pageResult(String pkg) {
		return """
				package %s.contract.common.page;

				import java.util.List;

				/**
				 * Unified page payload (module-local common under contract — no platform common-core).
				 * Controllers must return this type; never expose MyBatis-Plus IPage.
				 */
				public record PageResult<T>(List<T> records, long total, long current, long size) {

					public static <T> PageResult<T> of(List<T> records, long total, long current, long size) {
						return new PageResult<>(records, total, current, size);
					}

				}
				""".formatted(pkg);
	}

	private static String snowflakeIdGenerator(String pkg) {
		return """
				package %s.contract.common.id;

				import java.util.concurrent.atomic.AtomicLong;

				/**
				 * Snowflake ID generator (no synchronized — virtual-thread friendly).
				 * Production: assign workerId via instance index; default workerId=1 for single-node.
				 */
				public final class SnowflakeIdGenerator {

					private static final long EPOCH = 1700000000000L;
					private static final long SEQUENCE_BITS = 12L;
					private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

					private final long workerId;
					private final AtomicLong lastTimestamp = new AtomicLong(-1L);
					private final AtomicLong sequence = new AtomicLong(0L);

					public SnowflakeIdGenerator() {
						this(1L);
					}

					public SnowflakeIdGenerator(long workerId) {
						this.workerId = workerId & 0x1FL;
					}

					public String nextId() {
						long timestamp = System.currentTimeMillis();
						while (true) {
							long last = lastTimestamp.get();
							long seq;
							if (timestamp == last) {
								seq = sequence.incrementAndGet() & SEQUENCE_MASK;
								if (seq == 0L) {
									timestamp = nextMillis(last);
									continue;
								}
							}
							else {
								sequence.set(0L);
								seq = 0L;
								if (!lastTimestamp.compareAndSet(last, timestamp)) {
									continue;
								}
							}
							long id = ((timestamp - EPOCH) << 22) | (workerId << 12) | seq;
							return String.valueOf(id);
						}
					}

					private long nextMillis(long last) {
						long timestamp = System.currentTimeMillis();
						while (timestamp <= last) {
							timestamp = System.currentTimeMillis();
						}
						return timestamp;
					}

				}
				""".formatted(pkg);
	}

	private static String businessException(String pkg) {
		return """
				package %s.domain.exception;

				/**
				 * Business exception base — errorType feeds Problem Detail type URI; status is HTTP code.
				 */
				public abstract class BusinessException extends RuntimeException {

					protected BusinessException(String message) {
						super(message);
					}

					public abstract String errorType();

					public abstract int status();

				}
				""".formatted(pkg);
	}

	private static String pageSlice(String pkg) {
		return """
				package %s.domain.query;

				import java.util.List;

				/**
				 * Domain-layer page slice (no MyBatis-Plus IPage leak into domain).
				 */
				public record PageSlice<T>(List<T> records, long total) {

					public static <T> PageSlice<T> of(List<T> records, long total) {
						return new PageSlice<>(records, total);
					}

				}
				""".formatted(pkg);
	}

	private static String globalAdvice(String pkg, EntitySpec first) {
		String entity = first.name();
		return """
				package %s.application.advice;

				import %s.domain.exception.BusinessException;
				import %s.domain.exception.%sNotFoundException;

				import java.net.URI;
				import java.time.Instant;

				import org.springframework.http.HttpStatus;
				import org.springframework.http.ProblemDetail;
				import org.springframework.web.bind.MethodArgumentNotValidException;
				import org.springframework.web.bind.annotation.ExceptionHandler;
				import org.springframework.web.bind.annotation.RestControllerAdvice;
				import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
				import org.springframework.http.HttpHeaders;
				import org.springframework.http.HttpStatusCode;
				import org.springframework.http.ResponseEntity;
				import org.springframework.lang.Nullable;
				import org.springframework.web.context.request.WebRequest;

				/**
				 * Problem Details (RFC 7807) — no global ApiResponse/R wrapper.
				 * Maps BusinessException hierarchy + validation failures.
				 */
				@RestControllerAdvice
				public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

					private static final String ERROR_TYPE_BASE = "https://api.example.com/errors/";

					@ExceptionHandler(BusinessException.class)
					public ProblemDetail handleBusiness(BusinessException ex) {
						HttpStatus status = HttpStatus.resolve(ex.status());
						if (status == null) {
							status = HttpStatus.BAD_REQUEST;
						}
						ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
						detail.setType(URI.create(ERROR_TYPE_BASE + ex.errorType()));
						detail.setTitle("Business error");
						detail.setProperty("timestamp", Instant.now());
						return detail;
					}

					@ExceptionHandler(%sNotFoundException.class)
					public ProblemDetail handleNotFound(%sNotFoundException ex) {
						ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
						detail.setType(URI.create(ERROR_TYPE_BASE + ex.errorType()));
						detail.setTitle("Resource not found");
						detail.setProperty("id", ex.getId());
						detail.setProperty("timestamp", Instant.now());
						return detail;
					}

					@Override
					@Nullable
					protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
							HttpHeaders headers, HttpStatusCode status, WebRequest request) {
						ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
								"Validation failed");
						detail.setType(URI.create(ERROR_TYPE_BASE + "validation"));
						detail.setTitle("Validation failed");
						detail.setProperty("timestamp", Instant.now());
						detail.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
								.map(fe -> fe.getField() + ": " + fe.getDefaultMessage()).toList());
						return ResponseEntity.badRequest().body(detail);
					}

				}
				""".formatted(pkg, pkg, pkg, entity, entity, entity);
	}

	private static String mybatisConfig(String pkg) {
		return """
				package %s.infrastructure.config;

				import com.baomidou.mybatisplus.annotation.DbType;
				import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
				import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
				import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
				import %s.contract.common.id.SnowflakeIdGenerator;

				import org.springframework.context.annotation.Bean;
				import org.springframework.context.annotation.Configuration;

				@Configuration
				public class MyBatisPlusConfig {

					@Bean
					public MybatisPlusInterceptor mybatisPlusInterceptor() {
						MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
						interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
						interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
						return interceptor;
					}

					@Bean
					public SnowflakeIdGenerator snowflakeIdGenerator() {
						return new SnowflakeIdGenerator();
					}

				}
				""".formatted(pkg, pkg);
	}

	private static String h2ProfileYml() {
		return """
				# Local / smoke profile — activate with: --spring.profiles.active=h2
				# Default (no profile): PostgreSQL + Flyway (see application.yml).
				# H2: Flyway OFF; schema-h2.sql applies PG→H2 mapped DDL (TIMESTAMPTZ→TIMESTAMP, BOOLEAN kept).
				spring:
				  datasource:
				    url: jdbc:h2:mem:ddd_smoke;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1
				    username: sa
				    password:
				    driver-class-name: org.h2.Driver
				  flyway:
				    enabled: false
				  sql:
				    init:
				      mode: always
				      schema-locations: classpath:schema-h2.sql

				mybatis-plus:
				  mapper-locations: classpath*:mapper/*.xml
				  configuration:
				    map-underscore-to-camel-case: true
				  global-config:
				    db-config:
				      logic-delete-field: deleted
				      logic-delete-value: true
				      logic-not-delete-value: false
				""";
	}

	// ----- SQL / contract -----

	private static String sql(String table, String desc, List<FieldSpec> fields) {
		StringBuilder cols = new StringBuilder();
		StringBuilder comments = new StringBuilder();
		comments.append("COMMENT ON TABLE  ").append(table).append(" IS '").append(esc(desc)).append("表';\n");
		comments.append("COMMENT ON COLUMN ").append(table).append(".id IS '主键（Snowflake）';\n");
		for (FieldSpec f : fields) {
			cols.append("    ").append(pad(f.column(), 14)).append(f.sqlType());
			if (f.required()) {
				cols.append(" NOT NULL");
			}
			cols.append(",\n");
			String cmt = f.schemaDescription();
			comments.append("COMMENT ON COLUMN ")
				.append(table)
				.append(".")
				.append(f.column())
				.append(" IS '")
				.append(esc(cmt))
				.append("';\n");
		}
		comments.append("COMMENT ON COLUMN ").append(table).append(".status IS '状态';\n");
		comments.append("COMMENT ON COLUMN ").append(table).append(".version IS '乐观锁版本号';\n");
		comments.append("COMMENT ON COLUMN ").append(table).append(".deleted IS '删除标记';\n");
		comments.append("COMMENT ON COLUMN ").append(table).append(".create_time IS '创建时间';\n");
		comments.append("COMMENT ON COLUMN ").append(table).append(".update_time IS '更新时间';\n");
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
				%s""".formatted(desc, table, cols, uniques, table, table, comments);
	}

	private static String createRequest(String pkg, String entity, String desc, List<FieldSpec> fields,
			boolean swagger) {
		return """
				package %s.contract.dto.request;

				%s
				/**
				 * 创建%s请求
				 */
				%s
				public record Create%sRequest(
				%s) {
				}
				""".formatted(pkg, dtoImports(fields, swagger, true), desc, schemaType(swagger, "创建" + desc + "请求"),
				entity, validatedRecordFields(fields, true, swagger));
	}

	private static String updateRequest(String pkg, String entity, String desc, List<FieldSpec> fields,
			boolean swagger) {
		return """
				package %s.contract.dto.request;

				%s
				/**
				 * 更新%s请求
				 */
				%s
				public record Update%sRequest(
				%s) {
				}
				""".formatted(pkg, dtoImports(fields, swagger, true), desc, schemaType(swagger, "更新" + desc + "请求"),
				entity, validatedRecordFields(fields, false, swagger));
	}

	private static String queryRequest(String pkg, String entity, String desc, boolean swagger) {
		String schema = swagger ? """
				import io.swagger.v3.oas.annotations.media.Schema;

				""" : "";
		String ann = swagger ? "@Schema(description = \"%s查询请求\")\n".formatted(esc(desc)) : "";
		String kw = swagger ? "@Schema(description = \"关键字\") String keyword" : "String keyword";
		String st = swagger ? "@Schema(description = \"状态\") String status" : "String status";
		String cur = swagger ? "@Min(1) @Schema(description = \"页码\", defaultValue = \"1\") long current"
				: "@Min(1) long current";
		String size = swagger ? "@Min(1) @Max(200) @Schema(description = \"每页大小\", defaultValue = \"20\") long size"
				: "@Min(1) @Max(200) long size";
		return """
				package %s.contract.dto.request;

				import jakarta.validation.constraints.Max;
				import jakarta.validation.constraints.Min;

				%s/**
				 * 分页查询%s请求
				 */
				%spublic record Query%sRequest(%s, %s, %s, %s) {

					public Query%sRequest {
						if (current < 1) {
							current = 1;
						}
						if (size < 1 || size > 200) {
							size = 20;
						}
					}

				}
				""".formatted(pkg, schema, desc, ann, entity, st, kw, cur, size, entity);
	}

	private static String detailResponse(String pkg, String entity, String desc, List<FieldSpec> fields,
			boolean swagger) {
		String extras = fields.stream()
			.map((f) -> recordComponent(f, swagger, false))
			.collect(Collectors.joining(",\n"));
		if (!extras.isEmpty()) {
			extras = extras + ",\n";
		}
		return """
				package %s.contract.dto.response;

				%s
				/**
				 * %s详情
				 */
				%s
				public record %sDetailResponse(
						String id,
				%s		String status,
						long version) {
				}
				""".formatted(pkg, dtoImports(fields, swagger, false), desc, schemaType(swagger, desc + "详情"), entity,
				extras);
	}

	private static String summaryResponse(String pkg, String entity, String desc, List<FieldSpec> fields,
			boolean swagger) {
		List<FieldSpec> brief = fields.stream().limit(3).toList();
		String extras = brief.stream()
			.map((f) -> recordComponent(f, swagger, false))
			.collect(Collectors.joining(",\n"));
		if (!extras.isEmpty()) {
			extras = extras + ",\n";
		}
		return """
				package %s.contract.dto.response;

				%s
				/**
				 * %s摘要
				 */
				%s
				public record %sSummaryResponse(
						String id,
				%s		String status) {
				}
				""".formatted(pkg, dtoImports(brief, swagger, false), desc, schemaType(swagger, desc + "摘要"), entity,
				extras);
	}

	private static String apiPath(String pkg, String entity, String prefix, String resource) {
		return """
				package %s.contract.constant;

				/**
				 * %s API path (Controller + Feign). Convention: /{prefix}/v1/{resource}
				 * e.g. /demo/v1/users — module/menu style, not bare /users.
				 */
				public final class %sApiPath {

					public static final String BASE = "/%s/v1/%s";

					private %sApiPath() {
					}

				}
				""".formatted(pkg, entity, entity, prefix, resource, entity);
	}

	private static String serviceName(String pkg, String entity, String svc) {
		return """
				package %s.contract.constant;

				public final class %sServiceName {

					public static final String NAME = "%s";

					private %sServiceName() {
					}

				}
				""".formatted(pkg, entity, svc, entity);
	}

	private static String statusEnum(String pkg, String entity, String desc) {
		return """
				package %s.contract.enums;

				/**
				 * %s状态
				 */
				public enum %sStatusEnum {

					ACTIVE("启用"),
					DISABLED("停用");

					private final String description;

					%sStatusEnum(String description) {
						this.description = description;
					}

					public String getDescription() {
						return description;
					}

				}
				""".formatted(pkg, desc, entity, entity);
	}

	private static String feignClient(String pkg, String entity, String desc, boolean swagger) {
		return """
				package %s.feign;

				import %s.contract.common.page.PageResult;
				import %s.contract.constant.%sApiPath;
				import %s.contract.constant.%sServiceName;
				import %s.contract.dto.request.Create%sRequest;
				import %s.contract.dto.request.Query%sRequest;
				import %s.contract.dto.request.Update%sRequest;
				import %s.contract.dto.response.%sDetailResponse;
				import %s.contract.dto.response.%sSummaryResponse;

				import org.springframework.cloud.openfeign.FeignClient;
				import org.springframework.http.HttpStatus;
				import org.springframework.validation.annotation.Validated;
				import org.springframework.web.bind.annotation.DeleteMapping;
				import org.springframework.web.bind.annotation.GetMapping;
				import org.springframework.web.bind.annotation.PathVariable;
				import org.springframework.web.bind.annotation.PostMapping;
				import org.springframework.web.bind.annotation.PutMapping;
				import org.springframework.web.bind.annotation.RequestBody;
				import org.springframework.web.bind.annotation.ResponseStatus;

				/**
				 * %s Feign client — mirrors Controller paths/signatures.
				 */
				@FeignClient(name = %sServiceName.NAME, path = %sApiPath.BASE,
						fallbackFactory = %sFeignFallbackFactory.class)
				public interface %sFeignClient {

					@GetMapping
					PageResult<%sSummaryResponse> page(@Validated Query%sRequest query);

					@GetMapping("/{id}")
					%sDetailResponse detail(@PathVariable("id") String id);

					@PostMapping
					@ResponseStatus(HttpStatus.CREATED)
					%sDetailResponse create(@RequestBody @Validated Create%sRequest request);

					@PutMapping("/{id}")
					%sDetailResponse update(@PathVariable("id") String id,
							@RequestBody @Validated Update%sRequest request);

					@DeleteMapping("/{id}")
					@ResponseStatus(HttpStatus.NO_CONTENT)
					void delete(@PathVariable("id") String id);

				}
				""".formatted(pkg, pkg, pkg, entity, pkg, entity, pkg, entity, pkg, entity, pkg, entity, pkg, entity,
				pkg, entity, desc, entity, entity, entity, entity, entity, entity, entity, entity, entity, entity,
				entity);
	}

	private static String feignFallback(String pkg, String entity, String desc) {
		return """
				package %s.feign;

				import %s.contract.common.page.PageResult;
				import %s.contract.dto.request.Create%sRequest;
				import %s.contract.dto.request.Query%sRequest;
				import %s.contract.dto.request.Update%sRequest;
				import %s.contract.dto.response.%sDetailResponse;
				import %s.contract.dto.response.%sSummaryResponse;

				import org.springframework.cloud.openfeign.FallbackFactory;
				import org.springframework.stereotype.Component;

				@Component
				public class %sFeignFallbackFactory implements FallbackFactory<%sFeignClient> {

					@Override
					public %sFeignClient create(Throwable cause) {
						return new %sFeignClient() {
							@Override
							public PageResult<%sSummaryResponse> page(Query%sRequest query) {
								return PageResult.of(java.util.List.of(), 0, 1, 20);
							}

							@Override
							public %sDetailResponse detail(String id) {
								return null;
							}

							@Override
							public %sDetailResponse create(Create%sRequest request) {
								return null;
							}

							@Override
							public %sDetailResponse update(String id, Update%sRequest request) {
								return null;
							}

							@Override
							public void delete(String id) {
							}
						};
					}

				}
				""".formatted(pkg, pkg, pkg, entity, pkg, entity, pkg, entity, pkg, entity, pkg, entity, entity, entity,
				entity, entity, entity, entity, entity, entity, entity, entity, entity);
	}

	// ----- domain -----

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
		String restoreParams = fields.stream()
			.map((f) -> ", " + f.type() + " " + f.name())
			.collect(Collectors.joining());
		String restoreAssigns = fields.stream()
			.map((f) -> "		entity." + f.name() + " = " + f.name() + ";")
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
						entity.version = 0L;
						entity.createTime = OffsetDateTime.now();
						return entity;
					}

					public static %s restore(String id%s, %sStatus status, long version,
							OffsetDateTime createTime, OffsetDateTime updateTime) {
						%s entity = new %s();
						entity.id = id;
				%s
						entity.status = status;
						entity.version = version;
						entity.createTime = createTime;
						entity.updateTime = updateTime;
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
				entity, entity, entity, entity, entity, assigns, entity, entity, restoreParams, entity, entity, entity,
				restoreAssigns, entity, updateBody, getters, entity);
	}

	private static String domainStatus(String pkg, String entity) {
		return """
				package %s.domain.model;

				public enum %sStatus {

					ACTIVE, DISABLED

				}
				""".formatted(pkg, entity);
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
				""".formatted(pkg, importsForFields(fields), entity, fields.isEmpty() ? "		String placeholder"
				: fields.stream().map((f) -> "		" + f.type() + " " + f.name()).collect(Collectors.joining(",\n")));
	}

	private static String domainQuery(String pkg, String entity) {
		return """
				package %s.domain.query;

				public record %sQuery(String keyword, String status) {
				}
				""".formatted(pkg, entity);
	}

	private static String repositoryPort(String pkg, String entity) {
		return """
				package %s.domain.repository;

				import %s.domain.model.%s;
				import %s.domain.query.%sQuery;
				import %s.domain.query.PageSlice;

				import java.util.Optional;

				public interface %sRepository {

					%s save(%s aggregate);

					Optional<%s> findById(String id);

					void deleteById(String id);

					PageSlice<%s> findPage(%sQuery query, long current, long size);

				}
				""".formatted(pkg, pkg, entity, pkg, entity, pkg, entity, entity, entity, entity, entity, entity);
	}

	private static String notFound(String pkg, String entity, String desc) {
		String type = entity.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase(Locale.ROOT) + "-not-found";
		return """
				package %s.domain.exception;

				public class %sNotFoundException extends BusinessException {

					private final String id;

					public %sNotFoundException(String id) {
						super("%s not found: " + id);
						this.id = id;
					}

					public String getId() {
						return id;
					}

					@Override
					public String errorType() {
						return "%s";
					}

					@Override
					public int status() {
						return 404;
					}

				}
				""".formatted(pkg, entity, entity, desc, type);
	}

	// ----- infrastructure -----

	private static String po(String pkg, String entity, String table, List<FieldSpec> fields) {
		StringBuilder body = new StringBuilder();
		body.append("""
					@TableId(type = IdType.ASSIGN_ID)
					private String id;
				""");
		for (FieldSpec f : fields) {
			body.append("	private ").append(f.type()).append(" ").append(f.name()).append(";\n");
		}
		body.append("""
					private String status;
					@Version
					private Long version;
					@TableLogic
					private Boolean deleted;
					private OffsetDateTime createTime;
					private OffsetDateTime updateTime;
				""");
		body.append(accessor("String", "Id", "id"));
		for (FieldSpec f : fields) {
			body.append(accessor(f.type(), f.getter(), f.name()));
		}
		body.append(accessor("String", "Status", "status"));
		body.append(accessor("Long", "Version", "version"));
		body.append(accessor("Boolean", "Deleted", "deleted"));
		body.append(accessor("OffsetDateTime", "CreateTime", "createTime"));
		body.append(accessor("OffsetDateTime", "UpdateTime", "updateTime"));
		return """
				package %s.infrastructure.persistence.entity;

				import com.baomidou.mybatisplus.annotation.IdType;
				import com.baomidou.mybatisplus.annotation.TableId;
				import com.baomidou.mybatisplus.annotation.TableLogic;
				import com.baomidou.mybatisplus.annotation.TableName;
				import com.baomidou.mybatisplus.annotation.Version;

				import java.time.OffsetDateTime;

				%s
				/**
				 * %s 持久化对象（id: Snowflake via ASSIGN_ID / ApplicationService pre-assign）
				 */
				@TableName("%s")
				public class %sPO {

				%s
				}
				""".formatted(pkg, importsForFields(fields), entity, table, entity, body);
	}

	private static String accessor(String type, String cap, String name) {
		return """

					public %s get%s() {
						return %s;
					}

					public void set%s(%s %s) {
						this.%s = %s;
					}
				""".formatted(type, cap, name, cap, type, name, name, name);
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

	private static String converter(String pkg, String entity, List<FieldSpec> fields) {
		String toPoSets = fields.stream()
			.map((f) -> "		po.set" + f.getter() + "(entity.get" + f.getter() + "());")
			.collect(Collectors.joining("\n"));
		String toDomainArgs = fields.stream()
			.map((f) -> "				po.get" + f.getter() + "(),")
			.collect(Collectors.joining("\n"));
		return """
				package %s.infrastructure.persistence.converter;

				import %s.domain.model.%s;
				import %s.domain.model.%sStatus;
				import %s.infrastructure.persistence.entity.%sPO;

				import org.springframework.stereotype.Component;

				@Component
				public class %sConverter {

					public %sPO toPO(%s entity) {
						%sPO po = new %sPO();
						po.setId(entity.getId());
				%s
						po.setStatus(entity.getStatus().name());
						po.setVersion(entity.getVersion());
						po.setCreateTime(entity.getCreateTime());
						po.setUpdateTime(entity.getUpdateTime());
						return po;
					}

					public %s toDomain(%sPO po) {
						return %s.restore(
								po.getId(),
				%s
								%sStatus.valueOf(po.getStatus()),
								po.getVersion() == null ? 0L : po.getVersion(),
								po.getCreateTime(),
								po.getUpdateTime());
					}

				}
				""".formatted(pkg, pkg, entity, pkg, entity, pkg, entity, entity, entity, entity, entity, entity,
				toPoSets, entity, entity, entity, toDomainArgs, entity);
	}

	private static String repositoryImpl(String pkg, String entity, String lower) {
		String tpl = """
				package {{pkg}}.infrastructure.persistence.repository;

				import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
				import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
				import {{pkg}}.domain.model.{{entity}};
				import {{pkg}}.domain.query.{{entity}}Query;
				import {{pkg}}.domain.query.PageSlice;
				import {{pkg}}.domain.repository.{{entity}}Repository;
				import {{pkg}}.infrastructure.persistence.converter.{{entity}}Converter;
				import {{pkg}}.infrastructure.persistence.entity.{{entity}}PO;
				import {{pkg}}.infrastructure.persistence.mapper.{{entity}}Mapper;

				import java.util.Optional;

				import org.springframework.stereotype.Repository;
				import org.springframework.util.StringUtils;

				@Repository
				public class {{entity}}RepositoryImpl implements {{entity}}Repository {

					private final {{entity}}Mapper {{lower}}Mapper;
					private final {{entity}}Converter {{lower}}Converter;

					public {{entity}}RepositoryImpl({{entity}}Mapper {{lower}}Mapper, {{entity}}Converter {{lower}}Converter) {
						this.{{lower}}Mapper = {{lower}}Mapper;
						this.{{lower}}Converter = {{lower}}Converter;
					}

					@Override
					public {{entity}} save({{entity}} aggregate) {
						{{entity}}PO po = {{lower}}Converter.toPO(aggregate);
						{{entity}}PO existing = {{lower}}Mapper.selectById(po.getId());
						if (existing == null) {
							if (po.getVersion() == null) {
								po.setVersion(0L);
							}
							{{lower}}Mapper.insert(po);
						}
						else {
							if (po.getVersion() == null) {
								po.setVersion(existing.getVersion());
							}
							{{lower}}Mapper.updateById(po);
						}
						{{entity}}PO saved = {{lower}}Mapper.selectById(po.getId());
						return {{lower}}Converter.toDomain(saved != null ? saved : po);
					}

					@Override
					public Optional<{{entity}}> findById(String id) {
						{{entity}}PO po = {{lower}}Mapper.selectById(id);
						return Optional.ofNullable(po).map({{lower}}Converter::toDomain);
					}

					@Override
					public void deleteById(String id) {
						{{lower}}Mapper.deleteById(id);
					}

					@Override
					public PageSlice<{{entity}}> findPage({{entity}}Query query, long current, long size) {
						Page<{{entity}}PO> page = Page.of(current, size);
						LambdaQueryWrapper<{{entity}}PO> wrapper = buildWrapper(query);
						wrapper.orderByDesc({{entity}}PO::getCreateTime);
						Page<{{entity}}PO> result = {{lower}}Mapper.selectPage(page, wrapper);
						return PageSlice.of(
								result.getRecords().stream().map({{lower}}Converter::toDomain).toList(),
								result.getTotal());
					}

					private LambdaQueryWrapper<{{entity}}PO> buildWrapper({{entity}}Query query) {
						LambdaQueryWrapper<{{entity}}PO> wrapper = new LambdaQueryWrapper<>();
						if (query != null && StringUtils.hasText(query.status())) {
							wrapper.eq({{entity}}PO::getStatus, query.status());
						}
						return wrapper;
					}

				}
				""";
		return tpl.replace("{{pkg}}", pkg).replace("{{entity}}", entity).replace("{{lower}}", lower);
	}

	private static String assembler(String pkg, String entity, List<FieldSpec> fields) {
		String createArgs = fields.stream()
			.map((f) -> "				request." + f.name() + "()")
			.collect(Collectors.joining(",\n"));
		if (!createArgs.isEmpty()) {
			createArgs = ",\n" + createArgs;
		}
		String updateArgs = fields.stream()
			.map((f) -> "				request." + f.name() + "()")
			.collect(Collectors.joining(",\n"));
		String detailFields = fields.stream()
			.map((f) -> "				entity.get" + f.getter() + "(),")
			.collect(Collectors.joining("\n"));
		String summaryFields = fields.stream()
			.limit(3)
			.map((f) -> "				entity.get" + f.getter() + "(),")
			.collect(Collectors.joining("\n"));
		return """
				package %s.application.assembler;

				import %s.contract.dto.request.Create%sRequest;
				import %s.contract.dto.request.Update%sRequest;
				import %s.contract.dto.response.%sDetailResponse;
				import %s.contract.dto.response.%sSummaryResponse;
				import %s.domain.command.Create%sCommand;
				import %s.domain.command.Update%sCommand;
				import %s.domain.model.%s;

				import org.springframework.stereotype.Component;

				@Component
				public class %sAssembler {

					public Create%sCommand toCreateCommand(Create%sRequest request, String id) {
						return new Create%sCommand(id%s);
					}

					public Update%sCommand toUpdateCommand(Update%sRequest request) {
						return new Update%sCommand(
				%s);
					}

					public %sDetailResponse toDetailResponse(%s entity) {
						return new %sDetailResponse(
								entity.getId(),
				%s
								entity.getStatus().name(),
								entity.getVersion());
					}

					public %sSummaryResponse toSummaryResponse(%s entity) {
						return new %sSummaryResponse(
								entity.getId(),
				%s
								entity.getStatus().name());
					}

				}
				""".formatted(pkg, pkg, entity, pkg, entity, pkg, entity, pkg, entity, pkg, entity, pkg, entity, pkg,
				entity, entity, entity, entity, entity, createArgs, entity, entity, entity, updateArgs, entity, entity,
				entity, detailFields, entity, entity, entity, summaryFields);
	}

	private static String appService(String pkg, String entity, String lower, String desc, ApiFlags apis) {
		StringBuilder methods = new StringBuilder();
		if (apis.create()) {
			methods.append("""

						@Transactional(rollbackFor = Exception.class)
						public %sDetailResponse create(Create%sRequest request) {
							String id = idGenerator.nextId();
							%s aggregate = %s.create(assembler.toCreateCommand(request, id));
							%s saved = repository.save(aggregate);
							return assembler.toDetailResponse(saved);
						}
					""".formatted(entity, entity, entity, entity, entity));
		}
		if (apis.update()) {
			methods.append("""

						@Transactional(rollbackFor = Exception.class)
						public %sDetailResponse update(String id, Update%sRequest request) {
							%s aggregate = repository.findById(id)
								.orElseThrow(() -> new %sNotFoundException(id));
							aggregate.update(assembler.toUpdateCommand(request));
							return assembler.toDetailResponse(repository.save(aggregate));
						}
					""".formatted(entity, entity, entity, entity));
		}
		if (apis.delete()) {
			methods.append("""

						@Transactional(rollbackFor = Exception.class)
						public void delete(String id) {
							repository.findById(id).orElseThrow(() -> new %sNotFoundException(id));
							repository.deleteById(id);
						}
					""".formatted(entity));
		}
		return """
				package %s.application.service;

				import %s.application.assembler.%sAssembler;
				import %s.contract.common.id.SnowflakeIdGenerator;
				import %s.contract.dto.request.Create%sRequest;
				import %s.contract.dto.request.Update%sRequest;
				import %s.contract.dto.response.%sDetailResponse;
				import %s.domain.exception.%sNotFoundException;
				import %s.domain.model.%s;
				import %s.domain.repository.%sRepository;

				import org.springframework.stereotype.Service;
				import org.springframework.transaction.annotation.Transactional;

				/**
				 * %s 应用服务（写侧编排；ID = Snowflake）
				 */
				@Service
				public class %sApplicationService {

					private final %sRepository repository;
					private final %sAssembler assembler;
					private final SnowflakeIdGenerator idGenerator;

					public %sApplicationService(%sRepository repository, %sAssembler assembler,
							SnowflakeIdGenerator idGenerator) {
						this.repository = repository;
						this.assembler = assembler;
						this.idGenerator = idGenerator;
					}
				%s
				}
				""".formatted(pkg, pkg, entity, pkg, pkg, entity, pkg, entity, pkg, entity, pkg, entity, pkg, entity,
				pkg, entity, desc, entity, entity, entity, entity, entity, entity, methods);
	}

	private static String queryService(String pkg, String entity) {
		return """
				package %s.application.service;

				import %s.contract.common.page.PageResult;
				import %s.contract.dto.request.Query%sRequest;
				import %s.contract.dto.response.%sDetailResponse;
				import %s.contract.dto.response.%sSummaryResponse;

				public interface %sQueryService {

					%sDetailResponse detail(String id);

					PageResult<%sSummaryResponse> page(Query%sRequest query);

				}
				""".formatted(pkg, pkg, pkg, entity, pkg, entity, pkg, entity, entity, entity, entity, entity);
	}

	private static String queryServiceImpl(String pkg, String entity, String lower, String desc) {
		return """
				package %s.application.service.impl;

				import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
				import %s.application.service.%sQueryService;
				import %s.contract.common.page.PageResult;
				import %s.contract.dto.request.Query%sRequest;
				import %s.contract.dto.response.%sDetailResponse;
				import %s.contract.dto.response.%sSummaryResponse;
				import %s.domain.exception.%sNotFoundException;
				import %s.infrastructure.persistence.mapper.%sReadMapper;

				import org.springframework.stereotype.Service;
				import org.springframework.transaction.annotation.Transactional;

				/**
				 * %s 查询服务（读侧 CQRS：ReadMapper 投影，不经领域对象）
				 */
				@Service
				@Transactional(readOnly = true)
				public class %sQueryServiceImpl implements %sQueryService {

					private final %sReadMapper %sReadMapper;

					public %sQueryServiceImpl(%sReadMapper %sReadMapper) {
						this.%sReadMapper = %sReadMapper;
					}

					@Override
					public %sDetailResponse detail(String id) {
						%sDetailResponse detail = %sReadMapper.selectDetailById(id);
						if (detail == null) {
							throw new %sNotFoundException(id);
						}
						return detail;
					}

					@Override
					public PageResult<%sSummaryResponse> page(Query%sRequest query) {
						Page<%sSummaryResponse> page = Page.of(query.current(), query.size());
						return PageResult.of(
								%sReadMapper.selectSummaryPage(page, query).getRecords(),
								page.getTotal(),
								page.getCurrent(),
								page.getSize());
					}

				}
				""".formatted(pkg, pkg, entity, pkg, pkg, entity, pkg, entity, pkg, entity, pkg, entity, pkg, entity,
				desc, entity, entity, entity, lower, entity, entity, lower, lower, lower, entity, entity, lower, entity,
				entity, entity, entity, lower);
	}

	private static String controller(String pkg, String entity, String lower, String desc, ApiFlags apis,
			boolean swagger) {
		StringBuilder methods = new StringBuilder();
		if (apis.page()) {
			methods.append(swagger ? """

						@Operation(summary = "分页查询%s")
						@GetMapping
						public PageResult<%sSummaryResponse> page(@Validated Query%sRequest query) {
							return %sQueryService.page(query);
						}
					""".formatted(desc, entity, entity, lower) : """

						@GetMapping
						public PageResult<%sSummaryResponse> page(@Validated Query%sRequest query) {
							return %sQueryService.page(query);
						}
					""".formatted(entity, entity, lower));
		}
		if (apis.detail()) {
			methods.append(swagger ? """

						@Operation(summary = "获取%s详情")
						@GetMapping("/{id}")
						public %sDetailResponse detail(@Parameter(description = "ID") @PathVariable String id) {
							return %sQueryService.detail(id);
						}
					""".formatted(desc, entity, lower) : """

						@GetMapping("/{id}")
						public %sDetailResponse detail(@PathVariable String id) {
							return %sQueryService.detail(id);
						}
					""".formatted(entity, lower));
		}
		if (apis.create()) {
			methods.append(swagger ? """

						@Operation(summary = "创建%s")
						@PostMapping
						@ResponseStatus(HttpStatus.CREATED)
						public %sDetailResponse create(@RequestBody @Validated Create%sRequest request) {
							return %sApplicationService.create(request);
						}
					""".formatted(desc, entity, entity, lower) : """

						@PostMapping
						@ResponseStatus(HttpStatus.CREATED)
						public %sDetailResponse create(@RequestBody @Validated Create%sRequest request) {
							return %sApplicationService.create(request);
						}
					""".formatted(entity, entity, lower));
		}
		if (apis.update()) {
			methods.append(swagger ? """

						@Operation(summary = "更新%s")
						@PutMapping("/{id}")
						public %sDetailResponse update(@Parameter(description = "ID") @PathVariable String id,
								@RequestBody @Validated Update%sRequest request) {
							return %sApplicationService.update(id, request);
						}
					""".formatted(desc, entity, entity, lower) : """

						@PutMapping("/{id}")
						public %sDetailResponse update(@PathVariable String id,
								@RequestBody @Validated Update%sRequest request) {
							return %sApplicationService.update(id, request);
						}
					""".formatted(entity, entity, lower));
		}
		if (apis.delete()) {
			methods.append(swagger ? """

						@Operation(summary = "删除%s")
						@DeleteMapping("/{id}")
						@ResponseStatus(HttpStatus.NO_CONTENT)
						public void delete(@Parameter(description = "ID") @PathVariable String id) {
							%sApplicationService.delete(id);
						}
					""".formatted(desc, lower) : """

						@DeleteMapping("/{id}")
						@ResponseStatus(HttpStatus.NO_CONTENT)
						public void delete(@PathVariable String id) {
							%sApplicationService.delete(id);
						}
					""".formatted(lower));
		}
		String ieField = "";
		String ieCtor = "";
		String ieAssign = "";
		String ieImport = "";
		if (apis.importApi() || apis.exportApi()) {
			ieImport = "import " + pkg + ".application.service." + entity + "ImportExportService;\n";
			ieField = "\n	private final " + entity + "ImportExportService importExportService;\n";
			ieCtor = ",\n			" + entity + "ImportExportService importExportService";
			ieAssign = "\n		this.importExportService = importExportService;";
			if (apis.importApi()) {
				methods.append(swagger ? """

							@Operation(summary = "导入%s")
							@PostMapping("/import")
							public java.util.Map<String, Object> importData(
									@org.springframework.web.bind.annotation.RequestParam("file")
									org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
								return importExportService.importData(file);
							}
						""".formatted(desc) : """

							@PostMapping("/import")
							public java.util.Map<String, Object> importData(
									@org.springframework.web.bind.annotation.RequestParam("file")
									org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
								return importExportService.importData(file);
							}
						""");
			}
			if (apis.exportApi()) {
				methods.append(swagger ? """

							@Operation(summary = "导出%s")
							@GetMapping("/export")
							public void exportData(jakarta.servlet.http.HttpServletResponse response)
									throws java.io.IOException {
								importExportService.exportData(response);
							}
						""".formatted(desc) : """

							@GetMapping("/export")
							public void exportData(jakarta.servlet.http.HttpServletResponse response)
									throws java.io.IOException {
								importExportService.exportData(response);
							}
						""");
			}
		}
		String swaggerImports = swagger ? """
				import io.swagger.v3.oas.annotations.Operation;
				import io.swagger.v3.oas.annotations.Parameter;
				import io.swagger.v3.oas.annotations.tags.Tag;

				""" : "";
		String tag = swagger ? "@Tag(name = \"%s管理\")\n".formatted(esc(desc)) : "";
		return """
				package %s.application.controller;

				import %s.application.service.%sApplicationService;
				import %s.application.service.%sQueryService;
				%simport %s.contract.constant.%sApiPath;
				import %s.contract.dto.request.Create%sRequest;
				import %s.contract.dto.request.Query%sRequest;
				import %s.contract.dto.request.Update%sRequest;
				import %s.contract.common.page.PageResult;
				import %s.contract.dto.response.%sDetailResponse;
				import %s.contract.dto.response.%sSummaryResponse;

				%simport org.springframework.http.HttpStatus;
				import org.springframework.validation.annotation.Validated;
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
				%s@RestController
				@RequestMapping(%sApiPath.BASE)
				public class %sController {

					private final %sApplicationService %sApplicationService;
					private final %sQueryService %sQueryService;%s

					public %sController(%sApplicationService %sApplicationService,
							%sQueryService %sQueryService%s) {
						this.%sApplicationService = %sApplicationService;
						this.%sQueryService = %sQueryService;%s
					}
				%s
				}
				""".formatted(pkg, pkg, entity, pkg, entity, ieImport, pkg, entity, pkg, entity, pkg, entity, pkg,
				entity, pkg, pkg, entity, pkg, entity, swaggerImports, desc, tag, entity, entity, entity, lower, entity,
				lower, ieField, entity, entity, lower, entity, lower, ieCtor, lower, lower, lower, lower, ieAssign,
				methods);
	}

	private static String importExportService(String pkg, String entity, String desc, ApiFlags apis,
			List<FieldSpec> fields) {
		StringBuilder excelFields = new StringBuilder();
		for (FieldSpec f : fields) {
			excelFields.append("\t\t@ExcelProperty(\"").append(esc(f.schemaDescription())).append("\")\n");
			excelFields.append("\t\tprivate ").append(f.type()).append(" ").append(f.name()).append(";\n\n");
			excelFields.append("\t\tpublic ").append(f.type()).append(" get").append(f.getter()).append("() {\n");
			excelFields.append("\t\t\treturn ").append(f.name()).append(";\n\t\t}\n\n");
			excelFields.append("\t\tpublic void set")
				.append(f.getter())
				.append("(")
				.append(f.type())
				.append(" ")
				.append(f.name())
				.append(") {\n");
			excelFields.append("\t\t\tthis.").append(f.name()).append(" = ").append(f.name()).append(";\n\t\t}\n\n");
		}
		StringBuilder methods = new StringBuilder();
		if (apis.importApi()) {
			methods
				.append("""

							public java.util.Map<String, Object> importData(org.springframework.web.multipart.MultipartFile file)
									throws java.io.IOException {
								if (file == null || file.isEmpty()) {
									return java.util.Map.of("accepted", false, "message", "empty upload");
								}
								java.util.List<%sExcelRow> rows = EasyExcel.read(file.getInputStream())
										.head(%sExcelRow.class)
										.sheet()
										.doReadSync();
								// Wire rows to Create%sRequest / ApplicationService.create as needed
								return java.util.Map.of("accepted", true, "count", rows.size());
							}
						"""
					.formatted(entity, entity, entity));
		}
		if (apis.exportApi()) {
			methods
				.append("""

							public void exportData(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
								response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
								response.setCharacterEncoding("utf-8");
								response.setHeader("Content-Disposition", "attachment; filename=%s.xlsx");
								java.util.List<%sExcelRow> rows = java.util.List.of(); // fill from QueryService
								EasyExcel.write(response.getOutputStream(), %sExcelRow.class).sheet("%s").doWrite(rows);
							}
						"""
					.formatted(entity.toLowerCase(Locale.ROOT), entity, entity, esc(desc)));
		}
		return """
				package %s.application.service;

				import com.alibaba.excel.EasyExcel;
				import com.alibaba.excel.annotation.ExcelProperty;

				import org.springframework.stereotype.Service;

				/**
				 * %s 导入/导出（EasyExcel skeleton — compiles; wire business mapping next）.
				 */
				@Service
				public class %sImportExportService {
				%s

					/** Excel row DTO for EasyExcel bind */
					public static class %sExcelRow {
				%s
					}

				}
				""".formatted(pkg, desc, entity, methods, entity, excelFields);
	}

	private static String readMapper(String pkg, String entity) {
		return """
				package %s.infrastructure.persistence.mapper;

				import com.baomidou.mybatisplus.core.metadata.IPage;
				import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
				import %s.contract.dto.request.Query%sRequest;
				import %s.contract.dto.response.%sDetailResponse;
				import %s.contract.dto.response.%sSummaryResponse;

				import org.apache.ibatis.annotations.Mapper;
				import org.apache.ibatis.annotations.Param;

				/**
				 * 读侧 Mapper（CQRS 查询投影，直接出 DTO）
				 */
				@Mapper
				public interface %sReadMapper {

					%sDetailResponse selectDetailById(@Param("id") String id);

					IPage<%sSummaryResponse> selectSummaryPage(Page<?> page, @Param("query") Query%sRequest query);

				}
				""".formatted(pkg, pkg, entity, pkg, entity, pkg, entity, entity, entity, entity, entity);
	}

	private static String readMapperXml(String pkg, String entity, String table, List<FieldSpec> fields) {
		String detailCols = fields.stream()
			.map((f) -> "               " + f.column() + ",")
			.collect(Collectors.joining("\n"));
		List<FieldSpec> brief = fields.stream().limit(3).toList();
		String summaryCols = brief.stream()
			.map((f) -> "               " + f.column() + ",")
			.collect(Collectors.joining("\n"));
		return """
				<?xml version="1.0" encoding="UTF-8"?>
				<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
				    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
				<mapper namespace="%s.infrastructure.persistence.mapper.%sReadMapper">

				    <select id="selectDetailById" resultType="%s.contract.dto.response.%sDetailResponse">
				        SELECT id,
				%s
				               status,
				               version
				        FROM %s
				        WHERE id = #{id} AND deleted = FALSE
				    </select>

				    <!-- PaginationInnerInterceptor adds LIMIT — do not write LIMIT here -->
				    <select id="selectSummaryPage" resultType="%s.contract.dto.response.%sSummaryResponse">
				        SELECT id,
				%s
				               status
				        FROM %s
				        WHERE deleted = FALSE
				        <if test="query.status != null and query.status != ''">
				            AND status = #{query.status}
				        </if>
				        ORDER BY create_time DESC
				    </select>
				</mapper>
				""".formatted(pkg, entity, pkg, entity, detailCols, table, pkg, entity, summaryCols, table);
	}

	private static String applicationServiceTest(String pkg, String entity, String lower, List<FieldSpec> fields) {
		String createArgs = fields.stream().map((f) -> switch (f.type()) {
			case "Integer", "int" -> "1";
			case "Long", "long" -> "1L";
			case "Boolean", "boolean" -> "true";
			case "BigDecimal" -> "java.math.BigDecimal.ONE";
			default -> "\"" + ("email".equals(f.name()) ? "a@b.c" : "demo") + "\"";
		}).collect(Collectors.joining(", "));
		String tpl = """
				package {{pkg}}.application.service;

				import {{pkg}}.application.assembler.{{entity}}Assembler;
				import {{pkg}}.contract.common.id.SnowflakeIdGenerator;
				import {{pkg}}.contract.dto.request.Create{{entity}}Request;
				import {{pkg}}.contract.dto.response.{{entity}}DetailResponse;
				import {{pkg}}.domain.model.{{entity}};
				import {{pkg}}.domain.query.{{entity}}Query;
				import {{pkg}}.domain.query.PageSlice;
				import {{pkg}}.domain.repository.{{entity}}Repository;

				import java.util.HashMap;
				import java.util.Map;
				import java.util.Optional;

				import org.junit.jupiter.api.Test;

				import static org.assertj.core.api.Assertions.assertThat;

				/**
				 * Unit test with in-memory fake repository (no Spring context).
				 */
				class {{entity}}ApplicationServiceTest {

					@Test
					void createPersistsViaRepository() {
						Fake{{entity}}Repository repo = new Fake{{entity}}Repository();
						{{entity}}ApplicationService service = new {{entity}}ApplicationService(repo, new {{entity}}Assembler(),
								new SnowflakeIdGenerator(1L));
						Create{{entity}}Request request = new Create{{entity}}Request({{createArgs}});
						{{entity}}DetailResponse detail = service.create(request);
						assertThat(detail.id()).isNotBlank();
						assertThat(repo.store).containsKey(detail.id());
					}

					static final class Fake{{entity}}Repository implements {{entity}}Repository {

						final Map<String, {{entity}}> store = new HashMap<>();

						@Override
						public {{entity}} save({{entity}} aggregate) {
							store.put(aggregate.getId(), aggregate);
							return aggregate;
						}

						@Override
						public Optional<{{entity}}> findById(String id) {
							return Optional.ofNullable(store.get(id));
						}

						@Override
						public void deleteById(String id) {
							store.remove(id);
						}

						@Override
						public PageSlice<{{entity}}> findPage({{entity}}Query query, long current, long size) {
							return PageSlice.of(store.values().stream().toList(), store.size());
						}

					}

				}
				""";
		return tpl.replace("{{pkg}}", pkg).replace("{{entity}}", entity).replace("{{createArgs}}", createArgs);
	}

	private static String validatedRecordFields(List<FieldSpec> fields, boolean forCreate, boolean swagger) {
		if (fields.isEmpty()) {
			return "		String placeholder";
		}
		return fields.stream().map((f) -> {
			StringBuilder sb = new StringBuilder();
			if (forCreate && f.required()) {
				if ("String".equals(f.type())) {
					sb.append("		@NotBlank(message = \"").append(esc(f.schemaDescription())).append("不能为空\")\n");
					sb.append("		@Size(max = 255)\n");
				}
				else {
					sb.append("		@NotNull(message = \"").append(esc(f.schemaDescription())).append("不能为空\")\n");
				}
			}
			else if ("String".equals(f.type())) {
				sb.append("		@Size(max = 255)\n");
			}
			sb.append(recordComponent(f, swagger, forCreate && f.required()));
			return sb.toString();
		}).collect(Collectors.joining(",\n"));
	}

	private static String recordComponent(FieldSpec f, boolean swagger, boolean required) {
		if (swagger) {
			String mode = required ? ", requiredMode = Schema.RequiredMode.REQUIRED" : "";
			return "		@Schema(description = \"%s\"%s)\n		%s %s".formatted(esc(f.schemaDescription()), mode,
					f.type(), f.name());
		}
		return "		" + f.type() + " " + f.name();
	}

	private static String schemaType(boolean swagger, String desc) {
		if (!swagger) {
			return "";
		}
		return "@Schema(description = \"%s\")".formatted(esc(desc));
	}

	private static String dtoImports(List<FieldSpec> fields, boolean swagger, boolean withValidation) {
		StringBuilder sb = new StringBuilder();
		if (swagger) {
			sb.append("import io.swagger.v3.oas.annotations.media.Schema;\n");
		}
		if (withValidation) {
			boolean needNotBlank = fields.stream().anyMatch((f) -> f.required() && "String".equals(f.type()));
			boolean needNotNull = fields.stream().anyMatch((f) -> f.required() && !"String".equals(f.type()));
			boolean needSize = fields.stream().anyMatch((f) -> "String".equals(f.type()));
			if (needNotBlank) {
				sb.append("import jakarta.validation.constraints.NotBlank;\n");
			}
			if (needNotNull) {
				sb.append("import jakarta.validation.constraints.NotNull;\n");
			}
			if (needSize) {
				sb.append("import jakarta.validation.constraints.Size;\n");
			}
		}
		sb.append(importsForFields(fields));
		if (!sb.isEmpty() && !sb.toString().endsWith("\n\n")) {
			if (!sb.toString().endsWith("\n")) {
				sb.append('\n');
			}
			sb.append('\n');
		}
		return sb.toString();
	}

	private static String importsForFields(List<FieldSpec> fields) {
		StringBuilder sb = new StringBuilder();
		if (fields.stream().anyMatch((f) -> "BigDecimal".equals(f.type()))) {
			sb.append("import java.math.BigDecimal;\n");
		}
		if (fields.stream().anyMatch((f) -> "LocalDate".equals(f.type()))) {
			sb.append("import java.time.LocalDate;\n");
		}
		if (fields.stream().anyMatch((f) -> "LocalDateTime".equals(f.type()))) {
			sb.append("import java.time.LocalDateTime;\n");
		}
		return sb.toString();
	}

	private static String esc(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
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
