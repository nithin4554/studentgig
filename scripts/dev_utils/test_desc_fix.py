"""Test the AI job description generation after applying the fix."""
import urllib.request
import json
import time

print("Testing AI description output (3 calls)...")
print()

for i in range(3):
    data = json.dumps({'title': f'React Developer {i+1}', 'category': 'Tech', 'location': 'Remote'}).encode()
    req = urllib.request.Request(
        'http://localhost:8000/api/ai/generate-description',
        data=data,
        headers={'Content-Type': 'application/json'}
    )
    try:
        res = urllib.request.urlopen(req)
        r = json.loads(res.read())
        d = r.get('description', '')
        
        # Check if it starts with {"description" or similar JSON artifacts
        has_json = d.startswith('{') or '"description"' in d[:30]
        status = 'DIRTY' if has_json else 'CLEAN'
        icon = '❌' if has_json else '✅'
        
        preview = repr(d[:80])
        print(f"  {icon} Test {i+1}: [{status}] starts={preview}")
    except Exception as e:
        print(f"  ❌ ERR Test {i+1}: {e}")
    time.sleep(1.5)

print("\nDone! All descriptions should show CLEAN (no JSON artifacts)")
