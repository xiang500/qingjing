-- ============================================================
-- 轻境 · 演示数据（可重复执行：先清空演示数据再写入）
-- 目标库：chuaizhibeidatabase（表结构见 schema.sql）
-- 知识库片段不在此插入：后端启动时若表为空会用 ai/outfit-knowledge.json 初始化
-- ============================================================

SET NAMES utf8mb4;

-- ---------- 0. 重置演示数据 ----------
DELETE FROM `wear_log`;
DELETE FROM `comment`;
DELETE FROM `post_like`;
DELETE FROM `post`;
DELETE FROM `ai_recommendation`;
DELETE FROM `chat_message`;
DELETE FROM `chat_session`;
DELETE FROM `outfit`;
DELETE FROM `clothing`;
DELETE FROM `user`;
DELETE FROM `knowledge_chunk`;
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
-- ============================================================
-- 实拍演示数据（社区帖子 / 套装封面 / 衣橱实拍单品）
-- 图片素材位于 frontend/public/items 与 frontend/public/looks
-- 本段可重复执行，会覆盖上面的插画版单品与示例帖子
-- ============================================================
-- ============================================================
-- 真实照片演示数据：衣橱单品、套装封面、社区帖子与作者
-- 图片素材位于 frontend/public/items/ 与 frontend/public/looks/
-- 可重复执行（先删后插 / ON DUPLICATE KEY UPDATE）
-- ============================================================

SET NAMES utf8mb4;

-- ---------- 1. 三位示例作者（密码与 demo 相同：123456） ----------
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `avatar_url`, `gender`, `height_cm`, `weight_kg`, `body_type`, `style_preference`, `created_at`, `updated_at`, `deleted`)
SELECT 3, 'lin', u.`password`, '林一', NULL, 1, 178.0, 68.0, 'H', JSON_ARRAY('简约', '通勤', '街头'), NOW(), NOW(), 0 FROM `user` u WHERE u.`id` = 1
ON DUPLICATE KEY UPDATE `nickname` = VALUES(`nickname`), `style_preference` = VALUES(`style_preference`);

INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `avatar_url`, `gender`, `height_cm`, `weight_kg`, `body_type`, `style_preference`, `created_at`, `updated_at`, `deleted`)
SELECT 4, 'xiaoyu', u.`password`, '小鱼', NULL, 2, 163.0, 48.0, 'A', JSON_ARRAY('甜美', '学院', '叠穿'), NOW(), NOW(), 0 FROM `user` u WHERE u.`id` = 1
ON DUPLICATE KEY UPDATE `nickname` = VALUES(`nickname`), `style_preference` = VALUES(`style_preference`);

INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `avatar_url`, `gender`, `height_cm`, `weight_kg`, `body_type`, `style_preference`, `created_at`, `updated_at`, `deleted`)
SELECT 5, 'amu', u.`password`, '阿木', NULL, 1, 182.0, 74.0, 'Y', JSON_ARRAY('工装', '复古', '宽松'), NOW(), NOW(), 0 FROM `user` u WHERE u.`id` = 1
ON DUPLICATE KEY UPDATE `nickname` = VALUES(`nickname`), `style_preference` = VALUES(`style_preference`);

-- ---------- 2. 十二件真实单品 ----------
UPDATE `clothing` SET `name` = '基础款纯棉白T恤', `category` = 'TOP', `sub_category` = 'T恤',
  `color` = '本白', `color_hex` = '#F2EFE9', `season` = 'SPRING,SUMMER,AUTUMN',
  `style_tags` = JSON_ARRAY('简约', '基础款', '百搭'), `material` = '精梳棉', `brand` = 'LETTEHHEART',
  `image_url` = '/items/item-white-tee.jpg', `price` = 129.00
WHERE `id` = 1;

UPDATE `clothing` SET `name` = '做旧水洗宽松衬衫', `category` = 'TOP', `sub_category` = '衬衫',
  `color` = '米白做旧', `color_hex` = '#E7E0D2', `season` = 'SPRING,AUTUMN',
  `style_tags` = JSON_ARRAY('复古', '宽松', '叠穿'), `material` = '水洗棉', `brand` = 'NEHD',
  `image_url` = '/items/item-bleached-shirt.jpg', `price` = 359.00
WHERE `id` = 2;

UPDATE `clothing` SET `name` = '黑色粗针麻花毛衣', `category` = 'TOP', `sub_category` = '毛衣',
  `color` = '墨黑', `color_hex` = '#26262A', `season` = 'AUTUMN,WINTER',
  `style_tags` = JSON_ARRAY('日系', '宽松', '针织'), `material` = '羊毛混纺', `brand` = 'studio',
  `image_url` = '/items/item-black-knit.jpg', `price` = 469.00
WHERE `id` = 3;

UPDATE `clothing` SET `name` = '灰色连帽卫衣', `category` = 'TOP', `sub_category` = '卫衣',
  `color` = '灰绿', `color_hex` = '#6E6A5C', `season` = 'AUTUMN,WINTER',
  `style_tags` = JSON_ARRAY('街头', '宽松', '刺绣'), `material` = '抓绒棉', `brand` = 'Unreborn',
  `image_url` = '/items/item-grey-hoodie.jpg', `price` = 329.00
WHERE `id` = 4;

UPDATE `clothing` SET `name` = '格纹翻领POLO卫衣', `category` = 'TOP', `sub_category` = '卫衣',
  `color` = '奶油白', `color_hex` = '#EDE8DC', `season` = 'SPRING,AUTUMN',
  `style_tags` = JSON_ARRAY('学院', '甜美', '刺绣'), `material` = '卫衣布', `brand` = 'kitty',
  `image_url` = '/items/item-polo-sweatshirt.jpg', `price` = 189.00
WHERE `id` = 5;

UPDATE `clothing` SET `name` = '蓝色绣花开衫', `category` = 'OUTER', `sub_category` = '针织开衫',
  `color` = '牛仔蓝', `color_hex` = '#38528C', `season` = 'AUTUMN,WINTER',
  `style_tags` = JSON_ARRAY('复古', '花纹', '慵懒'), `material` = '马海毛混纺', `brand` = 'futenali',
  `image_url` = '/items/item-blue-cardigan.jpg', `price` = 529.00
WHERE `id` = 6;

UPDATE `clothing` SET `name` = '灰色双面呢长大衣', `category` = 'OUTER', `sub_category` = '大衣',
  `color` = '深灰', `color_hex` = '#4B4B4F', `season` = 'AUTUMN,WINTER',
  `style_tags` = JSON_ARRAY('通勤', '正装', '羊毛'), `material` = '羊毛双面呢', `brand` = 'FW26',
  `image_url` = '/items/item-grey-coat.jpg', `price` = 1980.00
WHERE `id` = 7;

UPDATE `clothing` SET `name` = '灰色阔腿卫裤', `category` = 'BOTTOM', `sub_category` = '卫裤',
  `color` = '中灰', `color_hex` = '#9C9C9C', `season` = 'AUTUMN,WINTER',
  `style_tags` = JSON_ARRAY('宽松', '舒适', '街头'), `material` = '抓绒棉', `brand` = 'studio',
  `image_url` = '/items/item-grey-pants.jpg', `price` = 299.00
WHERE `id` = 8;

UPDATE `clothing` SET `name` = '黑色百褶短裙', `category` = 'BOTTOM', `sub_category` = '半身裙',
  `color` = '黑色', `color_hex` = '#2E2E30', `season` = 'SPRING,AUTUMN',
  `style_tags` = JSON_ARRAY('学院', '甜美', '百褶'), `material` = '涤纶混纺', `brand` = 'kitty',
  `image_url` = '/items/item-pleated-skirt.jpg', `price` = 159.00
WHERE `id` = 9;

UPDATE `clothing` SET `name` = '米白阔腿休闲裤', `category` = 'BOTTOM', `sub_category` = '休闲裤',
  `color` = '米白', `color_hex` = '#DDD6C6', `season` = 'SPRING,SUMMER,AUTUMN',
  `style_tags` = JSON_ARRAY('简约', '宽松', '通勤'), `material` = '棉麻', `brand` = 'futenali',
  `image_url` = '/items/item-cream-pants.jpg', `price` = 269.00
WHERE `id` = 10;

UPDATE `clothing` SET `name` = '豹纹托特包', `category` = 'BAG', `sub_category` = '托特包',
  `color` = '豹纹', `color_hex` = '#C9A99A', `season` = 'SPRING,AUTUMN,WINTER',
  `style_tags` = JSON_ARRAY('花纹', '个性', '大容量'), `material` = '帆布', `brand` = 'mila',
  `image_url` = '/items/item-tote-bag.jpg', `price` = 239.00
WHERE `id` = 11;

UPDATE `clothing` SET `name` = '银色粗链项链', `category` = 'ACCESSORY', `sub_category` = '项链',
  `color` = '银色', `color_hex` = '#C9CCD1', `season` = 'SPRING,SUMMER,AUTUMN,WINTER',
  `style_tags` = JSON_ARRAY('街头', '金属', '点缀'), `material` = '925 银', `brand` = 'LETTEHHEART',
  `image_url` = '/items/item-necklace.jpg', `price` = 189.00
WHERE `id` = 12;

-- ---------- 3. 套装：换上真实封面 ----------
DELETE FROM `outfit` WHERE `id` IN (1, 3, 4);
INSERT INTO `outfit` (`id`, `user_id`, `name`, `cover_url`, `occasion`, `season`, `item_ids`, `score`, `ai_generated`, `ai_reason`)
VALUES
(1, 1, '灰调通勤三件套', '/looks/look-black-knit.jpg', '通勤', 'AUTUMN', JSON_ARRAY(3, 8, 12), 88, 1,
 '同色系叠穿，靠材质（羊毛 × 抓绒）拉开层次，银色粗链是唯一亮点，通勤不会显得用力。'),
(3, 1, '灰绿户外叠穿', '/looks/look-grey-hoodie.jpg', '休闲', 'AUTUMN', JSON_ARRAY(4, 8, 11), 84, 1,
 '灰绿卫衣压住豹纹包的张扬，全身只有两个色域，松垮廓形配大包比例刚好。'),
(4, 1, '蓝调针织周末', '/looks/look-blue-cardigan.jpg', '约会', 'AUTUMN', JSON_ARRAY(6, 10, 12), 86, 1,
 '牛仔蓝开衫配米白阔腿裤，冷调上衣 + 暖调下装，饱和度都不高，周末的松弛感。');

-- ---------- 4. 社区：六条真实穿搭分享 ----------
DELETE FROM `post` WHERE `id` IN (1, 2, 3, 4, 5, 6);

INSERT INTO `post` (`id`, `user_id`, `outfit_id`, `title`, `content`, `images`, `tags`, `like_count`, `comment_count`, `view_count`, `status`, `created_at`)
VALUES
(1, 1, 1, '今天的通勤穿搭被同事要链接了',
 '灰调三件套真的不容易出错：粗针毛衣 + 阔腿卫裤 + 银链。全身没有跳色，靠材质对比撑住，开会也不虚。',
 JSON_ARRAY('/looks/look-black-knit.jpg'),
 JSON_ARRAY('通勤', '同色系', '针织'), 47, 6, 320, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 3, NULL, '灰绿卫衣配豹纹包，松垮廓形也太好穿了',
 '一直不敢碰花色的包，结果配灰绿卫衣意外和谐——大面积素色 + 一处花纹，记住这条就不容易翻车。',
 JSON_ARRAY('/looks/look-grey-hoodie.jpg'),
 JSON_ARRAY('街头', '宽松', '花纹点缀'), 63, 9, 512, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(3, 4, NULL, '学院风套头卫衣，小个子也能穿',
 '格纹翻领 + 百褶裙，裙长刚好在膝盖上一点，比例一下就出来了。配白鞋和白袜子是重点。',
 JSON_ARRAY('/looks/look-polo-sweatshirt.jpg'),
 JSON_ARRAY('学院', '甜美', '小个子'), 88, 14, 764, 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(4, 4, NULL, '蓝色绣花开衫，秋天最上镜的一件',
 '深蓝底 + 姜黄刺绣，本来怕太花，配米白阔腿裤之后反而显得很温柔。开衫一定要选落肩的。',
 JSON_ARRAY('/looks/look-blue-cardigan.jpg'),
 JSON_ARRAY('复古', '开衫', '秋日'), 102, 18, 933, 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(5, 5, NULL, '做旧衬衫这样叠穿，比西装更松弛',
 '衬衫不扣，里面叠白 T，下身同色系工装裤，整体像刚从工作室出来。鞋子选厚底的，显腿长。',
 JSON_ARRAY('/looks/look-bleached-shirt.jpg'),
 JSON_ARRAY('工装', '叠穿', '做旧'), 71, 11, 605, 1, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(6, 5, NULL, '灰色双面呢大衣，两种领型怎么选',
 '翻领更正式，立领更利落。185 以上选中长款不容易压个子，里面一件黑高领就够了。',
 JSON_ARRAY('/looks/look-grey-coats.jpg'),
 JSON_ARRAY('大衣', '通勤', '冬日'), 55, 7, 428, 1, DATE_SUB(NOW(), INTERVAL 8 DAY));

-- 帖子与套装的关联（第 1 条帖子挂在通勤套装上）
UPDATE `post` SET `outfit_id` = 1 WHERE `id` = 1;

-- ---------- 5. 修正历史计数 ----------
UPDATE `post` SET `comment_count` = (SELECT COUNT(*) FROM `comment` c WHERE c.`post_id` = `post`.`id`) WHERE `id` IN (1, 2, 3, 4, 5, 6);