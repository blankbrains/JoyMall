# 🛒 JoyMall — 全链路微服务电商平台

> Spring Cloud Alibaba 全栈微服务电商项目 · 涵盖商品、订单、购物车、搜索、秒杀、支付等核心电商业务

[![Java](https://img.shields.io/badge/Java-8%2B-orange)](https://java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.2.5-brightgreen)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-Hoxton.SR3-blue)](https://spring.io/projects/spring-cloud)
[![Spring Cloud Alibaba](https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2.2.1-red)](https://spring.io/projects/spring-cloud-alibaba)
[![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.3.2-yellow)](https://baomidou.com/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

---

## 📋 项目概述

JoyMall 是一个基于 **Spring Cloud Alibaba** 微服务架构的电商平台，实现了电商核心业务的全链路闭环。项目采用分布式架构设计，集成了服务注册与发现、配置中心、流量治理、分布式事务等主流微服务技术栈。

### 📌 核心业务

| 业务 | 说明 |
|------|------|
| 🏠 **商品服务** | SPU/SKU 管理、分类与品牌、属性管理、商品上架 |
| 📦 **库存服务** | 库存锁定与释放、采购单、分布式库存 |
| 🛒 **购物车** | Redis 购物车、登录合并、临时购物车 |
| 📄 **订单服务** | 订单创建与关闭、分布式锁幂等、MQ 延迟队列关闭超时订单 |
| 💳 **支付** | 支付宝沙箱对接、异步回调处理、签名验证 |
| 🔍 **搜索** | ElasticSearch 全文检索、聚合分析、高亮显示 |
| ⚡ **秒杀** | Redisson 信号量限流、Redis 预热、MQ 异步削峰、SetNX 幂等 |
| 👤 **会员** | 注册/登录、社交登录（Gitee OAuth2）、积分体系 |
| 🎫 **优惠券** | 满减/折扣/会员价、优惠券领取与使用 |
| 🔐 **认证** | 短信验证码、分布式 Session、OAuth2 社交登录 |
| 📡 **网关** | Spring Cloud Gateway 路由、CORS、Sentinel 流控 |

---

## 🏗️ 技术栈

### 📦 框架与中间件

| 类别 | 技术 | 版本 |
|------|------|------|
| 基础框架 | Spring Boot | 2.2.5.RELEASE |
| 微服务 | Spring Cloud | Hoxton.SR3 |
| 微服务 | Spring Cloud Alibaba | 2.2.1.RELEASE |
| 注册/配置中心 | Nacos (Spring Cloud Alibaba 内置) | 1.2.1+ |
| 服务网关 | Spring Cloud Gateway | — |
| 流量治理 | Sentinel | — |
| ORM | MyBatis-Plus | 3.3.2 |
| 数据库 | MySQL | 8.0.17 |
| 缓存 | Redis | — |
| 消息队列 | RabbitMQ | — |
| 搜索引擎 | ElasticSearch | 7.4.2 |
| 分布式锁 | Redisson | — |

### 🌐 第三方服务

| 服务 | 用途 |
|------|------|
| 阿里云 OSS | 图片/文件存储 |
| 阿里云 SMS | 短信验证码 |
| 支付宝沙箱 | 支付对接 |
| Gitee OAuth2 | 社交登录 |

---

## 🧩 模块架构

```
joymall/
├── joymall-common          # 公共模块 (Utils、VO、异常、常量)
├── joymall-gateway         # API 网关 (路由、鉴权、CORS)
├── joymall-auth-server     # 认证中心 (登录、注册、OAuth2、验证码)
│
├── joymall-product         # 商品服务 (SPU/SKU/分类/品牌/属性)
├── joymall-coupon          # 优惠券服务 (满减/折扣/会员价)
├── joymall-member          # 会员服务 (用户/地址/等级/积分)
├── joymall-order           # 订单服务 (订单/支付/物流)
├── joymall-ware            # 库存服务 (库存锁定/采购/解锁)
│
├── joymall-cart            # 购物车服务 (Redis 购物车/合并)
├── joymall-search          # 搜索服务 (ES 全文检索/聚合)
├── joymall-seckill         # 秒杀服务 (信号量/MQ 异步)
├── joymall-third-party     # 第三方集成 (OSS/SMS)
│
├── renren-fast             # 后台管理 (基于 renren-fast)
└── renren-generator        # 代码生成器
```

### 🔌 服务端口

| 服务 | 端口 |
|------|------|
| Gateway 网关 | `88` |
| Auth 认证中心 | `20000` |
| Product 商品 | `10000` |
| Order 订单 | `9010` |
| Member 会员 | `8010` |
| Coupon 优惠券 | `7001` |
| Warehouse 库存 | `11000` |
| Cart 购物车 | `40000` |
| Search 搜索 | `12001` |
| Seckill 秒杀 | `25000` |
| Third-Party | `30000` |
| renren-fast 后台 | `8080` |

---

## 🚀 快速启动

### 📋 前置环境

| 组件 | 说明 |
|------|------|
| JDK | 8+ (已在 JDK 17 下测试通过) |
| Maven | 3.6+ |
| MySQL | 8.0+ |
| Redis | 5.x+ |
| RabbitMQ | 3.8+ |
| ElasticSearch | 7.x |
| Nacos | 1.2.1+ |

### ⚙️ 启动步骤

```bash
# 1. 启动基础设施
# 启动 Nacos、MySQL、Redis、RabbitMQ、ElasticSearch

# 2. 创建数据库
# 分别在 MySQL 中创建：
#   joymall_pms (商品)
#   joymall_oms (订单)
#   joymall_ums (会员)
#   joymall_sms (营销)
#   joymall_wms (库存)
#   joymall_admin (后台)

# 3. 配置 Nacos
# 在 Nacos 中配置各服务的 datasource.yaml、oss.yaml 等共享配置

# 4. 修改配置
# 将所有 application.yaml / bootstrap.yaml 中的
#   your-nacos-host → 实际的 Nacos 地址
#   your-mysql-host → 实际的 MySQL 地址
#   your-redis-host → 实际的 Redis 地址
#   your-oss-*     → 实际的阿里云 OSS 配置
# 等占位符替换为实际值

# 5. 编译
mvn clean compile

# 6. 按依赖顺序启动
# Nacos → renren-fast → joymall-common → 业务服务 → Gateway
```

### 🔑 关键配置

所有凭证/密钥通过 `@ConfigurationProperties` 和 `@Value` 从配置文件中注入，**禁止硬编码**。参考各模块的 `application.yaml` 中的 `your-*` 占位符进行替换。

> 详细部署文档请参见各模块下的 `application.yaml` 和 `bootstrap.yaml`。



## 📊 架构要点

### 🔀 流量链路

```
客户端 → Gateway (路由/Sentinel 流控) → 业务服务 (各服务独立鉴权)
              │                            │
              ├─ /api/** ──→ 业务微服务 ───┼→ 数据库
              ├─ /auth/** ─→ Auth 认证中心 ──→ Redis (Session)
              └─ /search/** → Search 搜索服务 → Elasticsearch
                                              ↓
                                         RabbitMQ (异步解耦/最终一致性)
```

### ⚡ 关键技术方案

- **分布式 Session**: Spring Session + Redis，支持子域共享
- **分布式锁**: Redisson + Lua 脚本，确保锁原子性释放
- **订单超时关闭**: RabbitMQ 延迟队列 + 死信队列处理
- **库存锁定**: 数据库行锁 + MQ 延迟队列自动解锁补偿
- **秒杀防超卖**: Redisson 信号量令牌桶 + Redis SetNX 幂等
- **搜索**: ElasticSearch 商品上架，Nested 聚合 + Highlight + 排序

---

## 🔧 开发工具

- IDE: IntelliJ IDEA / VS Code
- API 测试: Postman / Swagger (`renren-fast/swagger/index.html`)
- 接口文档: Springfox Swagger 2.9.2
- 代码生成: renren-generator



## 👤 作者

- GitHub: [@blankbrains](https://github.com/blankbrains)

> **注意**: 本项目中的阿里云 AccessKey/SecretKey、短信 AppCode、支付宝密钥等凭证已全部清除为占位符。使用前请替换为实际值。
