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
-- 系统菜单（示例数据，需根据实际菜单ID调整）
-- =========================================================

-- 添加促销活动菜单
-- 注意：下述“营销菜单ID/优惠券菜单ID”请替换为实际的父级菜单ID
INSERT INTO eb_system_menu (pid, name, icon, perms, component, menu_type, sort, is_show)
VALUES
-- 促销活动一级菜单
(营销菜单ID, '促销活动', 'el-icon-present', '', '', 'M', 10, 1),
-- 满减活动
((SELECT id FROM (SELECT id FROM eb_system_menu WHERE name='促销活动') t), '满减活动', '', 'admin:promotion:full-reduction:list', 'marketing/promotion/fullReduction/index', 'C', 1, 1),
-- 买赠活动
((SELECT id FROM (SELECT id FROM eb_system_menu WHERE name='促销活动') t), '买赠活动', '', 'admin:promotion:buy-gift:list', 'marketing/promotion/buyGift/index', 'C', 2, 1),
-- 代金券（放在优惠券下）
(优惠券菜单ID, '代金券列表', '', 'admin:marketing:voucher:list', 'marketing/voucher/index', 'C', 2, 1);
