# Bootstrap 配置模板（{svc}-bootstrap）

> bootstrap 是唯一可执行模块：启动类 + application.yml + db/migration。
> 占位符：`{svc}`（如 order-service）、`{prefix}`（如 order）、`{basePackage}`。

## 1. 启动类

```java
package {basePackage}.{prefix};

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * {description}启动类
 *
 * 注意：
 * 1. scanBasePackages 只扫本服务包；common 能力由 starter @AutoConfiguration 装配
 * 2. @MapperScan 指向本服务 infrastructure 的 mapper 包
 */
@SpringBootApplication(scanBasePackages = "{basePackage}.{prefix}")
@MapperScan("{basePackage}.{prefix}.infrastructure.persistence.mapper")
public class {X}Application {

    public static void main(String[] args) {
        SpringApplication.run({X}Application.class, args);
    }
}
```

> 调用其他服务时，在 bootstrap 增加配置类：`@EnableFeignClients(clients = {InventoryFeignClient.class})`——显式枚举客户端类，避免扫描越界。

## 2. application.yml

```yaml
spring:
  application:
    name: {svc}

  # ========== Nacos 配置中心（禁止 bootstrap.yml） ==========
  config:
    import:
      - nacos:{svc}.yml
      - nacos:{svc}-${spring.profiles.active}.yml
  cloud:
    nacos:
      server-addr: ${NACOS_ADDR:localhost:8848}
    # ========== Feign 超时与熔断 ==========
    openfeign:
      client:
        config:
          default:
            connect-timeout: 3000
            read-timeout: 5000
      circuitbreaker:
        enabled: true          # 服务内部方法级熔断：Resilience4j（网关层用 Sentinel，不双轨）

  # ========== 虚拟线程（Java 21 默认开启） ==========
  threads:
    virtual:
      enabled: true
  task:
    execution:
      pool:
        core-size: 4           # CPU 密集型任务单独走平台线程池
        max-size: 8

  # ========== 数据源（每服务独立 Database，禁止跨服务 JOIN） ==========
  datasource:
    url: jdbc:postgresql://${PG_HOST:localhost:5432}/{svc_db}
    username: ${PG_USER}
    password: ${PG_PASSWORD}   # 生产：Jasypt 密文存 Nacos，密钥由 K8s Secret 注入
    hikari:
      maximum-pool-size: 20    # 最后一道舱壁，按压测调整

  # ========== Flyway ==========
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

  # ========== Problem Details（RFC 7807，必须开启） ==========
  mvc:
    problemdetails:
      enabled: true

# ========== 可观测性 ==========
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      probes:
        enabled: true          # /actuator/health/liveness 与 /readiness（K8s 探针）
  metrics:
    tags:
      application: ${spring.application.name}
  tracing:
    sampling:
      probability: 1.0         # 生产按流量下调
  otlp:
    tracing:
      endpoint: http://otel-collector:4318/v1/traces

server:
  port: 8080
  shutdown: graceful           # 优雅停机，配合 preStop sleep 5
```

JVM 启动参数（按容器 CPU limit 调整）：

```bash
-Djdk.virtualThreadScheduler.parallelism=4
-Djdk.virtualThreadScheduler.maxPoolSize=256
```

## 3. logback-spring.xml（JSON 结构化 + MDC 字段）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty scope="context" name="appName" source="spring.application.name"/>

    <!-- JSON 结构化输出到 stdout，由 Fluentd 采集到 Elasticsearch -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
            <customFields>{"application":"${appName}"}</customFields>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

**虚拟线程 MDC 防护**：在 common-web 的 Filter 中进入时写入、离开时**显式 `MDC.clear()`**，避免虚拟线程复用导致上下文污染。

## 4. Feign 拦截器（common-cloud 提供，此处为参考实现）

```java
package com.enterprise.common.cloud.feign;

import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Feign 拦截器：透传 TraceId 与鉴权信息
 * 注意：服务间调用传播的是网关签名的内部短效 Token，不是用户 JWT 简单透传
 */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor authRequestInterceptor() {
        return template -> {
            String traceId = MDC.get("traceId");
            if (StringUtils.hasText(traceId)) {
                template.header("X-Trace-Id", traceId);
            }
            String token = MDC.get("authorization");
            if (StringUtils.hasText(token)) {
                template.header("Authorization", token);
            }
            // 灰度标记透传
            String gray = MDC.get("xGray");
            if (StringUtils.hasText(gray)) {
                template.header("X-Gray", gray);
            }
        };
    }
}
```

## 5. Snowflake ID 生成器要点（common-core 提供）

- 实例启动时通过 Nacos 客户端订阅本服务实例列表，按 IP+端口排序取索引 nodeId
- `workerId = nodeId % 32`，`datacenterId = nodeId / 32`
- Nacos 订阅回调触发重算，nodeId 尽量稳定；启动时先注册临时节点获取租约再发号
- 时钟回拨：等待追上或使用历史序列号缓存

> 代码生成时只需注入 `SnowflakeIdGenerator` 使用（见 application.md 的 ApplicationService）；生成器本体属于 common 组件，不在业务服务内生成。
