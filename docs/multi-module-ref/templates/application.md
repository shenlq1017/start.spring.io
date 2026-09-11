# 应用层模板（{svc}-application）

> 应用层只做编排：参数校验 → 领域行为 → 仓储 → Outbox；不写业务规则。
> Controller 直接返回 DTO / PageResult，**禁止任何全局包装**。

## 1. Controller

```java
package {basePackage}.{prefix}.controller;

import com.enterprise.common.core.page.PageResult;
import {basePackage}.{prefix}.assembler.{Entity}Assembler;
import {basePackage}.{prefix}.contract.constant.{X}ApiPath;
import {basePackage}.{prefix}.contract.dto.request.*;
import {basePackage}.{prefix}.contract.dto.response.{Entity}DetailResponse;
import {basePackage}.{prefix}.contract.dto.response.{Entity}SummaryResponse;
import {basePackage}.{prefix}.service.{Entity}ApplicationService;
import {basePackage}.{prefix}.service.{Entity}QueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * {description}接口
 */
@Tag(name = "{description}管理")
@RestController
@RequestMapping({X}ApiPath.BASE)   // 路径常量来自 contract，与 Feign 共用
@RequiredArgsConstructor
public class {Entity}Controller {

    private final {Entity}ApplicationService {entity}ApplicationService;
    private final {Entity}QueryService {entity}QueryService;
    private final {Entity}Assembler {entity}Assembler;

    @Operation(summary = "分页查询{description}")
    @GetMapping
    public PageResult<{Entity}SummaryResponse> page(@Validated Query{Entity}Request query) {
        // 读侧 CQRS：直接投影 DTO，不经过领域对象
        return {entity}QueryService.page(query);
    }

    @Operation(summary = "获取{description}详情")
    @GetMapping("/{id}")
    public {Entity}DetailResponse detail(@Parameter(description = "ID") @PathVariable String id) {
        return {entity}QueryService.detail(id);
    }

    @Operation(summary = "创建{description}")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public {Entity}DetailResponse create(@RequestBody @Validated Create{Entity}Request request) {
        return {entity}ApplicationService.create({entity}Assembler.toCommand(request));
    }

    @Operation(summary = "取消{description}")
    @PostMapping("/{id}/cancel")
    public {Entity}DetailResponse cancel(@Parameter(description = "ID") @PathVariable String id,
                                         @RequestBody @Validated Cancel{Entity}Request request) {
        return {entity}ApplicationService.cancel(id, request);
    }

    @Operation(summary = "删除{description}")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "ID") @PathVariable String id) {
        {entity}ApplicationService.delete(id);
    }
}
```

**Controller 规范**：
- 写操作路径用子资源动作（`/{id}/cancel`），避免在一个接口里用 switch 分发（Map 返回仅限确属动态结果的场景）
- 方法级权限：`@PreAuthorize("hasRole('{X}_ADMIN')")`，按需在方法上标注
- 创建返回 201 + 详情 DTO；删除返回 204；更新/动作返回最新详情 DTO

## 2. ApplicationService（写侧编排）

```java
package {basePackage}.{prefix}.service;

import com.enterprise.common.core.id.SnowflakeIdGenerator;
import {basePackage}.{prefix}.assembler.{Entity}Assembler;
import {basePackage}.{prefix}.contract.dto.response.{Entity}DetailResponse;
import {basePackage}.{prefix}.domain.command.Create{Entity}Command;
import {basePackage}.{prefix}.domain.exception.{Entity}NotFoundException;
import {basePackage}.{prefix}.domain.model.{Entity};
import {basePackage}.{prefix}.domain.repository.{Entity}Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {description}应用服务（写侧）：编排领域行为，业务规则在聚合内
 */
@Service
@RequiredArgsConstructor
public class {Entity}ApplicationService {

    private final {Entity}Repository {entity}Repository;
    private final {Entity}Assembler {entity}Assembler;
    private final SnowflakeIdGenerator idGenerator;

    @Transactional(rollbackFor = Exception.class)
    public {Entity}DetailResponse create(Create{Entity}Command command) {
        // 幂等：requestId 唯一索引；冲突时直接返回已存在的单据
        // ID 由应用层 Snowflake 预生成，Command 中已携带
        {Entity} entity = {Entity}.create(command);
        {Entity} saved = {entity}Repository.save(entity);
        // 领域事件由 RepositoryImpl 在同一事务写入 Outbox，无需在此显式发布
        return {entity}Assembler.toDetailResponse(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public {Entity}DetailResponse cancel(String id, Cancel{Entity}Request request) {
        {Entity} entity = {entity}Repository.findById(id)
                .orElseThrow(() -> new {Entity}NotFoundException(id));
        entity.cancel();   // 状态机校验在聚合内
        {entity}Repository.save(entity);
        return {entity}Assembler.toDetailResponse(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        {entity}Repository.findById(id)
                .orElseThrow(() -> new {Entity}NotFoundException(id));
        {entity}Repository.deleteById(id);
    }
}
```

> **写后读一致性**：创建/更新后紧接着的查询走主库路由（同事务内查询天然主库）；跨请求场景用基于 ID 的短时缓存兜底复制延迟。

## 3. QueryService（读侧 CQRS，默认具体类）

> **与 start.spring.io / P11 对齐**：`ApplicationService` / `QueryService` 默认生成**具体 `@Service` 类**，不强制 `Interface` + `service/impl/`。仅当同一端口需多实现（如多数据源读模型）时再拆接口；脚手架与 Initializr 增强 CRUD 默认不生成 impl。

```java
package {basePackage}.{prefix}.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.common.core.page.PageResult;
import {basePackage}.{prefix}.contract.dto.request.Query{Entity}Request;
import {basePackage}.{prefix}.contract.dto.response.{Entity}DetailResponse;
import {basePackage}.{prefix}.contract.dto.response.{Entity}SummaryResponse;
import {basePackage}.{prefix}.domain.exception.{Entity}NotFoundException;
import {basePackage}.{prefix}.infrastructure.persistence.mapper.{Entity}ReadMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {description}查询服务（读侧 CQRS）：
 * ReadMapper XML 直接投影 DTO，不加载聚合、不触发领域逻辑；具体类，无 Interface+Impl
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class {Entity}QueryService {

    // 注入 infrastructure 的 ReadMapper（bootstrap 完成装配；读侧允许直接依赖 Mapper 投影）
    private final {Entity}ReadMapper {entity}ReadMapper;

    public {Entity}DetailResponse detail(String id) {
        {Entity}DetailResponse detail = {entity}ReadMapper.selectDetailById(id);
        if (detail == null) {
            throw new {Entity}NotFoundException(id);
        }
        return detail;
    }

    public PageResult<{Entity}SummaryResponse> page(Query{Entity}Request query) {
        Page<{Entity}SummaryResponse> page = Page.of(query.current(), query.size());
        return PageResult.of(
                {entity}ReadMapper.selectSummaryPage(page, query).getRecords(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize());
    }
}
```

> 读侧 Mapper（`{Entity}ReadMapper`，接口放 infrastructure.persistence.mapper，XML 投影 DTO）见 [infrastructure.md](infrastructure.md) 第 5 节。

## 4. Assembler（DTO ↔ Command/Domain 转换）

```java
package {basePackage}.{prefix}.assembler;

import {basePackage}.{prefix}.contract.dto.request.Create{Entity}Request;
import {basePackage}.{prefix}.contract.dto.response.{Entity}DetailResponse;
import {basePackage}.{prefix}.domain.command.Create{Entity}Command;
import {basePackage}.{prefix}.domain.model.{Entity};
import {basePackage}.{prefix}.domain.model.Money;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * {description}装配器：contract DTO <-> domain 类型
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface {Entity}Assembler {

    @Mapping(target = "id", ignore = true)   // ID 由 ApplicationService 用 Snowflake 生成后填充
    @Mapping(target = "amount", expression = "java(Money.of(request.amount().toPlainString()))")
    Create{Entity}Command toCommand(Create{Entity}Request request);

    @Mapping(target = "available", constant = "true")
    @Mapping(target = "status", expression = "java(entity.getStatus().name())")
    @Mapping(target = "amount", expression = "java(entity.getAmount().amount())")
    {Entity}DetailResponse toDetailResponse({Entity} entity);
}
```

## 5. GlobalExceptionHandler（Problem Details）

```java
package {basePackage}.{prefix}.advice;

import {basePackage}.{prefix}.domain.exception.{Entity}NotFoundException;
import {basePackage}.{prefix}.domain.exception.{Entity}StatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;

/**
 * 全局异常处理：统一 Problem Details（RFC 7807），业务错误码用 type URI 承载
 * 前置：application.yml 开启 spring.mvc.problemdetails.enabled: true
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String ERROR_TYPE_BASE = "https://api.example.com/errors/";

    @ExceptionHandler({Entity}NotFoundException.class)
    public ProblemDetail handleNotFound({Entity}NotFoundException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setType(URI.create(ERROR_TYPE_BASE + "{prefix}-not-found"));
        detail.setTitle("{description}不存在");
        detail.setProperty("{entity}Id", ex.get{Entity}Id());
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler({Entity}StatusException.class)
    public ProblemDetail handleStatus({Entity}StatusException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        detail.setType(URI.create(ERROR_TYPE_BASE + "{prefix}-status-conflict"));
        detail.setTitle("{description}状态异常");
        detail.setProperty("currentStatus", ex.getCurrentStatus().name());
        detail.setProperty("targetStatus", ex.getTargetStatus().name());
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }
}
```

> 通用校验异常（`MethodArgumentNotValidException` 等）由 `ResponseEntityExceptionHandler` 父类按 ProblemDetail 输出；common-web 可提供共享基类，服务级 Advice 只处理本服务领域异常。

## 铁律速查

1. Controller 禁止出现 `ApiResponse`/`R`，禁止使用 `ResponseBodyAdvice` 自动包装
2. ApplicationService 只做编排：装配命令 → 聚合行为 → 仓储 → 返回；业务规则写进聚合
3. 读侧走具体类 QueryService + XML 投影；写侧走具体类 ApplicationService + 聚合 + 仓储；两者不混用；默认无 Interface+impl/
4. 分页响应统一 `PageResult<T>`，禁止直接暴露 MyBatis-Plus `IPage`
