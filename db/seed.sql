-- =============================================================
-- 《轻境》演示数据（演示账号 demo / 123456）
-- 注意：password 字段为 BCrypt("123456") 的哈希，见 README 说明
-- =============================================================

USE `qingjing`;

-- 演示用户
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `gender`, `height_cm`, `weight_kg`, `body_type`, `style_preference`)
VALUES (1, 'demo', '$2b$10$dXFU1y7F0wLyA/HooFOF6u2mi9wBHLmgy3h9vljpoSgylK9.aaJdW', '轻境体验官', 2, 165.0, 52.0, 'A',
        JSON_ARRAY('通勤', '法式', '简约'))
ON DUPLICATE KEY UPDATE `nickname` = VALUES(`nickname`);

-- 衣橱单品
INSERT INTO `clothing`
(`id`, `user_id`, `name`, `category`, `sub_category`, `color`, `color_hex`, `season`, `style_tags`, `material`, `brand`, `image_url`, `model_url`, `price`, `purchase_date`, `wear_count`, `last_worn_at`, `favorite`, `note`)
VALUES
(1, 1, '基础款纯棉白T恤', 'TOP', 'T恤', '米白', '#F3F1EA', 'SPRING,SUMMER', JSON_ARRAY('基础', '简约'), '纯棉', 'MUJI', '/clothing/1.svg', NULL, 79.00, '2026-03-12', 42, '2026-10-26', 1, '百搭打底，领口略松'),
(2, 1, '雾霾蓝宽松衬衫', 'TOP', '衬衫', '雾霾蓝', '#8CA3B8', 'SPRING,AUTUMN', JSON_ARRAY('通勤', '法式'), '天丝棉', 'COS', '/clothing/2.svg', NULL, 259.00, '2026-04-02', 18, '2026-10-20', 1, '袖口可挽起'),
(3, 1, '直筒水洗牛仔裤', 'BOTTOM', '牛仔', '深蓝', '#4B6584', 'SPRING,AUTUMN,WINTER', JSON_ARRAY('休闲', '复古'), '牛仔', 'Levi''s', '/clothing/3.svg', NULL, 499.00, '2025-11-08', 63, '2026-10-28', 1, '中腰直筒，显腿直'),
(4, 1, '燕麦色针织开衫', 'OUTER', '针织', '燕麦', '#D8C9B0', 'SPRING,AUTUMN', JSON_ARRAY('温柔', '居家'), '羊毛混纺', 'Uniqlo', '/clothing/4.svg', NULL, 299.00, '2026-02-20', 27, '2026-10-22', 0, ''),
(5, 1, '黑色垫肩西装外套', 'OUTER', '西装', '黑', '#1F1F1F', 'SPRING,AUTUMN', JSON_ARRAY('通勤', '气场'), '羊毛', 'Theory', '/clothing/5.svg', NULL, 1290.00, '2026-01-15', 12, '2026-10-18', 1, '见客户必备'),
(6, 1, '米白垂坠伞裙', 'BOTTOM', '半裙', '米白', '#EAE3D8', 'SPRING,SUMMER', JSON_ARRAY('温柔', '法式'), '醋酸', 'MAX&Co.', '/clothing/6.svg', NULL, 399.00, '2026-05-06', 0, NULL, 0, '买回来还没穿过，等一个场合'),
(7, 1, '黑色缎面小礼服', 'DRESS', '连衣裙', '黑', '#141414', 'SUMMER,AUTUMN', JSON_ARRAY('约会', '优雅'), '缎面', 'ZARA', '/clothing/7.svg', NULL, 599.00, '2026-06-18', 3, '2026-09-30', 1, '年会穿过一次'),
(8, 1, '小白鞋帆布鞋', 'SHOES', '帆布鞋', '白', '#F0F0F0', 'SPRING,SUMMER', JSON_ARRAY('休闲', '运动'), '帆布', 'Converse', '/clothing/8.svg', NULL, 459.00, '2026-03-01', 55, '2026-10-28', 1, ''),
(9, 1, '焦糖色乐福鞋', 'SHOES', '乐福鞋', '焦糖', '#8B5E3C', 'SPRING,AUTUMN', JSON_ARRAY('通勤', '复古'), '牛皮', 'Clarks', '/clothing/9.svg', NULL, 799.00, '2025-10-12', 21, '2026-10-18', 0, ''),
(10, 1, '卡其色长风衣', 'OUTER', '风衣', '卡其', '#C3A57E', 'SPRING,AUTUMN', JSON_ARRAY('通勤', '英伦'), '棉质华达呢', 'Burberry', '/clothing/10.svg', NULL, 2980.00, '2025-09-20', 15, '2026-10-14', 1, '雨天慎穿'),
(11, 1, '手工编织托特包', 'BAG', '托特包', '藤黄', '#C9A96A', 'SPRING,SUMMER', JSON_ARRAY('度假', '休闲'), '草编', 'LOEWE', '/clothing/11.svg', NULL, 1690.00, '2026-05-20', 8, '2026-10-02', 0, ''),
(12, 1, '细链金色项链', 'ACCESSORY', '项链', '金', '#D9C07A', 'SPRING,SUMMER,AUTUMN,WINTER', JSON_ARRAY('精致', '通勤'), '925银镀金', 'APM', '/clothing/12.svg', NULL, 520.00, '2026-07-01', 33, '2026-10-27', 1, '锁骨链');

-- 一套 AI 生成的示例穿搭
INSERT INTO `outfit` (`id`, `user_id`, `name`, `cover_url`, `occasion`, `season`, `item_ids`, `score`, `ai_generated`, `ai_reason`)
VALUES (1, 1, '雾蓝衬衫通勤日', '/clothing/2.svg', '通勤', 'AUTUMN', JSON_ARRAY(2, 3, 9, 12), 88, 1,
        '雾霾蓝与深蓝牛仔同色系递进，乐福鞋稳住气场，金色细链提亮面部，适合 18~24℃ 的办公室场景。')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- 社区帖子
INSERT INTO `post` (`id`, `user_id`, `outfit_id`, `title`, `content`, `images`, `tags`, `like_count`, `view_count`)
VALUES (1, 1, 1, '今天的通勤穿搭被同事要链接了',
        '雾蓝衬衫 + 直筒牛仔真的是懒人公式，鞋换成乐福立刻正式起来。', JSON_ARRAY('/clothing/2.svg', '/clothing/9.svg'),
        JSON_ARRAY('通勤', '秋季'), 12, 86)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);