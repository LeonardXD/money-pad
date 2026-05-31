<?php
// backend/upload.php

require_once __DIR__ . '/config/db.php';

// Allow POST requests only
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    respondError("Method not allowed", 405);
}

// Check if image file exists in files
if (!isset($_FILES['image'])) {
    respondError("No file uploaded");
}

$file = $_FILES['image'];

if ($file['error'] !== UPLOAD_ERR_OK) {
    respondError("Upload failed with error code: " . $file['error']);
}

// Validate file content type using Fileinfo extension
$allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
$finfo = finfo_open(FILEINFO_MIME_TYPE);
$mimeType = finfo_file($finfo, $file['tmp_name']);
finfo_close($finfo);

if (!in_array($mimeType, $allowedTypes)) {
    respondError("Invalid file type. Only JPEG, PNG, GIF, and WEBP images are allowed.");
}

// Create uploads directory if it doesn't exist
$uploadDir = __DIR__ . '/uploads/';
if (!file_exists($uploadDir)) {
    if (!mkdir($uploadDir, 0755, true)) {
        respondError("Failed to create uploads directory", 500);
    }
}

// Generate unique filename to avoid collision
$ext = pathinfo($file['name'], PATHINFO_EXTENSION);
if (empty($ext)) {
    $ext = ($mimeType === 'image/png') ? 'png' : 'jpg';
}
$filename = uniqid('img_', true) . '.' . $ext;
$targetFile = $uploadDir . $filename;

if (move_uploaded_file($file['tmp_name'], $targetFile)) {
    // Construct dynamic absolute URL
    $serverUrl = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http") . "://$_SERVER[HTTP_HOST]";
    $scriptPath = $_SERVER['SCRIPT_NAME'];
    $dirPath = dirname($scriptPath);
    $dirPath = rtrim($dirPath, '/');
    $absoluteUrl = $serverUrl . $dirPath . '/uploads/' . $filename;

    respondJson([
        "success" => true,
        "url" => $absoluteUrl
    ]);
} else {
    respondError("Failed to save uploaded file", 500);
}
