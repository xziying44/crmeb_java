# CRMEB Java 宝塔面板快速部署指南

## 一、部署产物清单

打包后需要上传到服务器的文件：

| 文件 | 路径 | 说明 |
|-----|------|------|
| `Crmeb-admin.jar` | `crmeb/crmeb-admin/target/` | 管理端 API 服务 (120MB) |
| `Crmeb-front.jar` | `crmeb/crmeb-front/target/` | 移动端 API 服务 (115MB) |
| `admin/dist/` | `admin/dist/` | 管理后台前端静态文件 (11MB) |
| `Crmeb_v1.4.sql` | `crmeb/sql/` | 基础数据库脚本 |
| `inventory_tables.sql` | `crmeb/sql/` | 库存表结构 |
| `inventory_menu.sql` | `crmeb/sql/` | 库存菜单数据 |
| `salesman_schema.sql` | `crmeb/sql/` | 业务员表结构 |
| `update_menu_names.sql` | `crmeb/sql/` | 菜单名称更新 |
| `promotion_tables.sql` | `crmeb/crmeb-common/src/main/resources/sql/` | 促销活动表结构 |

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
1. Crmeb_v1.4.sql          # 基础表结构和数据（必须第一个导入）
2. inventory_tables.sql     # 库存扩展表
3. inventory_menu.sql       # 库存菜单
4. salesman_schema.sql      # 业务员表
5. update_menu_names.sql    # 菜单名称更新
6. promotion_tables.sql     # 促销活动表
```

或通过命令行导入：

```bash
# 进入 MySQL
mysql -u crmeb -p crmeb

# 按顺序执行
source /www/server/crmeb/sql/Crmeb_v1.4.sql;
source /www/server/crmeb/sql/inventory_tables.sql;
source /www/server/crmeb/sql/inventory_menu.sql;
source /www/server/crmeb/sql/salesman_schema.sql;
source /www/server/crmeb/sql/update_menu_names.sql;
source /www/server/crmeb/sql/promotion_tables.sql;
```

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
/www/server/crmeb/sql/inventory_tables.sql
/www/server/crmeb/sql/inventory_menu.sql
/www/server/crmeb/sql/salesman_schema.sql
/www/server/crmeb/sql/update_menu_names.sql
/www/server/crmeb/sql/promotion_tables.sql
```

### 5.3 修改生产配置

JAR 包内已包含 `application-prod.yml`，部署时通过外部配置文件覆盖。

创建管理端配置 `/www/server/crmeb/admin/application-prod.yml`：

```yaml
crmeb:
  imagePath: /www/server/crmeb/upload/  # 图片存储路径，斜杠结尾
  demoSite: false  # 生产环境关闭演示模式
  captchaOn: true  # 建议开启验证码

server:
  port: 20400

spring:
  profiles:
    active: prod
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/crmeb?characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: crmeb
    password: 你的数据库密码
  redis:
    host: 127.0.0.1
    port: 6379
    password: 你的Redis密码
    database: 0

debug: false
logging:
  level:
    com.zbjk.crmeb: info
  file:
    path: /www/server/crmeb/logs

swagger:
  basic:
    enable: false  # 生产环境建议关闭 Swagger
```

创建移动端配置 `/www/server/crmeb/front/application-prod.yml`：

```yaml
crmeb:
  imagePath: /www/server/crmeb/upload/  # 与管理端保持一致

server:
  port: 20410

spring:
  profiles:
    active: prod
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/crmeb?characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: crmeb
    password: 你的数据库密码
  redis:
    host: 127.0.0.1
    port: 6379
    password: 你的Redis密码
    database: 0

debug: false
logging:
  level:
    com.zbjk.crmeb: info
  file:
    path: /www/server/crmeb/logs

swagger:
  basic:
    enable: false
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
   - 启动参数：`--spring.config.additional-location=/www/server/crmeb/admin/application-prod.yml`
   - JVM 参数：`-Xms256m -Xmx512m`
4. 同样添加 `crmeb-front` 项目：
   - 项目名称：`crmeb-front`
   - 项目路径：`/www/server/crmeb/front/Crmeb-front.jar`
   - 启动参数：`--spring.config.additional-location=/www/server/crmeb/front/application-prod.yml`
   - JVM 参数：`-Xms256m -Xmx512m`

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
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar Crmeb-admin.jar --spring.config.additional-location=application-prod.yml
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
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar Crmeb-front.jar --spring.config.additional-location=application-prod.yml
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

### Q: JAR 启动报错 "Cannot determine embedded database driver class"
数据库连接配置有误，检查 `application-prod.yml` 中的数据库地址、用户名、密码。

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
├── admin/
│   ├── Crmeb-admin.jar           # 管理端 JAR
│   └── application-prod.yml      # 管理端生产配置
├── front/
│   ├── Crmeb-front.jar           # 移动端 JAR
│   └── application-prod.yml      # 移动端生产配置
├── sql/                          # SQL 脚本备份
├── upload/                       # 图片上传目录
└── logs/                         # 日志目录

/www/wwwroot/crmeb-admin/         # 管理后台前端静态文件
├── index.html
└── static/
```
