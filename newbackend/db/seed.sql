-- 轻境演示数据：演示账号 demo / 123456（password 为 BCrypt 哈希）



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

-- -------------------------------------------------------------
-- 穿着记录（wear_log）演示数据：喂给 365 天热力图与 CPW
--
-- 可重复执行：先清掉演示用户的历史记录，再以「今天」为终点重新生成，
-- 所以任何时候重跑都能得到一段以当天结尾的 365 天窗口（约 150~200 条）。
-- 分布规律：工作日多、周末少；近 3 个月更密集；中间偶尔断几天；
-- 另外固定植入两段连续记录（距今 6~14 天、38~45 天），保证最长连续 >= 5 天。
-- 少量日期有 2 条记录（一天里换过衣服），热力图按天聚合计数。
-- -------------------------------------------------------------
DELETE FROM `wear_log` WHERE `user_id` = 1;

INSERT INTO `wear_log` (`user_id`, `outfit_id`, `worn_date`, `weather`, `occasion`, `rating`)
WITH RECURSIVE seq (n) AS (
    SELECT 0
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 364
),
days AS (
    -- ago = 距今多少天：0 是今天，364 是窗口最老的一天
    SELECT DATE_SUB(CURDATE(), INTERVAL (364 - n) DAY) AS worn_day,
           364 - n AS ago
    FROM seq
),
multi (k) AS (
    -- k = 1 是每天的主记录，k = 2 是「一天两穿」的补充记录
    SELECT 1 UNION ALL SELECT 2
)
SELECT 1,
       CASE WHEN MOD(CRC32(CONCAT('outfit-', worn_day)), 10) = 0 THEN 1 ELSE NULL END,
       worn_day,
       ELT(1 + MOD(CRC32(CONCAT('weather-', worn_day)), 4), '晴 22℃', '多云 18℃', '小雨 15℃', '阴 12℃'),
       -- 场合跟着星期走：工作日通勤为主，周末才是约会/休闲
       ELT(1 + MOD(CRC32(CONCAT('occasion-', worn_day, '-', multi.k)), 2),
           CASE WHEN DAYOFWEEK(worn_day) IN (1, 7) THEN '周末' ELSE '通勤' END,
           CASE WHEN DAYOFWEEK(worn_day) IN (1, 7) THEN '约会' ELSE '休闲' END),
       3 + MOD(CRC32(CONCAT('rating-', worn_day, '-', multi.k)), 3)
FROM days
CROSS JOIN multi
WHERE (multi.k = 1 OR MOD(CRC32(CONCAT('extra-', worn_day)), 100) < 12)
  AND (
        (ago BETWEEN 6 AND 14)
        OR (ago BETWEEN 38 AND 45)
        OR MOD(CRC32(CONCAT('w-', worn_day)), 1000) <
           CASE
             WHEN DAYOFWEEK(worn_day) IN (1, 7) THEN
                  -- 周末明显稀疏：阈值约打 3.5 折
                  CASE WHEN ago <= 90 THEN 250 WHEN ago <= 200 THEN 180 ELSE 140 END
             ELSE
                  -- 工作日：最近 3 个月 72%，3~7 个月前 52%，更早 40%
                  CASE WHEN ago <= 90 THEN 720 WHEN ago <= 200 THEN 520 ELSE 400 END
           END
      );