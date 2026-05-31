<?php
require_once __DIR__ . '/../config/db.php';

$action = $_GET['action'] ?? '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $input = getJsonInput();
} else {
    $input = $_GET;
}

switch ($action) {
    case 'add_library':
        $userId = $input['userId'] ?? '';
        $storyId = $input['storyId'] ?? '';
        $downloadedAt = (int)($input['downloadedAt'] ?? 0);

        if (empty($userId) || empty($storyId)) {
            respondError("Missing library parameters");
        }

        // Limit library count to 15 per user
        $stmtCount = $pdo->prepare("SELECT COUNT(*) FROM library_stories WHERE userId = ?");
        $stmtCount->execute([$userId]);
        $count = (int)$stmtCount->fetchColumn();

        if ($count >= 15) {
            respondError("Library is full. Please remove a story before downloading again.");
        }

        $stmt = $pdo->prepare("INSERT IGNORE INTO library_stories (userId, storyId, downloadedAt) VALUES (?, ?, ?)");
        $stmt->execute([$userId, $storyId, $downloadedAt]);
        respondJson(["success" => true]);
        break;

    case 'remove_library':
        $userId = $input['userId'] ?? '';
        $storyId = $input['storyId'] ?? '';

        if (empty($userId) || empty($storyId)) {
            respondError("Missing library parameters");
        }

        $stmt = $pdo->prepare("DELETE FROM library_stories WHERE userId = ? AND storyId = ?");
        $stmt->execute([$userId, $storyId]);
        respondJson(["success" => true]);
        break;

    case 'get_library':
        $userId = $input['userId'] ?? '';
        if (empty($userId)) {
            respondError("Missing user ID");
        }

        $stmt = $pdo->prepare("SELECT * FROM stories WHERE id IN (SELECT storyId FROM library_stories WHERE userId = ? ORDER BY downloadedAt DESC)");
        $stmt->execute([$userId]);
        $stories = $stmt->fetchAll();

        foreach ($stories as &$story) {
            $story['readCount'] = (int)$story['readCount'];
            $story['isPublished'] = (bool)$story['isPublished'];
            $story['isCompleted'] = (bool)$story['isCompleted'];
            $story['isMature'] = (bool)$story['isMature'];
            $story['likes'] = (int)$story['likes'];
            $story['commentsCount'] = (int)$story['commentsCount'];
            $story['uniqueViews'] = (int)$story['uniqueViews'];
            $story['repeatedViews'] = (int)$story['repeatedViews'];
            $story['isAuthorVerified'] = (bool)$story['isAuthorVerified'];
        }
        respondJson($stories);
        break;

    case 'create_reading_list':
        $id = $input['id'] ?? '';
        $userId = $input['userId'] ?? '';
        $name = trim($input['name'] ?? '');
        $description = trim($input['description'] ?? '');
        $createdAt = (int)($input['createdAt'] ?? 0);

        if (empty($id) || empty($userId) || empty($name)) {
            respondError("Missing reading list fields");
        }

        $stmt = $pdo->prepare("INSERT INTO reading_lists (id, name, description, userId, createdAt) VALUES (?, ?, ?, ?, ?)");
        $stmt->execute([$id, $name, $description, $userId, $createdAt]);
        respondJson(["success" => true]);
        break;

    case 'delete_reading_list':
        $listId = $input['listId'] ?? '';
        if (empty($listId)) {
            respondError("Missing reading list ID");
        }

        try {
            $pdo->beginTransaction();

            $stmt = $pdo->prepare("DELETE FROM reading_lists WHERE id = ?");
            $stmt->execute([$listId]);

            $stmtStories = $pdo->prepare("DELETE FROM reading_list_stories WHERE listId = ?");
            $stmtStories->execute([$listId]);

            $pdo->commit();
            respondJson(["success" => true]);
        } catch (Exception $e) {
            $pdo->rollBack();
            respondError("Delete reading list failed: " . $e->getMessage(), 500);
        }
        break;

    case 'get_reading_lists':
        $userId = $input['userId'] ?? '';
        if (empty($userId)) {
            respondError("Missing user ID");
        }

        $stmt = $pdo->prepare("SELECT * FROM reading_lists WHERE userId = ? ORDER BY createdAt DESC");
        $stmt->execute([$userId]);
        $lists = $stmt->fetchAll();

        foreach ($lists as &$list) {
            $list['createdAt'] = (int)$list['createdAt'];
        }
        respondJson($lists);
        break;

    case 'add_to_reading_list':
        $listId = $input['listId'] ?? '';
        $storyId = $input['storyId'] ?? '';
        $addedAt = (int)($input['addedAt'] ?? 0);

        if (empty($listId) || empty($storyId)) {
            respondError("Missing reading list details");
        }

        $stmt = $pdo->prepare("INSERT IGNORE INTO reading_list_stories (listId, storyId, addedAt) VALUES (?, ?, ?)");
        $stmt->execute([$listId, $storyId, $addedAt]);
        respondJson(["success" => true]);
        break;

    case 'remove_from_reading_list':
        $listId = $input['listId'] ?? '';
        $storyId = $input['storyId'] ?? '';

        if (empty($listId) || empty($storyId)) {
            respondError("Missing reading list details");
        }

        $stmt = $pdo->prepare("DELETE FROM reading_list_stories WHERE listId = ? AND storyId = ?");
        $stmt->execute([$listId, $storyId]);
        respondJson(["success" => true]);
        break;

    case 'get_reading_list_stories':
        $listId = $input['listId'] ?? '';
        if (empty($listId)) {
            respondError("Missing reading list ID");
        }

        $stmt = $pdo->prepare("SELECT * FROM stories WHERE id IN (SELECT storyId FROM reading_list_stories WHERE listId = ? ORDER BY addedAt DESC)");
        $stmt->execute([$listId]);
        $stories = $stmt->fetchAll();

        foreach ($stories as &$story) {
            $story['readCount'] = (int)$story['readCount'];
            $story['isPublished'] = (bool)$story['isPublished'];
            $story['isCompleted'] = (bool)$story['isCompleted'];
            $story['isMature'] = (bool)$story['isMature'];
            $story['likes'] = (int)$story['likes'];
            $story['commentsCount'] = (int)$story['commentsCount'];
            $story['uniqueViews'] = (int)$story['uniqueViews'];
            $story['repeatedViews'] = (int)$story['repeatedViews'];
            $story['isAuthorVerified'] = (bool)$story['isAuthorVerified'];
        }
        respondJson($stories);
        break;

    default:
        respondError("Invalid library action", 404);
}
