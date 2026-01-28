-- 促销活动扩展相关表结构
-- 说明：本文件用于记录“满减/买赠/代金券”等促销能力的数据库脚本，需在业务库中手动执行。

-- =========================================================
-- 满减活动（Full Reduction）
-- =========================================================

-- 满减活动主表
CREATE TABLE eb_full_reduction (
  id INT(11) PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL COMMENT '活动名称',
  scope_type TINYINT(1) NOT NULL DEFAULT 1 COMMENT '范围：1-全场 2-品类 3-指定商品',
  start_time DATETIME NOT NULL COMMENT '开始时间',
  end_time DATETIME NOT NULL COMMENT '结束时间',
  allow_coupon TINYINT(1) DEFAULT 1 COMMENT '是否允许叠加优惠券：0-否 1-是',
  status TINYINT(1) DEFAULT 0 COMMENT '状态：0-关闭 1-开启',
  is_del TINYINT(1) DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='满减活动主表';

-- 满减阶梯表
CREATE TABLE eb_full_reduction_level (
  id INT(11) PRIMARY KEY AUTO_INCREMENT,
  reduction_id INT(11) NOT NULL COMMENT '满减活动ID',
  full_amount DECIMAL(10,2) NOT NULL COMMENT '满足金额',
  reduce_amount DECIMAL(10,2) NOT NULL COMMENT '减免金额',
  KEY idx_reduction_id (reduction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='满减阶梯表';

-- 满减关联表
CREATE TABLE eb_full_reduction_product (
  id INT(11) PRIMARY KEY AUTO_INCREMENT,
  reduction_id INT(11) NOT NULL COMMENT '满减活动ID',
  relation_type TINYINT(1) NOT NULL COMMENT '关联类型：1-品类 2-商品',
  relation_id INT(11) NOT NULL COMMENT '品类ID或商品ID',
  KEY idx_reduction_id (reduction_id),
  KEY idx_relation (relation_type, relation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='满减关联表';

-- =========================================================
-- 买赠活动（Buy Gift）
-- =========================================================

-- 买赠活动主表
CREATE TABLE eb_buy_gift (
  id INT(11) PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL COMMENT '活动名称',
  gift_type TINYINT(1) NOT NULL DEFAULT 1 COMMENT '类型：1-同商品买N送M 2-跨商品买A送B',
  start_time DATETIME NOT NULL COMMENT '开始时间',
  end_time DATETIME NOT NULL COMMENT '结束时间',
  limit_type TINYINT(1) DEFAULT 0 COMMENT '限制类型：0-不限 1-总次数 2-每日次数',
  limit_num INT(11) DEFAULT 0 COMMENT '限制次数',
  status TINYINT(1) DEFAULT 0 COMMENT '状态：0-关闭 1-开启',
  is_del TINYINT(1) DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='买赠活动主表';

-- 买赠商品关联表
CREATE TABLE eb_buy_gift_product (
  id INT(11) PRIMARY KEY AUTO_INCREMENT,
  gift_id INT(11) NOT NULL COMMENT '买赠活动ID',
  product_id INT(11) NOT NULL COMMENT '商品ID',
  attr_value_id INT(11) DEFAULT 0 COMMENT '规格ID（0=不限规格）',
  product_type TINYINT(1) NOT NULL COMMENT '类型：1-购买商品 2-赠品',
  buy_num INT(11) DEFAULT 0 COMMENT '购买数量',
  gift_num INT(11) DEFAULT 0 COMMENT '赠送数量',
  KEY idx_gift_id (gift_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='买赠商品关联表';

-- 买赠参与记录表
CREATE TABLE eb_buy_gift_record (
  id INT(11) PRIMARY KEY AUTO_INCREMENT,
  gift_id INT(11) NOT NULL COMMENT '买赠活动ID',
  uid INT(11) NOT NULL COMMENT '用户ID',
  order_id VARCHAR(32) NOT NULL COMMENT '订单号',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '参与时间',
  KEY idx_gift_uid (gift_id, uid),
  KEY idx_uid_date (uid, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='买赠参与记录表';

-- =========================================================
-- 代金券扩展（基于现有优惠券表）
-- =========================================================

-- 扩展优惠券表（添加代金券相关字段）
ALTER TABLE eb_store_coupon
ADD COLUMN coupon_type TINYINT(1) DEFAULT 1 COMMENT '券类型：1-优惠券 2-代金券' AFTER id,
ADD COLUMN can_deduct_freight TINYINT(1) DEFAULT 0 COMMENT '是否可抵扣运费：0-否 1-是' AFTER min_price;

-- 扩展订单表（添加代金券和满减相关字段）
ALTER TABLE eb_store_order
ADD COLUMN voucher_id INT(11) DEFAULT 0 COMMENT '代金券ID' AFTER coupon_price,
ADD COLUMN voucher_price DECIMAL(8,2) DEFAULT 0.00 COMMENT '代金券抵扣金额' AFTER voucher_id,
ADD COLUMN full_reduction_id INT(11) DEFAULT 0 COMMENT '满减活动ID' AFTER voucher_price,
ADD COLUMN full_reduction_price DECIMAL(8,2) DEFAULT 0.00 COMMENT '满减金额' AFTER full_reduction_id;

-- 订单明细表添加赠品标记
ALTER TABLE eb_store_order_info
ADD COLUMN is_gift TINYINT(1) DEFAULT 0 COMMENT '是否赠品：0-否 1-是' AFTER price;

-- =========================================================
-- 系统菜单（建议通过“系统菜单管理”后台新增；若用SQL导入，需注意清理 Redis 缓存 menuList）
-- 说明：
-- 1) 本项目 eb_system_menu.component 存储的是前端路由 path（例如：/marketing/promotion/fullReduction）
-- 2) 通过 SQL 直接插入后，后端 Redis 的 menuList 缓存不会自动失效，需要手动删除该 key 或重启服务
-- =========================================================

-- 取“营销”父菜单ID（找不到请手动替换 @marketing_id）
SET @marketing_id = (
  SELECT id FROM eb_system_menu
  WHERE name = '营销' AND menu_type = 'M'
  ORDER BY id DESC LIMIT 1
);

-- 促销活动一级菜单（目录）
INSERT INTO eb_system_menu (pid, name, icon, perms, menu_type, component, sort, is_show, create_time, update_time)
SELECT @marketing_id, '促销活动', 'present', '', 'M', '/marketing/promotion', 10, 1, NOW(), NOW()
FROM DUAL
WHERE @marketing_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE component = '/marketing/promotion' AND menu_type = 'M');

SET @promotion_id = (
  SELECT id FROM eb_system_menu
  WHERE component = '/marketing/promotion' AND menu_type = 'M'
  ORDER BY id DESC LIMIT 1
);

-- 满减活动菜单
INSERT INTO eb_system_menu (pid, name, icon, perms, menu_type, component, sort, is_show, create_time, update_time)
SELECT @promotion_id, '满减活动', '', 'admin:promotion:full-reduction:list', 'C', '/marketing/promotion/fullReduction', 1, 1, NOW(), NOW()
FROM DUAL
WHERE @promotion_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE perms = 'admin:promotion:full-reduction:list');

SET @full_reduction_menu_id = (
  SELECT id FROM eb_system_menu
  WHERE perms = 'admin:promotion:full-reduction:list'
  ORDER BY id DESC LIMIT 1
);

-- 满减活动按钮权限
INSERT INTO eb_system_menu (pid, name, icon, perms, menu_type, component, sort, is_show, create_time, update_time)
SELECT @full_reduction_menu_id, '满减活动详情', '', 'admin:promotion:full-reduction:info', 'A', '', 1, 0, NOW(), NOW()
FROM DUAL
WHERE @full_reduction_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE perms = 'admin:promotion:full-reduction:info');

INSERT INTO eb_system_menu (pid, name, icon, perms, menu_type, component, sort, is_show, create_time, update_time)
SELECT @full_reduction_menu_id, '满减活动新增/编辑', '', 'admin:promotion:full-reduction:save', 'A', '', 2, 0, NOW(), NOW()
FROM DUAL
WHERE @full_reduction_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE perms = 'admin:promotion:full-reduction:save');

INSERT INTO eb_system_menu (pid, name, icon, perms, menu_type, component, sort, is_show, create_time, update_time)
SELECT @full_reduction_menu_id, '满减活动删除', '', 'admin:promotion:full-reduction:delete', 'A', '', 3, 0, NOW(), NOW()
FROM DUAL
WHERE @full_reduction_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE perms = 'admin:promotion:full-reduction:delete');

INSERT INTO eb_system_menu (pid, name, icon, perms, menu_type, component, sort, is_show, create_time, update_time)
SELECT @full_reduction_menu_id, '满减活动状态', '', 'admin:promotion:full-reduction:status', 'A', '', 4, 0, NOW(), NOW()
FROM DUAL
WHERE @full_reduction_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE perms = 'admin:promotion:full-reduction:status');

-- 买赠活动菜单
INSERT INTO eb_system_menu (pid, name, icon, perms, menu_type, component, sort, is_show, create_time, update_time)
SELECT @promotion_id, '买赠活动', '', 'admin:promotion:buy-gift:list', 'C', '/marketing/promotion/buyGift', 2, 1, NOW(), NOW()
FROM DUAL
WHERE @promotion_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE perms = 'admin:promotion:buy-gift:list');

SET @buy_gift_menu_id = (
  SELECT id FROM eb_system_menu
  WHERE perms = 'admin:promotion:buy-gift:list'
  ORDER BY id DESC LIMIT 1
);

-- 买赠活动按钮权限
INSERT INTO eb_system_menu (pid, name, icon, perms, menu_type, component, sort, is_show, create_time, update_time)
SELECT @buy_gift_menu_id, '买赠活动详情', '', 'admin:promotion:buy-gift:info', 'A', '', 1, 0, NOW(), NOW()
FROM DUAL
WHERE @buy_gift_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE perms = 'admin:promotion:buy-gift:info');

INSERT INTO eb_system_menu (pid, name, icon, perms, menu_type, component, sort, is_show, create_time, update_time)
SELECT @buy_gift_menu_id, '买赠活动新增/编辑', '', 'admin:promotion:buy-gift:save', 'A', '', 2, 0, NOW(), NOW()
FROM DUAL
WHERE @buy_gift_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE perms = 'admin:promotion:buy-gift:save');

INSERT INTO eb_system_menu (pid, name, icon, perms, menu_type, component, sort, is_show, create_time, update_time)
SELECT @buy_gift_menu_id, '买赠活动删除', '', 'admin:promotion:buy-gift:delete', 'A', '', 3, 0, NOW(), NOW()
FROM DUAL
WHERE @buy_gift_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE perms = 'admin:promotion:buy-gift:delete');

INSERT INTO eb_system_menu (pid, name, icon, perms, menu_type, component, sort, is_show, create_time, update_time)
SELECT @buy_gift_menu_id, '买赠活动状态', '', 'admin:promotion:buy-gift:status', 'A', '', 4, 0, NOW(), NOW()
FROM DUAL
WHERE @buy_gift_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE perms = 'admin:promotion:buy-gift:status');

-- 代金券菜单（作为“营销”下的独立菜单；如需放到“优惠券”下，可把 pid 改为优惠券菜单ID）
INSERT INTO eb_system_menu (pid, name, icon, perms, menu_type, component, sort, is_show, create_time, update_time)
SELECT @marketing_id, '代金券列表', '', 'admin:marketing:voucher:list', 'C', '/marketing/voucher', 11, 1, NOW(), NOW()
FROM DUAL
WHERE @marketing_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE perms = 'admin:marketing:voucher:list');

SET @voucher_menu_id = (
  SELECT id FROM eb_system_menu
  WHERE perms = 'admin:marketing:voucher:list'
  ORDER BY id DESC LIMIT 1
);

-- 代金券按钮权限
INSERT INTO eb_system_menu (pid, name, icon, perms, menu_type, component, sort, is_show, create_time, update_time)
SELECT @voucher_menu_id, '代金券详情', '', 'admin:marketing:voucher:info', 'A', '', 1, 0, NOW(), NOW()
FROM DUAL
WHERE @voucher_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE perms = 'admin:marketing:voucher:info');

INSERT INTO eb_system_menu (pid, name, icon, perms, menu_type, component, sort, is_show, create_time, update_time)
SELECT @voucher_menu_id, '代金券新增/编辑', '', 'admin:marketing:voucher:save', 'A', '', 2, 0, NOW(), NOW()
FROM DUAL
WHERE @voucher_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE perms = 'admin:marketing:voucher:save');

INSERT INTO eb_system_menu (pid, name, icon, perms, menu_type, component, sort, is_show, create_time, update_time)
SELECT @voucher_menu_id, '代金券删除', '', 'admin:marketing:voucher:delete', 'A', '', 3, 0, NOW(), NOW()
FROM DUAL
WHERE @voucher_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE perms = 'admin:marketing:voucher:delete');

INSERT INTO eb_system_menu (pid, name, icon, perms, menu_type, component, sort, is_show, create_time, update_time)
SELECT @voucher_menu_id, '代金券发放', '', 'admin:marketing:voucher:send', 'A', '', 4, 0, NOW(), NOW()
FROM DUAL
WHERE @voucher_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE perms = 'admin:marketing:voucher:send');
