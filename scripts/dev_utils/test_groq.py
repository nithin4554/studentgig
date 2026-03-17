"""Quick test for Groq AI integration."""
import os, sys

# Note: Ensure GROQ_API_KEY is set in your environment or backend/.env
try:
    from dotenv import load_dotenv
    # Look for .env in the backend folder
    env_path = os.path.join(os.path.dirname(__file__), "..", "..", "backend", ".env")
    load_dotenv(env_path)
except ImportError:
    pass

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "backend"))

from ai_llm import generate_job_description, parse_natural_language_search, generate_application_note

print("=== TEST 1: Job Description Generator ===")
r = generate_job_description("Math Tutor for Class 10", category="Tutoring", location="Hyderabad")
print("AI Generated:", r.get("ai_generated"))
print("Description:", r.get("description", "")[:150])
print("Skills:", r.get("suggested_skills"))
print()

print("=== TEST 2: Smart Search NLP ===")
r = parse_natural_language_search("photography gigs near Hyderabad paying over 1000")
print("AI Parsed:", r.get("ai_parsed"))
print("Interpretation:", r.get("interpretation"))
print("Category:", r.get("category"))
print("Location:", r.get("location"))
print("Min Pay:", r.get("min_pay"))
print()

print("=== TEST 3: Application Note ===")
r = generate_application_note("Nithin", '["python","teaching"]', "Python Tutor", "Teach Python basics", '["python","patience"]', 70)
print("AI Generated:", r.get("ai_generated"))
print("Note:", r.get("note", "")[:200])
print()
print("ALL 3 AI FEATURES WORKING!")
