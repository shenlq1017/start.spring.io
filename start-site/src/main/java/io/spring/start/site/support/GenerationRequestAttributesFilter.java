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

package io.spring.start.site.support;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Captures {@code template} and {@code entities} query params into
 * {@link GenerationRequestAttributes} for the duration of project generation.
 *
 * @author start.spring.io China ecosystem
 */
public class GenerationRequestAttributesFilter extends OncePerRequestFilter {

	private static final JsonMapper MAPPER = JsonMapper.builder().build();

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			String template = request.getParameter("template");
			String entitiesRaw = request.getParameter("entities");
			List<GenerationRequestAttributes.EntitySpec> entities = parseEntities(entitiesRaw);
			GenerationRequestAttributes.set(new GenerationRequestAttributes(template, entitiesRaw, entities));
			filterChain.doFilter(request, response);
		}
		finally {
			GenerationRequestAttributes.clear();
		}
	}

	static List<GenerationRequestAttributes.EntitySpec> parseEntities(String raw) {
		if (raw == null || raw.isBlank()) {
			return List.of();
		}
		String json = raw;
		try {
			// Some clients double-encode; tolerate a single decode pass when needed
			if (!json.trim().startsWith("[")) {
				json = URLDecoder.decode(json, StandardCharsets.UTF_8);
			}
			JsonNode root = MAPPER.readTree(json);
			if (!root.isArray()) {
				return List.of();
			}
			List<GenerationRequestAttributes.EntitySpec> list = new ArrayList<>();
			for (JsonNode node : root) {
				list.add(toEntity(node));
			}
			return list;
		}
		catch (Exception ex) {
			return List.of();
		}
	}

	private static GenerationRequestAttributes.EntitySpec toEntity(JsonNode node) {
		List<GenerationRequestAttributes.FieldSpec> fields = new ArrayList<>();
		JsonNode fieldsNode = node.get("fields");
		if (fieldsNode != null && fieldsNode.isArray()) {
			for (JsonNode f : fieldsNode) {
				fields.add(new GenerationRequestAttributes.FieldSpec(text(f, "name"), text(f, "type"),
						bool(f, "required", false), bool(f, "unique", false)));
			}
		}
		JsonNode apisNode = node.get("apis");
		GenerationRequestAttributes.ApiFlags apis = GenerationRequestAttributes.ApiFlags.allCrud();
		if (apisNode != null && apisNode.isObject()) {
			apis = new GenerationRequestAttributes.ApiFlags(bool(apisNode, "create", true),
					bool(apisNode, "detail", true), bool(apisNode, "page", true), bool(apisNode, "update", true),
					bool(apisNode, "delete", true), bool(apisNode, "import", false), bool(apisNode, "export", false));
		}
		return new GenerationRequestAttributes.EntitySpec(text(node, "name"), text(node, "table"), text(node, "db"),
				text(node, "orm"), text(node, "description"), fields, apis);
	}

	private static String text(JsonNode node, String field) {
		JsonNode n = node.get(field);
		return ((n == null) || n.isNull()) ? null : n.asString();
	}

	private static boolean bool(JsonNode node, String field, boolean defaultValue) {
		JsonNode n = node.get(field);
		return ((n == null) || n.isNull()) ? defaultValue : n.asBoolean();
	}

}
