<?php
define('DB_HOST', 'localhost');
define('DB_USER', 'root');
define('DB_PASS', 'root');
define('DB_NAME', 'moneypad');

// Set content type to JSON globally
header('Content-Type: application/json; charset=utf-8');

try {
    $pdo = new PDO("mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=utf8mb4", DB_USER, DB_PASS, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES => false,
    ]);
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(["error" => "Database connection failed: " . $e->getMessage()]);
    exit;
}

// Utility function to get raw POST body as JSON array
function getJsonInput() {
    $raw = file_get_contents('php://input');
    $decoded = json_decode($raw, true);
    return is_array($decoded) ? $decoded : [];
}

// Utility function to handle API response and exit
function respondJson($data, $statusCode = 200) {
    http_response_code($statusCode);
    echo json_encode($data);
    exit;
}

// Utility function to handle error responses
function respondError($message, $statusCode = 400) {
    respondJson(["error" => $message], $statusCode);
}

// Helper to create notifications for followers of an author
function notifyFollowersServer($pdo, $authorId, $type, $storyId, $storyTitle, $partId = null, $partTitle = null, $timestamp = null) {
    if ($timestamp === null || $timestamp === 0) {
        $timestamp = round(microtime(true) * 1000);
    }
    
    // Get author details
    $stmtUser = $pdo->prepare("SELECT username, isVerified, profileImageUrl FROM users WHERE id = ?");
    $stmtUser->execute([$authorId]);
    $author = $stmtUser->fetch();
    if (!$author) return;
    
    $authorName = $author['username'];
    $isAuthorVerified = (int)$author['isVerified'];
    $authorProfileImageUrl = $author['profileImageUrl'];
    if ($authorId === 'moneypad_official_id') {
        $isAuthorVerified = 1;
    }
    
    // Get followers
    $stmtFoll = $pdo->prepare("SELECT followerId FROM follows WHERE followedId = ?");
    $stmtFoll->execute([$authorId]);
    $followers = $stmtFoll->fetchAll(PDO::FETCH_COLUMN);
    
    if (empty($followers)) return;
    
    // Insert notifications
    $stmtNotif = $pdo->prepare("INSERT INTO notifications (id, userId, type, actorId, actorName, actorProfileImageUrl, storyId, storyTitle, partId, partTitle, content, timestamp, isRead, isActorVerified) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?)");
    
    $content = null;
    
    foreach ($followers as $followerId) {
        $nId = bin2hex(random_bytes(16));
        $stmtNotif->execute([
            $nId,
            $followerId,
            $type,
            $authorId,
            $authorName,
            $authorProfileImageUrl,
            $storyId,
            $storyTitle,
            $partId,
            $partTitle,
            $content,
            $timestamp,
            $isAuthorVerified
        ]);
    }
}
