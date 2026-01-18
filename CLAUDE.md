# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

CRMEB 开源商城系统 Java 版 - 基于 Apache-2.0 协议的多端电商解决方案（H5、微信小程序、公众号、APP）。

## 技术栈

- **后端**: SpringBoot 2.2.6 + MyBatis-Plus 3.3.1 + Spring Security + JDK 1.8
- **前端**: Vue 2.x + Element UI 2.15 (管理端) / uni-app (移动端)
- **数据**: MySQL 5.7 + Redis

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    客户端层                                  │
│   admin (Vue+EUI)    app (uni-app)    H5/小程序              │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    API 服务层                                │
│   crmeb-admin (:8080)        crmeb-front (:8081)            │
│   管理端 API + 定时任务       移动端 API                      │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    业务服务层                                │
│                    crmeb-service                            │
│   商品/订单/用户/营销/支付/微信 业务逻辑                      │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    公共基础层                                │
│                    crmeb-common                             │
│   实体模型 / 常量 / 工具类 / 异常处理 / Redis配置            │
└─────────────────────────────────────────────────────────────┘
```

**关键依赖关系**: `crmeb-admin/crmeb-front` → `crmeb-service` → `crmeb-common`

## 常用命令

### 后端构建与运行

```bash
# 编译全部后端模块
cd crmeb && mvn clean install -DskipTests

# 启动管理端服务 (端口 8080)
cd crmeb/crmeb-admin && mvn spring-boot:run

# 启动移动端服务 (端口 8081)
cd crmeb/crmeb-front && mvn spring-boot:run

# 运行单个测试类
mvn test -Dtest=ClassName

# 运行单个测试方法
mvn test -Dtest=ClassName#methodName
```

### 管理后台前端

```bash
cd admin
npm install
npm run dev          # 开发模式
npm run build:prod   # 生产构建
npm run lint         # ESLint 检查
npm run test:unit    # 单元测试
```

### 移动端前端

```bash
cd app
npm install
# 使用 HBuilderX 打开运行 (H5/微信小程序/APP)
```

## 包结构约定

- **Controller**: `com.zbkj.{admin|front}.controller.*`
- **Service 接口**: `com.zbkj.service.service.*`
- **Service 实现**: `com.zbkj.service.service.impl.*`
- **DAO**: `com.zbkj.service.dao.*`
- **实体模型**: `com.zbkj.common.model.{业务模块}.*`
- **请求/响应对象**: `com.zbkj.common.request.*` / `com.zbkj.common.response.*`
- **VO 对象**: `com.zbkj.common.vo.*`

## 核心配置文件

| 文件 | 用途 |
|-----|------|
| `crmeb/crmeb-admin/src/main/resources/application-dev.yml` | 管理端开发环境配置 |
| `crmeb/crmeb-front/src/main/resources/application-dev.yml` | 移动端开发环境配置 |
| `admin/.env.development` | 前端开发环境变量 |

## 开发模式

1. **新增 API**: Controller 在 `crmeb-admin/controller` 或 `crmeb-front/controller`，业务逻辑在 `crmeb-service`
2. **新增实体**: 放在 `crmeb-common/model/{业务模块}/`，使用 `@Data` + `@TableName`
3. **新增前端页面**: 页面在 `admin/src/views/`，API 封装在 `admin/src/api/`
4. **动态配置**: 优先使用 `SystemConfig` 表，避免硬编码

## 代码风格

- Java: 使用 Lombok (`@Data`, `@Slf4j`)，Swagger 注解 (`@Api`, `@ApiOperation`)
- Vue: ESLint + Prettier，单引号，无分号，2 空格缩进

## API 文档

启动后访问 `http://localhost:8080/doc.html` (Swagger)
