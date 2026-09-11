/**
 * Chinese names/descriptions for Dependencies panel.
 * Unknown ids fall back to English metadata from the server.
 */
const DEP = {
  // Developer Tools
  native: {
    name: 'GraalVM 原生镜像',
    description: '使用 GraalVM native-image 将 Spring 应用编译为原生可执行文件。',
  },
  'dgs-codegen': {
    name: 'GraphQL DGS 代码生成',
    description: '通过解析 Schema 文件，为 GraphQL API 生成数据类型与类型安全客户端。',
  },
  devtools: {
    name: 'Spring Boot DevTools',
    description: '提供快速重启、LiveReload 以及增强开发体验的配置。',
  },
  lombok: {
    name: 'Lombok',
    description: '通过注解减少 Java 样板代码（getter/setter/构造器等）。',
  },
  mapstruct: {
    name: 'MapStruct',
    description: '类型安全的 Bean 映射注解处理器（对象映射代码生成）。',
  },
  'configuration-processor': {
    name: '配置注解处理器',
    description: '为自定义配置键生成元数据，便于 IDE 提示与自动完成。',
  },
  'docker-compose': {
    name: 'Docker Compose 支持',
    description: '在开发阶段集成 Docker Compose，提升本地联调体验。',
  },
  modulith: {
    name: 'Spring Modulith',
    description: '构建模块化单体应用的 Spring 支持。',
  },
  // Web
  web: {
    name: 'Spring Web',
    description: '使用 Spring MVC 构建 Web / RESTful 应用，默认嵌入 Tomcat。',
  },
  webflux: {
    name: 'Spring Reactive Web',
    description: '使用 Spring WebFlux 与 Netty 构建响应式 Web 应用。',
  },
  'spring-restclient': {
    name: 'HTTP 客户端',
    description: 'Spring 同步 HTTP 客户端支持。',
  },
  'spring-webclient': {
    name: '响应式 HTTP 客户端',
    description: '基于 WebClient 的响应式 HTTP 客户端。',
  },
  graphql: {
    name: 'Spring for GraphQL',
    description: '使用 Spring for GraphQL 构建 GraphQL 应用。',
  },
  'data-rest': {
    name: 'REST 仓储',
    description: '将 Spring Data 仓储以超媒体驱动的 REST 资源暴露。',
  },
  hateoas: {
    name: 'Spring HATEOAS',
    description: '简化基于超媒体的 RESTful API 开发。',
  },
  'web-services': {
    name: 'Spring Web Services',
    description: '面向契约优先的 SOAP Web 服务开发。',
  },
  jersey: {
    name: 'Jersey',
    description: 'JAX-RS 参考实现，用于构建 RESTful Web 服务。',
  },
  vaadin: {
    name: 'Vaadin',
    description: '面向 Spring 的全栈 Web 应用平台。',
  },
  htmx: {
    name: 'htmx',
    description: '用超文本的简单方式构建现代用户界面。',
  },
  'springdoc-openapi': {
    name: 'SpringDoc OpenAPI',
    description: '为 Spring Web 应用添加 OpenAPI / Swagger 文档。',
  },
  knife4j: {
    name: 'Knife4j',
    description: '增强版 OpenAPI 3 UI（访问 /doc.html），国内常用 Swagger 增强。',
  },
  thymeleaf: {
    name: 'Thymeleaf',
    description: '现代服务端 Java 模板引擎。',
  },
  freemarker: {
    name: 'Apache FreeMarker',
    description: '基于模板的文本生成引擎。',
  },
  mustache: {
    name: 'Mustache',
    description: '无逻辑模板引擎。',
  },
  // Security
  security: {
    name: 'Spring Security',
    description: '高度可定制的认证与访问控制框架。',
  },
  'oauth2-client': {
    name: 'OAuth2 客户端',
    description: 'Spring Security OAuth2 / OIDC 客户端集成。',
  },
  'oauth2-authorization-server': {
    name: 'OAuth2 授权服务器',
    description: 'Spring Authorization Server 支持。',
  },
  'oauth2-resource-server': {
    name: 'OAuth2 资源服务器',
    description: 'Spring Security OAuth2 资源服务器支持。',
  },
  'sa-token': {
    name: 'Sa-Token',
    description: '国产轻量级权限认证框架（登录认证 / 权限校验 / SSO）。',
  },
  // SQL / Persistence
  jdbc: {
    name: 'JDBC API',
    description: '标准 JDBC 数据库访问支持。',
  },
  'data-jpa': {
    name: 'Spring Data JPA',
    description: '使用 Spring Data 与 Hibernate 持久化到 SQL 数据库。',
  },
  'data-jdbc': {
    name: 'Spring Data JDBC',
    description: '使用纯 JDBC 与 Spring Data 持久化到 SQL 数据库。',
  },
  mybatis: {
    name: 'MyBatis',
    description: '支持自定义 SQL、存储过程与高级映射的持久层框架。',
  },
  'mybatis-plus': {
    name: 'MyBatis-Plus',
    description: 'MyBatis 增强工具包（CRUD / 条件构造器 / 分页）。',
  },
  flyway: {
    name: 'Flyway 数据库迁移',
    description: '数据库版本控制与迁移工具。',
  },
  liquibase: {
    name: 'Liquibase 数据库迁移',
    description: '数据库变更管理与版本控制。',
  },
  h2: {
    name: 'H2 数据库',
    description: '快速内存数据库，支持 JDBC / R2DBC。',
  },
  mysql: {
    name: 'MySQL 驱动',
    description: 'MySQL JDBC 驱动。',
  },
  mariadb: {
    name: 'MariaDB 驱动',
    description: 'MariaDB JDBC / R2DBC 驱动。',
  },
  postgresql: {
    name: 'PostgreSQL 驱动',
    description: 'PostgreSQL JDBC / R2DBC 驱动。',
  },
  oracle: {
    name: 'Oracle 驱动',
    description: 'Oracle JDBC 驱动。',
  },
  sqlserver: {
    name: 'MS SQL Server 驱动',
    description: 'Microsoft SQL Server / Azure SQL JDBC 与 R2DBC 驱动。',
  },
  // China ecosystem
  hutool: {
    name: 'Hutool',
    description: '国产 Java 工具集（日期 / 加密 / HTTP / IO 等）。',
  },
  easyexcel: {
    name: 'EasyExcel',
    description: '阿里巴巴 Excel 读写库，适合大文件导入导出。',
  },
  // NoSQL / Cache / Messaging
  'data-redis': {
    name: 'Spring Data Redis',
    description: 'Redis 访问与驱动集成。',
  },
  'data-redis-reactive': {
    name: '响应式 Redis',
    description: '响应式 Spring Data Redis 支持。',
  },
  mongodb: {
    name: 'MongoDB',
    description: 'MongoDB 文档数据库驱动。',
  },
  'data-mongodb': {
    name: 'Spring Data MongoDB',
    description: '使用 Spring Data 访问 MongoDB。',
  },
  elasticsearch: {
    name: 'Elasticsearch',
    description: '分布式搜索与分析引擎。',
  },
  'data-elasticsearch': {
    name: 'Spring Data Elasticsearch',
    description: '使用 Spring Data 访问 Elasticsearch。',
  },
  amqp: {
    name: 'Spring for RabbitMQ',
    description: '基于 AMQP 的消息发送与接收。',
  },
  kafka: {
    name: 'Spring for Apache Kafka',
    description: '发布、订阅与处理 Kafka 记录流。',
  },
  // Ops
  actuator: {
    name: 'Spring Boot Actuator',
    description: '内置（或自定义）端点，用于监控与管理应用。',
  },
  prometheus: {
    name: 'Prometheus',
    description: '以 Prometheus 格式暴露 Micrometer 指标。',
  },
  zipkin: {
    name: 'Zipkin',
    description: '将链路追踪 span / trace 暴露给 Zipkin。',
  },
  testcontainers: {
    name: 'Testcontainers',
    description: '为集成测试提供一次性的数据库等容器实例。',
  },
  validation: {
    name: '校验',
    description: '基于 Hibernate Validator 的 Bean Validation。',
  },
  cache: {
    name: 'Spring 缓存抽象',
    description: '提供缓存相关操作与抽象。',
  },
  mail: {
    name: 'Java Mail Sender',
    description: '使用 JavaMailSender 发送邮件。',
  },
  quartz: {
    name: 'Quartz 调度器',
    description: '使用 Quartz 调度任务。',
  },
  batch: {
    name: 'Spring Batch',
    description: '批处理应用（事务、重试/跳过、分块处理）。',
  },
  websocket: {
    name: 'WebSocket',
    description: '基于 Servlet 的 WebSocket（SockJS / STOMP）。',
  },
  // Spring Cloud
  'cloud-config-client': {
    name: 'Config 客户端',
    description: '连接 Spring Cloud Config Server 拉取配置。',
  },
  'cloud-eureka': {
    name: 'Eureka 发现客户端',
    description: '服务发现与注册（Eureka）。',
  },
  'cloud-gateway': {
    name: 'Gateway',
    description: 'Spring Cloud Gateway API 路由网关。',
  },
  'session-data-redis': {
    name: 'Spring Session Redis',
    description: '基于 Redis 的用户会话管理。',
  },
}

const GROUP = {
  'Developer Tools': '开发者工具',
  Web: 'Web',
  'Template Engines': '模板引擎',
  Security: '安全',
  SQL: 'SQL',
  NoSQL: 'NoSQL',
  Messaging: '消息',
  'I/O': 'I/O',
  Ops: '运维监控',
  Observability: '可观测性',
  Testing: '测试',
  'Spring Cloud': 'Spring Cloud',
  'Spring Cloud Config': 'Spring Cloud Config',
  'Spring Cloud Discovery': 'Spring Cloud 服务发现',
  'Spring Cloud Routing': 'Spring Cloud 路由',
  'Spring Cloud Circuit Breaker': 'Spring Cloud 熔断',
  'Project Structure': '项目结构',
  'China Ecosystem': '国内生态',
  Ecosystem: '生态扩展',
}

export function translateDependency(id, field, fallback) {
  const entry = DEP[id]
  if (entry && entry[field]) {
    return entry[field]
  }
  return fallback || ''
}

export function translateDependencyGroup(name) {
  return GROUP[name] || name
}

export default DEP
