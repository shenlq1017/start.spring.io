# 契约模板（{svc}-contract / {svc}-feign-client）

> contract 零框架依赖、只增不改；Feign 接口只能由服务提供方维护，调用方禁止自定义。
> 占位符：`{basePackage}`（如 com.enterprise）、`{prefix}`（如 order）、`{Entity}`、`{description}`。

## 1. 请求 DTO（dto/request）

```java
package {basePackage}.{prefix}.contract.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 创建{description}请求
 */
@Schema(description = "创建{description}请求")
public record Create{Entity}Request(

        @NotBlank(message = "请求ID不能为空")
        @Size(max = 64)
        @Schema(description = "幂等请求ID（客户端预生成，唯一索引去重）", requiredMode = Schema.RequiredMode.REQUIRED)
        String requestId,

        @NotNull(message = "金额不能为空")
        @DecimalMin(value = "0.01", message = "金额必须大于0")
        @Schema(description = "金额", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal amount,

        @Nullable
        @Size(max = 500)
        @Schema(description = "备注")
        String remark
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
```

### 查询请求

```java
@Schema(description = "{description}查询请求")
public record Query{Entity}Request(

        @Nullable @Schema(description = "状态") String status,
        @Nullable @Schema(description = "关键字") String keyword,

        @Min(1) @Schema(description = "页码", defaultValue = "1")
        long current,

        @Min(1) @Max(200) @Schema(description = "每页大小", defaultValue = "20")
        long size
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public Query{X}Request {
        if (current < 1) current = 1;
        if (size < 1 || size > 200) size = 20;
    }
}
```

**DTO 规范**：
- 默认用 `record`（不可变、 Jackson 3 原生支持）；需要框架双向绑定的场景才退化为 `@Data` class
- 可空字段显式 `@Nullable`（JSpecify）
- 校验信息写中文 message；每个字段有 `@Schema(description = ...)`

## 2. 响应 DTO（dto/response）

```java
package {basePackage}.{prefix}.contract.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * {description}详情响应
 *
 * @param available 降级语义字段：false 表示下游降级/不可用，
 *                  调用方 ACL 层判断 false 后抛业务异常
 */
@Schema(description = "{description}详情响应")
public record {Entity}DetailResponse(

        @Schema(description = "是否可用（降级时为 false）")
        boolean available,

        @Nullable @Schema(description = "ID") String id,
        @Nullable @Schema(description = "状态") String status,
        @Nullable @Schema(description = "金额") BigDecimal amount,
        @Nullable @Schema(description = "创建时间") OffsetDateTime createTime
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 降级响应工厂：FallbackFactory 专用 */
    public static {Entity}DetailResponse unavailable(String id) {
        return new {Entity}DetailResponse(false, id, null, null, null);
    }
}
```

> 列表页用 `{Entity}SummaryResponse`（扁平、字段精简），详情用 `{Entity}DetailResponse`，二者分离。

## 3. 常量（constant）

```java
package {basePackage}.{prefix}.contract.constant;

/** {description}服务名常量 */
public final class {X}ServiceName {

    private {X}ServiceName() {}

    /** Nacos 注册服务名，Feign name 引用此常量 */
    public static final String NAME = "{svc}";
}
```

```java
package {basePackage}.{prefix}.contract.constant;

/** {description} API 路径常量（Controller 与 Feign 共用，保证契约一致） */
public final class {X}ApiPath {

    private {X}ApiPath() {}

    /** 路径前缀，如 /order/v1/orders */
    public static final String BASE = "/{prefix}/v1/{resource}";
}
```

## 4. 枚举（enums）

```java
package {basePackage}.{prefix}.contract.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * {description}状态枚举（契约侧，供请求/响应 DTO 使用）
 */
@Schema(description = "{description}状态")
public enum {Entity}StatusEnum {

    PENDING("待处理"),
    PROCESSING("处理中"),
    COMPLETED("已完成"),
    CLOSED("已关闭");

    private final String description;

    {Entity}StatusEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
```

> 注意：contract 中的状态枚举用于**序列化契约**；domain 层如有状态机，使用 domain 自己的状态枚举（含 TRANSITIONS），两者由 assembler/converter 映射，禁止把领域状态机下沉到 contract。

## 5. Feign Client（{svc}-feign-client）

```java
package {basePackage}.{prefix}.feign;

import {basePackage}.{prefix}.contract.constant.{X}ApiPath;
import {basePackage}.{prefix}.contract.constant.{X}ServiceName;
import {basePackage}.{prefix}.contract.dto.response.{Entity}DetailResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * {description} Feign 客户端（由服务提供方维护，调用方直接依赖本模块）
 */
@FeignClient(
        name = {X}ServiceName.NAME,
        path = {X}ApiPath.BASE,
        fallbackFactory = {X}FeignFallbackFactory.class
)
public interface {X}FeignClient {

    @GetMapping("/{id}")
    {Entity}DetailResponse detail(@PathVariable("id") String id);
}
```

### FallbackFactory（降级不抛异常，返回语义明确的降级响应）

```java
package {basePackage}.{prefix}.feign;

import {basePackage}.{prefix}.contract.dto.response.{Entity}DetailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class {X}FeignFallbackFactory implements FallbackFactory<{X}FeignClient> {

    private static final Logger log = LoggerFactory.getLogger({X}FeignFallbackFactory.class);

    @Override
    public {X}FeignClient create(Throwable cause) {
        return id -> {
            log.warn("[{svc}-feign] detail fallback, id={}, cause={}", id, cause.toString());
            return {Entity}DetailResponse.unavailable(id);
        };
    }
}
```

## 6. 契约维护铁律

1. **只增不改**：废弃字段标 `@Deprecated` 保留至少一个大版本；CI 用 oasdiff 拦截破坏性变更
2. DTO 里**不出现**领域对象、PO、框架类型（如 `IPage`）；分页响应统一 `PageResult<T>`
3. Feign 方法签名必须与 Controller 保持一致（同路径常量、同 DTO）
4. `feign-client` 模块的 openfeign 依赖必须 `optional`
