-- =====================================================
-- 业务员模块数据库变更脚本（阶段一）
-- 说明：
-- 1) 本脚本尽量保持可重复执行（通过 EXISTS/条件插入避免重复数据）
-- 2) 正式执行前请在测试库验证无误
-- =====================================================

-- 1. eb_system_role 新增字段：是否业务员角色
-- 提示：MySQL 5.7 不支持 ADD COLUMN IF NOT EXISTS，如重复执行遇到“Duplicate column name”可忽略或先手工判断是否已存在
ALTER TABLE eb_system_role
  ADD COLUMN is_salesman_role TINYINT(1) DEFAULT 0 COMMENT '是否业务员角色 0-否 1-是';

-- 2. 创建业务员扩展信息表
CREATE TABLE IF NOT EXISTS eb_salesman_info (
  id              INT(11) PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  admin_id        INT(11) NOT NULL COMMENT '关联的管理员ID',
  salesman_code   VARCHAR(8) NOT NULL COMMENT '业务员邀请码（6-8位字母数字）',
  salesman_qrcode VARCHAR(255) NULL COMMENT '小程序码图片地址',
  qrcode_scene    VARCHAR(32) NULL COMMENT '小程序码scene参数',
  bindable        TINYINT(1) DEFAULT 1 COMMENT '是否可被绑定 0-否 1-是',
  create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_admin_id (admin_id),
  UNIQUE KEY uk_salesman_code (salesman_code),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务员扩展信息表';

-- 3. eb_user 新增字段：绑定业务员（admin_id）与绑定时间
-- 提示：如重复执行遇到“Duplicate column name”可忽略或先手工判断是否已存在
ALTER TABLE eb_user
  ADD COLUMN salesman_id INT(11) DEFAULT 0 COMMENT '绑定的业务员ID（admin_id）';
ALTER TABLE eb_user
  ADD COLUMN salesman_bind_time DATETIME NULL COMMENT '绑定业务员时间';

-- 索引（如已存在可忽略报错）
ALTER TABLE eb_user ADD INDEX idx_salesman_id (salesman_id);

-- 4. 创建预设业务员角色（若已存在则跳过）
INSERT INTO eb_system_role (role_name, rules, level, status, is_salesman_role, create_time, update_time)
SELECT '业务员', '', 1, 1, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM eb_system_role WHERE role_name = '业务员' OR is_salesman_role = 1
);

-- 5. 创建业务员专属菜单（示例：pid=0 顶级菜单；如已存在则跳过）
INSERT INTO eb_system_menu (pid, name, icon, perms, menu_type, path, component, sort, is_show, create_time, update_time)
SELECT 0, '业务员管理', 'el-icon-user', '', 'M', 'salesman', '', 100, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM eb_system_menu WHERE path = 'salesman' AND menu_type = 'M'
);

-- 取父菜单ID（如果父菜单已存在，则取已存在的 ID）
SET @parent_id = (
  SELECT id FROM eb_system_menu WHERE path = 'salesman' AND menu_type = 'M' ORDER BY id DESC LIMIT 1
);

-- 子菜单：业务员列表
INSERT INTO eb_system_menu (pid, name, icon, perms, menu_type, path, component, sort, is_show, create_time, update_time)
SELECT @parent_id, '业务员列表', '', 'admin:salesman:list', 'C', 'list', 'salesman/list/index', 1, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM eb_system_menu WHERE perms = 'admin:salesman:list'
);

-- 子菜单：客户绑定记录
INSERT INTO eb_system_menu (pid, name, icon, perms, menu_type, path, component, sort, is_show, create_time, update_time)
SELECT @parent_id, '客户绑定记录', '', 'admin:salesman:bindList', 'C', 'bindList', 'salesman/bindList/index', 2, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM eb_system_menu WHERE perms = 'admin:salesman:bindList'
);

-- 子菜单：业绩统计
INSERT INTO eb_system_menu (pid, name, icon, perms, menu_type, path, component, sort, is_show, create_time, update_time)
SELECT @parent_id, '业绩统计', '', 'admin:salesman:statistics', 'C', 'statistics', 'salesman/statistics/index', 3, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM eb_system_menu WHERE perms = 'admin:salesman:statistics'
);
