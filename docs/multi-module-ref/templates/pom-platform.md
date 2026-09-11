# 平台级 POM 模板

> 平台父 POM 统一 BOM；common 模块严格单向依赖；starter 只做自动配置与依赖聚合。

## 1. 平台父 POM（platform-parent/pom.xml）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.enterprise</groupId>
    <artifactId>platform-parent</artifactId>
    <version>${revision}</version>
    <packaging>pom</packaging>

    <modules>
        <module>common</module>
        <module>starters</module>
        <module>services</module>
        <module>gateway</module>
    </modules>

    <properties>
        <revision>1.0.0-SNAPSHOT</revision>
        <java.version>21</java.version>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- ========== 版本统一在此管理，升级只改这里 ========== -->
        <spring-boot.version>4.1.1</spring-boot.version>
        <spring-cloud.version>2025.1.0</spring-cloud.version>
        <spring-cloud-alibaba.version>2025.1.0.0</spring-cloud-alibaba.version>
        <mybatis-plus.version>3.5.15</mybatis-plus.version>
        <springdoc.version>3.1.0</springdoc.version>
        <mapstruct.version>1.6.3</mapstruct.version>
        <lombok.version>1.18.42</lombok.version>
        <testcontainers.version>2.0.5</testcontainers.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- BOM 导入顺序不可颠倒：Spring Boot -> Spring Cloud -> SCA -> 其余 -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>${spring-cloud-alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-bom</artifactId>
                <version>${mybatis-plus.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers-bom</artifactId>
                <version>${testcontainers.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- 平台内部模块版本（common / starters），业务服务只写 artifactId -->
            <dependency>
                <groupId>com.enterprise</groupId>
                <artifactId>common-core</artifactId>
                <version>${revision}</version>
            </dependency>
            <!-- ... common-web / common-mybatis / common-redis / common-cloud / common-test
                 及 common-web-starter / common-mybatis-starter / common-cloud-starter 同法声明 ... -->
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <configuration>
                        <!-- MapStruct + Lombok 联合注解处理，lombok 必须在 mapstruct 之前 -->
                        <annotationProcessorPaths>
                            <path>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                                <version>${lombok.version}</version>
                            </path>
                            <path>
                                <groupId>org.mapstruct</groupId>
                                <artifactId>mapstruct-processor</artifactId>
                                <version>${mapstruct.version}</version>
                            </path>
                        </annotationProcessorPaths>
                    </configuration>
                </plugin>
                <plugin>
                    <groupId>org.codehaus.mojo</groupId>
                    <artifactId>flatten-maven-plugin</artifactId>
                    <!-- 展开 ${revision} 占位符，保证 install/deploy 的 POM 可解析 -->
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

> **POM 注释规范**：禁止 `<!--` 后紧跟 `--`；多行分隔注释用 `=`。

## 2. common 模块依赖矩阵（ArchUnit 强制校验）

| 模块 | 允许依赖 | 职责 | 禁止 |
|------|----------|------|------|
| common-core | 仅 JDK | 工具类、异常基类、枚举、`PageResult` | 任何框架依赖 |
| common-web | common-core + spring-web(mvc) | ProblemDetail 支持、全局异常基类 | 依赖其他 common-* |
| common-mybatis | common-core + mybatis-plus | `JsonbTypeHandler`、分页配置、MetaObjectHandler 基类 | 依赖其他 common-* |
| common-redis | common-core + spring-data-redis | Redis 封装 | 依赖其他 common-* |
| common-cloud | common-core + openfeign | Feign 拦截器（TraceId/内部 Token 透传） | 依赖其他 common-* |
| common-test | common-core + testcontainers | 测试基类 | 依赖其他 common-* |

需要跨组件能力时，在 common-core 定义接口，由对应 common 模块实现。

## 3. common-core 必备类

### PageResult（分页响应统一载体，Controller 分页接口返回它而非 IPage）

```java
package com.enterprise.common.core.page;

import java.util.List;
import java.util.function.Function;

/**
 * 分页结果（common-core 提供，Controller 禁止直接暴露 MyBatis-Plus IPage）
 */
public record PageResult<T>(List<T> records, long total, long current, long size) {

    public static <T> PageResult<T> of(List<T> records, long total, long current, long size) {
        return new PageResult<>(records, total, current, size);
    }

    public static <S, T> PageResult<T> of(com.baomidou.mybatisplus.core.metadata.IPage<S> page,
                                          Function<S, T> mapper) {
        return new PageResult<>(page.getRecords().stream().map(mapper).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }
}
```

> `PageResult` 放在 common-core 且为纯 Java。若不希望 common-core 依赖 MyBatis-Plus，删除第二个工厂方法，由各服务 application 层自行转换。

### 业务异常基类

```java
package com.enterprise.common.core.exception;

import org.jspecify.annotations.Nullable;

/**
 * 业务异常基类：errorType 用于 Problem Detail 的 type URI
 */
public abstract class BusinessException extends RuntimeException {

    protected BusinessException(String message) {
        super(message);
    }

    /** 错误类型标识，拼接到 Problem Detail type URI，如 "order-not-found" */
    public abstract String errorType();

    /** HTTP 状态码 */
    public abstract int status();
}
```

## 4. starter 规范

每个 starter 只做两件事：聚合依赖 + 自动配置。

```java
package com.enterprise.starter.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(CommonWebProperties.class)
public class CommonWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
```

注册文件：`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
com.enterprise.starter.web.CommonWebAutoConfiguration
```

**业务服务只依赖 starter，不直接依赖 common-* 内部模块；启动类 `scanBasePackages` 只扫本服务包。**

## 5. gateway 模块要点

依赖：`spring-cloud-starter-gateway`、`spring-cloud-starter-alibaba-sentinel`（网关适配）、`spring-boot-starter-oauth2-resource-server`。
职责：JWT 校验、注入 `X-User-Id`/`X-User-Roles`、Sentinel 路由级限流熔断、CORS、灰度路由（`X-Gray` Header）。
**不在 gateway 写任何业务逻辑。**
