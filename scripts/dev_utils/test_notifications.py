"""Test AI Smart Notifications — verify students get notified on matching job posts."""
import urllib.request
import urllib.error
import json

BASE = "http://localhost:8000"

def api(method, path, token=None, body=None):
    url = f"{BASE}{path}"
    data = json.dumps(body).encode() if body else None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        resp = urllib.request.urlopen(req)
        return resp.status, json.loads(resp.read())
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode())

# Step 1: Create/login a student with Python skills
print("1️⃣  Creating student with Python skills...")
code, student = api("POST", "/api/login", body={"phone": "8888888888", "name": "AI Test Student"})
student_token = student["access_token"]
student_id = student["user"]["id"]
print(f"   Student ID={student_id}, Token={student_token[:20]}...")

# Update student's skills to include python, machine-learning, etc.
code, updated = api("PUT", "/api/profile", student_token, {
    "skills_json": '["python", "machine-learning", "data-analysis", "teaching"]'
})
print(f"   Updated skills: {updated.get('skills_json')}")

# Step 2: Check current notifications (before job post)
code, notifs_before = api("GET", "/api/notifications", student_token)
before_count = len(notifs_before) if isinstance(notifs_before, list) else 0
print(f"2️⃣  Notifications before: {before_count}")

# Step 3: Create employer and post a MATCHING job
print("3️⃣  Creating employer and posting a matching job...")
code, employer = api("POST", "/api/login", body={"phone": "7777777777", "name": "Test Employer"})
employer_token = employer["access_token"]

code, job = api("POST", "/api/jobs", employer_token, {
    "title": "Python Data Analysis Tutor",
    "description": "Teach Python and data analysis to college freshers",
    "pay_amount": 1500,
    "location": "Hyderabad",
    "skills_required": '["python", "data-analysis", "teaching"]',
    "is_urgent": True
})
print(f"   Job created: ID={job.get('id')}, title='{job.get('title')}'")

# Step 4: Check notifications AFTER job post
code, notifs_after = api("GET", "/api/notifications", student_token)
after_count = len(notifs_after) if isinstance(notifs_after, list) else 0
print(f"4️⃣  Notifications after: {after_count}")

new_notifs = after_count - before_count
if new_notifs > 0:
    print(f"\n✅ SUCCESS! {new_notifs} AI Smart Notification(s) received!")
    # Show the newest notification
    latest = notifs_after[0]
    print(f"   📢 Title: {latest['title']}")
    print(f"   📝 Message: {latest['message']}")
    print(f"   🏷️  Type: {latest['type']}")
    print(f"   🔗 Job ID: {latest.get('related_job_id')}")
else:
    print("\n❌ FAIL: No new notifications received")
    # Debug: show all notifications
    print("   All notifications:", json.dumps(notifs_after[:3], indent=2))
