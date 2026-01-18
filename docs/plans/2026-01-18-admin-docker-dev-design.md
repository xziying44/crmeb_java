# Admin 前端 Docker 开发环境设计

## 背景

当前本地全局 Node.js 版本过高，与 admin 前端（Vue 2.6 + Vue CLI 4.0）不兼容。需要将管理端前端添加到 Docker 中运行开发服务器，同时支持代码热更新。

## 需求

- 在 Docker 容器中运行 admin 前端开发服务器
- 支持代码修改后热更新（HMR）
- 后端服务运行在宿主机 20500 端口
- 前端映射到本机 9527 端口
- 使用 Node 14（与 Vue CLI 4.x 最佳兼容）

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                      宿主机 (macOS)                          │
│                                                             │
│   ┌─────────────────┐    ┌─────────────────────────────┐   │
│   │ 后端服务 (Java)  │    │        Docker 环境           │   │
│   │   :20500        │◄───│                             │   │
│   └─────────────────┘    │  ┌─────────────────────────┐   │
│                          │  │   admin-dev 容器     │   │   │
│   ┌─────────────────┐    │  │   Node 14 + Vue CLI │   │   │
│   │ 浏览器访问       │◄──────│   :9527             │   │   │
│   │ localhost:9527  │    │  └─────────────────────┘   │   │
│   └─────────────────┘    │            │               │   │
│                          │            ▼               │   │
│   ┌─────────────────┐    │  ┌─────────────────────┐   │   │
│   │ admin/ 源代码    │────────►  /app (挂载)        │   │   │
│   │ (本地文件系统)   │    │  └─────────────────────┘   │   │
│   └─────────────────┘    │            │               │   │
│                          │            ▼               │   │
│                          │  ┌─────────────────────┐   │   │
│                          │  │ node_modules (命名卷)│   │   │
│                          │  │ 持久化缓存依赖       │   │   │
│                          │  └─────────────────────┘   │   │
│                          └─────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## 实施方案

### 1. 新增文件

#### admin/Dockerfile.dev

```dockerfile
FROM node:14-alpine

WORKDIR /app

# 安装依赖用的临时目录
COPY package*.json ./

# 安装依赖（利用 Docker 缓存层）
RUN npm install --registry=https://registry.npmmirror.com

# 暴露开发服务器端口
EXPOSE 9527

# 启动开发服务器
CMD ["npm", "run", "dev"]
```

#### admin/.dockerignore

```
node_modules
dist
.git
*.log
```

### 2. 更新 docker-compose.yml

新增 admin 服务：

```yaml
admin:
  build:
    context: ./admin
    dockerfile: Dockerfile.dev
  container_name: crmeb-admin
  restart: unless-stopped
  ports:
    - "${ADMIN_PORT:-9527}:9527"
  environment:
    - NODE_ENV=development
    - CHOKIDAR_USEPOLLING=true
    - CHOKIDAR_INTERVAL=1000
    - VUE_APP_BASE_API=http://host.docker.internal:20500
  volumes:
    - ./admin:/app
    - admin_node_modules:/app/node_modules
  extra_hosts:
    - "host.docker.internal:host-gateway"

volumes:
  admin_node_modules:
```

### 3. 更新 .env

```bash
# Admin 前端配置
ADMIN_PORT=9527
```

## 使用方式

### 常用命令

```bash
# 首次启动（构建镜像 + 安装依赖，约 2-3 分钟）
docker-compose up -d admin

# 查看日志（实时）
docker-compose logs -f admin

# 停止服务
docker-compose stop admin

# 重启服务（秒级，依赖已缓存）
docker-compose restart admin

# 依赖变更后重新构建
docker-compose up -d --build admin

# 清理依赖缓存（强制重装 node_modules）
docker volume rm crmeb_java_admin_node_modules
docker-compose up -d --build admin
```

### 访问地址

| 服务 | 地址 |
|-----|------|
| 管理后台前端 | http://localhost:9527 |
| 后端 API（宿主机） | http://localhost:20500 |

## 关键设计决策

1. **Node 14**：与 Vue CLI 4.x 最佳兼容
2. **命名卷缓存 node_modules**：避免每次重装依赖，重启秒级
3. **CHOKIDAR_USEPOLLING**：解决 macOS Docker 文件监听问题
4. **host.docker.internal**：容器内访问宿主机服务的标准方式
5. **环境变量配置 API 地址**：集中管理，无需新增环境文件
