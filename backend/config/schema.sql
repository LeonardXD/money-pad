-- Database Schema for Money Pad (MySQL)
CREATE DATABASE IF NOT EXISTS `moneypad` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `moneypad`;

-- Users Table
CREATE TABLE IF NOT EXISTS `users` (
  `id` VARCHAR(50) NOT NULL,
  `username` VARCHAR(50) NOT NULL,
  `email` VARCHAR(100) NOT NULL,
  `password` VARCHAR(255) NOT NULL DEFAULT '',
  `bio` TEXT DEFAULT NULL,
  `followers` INT NOT NULL DEFAULT 0,
  `following` INT NOT NULL DEFAULT 0,
  `profileImageUrl` VARCHAR(255) DEFAULT NULL,
  `coverImageUrl` VARCHAR(255) DEFAULT NULL,
  `balance` DOUBLE NOT NULL DEFAULT 0.0,
  `authorIncome` DOUBLE NOT NULL DEFAULT 0.0,
  `readerCoins` INT NOT NULL DEFAULT 0,
  `totalReaderCoins` INT NOT NULL DEFAULT 0,
  `birthday` VARCHAR(10) NOT NULL DEFAULT '',
  `gender` VARCHAR(20) NOT NULL DEFAULT '',
  `preferredGenres` VARCHAR(255) NOT NULL DEFAULT '',
  `referredBy` VARCHAR(50) NOT NULL DEFAULT '',
  `referralCount` INT NOT NULL DEFAULT 0,
  `signupTimestamp` BIGINT NOT NULL DEFAULT 0,
  `isReferralRewardClaimed` TINYINT(1) NOT NULL DEFAULT 0,
  `loginTimestamp` BIGINT NOT NULL DEFAULT 0,
  `onboardingStep` INT NOT NULL DEFAULT 1,
  `onboardingCompleted` TINYINT(1) NOT NULL DEFAULT 0,
  `isVerified` TINYINT(1) NOT NULL DEFAULT 0,
  `adFreeUntil` BIGINT NOT NULL DEFAULT 0,
  `isAdFreePermanently` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_users_username` (`username`),
  UNIQUE KEY `idx_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Stories Table
CREATE TABLE IF NOT EXISTS `stories` (
  `id` VARCHAR(50) NOT NULL,
  `authorId` VARCHAR(50) NOT NULL,
  `authorName` VARCHAR(50) NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `overview` TEXT NOT NULL,
  `genres` VARCHAR(255) NOT NULL DEFAULT '',
  `coverImageUrl` VARCHAR(255) DEFAULT NULL,
  `readCount` INT NOT NULL DEFAULT 0,
  `isPublished` TINYINT(1) NOT NULL DEFAULT 0,
  `isCompleted` TINYINT(1) NOT NULL DEFAULT 0,
  `isMature` TINYINT(1) NOT NULL DEFAULT 0,
  `likes` INT NOT NULL DEFAULT 0,
  `commentsCount` INT NOT NULL DEFAULT 0,
  `uniqueViews` INT NOT NULL DEFAULT 0,
  `repeatedViews` INT NOT NULL DEFAULT 0,
  `lastUpdatedAt` BIGINT NOT NULL DEFAULT 0,
  `isAuthorVerified` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_stories_author` (`authorId`),
  KEY `idx_stories_published` (`isPublished`, `lastUpdatedAt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Story Parts Table
CREATE TABLE IF NOT EXISTS `story_parts` (
  `id` VARCHAR(50) NOT NULL,
  `storyId` VARCHAR(50) NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `content` MEDIUMTEXT NOT NULL,
  `order` INT NOT NULL,
  `publishedAt` BIGINT NOT NULL DEFAULT 0,
  `isPublished` TINYINT(1) NOT NULL DEFAULT 0,
  `readCount` INT NOT NULL DEFAULT 0,
  `headerImageUrl` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_parts_story` (`storyId`, `order`),
  KEY `idx_parts_published` (`storyId`, `isPublished`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Conversations Table
CREATE TABLE IF NOT EXISTS `conversations` (
  `id` VARCHAR(50) NOT NULL,
  `authorId` VARCHAR(50) NOT NULL,
  `senderId` VARCHAR(50) NOT NULL,
  `senderName` VARCHAR(50) NOT NULL,
  `message` TEXT NOT NULL,
  `senderProfileImageUrl` VARCHAR(255) DEFAULT NULL,
  `timestamp` BIGINT NOT NULL DEFAULT 0,
  `parentId` VARCHAR(50) DEFAULT NULL,
  `isSenderVerified` TINYINT(1) NOT NULL DEFAULT 0,
  `likes` INT NOT NULL DEFAULT 0,
  `isLiked` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_convs_author` (`authorId`, `timestamp` DESC),
  KEY `idx_convs_parent` (`parentId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Follows Table
CREATE TABLE IF NOT EXISTS `follows` (
  `followerId` VARCHAR(50) NOT NULL,
  `followedId` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`followerId`, `followedId`),
  KEY `idx_follows_followed` (`followedId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User Read Parts Table
CREATE TABLE IF NOT EXISTS `user_read_parts` (
  `userId` VARCHAR(50) NOT NULL,
  `partId` VARCHAR(50) NOT NULL,
  `storyId` VARCHAR(50) NOT NULL,
  `readAt` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`userId`, `partId`),
  KEY `idx_urp_story` (`userId`, `storyId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Transactions Table
CREATE TABLE IF NOT EXISTS `transactions` (
  `id` VARCHAR(50) NOT NULL,
  `userId` VARCHAR(50) NOT NULL,
  `amount` DOUBLE NOT NULL,
  `method` VARCHAR(50) NOT NULL,
  `accountInfo` VARCHAR(255) NOT NULL,
  `source` VARCHAR(20) NOT NULL DEFAULT '',
  `timestamp` BIGINT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'Pending',
  PRIMARY KEY (`id`),
  KEY `idx_transactions_user` (`userId`, `timestamp` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Reviews Table
CREATE TABLE IF NOT EXISTS `reviews` (
  `id` VARCHAR(50) NOT NULL,
  `storyId` VARCHAR(50) NOT NULL,
  `userId` VARCHAR(50) NOT NULL,
  `username` VARCHAR(50) NOT NULL,
  `userProfileImageUrl` VARCHAR(255) DEFAULT NULL,
  `rating` INT NOT NULL,
  `comment` TEXT NOT NULL,
  `timestamp` BIGINT NOT NULL DEFAULT 0,
  `isUserVerified` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_reviews_story` (`storyId`, `timestamp` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User Story Likes Table
CREATE TABLE IF NOT EXISTS `user_story_likes` (
  `userId` VARCHAR(50) NOT NULL,
  `storyId` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`userId`, `storyId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Part Annotations Table
CREATE TABLE IF NOT EXISTS `part_annotations` (
  `id` VARCHAR(50) NOT NULL,
  `partId` VARCHAR(50) NOT NULL,
  `userId` VARCHAR(50) NOT NULL,
  `username` VARCHAR(50) NOT NULL,
  `selectedText` TEXT NOT NULL,
  `startIndex` INT NOT NULL,
  `endIndex` INT NOT NULL,
  `type` VARCHAR(10) NOT NULL,
  `content` TEXT DEFAULT NULL,
  `timestamp` BIGINT NOT NULL DEFAULT 0,
  `isUserVerified` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_annotations_part` (`partId`, `timestamp` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Library Stories Table
CREATE TABLE IF NOT EXISTS `library_stories` (
  `userId` VARCHAR(50) NOT NULL,
  `storyId` VARCHAR(50) NOT NULL,
  `downloadedAt` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`userId`, `storyId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Reading Lists Table
CREATE TABLE IF NOT EXISTS `reading_lists` (
  `id` VARCHAR(50) NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `description` TEXT DEFAULT NULL,
  `userId` VARCHAR(50) NOT NULL,
  `createdAt` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_reading_lists_user` (`userId`, `createdAt` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Reading List Stories Table
CREATE TABLE IF NOT EXISTS `reading_list_stories` (
  `listId` VARCHAR(50) NOT NULL,
  `storyId` VARCHAR(50) NOT NULL,
  `addedAt` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`listId`, `storyId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Notifications Table
CREATE TABLE IF NOT EXISTS `notifications` (
  `id` VARCHAR(50) NOT NULL,
  `userId` VARCHAR(50) NOT NULL,
  `type` VARCHAR(50) NOT NULL,
  `actorId` VARCHAR(50) NOT NULL,
  `actorName` VARCHAR(50) NOT NULL,
  `actorProfileImageUrl` VARCHAR(255) DEFAULT NULL,
  `storyId` VARCHAR(50) DEFAULT NULL,
  `storyTitle` VARCHAR(255) DEFAULT NULL,
  `partId` VARCHAR(50) DEFAULT NULL,
  `partTitle` VARCHAR(255) DEFAULT NULL,
  `content` TEXT DEFAULT NULL,
  `timestamp` BIGINT NOT NULL DEFAULT 0,
  `isRead` TINYINT(1) NOT NULL DEFAULT 0,
  `isActorVerified` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_notifications_user` (`userId`, `timestamp` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed Official User
INSERT IGNORE INTO `users` (id, username, email, password, bio, onboardingCompleted, onboardingStep, isVerified) 
VALUES ('moneypad_official_id', 'moneypad', 'moneypad@moneypad.com', '@Moneypad3014', 'Official Money Pad Account', 1, 3, 1);

