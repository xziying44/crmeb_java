# Docker Compose 本地开发环境设计

> 创建日期: 2026-01-18

## 概述

为 CRMEB Java 项目创建 Docker Compose 本地开发环境，容器化 MySQL 和 Redis 服务，Java 后端服务在 IDE 中调试运行。

## 需求确认

| 项目 | 选择 |
|-----|------|
| 用途 | 本地开发环境（MySQL + Redis 容器化） |
| 数据持久化 | Docker Volume 持久化 |
| 数据库初始化 | 自动导入 `Crmeb_v1.4.sql` |
| 端口 | MySQL: 3307, Redis: 6380 |
| 文档 | 基础运维命令 |

## 文件结构

```
crmeb_java/
├── docker-compose.yml      # Docker 编排文件
├── .env                    # 环境变量（密码等敏感信息）
├── .env.example            # 环境变量示例（提交到 git）
├── docker/                 # Docker 相关配置目录
│   └── mysql/
│       └── init/           # MySQL 初始化脚本目录
│           └── Crmeb_v1.4.sql
└── .gitignore              # 添加 .env 忽略规则
```

## 环境变量配置

### .env 文件

```env
# MySQL 配置
MYSQL_ROOT_PASSWORD=root123456
MYSQL_DATABASE=single_open
MYSQL_USER=single_open
MYSQL_PASSWORD=111111
MYSQL_PORT=3307

# Redis 配置
REDIS_PASSWORD=111111
REDIS_PORT=6380
```

### .env.example 文件

```env
# MySQL 配置
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_DATABASE=single_open
MYSQL_USER=single_open
MYSQL_PASSWORD=your_password
MYSQL_PORT=3307

# Redis 配置
REDIS_PASSWORD=your_redis_password
REDIS_PORT=6380
```

## docker-compose.yml

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:5.7
    container_name: crmeb-mysql
    restart: unless-stopped
    ports:
      - "${MYSQL_PORT}:3306"
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      TZ: Asia/Shanghai
    volumes:
      - mysql_data:/var/lib/mysql
      - ./docker/mysql/init:/docker-entrypoint-initdb.d:ro
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_general_ci

  redis:
    image: redis:6-alpine
    container_name: crmeb-redis
    restart: unless-stopped
    ports:
      - "${REDIS_PORT}:6379"
    environment:
      TZ: Asia/Shanghai
    volumes:
      - redis_data:/data
    command: redis-server --requirepass ${REDIS_PASSWORD} --appendonly yes

volumes:
  mysql_data:
  redis_data:
```

## CLAUDE.md 新增内容

在常用命令部分后新增 Docker 运维章节：

```markdown
### Docker 开发环境

> 首次使用需复制环境变量文件：`cp .env.example .env`

# 启动服务（后台运行）
docker-compose up -d

# 停止服务
docker-compose stop

# 重启服务
docker-compose restart

# 重启单个服务
docker-compose restart mysql
docker-compose restart redis

# 查看服务状态
docker-compose ps

# 查看日志（实时）
docker-compose logs -f

# 查看单个服务日志
docker-compose logs -f mysql
docker-compose logs -f redis

# 完全停止并删除容器（数据保留在 Volume 中）
docker-compose down

# 连接 MySQL 客户端
docker-compose exec mysql mysql -u single_open -p single_open

# 连接 Redis 客户端
docker-compose exec redis redis-cli -a 111111

**端口映射：**
| 服务 | 容器端口 | 本机端口 |
|-----|---------|---------|
| MySQL | 3306 | 3307 |
| Redis | 6379 | 6380 |
```

## 环境依赖

| 依赖 | 版本/路径 |
|-----|----------|
| JDK | 1.8 (`/Users/xziying/Library/Java/JavaVirtualMachines/azul-1.8.0_472/Contents/Home`) |
| Docker | 已安装 |
| Maven | 已安装 |

## 实施步骤

### 阶段一：Docker 环境搭建

1. 创建 `docker/mysql/init/` 目录
2. 复制 SQL 初始化脚本到 init 目录
3. 创建 `.env` 文件
4. 创建 `.env.example` 文件
5. 创建 `docker-compose.yml` 文件
6. 更新 `.gitignore` 添加 `.env`
7. 更新 `CLAUDE.md` 添加 Docker 命令文档
8. 更新 `application-dev.yml` 修改 MySQL 和 Redis 端口

### 阶段二：启动 Docker 服务

9. 启动 Docker 容器 (`docker-compose up -d`)
10. 等待 MySQL 初始化完成（检查日志）
11. 验证 MySQL 连接正常
12. 验证 Redis 连接正常

### 阶段三：后端服务测试

13. 设置 JAVA_HOME 环境变量
14. 编译整个项目 (`mvn clean install -DskipTests`)
15. 启动 crmeb-admin 服务 (端口 20500)
16. 验证 admin 服务健康状态（访问 Swagger 文档）
17. 启动 crmeb-front 服务 (端口 20510)
18. 验证 front 服务健康状态

### 验证检查点

| 检查项 | 预期结果 |
|-------|---------|
| `docker-compose ps` | mysql 和 redis 状态为 Up |
| `curl localhost:3307` | MySQL 端口可访问 |
| `redis-cli -p 6380 -a 111111 ping` | 返回 PONG |
| `curl localhost:20500/doc.html` | 返回 Swagger 页面 |
| `curl localhost:20510/doc.html` | 返回 Swagger 页面 |

## 注意事项

- 首次启动 MySQL 容器会自动执行 init 目录下的 SQL 文件
- 数据存储在 Docker Volume 中，`docker-compose down` 不会丢失数据
- 如需完全重置数据，使用 `docker-compose down -v` 删除 Volume
