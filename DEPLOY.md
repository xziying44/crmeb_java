# CRMEB Java 宝塔面板快速部署指南

> 分支：`custom`（基于官方 1.4 二次开发）。最后更新对应提交 `033dffc5`。

## 〇、本次更新摘要（务必先读）

本次在基线 `7e180300` 之后新增/修复 13 个提交，部署时相对旧指南有 **3 处关键变化**：

1. **数据库口令改为环境变量注入（H1 安全修复）**
   JAR 内置的 `application-prod.yml` 已不再含明文口令，改用占位符：
   `${DB_PASSWORD}`（**必填，无默认值，不设则启动失败**）、`${REDIS_PASSWORD:}`（默认空）、
   以及 `${DB_HOST:127.0.0.1}` `${DB_PORT:3306}` `${DB_NAME:crmeb}` `${DB_USERNAME:crmeb}` `${REDIS_HOST:127.0.0.1}` `${REDIS_PORT:6379}`。
   **推荐通过环境变量注入这些值，不要把明文口令写进磁盘上的 yml**（见 5.3 / 5.6 的更新写法）。

2. **SQL 脚本清单已变更（旧指南引用了已不存在的文件）**
   仓库现有 SQL：`Crmeb_v1.4.sql`、`activity_banner.sql`、`salesman_schema.sql`（均在 `crmeb/sql/`）、
   `promotion_tables.sql`（在 `crmeb/crmeb-common/src/main/resources/sql/`）。
   旧指南里的 `inventory_tables.sql` / `inventory_menu.sql` / `update_menu_names.sql` **已不存在**。

3. **⚠️ 销存/库存模块缺 DDL（部署阻断项）**
   代码引用 `eb_stock` `eb_stock_log` `eb_purchase` `eb_purchase_item` `eb_stock_check` `eb_stock_check_item` `eb_supplier`，
   但仓库**未提交任何对应的建表脚本**。若要启用"销存/库存"功能，需先自行补齐这些表的 DDL，否则该模块运行报错。
   其余功能（商城/订单/营销/买赠/满减/活动横幅/业务员）不受影响。

**本次修复均为代码层改动，不新增数据库表**；买赠/满减相关表由 `promotion_tables.sql` 提供（M3/M5/M6 修复依赖）。

## 一、部署产物清单

打包后需要上传到服务器的文件：

| 文件 | 路径 | 说明 |
|-----|------|------|
| `Crmeb-admin.jar` | `crmeb/crmeb-admin/target/` | 管理端 API 服务 (120MB) |
| `Crmeb-front.jar` | `crmeb/crmeb-front/target/` | 移动端 API 服务 (116MB) |
| `admin/dist/` | `admin/dist/` | 管理后台前端静态文件 (11MB) |
| `Crmeb_v1.4.sql` | `crmeb/sql/` | 基础库（必须最先导入） |
| `promotion_tables.sql` | `crmeb/crmeb-common/src/main/resources/sql/` | 促销/买赠/满减表（本次修复依赖） |
| `activity_banner.sql` | `crmeb/sql/` | 活动横幅表与菜单 |
| `salesman_schema.sql` | `crmeb/sql/` | 业务员表与菜单 |

> ⚠️ 旧指南里的 `inventory_tables.sql` / `inventory_menu.sql` / `update_menu_names.sql` 已不在仓库中，请勿再引用。
> 销存/库存模块（`eb_stock` 等）目前**无建表脚本**，如需启用须自行补 DDL（见〇节第 3 点）。

## 二、服务器环境要求

- 操作系统：CentOS 7+ / Ubuntu 18+
- 内存：建议 2GB+（两个 Java 服务各占约 512MB）
- 宝塔面板版本：7.x+

## 三、宝塔面板安装软件

在宝塔「软件商店」中安装以下软件：

1. **Nginx** (任意版本)
2. **MySQL 8.0**
3. **Redis** (任意版本)
4. **Java 项目管理器** (宝塔插件，用于管理 Spring Boot 应用)

> 如果宝塔没有 Java 项目管理器插件，也可以手动安装 JDK 1.8 并用 systemd 管理服务（见第八节）。

## 四、数据库配置

### 4.1 创建数据库

在宝塔「数据库」页面：

1. 点击「添加数据库」
2. 数据库名：`crmeb`（可自定义）
3. 用户名：`crmeb`（可自定义）
4. 密码：设置一个强密码
5. 访问权限：本地服务器
6. 字符集：`utf8mb4`

### 4.2 导入 SQL

按顺序导入以下 SQL 文件（在宝塔数据库管理界面的「导入」功能）：

```
1. Crmeb_v1.4.sql        # 基础表结构和数据（必须第一个导入）
2. promotion_tables.sql  # 促销/买赠/满减表（M3/M5/M6 修复依赖）
3. activity_banner.sql   # 活动横幅表与菜单
4. salesman_schema.sql   # 业务员表与菜单
```

或通过命令行导入（注意 promotion_tables.sql 在 crmeb-common 资源目录下）：

```bash
# 进入 MySQL
mysql -u crmeb -p crmeb

# 按顺序执行
source /www/server/crmeb/sql/Crmeb_v1.4.sql;
source /www/server/crmeb/sql/promotion_tables.sql;
source /www/server/crmeb/sql/activity_banner.sql;
source /www/server/crmeb/sql/salesman_schema.sql;
```

> 上传时请把 `crmeb/crmeb-common/src/main/resources/sql/promotion_tables.sql` 一并放到服务器的
> `/www/server/crmeb/sql/` 目录，便于统一执行。
>
> ⚠️ **销存/库存功能**：代码依赖 `eb_stock` `eb_stock_log` `eb_purchase` `eb_purchase_item`
> `eb_stock_check` `eb_stock_check_item` `eb_supplier` 等表，但仓库未提供建表脚本。
> 不补这些表时请勿在后台开启销存相关菜单/接口，否则会因缺表报错；其余功能不受影响。

### 4.3 配置 Redis

在宝塔「软件商店」> Redis > 设置：

1. 设置 Redis 密码（记住这个密码，后面配置要用）
2. 默认端口 6379 即可

## 五、后端部署

### 5.1 创建目录结构

```bash
mkdir -p /www/server/crmeb/{admin,front,sql,logs}
```

### 5.2 上传文件

将以下文件上传到服务器（通过宝塔文件管理或 scp）：

```bash
# JAR 包
/www/server/crmeb/admin/Crmeb-admin.jar
/www/server/crmeb/front/Crmeb-front.jar

# SQL 文件
/www/server/crmeb/sql/Crmeb_v1.4.sql
/www/server/crmeb/sql/promotion_tables.sql
/www/server/crmeb/sql/activity_banner.sql
/www/server/crmeb/sql/salesman_schema.sql
```

### 5.3 生产配置：用环境变量注入口令（H1 安全要求）

JAR 内置的 `application-prod.yml` 已激活 `prod` profile、内置正确端口（admin 20400 / front 20410），
并把数据库/Redis 连接信息全部改为**环境变量占位符**，**不再含任何明文口令**：

| 环境变量 | 是否必填 | 默认值 | 说明 |
|---------|---------|-------|------|
| `DB_PASSWORD` | **必填** | 无（不设则启动失败） | 数据库口令 |
| `DB_HOST` | 否 | `127.0.0.1` | 数据库主机 |
| `DB_PORT` | 否 | `3306` | 数据库端口 |
| `DB_NAME` | 否 | `crmeb` | 数据库名 |
| `DB_USERNAME` | 否 | `crmeb` | 数据库用户 |
| `REDIS_HOST` | 否 | `127.0.0.1` | Redis 主机 |
| `REDIS_PORT` | 否 | `6379` | Redis 端口 |
| `REDIS_PASSWORD` | 否 | 空 | Redis 口令（未设密码留空即可） |

**推荐做法：把口令放进一份仅 root 可读的 EnvironmentFile，不要写进 yml。**

```bash
# 创建 /www/server/crmeb/crmeb.env（systemd 与启动脚本共用）
cat > /www/server/crmeb/crmeb.env <<'EOF'
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=crmeb
DB_USERNAME=crmeb
DB_PASSWORD=替换为真实数据库口令
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=替换为真实Redis口令
EOF
chmod 600 /www/server/crmeb/crmeb.env   # 仅 root 可读，避免泄露
```

> ⚠️ 安全提醒：内置 yml 默认 `swagger.basic.enable: true`、Swagger 账号 `crmeb/crmeb.com`，
> 生产环境务必关闭或改密（见下方可选覆盖）。

#### （可选）外部覆盖非敏感项

只有当你需要改图片路径、关闭 Swagger 等**非敏感**设置时，才创建外部覆盖文件；
**绝不要在其中写明文口令**——口令始终走环境变量。

`/www/server/crmeb/admin/application-prod.yml`（front 同理，端口换 20410、去掉 captchaOn）：

```yaml
crmeb:
  imagePath: /www/server/crmeb/upload/  # 图片存储路径，斜杠结尾
  demoSite: false
  captchaOn: true                       # 建议开启行为验证码

logging:
  file:
    path: /www/server/crmeb/logs

swagger:
  basic:
    enable: false                       # 生产环境关闭 Swagger UI
```

### 5.4 创建图片上传目录

```bash
mkdir -p /www/server/crmeb/upload
chmod 755 /www/server/crmeb/upload
```

### 5.5 方式一：使用宝塔 Java 项目管理器

1. 打开宝塔「软件商店」> 「Java 项目管理器」
2. 安装 JDK 1.8（如果没有）
3. 添加项目：
   - 项目名称：`crmeb-admin`
   - 项目路径：`/www/server/crmeb/admin/Crmeb-admin.jar`
   - JDK 版本：1.8
   - **环境变量**：在「环境变量」栏逐行填入 `DB_PASSWORD`、`REDIS_PASSWORD` 等（见 5.3 表格）；这是口令的来源，**必须配置 `DB_PASSWORD`**
   - 启动参数（可选）：仅当用了 5.3 的外部覆盖文件时才加 `--spring.config.additional-location=/www/server/crmeb/admin/application-prod.yml`
   - JVM 参数：`-Xms256m -Xmx512m`
4. 同样添加 `crmeb-front` 项目（同样在「环境变量」栏填入 `DB_PASSWORD` 等）：
   - 项目名称：`crmeb-front`
   - 项目路径：`/www/server/crmeb/front/Crmeb-front.jar`
   - 启动参数（可选）：`--spring.config.additional-location=/www/server/crmeb/front/application-prod.yml`
   - JVM 参数：`-Xms256m -Xmx512m`

> 若 Java 项目管理器版本不支持「环境变量」栏，请改用 5.6 的 systemd 方式，用 `EnvironmentFile` 注入口令。

### 5.6 方式二：使用 systemd 手动管理

如果没有 Java 项目管理器，手动创建 systemd 服务。

确保已安装 JDK 1.8：

```bash
# CentOS
yum install java-1.8.0-openjdk java-1.8.0-openjdk-devel -y

# Ubuntu
apt install openjdk-8-jdk -y
```

创建管理端服务 `/etc/systemd/system/crmeb-admin.service`：

```ini
[Unit]
Description=CRMEB Admin API Service
After=network.target mysql.service redis.service

[Service]
Type=simple
User=root
WorkingDirectory=/www/server/crmeb/admin
# 口令通过 EnvironmentFile 注入（见 5.3），JAR 内置 prod 配置会读取这些环境变量
EnvironmentFile=/www/server/crmeb/crmeb.env
# 若用了 5.3 的外部覆盖文件，再追加 --spring.config.additional-location=application-prod.yml
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar Crmeb-admin.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

创建移动端服务 `/etc/systemd/system/crmeb-front.service`：

```ini
[Unit]
Description=CRMEB Front API Service
After=network.target mysql.service redis.service

[Service]
Type=simple
User=root
WorkingDirectory=/www/server/crmeb/front
EnvironmentFile=/www/server/crmeb/crmeb.env
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar Crmeb-front.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启动服务：

```bash
systemctl daemon-reload
systemctl enable crmeb-admin crmeb-front
systemctl start crmeb-admin
systemctl start crmeb-front

# 查看状态
systemctl status crmeb-admin
systemctl status crmeb-front

# 查看日志
journalctl -u crmeb-admin -f
journalctl -u crmeb-front -f
```

## 六、前端部署

### 6.1 修改 API 地址

部署前需要修改 `admin/.env.production` 中的 API 地址：

```
VUE_APP_BASE_API = 'https://你的域名'
```

然后重新构建：

```bash
cd admin && npm run build:prod
```

### 6.2 上传前端文件

将 `admin/dist/` 目录下的所有文件上传到服务器：

```bash
/www/wwwroot/crmeb-admin/
```

### 6.3 宝塔创建网站

在宝塔「网站」页面：

1. 点击「添加站点」
2. 域名：填写你的管理后台域名（如 `admin.yourdomain.com`）
3. 根目录：`/www/wwwroot/crmeb-admin`
4. PHP 版本：纯静态

## 七、Nginx 反向代理配置

在宝塔「网站」> 选择站点 > 「配置文件」，替换为以下配置：

```nginx
server {
    listen 80;
    server_name admin.yourdomain.com;  # 替换为你的域名

    # 管理后台前端
    root /www/wwwroot/crmeb-admin;
    index index.html;

    # 前端路由 history 模式
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 管理端 API 代理
    location /api/admin/ {
        proxy_pass http://127.0.0.1:20400/api/admin/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 600;
        proxy_read_timeout 600;
        client_max_body_size 50m;
    }

    # 公共接口代理（验证码、文件上传等）
    location /api/public/ {
        proxy_pass http://127.0.0.1:20400/api/public/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 50m;
    }

    # 移动端 API 代理
    location /api/front/ {
        proxy_pass http://127.0.0.1:20410/api/front/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 600;
        proxy_read_timeout 600;
        client_max_body_size 50m;
    }

    # 图片资源
    location /crmeb/ {
        alias /www/server/crmeb/upload/;
    }

    # 禁止访问隐藏文件
    location ~ /\. {
        deny all;
    }
}
```

> 建议在宝塔中为该站点申请 SSL 证书，开启 HTTPS。

## 八、部署验证

### 8.1 检查服务状态

```bash
# 检查端口是否监听
ss -tlnp | grep -E '20400|20410'

# 测试管理端 API
curl http://127.0.0.1:20400/api/public/getVersion

# 测试移动端 API
curl http://127.0.0.1:20410/api/front/index
```

### 8.2 检查日志

```bash
# 查看管理端日志
tail -100f /www/server/crmeb/logs/crmeb_log/crmeb-admin.log

# 查看移动端日志
tail -100f /www/server/crmeb/logs/crmeb_log/crmeb-front.log
```

### 8.3 访问测试

- 管理后台：`http://admin.yourdomain.com`
- 默认管理员账号：`admin` / `123456`（首次登录后请立即修改密码）

## 九、常见问题

### Q: JAR 启动报错 "Could not resolve placeholder 'DB_PASSWORD'"
未注入必填环境变量 `DB_PASSWORD`。检查 systemd 的 `EnvironmentFile=/www/server/crmeb/crmeb.env` 是否存在、
是否包含 `DB_PASSWORD=...`；用宝塔 Java 管理器时检查「环境变量」栏是否填了 `DB_PASSWORD`。
可用 `systemctl show crmeb-admin -p Environment` 确认变量是否真正注入。

### Q: JAR 启动报错 "Cannot determine embedded database driver class"
数据库连接配置有误。检查 `DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD` 环境变量是否正确、数据库是否可达。

### Q: Redis 连接失败
检查 Redis 是否启动、密码是否正确、端口是否为 6379。

### Q: 前端页面空白或 API 404
1. 检查 `.env.production` 中的 `VUE_APP_BASE_API` 是否指向正确域名
2. 检查 Nginx 反向代理配置是否正确
3. 确认后端服务已启动

### Q: 图片上传失败
1. 检查 `/www/server/crmeb/upload/` 目录权限
2. 检查 `application-prod.yml` 中 `crmeb.imagePath` 路径是否正确
3. 检查 Nginx `client_max_body_size` 是否足够大

### Q: 内存不足导致服务崩溃
调整 JVM 参数，减小堆内存：`-Xms128m -Xmx256m`，或升级服务器配置。

## 十、服务器目录结构总览

```
/www/server/crmeb/
├── crmeb.env                     # 口令环境变量（chmod 600，禁止入库）
├── admin/
│   ├── Crmeb-admin.jar           # 管理端 JAR
│   └── application-prod.yml      # 可选：仅覆盖非敏感项（imagePath/swagger 等）
├── front/
│   ├── Crmeb-front.jar           # 移动端 JAR
│   └── application-prod.yml      # 可选：同上
├── sql/                          # SQL 脚本备份
├── upload/                       # 图片上传目录
└── logs/                         # 日志目录

/www/wwwroot/crmeb-admin/         # 管理后台前端静态文件
├── index.html
└── static/
```

## 十一、升级部署（覆盖已有环境）

本次为在已上线环境上替换 bug 修复版 JAR，**不新增数据库表**（M1/M2/M3/M5/M6 均为代码层修复）。
按以下步骤平滑升级：

```bash
# 1. 备份（务必先备份，便于回滚）
cp /www/server/crmeb/admin/Crmeb-admin.jar /www/server/crmeb/admin/Crmeb-admin.jar.bak
cp /www/server/crmeb/front/Crmeb-front.jar /www/server/crmeb/front/Crmeb-front.jar.bak
mysqldump -u crmeb -p crmeb > /www/server/crmeb/sql/backup_$(date +%F).sql   # 升级前备份数据库

# 2. 停服
systemctl stop crmeb-admin crmeb-front   # 或在宝塔 Java 管理器中停止

# 3. 上传新 JAR，覆盖旧文件
#    Crmeb-admin.jar -> /www/server/crmeb/admin/
#    Crmeb-front.jar -> /www/server/crmeb/front/

# 4. 首次升级到本版本时，补充缺失的表（已导入过可跳过；脚本可重复执行需自行确认幂等）
#    - promotion_tables.sql：若旧库还没有买赠/满减表（eb_buy_gift_record / eb_full_reduction* 等），必须导入
#    - activity_banner.sql：若使用活动横幅功能
mysql -u crmeb -p crmeb -e "SHOW TABLES LIKE 'eb_buy_gift_record';"   # 为空则需导入 promotion_tables.sql

# 5. 确认口令环境变量仍在（H1 升级后必须）
test -f /www/server/crmeb/crmeb.env && grep -q DB_PASSWORD /www/server/crmeb/crmeb.env && echo OK

# 6. 启服并验证
systemctl start crmeb-admin crmeb-front
ss -tlnp | grep -E '20400|20410'
curl http://127.0.0.1:20400/api/public/getVersion

# 回滚：用 .bak 覆盖回旧 JAR 并重启即可
```

> **从旧版本（口令写在 yml）升级到本版本的注意事项**：本版本起口令改由环境变量注入。
> 升级时请按 5.3 建好 `crmeb.env` 并在 systemd/Java 管理器里挂上，否则会因 `DB_PASSWORD` 缺失启动失败。
>
> **安全收尾（需人工完成）**：旧版本曾把口令明文写进配置并提交，明文仍残留在 git 历史中。
> 建议轮换数据库/Redis 口令，并用 `git filter-repo` / BFG 清理历史后强推。
