# 领域层模板（{svc}-domain）

> domain 层**纯 Java**：只依赖 common-core + jspecify，禁止 Spring/MyBatis/Jackson 注解。
> 业务规则内聚在聚合行为方法中；application 层只做编排，不写业务规则。

## 1. 聚合根（model/{Entity}.java）

```java
package {basePackage}.{prefix}.domain.model;

import {basePackage}.{prefix}.domain.command.Create{Entity}Command;
import {basePackage}.{prefix}.domain.event.{Entity}CreatedEvent;
import {basePackage}.{prefix}.domain.exception.{Entity}StatusException;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {description}聚合根
 *
 * 设计要点：
 * 1. 私有构造 + 工厂方法创建，保证不变量
 * 2. 状态变更必须通过行为方法，内部做状态机校验
 * 3. 领域事件先暂存聚合内，由仓储实现落库时取出（配合 Outbox）
 */
public class {Entity} {

    private String id;
    private {Entity}Status status;
    private Money amount;
    private long version;
    private OffsetDateTime createTime;
    private OffsetDateTime updateTime;

    /** 未发布的领域事件（随聚合持久化流程由 RepositoryImpl 写入 Outbox） */
    private final List<Object> domainEvents = new ArrayList<>();

    private {Entity}() {
    }

    /** 工厂方法：创建聚合的唯一入口 */
    public static {Entity} create(Create{Entity}Command command) {
        {Entity} entity = new {Entity}();
        entity.id = command.id();          // 幂等：ID 由应用层用 Snowflake 预生成
        entity.status = {Entity}Status.PENDING;
        entity.amount = command.amount();
        entity.createTime = OffsetDateTime.now();
        entity.registerEvent(new {Entity}CreatedEvent(entity.id, OffsetDateTime.now()));
        return entity;
    }

    // ========== 行为方法（业务规则内聚于此） ==========

    public void confirm() {
        this.status.validateTransition({Entity}Status.COMPLETED);
        this.status = {Entity}Status.COMPLETED;
        this.updateTime = OffsetDateTime.now();
    }

    public void cancel() {
        this.status.validateTransition({Entity}Status.CLOSED);
        this.status = {Entity}Status.CLOSED;
        this.updateTime = OffsetDateTime.now();
    }

    // ========== 领域事件 ==========

    protected void registerEvent(Object event) {
        this.domainEvents.add(event);
    }

    public List<Object> pullDomainEvents() {
        List<Object> events = List.copyOf(this.domainEvents);
        this.domainEvents.clear();
        return events;
    }

    // ========== getter（不提供 setter，状态变更走行为方法） ==========

    public String getId() { return id; }
    public {Entity}Status getStatus() { return status; }
    public Money getAmount() { return amount; }
    public long getVersion() { return version; }
    public OffsetDateTime getCreateTime() { return createTime; }
    public OffsetDateTime getUpdateTime() { return updateTime; }

    /** 仅 RepositoryImpl 重建聚合时使用（从 PO 还原全部字段） */
    public static {Entity} restore(String id, {Entity}Status status, Money amount,
                                   long version, OffsetDateTime createTime, OffsetDateTime updateTime) {
        {Entity} entity = new {Entity}();
        entity.id = id;
        entity.status = status;
        entity.amount = amount;
        entity.version = version;
        entity.createTime = createTime;
        entity.updateTime = updateTime;
        return entity;
    }
}
```

## 2. 状态机枚举（model/{Entity}Status.java）

```java
package {basePackage}.{prefix}.domain.model;

import {basePackage}.{prefix}.domain.exception.{Entity}StatusException;

import java.util.Map;
import java.util.Set;

/**
 * {description}状态机：所有状态流转必须经过 validateTransition
 */
public enum {Entity}Status {

    PENDING, PROCESSING, COMPLETED, CLOSED;

    private static final Map<{Entity}Status, Set<{Entity}Status>> TRANSITIONS = Map.of(
            PENDING, Set.of(PROCESSING, CLOSED),
            PROCESSING, Set.of(COMPLETED, CLOSED),
            COMPLETED, Set.of(),
            CLOSED, Set.of()
    );

    public void validateTransition({Entity}Status target) {
        if (!TRANSITIONS.getOrDefault(this, Set.of()).contains(target)) {
            throw new {Entity}StatusException(this, target);
        }
    }
}
```

## 3. 值对象（model/Money.java）

```java
package {basePackage}.{prefix}.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额值对象：不可变，运算产生新实例
 */
public record Money(BigDecimal amount) implements Comparable<Money> {

    public Money {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("金额不能为空或负数");
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money subtract(Money other) {
        return new Money(this.amount.subtract(other.amount));
    }

    @Override
    public int compareTo(Money other) {
        return this.amount.compareTo(other.amount);
    }
}
```

## 4. 命令与查询（command/、query/）

```java
package {basePackage}.{prefix}.domain.command;

import {basePackage}.{prefix}.domain.model.Money;
import org.jspecify.annotations.Nullable;

/** 创建{description}命令（application 层由 Request DTO 转换而来） */
public record Create{Entity}Command(String id, Money amount, @Nullable String remark) {
}
```

```java
package {basePackage}.{prefix}.domain.query;

/** {description}领域查询条件（写侧仓储用；读侧 CQRS 直接用 contract 的 QueryRequest） */
public record {Entity}Query(String status, String keyword, long current, long size) {
}
```

## 5. 领域事件（event/）

```java
package {basePackage}.{prefix}.domain.event;

import java.time.OffsetDateTime;

/**
 * {description}已创建事件
 * 经 Outbox 表落库后异步发布到 RocketMQ
 */
public record {Entity}CreatedEvent(String {entity}Id, OffsetDateTime occurredAt) {
}
```

## 6. 仓储接口（repository/，仅接口）

```java
package {basePackage}.{prefix}.domain.repository;

import {basePackage}.{prefix}.domain.model.{Entity};
import {basePackage}.{prefix}.domain.query.{Entity}Query;

import java.util.List;
import java.util.Optional;

/**
 * {description}仓储接口：domain 层定义，infrastructure 层实现
 * 方法签名只出现领域类型，禁止暴露 PO / IPage
 */
public interface {Entity}Repository {

    {Entity} save({Entity} entity);

    Optional<{Entity}> findById(String id);

    List<{Entity}> findByQuery({Entity}Query query);

    boolean existsByRequestId(String requestId);

    void deleteById(String id);
}
```

## 7. 领域异常（exception/）

```java
package {basePackage}.{prefix}.domain.exception;

/**
 * {description}不存在
 */
public class {Entity}NotFoundException extends RuntimeException {

    private final String {entity}Id;

    public {Entity}NotFoundException(String {entity}Id) {
        super("未找到{description}: " + {entity}Id);
        this.{entity}Id = {entity}Id;
    }

    public String get{Entity}Id() {
        return {entity}Id;
    }
}
```

```java
package {basePackage}.{prefix}.domain.exception;

import {basePackage}.{prefix}.domain.model.{Entity}Status;

/**
 * {description}状态流转非法
 */
public class {Entity}StatusException extends RuntimeException {

    private final {Entity}Status currentStatus;
    private final {Entity}Status targetStatus;

    public {Entity}StatusException({Entity}Status current, {Entity}Status target) {
        super("状态不允许流转: " + current + " -> " + target);
        this.currentStatus = current;
        this.targetStatus = target;
    }

    public {Entity}Status getCurrentStatus() { return currentStatus; }
    public {Entity}Status getTargetStatus() { return targetStatus; }
}
```

> 异常携带上下文字段（如 id、currentStatus），由 application 层 `GlobalExceptionHandler` 提取并写入 Problem Detail 的扩展属性。

## 8. 领域服务（service/，可选）

仅当逻辑跨多个聚合或不属于任何单一聚合时创建：

```java
package {basePackage}.{prefix}.domain.service;

import {basePackage}.{prefix}.domain.model.{Entity};
import {basePackage}.{prefix}.domain.model.Money;

/**
 * {description}定价领域服务（示例）：纯 Java，无 Spring 注解，
 * 由 application 层以 constructor 注入编排（可在 application config 中 @Bean 注册）
 */
public class {Entity}PricingService {

    public Money calculate({Entity} entity) {
        // 领域规则...
        return entity.getAmount();
    }
}
```

## 铁律速查

1. 聚合不设 setter；状态变更走行为方法；不变量在工厂方法中保证
2. 跨服务操作不直接出现在 domain；由 application 层编排 Saga
3. 仓储接口禁止暴露 PO、`IPage`、SQL 概念
4. 领域事件先进聚合的 `domainEvents`，由 RepositoryImpl 在同事务写入 Outbox
