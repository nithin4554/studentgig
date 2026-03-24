"""
AI LLM Module — Groq-Powered Intelligence for StudentGig

Uses Groq API (free tier: 30 requests/minute) for AI features:
  1. Job Description Generator — turns rough ideas into professional postings
  2. Smart Search NLP Parser  — interprets natural language search queries
  3. Application Note Generator — writes personalized cover notes

Requires: GROQ_API_KEY environment variable
Get your free key at: https://console.groq.com
"""

import os
import json
import logging
import re
import time
import urllib.request
import urllib.error
from typing import Optional

logger = logging.getLogger("studentgig")

# ─── Groq Configuration ─────────────────────────────────────────────────────────

GROQ_API_KEY = os.environ.get("GROQ_API_KEY", "")
GROQ_MODEL = "llama-3.3-70b-versatile"
GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"


# ─── Core LLM Call ───────────────────────────────────────────────────────────────

def _call_groq(prompt: str, max_tokens: int = 1024, retries: int = 2) -> Optional[str]:
    """
    Call Groq API using simple HTTP — no SDK needed.
    
    Returns the response text, or None if the call fails.
    """
    from dotenv import load_dotenv
    load_dotenv()
    
    api_key = os.environ.get("GROQ_API_KEY", "")
    if not api_key:
        logger.warning("⚠️  GROQ_API_KEY not set — AI features will use fallback mode.")
        logger.warning("   Get your free key at: https://console.groq.com")
        return None

    payload = json.dumps({
        "model": GROQ_MODEL,
        "messages": [
            {
                "role": "system",
                "content": "You are an expert assistant for StudentGig, a student gig platform in India. Always respond precisely in the requested format."
            },
            {
                "role": "user",
                "content": prompt
            }
        ],
        "max_tokens": max_tokens,
        "temperature": 0.7,
    }).encode("utf-8")

    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {api_key}",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    }

    for attempt in range(retries + 1):
        try:
            req = urllib.request.Request(GROQ_API_URL, data=payload, headers=headers)
            with urllib.request.urlopen(req, timeout=30) as resp:
                result = json.loads(resp.read().decode("utf-8"))
                text = result["choices"][0]["message"]["content"]
                logger.info(f"✅ Groq response received ({len(text)} chars)")
                return text.strip()

        except urllib.error.HTTPError as e:
            error_body = e.read().decode("utf-8", errors="replace")
            if e.code == 429:
                if attempt < retries:
                    retry_match = re.search(r'try again in (\d+\.?\d*)', error_body)
                    wait = float(retry_match.group(1)) + 0.5 if retry_match else 3
                    logger.warning(f"Groq rate limited. Waiting {wait:.1f}s (retry {attempt + 1}/{retries})")
                    time.sleep(wait)
                    continue
                else:
                    logger.warning(f"Groq rate limited after {retries} retries. Using fallback.")
                    return None
            elif e.code == 401:
                logger.error("❌ Groq API key is invalid. Get a new key at: https://console.groq.com")
                return None
            else:
                logger.error(f"Groq API error {e.code}: {error_body[:200]}")
                return None
        except Exception as e:
            logger.error(f"Groq request error: {e}")
            return None

    return None


# Backward compatibility — old code calls _call_gemini
_call_gemini = _call_groq


def _ensure_gemini():
    """Backward compatibility — checks if Groq key is available."""
    return bool(os.environ.get("GROQ_API_KEY", ""))


# ═══════════════════════════════════════════════════════════════════════════════════
#  HELPERS: Clean AI-generated text
# ═══════════════════════════════════════════════════════════════════════════════════

def _clean_description(desc: str) -> str:
    """
    Clean a description that may contain JSON artifacts.
    
    Handles cases where Groq returns:
    - '{"description": "actual text..."}' (double-nested JSON)
    - '"description": "actual text"' (partial JSON key)
    - Normal clean text (passes through unchanged)
    """
    if not desc:
        return ""
    
    desc = desc.strip()
    
    # Case 1: The description itself is a JSON string — try to unwrap it
    if desc.startswith('{') and '"description"' in desc[:30]:
        try:
            inner = json.loads(desc)
            if isinstance(inner, dict) and "description" in inner:
                desc = inner["description"]
        except (json.JSONDecodeError, TypeError):
            # Strip the JSON wrapper manually
            match = re.search(r'"description"\s*:\s*"(.*)', desc, re.DOTALL)
            if match:
                desc = match.group(1)
                # Remove trailing JSON artifacts
                desc = re.sub(r'",\s*"suggested_.*$', '', desc, flags=re.DOTALL)
                desc = desc.rstrip('"}')
    
    # Case 2: Starts with "description": prefix
    if desc.lower().startswith('"description"'):
        match = re.search(r'"description"\s*:\s*"?(.*)', desc, re.DOTALL)
        if match:
            desc = match.group(1)
    
    # Clean up common artifacts
    desc = desc.strip().strip('"').strip("'")
    desc = desc.replace('\\"', '"')      # Unescape quotes
    desc = desc.replace('\\n', '\n')     # Unescape newlines
    desc = re.sub(r'\n{3,}', '\n\n', desc)  # Collapse excessive newlines
    desc = desc.strip()
    
    return desc


def _extract_description_from_raw(raw: str) -> str:
    """
    Extract description text from a raw LLM response that failed JSON parsing.
    """
    if not raw:
        return ""
    
    # Try to find the description value in the raw text
    match = re.search(r'"description"\s*:\s*"((?:[^"\\]|\\.)*)"', raw, re.DOTALL)
    if match:
        desc = match.group(1)
        desc = desc.replace('\\"', '"').replace('\\n', '\n')
        return desc.strip()
    
    # Last resort: strip any JSON-like wrapper and return the text
    cleaned = raw.strip()
    cleaned = re.sub(r'^```json\s*', '', cleaned)
    cleaned = re.sub(r'^```\s*', '', cleaned)
    cleaned = re.sub(r'\s*```$', '', cleaned)
    cleaned = re.sub(r'^\{.*?"description"\s*:\s*"', '', cleaned, flags=re.DOTALL)
    cleaned = re.sub(r'",\s*"suggested_.*$', '', cleaned, flags=re.DOTALL)
    cleaned = cleaned.strip().strip('"').rstrip('"}')
    
    return cleaned[:500] if cleaned else ""


# ═══════════════════════════════════════════════════════════════════════════════════
#  FEATURE 1: AI Job Description Generator
# ═══════════════════════════════════════════════════════════════════════════════════

JOB_CATEGORIES = [
    "Tutoring", "Delivery", "Events", "Tech", "Content Creation",
    "Design", "Marketing", "Data Entry", "Photography", "Volunteering",
    "Writing", "Translation", "Hospitality", "Fitness", "Other"
]


def generate_job_description(
    title: str,
    category: Optional[str] = None,
    rough_notes: Optional[str] = None,
    location: Optional[str] = None,
    duration: Optional[str] = None,
) -> dict:
    """Generate a professional job description from minimal input."""
    prompt = f"""You are an expert job posting writer for StudentGig, a platform connecting college students with local gig opportunities in India.

Generate a professional, appealing job description based on this input:

Title: {title}
Category: {category or 'Auto-detect'}
Additional Notes: {rough_notes or 'None provided'}
Location: {location or 'Not specified'}
Duration: {duration or 'Not specified'}

RULES:
1. Write a compelling 2-3 paragraph description that would attract college students
2. Be specific about duties, requirements, and what makes this gig attractive
3. Keep it natural and engaging — not corporate-stiff
4. Suggest 3-6 relevant skills as a JSON array (lowercase, hyphenated)
5. Suggest the best category from: {', '.join(JOB_CATEGORIES)}
6. Suggest a fair pay range in Indian Rupees (for students)
7. The currency is Indian Rupees, not dollars

Respond in this EXACT JSON format (no markdown, no code fences):
{{"description": "The full job description text here...", "suggested_skills": ["skill-1", "skill-2", "skill-3"], "suggested_category": "Category", "suggested_pay_min": 500, "suggested_pay_max": 1500}}"""

    result = _call_groq(prompt, max_tokens=800)

    if not result:
        return _fallback_description(title, category, rough_notes)

    try:
        cleaned = result.strip()
        cleaned = re.sub(r'^```json\s*', '', cleaned)
        cleaned = re.sub(r'^```\s*', '', cleaned)
        cleaned = re.sub(r'\s*```$', '', cleaned)
        parsed = json.loads(cleaned)

        # Sometimes the "description" value is itself a JSON string — unwrap it
        desc = parsed.get("description", "")
        desc = _clean_description(desc)

        return {
            "description": desc,
            "suggested_skills": json.dumps(parsed.get("suggested_skills", [])),
            "suggested_category": parsed.get("suggested_category", category or "Other"),
            "suggested_pay_min": parsed.get("suggested_pay_min", 300),
            "suggested_pay_max": parsed.get("suggested_pay_max", 1500),
            "ai_generated": True,
        }
    except (json.JSONDecodeError, KeyError) as e:
        logger.warning(f"Failed to parse Groq response: {e}")
        # Try to extract just the description from raw text
        desc = _extract_description_from_raw(result)
        return {
            "description": desc,
            "suggested_skills": "[]",
            "suggested_category": category or "Other",
            "suggested_pay_min": 300,
            "suggested_pay_max": 1500,
            "ai_generated": True,
        }


def _fallback_description(title: str, category: Optional[str], notes: Optional[str]) -> dict:
    """Generate a basic description when Groq is unavailable."""
    desc = f"We're looking for a motivated college student for: {title}."
    if notes:
        desc += f" {notes}"
    desc += " This is a great opportunity to earn while gaining real-world experience. Apply now!"

    skill_map = {
        "Tutoring": ["teaching", "patience", "communication"],
        "Delivery": ["punctuality", "navigation", "bicycle"],
        "Events": ["teamwork", "communication", "energy"],
        "Tech": ["programming", "problem-solving", "computer"],
        "Content Creation": ["writing", "creativity", "social-media"],
        "Design": ["canva", "figma", "creativity"],
        "Marketing": ["communication", "marketing", "social-media"],
        "Data Entry": ["typing", "excel", "attention-to-detail"],
        "Photography": ["photography", "editing", "creativity"],
    }

    return {
        "description": desc,
        "suggested_skills": json.dumps(skill_map.get(category, ["communication", "teamwork"])),
        "suggested_category": category or "Other",
        "suggested_pay_min": 300,
        "suggested_pay_max": 1500,
        "ai_generated": False,
    }


# ═══════════════════════════════════════════════════════════════════════════════════
#  FEATURE 2: Smart Search NLP Parser
# ═══════════════════════════════════════════════════════════════════════════════════

def parse_natural_language_search(query: str) -> dict:
    """Parse a natural language search query into structured filters."""
    prompt = f"""Parse this job search query into structured filters for a student gig platform in India.

Query: "{query}"

Available categories: {', '.join(JOB_CATEGORIES)}

Extract these filters from the natural language query. If a filter isn't mentioned, set it to null.

Respond in this EXACT JSON format (no markdown, no code fences):
{{"search_text": "core keyword(s) to search in job titles/descriptions, or null if too vague", "location": "city name or Remote if mentioned, or null", "min_pay": null, "max_pay": null, "category": "Best matching category from the list, or null", "urgent_only": false, "interpretation": "One sentence explaining how you interpreted the query"}}"""

    result = _call_groq(prompt, max_tokens=300)

    if not result:
        return _fallback_search_parse(query)

    try:
        cleaned = result.strip()
        cleaned = re.sub(r'^```json\s*', '', cleaned)
        cleaned = re.sub(r'^```\s*', '', cleaned)
        cleaned = re.sub(r'\s*```$', '', cleaned)
        parsed = json.loads(cleaned)

        return {
            "search_text": parsed.get("search_text"),
            "location": parsed.get("location"),
            "min_pay": parsed.get("min_pay"),
            "max_pay": parsed.get("max_pay"),
            "category": parsed.get("category"),
            "urgent_only": parsed.get("urgent_only", False),
            "interpretation": parsed.get("interpretation", ""),
            "ai_parsed": True,
        }
    except (json.JSONDecodeError, KeyError):
        return _fallback_search_parse(query)


def _fallback_search_parse(query: str) -> dict:
    """Basic keyword extraction when Groq is unavailable."""
    query_lower = query.lower()

    locations = ["hyderabad", "bangalore", "mumbai", "delhi", "pune", "chennai", "kolkata", "remote"]
    found_location = None
    for loc in locations:
        if loc in query_lower:
            found_location = loc.title()
            break

    min_pay = None
    pay_match = re.search(r'(?:over|above|more than|min|minimum|>\s*)\s*(\d+)', query_lower)
    if pay_match:
        min_pay = int(pay_match.group(1))

    max_pay = None
    pay_match = re.search(r'(?:under|below|less than|max|maximum|<\s*)\s*(\d+)', query_lower)
    if pay_match:
        max_pay = int(pay_match.group(1))

    urgent = any(word in query_lower for word in ["urgent", "asap", "immediately", "today"])

    category_map = {
        "tutor": "Tutoring", "teach": "Tutoring", "math": "Tutoring",
        "deliver": "Delivery", "tiffin": "Delivery",
        "event": "Events", "fest": "Events", "party": "Events",
        "code": "Tech", "programming": "Tech", "python": "Tech", "web": "Tech",
        "content": "Content Creation", "social media": "Content Creation", "instagram": "Content Creation",
        "design": "Design", "poster": "Design", "graphic": "Design",
        "photo": "Photography", "camera": "Photography",
        "data entry": "Data Entry", "typing": "Data Entry",
        "market": "Marketing",
        "write": "Writing", "blog": "Writing",
    }
    category = None
    for keyword, cat in category_map.items():
        if keyword in query_lower:
            category = cat
            break

    search_text = query
    for loc in locations:
        search_text = re.sub(loc, '', search_text, flags=re.IGNORECASE)
    search_text = re.sub(r'(?:over|above|under|below|more than|less than|paying|near|in|gigs?|jobs?)\s*\d*', '', search_text, flags=re.IGNORECASE)
    search_text = search_text.strip() or None

    return {
        "search_text": search_text,
        "location": found_location,
        "min_pay": min_pay,
        "max_pay": max_pay,
        "category": category,
        "urgent_only": urgent,
        "interpretation": f"Searching for '{search_text or query}'" + (f" in {found_location}" if found_location else ""),
        "ai_parsed": False,
    }


# ═══════════════════════════════════════════════════════════════════════════════════
#  FEATURE 3: AI Application Note Generator
# ═══════════════════════════════════════════════════════════════════════════════════

def generate_application_note(
    user_name: str,
    user_skills: str,
    job_title: str,
    job_description: str,
    job_skills_required: str,
    match_score: int,
) -> dict:
    """Generate a personalized cover note for a job application."""
    prompt = f"""Write a brief, friendly application message (2-3 sentences) for a college student applying to a gig.

Student Name: {user_name}
Student Skills: {user_skills or 'Not specified'}
Job Title: {job_title}
Job Description: {job_description or 'Not specified'}
Required Skills: {job_skills_required or 'Not specified'}
Skill Match Score: {match_score}%

RULES:
1. Keep it casual but professional — this is a student applying for a gig, not a corporate job
2. Highlight matching skills if any
3. Show enthusiasm
4. Max 3 sentences
5. Don't use formal salutations like "Dear Sir/Madam"
6. Return ONLY the message text, nothing else

Example style: "Hi! I'm experienced in Python and have tutored 5+ students before. I'd love to help with this gig — my skill match shows I'm a great fit!"
"""

    result = _call_groq(prompt, max_tokens=200)

    if result:
        note = result.strip().strip('"').strip("'")
        return {"note": note, "ai_generated": True}

    # Fallback
    from ai_engine import _parse_to_set
    user_set = _parse_to_set(user_skills)
    job_set = _parse_to_set(job_skills_required)
    matched = user_set & job_set

    if matched:
        note = f"Hi! I'm {user_name} and I'm skilled in {', '.join(list(matched)[:3])}. I'd love to take on this gig — it's a great match for my skills!"
    else:
        note = f"Hi! I'm {user_name} and I'm really interested in this opportunity. I'm a quick learner and eager to contribute!"

    return {"note": note, "ai_generated": False}
