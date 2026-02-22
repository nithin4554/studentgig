"""
AI Match Engine — compares user skills to job requirements.
Uses TF-IDF vectorization + cosine similarity from scikit-learn.

Returns: integer 0–100 representing match percentage.
"""

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import json


def parse_skills_string(skills_str: str) -> str:
    """
    Convert a JSON skill array to a space-separated string for TF-IDF.
    Input:  '["python", "hindi", "communication"]'
    Output: 'python hindi communication'
    """
    if not skills_str:
        return ""
    try:
        skills_list = json.loads(skills_str)
        if isinstance(skills_list, list):
            return " ".join(str(s).lower().strip() for s in skills_list)
    except (json.JSONDecodeError, TypeError):
        pass
    # Fallback: treat as comma-separated or plain text
    return skills_str.lower().replace(",", " ").replace("[", "").replace("]", "").replace('"', "")


def calculate_match_score(user_skills: str, job_requirements: str) -> int:
    """
    Calculate how well a user's skills match a job's requirements.

    Args:
        user_skills: JSON string of user skills, e.g., '["python", "hindi"]'
        job_requirements: JSON string of job requirements, e.g., '["hindi", "teamwork"]'

    Returns:
        Integer 0–100 representing match percentage.
        Returns 0 if either input is empty/None.
    """
    user_text = parse_skills_string(user_skills) if user_skills else ""
    job_text = parse_skills_string(job_requirements) if job_requirements else ""

    if not user_text or not job_text:
        return 0

    try:
        vectorizer = TfidfVectorizer()
        tfidf_matrix = vectorizer.fit_transform([user_text, job_text])
        similarity = cosine_similarity(tfidf_matrix[0:1], tfidf_matrix[1:2])
        score = int(round(similarity[0][0] * 100))
        return min(max(score, 0), 100)  # Clamp to 0–100
    except Exception:
        return 0
