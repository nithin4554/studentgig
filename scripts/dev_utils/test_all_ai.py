"""
Comprehensive test for ALL AI backend endpoints.
Tests all 10 AI features from the blueprint.
"""
import urllib.request
import urllib.error
import json
import sys

BASE = "http://localhost:8000"

# First, login to get a JWT token
def login():
    data = json.dumps({"phone": "9999999999", "name": "Test Student"}).encode()
    req = urllib.request.Request(f"{BASE}/api/login", data=data, headers={"Content-Type": "application/json"})
    resp = urllib.request.urlopen(req)
    result = json.loads(resp.read())
    return result["access_token"]

def api_call(method, path, token=None, body=None):
    """Make an API call and return (status_code, result_dict)."""
    url = f"{BASE}{path}"
    data = json.dumps(body).encode() if body else None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    
    if method == "GET" and data:
        # GET with query params — skip body
        data = None
    
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        resp = urllib.request.urlopen(req)
        return resp.status, json.loads(resp.read())
    except urllib.error.HTTPError as e:
        body_text = e.read().decode("utf-8", errors="replace")
        try:
            return e.code, json.loads(body_text)
        except:
            return e.code, {"raw": body_text[:300]}
    except Exception as e:
        return 0, {"error": str(e)}

results = []

def test(name, passed, detail=""):
    status = "✅ PASS" if passed else "❌ FAIL"
    results.append((name, passed, detail))
    print(f"  {status}  {name}" + (f" — {detail}" if detail else ""))

print("=" * 60)
print("  STUDENTGIG AI BACKEND — FULL VERIFICATION")
print("=" * 60)

# Login
token = login()
print(f"\n🔐 Logged in (token: {token[:20]}...)\n")

# ─── 1. AI Smart Feed ────────────────────────────────────────
print("─── 1. AI Smart Feed (GET /api/ai/feed) ───")
code, r = api_call("GET", "/api/ai/feed", token)
test("Endpoint exists", code == 200, f"HTTP {code}")
if code == 200:
    test("Returns list", isinstance(r, list))
    if len(r) > 0:
        test("Has ai_score", "ai_score" in r[0], f"ai_score={r[0].get('ai_score')}")
        test("Has ai_reason", "ai_reason" in r[0], f"reason={r[0].get('ai_reason','')[:50]}")
        test("Has ai_breakdown", "ai_breakdown" in r[0])
    else:
        test("Has jobs", False, "Empty list")
print()

# ─── 2. AI Job Description Generator ─────────────────────────
print("─── 2. AI Job Description Generator (POST /api/ai/generate-description) ───")
code, r = api_call("POST", "/api/ai/generate-description", token, {
    "title": "Campus Photographer",
    "category": "Photography",
    "rough_notes": "college fest photos",
    "location": "Hyderabad"
})
test("Endpoint exists", code == 200, f"HTTP {code}")
if code == 200:
    test("Has description", bool(r.get("description")), f"len={len(r.get('description',''))}")
    test("Has ai_generated", "ai_generated" in r, f"ai_generated={r.get('ai_generated')}")
    test("Has suggested_skills", "suggested_skills" in r)
    test("Has suggested_category", "suggested_category" in r)
    test("Has pay range", "suggested_pay_min" in r and "suggested_pay_max" in r)
print()

# ─── 3. AI Skill Recommendations ─────────────────────────────
print("─── 3. AI Skill Recommendations (GET /api/ai/skill-recommendations) ───")
code, r = api_call("GET", "/api/ai/skill-recommendations", token)
test("Endpoint exists", code == 200, f"HTTP {code}")
if code == 200:
    test("Has recommendations", "recommendations" in r, f"count={len(r.get('recommendations', []))}")
    test("Has current_skills", "current_skills" in r)
    test("Has total_open_jobs", "total_open_jobs" in r)
    if r.get("recommendations"):
        rec = r["recommendations"][0]
        test("Rec has skill", "skill" in rec, f"skill={rec.get('skill')}")
        test("Rec has demand_count", "demand_count" in rec)
        test("Rec has reason", "reason" in rec)
print()

# ─── 4. AI Smart Search ──────────────────────────────────────
print("─── 4. AI Smart Search (POST /api/ai/smart-search) ───")
code, r = api_call("POST", "/api/ai/smart-search", None, {
    "query": "tutoring jobs in Delhi paying over 500"
})
test("Endpoint exists", code == 200, f"HTTP {code}")
if code == 200:
    test("Has interpretation", bool(r.get("interpretation")), f"interp={r.get('interpretation','')[:60]}")
    test("Has ai_parsed", "ai_parsed" in r, f"ai_parsed={r.get('ai_parsed')}")
    test("Has jobs list", "jobs" in r, f"count={len(r.get('jobs', []))}")
    test("Has category", "category" in r)
    test("Has location", "location" in r)
print()

# ─── 5. AI Applicant Ranking ─────────────────────────────────
print("─── 5. AI Applicant Ranking (GET /api/ai/applicants/{job_id}) ───")
# First create a job to test with
code_job, job = api_call("POST", "/api/jobs", token, {
    "title": "Test Job for AI",
    "description": "Testing AI features",
    "pay_amount": 500,
    "location": "Test",
    "skills_required": "[\"python\"]",
    "is_urgent": False
})
if code_job == 201:
    job_id = job["id"]
    code, r = api_call("GET", f"/api/ai/applicants/{job_id}", token)
    test("Endpoint exists", code == 200, f"HTTP {code}")
    if code == 200:
        test("Returns list", isinstance(r, list), f"count={len(r)}")
else:
    test("Endpoint exists", False, f"Could not create test job: HTTP {code_job}")
print()

# ─── 6. AI Pay Estimator ─────────────────────────────────────
print("─── 6. AI Pay Estimator (POST /api/ai/estimate-pay) ───")
code, r = api_call("POST", "/api/ai/estimate-pay", None, {
    "category": "Tutoring",
    "location": "Hyderabad",
    "duration": "2 hours",
    "job_type": "one-time"
})
test("Endpoint exists", code == 200, f"HTTP {code}")
if code == 200:
    test("Has confidence", "confidence" in r, f"confidence={r.get('confidence')}")
    test("Has reasoning", "reasoning" in r, f"reasoning={r.get('reasoning','')[:60]}")
    test("Has sample_size", "sample_size" in r, f"sample_size={r.get('sample_size')}")
print()

# ─── 7. AI Smart Notifications ───────────────────────────────
print("─── 7. AI Smart Notifications ───")
code, r = api_call("GET", "/api/notifications", token)
test("Notifications endpoint exists", code == 200, f"HTTP {code}")
code2, r2 = api_call("GET", "/api/notifications/unread-count", token)
test("Unread count endpoint exists", code2 == 200, f"HTTP {code2}")
print("  ⚠️  Note: AI-targeted notifications on job post is partially implemented")
print()

# ─── 8. AI Earnings Insights ─────────────────────────────────
print("─── 8. AI Earnings Insights (GET /api/ai/earnings-insights) ───")
code, r = api_call("GET", "/api/ai/earnings-insights", token)
test("Endpoint exists", code == 200, f"HTTP {code}")
if code == 200:
    test("Has insights", "insights" in r, f"count={len(r.get('insights', []))}")
    test("Has tips", "tips" in r)
    test("Has projected_monthly", "projected_monthly" in r)
    test("Has best_category", "best_category" in r)
print()

# ─── 9. AI Application Note Generator ────────────────────────
print("─── 9. AI Application Note Generator (POST /api/ai/generate-application-note) ───")
# Need a job_id
jobs_code, jobs = api_call("GET", "/api/jobs")
if jobs_code == 200 and len(jobs) > 0:
    test_job_id = jobs[0]["id"]
    code, r = api_call("POST", "/api/ai/generate-application-note", token, {
        "job_id": test_job_id
    })
    test("Endpoint exists", code == 200, f"HTTP {code}")
    if code == 200:
        test("Has note", bool(r.get("note")), f"note={r.get('note','')[:80]}")
        test("Has ai_generated", "ai_generated" in r, f"ai_generated={r.get('ai_generated')}")
else:
    test("Endpoint exists", False, "No jobs to test with")
print()

# ─── 10. AI Match Explanation ─────────────────────────────────
print("─── 10. AI Match Explanation (GET /api/ai/match-explanation/{job_id}) ───")
if jobs_code == 200 and len(jobs) > 0:
    code, r = api_call("GET", f"/api/ai/match-explanation/{test_job_id}", token)
    test("Endpoint exists", code == 200, f"HTTP {code}")
    if code == 200:
        test("Has score", "score" in r)
        test("Has matched_skills", "matched_skills" in r)
        test("Has missing_skills", "missing_skills" in r)
        test("Has explanation", "explanation" in r, f"exp={r.get('explanation','')[:60]}")
else:
    test("Endpoint exists", False, "No jobs to test with")
print()

# ─── Summary ─────────────────────────────────────────────────
print("=" * 60)
passed = sum(1 for _, p, _ in results if p)
failed = sum(1 for _, p, _ in results if not p)
print(f"  RESULTS: {passed} passed, {failed} failed out of {len(results)} tests")
if failed == 0:
    print("  🎉 ALL BACKEND AI ENDPOINTS WORKING!")
else:
    print("  ⚠️  Some tests failed — see details above")
print("=" * 60)
