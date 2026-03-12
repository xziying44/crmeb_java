#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import argparse
import hashlib
import json
import mimetypes
import os
import re
import shutil
from dataclasses import dataclass
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path
from typing import Dict, List, Tuple

import pymysql
from qcloud_cos import CosConfig, CosS3Client
from qcloud_cos.cos_exception import CosServiceError


BASE_DIR = Path(__file__).resolve().parent
MANIFEST_PATH = BASE_DIR / "supermarket_seed_manifest.json"
GENERATED_DIR = BASE_DIR / "generated"
FINAL_SQL_PATH = BASE_DIR / "supermarket_seed.sql"
FINAL_VERIFY_SQL_PATH = BASE_DIR / "supermarket_seed_verify.sql"
DOTENV_PATH = BASE_DIR.parent.parent / ".env"
ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp"}
SEED_DATE = "2026-03-09"
SEED_DATETIME = "2026-03-09 12:00:00"
IMAGE_PREFIX = "crmebimage/public/product/2026/03/09"
LEGACY_CATEGORY_NAMES = ["酒类", "22222"]
LEGACY_PRODUCT_NAMES = ["娃哈哈饮用纯净水350ml"]


@dataclass
class DbConfig:
    host: str
    port: int
    user: str
    password: str
    database: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="生成超市演示数据 SQL 并上传图片")
    parser.add_argument("--check-only", action="store_true", help="只做清单和配置校验，不上传也不生成 SQL")
    return parser.parse_args()


def load_manifest() -> dict:
    if not MANIFEST_PATH.exists():
        raise FileNotFoundError(f"未找到清单文件: {MANIFEST_PATH}")
    return json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))


def load_env() -> Dict[str, str]:
    env: Dict[str, str] = {}
    if DOTENV_PATH.exists():
        for line in DOTENV_PATH.read_text(encoding="utf-8").splitlines():
            stripped = line.strip()
            if not stripped or stripped.startswith("#") or "=" not in stripped:
                continue
            key, value = stripped.split("=", 1)
            env[key.strip()] = value.strip()
    return env


def get_db_config() -> DbConfig:
    env = load_env()
    return DbConfig(
        host="127.0.0.1",
        port=int(env.get("MYSQL_PORT", "3307")),
        user=env.get("MYSQL_USER", "single_open"),
        password=env.get("MYSQL_PASSWORD", "111111"),
        database=env.get("MYSQL_DATABASE", "single_open"),
    )


def get_connection(db_config: DbConfig):
    return pymysql.connect(
        host=db_config.host,
        port=db_config.port,
        user=db_config.user,
        password=db_config.password,
        database=db_config.database,
        charset="utf8mb4",
        autocommit=False,
        cursorclass=pymysql.cursors.DictCursor,
    )


def fetch_cos_settings(connection) -> Dict[str, str]:
    keys = [
        "uploadType",
        "txUploadUrl",
        "txAccessKey",
        "txSecretKey",
        "txStorageName",
        "txStorageRegion",
    ]
    sql = "SELECT name, value FROM eb_system_config WHERE name IN ({})".format(",".join(["%s"] * len(keys)))
    with connection.cursor() as cursor:
        cursor.execute(sql, keys)
        rows = cursor.fetchall()
    settings = {row["name"]: row["value"] for row in rows}
    missing = [key for key in keys if key not in settings]
    if missing:
        raise RuntimeError(f"数据库缺少 COS 配置项: {', '.join(missing)}")
    if settings["uploadType"] != "4":
        raise RuntimeError(f"当前 uploadType={settings['uploadType']}，不是腾讯云 COS(4)")
    return settings


def normalize_text(value: str) -> str:
    return re.sub(r"[^0-9A-Za-z\u4e00-\u9fff]+", "", value or "").lower()


def ensure_decimal(value: str) -> str:
    return str(Decimal(str(value)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP))


def sql_quote(value: str) -> str:
    return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'"


def file_att_type(path: Path) -> str:
    suffix = path.suffix.lower().lstrip(".")
    if suffix == "jpg":
        return "jpeg"
    return suffix or "jpeg"


def file_size_label(path: Path) -> str:
    size = path.stat().st_size / 1024
    return f"{size:.2f}KB"


def build_file_index(material_root: Path) -> Dict[str, List[Path]]:
    result: Dict[str, List[Path]] = {}
    for file_path in material_root.rglob("*"):
        if not file_path.is_file():
            continue
        if file_path.suffix.lower() not in ALLOWED_EXTENSIONS:
            continue
        relative_root = file_path.relative_to(material_root)
        top_dir = relative_root.parts[0]
        result.setdefault(top_dir, []).append(file_path)
    for key in result:
        result[key].sort(key=lambda item: str(item))
    return result


def resolve_image(material_root: Path, file_index: Dict[str, List[Path]], source: dict) -> Path:
    source_dir = source["source_dir"]
    if source_dir not in file_index:
        raise FileNotFoundError(f"素材目录不存在或没有可用图片: {source_dir}")
    if "relative_path" in source:
        candidate = material_root / source_dir / source["relative_path"]
        if not candidate.exists():
            raise FileNotFoundError(f"找不到素材图片: {candidate}")
        return candidate

    match_value = normalize_text(source.get("match", ""))
    if not match_value:
        raise ValueError(f"图片来源缺少 match 或 relative_path: {source}")

    candidates: List[Tuple[int, str, Path]] = []
    for file_path in file_index[source_dir]:
        relative_path = str(file_path.relative_to(material_root / source_dir))
        normalized_relative = normalize_text(relative_path)
        if match_value not in normalized_relative:
            continue
        score = 0
        if "长方图" in relative_path or "美团" in relative_path:
            score -= 2
        if "/800x800" in relative_path or "白底" in relative_path:
            score -= 1
        candidates.append((score, relative_path, file_path))

    if not candidates:
        raise FileNotFoundError(f"未找到匹配素材: dir={source_dir}, match={source.get('match')}")
    candidates.sort(key=lambda item: (item[0], item[1]))
    return candidates[0][2]


def build_object_key(local_path: Path) -> str:
    digest = hashlib.sha1(local_path.read_bytes()).hexdigest()[:32]
    return f"{IMAGE_PREFIX}/{digest}{local_path.suffix.lower()}"


def build_cos_client(settings: Dict[str, str]) -> CosS3Client:
    config = CosConfig(
        Region=settings["txStorageRegion"],
        SecretId=settings["txAccessKey"],
        SecretKey=settings["txSecretKey"],
        Token=None,
        Scheme="https",
    )
    return CosS3Client(config)


def upload_if_needed(client: CosS3Client, bucket: str, object_key: str, local_path: Path) -> None:
    try:
        client.head_object(Bucket=bucket, Key=object_key)
        return
    except CosServiceError as error:
        if error.get_status_code() != 404:
            raise
    content_type = mimetypes.guess_type(str(local_path))[0] or "application/octet-stream"
    with local_path.open("rb") as file_obj:
        client.put_object(
            Bucket=bucket,
            Key=object_key,
            Body=file_obj,
            ContentType=content_type,
        )


def validate_manifest(manifest: dict, material_root: Path, file_index: Dict[str, List[Path]]) -> Tuple[List[dict], List[dict]]:
    categories = manifest.get("categories", [])
    if not categories:
        raise ValueError("清单中没有任何分类")
    if not (30 <= sum(len(category["products"]) for category in categories) <= 50):
        raise ValueError("商品数量必须控制在 30-50 个之间")

    resolved_categories: List[dict] = []
    resolved_products: List[dict] = []

    for category_index, category in enumerate(categories, start=1):
        root_image = resolve_image(material_root, file_index, category["image"])
        root_var = f"@cat_r{category_index:02d}"
        resolved_children: List[dict] = []
        child_vars: Dict[str, str] = {}

        for child_index, child in enumerate(category["children"], start=1):
            child_image = resolve_image(material_root, file_index, child["image"])
            child_var = f"@cat_r{category_index:02d}_c{child_index:02d}"
            resolved_children.append(
                {
                    "name": child["name"],
                    "sort": child["sort"],
                    "object_key": build_object_key(child_image),
                    "local_path": str(child_image),
                    "var_name": child_var,
                }
            )
            child_vars[child["name"]] = child_var

        resolved_categories.append(
            {
                "name": category["name"],
                "sort": category["sort"],
                "object_key": build_object_key(root_image),
                "local_path": str(root_image),
                "var_name": root_var,
                "children": resolved_children,
            }
        )

        for product in category["products"]:
            child_name = product["child_name"]
            if child_name not in child_vars:
                raise ValueError(f"商品 {product['store_name']} 引用了不存在的二级分类: {child_name}")
            product_image = resolve_image(material_root, file_index, product["image"])
            resolved_products.append(
                {
                    "store_name": product["store_name"],
                    "child_name": child_name,
                    "child_var_name": child_vars[child_name],
                    "store_info": product["store_info"],
                    "keyword": product["keyword"],
                    "unit_name": product["unit_name"],
                    "price": ensure_decimal(product["price"]),
                    "cost": ensure_decimal(product["cost"]),
                    "ot_price": ensure_decimal(product["ot_price"]),
                    "stock": int(product["stock"]),
                    "object_key": build_object_key(product_image),
                    "local_path": str(product_image),
                }
            )
    return resolved_categories, resolved_products


def collect_upload_items(resolved_categories: List[dict], resolved_products: List[dict]) -> Dict[str, dict]:
    uploads: Dict[str, dict] = {}
    for category in resolved_categories:
        uploads[category["object_key"]] = {
            "object_key": category["object_key"],
            "local_path": category["local_path"],
            "usage": [f"分类:{category['name']}"],
        }
        for child in category["children"]:
            uploads.setdefault(
                child["object_key"],
                {"object_key": child["object_key"], "local_path": child["local_path"], "usage": []},
            )["usage"].append(f"分类:{category['name']}/{child['name']}")
    for product in resolved_products:
        uploads.setdefault(
            product["object_key"],
            {"object_key": product["object_key"], "local_path": product["local_path"], "usage": []},
        )["usage"].append(f"商品:{product['store_name']}")
    return dict(sorted(uploads.items(), key=lambda item: item[0]))


def build_attachment_sql(upload_items: Dict[str, dict]) -> List[str]:
    statements: List[str] = []
    for item in upload_items.values():
        local_path = Path(item["local_path"])
        statements.append(
            "INSERT INTO eb_system_attachment "
            "(name, att_dir, satt_dir, att_size, att_type, pid, image_type, create_time, update_time) VALUES "
            f"({sql_quote(local_path.name)}, '', {sql_quote(item['object_key'])}, {sql_quote(file_size_label(local_path))}, "
            f"{sql_quote(file_att_type(local_path))}, 0, 4, NOW(), NOW());"
        )
    return statements


def build_cleanup_sql(category_names: List[str], product_names: List[str], attachment_keys: List[str]) -> List[str]:
    merged_product_names = product_names + [name for name in LEGACY_PRODUCT_NAMES if name not in product_names]
    merged_category_names = category_names + [name for name in LEGACY_CATEGORY_NAMES if name not in category_names]
    quoted_products = ", ".join(sql_quote(name) for name in merged_product_names)
    quoted_categories = ", ".join(sql_quote(name) for name in merged_category_names)
    quoted_attachments = ", ".join(sql_quote(key) for key in attachment_keys)
    return [
        f"DELETE FROM eb_store_product_description WHERE product_id IN (SELECT id FROM (SELECT id FROM eb_store_product WHERE store_name IN ({quoted_products})) tmp);",
        f"DELETE FROM eb_store_product_attr_value WHERE product_id IN (SELECT id FROM (SELECT id FROM eb_store_product WHERE store_name IN ({quoted_products})) tmp);",
        f"DELETE FROM eb_store_product_attr WHERE product_id IN (SELECT id FROM (SELECT id FROM eb_store_product WHERE store_name IN ({quoted_products})) tmp);",
        f"DELETE FROM eb_store_product WHERE store_name IN ({quoted_products});",
        f"DELETE FROM eb_category WHERE type = 1 AND name IN ({quoted_categories});",
        f"DELETE FROM eb_system_attachment WHERE satt_dir IN ({quoted_attachments});",
    ]


def build_category_sql(resolved_categories: List[dict]) -> List[str]:
    statements: List[str] = []
    for category in resolved_categories:
        statements.append(
            "INSERT INTO eb_category (pid, path, name, type, url, extra, status, sort, create_time, update_time) VALUES "
            f"(0, '/0/', {sql_quote(category['name'])}, 1, '', {sql_quote(category['object_key'])}, 1, {category['sort']}, NOW(), NOW());"
        )
        statements.append(f"SET {category['var_name']} := LAST_INSERT_ID();")
        for child in category["children"]:
            statements.append(
                "INSERT INTO eb_category (pid, path, name, type, url, extra, status, sort, create_time, update_time) VALUES "
                f"({category['var_name']}, CONCAT('/0/', {category['var_name']}, '/'), {sql_quote(child['name'])}, 1, '', "
                f"{sql_quote(child['object_key'])}, 1, {child['sort']}, NOW(), NOW());"
            )
            statements.append(f"SET {child['var_name']} := LAST_INSERT_ID();")
    return statements


def build_product_sql(resolved_products: List[dict]) -> List[str]:
    statements: List[str] = []
    for index, product in enumerate(resolved_products, start=1):
        product_var = f"@product_{index:03d}"
        price = product["price"]
        cost = product["cost"]
        ot_price = product["ot_price"]
        vip_price = price
        is_best = 1 if index % 3 == 0 else 0
        is_new = 1 if index % 4 == 0 else 0
        is_hot = 1 if index % 5 == 0 else 0
        ficti = 20 + index * 3
        browse = 80 + index * 11
        barcode = f"690260309{index:04d}"
        slider_json = json.dumps([product["object_key"]], ensure_ascii=False)
        description = f"<p>{product['store_info']}</p><p>适合超市进货商城演示使用，支持门店陈列和日常补货展示。</p>"

        statements.append(
            "INSERT INTO eb_store_product "
            "(mer_id, image, slider_image, store_name, store_info, keyword, bar_code, cate_id, price, vip_price, ot_price, postage, "
            "unit_name, sort, sales, stock, is_show, is_hot, is_benefit, is_best, is_new, add_time, is_postage, is_del, mer_use, "
            "give_integral, cost, is_seckill, is_bargain, is_good, is_sub, ficti, browse, code_path, soure_link, video_link, temp_id, "
            "spec_type, activity, flat_pattern, version) VALUES "
            f"(0, {sql_quote(product['object_key'])}, {sql_quote(slider_json)}, {sql_quote(product['store_name'])}, "
            f"{sql_quote(product['store_info'])}, {sql_quote(product['keyword'])}, {sql_quote(barcode)}, CAST({product['child_var_name']} AS CHAR), "
            f"{price}, {vip_price}, {ot_price}, 0.00, {sql_quote(product['unit_name'])}, {2000 - index}, 0, {product['stock']}, 1, {is_hot}, 0, "
            f"{is_best}, {is_new}, UNIX_TIMESTAMP({sql_quote(SEED_DATETIME)}), 1, 0, 0, 0, {cost}, 0, 0, 0, 0, {ficti}, {browse}, '', '', '', 1, 0, "
            "'0, 1, 2, 3', '', 0);"
        )
        statements.append(f"SET {product_var} := LAST_INSERT_ID();")
        statements.append(
            f"INSERT INTO eb_store_product_attr (product_id, attr_name, attr_values, type, is_del) VALUES ({product_var}, '规格', '默认', 0, 0);"
        )
        statements.append(
            "INSERT INTO eb_store_product_attr_value "
            "(product_id, suk, stock, sales, price, image, `unique`, cost, bar_code, ot_price, weight, volume, brokerage, brokerage_two, "
            "type, quota, quota_show, attr_value, is_del, version) VALUES "
            f"({product_var}, '默认', {product['stock']}, 0, {price}, {sql_quote(product['object_key'])}, '', {cost}, {sql_quote(barcode)}, "
            f"{ot_price}, 0.00, 0.00, 0.00, 0.00, 0, 0, 0, '{{\"规格\":\"默认\"}}', 0, 0);"
        )
        statements.append(
            f"INSERT INTO eb_store_product_description (product_id, description, type) VALUES ({product_var}, {sql_quote(description)}, 0);"
        )
    return statements


def build_verify_sql(resolved_categories: List[dict], resolved_products: List[dict], upload_items: Dict[str, dict]) -> str:
    category_names = [category["name"] for category in resolved_categories]
    for category in resolved_categories:
        category_names.extend(child["name"] for child in category["children"])
    product_names = [product["store_name"] for product in resolved_products]
    attachment_keys = list(upload_items.keys())

    lines = [
        "SET NAMES utf8mb4;",
        f"SELECT COUNT(*) AS category_count FROM eb_category WHERE type = 1 AND name IN ({', '.join(sql_quote(name) for name in category_names)});",
        f"SELECT COUNT(*) AS product_count FROM eb_store_product WHERE is_del = 0 AND store_name IN ({', '.join(sql_quote(name) for name in product_names)});",
        f"SELECT COUNT(*) AS attachment_count FROM eb_system_attachment WHERE satt_dir IN ({', '.join(sql_quote(key) for key in attachment_keys)});",
        "SELECT id, pid, name, sort, extra FROM eb_category WHERE type = 1 AND name IN "
        f"({', '.join(sql_quote(name) for name in ['饮品酒水', '饮用水', '白酒', '红酒', '新鲜水果', '休闲零食'])}) ORDER BY id;",
        "SELECT id, store_name, cate_id, price, stock, image FROM eb_store_product WHERE store_name IN "
        f"({', '.join(sql_quote(name) for name in ['农夫山泉饮用天然水', '东方树叶乌龙茶', '五粮液浓香白酒', '红富士苹果', '海飞丝洗发露'])}) ORDER BY id;",
    ]
    return "\n".join(lines) + "\n"


def write_outputs(sql_text: str, verify_sql_text: str, upload_items: Dict[str, dict]) -> None:
    GENERATED_DIR.mkdir(parents=True, exist_ok=True)
    generated_sql = GENERATED_DIR / "01_supermarket_seed.sql"
    generated_verify_sql = GENERATED_DIR / "02_supermarket_seed_verify.sql"
    upload_manifest = GENERATED_DIR / "upload_manifest.json"

    generated_sql.write_text(sql_text, encoding="utf-8")
    generated_verify_sql.write_text(verify_sql_text, encoding="utf-8")
    upload_manifest.write_text(
        json.dumps(list(upload_items.values()), ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    shutil.copyfile(generated_sql, FINAL_SQL_PATH)
    shutil.copyfile(generated_verify_sql, FINAL_VERIFY_SQL_PATH)


def build_sql(manifest: dict, resolved_categories: List[dict], resolved_products: List[dict], upload_items: Dict[str, dict]) -> str:
    category_names = [category["name"] for category in resolved_categories]
    for category in resolved_categories:
        category_names.extend(child["name"] for child in category["children"])
    product_names = [product["store_name"] for product in resolved_products]
    attachment_keys = list(upload_items.keys())

    parts: List[str] = [
        "SET NAMES utf8mb4;",
        "START TRANSACTION;",
        f"-- {manifest['seed_tag']} 超市演示数据导入脚本",
    ]
    parts.extend(build_cleanup_sql(category_names, product_names, attachment_keys))
    parts.extend(build_attachment_sql(upload_items))
    parts.extend(build_category_sql(resolved_categories))
    parts.extend(build_product_sql(resolved_products))
    parts.append("COMMIT;")
    return "\n".join(parts) + "\n"


def update_readme_summary(root_count: int, child_count: int, product_count: int, image_count: int) -> None:
    readme_path = BASE_DIR / "README.md"
    content = readme_path.read_text(encoding="utf-8")
    content = re.sub(r"- 一级分类：\d+ 个", f"- 一级分类：{root_count} 个", content)
    content = re.sub(r"- 二级分类：\d+ 个", f"- 二级分类：{child_count} 个", content)
    content = re.sub(r"- 示例商品：\d+ 个", f"- 示例商品：{product_count} 个", content)
    if "- 上传图片：" not in content:
        content += f"\n- 上传图片：{image_count} 张\n"
    else:
        content = re.sub(r"- 上传图片：\d+ 张", f"- 上传图片：{image_count} 张", content)
    readme_path.write_text(content, encoding="utf-8")


def main() -> None:
    args = parse_args()
    manifest = load_manifest()
    material_root = Path(manifest["material_root"])
    if not material_root.exists():
        raise FileNotFoundError(f"素材根目录不存在: {material_root}")

    file_index = build_file_index(material_root)
    resolved_categories, resolved_products = validate_manifest(manifest, material_root, file_index)

    db_config = get_db_config()
    connection = get_connection(db_config)
    try:
        cos_settings = fetch_cos_settings(connection)
    finally:
        connection.close()

    upload_items = collect_upload_items(resolved_categories, resolved_products)
    update_readme_summary(
        len(resolved_categories),
        sum(len(category["children"]) for category in resolved_categories),
        len(resolved_products),
        len(upload_items),
    )

    if args.check_only:
        print(f"校验通过: 一级分类 {len(resolved_categories)} 个, 二级分类 {sum(len(category['children']) for category in resolved_categories)} 个, 商品 {len(resolved_products)} 个, 图片 {len(upload_items)} 张")
        return

    client = build_cos_client(cos_settings)
    bucket = cos_settings["txStorageName"]
    for item in upload_items.values():
        upload_if_needed(client, bucket, item["object_key"], Path(item["local_path"]))

    sql_text = build_sql(manifest, resolved_categories, resolved_products, upload_items)
    verify_sql_text = build_verify_sql(resolved_categories, resolved_products, upload_items)
    write_outputs(sql_text, verify_sql_text, upload_items)
    print(f"生成完成: {FINAL_SQL_PATH}")
    print(f"校验 SQL: {FINAL_VERIFY_SQL_PATH}")


if __name__ == "__main__":
    main()
