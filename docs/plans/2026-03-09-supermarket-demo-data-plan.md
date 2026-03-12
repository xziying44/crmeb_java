# 超市演示数据实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 基于本地超市素材为 CRMEB 演示库补齐常用超市分类、30-50 个示例商品和对应腾讯云 COS 图片，并产出可直接导入真实演示环境的 SQL。

**Architecture:** 先梳理现有分类、商品、附件和 COS 上传模式，再用一份可维护的数据清单驱动脚本生成。脚本负责扫描素材、选择图片、上传 COS、生成附件和商品 SQL，并在本地 Docker MySQL 上验证可重放性，最后将最终 SQL 和说明输出到 `db_sql/2026-03-09/`。

**Tech Stack:** Shell / Python 3 / MySQL 8 / 腾讯云 COS / CRMEB Java 数据表结构

**设计文档:** `docs/plans/2026-03-09-supermarket-demo-data-design.md`

---

### Task 1: 盘点数据库结构与现有演示数据

**Files:**
- Modify: `docs/plans/2026-03-09-supermarket-demo-data-design.md`
- Create: `db_sql/2026-03-09/README.md`

**Step 1: 查询现有分类、商品、附件和上传配置**

Run:

```bash
docker exec crmeb-mysql mysql --default-character-set=utf8mb4 -usingle_open -p111111 single_open -e "
SELECT name, value FROM eb_system_config WHERE name IN ('uploadType','txUploadUrl','txStorageName','txStorageRegion');
SELECT id,pid,path,name,sort,status FROM eb_category WHERE type=1 ORDER BY pid,sort DESC,id DESC;
SELECT id,store_name,cate_id,price,stock,is_show FROM eb_store_product WHERE is_del=0 ORDER BY id DESC;
SELECT att_id,name,satt_dir,image_type,create_time FROM eb_system_attachment ORDER BY att_id DESC LIMIT 20;
"
```

Expected: 能看到当前超市相关分类、商品与 COS 上传配置。

**Step 2: 编写 README 记录导入范围和执行顺序**

在 `db_sql/2026-03-09/README.md` 中写明：

- 本次新增的一级/二级分类范围
- 预计商品数量
- SQL 文件执行顺序
- 对正式环境的影响范围

**Step 3: 手工校验设计是否需要补充**

若查询到的结构与设计不一致，先更新设计文档，再继续后续任务。

**Step 4: 建议提交信息**

```text
docs: 补充超市演示数据设计和导入说明
```

---

### Task 2: 建立分类与商品数据清单

**Files:**
- Create: `db_sql/2026-03-09/supermarket_seed_manifest.json`
- Create: `db_sql/2026-03-09/supermarket_seed_manifest.example.md`

**Step 1: 从素材目录整理候选图片**

Run:

```bash
find '/Users/xziying/Downloads/超市海报商品图片水果蔬菜图生鲜饮料海鲜零食便利店外卖素材大全' -type f | sed -n '1,400p'
```

Expected: 输出足够多的素材文件路径，覆盖水果、蔬菜、饮料、零食、海鲜、个护等品类。

**Step 2: 编写清单文件**

在 `supermarket_seed_manifest.json` 中定义：

- 一级分类名
- 二级分类名数组
- 分类图本地路径
- 商品名
- 商品简介
- 价格、成本、划线价、库存
- 规格配置
- 主图和轮播图本地路径

要求：

- 分类做齐全
- 商品总数控制在 30-50
- 图片风格统一
- 替换现有 `饮品酒水/酒类` 分类图片

**Step 3: 写一份示例说明**

在 `supermarket_seed_manifest.example.md` 中说明清单字段含义和维护方式，避免后续扩展时误改格式。

**Step 4: 最小校验**

Run:

```bash
python3 - <<'PY'
import json
from pathlib import Path
path = Path('db_sql/2026-03-09/supermarket_seed_manifest.json')
data = json.loads(path.read_text())
print('一级分类数:', len(data['categories']))
print('商品数:', sum(len(c['products']) for c in data['categories']))
PY
```

Expected: 一级分类数符合预期，商品数在 30-50 之间。

**Step 5: 建议提交信息**

```text
feat: 添加超市演示数据清单
```

---

### Task 3: 编写图片上传与 SQL 生成脚本

**Files:**
- Create: `db_sql/2026-03-09/generate_supermarket_seed.py`
- Create: `db_sql/2026-03-09/generated/`

**Step 1: 先写失败校验脚本**

先写最小校验逻辑，确保以下情况会失败退出：

- 清单文件不存在
- 本地图片不存在
- 数据库未配置腾讯云 COS
- 商品数超出 30-50

Run:

```bash
python3 db_sql/2026-03-09/generate_supermarket_seed.py --check-only
```

Expected: 若脚本尚未实现，先失败；实现后在输入正确时返回校验通过。

**Step 2: 实现最小生成逻辑**

脚本需要完成：

- 读取清单
- 读取 MySQL 当前配置
- 为每张图片生成 COS 对象键
- 上传图片到 COS
- 生成附件 SQL
- 生成分类 SQL
- 生成商品与关联表 SQL

输出文件至少包括：

- `db_sql/2026-03-09/generated/01_supermarket_seed.sql`
- `db_sql/2026-03-09/generated/02_supermarket_seed_verify.sql`
- `db_sql/2026-03-09/generated/upload_manifest.json`

**Step 3: 运行脚本生成结果**

Run:

```bash
python3 db_sql/2026-03-09/generate_supermarket_seed.py
```

Expected: 成功上传图片并生成 SQL 与上传清单。

**Step 4: 建议提交信息**

```text
feat: 添加超市演示数据生成脚本
```

---

### Task 4: 在本地数据库执行 SQL 并验证

**Files:**
- Modify: `db_sql/2026-03-09/README.md`
- Modify: `db_sql/2026-03-09/generated/02_supermarket_seed_verify.sql`

**Step 1: 执行导入 SQL**

Run:

```bash
docker exec -i crmeb-mysql mysql --default-character-set=utf8mb4 -usingle_open -p111111 single_open < db_sql/2026-03-09/generated/01_supermarket_seed.sql
```

Expected: SQL 执行成功，无语法错误。

**Step 2: 运行验证 SQL**

Run:

```bash
docker exec -i crmeb-mysql mysql --default-character-set=utf8mb4 -usingle_open -p111111 single_open < db_sql/2026-03-09/generated/02_supermarket_seed_verify.sql
```

Expected: 能看到分类数量、商品数量、附件数量以及抽样商品图片字段。

**Step 3: 验证可重放**

再次执行一次导入 SQL：

```bash
docker exec -i crmeb-mysql mysql --default-character-set=utf8mb4 -usingle_open -p111111 single_open < db_sql/2026-03-09/generated/01_supermarket_seed.sql
```

Expected: 仍然成功，且商品总数不重复增长。

**Step 4: 更新 README 的最终执行说明**

补充：

- 正式环境执行前置条件
- 需要保证 COS 对象已上传
- 推荐执行命令
- 回滚建议

**Step 5: 建议提交信息**

```text
feat: 生成并验证超市演示数据 SQL
```

---

### Task 5: 整理最终交付文件

**Files:**
- Modify: `db_sql/2026-03-09/README.md`
- Create: `db_sql/2026-03-09/supermarket_seed.sql`
- Create: `db_sql/2026-03-09/supermarket_seed_verify.sql`

**Step 1: 固化最终 SQL**

将 `generated/` 下经验证的 SQL 复制为最终交付版本：

- `db_sql/2026-03-09/supermarket_seed.sql`
- `db_sql/2026-03-09/supermarket_seed_verify.sql`

**Step 2: 补充最终清单**

README 中增加：

- 一级/二级分类总数
- 商品总数
- 上传图片总数
- 抽样商品名列表

**Step 3: 最终验证**

Run:

```bash
python3 db_sql/2026-03-09/generate_supermarket_seed.py --check-only
docker exec -i crmeb-mysql mysql --default-character-set=utf8mb4 -usingle_open -p111111 single_open < db_sql/2026-03-09/supermarket_seed_verify.sql
```

Expected: 清单校验通过，验证 SQL 输出与 README 记录一致。

**Step 4: 建议提交信息**

```text
feat: 输出超市演示数据最终 SQL
```
