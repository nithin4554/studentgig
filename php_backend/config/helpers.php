<?php
// Function for CORS headers (API entry points)
function set_cors_headers() {
    // In production, tighten this to your frontend URL
    header("Access-Control-Allow-Origin: *");
    header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
    header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");
    header("Content-Type: application/json; charset=UTF-8");

    // Handle Pre-flight OPTIONS request
    if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
        http_response_code(200);
        exit();
    }
}

// Simple JSON Response helper
function send_response($code, $data) {
    http_response_code($code);
    echo json_encode($data);
    exit();
}

// Simple request extractor
function get_json_input() {
    return json_decode(file_get_contents("php://input"), true) ?: [];
}

// Placeholder for Token verification
function get_current_user_id() {
    $headers = getallheaders();
    $auth_header = isset($headers['Authorization']) ? $headers['Authorization'] : '';
    if (strpos($auth_header, 'Bearer ') === 0) {
        $token = substr($auth_header, 7);
        // Implement JWT decoding here (e.g. firebase/php-jwt)
        // For now, let's assume we decode the 'sub'
        // REAL IMPLEMENTATION REQUIRES PHP-JWT LIBRARY
        return null; 
    }
    return null;
}
?>
