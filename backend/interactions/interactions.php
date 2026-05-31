<?php
require_once __DIR__ . '/../config/db.php';

$action = $_GET['action'] ?? '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $input = getJsonInput();
} else {
    $input = $_GET;
}

switch ($action) {
    case 'follow':
        $followerId = $input['followerId'] ?? '';
        $followedId = $input['followedId'] ?? '';

        if (empty($followerId) || empty($followedId)) {
            respondError("Missing follower parameters");
        }

        try {
            $pdo->beginTransaction();

            $stmtInsert = $pdo->prepare("INSERT IGNORE INTO follows (followerId, followedId) VALUES (?, ?)");
            $stmtInsert->execute([$followerId, $followedId]);

            if ($stmtInsert->rowCount() > 0) {
                // Update following/followers count
                $stmtIncFollowing = $pdo->prepare("UPDATE users SET following = following + 1 WHERE id = ?");
                $stmtIncFollowing->execute([$followerId]);

                $stmtIncFollowers = $pdo->prepare("UPDATE users SET followers = followers + 1 WHERE id = ?");
                $stmtIncFollowers->execute([$followedId]);

                // Create follow notification
                $stmtUser = $pdo->prepare("SELECT username, profileImageUrl FROM users WHERE id = ?");
                $stmtUser->execute([$followerId]);
                $actor = $stmtUser->fetch();

                if ($actor) {
                    $notifId = bin2hex(random_bytes(16));
                    $stmtNotif = $pdo->prepare("INSERT INTO notifications (id, userId, type, actorId, actorName, actorProfileImageUrl) VALUES (?, ?, 'FOLLOW', ?, ?, ?)");
                    $stmtNotif->execute([$notifId, $followedId, $followerId, $actor['username'], $actor['profileImageUrl']]);
                }
            }

            $pdo->commit();
            respondJson(["success" => true]);
        } catch (Exception $e) {
            $pdo->rollBack();
            respondError("Follow operation failed: " . $e->getMessage(), 500);
        }
        break;

    case 'unfollow':
        $followerId = $input['followerId'] ?? '';
        $followedId = $input['followedId'] ?? '';

        if (empty($followerId) || empty($followedId)) {
            respondError("Missing follower parameters");
        }

        if ($followedId === 'moneypad_official_id') {
            respondError("Cannot unfollow official account");
        }

        try {
            $pdo->beginTransaction();

            $stmtDelete = $pdo->prepare("DELETE FROM follows WHERE followerId = ? AND followedId = ?");
            $stmtDelete->execute([$followerId, $followedId]);

            if ($stmtDelete->rowCount() > 0) {
                // Update following/followers count
                $stmtDecFollowing = $pdo->prepare("UPDATE users SET following = GREATEST(following - 1, 0) WHERE id = ?");
                $stmtDecFollowing->execute([$followerId]);

                $stmtDecFollowers = $pdo->prepare("UPDATE users SET followers = GREATEST(followers - 1, 0) WHERE id = ?");
                $stmtDecFollowers->execute([$followedId]);
            }

            $pdo->commit();
            respondJson(["success" => true]);
        } catch (Exception $e) {
            $pdo->rollBack();
            respondError("Unfollow operation failed: " . $e->getMessage(), 500);
        }
        break;

    case 'is_following':
        $followerId = $input['followerId'] ?? '';
        $followedId = $input['followedId'] ?? '';

        $stmt = $pdo->prepare("SELECT 1 FROM follows WHERE followerId = ? AND followedId = ?");
        $stmt->execute([$followerId, $followedId]);
        respondJson(["following" => $stmt->fetch() ? true : false]);
        break;

    case 'get_followers':
        $userId = $input['userId'] ?? '';
        $stmt = $pdo->prepare("SELECT * FROM users WHERE id IN (SELECT followerId FROM follows WHERE followedId = ?)");
        $stmt->execute([$userId]);
        $followers = $stmt->fetchAll();

        foreach ($followers as &$user) {
            $user['followers'] = (int)$user['followers'];
            $user['following'] = (int)$user['following'];
            $user['balance'] = (double)$user['balance'];
            $user['authorIncome'] = (double)$user['authorIncome'];
            $user['readerCoins'] = (double)$user['readerCoins'];
            $user['totalReaderCoins'] = (double)$user['totalReaderCoins'];
            $user['referralCount'] = (int)$user['referralCount'];
            $user['isReferralRewardClaimed'] = (bool)$user['isReferralRewardClaimed'];
            $user['onboardingStep'] = (int)$user['onboardingStep'];
            $user['onboardingCompleted'] = (bool)$user['onboardingCompleted'];
            $user['isVerified'] = (bool)$user['isVerified'];
            $user['isAdFreePermanently'] = (bool)$user['isAdFreePermanently'];
        }
        respondJson($followers);
        break;

    case 'get_following':
        $userId = $input['userId'] ?? '';
        $stmt = $pdo->prepare("SELECT * FROM users WHERE id IN (SELECT followedId FROM follows WHERE followerId = ?)");
        $stmt->execute([$userId]);
        $following = $stmt->fetchAll();

        foreach ($following as &$user) {
            $user['followers'] = (int)$user['followers'];
            $user['following'] = (int)$user['following'];
            $user['balance'] = (double)$user['balance'];
            $user['authorIncome'] = (double)$user['authorIncome'];
            $user['readerCoins'] = (double)$user['readerCoins'];
            $user['totalReaderCoins'] = (double)$user['totalReaderCoins'];
            $user['referralCount'] = (int)$user['referralCount'];
            $user['isReferralRewardClaimed'] = (bool)$user['isReferralRewardClaimed'];
            $user['onboardingStep'] = (int)$user['onboardingStep'];
            $user['onboardingCompleted'] = (bool)$user['onboardingCompleted'];
            $user['isVerified'] = (bool)$user['isVerified'];
            $user['isAdFreePermanently'] = (bool)$user['isAdFreePermanently'];
        }
        respondJson($following);
        break;

    case 'send_message':
        $id = $input['id'] ?? '';
        $authorId = $input['authorId'] ?? '';
        $senderId = $input['senderId'] ?? '';
        $senderName = $input['senderName'] ?? '';
        $message = $input['message'] ?? '';
        $parentId = $input['parentId'] ?? null;
        $timestamp = (int)($input['timestamp'] ?? 0);

        if (empty($id) || empty($authorId) || empty($senderId) || empty($message)) {
            respondError("Missing message fields");
        }

        try {
            $pdo->beginTransaction();

            $stmtUser = $pdo->prepare("SELECT isVerified, profileImageUrl FROM users WHERE id = ?");
            $stmtUser->execute([$senderId]);
            $sender = $stmtUser->fetch();
            
            $isSenderVerified = 0;
            $profileImageUrl = null;
            if ($sender) {
                $isSenderVerified = (int)$sender['isVerified'];
                $profileImageUrl = $sender['profileImageUrl'];
            }
            if ($senderId === 'moneypad_official_id') {
                $isSenderVerified = 1;
            }

            // Insert conversation message
            $stmtInsert = $pdo->prepare("INSERT INTO conversations (id, authorId, senderId, senderName, message, senderProfileImageUrl, timestamp, parentId, isSenderVerified) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            $stmtInsert->execute([$id, $authorId, $senderId, $senderName, $message, $profileImageUrl, $timestamp, $parentId, $isSenderVerified]);

            // Notification updates
            if ($parentId === null) {
                if ($authorId === $senderId) {
                    // Posted on own wall: Notify followers
                    $stmtFoll = $pdo->prepare("SELECT followerId FROM follows WHERE followedId = ?");
                    $stmtFoll->execute([$senderId]);
                    $followers = $stmtFoll->fetchAll(PDO::FETCH_COLUMN);

                    foreach ($followers as $followerId) {
                        $nId = bin2hex(random_bytes(16));
                        $stmtNotif = $pdo->prepare("INSERT INTO notifications (id, userId, type, actorId, actorName, actorProfileImageUrl, storyId, partId, content, timestamp, isActorVerified) VALUES (?, ?, 'CONVERSATION', ?, ?, ?, ?, ?, ?, ?, ?)");
                        $stmtNotif->execute([$nId, $followerId, $senderId, $senderName, $profileImageUrl, $authorId, $id, $message, $timestamp, $isSenderVerified]);
                    }
                } else {
                    // Posted on other's wall: Notify wall owner
                    $nId = bin2hex(random_bytes(16));
                    $stmtNotif = $pdo->prepare("INSERT INTO notifications (id, userId, type, actorId, actorName, actorProfileImageUrl, storyId, partId, content, timestamp, isActorVerified) VALUES (?, ?, 'CONVERSATION', ?, ?, ?, ?, ?, ?, ?, ?)");
                    $stmtNotif->execute([$nId, $authorId, $senderId, $senderName, $profileImageUrl, $authorId, $id, $message, $timestamp, $isSenderVerified]);
                }
            } else {
                // Reply to a message
                if ($authorId !== $senderId) {
                    $nId = bin2hex(random_bytes(16));
                    $stmtNotif = $pdo->prepare("INSERT INTO notifications (id, userId, type, actorId, actorName, actorProfileImageUrl, storyId, partId, content, timestamp, isActorVerified) VALUES (?, ?, 'REPLY', ?, ?, ?, ?, ?, ?, ?, ?)");
                    $stmtNotif->execute([$nId, $authorId, $senderId, $senderName, $profileImageUrl, $authorId, $id, $message, $timestamp, $isSenderVerified]);
                }

                // Notify parent sender if not replier and not wall owner
                $stmtParent = $pdo->prepare("SELECT senderId FROM conversations WHERE id = ?");
                $stmtParent->execute([$parentId]);
                $parentSenderId = $stmtParent->fetchColumn();

                if ($parentSenderId && $parentSenderId !== $senderId && $parentSenderId !== $authorId) {
                    $nId = bin2hex(random_bytes(16));
                    $stmtNotif = $pdo->prepare("INSERT INTO notifications (id, userId, type, actorId, actorName, actorProfileImageUrl, storyId, partId, content, timestamp, isActorVerified) VALUES (?, ?, 'REPLY', ?, ?, ?, ?, ?, ?, ?, ?)");
                    $stmtNotif->execute([$nId, $parentSenderId, $senderId, $senderName, $profileImageUrl, $authorId, $id, $message, $timestamp, $isSenderVerified]);
                }
            }

            // Mentions notifications check
            preg_match_all('/@(\w+)/', $message, $mentions);
            if (!empty($mentions[1])) {
                $uniqueMentions = array_unique($mentions[1]);
                foreach ($uniqueMentions as $mentionedUsername) {
                    if ($mentionedUsername !== $senderName) {
                        $stmtMentioned = $pdo->prepare("SELECT id FROM users WHERE username = ?");
                        $stmtMentioned->execute([$mentionedUsername]);
                        $mId = $stmtMentioned->fetchColumn();

                        if ($mId) {
                            $nId = bin2hex(random_bytes(16));
                            $stmtNotif = $pdo->prepare("INSERT INTO notifications (id, userId, type, actorId, actorName, actorProfileImageUrl, storyId, partId, content, timestamp, isActorVerified) VALUES (?, ?, 'MENTION', ?, ?, ?, ?, ?, ?, ?, ?)");
                            $stmtNotif->execute([$nId, $mId, $senderId, $senderName, $profileImageUrl, $authorId, $id, $message, $timestamp, $isSenderVerified]);
                        }
                    }
                }
            }

            $pdo->commit();
            respondJson(["success" => true]);
        } catch (Exception $e) {
            $pdo->rollBack();
            respondError("Failed to send message: " . $e->getMessage(), 500);
        }
        break;

    case 'get_conversations':
        $authorId = $input['authorId'] ?? '';
        if (empty($authorId)) {
            respondError("Missing author ID");
        }

        $stmt = $pdo->prepare("SELECT * FROM conversations WHERE authorId = ? AND parentId IS NULL ORDER BY timestamp DESC");
        $stmt->execute([$authorId]);
        $conversations = $stmt->fetchAll();

        foreach ($conversations as &$c) {
            $c['timestamp'] = (int)$c['timestamp'];
            $c['isSenderVerified'] = (bool)$c['isSenderVerified'];
            $c['likes'] = (int)$c['likes'];
            $c['isLiked'] = (bool)$c['isLiked'];
        }
        respondJson($conversations);
        break;

    case 'get_replies':
        $parentId = $input['parentId'] ?? '';
        if (empty($parentId)) {
            respondError("Missing parent ID");
        }

        $stmt = $pdo->prepare("SELECT * FROM conversations WHERE parentId = ? ORDER BY timestamp ASC");
        $stmt->execute([$parentId]);
        $replies = $stmt->fetchAll();

        foreach ($replies as &$r) {
            $r['timestamp'] = (int)$r['timestamp'];
            $r['isSenderVerified'] = (bool)$r['isSenderVerified'];
            $r['likes'] = (int)$r['likes'];
            $r['isLiked'] = (bool)$r['isLiked'];
        }
        respondJson($replies);
        break;

    case 'toggle_conversation_like':
        $conversationId = $input['conversationId'] ?? '';
        $delta = (int)($input['delta'] ?? 0);
        $userId = $input['userId'] ?? '';

        if (empty($conversationId) || empty($userId)) {
            respondError("Missing conversation like parameters");
        }

        try {
            $pdo->beginTransaction();

            $isLiked = ($delta > 0) ? 1 : 0;
            $stmtUpdate = $pdo->prepare("UPDATE conversations SET likes = likes + ?, isLiked = ? WHERE id = ?");
            $stmtUpdate->execute([$delta, $isLiked, $conversationId]);

            // Notify conversation sender if liked
            if ($delta > 0) {
                $stmtConv = $pdo->prepare("SELECT senderId, authorId FROM conversations WHERE id = ?");
                $stmtConv->execute([$conversationId]);
                $conv = $stmtConv->fetch();

                if ($conv && $conv['senderId'] !== $userId) {
                    $stmtActor = $pdo->prepare("SELECT username, profileImageUrl, isVerified FROM users WHERE id = ?");
                    $stmtActor->execute([$userId]);
                    $actor = $stmtActor->fetch();

                    if ($actor) {
                        $nId = bin2hex(random_bytes(16));
                        $isVerified = (int)$actor['isVerified'];
                        if ($userId === 'moneypad_official_id') {
                            $isVerified = 1;
                        }

                        $stmtNotif = $pdo->prepare("INSERT INTO notifications (id, userId, type, actorId, actorName, actorProfileImageUrl, storyId, partId, isActorVerified) VALUES (?, ?, 'CONVERSATION_LIKE', ?, ?, ?, ?, ?, ?)");
                        $stmtNotif->execute([$nId, $conv['senderId'], $userId, $actor['username'], $actor['profileImageUrl'], $conv['authorId'], $conversationId, $isVerified]);
                    }
                }
            }

            $pdo->commit();
            respondJson(["success" => true]);
        } catch (Exception $e) {
            $pdo->rollBack();
            respondError("Toggle conversation like failed: " . $e->getMessage(), 500);
        }
        break;

    case 'add_review':
        $id = $input['id'] ?? '';
        $storyId = $input['storyId'] ?? '';
        $userId = $input['userId'] ?? '';
        $username = $input['username'] ?? '';
        $rating = (int)($input['rating'] ?? 0);
        $comment = $input['comment'] ?? '';
        $timestamp = (int)($input['timestamp'] ?? 0);

        if (empty($id) || empty($storyId) || empty($userId) || empty($username)) {
            respondError("Missing review fields");
        }

        $stmtUser = $pdo->prepare("SELECT profileImageUrl, isVerified FROM users WHERE id = ?");
        $stmtUser->execute([$userId]);
        $user = $stmtUser->fetch();
        
        $profileImageUrl = $user ? $user['profileImageUrl'] : null;
        $isUserVerified = $user ? (int)$user['isVerified'] : 0;
        if ($userId === 'moneypad_official_id') {
            $isUserVerified = 1;
        }

        $stmt = $pdo->prepare("INSERT INTO reviews (id, storyId, userId, username, userProfileImageUrl, rating, comment, timestamp, isUserVerified) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
        $stmt->execute([$id, $storyId, $userId, $username, $profileImageUrl, $rating, $comment, $timestamp, $isUserVerified]);
        respondJson(["success" => true]);
        break;

    case 'get_reviews':
        $storyId = $input['storyId'] ?? '';
        $stmt = $pdo->prepare("SELECT * FROM reviews WHERE storyId = ? ORDER BY timestamp DESC");
        $stmt->execute([$storyId]);
        $reviews = $stmt->fetchAll();

        foreach ($reviews as &$r) {
            $r['rating'] = (int)$r['rating'];
            $r['timestamp'] = (int)$r['timestamp'];
            $r['isUserVerified'] = (bool)$r['isUserVerified'];
        }
        respondJson($reviews);
        break;

    case 'has_reviewed':
        $storyId = $input['storyId'] ?? '';
        $userId = $input['userId'] ?? '';

        $stmt = $pdo->prepare("SELECT 1 FROM reviews WHERE storyId = ? AND userId = ?");
        $stmt->execute([$storyId, $userId]);
        respondJson(["reviewed" => $stmt->fetch() ? true : false]);
        break;

    case 'toggle_story_like':
        $userId = $input['userId'] ?? '';
        $storyId = $input['storyId'] ?? '';

        if (empty($userId) || empty($storyId)) {
            respondError("Missing required parameters for story like");
        }

        try {
            $pdo->beginTransaction();

            $stmtCheck = $pdo->prepare("SELECT 1 FROM user_story_likes WHERE userId = ? AND storyId = ?");
            $stmtCheck->execute([$userId, $storyId]);
            $alreadyLiked = $stmtCheck->fetchColumn();

            if ($alreadyLiked) {
                $stmtDel = $pdo->prepare("DELETE FROM user_story_likes WHERE userId = ? AND storyId = ?");
                $stmtDel->execute([$userId, $storyId]);
            } else {
                $stmtInsert = $pdo->prepare("INSERT INTO user_story_likes (userId, storyId) VALUES (?, ?)");
                $stmtInsert->execute([$userId, $storyId]);
            }

            // Sync like count to stories table
            $stmtCount = $pdo->prepare("SELECT COUNT(*) FROM user_story_likes WHERE storyId = ?");
            $stmtCount->execute([$storyId]);
            $likesCount = (int)$stmtCount->fetchColumn();

            $stmtStory = $pdo->prepare("UPDATE stories SET likes = ? WHERE id = ?");
            $stmtStory->execute([$likesCount, $storyId]);

            $pdo->commit();
            respondJson(["success" => true, "liked" => !$alreadyLiked]);
        } catch (Exception $e) {
            $pdo->rollBack();
            respondError("Story like toggle failed: " . $e->getMessage(), 500);
        }
        break;

    case 'is_story_liked':
        $userId = $input['userId'] ?? '';
        $storyId = $input['storyId'] ?? '';

        $stmt = $pdo->prepare("SELECT 1 FROM user_story_likes WHERE userId = ? AND storyId = ?");
        $stmt->execute([$userId, $storyId]);
        respondJson(["liked" => $stmt->fetch() ? true : false]);
        break;

    case 'add_annotation':
        $id = $input['id'] ?? '';
        $partId = $input['partId'] ?? '';
        $userId = $input['userId'] ?? '';
        $username = $input['username'] ?? '';
        $selectedText = $input['selectedText'] ?? '';
        $startIndex = (int)($input['startIndex'] ?? 0);
        $endIndex = (int)($input['endIndex'] ?? 0);
        $type = $input['type'] ?? '';
        $content = $input['content'] ?? null;
        $timestamp = (int)($input['timestamp'] ?? 0);

        if (empty($id) || empty($partId) || empty($userId) || empty($username) || empty($type)) {
            respondError("Missing annotation parameters");
        }

        try {
            $pdo->beginTransaction();

            $stmtUser = $pdo->prepare("SELECT isVerified FROM users WHERE id = ?");
            $stmtUser->execute([$userId]);
            $isUserVerified = (int)($stmtUser->fetchColumn() ?: 0);
            if ($userId === 'moneypad_official_id') {
                $isUserVerified = 1;
            }

            $stmt = $pdo->prepare("INSERT INTO part_annotations (id, partId, userId, username, selectedText, startIndex, endIndex, `type`, content, timestamp, isUserVerified) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            $stmt->execute([$id, $partId, $userId, $username, $selectedText, $startIndex, $endIndex, $type, $content, $timestamp, $isUserVerified]);

            // Sync commentsCount if type is comment
            if ($type === 'COMMENT') {
                $stmtPart = $pdo->prepare("SELECT storyId FROM story_parts WHERE id = ?");
                $stmtPart->execute([$partId]);
                $storyId = $stmtPart->fetchColumn();

                if ($storyId) {
                    $stmtCount = $pdo->prepare("UPDATE stories SET commentsCount = (
                        SELECT COUNT(*) FROM part_annotations 
                        WHERE partId IN (SELECT id FROM story_parts WHERE storyId = ?) 
                        AND type = 'COMMENT'
                    ) WHERE id = ?");
                    $stmtCount->execute([$storyId, $storyId]);
                }
            }

            $pdo->commit();
            respondJson(["success" => true]);
        } catch (Exception $e) {
            $pdo->rollBack();
            respondError("Add annotation failed: " . $e->getMessage(), 500);
        }
        break;

    case 'get_annotations':
        $partId = $input['partId'] ?? '';
        if (empty($partId)) {
            respondError("Missing part ID");
        }

        $stmt = $pdo->prepare("SELECT * FROM part_annotations WHERE partId = ? ORDER BY timestamp DESC");
        $stmt->execute([$partId]);
        $annotations = $stmt->fetchAll();

        foreach ($annotations as &$a) {
            $a['startIndex'] = (int)$a['startIndex'];
            $a['endIndex'] = (int)$a['endIndex'];
            $a['timestamp'] = (int)$a['timestamp'];
            $a['isUserVerified'] = (bool)$a['isUserVerified'];
        }
        respondJson($annotations);
        break;

    default:
        respondError("Invalid interactions action", 404);
}
