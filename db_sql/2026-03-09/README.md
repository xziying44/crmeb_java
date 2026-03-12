# 2026-03-09 超市演示数据

本目录用于生成和存放“超市进货商城”演示数据。

## 范围

- 一级分类：13 个
- 二级分类：35 个
- 示例商品：37 个
- 图片来源：`/Users/xziying/Downloads/超市海报商品图片水果蔬菜图生鲜饮料海鲜零食便利店外卖素材大全`
- 图片上传：当前数据库配置的腾讯云 COS

## 文件说明

- `supermarket_seed_manifest.json`：分类、商品、图片来源清单
- `supermarket_seed_manifest.example.md`：清单字段说明
- `generate_supermarket_seed.py`：图片上传与 SQL 生成脚本
- `generated/01_supermarket_seed.sql`：脚本生成的导入 SQL
- `generated/02_supermarket_seed_verify.sql`：脚本生成的校验 SQL
- `generated/upload_manifest.json`：脚本生成的上传结果清单
- `supermarket_seed.sql`：最终交付 SQL
- `supermarket_seed_verify.sql`：最终交付校验 SQL

## 执行顺序

1. 运行 `python3 db_sql/2026-03-09/generate_supermarket_seed.py`
2. 执行 `generated/01_supermarket_seed.sql` 写入本地库验证
3. 执行 `generated/02_supermarket_seed_verify.sql` 核对结果
4. 验证通过后，将 `supermarket_seed.sql` 上传到真实环境执行

## 影响范围

- 会重建本次约定名称集内的产品分类
- 会重建本次约定名称集内的示例商品及关联规格、详情
- 不会修改无关分类和商品

## 真实环境前置条件

- 真实环境与本地使用同一腾讯云 COS 或可访问相同对象键
- 真实环境表结构与当前本地 Docker MySQL 一致
- 导入前确认不存在同名正式业务商品，避免被本次重建 SQL 清理

- 上传图片：45 张
