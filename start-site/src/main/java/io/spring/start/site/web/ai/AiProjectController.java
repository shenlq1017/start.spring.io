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

package io.spring.start.site.web.ai;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.spring.start.site.support.GenerationRequestAttributesFilter;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * AI-friendly discovery + project generation helpers for MCP / agent clients.
 *
 * @author start.spring.io China ecosystem
 */
@RestController
@RequestMapping("/ai/v1")
public class AiProjectController {

	private static final JsonMapper MAPPER = JsonMapper.builder().build();

	@GetMapping(value = "/capabilities", produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, Object> capabilities() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("name", "start.spring.io China ecosystem");
		body.put("offlineHint", "Run with --application.offline=true for air-gapped / local metadata");
		body.put("architectures", List.of(Map.of("id", "ddd-six-module", "label", "企业DDD六模块"),
				Map.of("id", "platform-monorepo", "label", "平台 Monorepo")));
		body.put("templates", List.of(Map.of("id", "ddd-standard", "crud", false),
				Map.of("id", "ddd-enhanced", "crud", true, "notes", "Generates vertical CRUD slices"),
				Map.of("id", "platform-standard", "crud", false), Map.of("id", "platform-enhanced", "crud", false)));
		body.put("dependencyKits", Map.of("ddd-enhanced",
				List.of("web", "validation", "mybatis-plus", "postgresql", "flyway", "knife4j", "ddd-six-module")));
		body.put("urlConvention",
				"/{prefix}/v1/{resource} — prefix from artifact (strip -service), e.g. /order/v1/orders");
		body.put("examples", List.of(Map.of("name", "Order", "artifact", "order-service", "path", "/order/v1/orders"),
				Map.of("name", "Activity", "artifact", "activity-service", "path", "/activity/v1/activities")));
		body.put("endpoints", Map.of("metadata", "/metadata/client", "starterZip", "/starter.zip", "entitySchema",
				"/ai/v1/schemas/entity", "createProject", "POST /ai/v1/projects", "validate", "POST /ai/v1/validate"));
		return body;
	}

	@GetMapping(value = "/schemas/entity", produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, Object> entitySchema() {
		return Map.of("type", "array", "items", Map.of("type", "object", "required", List.of("name"), "properties",
				Map.of("name", Map.of("type", "string"), "table", Map.of("type", "string"), "description",
						Map.of("type", "string"), "swagger", Map.of("type", "boolean", "default", true), "fields",
						Map.of("type", "array", "items", Map.of("type", "object", "properties",
								Map.of("name", Map.of("type", "string"), "type", Map.of("type", "string"), "required",
										Map.of("type", "boolean"), "unique", Map.of("type", "boolean"), "description",
										Map.of("type", "string"), "swagger", Map.of("type", "boolean")))),
						"apis",
						Map.of("type", "object", "properties",
								Map.of("create", Map.of("type", "boolean"), "detail", Map.of("type", "boolean"), "page",
										Map.of("type", "boolean"), "update", Map.of("type", "boolean"), "delete",
										Map.of("type", "boolean"), "import", Map.of("type", "boolean"), "export",
										Map.of("type", "boolean"))))));
	}

	@GetMapping(value = "/openapi", produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, Object> openapi() {
		return Map.of("openapi", "3.0.3", "info", Map.of("title", "start.spring.io AI API", "version", "1.0.0"),
				"paths",
				Map.of("/ai/v1/capabilities", Map.of("get", Map.of("summary", "Generator capabilities")),
						"/ai/v1/schemas/entity", Map.of("get", Map.of("summary", "Entity JSON schema")),
						"/ai/v1/validate", Map.of("post", Map.of("summary", "Validate project request")),
						"/ai/v1/projects", Map.of("post", Map.of("summary", "Redirect to /starter.zip with AI body"))));
	}

	@PostMapping(value = "/validate", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, Object> validate(@RequestBody Map<String, Object> body) {
		List<String> errors = validateBody(body);
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("valid", errors.isEmpty());
		result.put("errors", errors);
		if (body.get("entities") != null) {
			try {
				String json = MAPPER.writeValueAsString(body.get("entities"));
				var entities = GenerationRequestAttributesFilter.parseEntities(json);
				result.put("entityCount", entities.size());
			}
			catch (Exception ex) {
				errors.add("entities JSON invalid: " + ex.getMessage());
				result.put("valid", false);
				result.put("errors", errors);
			}
		}
		return result;
	}

	/**
	 * Accepts a JSON body and redirects to {@code /starter.zip} with equivalent query
	 * parameters (entities JSON encoded). MCP clients may also call {@code /starter.zip}
	 * directly.
	 * @param body project request
	 * @return redirect to starter.zip
	 */
	@PostMapping(value = "/projects", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> createProject(@RequestBody Map<String, Object> body) {
		List<String> errors = validateBody(body);
		if (!errors.isEmpty()) {
			return ResponseEntity.badRequest().build();
		}
		UriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentContextPath().path("/starter.zip");
		put(builder, "type", str(body.get("type"), "maven-project"));
		put(builder, "language", str(body.get("language"), "java"));
		put(builder, "bootVersion", str(body.get("bootVersion"), body.get("boot")));
		put(builder, "baseDir", str(body.get("baseDir"), str(body.get("artifactId"), "demo")));
		put(builder, "groupId", str(body.get("groupId"), "com.example"));
		put(builder, "artifactId", str(body.get("artifactId"), "demo"));
		put(builder, "name", str(body.get("name"), str(body.get("artifactId"), "demo")));
		put(builder, "packageName", str(body.get("packageName"), "com.example.demo"));
		put(builder, "javaVersion", str(body.get("javaVersion"), str(body.get("java"), "21")));
		put(builder, "packaging", str(body.get("packaging"), "jar"));
		String deps = str(body.get("dependencies"), "");
		if (!StringUtils.hasText(deps) && body.get("architecture") != null) {
			deps = str(body.get("architecture"), "") + ",web,validation,mybatis-plus,postgresql,flyway,knife4j";
		}
		put(builder, "dependencies", deps);
		put(builder, "template", str(body.get("template"), "ddd-enhanced"));
		if (body.get("entities") != null) {
			try {
				String entitiesJson = MAPPER.writeValueAsString(body.get("entities"));
				builder.queryParam("entities", URLEncoder.encode(entitiesJson, StandardCharsets.UTF_8));
			}
			catch (Exception ex) {
				return ResponseEntity.badRequest().build();
			}
		}
		URI location = builder.build(true).toUri();
		return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, location.toString()).build();
	}

	private static List<String> validateBody(Map<String, Object> body) {
		List<String> errors = new java.util.ArrayList<>();
		if (body == null || body.isEmpty()) {
			errors.add("body required");
			return errors;
		}
		return errors;
	}

	private static void put(UriComponentsBuilder builder, String key, String value) {
		if (StringUtils.hasText(value)) {
			builder.queryParam(key, value);
		}
	}

	private static String str(Object value, Object fallback) {
		if (value == null) {
			return (fallback != null) ? String.valueOf(fallback) : null;
		}
		String s = String.valueOf(value).trim();
		return (s.isEmpty() && fallback != null) ? String.valueOf(fallback) : s;
	}

}
