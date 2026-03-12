# 超市演示数据清单说明

`supermarket_seed_manifest.json` 用于驱动图片上传和 SQL 生成，字段约定如下：

- `seed_tag`：本次导数批次标识。
- `material_root`：素材根目录。
- `categories`：一级分类数组。
- `categories[].name`：一级分类名称。
- `categories[].sort`：一级分类排序，数值越大越靠前。
- `categories[].image`：分类图来源。
- `categories[].children`：二级分类数组。
- `categories[].products`：该一级分类下的示例商品。
- `products[].child_name`：商品归属的二级分类名称。
- `products[].store_info`：商品简介。
- `products[].keyword`：商品关键词，逗号分隔。
- `products[].unit_name`：销售单位。
- `products[].price/cost/ot_price`：销售价、成本价、划线价。
- `products[].stock`：库存。
- `products[].image`：商品主图来源。

图片来源支持两种写法：

1. 关键字匹配

```json
{
  "source_dir": "CS-016超市饮料白底图700张",
  "match": "农夫山泉"
}
```

说明：脚本会在 `source_dir` 目录下递归查找文件，并优先选择 `jpg/jpeg/png/webp` 中名称或路径包含关键字的图片。

2. 显式路径

```json
{
  "source_dir": "CS-015 面包图片素材160张",
  "relative_path": "1 (100).jpg"
}
```

说明：当素材文件名无法通过关键字稳定定位时，使用相对路径直接指定。

维护要求：

- 产品分类名称尽量控制在 8 个字以内，方便后台继续维护。
- 商品总数控制在 30-50 个。
- 图片优先使用白底或浅背景成品图，保持超市演示风格统一。
- 如果修改了分类或商品名称，需要同步重新生成 SQL，不能只改最终 SQL 文件。
