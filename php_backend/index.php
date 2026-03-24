<?php
require_once __DIR__ . '/config/database.php';
require_once __DIR__ . '/config/helpers.php';

set_cors_headers();

$request_uri = $_SERVER['REQUEST_URI'];
$base_path = '/php_backend'; // Adjust based on your folder structure

// Simple manual router
if (strpos($request_uri, $base_path . '/api/jobs') === 0) {
    require_once __DIR__ . '/api/jobs.php';
} else {
    send_response(404, ['detail' => 'Endpoint not found in PHP backend']);
}
?>
