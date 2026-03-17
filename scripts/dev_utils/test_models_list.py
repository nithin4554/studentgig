import os
import sys

# Set key
os.environ["GEMINI_API_KEY"] = "AIzaSyC8JE7Gf1IG0ktMgaQMORPrRZ9uHZrya4g"

try:
    from google import genai
    client = genai.Client()
    for m in client.models.list():
        print(m.name)

except Exception as e:
    print("ERROR:", e)
