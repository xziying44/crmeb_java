-- 活动横幅表
CREATE TABLE `eb_activity_banner` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) NOT NULL DEFAULT '' COMMENT '横幅名称',
  `image` varchar(500) NOT NULL DEFAULT '' COMMENT '横幅图片地址',
  `activity_type` tinyint(4) NOT NULL COMMENT '活动类型：1=秒杀 2=砍价 3=拼团 4=买赠 5=满减',
  `status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '状态：0=下线 1=上线',
  `sort` int(11) NOT NULL DEFAULT 0 COMMENT '排序值（越小越靠前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动横幅表';

-- 活动横幅菜单和权限
INSERT INTO `eb_system_menu` (`pid`, `name`, `icon`, `perms`, `component`, `menu_type`, `sort`, `is_show`) VALUES
(7, '活动横幅', '', '', '/marketing/banner/list', 'M', 5, 1);

SET @banner_menu_id = LAST_INSERT_ID();

INSERT INTO `eb_system_menu` (`pid`, `name`, `icon`, `perms`, `component`, `menu_type`, `sort`, `is_show`) VALUES
(@banner_menu_id, '分页列表', '', 'admin:activity:banner:list', '', 'A', 0, 1),
(@banner_menu_id, '新增', '', 'admin:activity:banner:save', '', 'A', 0, 1),
(@banner_menu_id, '修改', '', 'admin:activity:banner:update', '', 'A', 0, 1),
(@banner_menu_id, '删除', '', 'admin:activity:banner:delete', '', 'A', 0, 1),
(@banner_menu_id, '更新状态', '', 'admin:activity:banner:status', '', 'A', 0, 1);

-- 隐藏活动边框菜单（按名称匹配，兼容不同环境的 id）
UPDATE `eb_system_menu` SET `is_show` = 0 WHERE `name` = '活动边框' AND `pid` = 7;
