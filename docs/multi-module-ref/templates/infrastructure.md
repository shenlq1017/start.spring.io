# 基础设施层模板（{svc}-infrastructure）

> 职责：PO 与 Mapper、仓储实现、缓存、MQ、Outbox、Saga、外部服务 ACL。
> 占位符：`{basePackage}`、`{prefix}`、`{Entity}`、`{entity}`、`{table_name}`、`{description}`。

## 1. 持久化对象 PO（persistence/entity）

```java
package {basePackage}.{prefix}.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.enterprise.common.mybatis.handler.JsonbTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * {description}持久化对象（与领域模型严格分离，经 {Entity}Converter 互转）
 */
@Data
@TableName(value = "{table_name}", autoResultMap = true)
public class {Entity}PO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键：Snowflake（Nacos 感知分配 workerId） */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 幂等键（唯一索引 uk_{table_name}_request_id） */
    private String requestId;

    // ========== 业务字段 ==========

    private String status;

    private BigDecimal amount;

    /** JSONB 扩展字段：必须用 common-mybatis 的 JsonbTypeHandler（Jackson 3），禁止 JacksonTypeHandler */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extension;

    // ========== 通用字段 ==========

    /** 乐观锁 */
    @Version
    private Long version;

    @TableLogic
    private Boolean deleted;

    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createTime;

    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updateTime;
}
```

## 2. Mapper 接口（persistence/mapper）

**核心原则：Mapper 保持干净，单表查询在 RepositoryImpl 用 Lambda。**

```java
package {basePackage}.{prefix}.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import {basePackage}.{prefix}.infrastructure.persistence.entity.{Entity}PO;
import org.apache.ibatis.annotations.Mapper;

/**
 * {description} Mapper（写侧）
 */
@Mapper
public interface {Entity}Mapper extends BaseMapper<{Entity}PO> {
    // 单表条件/分页/COUNT/exists：禁止在此写 SQL，RepositoryImpl 用 lambdaQuery()
}
```

**仅以下场景允许自定义 SQL（接口 + XML）**：联表查询、窗口函数/复杂子查询、PostgreSQL 特有函数（JSONB 操作符、ILIKE）、批量 `INSERT ... ON CONFLICT`。

## 3. 仓储实现（persistence/repository）

```java
package {basePackage}.{prefix}.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import {basePackage}.{prefix}.domain.model.{Entity};
import {basePackage}.{prefix}.domain.model.{Entity}Status;
import {basePackage}.{prefix}.domain.query.{Entity}Query;
import {basePackage}.{prefix}.domain.repository.{Entity}Repository;
import {basePackage}.{prefix}.infrastructure.persistence.converter.{Entity}Converter;
import {basePackage}.{prefix}.infrastructure.persistence.entity.{Entity}PO;
import {basePackage}.{prefix}.infrastructure.persistence.mapper.{Entity}Mapper;
import {basePackage}.{prefix}.infrastructure.outbox.OutboxEventWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * {description}仓储实现：PO <-> Domain 经 Converter 互转，签名只暴露领域类型
 */
@Repository
@RequiredArgsConstructor
public class {Entity}RepositoryImpl implements {Entity}Repository {

    private final {Entity}Mapper {entity}Mapper;
    private final {Entity}Converter {entity}Converter;
    private final OutboxEventWriter outboxEventWriter;

    @Override
    public {Entity} save({Entity} entity) {
        {Entity}PO po = {entity}Converter.toPO(entity);
        if (po.getVersion() == null) {
            {entity}Mapper.insert(po);
        } else {
            {entity}Mapper.updateById(po);   // @Version 乐观锁自动生效
        }
        // 领域事件与业务数据同事务写入 Outbox，由后台任务/CDC 发布到 RocketMQ
        outboxEventWriter.writeAll(entity.pullDomainEvents());
        return {entity}Converter.toDomain(po);
    }

    @Override
    public Optional<{Entity}> findById(String id) {
        return Optional.ofNullable({entity}Mapper.selectById(id))
                .map({entity}Converter::toDomain);
    }

    @Override
    public List<{Entity}> findByQuery({Entity}Query query) {
        return {entity}Mapper.selectList(Wrappers.<{Entity}PO>lambdaQuery()
                        .eq(StringUtils.hasText(query.status()),
                                {Entity}PO::getStatus, query.status())
                        .orderByDesc({Entity}PO::getCreateTime))
                .stream()
                .map({entity}Converter::toDomain)
                .toList();
    }

    @Override
    public boolean existsByRequestId(String requestId) {
        return {entity}Mapper.exists(Wrappers.<{Entity}PO>lambdaQuery()
                .eq({Entity}PO::getRequestId, requestId));
    }

    @Override
    public void deleteById(String id) {
        {entity}Mapper.deleteById(id);   // @TableLogic 逻辑删除
    }
}
```

## 4. MapStruct Converter（persistence/converter）

```java
package {basePackage}.{prefix}.infrastructure.persistence.converter;

import {basePackage}.{prefix}.domain.model.{Entity};
import {basePackage}.{prefix}.domain.model.{Entity}Status;
import {basePackage}.{prefix}.domain.model.Money;
import {basePackage}.{prefix}.infrastructure.persistence.entity.{Entity}PO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

/**
 * PO <-> Domain 转换（MapStruct，编译期生成，禁止手写 BeanUtil 拷贝）
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface {Entity}Converter {

    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    @Mapping(target = "amount", source = "amount", qualifiedByName = "moneyToDecimal")
    {Entity}PO toPO({Entity} entity);

    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatus")
    @Mapping(target = "amount", source = "amount", qualifiedByName = "decimalToMoney")
    {Entity} toDomain({Entity}PO po);

    @Named("statusToString")
    default String statusToString({Entity}Status status) {
        return status == null ? null : status.name();
    }

    @Named("stringToStatus")
    default {Entity}Status stringToStatus(String status) {
        return status == null ? null : {Entity}Status.valueOf(status);
    }

    @Named("moneyToDecimal")
    default java.math.BigDecimal moneyToDecimal(Money money) {
        return money == null ? null : money.amount();
    }

    @Named("decimalToMoney")
    default Money decimalToMoney(java.math.BigDecimal amount) {
        return amount == null ? null : new Money(amount);
    }
}
```

> 领域对象的重建（含全部私有字段）通过 `{Entity}.restore(...)` 工厂完成；若 MapStruct 无法直接映射私有构造，在 Converter 中用 `expression = "java(...)"` 调 restore。

## 5. 读侧 CQRS Mapper + XML 投影

```java
package {basePackage}.{prefix}.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import {basePackage}.{prefix}.contract.dto.request.Query{Entity}Request;
import {basePackage}.{prefix}.contract.dto.response.{Entity}DetailResponse;
import {basePackage}.{prefix}.contract.dto.response.{Entity}SummaryResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * {description}读侧 Mapper（CQRS 查询投影，直接出 DTO）
 */
@Mapper
public interface {Entity}ReadMapper {

    {Entity}DetailResponse selectDetailById(@Param("id") String id);

    IPage<{Entity}SummaryResponse> selectSummaryPage(Page<?> page,
                                                     @Param("query") Query{Entity}Request query);
}
```

`src/main/resources/mapper/{Entity}ReadMapper.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="{basePackage}.{prefix}.infrastructure.persistence.mapper.{Entity}ReadMapper">

    <select id="selectDetailById" resultType="{basePackage}.{prefix}.contract.dto.response.{Entity}DetailResponse">
        SELECT id, status, amount, create_time AS createTime, TRUE AS available
        FROM {table_name}
        WHERE id = #{id} AND deleted = FALSE
    </select>

    <!-- 分页查询：XML 中不写 LIMIT，PaginationInnerInterceptor 自动处理 -->
    <select id="selectSummaryPage" resultType="{basePackage}.{prefix}.contract.dto.response.{Entity}SummaryResponse">
        SELECT id, status, amount, create_time AS createTime
        FROM {table_name}
        WHERE deleted = FALSE
        <if test="query.status != null and query.status != ''">
            AND status = #{query.status}
        </if>
        <if test="query.keyword != null and query.keyword != ''">
            AND name ILIKE CONCAT('%', #{query.keyword}, '%')
        </if>
        ORDER BY create_time DESC
    </select>
</mapper>
```

> 带 `LEFT JOIN` 时**所有表和字段必须加别名**，否则 COUNT 优化可能生成错误 SQL。

## 6. MyBatis-Plus 配置（config）

```java
package {basePackage}.{prefix}.infrastructure.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件必须最后添加；单数据源必须指定 DbType
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }
}
```

### 字段自动填充（persistence/handler）

```java
package {basePackage}.{prefix}.infrastructure.persistence.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", OffsetDateTime.class, OffsetDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", OffsetDateTime.class, OffsetDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", OffsetDateTime.class, OffsetDateTime.now());
    }
}
```

## 7. Outbox 事件写入（outbox/）

```java
package {basePackage}.{prefix}.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Outbox 写入器：领域事件与业务数据在同一本地事务落库
 * 发布由独立后台任务轮询 {aggregate}_outbox 表投递 RocketMQ，成功后标记 PUBLISHED
 */
@Component
@RequiredArgsConstructor
public class OutboxEventWriter {

    private final OutboxMapper outboxMapper;
    private final ObjectMapper objectMapper;   // Jackson 3：tools.jackson

    public void writeAll(List<Object> domainEvents) {
        for (Object event : domainEvents) {
            OutboxPO po = new OutboxPO();
            po.setEventType(event.getClass().getSimpleName());
            po.setPayload(objectMapper.writeValueAsString(event));
            po.setStatus("PENDING");
            outboxMapper.insert(po);
        }
    }
}
```

> 已发布记录保留 7 天，按月分区，凌晨低峰清理（`@Scheduled(cron = "0 0 3 * * ?")`），清理前归档。

## 8. Saga 编排骨架（saga/，跨服务流程时生成）

```java
package {basePackage}.{prefix}.infrastructure.saga;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {description} Saga 编排器
 * 状态持久化到 saga_executions 表，与业务数据同事务更新；
 * 重启恢复：启动时扫描 status='RUNNING' 且 updated_at 超 5 分钟的记录，
 * 用 SELECT ... FOR UPDATE 加锁后按 current_step / completed_steps 恢复或补偿。
 */
@Component
@RequiredArgsConstructor
public class {Entity}SagaOrchestrator {

    private final SagaExecutionMapper sagaExecutionMapper;
    // private final {Other}AclAdapter ...   // 各步骤通过对方 ACL 防腐层调用

    @Transactional(rollbackFor = Exception.class)
    public void execute(String executionId, SagaContext context) {
        // Step 1: 本地聚合状态变更（同事务更新 saga_executions.current_step）
        // Step 2: 调用下游服务（失败则触发已完成步骤的逆序补偿）
        // 每一步完成即持久化 completed_steps + context 快照
    }

    private void compensate(SagaExecution execution) {
        // 按 completed_steps 逆序执行补偿动作
    }
}
```

## 9. 外部服务 ACL（client/adapter）

```java
package {basePackage}.{prefix}.infrastructure.client.adapter;

import com.enterprise.common.core.exception.ExternalServiceException;
import {basePackage}.inventory.feign.InventoryFeignClient;
import {basePackage}.inventory.contract.dto.response.StockDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 库存服务防腐层：
 * 1. 调用方只依赖对方 contract + feign-client
 * 2. available=false（降级响应）在此转换为业务异常
 * 3. 对方 DTO 在此转换为本服务领域类型，禁止蔓延到上层
 */
@Component
@RequiredArgsConstructor
public class InventoryAclAdapter {

    private final InventoryFeignClient inventoryFeignClient;

    public void reserveStock(String skuId, int quantity) {
        StockDetailResponse response = inventoryFeignClient.reserve(skuId, quantity);
        if (!response.available()) {
            throw new ExternalServiceException("库存服务暂不可用");
        }
        // 转换为本服务领域概念后返回...
    }
}
```

## 铁律速查

1. PO 与领域模型分离，互转只走 MapStruct Converter，禁止 `BeanUtil.copyProperties` 跨层拷贝
2. 单表 SQL 在 RepositoryImpl 用 `lambdaQuery()`；XML 只承载复杂查询与读侧投影
3. JSONB 列用 `JsonbTypeHandler`；PO 必带 `@Version`；分页插件最后注册且指定 `DbType.POSTGRE_SQL`
4. 事件发布走 Outbox（同事务落库），禁止在业务代码里直接发 MQ
5. 跨服务调用必须经 ACL 适配器；`available=false` 转业务异常
