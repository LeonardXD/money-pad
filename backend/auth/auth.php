<?php
require_once __DIR__ . '/../config/db.php';

$action = $_GET['action'] ?? '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $input = getJsonInput();
} else {
    $input = $_GET;
}

switch ($action) {
    case 'login':
        $identifier = $input['identifier'] ?? '';
        $password = $input['password'] ?? '';

        if (empty($identifier) || empty($password)) {
            respondError("Missing login credentials");
        }

        // Regular user login

        $stmt = $pdo->prepare("SELECT * FROM users WHERE (username = ? OR email = ?) AND password = ?");
        $stmt->execute([$identifier, $identifier, $password]);
        $user = $stmt->fetch();

        if ($user) {
            $now = round(microtime(true) * 1000);
            $stmtUpdate = $pdo->prepare("UPDATE users SET loginTimestamp = ? WHERE id = ?");
            $stmtUpdate->execute([$now, $user['id']]);
            $user['loginTimestamp'] = $now;

            // PHP maps integers and booleans as strings/ints. Let's cast values to ensure type matching on Android side.
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

            respondJson($user);
        } else {
            respondError("Invalid username or password");
        }
        break;

    case 'signup':
        $username = trim($input['username'] ?? '');
        $email = trim($input['email'] ?? '');
        $password = $input['password'] ?? '';
        $referrerUsername = trim($input['referrerUsername'] ?? '');

        if (empty($username) || empty($email) || empty($password)) {
            respondError("Username, email, and password are required");
        }

        // Check if username taken
        $stmt = $pdo->prepare("SELECT 1 FROM users WHERE username = ?");
        $stmt->execute([$username]);
        if ($stmt->fetch()) {
            respondError("Username already taken");
        }

        // Check if email taken
        $stmt = $pdo->prepare("SELECT 1 FROM users WHERE email = ?");
        $stmt->execute([$email]);
        if ($stmt->fetch()) {
            respondError("Email already registered");
        }

        // Validate referrer if provided
        $validatedReferrer = "";
        if (!empty($referrerUsername)) {
            $stmt = $pdo->prepare("SELECT 1 FROM users WHERE username = ?");
            $stmt->execute([$referrerUsername]);
            if (!$stmt->fetch()) {
                respondError("Referrer username not found");
            }
            $validatedReferrer = $referrerUsername;
        }

        $userId = bin2hex(random_bytes(16)); // UUID representation
        $now = round(microtime(true) * 1000);

        try {
            $pdo->beginTransaction();

            // 1. Insert user
            $stmt = $pdo->prepare("INSERT INTO users (id, username, email, password, referredBy, signupTimestamp, loginTimestamp) VALUES (?, ?, ?, ?, ?, ?, ?)");
            $stmt->execute([$userId, $username, $email, $password, $validatedReferrer, $now, $now]);

            // 2. Welcome notification
            $notifId = bin2hex(random_bytes(16));
            $stmtNotif = $pdo->prepare("INSERT INTO notifications (id, userId, type, actorId, actorName, isActorVerified) VALUES (?, ?, 'WELCOME', 'SYSTEM', 'Money Pad Team', 1)");
            $stmtNotif->execute([$notifId, $userId]);

            // 3. Announcements sync (conversations posted on official user wall)
            $stmtAnnounce = $pdo->prepare("SELECT * FROM conversations WHERE authorId = 'moneypad_official_id' AND parentId IS NULL");
            $stmtAnnounce->execute();
            $announcements = $stmtAnnounce->fetchAll();

            foreach ($announcements as $announcement) {
                $aNotifId = bin2hex(random_bytes(16));
                $stmtInsertAnnounceNotif = $pdo->prepare("INSERT INTO notifications (id, userId, type, actorId, actorName, storyId, partId, content, timestamp, isActorVerified) VALUES (?, ?, 'CONVERSATION', 'moneypad_official_id', 'moneypad', 'moneypad_official_id', ?, ?, ?, 1)");
                $stmtInsertAnnounceNotif->execute([$aNotifId, $userId, $announcement['id'], $announcement['message'], $announcement['timestamp']]);
            }

            // 4. Referrer increment referral count
            if (!empty($validatedReferrer)) {
                $stmtRef = $pdo->prepare("UPDATE users SET referralCount = referralCount + 1 WHERE username = ?");
                $stmtRef->execute([$validatedReferrer]);
            }

            // 5. Auto-follow official account
            $stmtFollow = $pdo->prepare("INSERT IGNORE INTO follows (followerId, followedId) VALUES (?, 'moneypad_official_id')");
            $stmtFollow->execute([$userId]);

            // Update followers count of official account (ensure official account exists first)
            $stmtEnsureOfficial = $pdo->prepare("SELECT 1 FROM users WHERE id = 'moneypad_official_id'");
            $stmtEnsureOfficial->execute();
            if ($stmtEnsureOfficial->fetch()) {
                $stmtOfficialFollowers = $pdo->prepare("UPDATE users SET followers = followers + 1 WHERE id = 'moneypad_official_id'");
                $stmtOfficialFollowers->execute();
            }

            // Update user following count
            $stmtUserFollowing = $pdo->prepare("UPDATE users SET following = 1 WHERE id = ?");
            $stmtUserFollowing->execute([$userId]);

            $pdo->commit();

            // Fetch created user
            $stmtFetch = $pdo->prepare("SELECT * FROM users WHERE id = ?");
            $stmtFetch->execute([$userId]);
            $user = $stmtFetch->fetch();

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

            respondJson($user);

        } catch (Exception $e) {
            $pdo->rollBack();
            respondError("Signup transaction failed: " . $e->getMessage(), 500);
        }
        break;

    case 'check_username':
        $username = trim($input['username'] ?? '');
        if (empty($username)) {
            respondJson(["taken" => false]);
        }
        $stmt = $pdo->prepare("SELECT 1 FROM users WHERE username = ?");
        $stmt->execute([$username]);
        respondJson(["taken" => $stmt->fetch() ? true : false]);
        break;

    case 'check_email':
        $email = trim($input['email'] ?? '');
        if (empty($email)) {
            respondJson(["taken" => false]);
        }
        $stmt = $pdo->prepare("SELECT 1 FROM users WHERE email = ?");
        $stmt->execute([$email]);
        respondJson(["taken" => $stmt->fetch() ? true : false]);
        break;

    case 'change_password':
        $userId = $input['userId'] ?? '';
        $currentPassword = $input['currentPassword'] ?? '';
        $newPassword = $input['newPassword'] ?? '';

        if (empty($userId) || empty($currentPassword) || empty($newPassword)) {
            respondError("Missing required password details");
        }

        $stmt = $pdo->prepare("SELECT password FROM users WHERE id = ?");
        $stmt->execute([$userId]);
        $stored = $stmt->fetchColumn();

        if ($stored !== $currentPassword) {
            respondError("Current password is incorrect");
        }

        $stmtUpdate = $pdo->prepare("UPDATE users SET password = ? WHERE id = ?");
        $stmtUpdate->execute([$newPassword, $userId]);
        respondJson(["success" => true]);
        break;

    default:
        respondError("Invalid authentication action", 404);
}
