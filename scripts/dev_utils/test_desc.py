"""Debug the AI description response."""
import urllib.request, json

data = json.dumps({
    'title': 'Math Tutor for Class 10',
    'category': 'Tutoring',
    'location': 'Hyderabad'
}).encode()

req = urllib.request.Request(
    'http://localhost:8000/api/ai/generate-description',
    data=data,
    headers={'Content-Type': 'application/json'}
)
res = urllib.request.urlopen(req)
r = json.loads(res.read())

print("=== RAW description field ===")
print(repr(r.get("description", "")[:500]))
print()
print("=== What user sees ===")
print(r.get("description", "")[:500])
