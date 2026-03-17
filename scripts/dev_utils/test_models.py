import urllib.request
import json
import sys

key = sys.argv[1]
for model in ['gemini-1.5-flash', 'gemini-1.5-flash-latest', 'gemini-pro']:
    print(f"Testing {model}")
    url = f'https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={key}'
    data = json.dumps({'contents':[{'parts':[{'text':'Say Hello'}]}]}).encode('utf-8')
    req = urllib.request.Request(url, data=data, headers={'Content-Type': 'application/json'})

    try:
        res = urllib.request.urlopen(req)
        print("SUCCESS!")
        print(res.read().decode('utf-8'))
        break
    except urllib.error.HTTPError as e:
        print(f"HTTP ERROR: {e.code}")
        print(e.read().decode('utf-8'))
    except Exception as e:
        print(f"OTHER ERROR: {e}")
