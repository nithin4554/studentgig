import os
import sys

# Set key
os.environ["GEMINI_API_KEY"] = "AIzaSyC8JE7Gf1IG0ktMgaQMORPrRZ9uHZrya4g"

try:
    from google import genai
    from google.genai import types
    from google.genai.errors import APIError
    client = genai.Client()
    try:
        response = client.models.generate_content(
            model='gemini-2.0-flash',
            contents='Say hello'
        )
        print("Response 2.0:", response.text)
    except APIError as e:
        print("2.0 ERROR:", e)
        
    try:
        response = client.models.generate_content(
            model='gemini-1.5-flash',
            contents='Say hello'
        )
        print("Response 1.5:", response.text)
    except APIError as e:
        print("1.5 ERROR:", e)

except ImportError:
    import google.generativeai as genai_old
    genai_old.configure()
    try:
        m = genai_old.GenerativeModel('gemini-2.0-flash')
        response = m.generate_content("Say hello")
        print("Response 2.0 (old):", response.text)
    except Exception as e:
        print("2.0 ERROR (old):", e)
