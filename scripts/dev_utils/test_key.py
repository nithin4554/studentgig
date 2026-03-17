import os
import sys

# Set key
os.environ["GEMINI_API_KEY"] = "AIzaSyC8JE7Gf1IG0ktMgaQMORPrRZ9uHZrya4g"

sys.path.append(os.path.join(os.path.dirname(__file__), 'backend'))
try:
    from backend.ai_llm import _ensure_gemini, _call_gemini
    
    print("Ensure Gemini:", _ensure_gemini())
    if _ensure_gemini():
        res = _call_gemini("Say hello")
        print("Gemini response:", res)
    else:
        print("ensure_gemini() returned False.")
except Exception as e:
    print("Error:", e)
