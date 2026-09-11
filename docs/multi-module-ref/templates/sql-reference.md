# SQL 参考（PostgreSQL 17 + Flyway）

> 脚本放 `{svc}-bootstrap/src/main/resources/db/migration/`。
> 占位符：`{table_name}`、`{aggregate}`、`{description}`。

## 1. Flyway 命名与回滚

| 类型 | 命名 | 位置 | 说明 |
|------|------|------|------|
| 版本迁移 | `V{x.y.z}__{snake_desc}.sql` | `db/migration/` | 如 `V1.0.0__create_order_tables.sql` |
| 紧急回滚 | `U{x.y.z}__rollback_{desc}.sql` | `deploy/scripts/` | 手动触发；**前向修复优先** |

铁律：
- 已合并的 V 脚本**禁止修改**，变更走新脚本
- 迁移前自动备份数据库快照
- 每个服务独立 Database，脚本中禁止出现跨库对象

## 2. 字段类型对照（PostgreSQL ↔ Java）

| 场景 | PostgreSQL | Java | 说明 |
|------|-----------|------|------|
| 主键 | VARCHAR(64) | String | Snowflake（`IdType.ASSIGN_ID`），应用层生成 |
| 短字符串 | VARCHAR(n) | String | n ≤ 255 |
| 长文本 | TEXT | String | TEXT 与 VARCHAR 性能无差异，长文本直接用 TEXT |
| 整数 | INTEGER | Integer | |
| 长整数 | BIGINT | Long | |
| 状态/标记 | SMALLINT 或 VARCHAR(32) | Integer / String | 有状态机的枚举建议存 VARCHAR（可读性、避免序数耦合） |
| 布尔 | BOOLEAN | Boolean | 禁止 TINYINT |
| 金额 | NUMERIC(19,2) | BigDecimal | 普通金额可 NUMERIC(10,2)；汇率 NUMERIC(10,4) |
| 时间（默认） | **TIMESTAMPTZ** | **OffsetDateTime** | 平台统一带时区 |
| 日期 | DATE | LocalDate | 仅日期 |
| JSON | **JSONB** | String（JsonbTypeHandler） | 禁止 JSON 类型；禁止 JacksonTypeHandler |
| 乐观锁 | INT | Long | `version INT NOT NULL DEFAULT 0` |

字符串长度建议：名称 VARCHAR(200)、手机号 VARCHAR(20)、邮箱 VARCHAR(100)、URL VARCHAR(500)、IP VARCHAR(45)。

## 3. 业务建表模板

```sql
CREATE TABLE {table_name} (
    id           VARCHAR(64)  PRIMARY KEY,
    request_id   VARCHAR(64)  NOT NULL,               -- 幂等键（唯一索引）
    -- ========== 业务字段 ==========
    status       VARCHAR(32)  NOT NULL,
    amount       NUMERIC(19,2) NOT NULL,
    extension    JSONB,                                 -- 扩展字段（JsonbTypeHandler）
    -- ========== 通用字段 ==========
    version      INT          NOT NULL DEFAULT 0,     -- 乐观锁（@Version）
    deleted      BOOLEAN      NOT NULL DEFAULT FALSE, -- 逻辑删除（@TableLogic）
    create_by    VARCHAR(64),
    create_time  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    update_by    VARCHAR(64),
    update_time  TIMESTAMPTZ
);

-- 幂等唯一索引
CREATE UNIQUE INDEX uk_{table_name}_request_id ON {table_name} (request_id);

COMMENT ON TABLE  {table_name}               IS '{description}';
COMMENT ON COLUMN {table_name}.id            IS '主键（Snowflake）';
COMMENT ON COLUMN {table_name}.request_id    IS '幂等请求ID';
COMMENT ON COLUMN {table_name}.status        IS '状态';
COMMENT ON COLUMN {table_name}.extension     IS '扩展字段（JSONB）';
COMMENT ON COLUMN {table_name}.version       IS '乐观锁版本号';
COMMENT ON COLUMN {table_name}.deleted       IS '删除标记';
COMMENT ON COLUMN {table_name}.create_time   IS '创建时间';
COMMENT ON COLUMN {table_name}.update_time   IS '更新时间';
```

## 4. 索引规范

| 类型 | 命名 | 示例 |
|------|------|------|
| 普通索引 | `idx_{table}_{col}` | `idx_order_user_id` |
| 唯一索引 | `uk_{table}_{col}` | `uk_order_request_id` |
| 联合索引 | `idx_{table}_{c1}_{c2}`（高选择性在前） | `idx_order_status_create_time` |
| JSONB 索引 | `idx_{table}_{col}_gin`，`USING GIN` | `idx_order_extension_gin` |

原则：只为 WHERE / ORDER BY / JOIN 字段建索引；避免过度索引影响写入。

## 5. 平台通用表（直接复用，勿改结构语义）

### 5.1 Saga 执行表

```sql
CREATE TABLE saga_executions (
    execution_id    TEXT PRIMARY KEY,
    saga_id         TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL,       -- RUNNING / COMPLETED / COMPENSATING / FAILED
    current_step    INT NOT NULL,
    completed_steps JSONB NOT NULL DEFAULT '[]',
    context         JSONB NOT NULL,             -- 业务上下文快照
    deadline        TIMESTAMPTZ,
    retry_count     INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_saga_status ON saga_executions(status, deadline);

COMMENT ON TABLE saga_executions IS 'Saga 编排状态表（重启恢复依据）';
```

### 5.2 Outbox 表（按聚合建表，按月分区）

```sql
CREATE TABLE {aggregate}_outbox (
    id            BIGSERIAL PRIMARY KEY,
    aggregate_id  VARCHAR(64) NOT NULL,
    event_type    VARCHAR(100) NOT NULL,
    payload       JSONB NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at  TIMESTAMPTZ
) PARTITION BY RANGE (created_at);              -- 按月分区，便于清理归档

CREATE INDEX idx_{aggregate}_outbox_status ON {aggregate}_outbox(status, created_at);

COMMENT ON TABLE {aggregate}_outbox IS '{description} Outbox 事件表';
```

> 已发布记录保留 7 天，凌晨低峰清理（`@Scheduled(cron = "0 0 3 * * ?")`），清理前复制到 `{aggregate}_outbox_archive`。

### 5.3 幂等记录表

```sql
CREATE TABLE idempotent_record (
    id             BIGSERIAL PRIMARY KEY,
    idempotent_key VARCHAR(128) NOT NULL,
    business_type  VARCHAR(50)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
) PARTITION BY RANGE (created_at);

CREATE UNIQUE INDEX uk_idempotent_key ON idempotent_record(idempotent_key);

COMMENT ON TABLE idempotent_record IS '幂等记录表（INSERT ... ON CONFLICT DO NOTHING 原子去重）';
```

> 保留期限按 business_type 差异化：创建类 30 天、支付回调 90 天、MQ 消费去重 7 天。

### 5.4 审计日志表

```sql
CREATE TABLE {aggregate}_audit_log (
    id            BIGSERIAL PRIMARY KEY,
    {aggregate}_id VARCHAR(64) NOT NULL,
    operation     VARCHAR(50)  NOT NULL,        -- CREATE / CANCEL / PAY / REFUND
    operator_id   VARCHAR(64)  NOT NULL,
    operator_ip   VARCHAR(45),
    before_state  JSONB,
    after_state   JSONB,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
) PARTITION BY RANGE (created_at);

CREATE INDEX idx_audit_{aggregate} ON {aggregate}_audit_log({aggregate}_id, created_at);
CREATE INDEX idx_audit_operator ON {aggregate}_audit_log(operator_id, created_at);

COMMENT ON TABLE {aggregate}_audit_log IS '{description}操作审计表（与业务操作同事务写入）';
```

## 6. 常用变更 SQL

```sql
-- 添加字段
ALTER TABLE {table_name} ADD COLUMN {col} VARCHAR(100);
COMMENT ON COLUMN {table_name}.{col} IS '{描述}';

-- 修改类型
ALTER TABLE {table_name} ALTER COLUMN {col} TYPE TEXT;

-- 添加索引
CREATE INDEX idx_{table_name}_{col} ON {table_name} ({col});

-- 幂等写入（原子去重）
INSERT INTO idempotent_record(idempotent_key, business_type)
VALUES (#{key}, #{type}) ON CONFLICT DO NOTHING;

-- JSONB 查询
SELECT * FROM {table_name} WHERE extension->>'{key}' = #{value};

-- 不区分大小写模糊查询
SELECT * FROM {table_name} WHERE name ILIKE CONCAT('%', #{keyword}, '%');
```

## 7. PostgreSQL 注意事项

- `COMMENT ON` 独立语句，不支持行内 COMMENT
- 字符串比较默认区分大小写，需要不区分时用 `ILIKE`
- 时间一律 `TIMESTAMPTZ`，禁止 `TIMESTAMP`（避免时区歧义）
- 分区表主键必须包含分区键（BIGSERIAL id + created_at 组合或接受其约束）
