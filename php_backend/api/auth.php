<?php
require_once dirname(__DIR__) . '/config/database.php';
require_once dirname(__DIR__) . '/config/helpers.php';

set_cors_headers();

// POST /api/login equivalent
if ($_SERVER['REQUEST_METHOD'] == 'POST' && strpos($_SERVER['REQUEST_URI'], '/api/login') !== false) {
    $input = get_json_input();
    $phone = isset($input['phone']) ? $input['phone'] : '';
    $password = isset($input['password']) ? $input['password'] : '';

    if (!$phone || !$password) {
        send_response(400, ['detail' => 'Phone and password required']);
    }

    $stmt = $pdo->prepare("SELECT * FROM users WHERE phone = ?");
    $stmt->execute([$phone]);
    $user = $stmt->fetch();

    if ($user && password_verify($password, $user['hashed_password'])) {
        // In a real app, generate JWT here
        send_response(200, [
            'access_token' => 'dummy_token_for_now',
            'token_type' => 'bearer',
            'user' => [
                'id' => (int)$user['id'],
                'phone' => $user['phone'],
                'name' => $user['name'],
                'role' => $user['role']
            ]
        ]);
    } else {
        send_response(401, ['detail' => 'Invalid phone or password']);
    }
}

// POST /api/register equivalent
if ($_SERVER['REQUEST_METHOD'] == 'POST' && strpos($_SERVER['REQUEST_URI'], '/api/register') !== false) {
    $input = get_json_input();
    $phone = $input['phone'];
    $name = $input['name'];
    $password = password_hash($input['password'], PASSWORD_BCRYPT);
    $role = isset($input['role']) ? $input['role'] : 'student';

    $stmt = $pdo->prepare("INSERT INTO users (phone, name, hashed_password, role) VALUES (?, ?, ?, ?)");
    try {
        $stmt->execute([$phone, $name, $password, $role]);
        $new_id = $pdo->lastInsertId();
        send_response(201, ['id' => (int)$new_id, 'message' => 'User registered']);
    } catch (PDOException $e) {
        send_response(400, ['detail' => 'User already exists or registration failed']);
    }
}
?>
