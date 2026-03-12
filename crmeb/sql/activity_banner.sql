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
