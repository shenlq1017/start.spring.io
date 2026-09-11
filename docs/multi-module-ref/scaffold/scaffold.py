#!/usr/bin/env python3
"""spring-boot-gen 脚手架生成器。

用法：
  python scaffold.py init    --base-package com.enterprise
  python scaffold.py service --name user-service --prefix user --description 用户 --base-package com.enterprise
  python scaffold.py crud    --service user-service --prefix user --entity User --base-package com.enterprise

设计说明：
  - 参数化的业务切片（六模块 CRUD）由 templates/*.j2 用 Jinja2 渲染，可复用于任意聚合。
  - 平台固定文件（common / starters / gateway / deploy / platform-pom）为纯静态，内嵌于
    PLATFORM_FILES 常量，一次写入。
  - 新增一个聚合：向 ENTITIES 增加定义（或从头按同样字段结构传参）后执行 crud 命令即可。
"""
from __future__ import annotations

import argparse
import os
import re
import sys
from pathlib import Path

from jinja2 import Environment, FileSystemLoader, StrictUndefined

ROOT = Path(__file__).resolve().parent
TPL_DIR = ROOT / "templates"

_env = Environment(
    loader=FileSystemLoader(str(TPL_DIR)),
    undefined=StrictUndefined,
    trim_blocks=True,
    lstrip_blocks=True,
    keep_trailing_newline=True,
)


def _render(template: str, ctx: dict) -> str:
    return _env.get_template(template).render(**ctx)


# ============================================================================
# 实体（聚合）定义：内置 User；扩展新聚合在 ENTITIES 增加同名条目即可。
# field 属性：name/java/sql/comment/required/max/unique
# ============================================================================
ENTITIES = {
    "User": {
        "description": "用户",
        "table": "sys_user",
        "resource": "users",
        "statuses": ["ACTIVE", "DISABLED", "LOCKED"],
        "status_desc": {"ACTIVE": "正常", "DISABLED": "禁用", "LOCKED": "锁定"},
        "transitions": {
            "ACTIVE": ["DISABLED", "LOCKED"],
            "DISABLED": ["ACTIVE"],
            "LOCKED": ["ACTIVE"],
        },
        "fields": [
            {"name": "username", "java": "String", "sql": "VARCHAR(50)",
             "comment": "用户名", "required": True, "max": 50, "unique": True},
            {"name": "password", "java": "String", "sql": "VARCHAR(100)",
             "comment": "密码哈希", "required": True, "max": 100},
            {"name": "email", "java": "String", "sql": "VARCHAR(100)",
             "comment": "邮箱", "required": False, "max": 100},
            {"name": "phone", "java": "String", "sql": "VARCHAR(20)",
             "comment": "手机号", "required": False, "max": 20},
            {"name": "nickname", "java": "String", "sql": "VARCHAR(50)",
             "comment": "昵称", "required": False, "max": 50},
            {"name": "avatar", "java": "String", "sql": "VARCHAR(500)",
             "comment": "头像URL", "required": False, "max": 500},
            {"name": "remark", "java": "String", "sql": "VARCHAR(500)",
             "comment": "备注", "required": False, "max": 500},
        ],
    },
    "Order": {
        "description": "订单",
        "table": "mall_order",
        "resource": "orders",
        "statuses": ["PENDING", "PAID", "SHIPPED", "COMPLETED", "CANCELLED"],
        "status_desc": {"PENDING": "待支付", "PAID": "已支付", "SHIPPED": "已发货",
                        "COMPLETED": "已完成", "CANCELLED": "已取消"},
        "transitions": {
            "PENDING": ["PAID", "CANCELLED"],
            "PAID": ["SHIPPED", "CANCELLED"],
            "SHIPPED": ["COMPLETED"],
            "COMPLETED": [],
            "CANCELLED": [],
        },
        "fields": [
            {"name": "orderNo", "java": "String", "sql": "VARCHAR(32)",
             "comment": "订单号", "required": True, "max": 32, "unique": True},
            {"name": "userId", "java": "String", "sql": "VARCHAR(64)",
             "comment": "用户ID", "required": True, "max": 64},
            {"name": "totalAmount", "java": "BigDecimal", "sql": "NUMERIC(12,2)",
             "comment": "订单金额", "required": True, "max": None},
            {"name": "receiverName", "java": "String", "sql": "VARCHAR(50)",
             "comment": "收货人", "required": True, "max": 50},
            {"name": "receiverPhone", "java": "String", "sql": "VARCHAR(20)",
             "comment": "收货电话", "required": True, "max": 20},
            {"name": "receiverAddress", "java": "String", "sql": "VARCHAR(200)",
             "comment": "收货地址", "required": True, "max": 200},
            {"name": "remark", "java": "String", "sql": "VARCHAR(500)",
             "comment": "备注", "required": False, "max": 500},
        ],
    },
}


def _to_snake(name: str) -> str:
    return re.sub(r"(?<!^)(?=[A-Z])", "_", name).lower()


# 保留字：与基座字段冲突，禁止业务字段使用
_RESERVED_FIELDS = {"id", "status", "version", "deleted", "createby", "createtime",
                    "updateby", "updatetime", "create_by", "create_time",
                    "update_by", "update_time"}


def _validate_entity(entity: str, e: dict) -> None:
    """生成前校验实体定义，尽早暴露配置错误（而不是生成出编译失败的代码）。"""
    errors: list[str] = []
    if not re.fullmatch(r"[A-Z][A-Za-z0-9]*", entity):
        errors.append(f"聚合名必须是 PascalCase: {entity}")
    if not re.fullmatch(r"[a-z][a-z0-9_]*", e.get("table", "")):
        errors.append(f"表名必须是小写下划线: {e.get('table')}")
    if not re.fullmatch(r"[a-z][a-z0-9_-]*", e.get("resource", "")):
        errors.append(f"资源路径必须是小写: {e.get('resource')}")
    statuses = e.get("statuses") or []
    if not statuses:
        errors.append("statuses 至少要有一个状态")
    desc = e.get("status_desc") or {}
    for s in statuses:
        if not re.fullmatch(r"[A-Z][A-Z0-9_]*", s):
            errors.append(f"状态名必须是大写下划线: {s}")
        if s not in desc:
            errors.append(f"status_desc 缺少状态 {s} 的中文描述")
    transitions = e.get("transitions") or {}
    for s in statuses:
        if s not in transitions:
            errors.append(f"transitions 缺少状态 {s} 的流转定义（终态配空列表）")
        else:
            for t in transitions[s]:
                if t not in statuses:
                    errors.append(f"transitions[{s}] 引用了未定义状态 {t}")
    names: set[str] = set()
    for f in e.get("fields") or []:
        n = f.get("name")
        if not n or not re.fullmatch(r"[a-z][A-Za-z0-9]*", n or ""):
            errors.append(f"字段名必须是 lowerCamelCase: {n}")
        if n in names:
            errors.append(f"字段重复: {n}")
        names.add(n)
        if n and n.lower() in _RESERVED_FIELDS:
            errors.append(f"字段 {n} 与基座保留字段冲突（id/status/version/deleted/审计字段）")
        if f.get("java") == "String" and not f.get("max"):
            errors.append(f"String 字段 {n} 必须声明 max（用于 @Size 与 VARCHAR 长度）")
        if not f.get("comment"):
            errors.append(f"字段 {n} 缺少中文 comment")
    if errors:
        raise ValueError(f"实体 {entity} 定义有误：\n  - " + "\n  - ".join(errors))


def _build_ctx(base_package: str, svc: str, prefix: str, entity: str) -> dict:
    e = ENTITIES[entity]
    _validate_entity(entity, e)
    fields = [
        {"col": _to_snake(f["name"]),
         "getter": f["name"][0].upper() + f["name"][1:], **f}
        for f in e["fields"]
    ]
    return {
        "base_package": base_package,
        "common_package": f"{base_package}.common",
        "svc": svc,
        "prefix": prefix,
        "entity": entity,
        "entity_lower": entity[0].lower() + entity[1:],
        "description": e["description"],
        "table": e["table"],
        "resource": e["resource"],
        "fields": fields,
        # 供模板条件导入使用（避免生成 unused import / 缺 import）
        "has_decimal": any(f["java"] == "BigDecimal" for f in e["fields"]),
        "has_optional": any(not f["required"] for f in e["fields"]),
        "has_required_nonstring": any(f["required"] and f["java"] != "String" for f in e["fields"]),
        "has_string": any(f["java"] == "String" for f in e["fields"]),
        "statuses": e["statuses"],
        "status_desc": e["status_desc"],
        "transitions": e["transitions"],
    }


def _write(target: Path, content: str) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8")
    print(f"  + {target}")


# ============================================================================
# 平台固定文件（纯静态）
# ============================================================================
_PLATFORM_FILES: dict[str, str] = {}

_PLATFORM_FILES["pom.xml"] = """\
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

        <spring-boot.version>4.1.1</spring-boot.version>
        <spring-cloud.version>2025.1.0</spring-cloud.version>
        <spring-cloud-alibaba.version>2025.1.0.0</spring-cloud-alibaba.version>
        <mybatis-plus.version>3.5.15</mybatis-plus.version>
        <springdoc.version>3.1.0</springdoc.version>
        <mapstruct.version>1.6.3</mapstruct.version>
        <lombok.version>1.18.42</lombok.version>
        <jspecify.version>1.0.0</jspecify.version>
        <testcontainers.version>2.0.5</testcontainers.version>
        <archunit.version>1.4.2</archunit.version>
    </properties>

    <dependencyManagement>
        <dependencies>
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
            <dependency>
                <groupId>org.jspecify</groupId>
                <artifactId>jspecify</artifactId>
                <version>${jspecify.version}</version>
            </dependency>
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct</artifactId>
                <version>${mapstruct.version}</version>
            </dependency>
            <dependency>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </dependency>
            <dependency>
                <groupId>org.springdoc</groupId>
                <artifactId>springdoc-openapi-starter-common</artifactId>
                <version>${springdoc.version}</version>
            </dependency>
            <dependency>
                <groupId>org.springdoc</groupId>
                <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
                <version>${springdoc.version}</version>
            </dependency>
            <dependency>
                <groupId>com.tngtech.archunit</groupId>
                <artifactId>archunit-junit5</artifactId>
                <version>${archunit.version}</version>
            </dependency>

            <dependency>
                <groupId>com.enterprise</groupId>
                <artifactId>common-core</artifactId>
                <version>${revision}</version>
            </dependency>
            <dependency>
                <groupId>com.enterprise</groupId>
                <artifactId>common-web</artifactId>
                <version>${revision}</version>
            </dependency>
            <dependency>
                <groupId>com.enterprise</groupId>
                <artifactId>common-mybatis</artifactId>
                <version>${revision}</version>
            </dependency>
            <dependency>
                <groupId>com.enterprise</groupId>
                <artifactId>common-redis</artifactId>
                <version>${revision}</version>
            </dependency>
            <dependency>
                <groupId>com.enterprise</groupId>
                <artifactId>common-cloud</artifactId>
                <version>${revision}</version>
            </dependency>
            <dependency>
                <groupId>com.enterprise</groupId>
                <artifactId>common-test</artifactId>
                <version>${revision}</version>
            </dependency>
            <dependency>
                <groupId>com.enterprise</groupId>
                <artifactId>common-web-starter</artifactId>
                <version>${revision}</version>
            </dependency>
            <dependency>
                <groupId>com.enterprise</groupId>
                <artifactId>common-mybatis-starter</artifactId>
                <version>${revision}</version>
            </dependency>
            <dependency>
                <groupId>com.enterprise</groupId>
                <artifactId>common-cloud-starter</artifactId>
                <version>${revision}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <configuration>
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
                    <version>1.6.0</version>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
"""


def _common_module_pom(artifactId: str, deps: str) -> str:
    return f"""\
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.enterprise</groupId>
        <artifactId>platform-parent</artifactId>
        <version>${{revision}}</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>
    <artifactId>{artifactId}</artifactId>
{deps}
</project>
"""


_PLATFORM_FILES["services/pom.xml"] = """\
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.enterprise</groupId>
        <artifactId>platform-parent</artifactId>
        <version>${revision}</version>
    </parent>
    <artifactId>services</artifactId>
    <packaging>pom</packaging>
    <modules>
    </modules>
</project>
"""

_PLATFORM_FILES["common/pom.xml"] = """\
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.enterprise</groupId>
        <artifactId>platform-parent</artifactId>
        <version>${revision}</version>
    </parent>
    <artifactId>common</artifactId>
    <packaging>pom</packaging>
    <modules>
        <module>common-core</module>
        <module>common-web</module>
        <module>common-mybatis</module>
        <module>common-redis</module>
        <module>common-cloud</module>
        <module>common-test</module>
    </modules>
</project>
"""

_PLATFORM_FILES["common/common-core/pom.xml"] = _common_module_pom(
    "common-core",
    """    <dependencies>
        <dependency>
            <groupId>org.jspecify</groupId>
            <artifactId>jspecify</artifactId>
        </dependency>
    </dependencies>""",
)

_PLATFORM_FILES["common/common-web/pom.xml"] = _common_module_pom(
    "common-web",
    """    <dependencies>
        <dependency>
            <groupId>com.enterprise</groupId>
            <artifactId>common-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>""",
)

_PLATFORM_FILES["common/common-mybatis/pom.xml"] = _common_module_pom(
    "common-mybatis",
    """    <dependencies>
        <dependency>
            <groupId>com.enterprise</groupId>
            <artifactId>common-core</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot4-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-jsqlparser</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jackson</artifactId>
        </dependency>
    </dependencies>""",
)

_PLATFORM_FILES["common/common-redis/pom.xml"] = _common_module_pom(
    "common-redis",
    """    <dependencies>
        <dependency>
            <groupId>com.enterprise</groupId>
            <artifactId>common-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
    </dependencies>""",
)

_PLATFORM_FILES["common/common-cloud/pom.xml"] = _common_module_pom(
    "common-cloud",
    """    <dependencies>
        <dependency>
            <groupId>com.enterprise</groupId>
            <artifactId>common-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
    </dependencies>""",
)

_PLATFORM_FILES["common/common-test/pom.xml"] = _common_module_pom(
    "common-test",
    """    <dependencies>
        <dependency>
            <groupId>com.enterprise</groupId>
            <artifactId>common-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>""",
)


def gen_platform(target: Path) -> None:
    print("[init] 生成平台 Monorepo 静态文件 ...")
    for rel, content in _PLATFORM_FILES.items():
        _write(target / rel, content)

    bp = "com.enterprise"
    # common-core 关键类
    _write(target / "common/common-core/src/main/java/com/enterprise/common/core/page/PageResult.java",
           _render("common/PageResult.java.j2", {"base_package": bp}))
    _write(target / "common/common-core/src/main/java/com/enterprise/common/core/exception/BusinessException.java",
           _render("common/BusinessException.java.j2", {"base_package": bp}))
    _write(target / "common/common-core/src/main/java/com/enterprise/common/core/id/SnowflakeIdGenerator.java",
           _render("common/SnowflakeIdGenerator.java.j2", {"base_package": bp}))

    # common-mybatis：JsonbTypeHandler
    _write(target / "common/common-mybatis/src/main/java/com/enterprise/common/mybatis/handler/JsonbTypeHandler.java",
           _render("common/JsonbTypeHandler.java.j2", {"base_package": bp}))

    # package-info 占位（避免聚合构建报依赖解析错误）
    for mod in ["common-redis", "common-cloud", "common-test"]:
        pkg = mod.replace("-", "")
        _write(target / f"common/{mod}/src/main/java/com/enterprise/common/{pkg}/package-info.java",
               f"package com.enterprise.common.{pkg};\n")

    # ===== starters =====
    _write(target / "starters/pom.xml",
           """\
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.enterprise</groupId>
        <artifactId>platform-parent</artifactId>
        <version>${revision}</version>
    </parent>
    <artifactId>starters</artifactId>
    <packaging>pom</packaging>
    <modules>
        <module>common-web-starter</module>
        <module>common-mybatis-starter</module>
        <module>common-cloud-starter</module>
    </modules>
</project>
""")
    _starter(target, "web", "com.enterprise.starter.web.CommonWebAutoConfiguration")
    _starter(target, "mybatis", "com.enterprise.starter.mybatis.CommonMyBatisAutoConfiguration")
    _starter(target, "cloud", "com.enterprise.starter.cloud.CommonCloudAutoConfiguration")

    # ===== gateway =====
    _write(target / "gateway/pom.xml",
           """\
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.enterprise</groupId>
        <artifactId>platform-parent</artifactId>
        <version>${revision}</version>
    </parent>
    <artifactId>gateway</artifactId>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>
    </dependencies>
</project>
""")

    # ===== deploy =====
    _write(target / "deploy/compose.yaml",
           """\
services:
  postgres:
    image: postgres:17
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
  redis:
    image: redis:7
    ports:
      - "6379:6379"
  nacos:
    image: nacos/nacos-server:v3.1.1
    ports:
      - "8848:8848"
      - "9848:9848"
""")
    print("[init] 平台骨架生成完成。")


def _starter(target: Path, name: str, auto_cfg_class: str) -> None:
    d = str(target / f"starters/common-{name}-starter")
    mod = f"common-{name}-starter"
    artifact = f"common-{name}"

    # 显式写 starter pom（relativePath 不同）
    _write(Path(d) / "pom.xml", f"""\
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.enterprise</groupId>
        <artifactId>platform-parent</artifactId>
        <version>${{revision}}</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>
    <artifactId>{mod}</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.enterprise</groupId>
            <artifactId>{artifact}</artifactId>
        </dependency>
    </dependencies>
</project>
""")
    auto_cfg = auto_cfg_class.rsplit(".", 1)
    pkg = ".".join(auto_cfg[:-1])
    name_cap = name.capitalize()
    _write(Path(d) / f"src/main/java/{pkg.replace('.', '/')}/{auto_cfg[-1]}.java",
           f"""\
package {pkg};

import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * {name_cap} 能力自动配置（本骨架仅声明占位，具体 Bean 由 common 模块提供）。
 */
@AutoConfiguration
public class {auto_cfg[-1]} {{
}}
""")
    _write(Path(d) / f"src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports",
           auto_cfg_class + "\n")


# ============================================================================
# 服务六模块骨架 + CRUD 切片
# ============================================================================
_SVC_MODULES = [
    "contract", "feign-client", "domain", "infrastructure", "application", "bootstrap",
]


def _service_dir(target: Path, svc: str) -> Path:
    return target / "services" / svc


def gen_service(target: Path, ctx: dict) -> None:
    svc = ctx["svc"]
    base = _service_dir(target, svc)
    print(f"[service] 生成六模块聚合骨架 {svc} ...")

    _write(base / "pom.xml", _render("pom/service-pom.xml.j2", ctx))
    for mod in _SVC_MODULES:
        _write(base / f"{svc}-{mod}/pom.xml", _render(f"pom/{mod}-pom.xml.j2", ctx))

    # 注册进 services 聚合 POM（幂等）
    svc_pom = target / "services" / "pom.xml"
    if svc_pom.exists():
        text = svc_pom.read_text(encoding="utf-8")
        if f"<module>{svc}</module>" not in text:
            text = text.replace("</modules>", f"        <module>{svc}</module>\n    </modules>")
            svc_pom.write_text(text, encoding="utf-8")
            print(f"  ~ {svc_pom} (注册模块 {svc})")
    print(f"[service] {svc} 六模块骨架生成完成。")


def gen_crud(target: Path, ctx: dict) -> None:
    svc = ctx["svc"]
    base = _service_dir(target, svc)
    print(f"[crud] 生成 {ctx['entity']} 聚合垂直切片 ...")

    tpl = _render

    # 1. SQL + bootstrap 启动/配置
    _write(base / f"{svc}-bootstrap/src/main/resources/db/migration/V1.0.0__create_{ctx['table']}_table.sql",
           tpl("sql/create_table.sql.j2", ctx))
    _write(base / f"{svc}-bootstrap/src/main/java/{ctx['base_package'].replace('.', '/')}/{ctx['prefix']}/{ctx['entity']}Application.java",
           tpl("bootstrap/Application.java.j2", ctx))
    _write(base / f"{svc}-bootstrap/src/main/resources/application.yml",
           tpl("bootstrap/application.yml.j2", ctx))
    _write(base / f"{svc}-bootstrap/src/test/java/{ctx['base_package'].replace('.', '/')}/{ctx['prefix']}/ModuleDependencyTest.java",
           tpl("bootstrap/ModuleDependencyTest.java.j2", ctx))

    # 2. contract
    c = f"{svc}-contract/src/main/java/{ctx['base_package'].replace('.', '/')}/{ctx['prefix']}/contract"
    _write(base / f"{c}/dto/request/Create{ctx['entity']}Request.java", tpl("contract/CreateRequest.java.j2", ctx))
    _write(base / f"{c}/dto/request/Update{ctx['entity']}Request.java", tpl("contract/UpdateRequest.java.j2", ctx))
    _write(base / f"{c}/dto/request/Query{ctx['entity']}Request.java", tpl("contract/QueryRequest.java.j2", ctx))
    _write(base / f"{c}/dto/response/{ctx['entity']}DetailResponse.java", tpl("contract/DetailResponse.java.j2", ctx))
    _write(base / f"{c}/dto/response/{ctx['entity']}SummaryResponse.java", tpl("contract/SummaryResponse.java.j2", ctx))
    _write(base / f"{c}/constant/{ctx['entity']}ServiceName.java", tpl("contract/ServiceName.java.j2", ctx))
    _write(base / f"{c}/constant/{ctx['entity']}ApiPath.java", tpl("contract/ApiPath.java.j2", ctx))
    _write(base / f"{c}/enums/{ctx['entity']}StatusEnum.java", tpl("contract/StatusEnum.java.j2", ctx))

    # 3. feign-client
    f = f"{svc}-feign-client/src/main/java/{ctx['base_package'].replace('.', '/')}/{ctx['prefix']}/feign"
    _write(base / f"{f}/{ctx['entity']}FeignClient.java", tpl("contract/FeignClient.java.j2", ctx))
    _write(base / f"{f}/{ctx['entity']}FeignFallbackFactory.java", tpl("contract/FeignFallbackFactory.java.j2", ctx))

    # 4. domain
    d = f"{svc}-domain/src/main/java/{ctx['base_package'].replace('.', '/')}/{ctx['prefix']}/domain"
    _write(base / f"{d}/model/{ctx['entity']}.java", tpl("domain/Entity.java.j2", ctx))
    _write(base / f"{d}/model/{ctx['entity']}Status.java", tpl("domain/Status.java.j2", ctx))
    _write(base / f"{d}/command/Create{ctx['entity']}Command.java", tpl("domain/CreateCommand.java.j2", ctx))
    _write(base / f"{d}/command/Update{ctx['entity']}Command.java", tpl("domain/UpdateCommand.java.j2", ctx))
    _write(base / f"{d}/query/{ctx['entity']}Query.java", tpl("domain/Query.java.j2", ctx))
    _write(base / f"{d}/event/{ctx['entity']}CreatedEvent.java", tpl("domain/CreatedEvent.java.j2", ctx))
    _write(base / f"{d}/repository/{ctx['entity']}Repository.java", tpl("domain/Repository.java.j2", ctx))
    _write(base / f"{d}/exception/{ctx['entity']}NotFoundException.java", tpl("domain/NotFoundException.java.j2", ctx))
    _write(base / f"{d}/exception/{ctx['entity']}StatusException.java", tpl("domain/StatusException.java.j2", ctx))

    # 5. infrastructure
    i = f"{svc}-infrastructure/src/main/java/{ctx['base_package'].replace('.', '/')}/{ctx['prefix']}/infrastructure"
    _write(base / f"{i}/persistence/entity/{ctx['entity']}PO.java", tpl("infrastructure/PO.java.j2", ctx))
    _write(base / f"{i}/persistence/mapper/{ctx['entity']}Mapper.java", tpl("infrastructure/Mapper.java.j2", ctx))
    _write(base / f"{i}/persistence/mapper/{ctx['entity']}ReadMapper.java", tpl("infrastructure/ReadMapper.java.j2", ctx))
    _write(base / f"{svc}-infrastructure/src/main/resources/mapper/{ctx['entity']}ReadMapper.xml",
           tpl("infrastructure/ReadMapper.xml.j2", ctx))
    _write(base / f"{i}/persistence/repository/{ctx['entity']}RepositoryImpl.java", tpl("infrastructure/RepositoryImpl.java.j2", ctx))
    _write(base / f"{i}/persistence/converter/{ctx['entity']}Converter.java", tpl("infrastructure/Converter.java.j2", ctx))
    _write(base / f"{i}/config/MyBatisPlusConfig.java", tpl("infrastructure/MyBatisPlusConfig.java.j2", ctx))
    _write(base / f"{i}/persistence/handler/MyMetaObjectHandler.java", tpl("infrastructure/MetaObjectHandler.java.j2", ctx))

    # 6. application
    a = f"{svc}-application/src/main/java/{ctx['base_package'].replace('.', '/')}/{ctx['prefix']}"
    _write(base / f"{a}/controller/{ctx['entity']}Controller.java", tpl("application/Controller.java.j2", ctx))
    _write(base / f"{a}/service/{ctx['entity']}ApplicationService.java", tpl("application/ApplicationService.java.j2", ctx))
    # Prefer concrete QueryService (P11 / start.spring.io); no Interface + service/impl/
    _write(base / f"{a}/service/{ctx['entity']}QueryService.java", tpl("application/QueryService.java.j2", ctx))
    _write(base / f"{a}/assembler/{ctx['entity']}Assembler.java", tpl("application/Assembler.java.j2", ctx))
    _write(base / f"{a}/advice/GlobalExceptionHandler.java", tpl("application/GlobalExceptionHandler.java.j2", ctx))

    print(f"[crud] {ctx['entity']} 聚合切片生成完成。")


# ============================================================================
# CLI
# ============================================================================
def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(prog="scaffold", description="spring-boot-gen 脚手架生成器")
    sub = parser.add_subparsers(dest="cmd", required=True)

    p_init = sub.add_parser("init", help="生成平台 Monorepo 骨架")
    p_init.add_argument("--base-package", default="com.enterprise")
    p_init.add_argument("--target", default=".", help="输出根目录")

    p_svc = sub.add_parser("service", help="生成业务微服务六模块骨架")
    _add_common(p_svc)
    p_svc.add_argument("--name", required=True)
    p_svc.add_argument("--prefix", required=True)
    p_svc.add_argument("--description", default="业务")

    p_crud = sub.add_parser("crud", help="生成聚合垂直全切片（骨架之上）")
    _add_common(p_crud)
    p_crud.add_argument("--service", required=True)
    p_crud.add_argument("--prefix", required=True)
    p_crud.add_argument("--entity", default="User")

    args = parser.parse_args(argv)

    if args.cmd == "init":
        gen_platform(Path(args.target))
        return 0

    bp = args.base_package
    if args.cmd == "service":
        ctx = _build_ctx(bp, args.name, args.prefix, "User")
        ctx["description"] = args.description
        gen_service(Path(args.target), ctx)
        return 0

    if args.cmd == "crud":
        ctx = _build_ctx(bp, args.service, args.prefix, args.entity)
        gen_crud(Path(args.target), ctx)
        return 0

    parser.print_help()
    return 1


def _add_common(p: argparse.ArgumentParser) -> None:
    p.add_argument("--base-package", required=True)
    p.add_argument("--target", default=".")


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))