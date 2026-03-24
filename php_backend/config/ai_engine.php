<?php
/**
 * AI Match Engine — StudentGig Intelligence Layer (PHP Port)
 */

function parse_skills_string($skills_str) {
    if (!$skills_str) return "";
    $decoded = json_decode($skills_str, true);
    if (is_array($decoded)) {
        return implode(" ", array_map('strtolower', array_map('trim', $decoded)));
    }
    // Fallback: treat as comma-separated or plain text
    return strtolower(str_replace(['[', ']', '"', ','], ['', '', '', ' '], $skills_str));
}

function _parse_to_set($skills_str) {
    $text = parse_skills_string($skills_str);
    if (!$text) return [];
    return array_unique(explode(" ", $text));
}

function calculate_match_score($user_skills, $job_requirements) {
    try {
        $user_set = $user_skills ? _parse_to_set($user_skills) : [];
        $job_set = $job_requirements ? _parse_to_set($job_requirements) : [];

        if (empty($user_set) || empty($job_set)) return 0;

        $overlap = array_intersect($user_set, $job_set);
        if (empty($overlap)) return 0;

        $job_coverage = count($overlap) / count($job_set);
        $jaccard = count($overlap) / count(array_unique(array_merge($user_set, $job_set)));

        $score = (int)round((0.7 * $job_coverage + 0.3 * $jaccard) * 100);
        return min(max($score, 0), 100);
    } catch (Exception $e) {
        return 0;
    }
}

// ... other AI functions would be ported here as needed
?>
