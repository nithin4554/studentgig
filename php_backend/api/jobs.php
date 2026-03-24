<?php
require_once dirname(__DIR__) . '/config/database.php';
require_once dirname(__DIR__) . '/config/helpers.php';

set_cors_headers();

// GET /api/jobs equivalent
if ($_SERVER['REQUEST_METHOD'] == 'GET') {
    $skip = isset($_GET['skip']) ? (int)$_GET['skip'] : 0;
    $limit = isset($_GET['limit']) ? (int)$_GET['limit'] : 50;
    $category = isset($_GET['category']) ? $_GET['category'] : null;

    $sql = "SELECT j.*, u.name as employer_name, 
            (SELECT COUNT(*) FROM applications WHERE job_id = j.id) as applicant_count
            FROM jobs j
            LEFT JOIN users u ON j.employer_id = u.id
            WHERE (j.status IS NULL OR j.status = 'open' OR j.status = '')";
    
    $params = [];
    if ($category) {
        $sql .= " AND j.category = ?";
        $params[] = $category;
    }

    $sql .= " ORDER BY j.created_at DESC LIMIT ?, ?";
    
    $stmt = $pdo->prepare($sql);
    $stmt->bindValue(1 + count($params), $skip, PDO::PARAM_INT);
    $stmt->bindValue(2 + count($params), $limit, PDO::PARAM_INT);
    // Bind additional parameters if needed
    for ($i = 0; $i < count($params); $i++) {
        $stmt->bindValue($i + 1, $params[$i]);
    }

    $stmt->execute();
    $jobs = $stmt->fetchAll();

    // Mapping float values because float(j.pay_amount) in Python
    foreach ($jobs as &$j) {
        $j['pay_amount'] = (float)$j['pay_amount'];
        $j['is_urgent'] = (bool)$j['is_urgent'];
        $j['id'] = (int)$j['id'];
        $j['employer_id'] = $j['employer_id'] ? (int)$j['employer_id'] : null;
        $j['applicant_count'] = (int)$j['applicant_count'];
    }

    send_response(200, $jobs);
}

// POST /api/jobs equivalent
if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $user_id = get_current_user_id();
    if (!$user_id) {
        send_response(401, ['detail' => 'Login required']);
    }

    $input = get_json_input();
    
    // Minimal validation (matches main.py logic)
    $sql = "INSERT INTO jobs (title, description, pay_amount, location, skills_required, is_urgent, 
                              employer_id, company_name, category, job_type, duration, max_applicants, 
                              contact_info, status, job_date, start_time, end_time, address)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'open', ?, ?, ?, ?)";
    
    $stmt = $pdo->prepare($sql);
    $stmt->execute([
        $input['title'],
        $input['description'],
        $input['pay_amount'],
        $input['location'],
        isset($input['skills_required']) ? $input['skills_required'] : null,
        isset($input['is_urgent']) ? (int)$input['is_urgent'] : 0,
        $user_id,
        isset($input['company_name']) ? $input['company_name'] : null,
        isset($input['category']) ? $input['category'] : null,
        isset($input['job_type']) ? $input['job_type'] : 'one-time',
        isset($input['duration']) ? $input['duration'] : null,
        isset($input['max_applicants']) ? (int)$input['max_applicants'] : 1,
        isset($input['contact_info']) ? $input['contact_info'] : null,
        isset($input['job_date']) ? $input['job_date'] : null,
        isset($input['start_time']) ? $input['start_time'] : null,
        isset($input['end_time']) ? $input['end_time'] : null,
        isset($input['address']) ? $input['address'] : null,
    ]);

    $new_id = $pdo->lastInsertId();
    send_response(201, ['id' => (int)$new_id, 'message' => 'Job created']);
}
?>
