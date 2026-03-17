import os
import sys

# Set key
os.environ["GEMINI_API_KEY"] = "AIzaSyC8JE7Gf1IG0ktMgaQMORPrRZ9uHZrya4g"

try:
    from google import genai
    from google.genai.errors import APIError
    client = genai.Client()
    try:
        response = client.models.generate_content(
            model='gemini-2.5-flash',
            contents='Say hello'
        )
        print("Response 2.5:", response.text)
    except APIError as e:
        print("2.5 ERROR:", e)
except Exception as e:
    print("FATAL ERROR", e)
