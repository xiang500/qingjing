-- =============================================================
-- 《轻境 · AI 实时穿搭与 3D 数字衣橱系统》
-- 数据库结构定义 (MySQL 8.0+)
-- 说明：本文件只建库建表，演示数据见 seed.sql
-- =============================================================

CREATE DATABASE IF NOT EXISTS `qingjing`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE `qingjing`;

-- -------------------------------------------------------------
-- 1. 用户表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`         VARCHAR(64)     NOT NULL                COMMENT '登录名',
  `password`         VARCHAR(100)    NOT NULL                COMMENT 'BCrypt 密码',
  `nickname`         VARCHAR(64)     NOT NULL DEFAULT ''     COMMENT '昵称',
  `avatar_url`       VARCHAR(512)             DEFAULT NULL   COMMENT '头像',
  `gender`           TINYINT         NOT NULL DEFAULT 0      COMMENT '0未知 1男 2女',
  `height_cm`        DECIMAL(5,1)             DEFAULT NULL   COMMENT '身高cm',
  `weight_kg`        DECIMAL(5,1)             DEFAULT NULL   COMMENT '体重kg',
  `body_type`        VARCHAR(16)              DEFAULT NULL   COMMENT '体型 H/A/O/Y/X',
  `style_preference` JSON                    DEFAULT NULL   COMMENT '风格偏好标签数组',
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`          TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB COMMENT = '用户';

-- -------------------------------------------------------------
-- 2. 衣橱单品表（数字衣橱的核心资产）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `clothing`;
CREATE TABLE `clothing` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '单品ID',
  `user_id`       BIGINT UNSIGNED NOT NULL                COMMENT '归属用户',
  `name`          VARCHAR(128)    NOT NULL                COMMENT '单品名称',
  `category`      VARCHAR(32)     NOT NULL                COMMENT 'TOP/BOTTOM/OUTER/DRESS/SHOES/BAG/ACCESSORY',
  `sub_category`  VARCHAR(32)              DEFAULT NULL   COMMENT '子类：T恤/衬衫/牛仔/西装…',
  `color`         VARCHAR(32)              DEFAULT NULL   COMMENT '颜色名',
  `color_hex`     CHAR(7)                  DEFAULT NULL   COMMENT '主色 #RRGGBB，用于 AI 配色推荐',
  `season`        VARCHAR(32)              DEFAULT NULL   COMMENT '逗号分隔：SPRING,SUMMER,AUTUMN,WINTER',
  `style_tags`    JSON                     DEFAULT NULL   COMMENT '风格标签数组',
  `material`      VARCHAR(64)              DEFAULT NULL   COMMENT '材质',
  `brand`         VARCHAR(64)              DEFAULT NULL   COMMENT '品牌',
  `image_url`     VARCHAR(512)             DEFAULT NULL   COMMENT '平铺图',
  `model_url`     VARCHAR(512)             DEFAULT NULL   COMMENT '3D/贴图资源地址（Three.js 换装用）',
  `price`         DECIMAL(10,2)            DEFAULT NULL   COMMENT '购入价',
  `purchase_date` DATE                     DEFAULT NULL,
  `wear_count`    INT             NOT NULL DEFAULT 0      COMMENT '穿着次数',
  `last_worn_at`  DATE                     DEFAULT NULL,
  `favorite`      TINYINT         NOT NULL DEFAULT 0      COMMENT '收藏',
  `note`          VARCHAR(255)             DEFAULT NULL,
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_category` (`user_id`, `category`, `deleted`),
  KEY `idx_user_deleted` (`user_id`, `deleted`)
) ENGINE = InnoDB COMMENT = '衣橱单品';

-- -------------------------------------------------------------
-- 3. 穿搭套装表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `outfit`;
CREATE TABLE `outfit` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT UNSIGNED NOT NULL,
  `name`         VARCHAR(128)    NOT NULL                COMMENT '套装名',
  `cover_url`    VARCHAR(512)             DEFAULT NULL   COMMENT '封面',
  `occasion`     VARCHAR(32)              DEFAULT NULL   COMMENT '通勤/约会/运动/旅行/居家',
  `season`       VARCHAR(16)              DEFAULT NULL,
  `item_ids`     JSON            NOT NULL                COMMENT '单品ID数组，按上装→下装→鞋→配饰顺序',
  `score`        TINYINT                  DEFAULT NULL   COMMENT 'AI 评分 0-100',
  `ai_generated` TINYINT         NOT NULL DEFAULT 0      COMMENT '是否 AI 生成',
  `ai_reason`    TEXT                     DEFAULT NULL   COMMENT 'AI 推荐理由',
  `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`, `deleted`)
) ENGINE = InnoDB COMMENT = '穿搭套装';

-- -------------------------------------------------------------
-- 4. 穿着记录表（喂给 AI 做「衣橱利用率/重复穿搭」分析）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `wear_log`;
CREATE TABLE `wear_log` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT UNSIGNED NOT NULL,
  `outfit_id`  BIGINT UNSIGNED          DEFAULT NULL,
  `worn_date`  DATE            NOT NULL,
  `weather`    VARCHAR(64)              DEFAULT NULL   COMMENT '晴/雨 + 温度',
  `occasion`   VARCHAR(32)              DEFAULT NULL,
  `rating`     TINYINT                  DEFAULT NULL   COMMENT '当天满意度 1-5',
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`, `worn_date`)
) ENGINE = InnoDB COMMENT = '穿着记录';

-- -------------------------------------------------------------
-- 5. 社区帖子表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `post`;
CREATE TABLE `post` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT UNSIGNED NOT NULL,
  `outfit_id`     BIGINT UNSIGNED          DEFAULT NULL,
  `title`         VARCHAR(128)             DEFAULT NULL,
  `content`       TEXT                     DEFAULT NULL,
  `images`        JSON                     DEFAULT NULL   COMMENT '图片地址数组',
  `tags`          JSON                     DEFAULT NULL,
  `like_count`    INT             NOT NULL DEFAULT 0,
  `comment_count` INT             NOT NULL DEFAULT 0,
  `view_count`    INT             NOT NULL DEFAULT 0,
  `status`        TINYINT         NOT NULL DEFAULT 1      COMMENT '1正常 0下架',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`, `deleted`),
  KEY `idx_created` (`created_at`)
) ENGINE = InnoDB COMMENT = '社区帖子';

-- -------------------------------------------------------------
-- 6. 点赞表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `post_like`;
CREATE TABLE `post_like` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `post_id`    BIGINT UNSIGNED NOT NULL,
  `user_id`    BIGINT UNSIGNED NOT NULL,
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_user` (`post_id`, `user_id`)
) ENGINE = InnoDB COMMENT = '帖子点赞';

-- -------------------------------------------------------------
-- 7. 评论表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `post_id`    BIGINT UNSIGNED NOT NULL,
  `user_id`    BIGINT UNSIGNED NOT NULL,
  `parent_id`  BIGINT UNSIGNED          DEFAULT NULL,
  `content`    VARCHAR(500)    NOT NULL,
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted`    TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_post` (`post_id`, `deleted`)
) ENGINE = InnoDB COMMENT = '评论';

-- -------------------------------------------------------------
-- 8. AI 对话会话表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT UNSIGNED NOT NULL,
  `title`      VARCHAR(128)    NOT NULL DEFAULT '新的穿搭咨询',
  `scene`      VARCHAR(32)     NOT NULL DEFAULT 'WARDROBE' COMMENT 'WARDROBE/MIRROR/COMMUNITY',
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`    TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`, `deleted`)
) ENGINE = InnoDB COMMENT = 'AI 对话会话';

-- -------------------------------------------------------------
-- 9. AI 对话消息表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `session_id`     BIGINT UNSIGNED NOT NULL,
  `role`           VARCHAR(16)     NOT NULL COMMENT 'system/user/assistant/tool',
  `content`        MEDIUMTEXT      NOT NULL,
  `tool_calls`     JSON                     DEFAULT NULL COMMENT 'Function Calling 轨迹',
  `recommendation` JSON                     DEFAULT NULL COMMENT '结构化推荐：单品ID + 套装',
  `prompt_tokens`  INT                      DEFAULT NULL,
  `completion_tokens` INT                   DEFAULT NULL,
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`)
) ENGINE = InnoDB COMMENT = 'AI 对话消息';