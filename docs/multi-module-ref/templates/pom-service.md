# 业务服务六模块 POM 模板

> 以 `order-service` 为例，替换 `{svc}`、`{prefix}`（包名片段）、`{basePackage}`。

## 1. 模块依赖矩阵（编译期强制，配 ArchUnit 校验）

| 模块 | packaging | 依赖（仅允许这些） | 职责 |
|------|-----------|--------------------|------|
| `{svc}-contract` | jar | jakarta.validation-api、springdoc 注解（provided） | 纯契约：DTO/常量/枚举，**零框架依赖** |
| `{svc}-feign-client` | jar | `{svc}-contract`、spring-cloud-starter-openfeign（**optional**） | Feign 接口 + FallbackFactory |
| `{svc}-domain` | jar | common-core、jspecify | 领域模型/事件/仓储接口，**纯 Java** |
| `{svc}-infrastructure` | jar | `{svc}-domain`、common-mybatis-starter、common-redis-starter、`{svc}-feign-client`（调用它方时：对方 contract+feign-client） | PO/Mapper/仓储实现/缓存/MQ/Outbox/Saga/ACL |
| `{svc}-application` | jar | `{svc}-contract`、`{svc}-domain`、common-web-starter、springdoc | Controller/AppService/Assembler/Advice，**不含启动类** |
| `{svc}-bootstrap` | jar（可执行） | `{svc}-application`、`{svc}-infrastructure`、Nacos config/discovery、micrometer-tracing-otel | 启动类 + application.yml + db/migration |

> application **不直接依赖** infrastructure：通过 domain 的仓储接口 + Spring 注入解耦，由 bootstrap 完成装配。

## 2. 服务聚合 POM（{svc}/pom.xml）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.enterprise</groupId>
        <artifactId>platform-parent</artifactId>
        <version>${revision}</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>{svc}</artifactId>
    <packaging>pom</packaging>

    <modules>
        <module>{svc}-contract</module>
        <module>{svc}-feign-client</module>
        <module>{svc}-domain</module>
        <module>{svc}-infrastructure</module>
        <module>{svc}-application</module>
        <module>{svc}-bootstrap</module>
    </modules>

    <dependencyManagement>
        <dependencies>
            <!-- 本服务内部模块版本统一为 ${revision}，子模块引用时不写 version -->
            <dependency>
                <groupId>com.enterprise</groupId>
                <artifactId>{svc}-contract</artifactId>
                <version>${revision}</version>
            </dependency>
            <!-- ... 其余五个内部模块同法声明 ... -->
        </dependencies>
    </dependencyManagement>
</project>
```

## 3. {svc}-contract/pom.xml（零框架依赖）

```xml
<parent>
    <groupId>com.enterprise</groupId>
    <artifactId>{svc}</artifactId>
    <version>${revision}</version>
</parent>
<artifactId>{svc}-contract</artifactId>

<dependencies>
    <!-- 仅注解类依赖，不引入任何 Spring 运行时 -->
    <dependency>
        <groupId>jakarta.validation</groupId>
        <artifactId>jakarta.validation-api</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-common</artifactId>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>org.jspecify</groupId>
        <artifactId>jspecify</artifactId>
    </dependency>
</dependencies>
```

## 4. {svc}-feign-client/pom.xml

```xml
<dependencies>
    <dependency>
        <groupId>com.enterprise</groupId>
        <artifactId>{svc}-contract</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
        <optional>true</optional>  <!-- 关键：optional，避免污染纯契约引用方 -->
    </dependency>
</dependencies>
```

## 5. {svc}-domain/pom.xml（纯 Java）

```xml
<dependencies>
    <dependency>
        <groupId>com.enterprise</groupId>
        <artifactId>common-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.jspecify</groupId>
        <artifactId>jspecify</artifactId>
    </dependency>
    <!-- 禁止：spring-context、mybatis-plus、jackson 等一切框架依赖 -->
</dependencies>
```

## 6. {svc}-infrastructure/pom.xml

```xml
<dependencies>
    <dependency>
        <groupId>com.enterprise</groupId>
        <artifactId>{svc}-domain</artifactId>
    </dependency>
    <dependency>
        <groupId>com.enterprise</groupId>
        <artifactId>common-mybatis-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>com.enterprise</groupId>
        <artifactId>common-redis-starter</artifactId>
    </dependency>

    <!-- MyBatis-Plus：SB4 必须用 boot4-starter；分页插件 3.5.9+ 已拆分为独立依赖 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot4-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-jsqlparser</artifactId>
    </dependency>

    <!-- MapStruct（PO <-> Domain 转换） -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
    </dependency>

    <!-- 需要调用其他服务时：依赖对方的 contract + feign-client，禁止自定义 Feign 接口 -->
    <!--
    <dependency>
        <groupId>com.enterprise</groupId>
        <artifactId>inventory-service-contract</artifactId>
    </dependency>
    <dependency>
        <groupId>com.enterprise</groupId>
        <artifactId>inventory-service-feign-client</artifactId>
    </dependency>
    -->

    <!-- 事件发布（Outbox -> RocketMQ），按需 -->
    <dependency>
        <groupId>org.apache.rocketmq</groupId>
        <artifactId>rocketmq-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

## 7. {svc}-application/pom.xml

```xml
<dependencies>
    <dependency>
        <groupId>com.enterprise</groupId>
        <artifactId>{svc}-contract</artifactId>
    </dependency>
    <dependency>
        <groupId>com.enterprise</groupId>
        <artifactId>{svc}-domain</artifactId>
    </dependency>
    <dependency>
        <groupId>com.enterprise</groupId>
        <artifactId>common-web-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <!-- 方法级权限，按需 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
    </dependency>
</dependencies>
```

## 8. {svc}-bootstrap/pom.xml（唯一可执行 JAR）

```xml
<dependencies>
    <dependency>
        <groupId>com.enterprise</groupId>
        <artifactId>{svc}-application</artifactId>
    </dependency>
    <dependency>
        <groupId>com.enterprise</groupId>
        <artifactId>{svc}-infrastructure</artifactId>
    </dependency>

    <!-- Nacos：配置中心 + 注册中心；配置用 spring.config.import，禁止 bootstrap.yml -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
    </dependency>
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>

    <!-- Flyway：迁移脚本放本模块 resources/db/migration -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>

    <!-- 可观测性：OTel 桥接 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-micrometer-tracing-opentelemetry</artifactId>
    </dependency>

    <dependency>
        <groupId>com.enterprise</groupId>
        <artifactId>common-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>com.tngtech.archunit</groupId>
        <artifactId>archunit-junit5</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <!-- 只有 bootstrap 配 spring-boot-maven-plugin，其余模块禁止配 repackage -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <executions>
                <execution>
                    <goals>
                        <goal>repackage</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## 9. ArchUnit 依赖方向校验（{svc}-bootstrap/src/test）

```java
@AnalyzeClasses(packages = "com.enterprise.{prefix}")
class ModuleDependencyTest {

    @ArchTest
    static final ArchRule domain_must_not_depend_on_frameworks = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "com.baomidou..", "tools.jackson..", "com.fasterxml..");

    @ArchTest
    static final ArchRule contract_must_not_depend_on_spring = noClasses()
            .that().resideInAPackage("..contract..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..");

    @ArchTest
    static final ArchRule application_must_not_depend_on_infrastructure = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule no_synchronized_in_virtual_thread_code = noClasses()
            .should().useSynchronization();  // Pinning 防护
}
```
