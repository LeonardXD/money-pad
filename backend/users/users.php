<?php
require_once __DIR__ . '/../config/db.php';

$action = $_GET['action'] ?? '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $input = getJsonInput();
} else {
    $input = $_GET;
}

switch ($action) {
    case 'get_user':
        $userId = $input['userId'] ?? '';
        if (empty($userId)) {
            respondError("Missing user identifier");
        }

        // Try getting by ID first
        $stmt = $pdo->prepare("SELECT * FROM users WHERE id = ?");
        $stmt->execute([$userId]);
        $user = $stmt->fetch();

        // If not found, try getting by username
        if (!$user) {
            $stmt = $pdo->prepare("SELECT * FROM users WHERE username = ?");
            $stmt->execute([$userId]);
            $user = $stmt->fetch();
        }

        if ($user) {
            // Type casts
            $user['followers'] = (int)$user['followers'];
            $user['following'] = (int)$user['following'];
            $user['balance'] = (double)$user['balance'];
            $user['authorIncome'] = (double)$user['authorIncome'];
            $user['readerCoins'] = (int)$user['readerCoins'];
            $user['totalReaderCoins'] = (int)$user['totalReaderCoins'];
            $user['referralCount'] = (int)$user['referralCount'];
            $user['isReferralRewardClaimed'] = (bool)$user['isReferralRewardClaimed'];
            $user['onboardingStep'] = (int)$user['onboardingStep'];
            $user['onboardingCompleted'] = (bool)$user['onboardingCompleted'];
            $user['isVerified'] = (bool)$user['isVerified'];
            $user['isAdFreePermanently'] = (bool)$user['isAdFreePermanently'];
            respondJson($user);
        } else {
            respondError("User not found", 404);
        }
        break;

    case 'update_profile':
        $userId = $input['userId'] ?? '';
        $bio = $input['bio'] ?? '';
        $profileImageUrl = $input['profileImageUrl'] ?? null;
        $coverImageUrl = $input['coverImageUrl'] ?? null;

        if (empty($userId)) {
            respondError("Missing user ID");
        }

        try {
            $pdo->beginTransaction();

            $stmt = $pdo->prepare("UPDATE users SET bio = ?, profileImageUrl = ?, coverImageUrl = ? WHERE id = ?");
            $stmt->execute([$bio, $profileImageUrl, $coverImageUrl, $userId]);

            // Sync user details across other tables
            $stmtUser = $pdo->prepare("SELECT username, isVerified FROM users WHERE id = ?");
            $stmtUser->execute([$userId]);
            $user = $stmtUser->fetch();

            if ($user) {
                $username = $user['username'];
                $isVerified = (int)$user['isVerified'];

                $stmtConv = $pdo->prepare("UPDATE conversations SET senderProfileImageUrl = ?, senderName = ?, isSenderVerified = ? WHERE senderId = ?");
                $stmtConv->execute([$profileImageUrl, $username, $isVerified, $userId]);

                $stmtNotif = $pdo->prepare("UPDATE notifications SET actorProfileImageUrl = ?, actorName = ?, isActorVerified = ? WHERE actorId = ?");
                $stmtNotif->execute([$profileImageUrl, $username, $isVerified, $userId]);

                $stmtReview = $pdo->prepare("UPDATE reviews SET username = ?, userProfileImageUrl = ?, isUserVerified = ? WHERE userId = ?");
                $stmtReview->execute([$username, $profileImageUrl, $isVerified, $userId]);

                $stmtAnnot = $pdo->prepare("UPDATE part_annotations SET username = ?, isUserVerified = ? WHERE userId = ?");
                $stmtAnnot->execute([$username, $isVerified, $userId]);

                $stmtStory = $pdo->prepare("UPDATE stories SET authorName = ?, isAuthorVerified = ? WHERE authorId = ?");
                $stmtStory->execute([$username, $isVerified, $userId]);
            }

            $pdo->commit();
            respondJson(["success" => true]);
        } catch (Exception $e) {
            $pdo->rollBack();
            respondError("Profile update failed: " . $e->getMessage(), 500);
        }
        break;

    case 'update_settings':
        $userId = $input['userId'] ?? '';
        $username = trim($input['username'] ?? '');
        $birthday = trim($input['birthday'] ?? '');
        $gender = trim($input['gender'] ?? '');
        $preferredGenres = trim($input['preferredGenres'] ?? '');

        if (empty($userId) || empty($username)) {
            respondError("Missing required parameters");
        }

        // Validate username uniqueness
        $stmt = $pdo->prepare("SELECT id FROM users WHERE username = ? AND id != ?");
        $stmt->execute([$username, $userId]);
        if ($stmt->fetch()) {
            respondError("Username already taken");
        }

        try {
            $pdo->beginTransaction();

            $stmt = $pdo->prepare("UPDATE users SET username = ?, birthday = ?, gender = ?, preferredGenres = ? WHERE id = ?");
            $stmt->execute([$username, $birthday, $gender, $preferredGenres, $userId]);

            // Sync user details across other tables
            $stmtUser = $pdo->prepare("SELECT profileImageUrl, isVerified FROM users WHERE id = ?");
            $stmtUser->execute([$userId]);
            $user = $stmtUser->fetch();

            if ($user) {
                $profileImageUrl = $user['profileImageUrl'];
                $isVerified = (int)$user['isVerified'];

                $stmtConv = $pdo->prepare("UPDATE conversations SET senderProfileImageUrl = ?, senderName = ?, isSenderVerified = ? WHERE senderId = ?");
                $stmtConv->execute([$profileImageUrl, $username, $isVerified, $userId]);

                $stmtNotif = $pdo->prepare("UPDATE notifications SET actorProfileImageUrl = ?, actorName = ?, isActorVerified = ? WHERE actorId = ?");
                $stmtNotif->execute([$profileImageUrl, $username, $isVerified, $userId]);

                $stmtReview = $pdo->prepare("UPDATE reviews SET username = ?, userProfileImageUrl = ?, isUserVerified = ? WHERE userId = ?");
                $stmtReview->execute([$username, $profileImageUrl, $isVerified, $userId]);

                $stmtAnnot = $pdo->prepare("UPDATE part_annotations SET username = ?, isUserVerified = ? WHERE userId = ?");
                $stmtAnnot->execute([$username, $isVerified, $userId]);

                $stmtStory = $pdo->prepare("UPDATE stories SET authorName = ?, isAuthorVerified = ? WHERE authorId = ?");
                $stmtStory->execute([$username, $isVerified, $userId]);
            }

            $pdo->commit();
            respondJson(["success" => true]);
        } catch (Exception $e) {
            $pdo->rollBack();
            respondError("Settings update failed: " . $e->getMessage(), 500);
        }
        break;

    case 'onboarding_gender':
        $userId = $input['userId'] ?? '';
        $gender = trim($input['gender'] ?? '');

        if (empty($userId) || empty($gender)) {
            respondError("Choose a gender to continue");
        }

        $stmt = $pdo->prepare("UPDATE users SET gender = ?, onboardingStep = GREATEST(onboardingStep, 2) WHERE id = ?");
        $stmt->execute([$gender, $userId]);
        respondJson(["success" => true]);
        break;

    case 'onboarding_birthday':
        $userId = $input['userId'] ?? '';
        $birthday = trim($input['birthday'] ?? '');

        if (empty($userId) || empty($birthday)) {
            respondError("Choose your birthday to continue");
        }

        $stmt = $pdo->prepare("UPDATE users SET birthday = ?, onboardingStep = GREATEST(onboardingStep, 3) WHERE id = ?");
        $stmt->execute([$birthday, $userId]);
        respondJson(["success" => true]);
        break;

    case 'onboarding_genres':
        $userId = $input['userId'] ?? '';
        $genres = trim($input['genres'] ?? '');

        if (empty($userId)) {
            respondError("Missing user ID");
        }

        $stmt = $pdo->prepare("UPDATE users SET preferredGenres = ? WHERE id = ?");
        $stmt->execute([$genres, $userId]);
        respondJson(["success" => true]);
        break;

    case 'complete_onboarding':
        $userId = $input['userId'] ?? '';
        if (empty($userId)) {
            respondError("Missing user ID");
        }

        $stmt = $pdo->prepare("UPDATE users SET onboardingCompleted = 1, onboardingStep = 3 WHERE id = ?");
        $stmt->execute([$userId]);
        respondJson(["success" => true]);
        break;

    case 'search_authors':
        $query = $input['query'] ?? '';
        $excludeUserId = $input['excludeUserId'] ?? '';

        $stmt = $pdo->prepare("SELECT * FROM users WHERE username LIKE ? AND id != ? ORDER BY isVerified DESC");
        $stmt->execute(["%" . $query . "%", $excludeUserId]);
        $authors = $stmt->fetchAll();

        // Type casts
        foreach ($authors as &$author) {
            $author['followers'] = (int)$author['followers'];
            $author['following'] = (int)$author['following'];
            $author['balance'] = (double)$author['balance'];
            $author['authorIncome'] = (double)$author['authorIncome'];
            $author['readerCoins'] = (int)$author['readerCoins'];
            $author['totalReaderCoins'] = (int)$author['totalReaderCoins'];
            $author['referralCount'] = (int)$author['referralCount'];
            $author['isReferralRewardClaimed'] = (bool)$author['isReferralRewardClaimed'];
            $author['onboardingStep'] = (int)$author['onboardingStep'];
            $author['onboardingCompleted'] = (bool)$author['onboardingCompleted'];
            $author['isVerified'] = (bool)$author['isVerified'];
            $author['isAdFreePermanently'] = (bool)$author['isAdFreePermanently'];
        }

        respondJson($authors);
        break;

    case 'verify_user':
        $userId = $input['userId'] ?? '';
        if (empty($userId)) {
            respondError("Missing user ID");
        }

        try {
            $pdo->beginTransaction();

            $stmt = $pdo->prepare("UPDATE users SET isVerified = 1 WHERE id = ?");
            $stmt->execute([$userId]);

            $stmtUser = $pdo->prepare("SELECT username, profileImageUrl FROM users WHERE id = ?");
            $stmtUser->execute([$userId]);
            $user = $stmtUser->fetch();

            if ($user) {
                $username = $user['username'];
                $profileImageUrl = $user['profileImageUrl'];

                $stmtConv = $pdo->prepare("UPDATE conversations SET isSenderVerified = 1, senderName = ?, senderProfileImageUrl = ? WHERE senderId = ?");
                $stmtConv->execute([$username, $profileImageUrl, $userId]);

                $stmtNotif = $pdo->prepare("UPDATE notifications SET isActorVerified = 1, actorName = ?, actorProfileImageUrl = ? WHERE actorId = ?");
                $stmtNotif->execute([$username, $profileImageUrl, $userId]);

                $stmtReview = $pdo->prepare("UPDATE reviews SET isUserVerified = 1, username = ?, userProfileImageUrl = ? WHERE userId = ?");
                $stmtReview->execute([$username, $profileImageUrl, $userId]);

                $stmtAnnot = $pdo->prepare("UPDATE part_annotations SET isUserVerified = 1, username = ? WHERE userId = ?");
                $stmtAnnot->execute([$username, $userId]);

                $stmtStory = $pdo->prepare("UPDATE stories SET isAuthorVerified = 1, authorName = ? WHERE authorId = ?");
                $stmtStory->execute([$username, $userId]);
            }

            $pdo->commit();
            respondJson(["success" => true]);
        } catch (Exception $e) {
            $pdo->rollBack();
            respondError("Verification failed: " . $e->getMessage(), 500);
        }
        break;

    case 'update_ad_free':
        $userId = $input['userId'] ?? '';
        $timestamp = (int)($input['timestamp'] ?? 0);
        $permanent = (bool)($input['permanent'] ?? false);

        if (empty($userId)) {
            respondError("Missing user ID");
        }

        if ($permanent) {
            $stmtUser = $pdo->prepare("SELECT balance FROM users WHERE id = ?");
            $stmtUser->execute([$userId]);
            $currentBalance = (double)($stmtUser->fetchColumn() ?: 0.0);

            if ($currentBalance < 1499.00) {
                respondError("Insufficient balance to upgrade");
            }

            try {
                $pdo->beginTransaction();

                $stmt = $pdo->prepare("UPDATE users SET isAdFreePermanently = 1, balance = balance - 1499.00 WHERE id = ?");
                $stmt->execute([$userId]);

                $txId = bin2hex(random_bytes(16));
                $now = round(microtime(true) * 1000);
                $stmtTx = $pdo->prepare("INSERT INTO transactions (id, userId, amount, method, accountInfo, source, timestamp, status) VALUES (?, ?, 1499.00, 'Ad-Free Upgrade', 'Permanent', 'REFERRAL', ?, 'Completed')");
                $stmtTx->execute([$txId, $userId, $now]);

                $pdo->commit();
            } catch (Exception $e) {
                $pdo->rollBack();
                respondError("Failed to purchase permanent ad-free: " . $e->getMessage(), 500);
            }
        } else {
            $stmt = $pdo->prepare("UPDATE users SET adFreeUntil = ? WHERE id = ?");
            $stmt->execute([$timestamp, $userId]);
        }
        respondJson(["success" => true]);
        break;

    default:
        respondError("Invalid users action", 404);
}
