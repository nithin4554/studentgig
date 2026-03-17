import urllib.request
import json
import sys

key = sys.argv[1]
url = f'https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key={key}'
data = json.dumps({'contents':[{'parts':[{'text':'Say Hello'}]}]}).encode('utf-8')
req = urllib.request.Request(url, data=data, headers={'Content-Type': 'application/json'})

try:
    res = urllib.request.urlopen(req)
    print("SUCCESS!")
    print(res.read().decode('utf-8'))
except urllib.error.HTTPError as e:
    print(f"HTTP ERROR: {e.code}")
    print(e.read().decode('utf-8'))
except Exception as e:
    print(f"OTHER ERROR: {e}")
