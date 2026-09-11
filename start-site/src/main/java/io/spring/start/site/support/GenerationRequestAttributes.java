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

import java.util.Collections;
import java.util.List;

/**
 * Request-scoped generation extras ({@code template}, {@code entities}) captured from
 * query parameters for ProjectContributor consumption via ThreadLocal.
 *
 * @author start.spring.io China ecosystem
 */
public final class GenerationRequestAttributes {

	private static final ThreadLocal<GenerationRequestAttributes> HOLDER = new ThreadLocal<>();

	private final String template;

	private final String entitiesJson;

	private final List<EntitySpec> entities;

	public GenerationRequestAttributes(String template, String entitiesJson, List<EntitySpec> entities) {
		this.template = (template != null) ? template : "";
		this.entitiesJson = (entitiesJson != null) ? entitiesJson : "";
		this.entities = (entities != null) ? List.copyOf(entities) : List.of();
	}

	public String getTemplate() {
		return this.template;
	}

	public String getEntitiesJson() {
		return this.entitiesJson;
	}

	public List<EntitySpec> getEntities() {
		return this.entities;
	}

	public boolean isDddEnhanced() {
		return "ddd-enhanced".equals(this.template)
				|| (!this.entities.isEmpty() && (this.template.isEmpty() || this.template.startsWith("ddd-")));
	}

	public boolean isDddStandard() {
		return "ddd-standard".equals(this.template);
	}

	public boolean isPlatformEnhanced() {
		return "platform-enhanced".equals(this.template);
	}

	public static void set(GenerationRequestAttributes attributes) {
		HOLDER.set(attributes);
	}

	public static GenerationRequestAttributes get() {
		GenerationRequestAttributes current = HOLDER.get();
		return (current != null) ? current : new GenerationRequestAttributes("", "", Collections.emptyList());
	}

	public static void clear() {
		HOLDER.remove();
	}

	/**
	 * Compact entity definition from the UI {@code entities=} JSON payload.
	 *
	 * @param name entity name
	 * @param table table name
	 * @param db database id
	 * @param orm ORM id
	 * @param description Chinese description
	 * @param fields field list
	 * @param apis API flags
	 */
	public record EntitySpec(String name, String table, String db, String orm, String description,
			List<FieldSpec> fields, ApiFlags apis) {

		public EntitySpec {
			name = ((name == null) || name.isBlank()) ? "Entity" : name.trim();
			table = ((table == null) || table.isBlank()) ? toSnake(name) : table.trim();
			db = ((db == null) || db.isBlank()) ? "postgresql" : db.trim();
			orm = ((orm == null) || orm.isBlank()) ? "mybatis-plus" : orm.trim();
			description = ((description == null) || description.isBlank()) ? name : description.trim();
			fields = (fields != null) ? List.copyOf(fields) : List.of();
			apis = (apis != null) ? apis : ApiFlags.allCrud();
		}

		public String entityLower() {
			if (this.name.isEmpty()) {
				return "entity";
			}
			return Character.toLowerCase(this.name.charAt(0)) + this.name.substring(1);
		}

		public String resource() {
			String lower = entityLower();
			if (lower.endsWith("y") && lower.length() > 1 && "aeiou".indexOf(lower.charAt(lower.length() - 2)) < 0) {
				return lower.substring(0, lower.length() - 1) + "ies";
			}
			if (!lower.endsWith("s")) {
				return lower + "s";
			}
			return lower;
		}

		private static String toSnake(String camel) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < camel.length(); i++) {
				char c = camel.charAt(i);
				if (Character.isUpperCase(c) && i > 0) {
					sb.append('_');
				}
				sb.append(Character.toLowerCase(c));
			}
			return sb.toString();
		}
	}

	/**
	 * Field definition.
	 *
	 * @param name field name
	 * @param type Java type
	 * @param required whether required
	 * @param unique whether unique
	 */
	public record FieldSpec(String name, String type, boolean required, boolean unique) {

		public FieldSpec {
			name = ((name == null) || name.isBlank()) ? "field" : name.trim();
			type = ((type == null) || type.isBlank()) ? "String" : type.trim();
		}

		public String getter() {
			return Character.toUpperCase(this.name.charAt(0)) + this.name.substring(1);
		}

		public String column() {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < this.name.length(); i++) {
				char c = this.name.charAt(i);
				if (Character.isUpperCase(c) && i > 0) {
					sb.append('_');
				}
				sb.append(Character.toLowerCase(c));
			}
			return sb.toString();
		}

		public String sqlType() {
			return switch (this.type) {
				case "Integer", "int" -> "INT";
				case "Long", "long" -> "BIGINT";
				case "Boolean", "boolean" -> "BOOLEAN";
				case "BigDecimal" -> "NUMERIC(19,2)";
				case "LocalDate" -> "DATE";
				case "LocalDateTime", "OffsetDateTime" -> "TIMESTAMPTZ";
				default -> "VARCHAR(255)";
			};
		}
	}

	/**
	 * API generation flags.
	 *
	 * @param create create
	 * @param detail detail
	 * @param page page
	 * @param update update
	 * @param delete delete
	 * @param importApi import
	 * @param exportApi export
	 */
	public record ApiFlags(boolean create, boolean detail, boolean page, boolean update, boolean delete,
			boolean importApi, boolean exportApi) {

		public static ApiFlags allCrud() {
			return new ApiFlags(true, true, true, true, true, false, false);
		}
	}

}
