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

package io.spring.start.site;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.spring.initializr.metadata.InitializrMetadata;
import io.spring.initializr.versionresolver.MavenVersionResolver;
import io.spring.initializr.web.support.InitializrMetadataUpdateStrategy;
import io.spring.start.site.container.SimpleDockerServiceResolver;
import io.spring.start.site.project.ProjectDescriptionCustomizerConfiguration;
import io.spring.start.site.support.CacheableMavenVersionResolver;
import io.spring.start.site.support.GenerationRequestAttributesFilter;
import io.spring.start.site.support.StartInitializrMetadataUpdateStrategy;
import io.spring.start.site.web.HomeController;
import io.spring.start.site.web.ai.AiProjectController;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Initializr website application.
 *
 * @author Stephane Nicoll
 * @author Shen Liqiang
 */
@EnableAutoConfiguration
@SpringBootConfiguration
@Import(ProjectDescriptionCustomizerConfiguration.class)
@EnableCaching
@EnableAsync
@EnableConfigurationProperties(StartConfigurationProperties.class)
public class StartApplication {

	public static void main(String[] args) {
		SpringApplication.run(StartApplication.class, args);
	}

	@Bean
	public HomeController homeController() {
		return new HomeController();
	}

	@Bean
	public AiProjectController aiProjectController() {
		return new AiProjectController();
	}

	@Bean
	public FilterRegistrationBean<GenerationRequestAttributesFilter> generationRequestAttributesFilter() {
		FilterRegistrationBean<GenerationRequestAttributesFilter> bean = new FilterRegistrationBean<>();
		bean.setFilter(new GenerationRequestAttributesFilter());
		// Broad pattern; filter self-scopes to starter.zip / starter.tgz
		bean.addUrlPatterns("/*");
		bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
		return bean;
	}

	@Bean
	@ConditionalOnProperty(prefix = "application", name = "offline", havingValue = "false", matchIfMissing = true)
	public StartInitializrMetadataUpdateStrategy initializrMetadataUpdateStrategy(
			RestTemplateBuilder restTemplateBuilder, JsonMapper jsonMapper) {
		RestTemplate restTemplate = restTemplateBuilder.defaultHeader(HttpHeaders.USER_AGENT, "start.spring.io")
			.build();
		return new StartInitializrMetadataUpdateStrategy(restTemplate, jsonMapper);
	}

	@Bean
	@ConditionalOnProperty(prefix = "application", name = "offline", havingValue = "true")
	public InitializrMetadataUpdateStrategy offlineInitializrMetadataUpdateStrategy() {
		// Keep YAML metadata as-is; do not call api.spring.io.
		return (InitializrMetadata metadata) -> metadata;
	}

	@Bean
	public CacheableMavenVersionResolver mavenVersionResolver(StartConfigurationProperties properties)
			throws IOException {
		Path location;
		if (StringUtils.hasText(properties.getMavenVersionResolver().getCacheDirectory())) {
			location = Path.of(properties.getMavenVersionResolver().getCacheDirectory());
		}
		else {
			location = Files.createTempDirectory("version-resolver-cache-");
		}
		return new CacheableMavenVersionResolver(MavenVersionResolver.withCacheLocation(location));
	}

	@Bean
	public SimpleDockerServiceResolver dockerServiceResolver() {
		return new SimpleDockerServiceResolver();
	}

}
