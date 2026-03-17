"""
AI Match Engine — StudentGig Intelligence Layer

Provides multiple AI-powered features:
  1. Smart Match Scoring   — skill overlap between user & job
  2. Smart Feed Ranking    — personalized job ranking (composite intelligence score)
  3. Skill Recommendations — suggests skills to learn based on market demand
  4. Applicant Ranking     — ranks job applicants for employers
  5. Pay Estimation        — suggests fair pay based on similar jobs

Returns: integer 0–100 representing match percentage, or structured AI insights.
"""

import json
import logging
import math
from collections import Counter
from typing import List, Dict, Optional, Tuple
from datetime import datetime, timezone, timedelta

logger = logging.getLogger("studentgig")


# ═══════════════════════════════════════════════════════════════════════════════════
#  UTILITY: Skill Parsing
# ═══════════════════════════════════════════════════════════════════════════════════

def parse_skills_string(skills_str: str) -> str:
    """
    Convert a JSON skill array to a space-separated string.
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


def _parse_to_set(skills_str: str) -> set:
    """Parse skills string into a set of individual skill tokens."""
    text = parse_skills_string(skills_str)
    if not text:
        return set()
    return set(text.split())


def _parse_to_list(skills_str: str) -> list:
    """Parse skills string into a list of individual skill tokens."""
    if not skills_str:
        return []
    try:
        skills_list = json.loads(skills_str)
        if isinstance(skills_list, list):
            return [str(s).lower().strip() for s in skills_list]
    except (json.JSONDecodeError, TypeError):
        pass
    return [s.strip().lower() for s in skills_str.replace('"', '').replace('[', '').replace(']', '').split(',') if s.strip()]


# ═══════════════════════════════════════════════════════════════════════════════════
#  FEATURE 1: Smart Match Scoring (Enhanced)
# ═══════════════════════════════════════════════════════════════════════════════════

def calculate_match_score(user_skills: str, job_requirements: str) -> int:
    """
    Calculate how well a user's skills match a job's requirements.

    Uses fast token-overlap for real-time scoring (no heavy ML dependency).
    This is O(n) and creates zero large objects, making it safe to call
    per-job inside a list endpoint.

    Args:
        user_skills: JSON string of user skills, e.g., '["python", "hindi"]'
        job_requirements: JSON string of job requirements, e.g., '["hindi", "teamwork"]'

    Returns:
        Integer 0–100 representing match percentage.
        Returns 0 if either input is empty/None.
    """
    try:
        user_set = _parse_to_set(user_skills) if user_skills else set()
        job_set = _parse_to_set(job_requirements) if job_requirements else set()

        if not user_set or not job_set:
            return 0

        # Overlap-based scoring (Jaccard-like, weighted toward job requirements)
        overlap = user_set & job_set
        if not overlap:
            return 0

        # Weight: 70% based on job coverage, 30% based on overall similarity
        job_coverage = len(overlap) / len(job_set)       # How many job skills user has
        jaccard = len(overlap) / len(user_set | job_set)  # Overall similarity

        score = int(round((0.7 * job_coverage + 0.3 * jaccard) * 100))
        return min(max(score, 0), 100)  # Clamp to 0–100
    except Exception:
        return 0


def calculate_match_explanation(user_skills: str, job_requirements: str) -> dict:
    """
    Returns a detailed match breakdown with explanation.
    Used on job detail pages to show WHY a match score is what it is.
    """
    user_set = _parse_to_set(user_skills) if user_skills else set()
    job_set = _parse_to_set(job_requirements) if job_requirements else set()

    if not user_set or not job_set:
        return {
            "score": 0,
            "matched_skills": [],
            "missing_skills": list(job_set),
            "extra_skills": list(user_set),
            "explanation": "Add your skills in Profile to see AI match scores."
        }

    matched = sorted(user_set & job_set)
    missing = sorted(job_set - user_set)
    extra = sorted(user_set - job_set)
    score = calculate_match_score(user_skills, job_requirements)

    if score >= 80:
        explanation = f"Excellent match! You have {len(matched)} of {len(job_set)} required skills."
    elif score >= 50:
        explanation = f"Good match — you have {len(matched)} of {len(job_set)} required skills. Learn {', '.join(missing[:2])} to boost your score."
    elif score > 0:
        explanation = f"Partial match. You need {len(missing)} more skills: {', '.join(missing[:3])}."
    else:
        explanation = "No skill overlap. Consider learning the required skills."

    return {
        "score": score,
        "matched_skills": matched,
        "missing_skills": missing,
        "extra_skills": extra,
        "explanation": explanation
    }


# ═══════════════════════════════════════════════════════════════════════════════════
#  FEATURE 2: AI Smart Feed (Personalized Job Ranking)
# ═══════════════════════════════════════════════════════════════════════════════════

def compute_smart_feed_score(
    job,
    user,
    user_application_history: List[dict] = None,
) -> dict:
    """
    Compute a composite AI ranking score for a job personalized to a user.
    
    Factors (weights):
      - Skill Match:        35%  (from calculate_match_score)
      - Category Affinity:  20%  (based on past applications in this category)
      - Pay Preference:     15%  (based on past pay ranges they applied to)
      - Urgency Boost:      10%  (urgent jobs get a bump)
      - Recency Decay:      10%  (newer jobs ranked higher)
      - Location Fit:       10%  (matching location preference)
    
    Returns:
        dict with ai_score (0-100), breakdown, and reason string
    """
    breakdown = {}
    
    # ─── 1. Skill Match (35%) ────────────────────────────────────────────
    skill_score = 0
    if user and getattr(user, 'skills_json', None) and job.skills_required:
        skill_score = calculate_match_score(user.skills_json, job.skills_required)
    breakdown["skill_match"] = skill_score
    
    # ─── 2. Category Affinity (20%) ──────────────────────────────────────
    category_score = 0
    if user_application_history and getattr(job, 'category', None):
        applied_categories = [h.get("category", "").lower() for h in user_application_history if h.get("category")]
        if applied_categories:
            job_cat = (job.category or "").lower()
            cat_count = sum(1 for c in applied_categories if c == job_cat)
            total_apps = len(applied_categories)
            category_score = min(int((cat_count / max(total_apps, 1)) * 100), 100)
            # Boost: if they've applied to this category 3+ times, strong signal
            if cat_count >= 3:
                category_score = min(category_score + 20, 100)
    breakdown["category_affinity"] = category_score
    
    # ─── 3. Pay Preference (15%) ─────────────────────────────────────────
    pay_score = 50  # Default neutral
    if user_application_history:
        past_pays = [h.get("pay_amount", 0) for h in user_application_history if h.get("pay_amount")]
        if past_pays:
            avg_pay = sum(past_pays) / len(past_pays)
            job_pay = float(getattr(job, 'pay_amount', 0) or 0)
            if avg_pay > 0 and job_pay > 0:
                # Score based on how close job pay is to their preference
                ratio = job_pay / avg_pay
                if 0.7 <= ratio <= 1.5:
                    pay_score = 80 + int(20 * (1 - abs(1 - ratio)))
                elif ratio > 1.5:
                    pay_score = 90  # Higher pay is always good
                else:
                    pay_score = max(int(ratio * 70), 20)
    breakdown["pay_preference"] = pay_score
    
    # ─── 4. Urgency Boost (10%) ──────────────────────────────────────────
    urgency_score = 80 if getattr(job, 'is_urgent', False) else 30
    breakdown["urgency_boost"] = urgency_score
    
    # ─── 5. Recency Decay (10%) ──────────────────────────────────────────
    recency_score = 50
    created = getattr(job, 'created_at', None)
    if created:
        try:
            if isinstance(created, str):
                created = datetime.fromisoformat(created.replace('Z', '+00:00'))
            now = datetime.now(timezone.utc)
            if created.tzinfo is None:
                created = created.replace(tzinfo=timezone.utc)
            hours_old = (now - created).total_seconds() / 3600
            # Exponential decay: fresh jobs score higher
            recency_score = max(int(100 * math.exp(-hours_old / 168)), 10)  # 168h = 1 week half-life
        except Exception:
            recency_score = 50
    breakdown["recency"] = recency_score
    
    # ─── 6. Location Fit (10%) ───────────────────────────────────────────
    location_score = 40  # Default neutral
    if user_application_history:
        past_locations = [h.get("location", "").lower() for h in user_application_history if h.get("location")]
        if past_locations:
            job_loc = (getattr(job, 'location', '') or '').lower()
            if job_loc:
                loc_match = sum(1 for l in past_locations if l == job_loc)
                if loc_match > 0:
                    location_score = min(60 + int(40 * loc_match / len(past_locations)), 100)
                if job_loc == "remote":
                    location_score = max(location_score, 70)  # Remote is always decent
    breakdown["location_fit"] = location_score
    
    # ─── Composite Score ─────────────────────────────────────────────────
    composite = int(
        0.35 * skill_score +
        0.20 * category_score +
        0.15 * pay_score +
        0.10 * urgency_score +
        0.10 * recency_score +
        0.10 * location_score
    )
    composite = min(max(composite, 0), 100)
    
    # ─── Generate Reason ─────────────────────────────────────────────────
    reasons = []
    if skill_score >= 60:
        reasons.append(f"{skill_score}% skill match")
    if category_score >= 50:
        reasons.append("fits your interests")
    if urgency_score >= 80:
        reasons.append("urgent opening")
    if recency_score >= 70:
        reasons.append("just posted")
    if pay_score >= 70:
        reasons.append("matches your pay range")
    if location_score >= 60:
        reasons.append("preferred location")
    
    if not reasons:
        reason = "Explore this opportunity"
    else:
        reason = "AI picked this: " + ", ".join(reasons[:3])
    
    return {
        "ai_score": composite,
        "breakdown": breakdown,
        "reason": reason
    }


# ═══════════════════════════════════════════════════════════════════════════════════
#  FEATURE 3: AI Skill Recommendations
# ═══════════════════════════════════════════════════════════════════════════════════

def recommend_skills(
    user_skills_json: str,
    all_jobs: list,
    max_recommendations: int = 8,
) -> List[dict]:
    """
    Analyze market demand and recommend skills for the user to learn.
    
    Reviews all open jobs, counts in-demand skills, subtracts user's current skills,
    and returns the top recommendations with impact analysis.
    
    Args:
        user_skills_json: User's current skills as JSON string
        all_jobs: List of Job model objects (from DB query)
        max_recommendations: Max skills to recommend
    
    Returns:
        List of dicts with: skill, demand_count, new_matches (jobs they'd newly qualify for),
        category (which job categories need this), reason
    """
    user_skills = _parse_to_set(user_skills_json)
    
    # Count skill demand across all open jobs
    skill_demand = Counter()
    skill_categories = {}  # skill → set of categories
    skill_jobs = {}  # skill → list of job IDs
    
    for job in all_jobs:
        job_skills = _parse_to_set(getattr(job, 'skills_required', '') or '')
        category = getattr(job, 'category', 'Other') or 'Other'
        job_id = getattr(job, 'id', 0)
        
        for skill in job_skills:
            skill_demand[skill] += 1
            if skill not in skill_categories:
                skill_categories[skill] = set()
            skill_categories[skill].add(category)
            if skill not in skill_jobs:
                skill_jobs[skill] = []
            skill_jobs[skill].append(job_id)
    
    # Filter out skills user already has
    new_skills = {s: count for s, count in skill_demand.items() if s not in user_skills}
    
    # Sort by demand (most demanded first)
    sorted_skills = sorted(new_skills.items(), key=lambda x: x[1], reverse=True)
    
    recommendations = []
    for skill, demand_count in sorted_skills[:max_recommendations]:
        # Calculate how many NEW job matches this skill would unlock
        new_matches = 0
        for job in all_jobs:
            job_skills = _parse_to_set(getattr(job, 'skills_required', '') or '')
            if skill in job_skills:
                # Would this skill help match a job they currently don't match?
                current_overlap = user_skills & job_skills
                new_overlap = (user_skills | {skill}) & job_skills
                if len(new_overlap) > len(current_overlap):
                    new_matches += 1
        
        categories = sorted(skill_categories.get(skill, set()))
        
        # Generate reason
        if demand_count >= 5:
            reason = f"High demand — {demand_count} open gigs need this skill"
        elif demand_count >= 3:
            reason = f"Growing demand — {demand_count} gigs need this"
        else:
            reason = f"Niche skill — {demand_count} gigs, less competition"
        
        recommendations.append({
            "skill": skill,
            "demand_count": demand_count,
            "new_matches": new_matches,
            "categories": categories[:3],
            "reason": reason,
        })
    
    return recommendations


# ═══════════════════════════════════════════════════════════════════════════════════
#  FEATURE 4: AI Applicant Ranking (For Employers)
# ═══════════════════════════════════════════════════════════════════════════════════

def rank_applicant(
    applicant_user,
    job,
    application,
) -> dict:
    """
    Compute an AI ranking score for an applicant applying to a specific job.
    
    Factors:
      - Skill Match:     40%  (how well skills match job requirements)
      - Reliability:     30%  (rating + gigs completed + completion rate)
      - Responsiveness:  15%  (how quickly they applied after job was posted)
      - Availability:    15%  (are they likely free? Based on active gigs)
    
    Returns:
        dict with ai_rank_score (0-100), breakdown, and badges
    """
    breakdown = {}
    
    # ─── Skill Match (40%) ───────────────────────────────────────────────
    skill_score = 0
    if applicant_user and getattr(applicant_user, 'skills_json', None):
        skill_score = calculate_match_score(
            applicant_user.skills_json,
            getattr(job, 'skills_required', '') or ''
        )
    breakdown["skill_match"] = skill_score
    
    # ─── Reliability (30%) ───────────────────────────────────────────────
    reliability = 30  # Base for new users
    rating = float(getattr(applicant_user, 'rating', 0) or 0)
    gigs = int(getattr(applicant_user, 'gigs_completed', 0) or 0)
    
    if gigs > 0:
        # Rating component (0-50)
        rating_component = int(rating * 10)  # 5.0 → 50
        # Experience component (0-50): logarithmic, plateaus at ~20 gigs
        experience_component = min(int(25 * math.log2(gigs + 1)), 50)
        reliability = min(rating_component + experience_component, 100)
    breakdown["reliability"] = reliability
    
    # ─── Responsiveness (15%) ────────────────────────────────────────────
    responsiveness = 50  # Default
    applied_at = getattr(application, 'applied_at', None)
    job_created = getattr(job, 'created_at', None)
    if applied_at and job_created:
        try:
            if isinstance(applied_at, str):
                applied_at = datetime.fromisoformat(applied_at.replace('Z', '+00:00'))
            if isinstance(job_created, str):
                job_created = datetime.fromisoformat(job_created.replace('Z', '+00:00'))
            if applied_at.tzinfo is None:
                applied_at = applied_at.replace(tzinfo=timezone.utc)
            if job_created.tzinfo is None:
                job_created = job_created.replace(tzinfo=timezone.utc)
            hours_delay = max((applied_at - job_created).total_seconds() / 3600, 0)
            # Fast applicants score higher (within 1h = 100, within 24h = 60, etc.)
            responsiveness = max(int(100 * math.exp(-hours_delay / 12)), 10)
        except Exception:
            responsiveness = 50
    breakdown["responsiveness"] = responsiveness
    
    # ─── Availability (15%) ──────────────────────────────────────────────
    # Default high — we'd need past application data to truly estimate
    availability = 70
    breakdown["availability"] = availability
    
    # ─── Composite ───────────────────────────────────────────────────────
    composite = int(
        0.40 * skill_score +
        0.30 * reliability +
        0.15 * responsiveness +
        0.15 * availability
    )
    composite = min(max(composite, 0), 100)
    
    # ─── Badges ──────────────────────────────────────────────────────────
    badges = []
    if composite >= 85:
        badges.append("🏆 AI Top Pick")
    if skill_score >= 90:
        badges.append("🎯 Perfect Match")
    if reliability >= 80:
        badges.append("⭐ Highly Reliable")
    if responsiveness >= 80:
        badges.append("⚡ Fast Responder")
    if gigs >= 10:
        badges.append("🔥 Experienced")
    
    return {
        "ai_rank_score": composite,
        "breakdown": breakdown,
        "badges": badges,
    }


# ═══════════════════════════════════════════════════════════════════════════════════
#  FEATURE 5: AI Pay Estimator
# ═══════════════════════════════════════════════════════════════════════════════════

def estimate_pay(
    category: str,
    location: str,
    duration: str,
    job_type: str,
    all_jobs: list,
) -> dict:
    """
    Estimate fair pay for a job based on similar past postings.
    
    Analyzes jobs with matching category, location, and type to suggest
    a pay range.
    
    Returns:
        dict with min_pay, avg_pay, max_pay, sample_size, confidence, reasoning
    """
    similar_pays = []
    
    for job in all_jobs:
        score = 0
        # Category match (strongest signal)
        if category and getattr(job, 'category', '') and \
           (getattr(job, 'category', '') or '').lower() == category.lower():
            score += 3
        # Location match
        if location and getattr(job, 'location', '') and \
           (getattr(job, 'location', '') or '').lower() == location.lower():
            score += 2
        # Job type match
        if job_type and getattr(job, 'job_type', '') and \
           (getattr(job, 'job_type', '') or '').lower() == job_type.lower():
            score += 1
        
        if score >= 2:  # At least category + one other factor
            pay = float(getattr(job, 'pay_amount', 0) or 0)
            if pay > 0:
                similar_pays.append(pay)
    
    if not similar_pays:
        # Fallback: use all jobs in the category
        for job in all_jobs:
            if category and getattr(job, 'category', '') and \
               (getattr(job, 'category', '') or '').lower() == category.lower():
                pay = float(getattr(job, 'pay_amount', 0) or 0)
                if pay > 0:
                    similar_pays.append(pay)
    
    if not similar_pays:
        return {
            "min_pay": None,
            "avg_pay": None,
            "max_pay": None,
            "sample_size": 0,
            "confidence": "low",
            "reasoning": "Not enough similar jobs to estimate. Set your own price."
        }
    
    min_pay = min(similar_pays)
    max_pay = max(similar_pays)
    avg_pay = sum(similar_pays) / len(similar_pays)
    
    # Confidence based on sample size
    if len(similar_pays) >= 10:
        confidence = "high"
    elif len(similar_pays) >= 5:
        confidence = "medium"
    else:
        confidence = "low"
    
    # Smart rounding
    def smart_round(x):
        if x >= 1000:
            return round(x / 100) * 100
        elif x >= 100:
            return round(x / 50) * 50
        return round(x / 10) * 10
    
    return {
        "min_pay": smart_round(min_pay),
        "avg_pay": smart_round(avg_pay),
        "max_pay": smart_round(max_pay),
        "sample_size": len(similar_pays),
        "confidence": confidence,
        "reasoning": f"Based on {len(similar_pays)} similar {category or ''} gigs in {location or 'all locations'}. Average pay: ₹{smart_round(avg_pay)}."
    }


# ═══════════════════════════════════════════════════════════════════════════════════
#  FEATURE 6: AI Earnings Insights
# ═══════════════════════════════════════════════════════════════════════════════════

def analyze_earnings(
    user,
    completed_applications: list,
    all_jobs_map: dict,
) -> dict:
    """
    Analyze a user's earning patterns and generate insights + predictions.
    
    Returns:
        dict with insights list, best_category, projected_monthly, tips
    """
    if not completed_applications:
        return {
            "insights": ["Complete your first gig to unlock AI earnings insights! 🚀"],
            "best_category": None,
            "projected_monthly": 0,
            "tips": ["Apply to jobs matching your skills for the best acceptance rate."],
        }
    
    # Analyze by category
    category_earnings = Counter()
    category_counts = Counter()
    total_earned = 0
    
    for app in completed_applications:
        job = all_jobs_map.get(app.job_id)
        if job:
            pay = float(getattr(job, 'pay_amount', 0) or 0)
            cat = getattr(job, 'category', 'Other') or 'Other'
            category_earnings[cat] += pay
            category_counts[cat] += 1
            total_earned += pay
    
    # Find best category
    best_cat = category_earnings.most_common(1)
    best_category = best_cat[0][0] if best_cat else None
    best_category_earnings = best_cat[0][1] if best_cat else 0
    
    # Project monthly earnings (based on last 30 days)
    recent_apps = []
    now = datetime.now(timezone.utc)
    for app in completed_applications:
        paid_at = getattr(app, 'paid_at', None)
        if paid_at:
            try:
                if isinstance(paid_at, str):
                    paid_at = datetime.fromisoformat(paid_at.replace('Z', '+00:00'))
                if paid_at.tzinfo is None:
                    paid_at = paid_at.replace(tzinfo=timezone.utc)
                if (now - paid_at).days <= 30:
                    job = all_jobs_map.get(app.job_id)
                    if job:
                        recent_apps.append(float(getattr(job, 'pay_amount', 0) or 0))
            except Exception:
                pass
    
    projected = sum(recent_apps) if recent_apps else 0
    
    # Generate insights
    insights = []
    
    if best_category:
        insights.append(f"🏆 Your top earning category: {best_category} (₹{int(best_category_earnings)} earned)")
    
    if len(category_counts) > 1:
        avg_per_gig = total_earned / sum(category_counts.values())
        insights.append(f"💰 Average earning per gig: ₹{int(avg_per_gig)}")
    
    total_gigs = sum(category_counts.values())
    if total_gigs >= 3:
        insights.append(f"📊 You've completed {total_gigs} gigs across {len(category_counts)} categories")
    
    if projected > 0:
        insights.append(f"📈 This month's projected earnings: ₹{int(projected)}")
    
    # Tips
    tips = []
    if len(category_counts) == 1:
        tips.append(f"Try gigs in other categories to diversify your income! You're only doing {best_category}.")
    if total_gigs < 5:
        tips.append("Apply to more gigs — your first 5 completions build your trust badge! ⭐")
    if best_category:
        tips.append(f"Double down on {best_category} gigs — that's where you earn the most.")
    
    return {
        "insights": insights or ["Keep completing gigs to unlock more insights! 🚀"],
        "best_category": best_category,
        "projected_monthly": int(projected),
        "tips": tips or ["Keep up the great work! 💪"],
    }
