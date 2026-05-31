<?php
require_once __DIR__ . '/../config/db.php';

$action = $_GET['action'] ?? '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $input = getJsonInput();
} else {
    $input = $_GET;
}

switch ($action) {
    case 'create_notification':
        $id = $input['id'] ?? '';
        $userId = $input['userId'] ?? '';
        $type = $input['type'] ?? '';
        $actorId = $input['actorId'] ?? '';
        $actorName = $input['actorName'] ?? '';
        $actorProfileImageUrl = $input['actorProfileImageUrl'] ?? null;
        $storyId = $input['storyId'] ?? null;
        $storyTitle = $input['storyTitle'] ?? null;
        $partId = $input['partId'] ?? null;
        $partTitle = $input['partTitle'] ?? null;
        $content = $input['content'] ?? null;
        $timestamp = (int)($input['timestamp'] ?? 0);
        $isActorVerified = (int)($input['isActorVerified'] ?? 0);

        if (empty($id) || empty($userId) || empty($type) || empty($actorId) || empty($actorName)) {
            respondError("Missing required notification fields");
        }

        $stmt = $pdo->prepare("INSERT INTO notifications (id, userId, type, actorId, actorName, actorProfileImageUrl, storyId, storyTitle, partId, partTitle, content, timestamp, isRead, isActorVerified) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?)");
        $stmt->execute([$id, $userId, $type, $actorId, $actorName, $actorProfileImageUrl, $storyId, $storyTitle, $partId, $partTitle, $content, $timestamp, $isActorVerified]);
        respondJson(["success" => true]);
        break;

    case 'list_notifications':
        $userId = $input['userId'] ?? '';
        if (empty($userId)) {
            respondError("Missing user ID");
        }

        $stmt = $pdo->prepare("SELECT * FROM notifications WHERE userId = ? ORDER BY timestamp DESC");
        $stmt->execute([$userId]);
        $notifications = $stmt->fetchAll();

        foreach ($notifications as &$n) {
            $n['timestamp'] = (int)$n['timestamp'];
            $n['isRead'] = (bool)$n['isRead'];
            $n['isActorVerified'] = (bool)$n['isActorVerified'];
        }
        respondJson($notifications);
        break;

    case 'get_unread_count':
        $userId = $input['userId'] ?? '';
        if (empty($userId)) {
            respondError("Missing user ID");
        }

        $stmt = $pdo->prepare("SELECT COUNT(*) FROM notifications WHERE userId = ? AND isRead = 0");
        $stmt->execute([$userId]);
        $count = (int)$stmt->fetchColumn();

        respondJson(["count" => $count]);
        break;

    case 'mark_read':
        $notificationId = $input['notificationId'] ?? '';
        if (empty($notificationId)) {
            respondError("Missing notification ID");
        }

        $stmt = $pdo->prepare("UPDATE notifications SET isRead = 1 WHERE id = ?");
        $stmt->execute([$notificationId]);
        respondJson(["success" => true]);
        break;

    case 'mark_all_read':
        $userId = $input['userId'] ?? '';
        if (empty($userId)) {
            respondError("Missing user ID");
        }

        $stmt = $pdo->prepare("UPDATE notifications SET isRead = 1 WHERE userId = ?");
        $stmt->execute([$userId]);
        respondJson(["success" => true]);
        break;

    default:
        respondError("Invalid notifications action", 404);
}
