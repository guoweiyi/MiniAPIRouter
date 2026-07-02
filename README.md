# MiniAPIRouter — AI API 智能路由器

## 概述

MiniAPIRouter 是一个轻量级的 AI API 智能路由网关。它位于客户端与上游 AI 提供商（DeepSeek、OpenAI、Anthropic 等）之间，统一接收 OpenAI 和 Anthropic 协议的请求，根据**意图识别 + 多策略路由**智能地分发到最适合的模型，降低调用成本、提升响应质量。

```
客户端请求 ──▶  MiniAPIRouter  ──▶  上游模型（DeepSeek / GPT / Claude ...）
                    │
                    ├── 意图评估：分析请求意图与复杂度
                    ├── 路由决策：权重 / 优先级 / 轮询 / 最少连接 / 意图匹配
                    ├── 协议转换：OpenAI ⇄ Anthropic 自动互转
                    └── 故障转移：上游超时自动切换备用 Key
```

## 核心场景

| 场景 | 说明 |
|------|------|
| **多模型统一接入** | 对外暴露一个 OpenAI 兼容端点，后端可对接任意模型，客户端无需关心具体供应商 |
| **成本优化** | 简单问题路由到低成本模型，复杂推理路由到强模型，避免大材小用 |
| **高可用** | 配置多个 API Key，单 Key 限流/故障时自动切换到备用 Key，支持流式故障转移 |
| **灰度/迁移** | 通过权重或优先级路由，逐步将流量从旧模型迁移到新模型 |
| **多租户隔离**（SaaS） | JWT 认证 + 租户级路由规则，每个租户独立配置和配额管理 |

## 设计理念

- **插件式 SPI 架构**：核心库定义接口，宿主按需提供实现。Standalone 用 SQLite + Caffeine 零依赖运行，SaaS 用 MariaDB + Redis 支持生产
- **协议透明**：内部统一请求/响应模型，支持 OpenAI 和 Anthropic 协议互转，流式与非流式代理
- **虚拟线程**：全链路采用 Java 21 虚拟线程，大幅降低高并发下的资源消耗

## 项目结构

```
ai-router-parent/
├── ai-router-core/           # 核心库（非应用）
│   └── src/main/java/com/miniapi/router/core/
│       ├── spi/              # 12 个 SPI 接口（缓存、存储、限流、日志等）
│       ├── routing/          # 路由流水线 + 5 种策略
│       ├── protocol/         # OpenAI / Anthropic 协议转换器
│       ├── streaming/        # SSE 流式代理
│       ├── intent/           # 意图评估与路由
│       └── domain/           # 领域模型
├── ai-router-standalone/     # 单体宿主（推荐）
│   └── 零外部依赖：SQLite + Caffeine + 本地文件
└── ai-router-saas/           # SaaS 宿主（MariaDB + Redis + JWT）
```

## 快速开始（Standalone）

```bash
# 构建
mvn clean package -pl ai-router-standalone -am -DskipTests

# 运行
java -jar ai-router-standalone/target/ai-router-standalone-1.0.0.jar
```

首次启动会自动运行交互式配置向导，可指定 API Key、供应商、端口等。后续可通过 API 管理。

```bash
# 跳过向导直接启动
java -jar ai-router-standalone/target/ai-router-standalone-1.0.0.jar --skip-setup
```

默认认证 Token：`sk-miniapi-standalone`（可通过 `miniapi.router.auth-token` 配置）。

## 环境配置

项目通过 `.env` 文件管理敏感配置。Spring Boot 在启动时通过 `spring.config.import=optional:file:.env[.properties]` 自动加载。

```bash
# 复制模板后编辑
cp .env.template .env
```

> **IDEA 开发**：Run Configuration → `Working directory` 设为 `$PROJECT_DIR$`（项目根目录），Spring Boot 才能找到 `.env` 文件。

### 必需字段（依宿主而定）

#### Standalone（单机模式）

| 变量 | 说明 | 示例 |
|------|------|------|
| `STANDALONE_CRYPTO_SECRET` | API Key 加密密钥，需 **32 字节以上** | `my-standalone-secret-key-at-least-32-char!` |

Standalone 仅需上述一个变量即可运行。其他配置（如认证 Token `sk-miniapi-standalone`）均为内置默认值，可直接使用。

#### SaaS（多租户模式）

| 变量 | 说明 | 示例 |
|------|------|------|
| `SAAS_DB_URL` | MariaDB JDBC 连接串 | `jdbc:mariadb://127.0.0.1:3306/miniapi_router?...` |
| `SAAS_DB_USERNAME` | 数据库用户名 | `root` |
| `SAAS_DB_PASSWORD` | 数据库密码 | |
| `SAAS_REDIS_HOST` | Redis 主机地址 | `127.0.0.1` |
| `SAAS_REDIS_PASSWORD` | Redis 密码 | |
| `SAAS_CRYPTO_SECRET` | API Key 加密密钥，需 **32 字节以上** | |
| `SAAS_JWT_SECRET` | JWT 签名密钥，需 **32 字节以上** | |
| `SAAS_ADMIN_DEFAULT_PASSWORD` | 超级管理员初始密码（首次启动创建） | `admin123` |
| `SAAS_DEMO_ADMIN_DEFAULT_PASSWORD` | Demo 租户管理员初始密码（首次启动创建） | `demo123` |

> `SAAS_ADMIN_DEFAULT_PASSWORD` 和 `SAAS_DEMO_ADMIN_DEFAULT_PASSWORD` 仅在首次初始化（用户表为空）时生效，后续修改不影响已有用户。

## 核心特性

| 特性 | 说明 |
|------|------|
| **多协议代理** | 接收 OpenAI `/v1/chat/completions` 和 Anthropic `/v1/messages` 格式，自动转换 |
| **智能路由** | 权重随机、优先级故障转移、轮询、最少连接数、意图评分路由 |
| **意图路由** | 调用评估模型分析请求意图，按能力阈值匹配最合适的模型 |
| **流式代理** | SSE 全双工流式传输，keepalive 心跳，流式故障转移 |
| **故障转移** | 上游超时/失败时自动切换到下一个可用 Key |
| **限流** | 令牌桶算法，支持 Redis（SaaS）或 Caffeine（Standalone） |
| **日志审计** | 每次请求的完整日志记录与检索 |

## 路由策略

| 策略 | 说明 |
|------|------|
| `weight` | 加权随机选择 |
| `priority` | 按优先级排序，高优先级的 Key 优先 |
| `round_robin` | 轮询 |
| `least_conn` | 最少活跃连接数 |
| `intent_weight` | 意图评估得分 × 权重（每次请求即时计算） |

## API 端点

### 管理接口（`/api/v1`）

| 路径 | 说明 |
|------|------|
| `GET    /system/health` | 健康检查（公开） |
| `GET    /system/version` | 版本信息（公开） |
| `GET    /config/api-keys` | API Key 列表 |
| `POST   /config/api-keys` | 创建 API Key |
| `PUT    /config/api-keys/{id}` | 更新 API Key |
| `DELETE /config/api-keys/{id}` | 删除 API Key |
| `PATCH  /config/api-keys/{id}/status` | 启用/禁用 |
| `GET    /config/route-rules` | 路由规则列表 |
| `POST   /config/route-rules` | 创建路由规则 |
| `GET    /config/intents` | 意图配置 |
| `GET    /logs` | 日志查询 |
| `GET    /dashboard/summary` | 仪表盘摘要 |

### 代理接口（`/v1`）

| 路径 | 说明 |
|------|------|
| `POST /v1/chat/completions` | OpenAI 格式（流式 + 非流式） |
| `POST /v1/messages` | Anthropic 格式（流式 + 非流式） |

所有接口请求 Header 需携带 `Authorization: Bearer <auth-token>` 或 `x-api-key`。

## 构建全部模块

```bash
mvn clean package -DskipTests
```

> 本项目不包含测试套件（无 `src/test` 目录），`mvn test` 不会验证任何内容。

## SPI 机制

`ai-router-core` 定义了 12 个 SPI 接口，每个宿主模块提供自己的实现：

| SPI 接口 | Standalone 实现 | SaaS 实现 |
|----------|----------------|-----------|
| `CacheService` | CaffeineCacheService | RedisCacheService |
| `RateLimiter` | CaffeineRateLimiter | RedisRateLimiter |
| `LogRepository` | SqliteLogRepository | MybatisLogRepository |
| `ApiKeyConfigRepository` | SqliteApiKeyConfigRepository | MybatisApiKeyConfigRepository |
| `BlobStorage` | LocalFileBlobStorage | LocalFileBlobStorage |
| `HealthChecker` | PassiveHealthChecker | ScheduledHealthChecker |
| `EventPublisher` | LocalEventPublisher | AsyncEventPublisher |

## 技术栈

- Java 21 + 虚拟线程
- Spring Boot 3.3.5 + Spring WebFlux (WebClient)
- Jackson（snake_case 序列化）
- MyBatis-Plus 3.5.7
- Caffeine 3.1.8
- JJWT 0.12.6（SaaS）
- MariaDB JDBC（SaaS）/ SQLite JDBC（Standalone）
