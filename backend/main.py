"""
StudentGig API — FastAPI Entry Point (Stage 5: Full Lifecycle)
Run with: uvicorn main:app --reload --host 0.0.0.0 --port 8000

Features:
  - GET  /api/jobs                          → List jobs (+ AI match score if logged in)
  - GET  /api/jobs/search                   → Search/filter jobs
  - POST /api/jobs                          → Create a job
  - GET  /api/jobs/{id}                     → Single job detail
  - POST /api/login                         → Phone-based login, returns JWT
  - POST /api/auth/google                   → Google login, returns JWT
  - GET  /api/profile                       → Get current user profile
  - PUT  /api/profile                       → Update name / skills
  - POST /api/apply                         → Apply to a job (requires JWT)
  - GET  /api/my-applications               → See your applications with job details
  - PUT  /api/applications/{id}/status      → Update application status (lifecycle)
  - POST /api/applications/{id}/start-work  → Student starts working
  - POST /api/applications/{id}/complete    → Student marks work done
  - POST /api/applications/{id}/confirm-pay → Confirm payment received
  - GET  /api/earnings                      → Student earnings summary
"""

import sys
import io

# --- Disabled wrapping because it was causing crashes on reload ---
# if sys.platform == "win32":
#     try:
#         sys.stdout = io.TextIOWrapper(
#             sys.stdout.buffer, encoding="utf-8", errors="replace", line_buffering=True
#         )
#         sys.stderr = io.TextIOWrapper(
#             sys.stderr.buffer, encoding="utf-8", errors="replace", line_buffering=True
#         )
#     except Exception:
#         pass

import logging
import os
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException, Depends, Query, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from sqlalchemy.orm import Session
from sqlalchemy.sql import func
from sqlalchemy import text as text
from typing import List, Optional
from datetime import datetime, timezone
import jwt as pyjwt  # Pre-import for fast Google login

# --- Direct bcrypt hashing (Fixes passlib Windows incompatibility) ---
import bcrypt as bcrypt_lib

def hash_password(password: str) -> str:
    """Securely hash a password using bcrypt."""
    if not password:
        return ""
    salt = bcrypt_lib.gensalt()
    return bcrypt_lib.hashpw(password.encode('utf-8'), salt).decode('utf-8')

def verify_password(password: str, hashed_password: str) -> bool:
    """Verify a plain password against a bcrypt hash."""
    if not password or not hashed_password:
        return False
    try:
        return bcrypt_lib.checkpw(password.encode('utf-8'), hashed_password.encode('utf-8'))
    except Exception as e:
        logger.error(f"Password verification error: {e}")
        return False

# --- Logging setup ---
logging.getLogger().setLevel(logging.INFO)
logger = logging.getLogger("studentgig")

from database import engine, get_db, SessionLocal
from models import Base, Job as JobModel, User as UserModel, Application as AppModel, Payment as PaymentModel, Rating as RatingModel, Notification as NotifModel
from schemas import (
    JobCreate, JobResponse, JobUpdate, LoginRequest, RegisterRequest, TokenResponse, UserResponse,
    ApplicationCreate, ApplicationResponse, ApplicationDetailResponse,
    ProfileUpdate, GoogleLoginRequest, FirebaseLoginRequest, ApplicationStatusUpdate, EarningsResponse,
    ApplicantResponse, JOB_CATEGORIES, ConflictCheckResponse, PaymentResponse,
    RatingCreate, RatingResponse, NotificationResponse, NotificationCountResponse,
    ResetPasswordCheck, ResetPasswordFinal,
    # AI Schemas
    AIJobResponse, AISkillRecommendationsResponse, AIApplicantResponse,
    AIPayEstimateRequest, AIPayEstimateResponse,
    AIGenerateDescriptionRequest, AIGenerateDescriptionResponse,
    AISmartSearchRequest, AISmartSearchResponse,
    DemoLoginRequest,
    AIApplicationNoteRequest, AIApplicationNoteResponse,
    AIMatchExplanationResponse, AIEarningsInsightsResponse,
)
from auth import create_access_token, get_current_user, get_optional_user
from passlib.hash import bcrypt
from ai_engine import (
    calculate_match_score, calculate_match_explanation,
    compute_smart_feed_score, recommend_skills,
    rank_applicant, estimate_pay, analyze_earnings,
)
from ai_llm import (
    generate_job_description, parse_natural_language_search,
    generate_application_note,
)

# ─── Firebase Setup ──────────────────────────────────────────────────────────────
import firebase_admin
from firebase_admin import credentials as firebase_credentials, auth as firebase_auth

try:
    if not firebase_admin._apps:
        # Check for service account key in backend folder (use __file__-relative path)
        _backend_dir = os.path.dirname(os.path.abspath(__file__))
        cred_path = os.path.join(_backend_dir, "serviceAccountKey.json")
        if os.path.exists(cred_path):
            cred = firebase_credentials.Certificate(cred_path)
            firebase_admin.initialize_app(cred)
            logger.info(f"Firebase initialized with service account key from {cred_path}")
        else:
            # Fallback to default credentials (works if GOOGLE_APPLICATION_CREDENTIALS is set)
            firebase_admin.initialize_app()
            logger.info("Firebase initialized with default credentials")
except Exception as e:
    logger.warning(f"Firebase initialization failed: {e}")

# ─── Create all tables on startup ────────────────────────────────────────────────
Base.metadata.create_all(bind=engine)

# ─── Auto-migration: Add missing columns to existing tables ──────────────────────
def migrate_database():
    """Add new columns that don't exist yet (safe to run multiple times)."""
    from sqlalchemy import text, inspect as sa_inspect
    with engine.connect() as conn:
        # Helper: check if column exists using SQLAlchemy inspect (safe — no raw SQL)
        inspector = sa_inspect(engine)
        def column_exists(table, column):
            try:
                columns = [c["name"] for c in inspector.get_columns(table)]
                return column in columns
            except Exception:
                return False

        migrations = [
            # Jobs table — original columns
            ("jobs", "employer_id", "ALTER TABLE jobs ADD COLUMN employer_id INT NULL"),
            ("jobs", "max_applicants", "ALTER TABLE jobs ADD COLUMN max_applicants INT DEFAULT 1"),
            ("jobs", "deadline", "ALTER TABLE jobs ADD COLUMN deadline DATETIME NULL"),
            # Jobs table — new real-posting columns
            ("jobs", "company_name", "ALTER TABLE jobs ADD COLUMN company_name VARCHAR(255) NULL"),
            ("jobs", "category", "ALTER TABLE jobs ADD COLUMN category VARCHAR(100) NULL"),
            ("jobs", "job_type", "ALTER TABLE jobs ADD COLUMN job_type VARCHAR(50) DEFAULT 'one-time'"),
            ("jobs", "duration", "ALTER TABLE jobs ADD COLUMN duration VARCHAR(100) NULL"),
            ("jobs", "status", "ALTER TABLE jobs ADD COLUMN status VARCHAR(50) DEFAULT 'open'"),
            ("jobs", "contact_info", "ALTER TABLE jobs ADD COLUMN contact_info VARCHAR(255) NULL"),
            # Phase 1: Scheduling
            ("jobs", "job_date", "ALTER TABLE jobs ADD COLUMN job_date VARCHAR(10) NULL"),
            ("jobs", "start_time", "ALTER TABLE jobs ADD COLUMN start_time VARCHAR(5) NULL"),
            ("jobs", "end_time", "ALTER TABLE jobs ADD COLUMN end_time VARCHAR(5) NULL"),
            ("jobs", "address", "ALTER TABLE jobs ADD COLUMN address TEXT NULL"),
            # Users table — new columns
            ("users", "total_earned", "ALTER TABLE users ADD COLUMN total_earned DECIMAL(10,2) DEFAULT 0.00"),
            ("users", "gigs_completed", "ALTER TABLE users ADD COLUMN gigs_completed INT DEFAULT 0"),
            ("users", "rating", "ALTER TABLE users ADD COLUMN rating DECIMAL(3,2) DEFAULT 0.00"),
            # Applications table — lifecycle timestamps
            ("applications", "accepted_at", "ALTER TABLE applications ADD COLUMN accepted_at DATETIME NULL"),
            ("applications", "started_at", "ALTER TABLE applications ADD COLUMN started_at DATETIME NULL"),
            ("applications", "completed_at", "ALTER TABLE applications ADD COLUMN completed_at DATETIME NULL"),
            ("applications", "paid_at", "ALTER TABLE applications ADD COLUMN paid_at DATETIME NULL"),
            ("applications", "employer_note", "ALTER TABLE applications ADD COLUMN employer_note TEXT NULL"),
            ("applications", "rating", "ALTER TABLE applications ADD COLUMN rating INT NULL"),
            # Phase 2: Two-Sided Confirmation
            ("applications", "checked_in_at", "ALTER TABLE applications ADD COLUMN checked_in_at DATETIME NULL"),
            ("applications", "work_done_at", "ALTER TABLE applications ADD COLUMN work_done_at DATETIME NULL"),
            ("applications", "confirmed_at", "ALTER TABLE applications ADD COLUMN confirmed_at DATETIME NULL"),
            # Authentication: Passwords & Recovery
            ("users", "hashed_password", "ALTER TABLE users ADD COLUMN hashed_password VARCHAR(255) NULL"),
            ("users", "security_question", "ALTER TABLE users ADD COLUMN security_question VARCHAR(255) NULL"),
            ("users", "hashed_security_answer", "ALTER TABLE users ADD COLUMN hashed_security_answer VARCHAR(255) NULL"),
            ("users", "role", "ALTER TABLE users ADD COLUMN role VARCHAR(50) DEFAULT 'student'"),
        ]

        applied = 0
        for table, column, sql in migrations:
            if not column_exists(table, column):
                try:
                    conn.execute(text(sql))
                    conn.commit()
                    applied += 1
                    logger.info(f"  [OK] Added column: {table}.{column}")
                except Exception as e:
                    logger.warning(f"  Migration skipped ({table}.{column}): {e}")
                    conn.rollback()

        if applied > 0:
            logger.info(f"[MIGRATE] Database migration complete - {applied} columns added")
        else:
            logger.info("[OK] Database schema up-to-date - no migration needed")

        # Ensure phone column is wide enough for emails (Google login)
        try:
            result = conn.execute(text(
                "SELECT CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS "
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'phone'"
            ))
            row = result.fetchone()
            if row and row[0] and row[0] < 255:
                conn.execute(text("ALTER TABLE users MODIFY COLUMN phone VARCHAR(255) NOT NULL"))
                conn.commit()
                logger.info(f"  [OK] Widened users.phone from VARCHAR({row[0]}) to VARCHAR(255)")
        except Exception as e:
            logger.warning(f"  Phone column check skipped: {e}")
            conn.rollback()

try:
    migrate_database()
except Exception as e:
    logger.warning(f"Migration check failed (tables may not exist yet): {e}")


# ─── Lifespan — modern startup/shutdown hook ─────────────────────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    """Startup: seed database if empty. Shutdown: cleanup."""
    _seed_database()
    yield
    # Shutdown cleanup (if needed in future)


app = FastAPI(
    title="StudentGig API",
    description="Backend for StudentGig — Auth, Jobs, Applications, Full Lifecycle, AI Matching.",
    version="0.5.1",
    lifespan=lifespan,
)

_cors_origins_raw = os.environ.get("CORS_ORIGINS", "")
_cors_origins = [o.strip() for o in _cors_origins_raw.split(",") if o.strip()]
# Always allow local web dev and local IP for mobile dev
_cors_origins.extend(["http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:3000"])

if not _cors_origins_raw:
    logger.warning("⚠️  CORS using developer defaults (localhost:5173/3000)")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ─── Rate Limiting ───────────────────────────────────────────────────────────────
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded

limiter = Limiter(key_func=get_remote_address)
app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)


# ─── Global Exception Handler — prevents server crash on unhandled errors ────────
from fastapi.exceptions import RequestValidationError
from starlette.exceptions import HTTPException as StarletteHTTPException

@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    """
    Catch-all handler: any unhandled exception returns a clean 500 JSON.
    We EXCLUDE HTTPException and RequestValidationError so standard FastAPI
    handlers can return 400/401/422 with useful detail messages.
    """
    if isinstance(exc, (StarletteHTTPException, RequestValidationError)):
        # Re-raise to let FastAPI's specialized handlers manage these
        raise exc

    logger.error(f"Unhandled error on {request.method} {request.url.path}: {exc}", exc_info=True)
    response = JSONResponse(
        status_code=500,
        content={"detail": "Internal server error. Please try again."},
    )
    # Manual CORS fallback for unhandled exceptions
    response.headers["Access-Control-Allow-Origin"] = "*"
    return response


# ─── SEED DATA ───────────────────────────────────────────────────────────────────

SEED_JOBS = [
    {
        "title": "Cricket Tournament Volunteer",
        "description": "Help organize the inter-college cricket tournament. Duties include crowd management, scorekeeping, and refreshment distribution.",
        "pay_amount": 500.00,
        "location": "Hyderabad",
        "skills_required": '["communication", "hindi", "teamwork"]',
        "is_urgent": True,
    },
    {
        "title": "College Fest Photographer",
        "description": "Capture high-quality photos of cultural events, performances, and candid moments during the 3-day annual fest.",
        "pay_amount": 1500.00,
        "location": "Bangalore",
        "skills_required": '["photography", "editing", "english"]',
        "is_urgent": False,
    },
    {
        "title": "Tiffin Delivery — Hostel Area",
        "description": "Deliver fresh tiffin boxes from kitchen to hostel rooms. Morning and evening shifts available. Bicycle provided.",
        "pay_amount": 300.00,
        "location": "Pune",
        "skills_required": '["bicycle", "punctuality"]',
        "is_urgent": True,
    },
    {
        "title": "Math Tutor (Class 10)",
        "description": "Teach CBSE Class 10 Mathematics to a group of 5 students. 2 hours daily, flexible timing. Must know NCERT curriculum.",
        "pay_amount": 800.00,
        "location": "Delhi",
        "skills_required": '["mathematics", "teaching", "patience"]',
        "is_urgent": False,
    },
    {
        "title": "Social Media Intern — Local Café",
        "description": "Create and manage Instagram content for a popular local café. 3 reels + 5 stories per week. Canva skills preferred.",
        "pay_amount": 2000.00,
        "location": "Mumbai",
        "skills_required": '["instagram", "canva", "content-writing"]',
        "is_urgent": False,
    },
    {
        "title": "Campus Ambassador — EdTech",
        "description": "Promote an online learning platform across your college. Organize workshops, distribute materials, and drive sign-ups.",
        "pay_amount": 1200.00,
        "location": "Hyderabad",
        "skills_required": '["marketing", "communication", "leadership"]',
        "is_urgent": False,
    },
    {
        "title": "Data Entry Operator",
        "description": "Enter student records into an Excel spreadsheet. Must be accurate and fast. Work from home, 3-hour shift daily.",
        "pay_amount": 400.00,
        "location": "Remote",
        "skills_required": '["excel", "typing", "attention-to-detail"]',
        "is_urgent": True,
    },
    {
        "title": "Event Anchor — Fresher's Party",
        "description": "Host the freshman welcome party. Engage the audience, announce performances, and keep the energy high!",
        "pay_amount": 1000.00,
        "location": "Chennai",
        "skills_required": '["public-speaking", "english", "hindi", "humor"]',
        "is_urgent": True,
    },
    {
        "title": "Graphic Designer — Posters",
        "description": "Design posters and social media creatives for college events. Proficiency with Canva or Figma required.",
        "pay_amount": 700.00,
        "location": "Bangalore",
        "skills_required": '["canva", "figma", "design"]',
        "is_urgent": False,
    },
    {
        "title": "Python Tutor — Online",
        "description": "Teach Python basics to a group of 8 beginners via Zoom. 1-hour sessions, 3 times a week. Must know OOP and file handling.",
        "pay_amount": 1500.00,
        "location": "Remote",
        "skills_required": '["python", "teaching", "patience", "zoom"]',
        "is_urgent": False,
    },
]


def _seed_database():
    """Insert seed jobs if the table is empty (first run only)."""
    db = SessionLocal()
    try:
        if db.query(JobModel).count() == 0:
            for job_data in SEED_JOBS:
                db.add(JobModel(**job_data))
            db.commit()
            logger.info(f"Seeded {len(SEED_JOBS)} jobs into database")
        else:
            logger.info(f"Database already has {db.query(JobModel).count()} jobs")
    except Exception as e:
        logger.error(f"Seed error: {e}", exc_info=True)
        db.rollback()
    finally:
        db.close()


# ─── Health Check ────────────────────────────────────────────────────────────────

@app.get("/", tags=["Health"])
async def root():
    # Verify DB connectivity
    db_status = "connected"
    try:
        db = SessionLocal()
        db.execute(text("SELECT 1"))
        db.close()
    except Exception:
        db_status = "disconnected"
    return {
        "status": "online",
        "service": "StudentGig API",
        "version": "0.5.1",
        "db": db_status
    }


# ═══════════════════════════════════════════════════════════════════════════════════
#  JOBS — with optional AI match scoring + search
# ═══════════════════════════════════════════════════════════════════════════════════

def _job_to_dict(job, match_score=None, db=None):
    """Convert a Job model to a response dict with all new fields."""
    # Get employer name if available
    employer_name = None
    applicant_count = None
    if db:
        if job.employer_id:
            employer = db.query(UserModel).filter(UserModel.id == job.employer_id).first()
            if employer:
                employer_name = employer.name
        applicant_count = db.query(AppModel).filter(AppModel.job_id == job.id).count()

    return {
        "id": job.id,
        "title": job.title,
        "description": job.description,
        "pay_amount": float(job.pay_amount),
        "location": job.location,
        "skills_required": job.skills_required,
        "is_urgent": job.is_urgent,
        "created_at": job.created_at,
        "match_score": match_score,
        "employer_id": job.employer_id,
        "max_applicants": job.max_applicants or 1,
        "company_name": job.company_name,
        "category": job.category,
        "job_type": getattr(job, 'job_type', 'one-time') or 'one-time',
        "duration": job.duration,
        "status": getattr(job, 'status', 'open') or 'open',
        "contact_info": job.contact_info,
        "employer_name": employer_name,
        "applicant_count": applicant_count,
        # Phase 1: Scheduling
        "job_date": getattr(job, 'job_date', None),
        "start_time": getattr(job, 'start_time', None),
        "end_time": getattr(job, 'end_time', None),
        "address": getattr(job, 'address', None),
    }


def _score_job(job, user, db):
    """Calculate AI match score for a job given the current user."""
    if user and user.skills_json:
        return calculate_match_score(user.skills_json, job.skills_required)
    return None


def _compute_trust_badge(rating, gigs_completed):
    """Phase 4: Compute trust badge based on user stats."""
    r = float(rating or 0)
    g = int(gigs_completed or 0)
    if r >= 4.5 and g >= 3:
        return "⭐ Trusted"
    elif g >= 10:
        return "⚡ Fast"
    elif g == 0:
        return "🆕 New"
    return None


def _user_response_dict(user):
    """Build a UserResponse dict with trust badge."""
    return {
        "id": user.id,
        "phone": user.phone,
        "name": user.name,
        "skills_json": user.skills_json,
        "role": user.role or "student",
        "total_earned": float(user.total_earned or 0),
        "gigs_completed": user.gigs_completed or 0,
        "rating": float(user.rating or 0),
        "trust_badge": _compute_trust_badge(user.rating, user.gigs_completed),
    }


@app.get("/api/jobs", response_model=List[JobResponse], tags=["Jobs"])
def get_jobs(
    skip: int = Query(0, description="Number of records to skip for pagination"),
    limit: int = Query(50, description="Maximum number of records to return"),
    category: Optional[str] = Query(None, description="Filter by category"),
    db: Session = Depends(get_db),
    current_user: Optional[dict] = Depends(get_optional_user),
):
    """Retrieve all open jobs. If user is logged in, each job gets an AI match_score."""
    query = db.query(JobModel)

    # Only show open jobs to seekers (unless they own the job)
    query = query.filter(
        (JobModel.status == None) | (JobModel.status == "open") | (JobModel.status == "")
    )

    if category:
        query = query.filter(JobModel.category == category)

    user = None
    if current_user:
        user_id = int(current_user["sub"])
        # Exclude jobs posted by the current user
        query = query.filter((JobModel.employer_id != user_id) | (JobModel.employer_id == None))
        user = db.query(UserModel).filter(UserModel.id == user_id).first()

    jobs = query.order_by(JobModel.created_at.desc()).offset(skip).limit(limit).all()

    return [_job_to_dict(j, _score_job(j, user, db), db) for j in jobs]


@app.get("/api/jobs/categories", tags=["Jobs"])
def get_job_categories():
    """Get the list of available job categories."""
    return {"categories": JOB_CATEGORIES}


@app.get("/api/jobs/search", response_model=List[JobResponse], tags=["Jobs"])
def search_jobs(
    q: Optional[str] = Query(None, description="Search query for title/description"),
    location: Optional[str] = Query(None, description="Filter by location"),
    min_pay: Optional[float] = Query(None, description="Minimum pay amount"),
    max_pay: Optional[float] = Query(None, description="Maximum pay amount"),
    urgent_only: bool = Query(False, description="Only show urgent jobs"),
    category: Optional[str] = Query(None, description="Filter by category"),
    skip: int = Query(0, description="Number of records to skip for pagination"),
    limit: int = Query(50, description="Maximum number of records to return"),
    db: Session = Depends(get_db),
    current_user: Optional[dict] = Depends(get_optional_user),
):
    """Search and filter jobs."""
    query = db.query(JobModel)

    # Only show open jobs
    query = query.filter(
        (JobModel.status == None) | (JobModel.status == "open") | (JobModel.status == "")
    )

    if q:
        search = f"%{q}%"
        query = query.filter(
            (JobModel.title.ilike(search)) | (JobModel.description.ilike(search))
        )
    if location:
        query = query.filter(JobModel.location.ilike(f"%{location}%"))
    if min_pay is not None:
        query = query.filter(JobModel.pay_amount >= min_pay)
    if max_pay is not None:
        query = query.filter(JobModel.pay_amount <= max_pay)
    if urgent_only:
        query = query.filter(JobModel.is_urgent == True)
    if category:
        query = query.filter(JobModel.category == category)

    user = None
    if current_user:
        user_id = int(current_user["sub"])
        # Exclude jobs posted by the current user
        query = query.filter((JobModel.employer_id != user_id) | (JobModel.employer_id == None))
        user = db.query(UserModel).filter(UserModel.id == user_id).first()

    jobs = query.order_by(JobModel.created_at.desc()).offset(skip).limit(limit).all()

    return [_job_to_dict(j, _score_job(j, user, db), db) for j in jobs]


@app.post("/api/jobs", response_model=JobResponse, status_code=201, tags=["Jobs"])
def create_job(
    job: JobCreate,
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """
    Create a new job posting. Requires JWT authentication.
    Automatically sets employer_id from the logged-in user.
    """
    user_id = int(current_user["sub"])
    user = db.query(UserModel).filter(UserModel.id == user_id).first()

    # ─── Role check: Only employers can post jobs ────────────────────────
    if user and user.role == "student":
        raise HTTPException(status_code=403, detail="Only employers can post jobs")

    db_job = JobModel(
        title=job.title,
        description=job.description,
        pay_amount=job.pay_amount,
        location=job.location,
        skills_required=job.skills_required,
        is_urgent=job.is_urgent,
        employer_id=user_id,
        company_name=job.company_name or (user.name if user else None),
        category=job.category,
        job_type=job.job_type,
        duration=job.duration,
        max_applicants=job.max_applicants,
        contact_info=job.contact_info,
        status="open",
        # Phase 1: Scheduling
        job_date=job.job_date,
        start_time=job.start_time,
        end_time=job.end_time,
        address=job.address,
        deadline=job.deadline,
    )
    db.add(db_job)
    db.commit()
    db.refresh(db_job)

    logger.info(f"User {user_id} posted new job: '{job.title}' (ID={db_job.id})")

    # ─── AI Smart Notifications: Notify high-match students ──────────────
    try:
        _notify_matching_students(db, db_job, employer_id=user_id)
    except Exception as e:
        logger.warning(f"Smart notification failed (non-critical): {e}")

    return _job_to_dict(db_job, db=db)


@app.get("/api/my-jobs", response_model=List[JobResponse], tags=["Jobs"])
def get_my_jobs(
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """Get all jobs posted by the current user."""
    user_id = int(current_user["sub"])
    jobs = db.query(JobModel).filter(
        JobModel.employer_id == user_id
    ).order_by(JobModel.created_at.desc()).all()

    return [_job_to_dict(j, db=db) for j in jobs]


@app.put("/api/jobs/{job_id}", response_model=JobResponse, tags=["Jobs"])
def update_job(
    job_id: int,
    update: JobUpdate,
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """Update your own job posting. Only the employer who created it can edit."""
    user_id = int(current_user["sub"])
    job = db.query(JobModel).filter(JobModel.id == job_id).first()
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")
    if job.employer_id != user_id:
        raise HTTPException(status_code=403, detail="Not authorized to edit this job")

    # Update only provided fields
    for field, value in update.model_dump(exclude_unset=True).items():
        if value is not None:
            setattr(job, field, value)

    db.commit()
    db.refresh(job)
    logger.info(f"User {user_id} updated job {job_id}")
    return _job_to_dict(job, db=db)


@app.delete("/api/jobs/{job_id}", tags=["Jobs"])
def delete_job(
    job_id: int,
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """Close/delete your own job posting. Sets status to 'closed'."""
    user_id = int(current_user["sub"])
    job = db.query(JobModel).filter(JobModel.id == job_id).first()
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")
    if job.employer_id != user_id:
        raise HTTPException(status_code=403, detail="Not authorized to delete this job")

    job.status = "closed"
    db.commit()
    logger.info(f"User {user_id} closed job {job_id}")
    return {"message": "Job closed successfully", "job_id": job_id}


@app.get("/api/jobs/{job_id}/applicants", response_model=List[ApplicantResponse], tags=["Jobs"])
def get_job_applicants(
    job_id: int,
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """Get all applicants for a job. Only the employer who posted it can view."""
    user_id = int(current_user["sub"])
    job = db.query(JobModel).filter(JobModel.id == job_id).first()
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")
    if job.employer_id != user_id:
        raise HTTPException(status_code=403, detail="Not authorized to view applicants")

    applications = db.query(AppModel).filter(AppModel.job_id == job_id).order_by(AppModel.applied_at.desc()).all()

    # Batch-fetch all applicant users in ONE query (fixes N+1)
    user_ids = list({app_entry.user_id for app_entry in applications})
    users_map = {}
    if user_ids:
        users = db.query(UserModel).filter(UserModel.id.in_(user_ids)).all()
        users_map = {u.id: u for u in users}

    result = []
    for app_entry in applications:
        applicant = users_map.get(app_entry.user_id)
        score = None
        if applicant and applicant.skills_json:
            score = calculate_match_score(applicant.skills_json, job.skills_required)
        result.append({
            "application_id": app_entry.id,
            "user_id": app_entry.user_id,
            "user_name": applicant.name if applicant else "Unknown",
            "user_phone": applicant.phone if applicant else "Unknown",
            "status": app_entry.status,
            "applied_at": app_entry.applied_at,
            "user_skills": applicant.skills_json if applicant else None,
            "match_score": score,
            "user_rating": float(applicant.rating or 0) if applicant else 0.0,
            "user_gigs_completed": (applicant.gigs_completed or 0) if applicant else 0,
            "trust_badge": _compute_trust_badge(applicant.rating, applicant.gigs_completed) if applicant else None,
        })

    return result


@app.get("/api/jobs/{job_id}/check-conflict", response_model=ConflictCheckResponse, tags=["Jobs"])
def check_schedule_conflict(
    job_id: int,
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """
    Check if the current user has a schedule conflict with this job.
    Conflict = user already has an accepted/in_progress application
    on the SAME date with OVERLAPPING time.
    """
    user_id = int(current_user["sub"])
    target_job = db.query(JobModel).filter(JobModel.id == job_id).first()
    if not target_job:
        raise HTTPException(status_code=404, detail="Job not found")

    # If job has no date, it's flexible — no conflict possible
    target_date = getattr(target_job, 'job_date', None)
    if not target_date:
        return {"has_conflict": False, "message": "This is a flexible job — no schedule conflict"}

    target_start = getattr(target_job, 'start_time', None)
    target_end = getattr(target_job, 'end_time', None)

    # Get all user's active applications (accepted or in_progress)
    active_apps = db.query(AppModel).filter(
        AppModel.user_id == user_id,
        AppModel.status.in_(["accepted", "in_progress", "checked_in"]),
    ).all()

    for app_entry in active_apps:
        other_job = db.query(JobModel).filter(JobModel.id == app_entry.job_id).first()
        if not other_job:
            continue

        other_date = getattr(other_job, 'job_date', None)
        if not other_date or other_date != target_date:
            continue  # Different date — no conflict

        # Same date — check time overlap
        other_start = getattr(other_job, 'start_time', None)
        other_end = getattr(other_job, 'end_time', None)

        # If either job has no time, assume full-day conflict on same date
        if not target_start or not other_start:
            return {
                "has_conflict": True,
                "conflicting_job_title": other_job.title,
                "conflicting_time": f"{other_date} (all day)",
                "message": f"You already have '{other_job.title}' on {other_date}",
            }

        # Time overlap check: new_start < existing_end AND new_end > existing_start
        t_start = target_start or "00:00"
        t_end = target_end or "23:59"
        o_start = other_start or "00:00"
        o_end = other_end or "23:59"

        if t_start < o_end and t_end > o_start:
            return {
                "has_conflict": True,
                "conflicting_job_title": other_job.title,
                "conflicting_time": f"{other_date} {o_start}-{o_end}",
                "message": f"Time conflict with '{other_job.title}' ({o_start}-{o_end})",
            }

    return {"has_conflict": False, "message": "No schedule conflict"}


@app.get("/api/jobs/{job_id}", response_model=JobResponse, tags=["Jobs"])
def get_job(
    job_id: int, 
    db: Session = Depends(get_db),
    current_user: Optional[dict] = Depends(get_optional_user)
):
    """Get a single job by ID. Includes AI match score if logged in."""
    job = db.query(JobModel).filter(JobModel.id == job_id).first()
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")
    
    user = None
    if current_user:
        user = db.query(UserModel).filter(
            UserModel.id == int(current_user["sub"])
        ).first()

    return _job_to_dict(job, _score_job(job, user, db), db)


# Legacy endpoint
@app.get("/jobs", response_model=List[JobResponse], tags=["Jobs (Legacy)"])
def get_jobs_legacy(db: Session = Depends(get_db)):
    return [_job_to_dict(j, db=db) for j in db.query(JobModel).all()]


# ═══════════════════════════════════════════════════════════════════════════════════
#  AUTH — Secure Password-Based Login & Registration
# ═══════════════════════════════════════════════════════════════════════════════════

@app.post("/api/register", response_model=TokenResponse, tags=["Auth"])
def register(body: RegisterRequest, db: Session = Depends(get_db)):
    """Register a new user with password and security recovery questions."""
    logger.info(f"--- [REGISTER START] Phone: {body.phone} ---")
    existing_user = db.query(UserModel).filter(UserModel.phone == body.phone).first()
    if existing_user:
        raise HTTPException(status_code=400, detail="User with this phone number already exists.")

    # Truncate strings to avoid bcrypt 72-byte limit (prevents ValueError)
    safe_pwd = body.password[:72]
    safe_ans = body.security_answer.strip().lower()[:72]

    user = UserModel(
        phone=body.phone,
        name=body.name,
        hashed_password=hash_password(safe_pwd),
        security_question=body.security_question,
        hashed_security_answer=hash_password(safe_ans),
        role=body.role
    )
    db.add(user)
    db.commit()
    db.refresh(user)

    logger.info(f"New user registered: {user.phone} ({user.name})")
    access_token = create_access_token(user_id=user.id, phone=user.phone)
    return {"access_token": access_token, "token_type": "bearer", "user": _user_response_dict(user)}


@app.post("/api/login", response_model=TokenResponse, tags=["Auth"])
@limiter.limit("10/minute")
def login(body: LoginRequest, request: Request, db: Session = Depends(get_db)):
    """Secure password-based login. Verifies hashed password."""
    user = db.query(UserModel).filter(UserModel.phone == body.phone).first()
    if not user:
        raise HTTPException(status_code=401, detail="User not found. Please register.")

    if not user.hashed_password:
        raise HTTPException(status_code=403, detail="Legacy account detected. Use 'Forgot Password'.")

    # Verify hashed password
    if not verify_password(body.password[:72], user.hashed_password):
        raise HTTPException(status_code=401, detail="Invalid password.")

    access_token = create_access_token(user_id=user.id, phone=user.phone)
    logger.info(f"User logged in: {user.phone}")
    return {"access_token": access_token, "token_type": "bearer", "user": _user_response_dict(user)}


@app.post("/api/auth/reset-get-question", tags=["Auth"])
def get_security_question(body: ResetPasswordCheck, db: Session = Depends(get_db)):
    user = db.query(UserModel).filter(UserModel.phone == body.phone).first()
    if not user or not user.security_question:
        raise HTTPException(status_code=404, detail="User not found or no recovery setup.")
    return {"question": user.security_question}


@app.post("/api/auth/reset-password", tags=["Auth"])
def reset_password(body: ResetPasswordFinal, db: Session = Depends(get_db)):
    user = db.query(UserModel).filter(UserModel.phone == body.phone).first()
    if not user or not user.hashed_security_answer:
        raise HTTPException(status_code=404, detail="Recovery not possible.")

    if not verify_password(body.security_answer.strip().lower()[:72], user.hashed_security_answer):
        raise HTTPException(status_code=401, detail="Incorrect security answer.")

    user.hashed_password = hash_password(body.new_password[:72])
    db.commit()
    logger.info(f"User {user.phone} reset password via security question.")
    return {"message": "Password reset successful."}



@app.post("/api/auth/google", response_model=TokenResponse, tags=["Auth"])
@limiter.limit("5/minute")
def google_login(body: GoogleLoginRequest, request: Request, db: Session = Depends(get_db)):
    """
    Google-based login. Verifies the ID token signature against Google's public keys.
    Creates user if new, returns JWT token.
    """
    from google.oauth2 import id_token as google_id_token
    from google.auth.transport import requests as google_requests

    GOOGLE_CLIENT_ID = os.environ.get("GOOGLE_CLIENT_ID", "")

    # Step 1: Verify the Google ID token
    try:
        if GOOGLE_CLIENT_ID:
            # Production: Full signature + audience verification
            payload = google_id_token.verify_oauth2_token(
                body.idToken,
                google_requests.Request(),
                GOOGLE_CLIENT_ID
            )
        else:
            # Local dev fallback (no client ID configured) — unsafe but functional
            logger.warning("⚠️  GOOGLE_CLIENT_ID not set — skipping token signature verification!")
            payload = pyjwt.decode(
                body.idToken,
                options={"verify_signature": False},
                algorithms=["RS256"]
            )
    except ValueError as e:
        logger.error(f"Google token verification failed: {e}")
        raise HTTPException(status_code=401, detail=f"Invalid Google Token: {e}")
    except Exception as e:
        logger.error(f"Google token decode failed: {e}")
        raise HTTPException(status_code=401, detail=f"Invalid Google Token: {e}")
    
    email = payload.get("email")
    name = payload.get("name", "Google User")
    
    if not email:
        raise HTTPException(status_code=400, detail="Invalid token: no email found")
    
    logger.info(f"Google login attempt for: {email}")
    
    # Step 2: Find or create user (robust upsert — handles stale sessions)
    from sqlalchemy.exc import IntegrityError
    
    # Expire cached objects so we get fresh data from DB
    db.expire_all()
    
    user = db.query(UserModel).filter(UserModel.phone == email).first()
    if user:
        # Returning user — update name if changed
        if name and user.name != name:
            user.name = name
            db.commit()
        logger.info(f"Returning Google user: {email} (ID={user.id})")
    else:
        # New user — try to insert
        try:
            user = UserModel(phone=email, name=name, role=body.role)
            db.add(user)
            db.commit()
            db.refresh(user)
            logger.info(f"New Google user registered: {email} (ID={user.id})")
        except IntegrityError:
            # Duplicate key — user was created between our SELECT and INSERT
            db.rollback()
            db.expire_all()
            user = db.query(UserModel).filter(UserModel.phone == email).first()
            if not user:
                logger.error(f"Cannot find user after IntegrityError: {email}")
                raise HTTPException(status_code=500, detail="User creation failed")
            logger.info(f"Found existing Google user after retry: {email} (ID={user.id})")
        except Exception as e:
            db.rollback()
            logger.error(f"Google login DB error: {e}", exc_info=True)
            raise HTTPException(status_code=500, detail=f"Database error: {e}")
        
    # Step 3: Generate JWT
    token = create_access_token(user_id=user.id, phone=user.phone)
    
    return {
        "access_token": token,
        "token_type": "bearer",
        "user": _user_response_dict(user),
    }

@app.post("/api/auth/firebase", response_model=TokenResponse, tags=["Auth"])
@limiter.limit("5/minute")
def firebase_login(body: FirebaseLoginRequest, request: Request, db: Session = Depends(get_db)):
    """
    Firebase-based login. Verifies the Firebase ID token.
    Creates user if new, returns JWT token.
    """
    try:
        # 1. Verify token with Firebase
        _fb_key = os.path.join(os.path.dirname(os.path.abspath(__file__)), "serviceAccountKey.json")
        if os.path.exists(_fb_key):
            decoded_token = firebase_auth.verify_id_token(body.idToken)
        else:
            logger.warning("⚠️  Firebase serviceAccountKey.json missing — skipping token signature verification!")
            decoded_token = pyjwt.decode(
                body.idToken,
                options={"verify_signature": False},
                algorithms=["RS256"]
            )
            
        uid = decoded_token.get('uid')
        phone = decoded_token.get('phone_number')
        
        if not phone:
            raise HTTPException(status_code=400, detail="Firebase token has no phone_number")
            
        # Clean phone number (Firebase returns it in E.164 format e.g., +919876543210)
        # Assuming we just store the digits here or keep the + (depends on DB schema, keeping it for now)
            
    except Exception as e:
        logger.error(f"Firebase token verification failed: {e}")
        raise HTTPException(status_code=401, detail=f"Invalid Firebase Token: {e}")

    logger.info(f"Firebase login attempt for: {phone}")

    # 2. Find or create user
    from sqlalchemy.exc import IntegrityError
    db.expire_all()
    
    user = db.query(UserModel).filter(UserModel.phone == phone).first()
    if user:
        if body.name and body.name != "Student" and user.name != body.name:
            user.name = body.name
            db.commit()
        logger.info(f"Returning Firebase user: {phone} (ID={user.id})")
    else:
        try:
            user = UserModel(phone=phone, name=body.name or "Student", role=body.role)
            db.add(user)
            db.commit()
            db.refresh(user)
            logger.info(f"New Firebase user registered: {phone} (ID={user.id})")
        except IntegrityError:
            db.rollback()
            db.expire_all()
            user = db.query(UserModel).filter(UserModel.phone == phone).first()
            if not user:
                raise HTTPException(status_code=500, detail="User creation failed")
            logger.info(f"Found existing Firebase user after retry: {phone} (ID={user.id})")
        except Exception as e:
            db.rollback()
            logger.error(f"Firebase login DB error: {e}")
            raise HTTPException(status_code=500, detail=f"Database error: {e}")

    # 3. Generate internal JWT Auth token
    token = create_access_token(user_id=user.id, phone=user.phone)
    
    return {
        "access_token": token,
        "token_type": "bearer",
        "user": _user_response_dict(user),
    }


# ═══════════════════════════════════════════════════════════════════════════════════
#  PROFILE — View & update user profile
# ═══════════════════════════════════════════════════════════════════════════════════

@app.get("/api/profile", response_model=UserResponse, tags=["Profile"])
def get_profile(
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """Get the currently logged-in user's profile."""
    user = db.query(UserModel).filter(UserModel.id == int(current_user["sub"])).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    return _user_response_dict(user)


@app.put("/api/profile", response_model=UserResponse, tags=["Profile"])
def update_profile(
    update: ProfileUpdate,
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """Update user name and/or skills (used by AI engine for match scoring)."""
    user = db.query(UserModel).filter(UserModel.id == int(current_user["sub"])).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    if update.name is not None:
        user.name = update.name
    if update.skills_json is not None:
        user.skills_json = update.skills_json

    db.commit()
    db.refresh(user)
    logger.info(f"Profile updated for user {user.id}: name={user.name}, skills={user.skills_json}")
    return _user_response_dict(user)


# ═══════════════════════════════════════════════════════════════════════════════════
#  APPLICATIONS — Full Lifecycle: Apply → Accept → Work → Complete → Pay
# ═══════════════════════════════════════════════════════════════════════════════════

# Phase 2: Valid status transitions (7-step two-sided confirmation)
# pending → accepted → checked_in → in_progress → work_done → confirmed → paid
VALID_TRANSITIONS = {
    "pending": ["accepted", "rejected"],
    "accepted": ["checked_in", "rejected"],     # Student checks in
    "checked_in": ["in_progress"],               # Employer confirms arrival
    "in_progress": ["work_done"],                # Student marks work done
    "work_done": ["confirmed"],                  # Employer confirms quality
    "confirmed": ["paid"],                       # Payment released
}


def _app_detail_dict(application, job):
    """Build a standardized ApplicationDetailResponse dict."""
    return {
        "id": application.id,
        "job_id": application.job_id,
        "user_id": application.user_id,
        "status": application.status,
        "applied_at": application.applied_at,
        "accepted_at": application.accepted_at,
        "started_at": application.started_at,
        "completed_at": application.completed_at,
        "paid_at": application.paid_at,
        "employer_note": application.employer_note,
        "rating": application.rating,
        # Phase 2
        "checked_in_at": getattr(application, 'checked_in_at', None),
        "work_done_at": getattr(application, 'work_done_at', None),
        "confirmed_at": getattr(application, 'confirmed_at', None),
        # Job details
        "job_title": job.title if job else "Unknown",
        "job_description": job.description if job else None,
        "job_pay_amount": float(job.pay_amount) if job else 0,
        "job_location": job.location if job else "Unknown",
        "job_is_urgent": job.is_urgent if job else False,
        "job_employer_id": job.employer_id if job else None,
        # Phase 1: Schedule
        "job_date": getattr(job, 'job_date', None) if job else None,
        "job_start_time": getattr(job, 'start_time', None) if job else None,
        "job_end_time": getattr(job, 'end_time', None) if job else None,
    }


@app.post("/api/apply", response_model=ApplicationResponse, status_code=201, tags=["Applications"])
def apply_to_job(
    application: ApplicationCreate,
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """
    Apply to a job. Requires JWT authentication.
    Prevents duplicate applications to the same job.
    """
    user_id = int(current_user["sub"])

    # ─── Role check: Only students can apply for jobs ────────────────────
    user = db.query(UserModel).filter(UserModel.id == user_id).first()
    if user and user.role == "employer":
        raise HTTPException(status_code=403, detail="Employers cannot apply to jobs")

    # Check if job exists
    job = db.query(JobModel).filter(JobModel.id == application.job_id).first()
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")

    # Prevent applying to own job
    if job.employer_id == user_id:
        raise HTTPException(status_code=400, detail="You cannot apply to your own job")

    # Check for duplicate application
    existing = db.query(AppModel).filter(
        AppModel.job_id == application.job_id,
        AppModel.user_id == user_id,
    ).first()
    if existing:
        raise HTTPException(status_code=409, detail="Already applied to this job")

    # Create application
    db_app = AppModel(
        job_id=application.job_id,
        user_id=user_id,
        status="pending",
    )
    db.add(db_app)
    db.commit()
    db.refresh(db_app)

    logger.info(f"User {user_id} applied to job {application.job_id}")
    return db_app


@app.get("/api/my-applications", response_model=List[ApplicationDetailResponse], tags=["Applications"])
def get_my_applications(
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """Get all applications for the current user — enriched with job details and lifecycle timestamps."""
    user_id = int(current_user["sub"])
    apps = db.query(AppModel).filter(AppModel.user_id == user_id).order_by(AppModel.applied_at.desc()).all()

    # Batch-fetch all related jobs in ONE query (fixes N+1)
    job_ids = list({a.job_id for a in apps})
    jobs_map = {}
    if job_ids:
        jobs = db.query(JobModel).filter(JobModel.id.in_(job_ids)).all()
        jobs_map = {j.id: j for j in jobs}

    result = []
    for a in apps:
        job = jobs_map.get(a.job_id)
        result.append(_app_detail_dict(a, job))

    return result


@app.put("/api/applications/{app_id}/status", response_model=ApplicationDetailResponse, tags=["Applications"])
def update_application_status(
    app_id: int,
    update: ApplicationStatusUpdate,
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """
    Update application status (employer action: accept/reject).
    Phase 2: pending → accepted/rejected only.
    Use dedicated endpoints for other transitions.
    """
    application = db.query(AppModel).filter(AppModel.id == app_id).first()
    if not application:
        raise HTTPException(status_code=404, detail="Application not found")

    # Only allow pending → accepted/rejected via this generic endpoint
    if application.status != "pending" or update.status not in ["accepted", "rejected"]:
        raise HTTPException(
            status_code=400,
            detail=f"Use this endpoint only for accept/reject. Current: '{application.status}', requested: '{update.status}'"
        )

    now = datetime.now(timezone.utc)
    application.status = update.status
    if update.note:
        application.employer_note = update.note

    if update.status == "accepted":
        application.accepted_at = now
        # Phase 3: Create Payment record
        job = db.query(JobModel).filter(JobModel.id == application.job_id).first()
        if job:
            payment = db.query(PaymentModel).filter(PaymentModel.application_id == application.id).first()
            if not payment:
                new_payment = PaymentModel(
                    application_id=application.id,
                    amount=job.pay_amount,
                    from_user_id=job.employer_id,
                    to_user_id=application.user_id,
                    status="pending"
                )
                db.add(new_payment)
            # Phase 6: Notify student
            _create_notification(
                db, application.user_id,
                "Application Accepted! 🎉",
                f"Your application for '{job.title}' has been accepted.",
                "application_accepted", job.id, application.id
            )
    elif update.status == "rejected":
        job = db.query(JobModel).filter(JobModel.id == application.job_id).first()
        if job:
            _create_notification(
                db, application.user_id,
                "Application Update",
                f"Your application for '{job.title}' was not selected.",
                "application_rejected", job.id, application.id
            )

    db.commit()
    db.refresh(application)

    job = db.query(JobModel).filter(JobModel.id == application.job_id).first()
    logger.info(f"Application {app_id} status → {update.status}")
    return _app_detail_dict(application, job)


# ─── Phase 2: Student Check-In ────────────────────────────────────────────────

@app.post("/api/applications/{app_id}/check-in", response_model=ApplicationDetailResponse, tags=["Applications"])
def check_in(
    app_id: int,
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """
    Student checks in — "I'm on my way!" (Phase 2)
    Only allowed when status is 'accepted'.
    Validates that today is on or after the job date (if set).
    """
    user_id = int(current_user["sub"])
    application = db.query(AppModel).filter(
        AppModel.id == app_id,
        AppModel.user_id == user_id,
    ).first()

    if not application:
        raise HTTPException(status_code=404, detail="Application not found")
    if application.status != "accepted":
        raise HTTPException(status_code=400, detail=f"Cannot check in — current status: {application.status}")

    # Validate job date (if set, can only check in on or after job day)
    job = db.query(JobModel).filter(JobModel.id == application.job_id).first()
    job_date = getattr(job, 'job_date', None) if job else None
    if job_date:
        from datetime import date
        try:
            jd = date.fromisoformat(job_date)
            today = date.today()
            if today < jd:
                raise HTTPException(
                    status_code=400,
                    detail=f"Too early to check in — job date is {job_date}"
                )
        except ValueError:
            pass  # If date is invalid, allow check-in anyway

    now = datetime.now(timezone.utc)
    application.status = "checked_in"
    application.checked_in_at = now

    # Phase 6: Notify employer that student is on their way
    if job and job.employer_id:
        _create_notification(
            db, job.employer_id,
            "Student On The Way! 🚶",
            f"A student has checked in for '{job.title}' and is on their way.",
            "check_in", job.id, application.id
        )

    db.commit()
    db.refresh(application)

    logger.info(f"User {user_id} checked in for job {application.job_id}")
    return _app_detail_dict(application, job)


# ─── Phase 2: Employer Confirms Arrival (Start Work) ─────────────────────────

@app.post("/api/applications/{app_id}/start-work", response_model=ApplicationDetailResponse, tags=["Applications"])
def start_work(
    app_id: int,
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """
    Employer confirms student has arrived — work begins. (Phase 2)
    Only the employer who owns the job can trigger this.
    Status: checked_in → in_progress
    """
    user_id = int(current_user["sub"])
    application = db.query(AppModel).filter(AppModel.id == app_id).first()

    if not application:
        raise HTTPException(status_code=404, detail="Application not found")

    # Verify caller is the employer for this job
    job = db.query(JobModel).filter(JobModel.id == application.job_id).first()
    if not job or job.employer_id != user_id:
        raise HTTPException(status_code=403, detail="Only the employer can confirm arrival")

    if application.status != "checked_in":
        raise HTTPException(status_code=400, detail=f"Cannot start work — current status: {application.status}")

    application.status = "in_progress"
    application.started_at = datetime.now(timezone.utc)

    # Phase 6: Notify student that employer confirmed arrival
    _create_notification(
        db, application.user_id,
        "Arrival Confirmed ✅",
        f"Employer confirmed your arrival for '{job.title}'. Work has begun!",
        "work_started", job.id, application.id
    )

    db.commit()
    db.refresh(application)

    logger.info(f"Employer {user_id} confirmed arrival for app {app_id} on job {application.job_id}")
    return _app_detail_dict(application, job)


# ─── Phase 2: Student Marks Work Done ─────────────────────────────────────────

@app.post("/api/applications/{app_id}/complete", response_model=ApplicationDetailResponse, tags=["Applications"])
def complete_work(
    app_id: int,
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """
    Student marks work as done. (Phase 2)
    Status: in_progress → work_done
    Now awaits employer confirmation.
    """
    user_id = int(current_user["sub"])
    application = db.query(AppModel).filter(
        AppModel.id == app_id,
        AppModel.user_id == user_id,
    ).first()

    if not application:
        raise HTTPException(status_code=404, detail="Application not found")
    if application.status != "in_progress":
        raise HTTPException(status_code=400, detail=f"Cannot mark done — current status: {application.status}")

    now = datetime.now(timezone.utc)
    application.status = "work_done"
    application.work_done_at = now
    application.completed_at = now  # Also set completed_at for backwards compat

    # Phase 6: Notify employer that work is done
    job = db.query(JobModel).filter(JobModel.id == application.job_id).first()
    if job and job.employer_id:
        _create_notification(
            db, job.employer_id,
            "Work Completed! 🏁",
            f"Student has completed work on '{job.title}'. Please review and confirm.",
            "work_done", job.id, application.id
        )

    db.commit()
    db.refresh(application)

    job = db.query(JobModel).filter(JobModel.id == application.job_id).first()
    logger.info(f"User {user_id} marked work done on job {application.job_id}")
    return _app_detail_dict(application, job)


# ─── Phase 2: Employer Confirms Completion ────────────────────────────────────

@app.post("/api/applications/{app_id}/confirm", response_model=ApplicationDetailResponse, tags=["Applications"])
def confirm_completion(
    app_id: int,
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """
    Employer confirms work quality — triggers payment. (Phase 2)
    Status: work_done → confirmed
    Also updates student earnings.
    """
    user_id = int(current_user["sub"])
    application = db.query(AppModel).filter(AppModel.id == app_id).first()

    if not application:
        raise HTTPException(status_code=404, detail="Application not found")

    # Verify caller is the employer for this job
    job = db.query(JobModel).filter(JobModel.id == application.job_id).first()
    if not job or job.employer_id != user_id:
        raise HTTPException(status_code=403, detail="Only the employer can confirm completion")

    if application.status != "work_done":
        raise HTTPException(status_code=400, detail=f"Cannot confirm — current status: {application.status}")

    now = datetime.now(timezone.utc)
    application.status = "confirmed"
    application.confirmed_at = now
    
    # Phase 3: Auto-release payment when employer confirms
    payment = db.query(PaymentModel).filter(PaymentModel.application_id == application.id).first()
    if payment:
        payment.status = "released"
        payment.released_at = now

    # Phase 6: Notify student that work was confirmed and payment released
    _create_notification(
        db, application.user_id,
        "Work Confirmed & Payment Released! 💰",
        f"Employer confirmed your work on '{job.title}'. Payment has been released!",
        "payment_released", job.id, application.id
    )

    db.commit()
    db.refresh(application)

    logger.info(f"Employer {user_id} confirmed completion for app {app_id}")
    return _app_detail_dict(application, job)


# ─── Phase 2: Confirm Payment Received ────────────────────────────────────────

@app.post("/api/applications/{app_id}/confirm-payment", response_model=ApplicationDetailResponse, tags=["Applications"])
def confirm_payment(
    app_id: int,
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """Confirm payment received. Updates user's total earnings."""
    user_id = int(current_user["sub"])
    application = db.query(AppModel).filter(
        AppModel.id == app_id,
        AppModel.user_id == user_id,
    ).first()

    if not application:
        raise HTTPException(status_code=404, detail="Application not found")
    if application.status != "confirmed":
        raise HTTPException(status_code=400, detail=f"Cannot confirm payment — current status: {application.status}")

    now = datetime.now(timezone.utc)
    application.status = "paid"
    application.paid_at = now

    # Update user's earnings
    job = db.query(JobModel).filter(JobModel.id == application.job_id).first()
    if job:
        user = db.query(UserModel).filter(UserModel.id == user_id).first()
        if user:
            current_earned = float(user.total_earned or 0)
            user.total_earned = current_earned + float(job.pay_amount)
            user.gigs_completed = (user.gigs_completed or 0) + 1
            
    # Phase 3: Mark payment as completed
    payment = db.query(PaymentModel).filter(PaymentModel.application_id == application.id).first()
    if payment:
        payment.status = "completed"

    # Phase 6: Notify employer that student collected payment
    if job and job.employer_id:
        _create_notification(
            db, job.employer_id,
            "Payment Collected ✅",
            f"Student confirmed receiving payment for '{job.title}'. Gig complete!",
            "payment_confirmed", job.id, application.id
        )

    db.commit()
    db.refresh(application)

    logger.info(f"User {user_id} confirmed payment for job {application.job_id} - Rs.{job.pay_amount if job else 0}")
    return _app_detail_dict(application, job)


# ─── Phase 4: Rate Experience ─────────────────────────────────────────────────

@app.post("/api/applications/{app_id}/rate", response_model=RatingResponse, tags=["Applications"])
def rate_application(
    app_id: int,
    rating: RatingCreate,
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """
    Rate the other party after the gig is fully paid/completed.
    If student calls this, rates employer. If employer calls, rates student.
    """
    user_id = int(current_user["sub"])
    application = db.query(AppModel).filter(AppModel.id == app_id).first()
    
    if not application:
        raise HTTPException(status_code=404, detail="Application not found")
        
    if application.status != "paid":
        raise HTTPException(status_code=400, detail="Can only rate after payment is completed")
        
    job = db.query(JobModel).filter(JobModel.id == application.job_id).first()
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")

    # Determine relative roles
    is_student = application.user_id == user_id
    is_employer = job.employer_id == user_id

    if not is_student and not is_employer:
        raise HTTPException(status_code=403, detail="Not authorized to rate this application")

    rated_id = job.employer_id if is_student else application.user_id

    # Check if already rated by this user
    existing_rating = db.query(RatingModel).filter(
        RatingModel.application_id == app_id,
        RatingModel.rater_id == user_id
    ).first()
    
    if existing_rating:
        raise HTTPException(status_code=400, detail="You have already rated this gig")

    # Create Rating
    new_rating = RatingModel(
        application_id=app_id,
        rater_id=user_id,
        rated_id=rated_id,
        score=rating.score,
        review=rating.review
    )
    db.add(new_rating)
    db.flush()

    # Update target user's average rating
    avg_score = db.query(func.avg(RatingModel.score)).filter(RatingModel.rated_id == rated_id).scalar()
    target_user = db.query(UserModel).filter(UserModel.id == rated_id).first()
    if target_user and avg_score:
        target_user.rating = float(avg_score)

    if is_employer:
        application.rating = rating.score

    # Notify rated user
    rater = db.query(UserModel).filter(UserModel.id == user_id).first()
    rater_name = rater.name if rater else "Someone"
    _create_notification(
        db, rated_id,
        "New Rating Received! ⭐",
        f"{rater_name} gave you a {rating.score}-star rating for '{job.title}'.",
        "rating_received", job.id, application.id
    )

    db.commit()
    db.refresh(new_rating)
    return new_rating


# ─── Employer: Get applications for their jobs ───────────────────────────────

@app.get("/api/employer/applications", response_model=List[ApplicationDetailResponse], tags=["Employer"])
def get_employer_applications(
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """Get all applications for jobs posted by the current user (employer view)."""
    user_id = int(current_user["sub"])

    # Get all jobs posted by this employer
    employer_jobs = db.query(JobModel).filter(JobModel.employer_id == user_id).all()
    job_ids = [j.id for j in employer_jobs]

    if not job_ids:
        return []

    # Get all applications for these jobs
    apps = db.query(AppModel).filter(
        AppModel.job_id.in_(job_ids)
    ).order_by(AppModel.applied_at.desc()).all()

    # Build job lookup for efficiency
    job_map = {j.id: j for j in employer_jobs}

    return [_app_detail_dict(a, job_map.get(a.job_id)) for a in apps]


# ═══════════════════════════════════════════════════════════════════════════════════
#  EARNINGS — Student earnings summary
# ═══════════════════════════════════════════════════════════════════════════════════

@app.get("/api/earnings", response_model=EarningsResponse, tags=["Earnings"])
def get_earnings(
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """Get the current user's earnings summary."""
    user_id = int(current_user["sub"])
    user = db.query(UserModel).filter(UserModel.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    # Count gigs in progress
    in_progress = db.query(AppModel).filter(
        AppModel.user_id == user_id,
        AppModel.status == "in_progress"
    ).count()

    # Count pending payment (work done or confirmed but not yet paid)
    pending_apps = db.query(AppModel).filter(
        AppModel.user_id == user_id,
        AppModel.status.in_(["work_done", "confirmed"])
    ).all()

    pending_amount = 0.0
    for a in pending_apps:
        job = db.query(JobModel).filter(JobModel.id == a.job_id).first()
        if job:
            pending_amount += float(job.pay_amount)

    # Phase 3: Recent payments
    payments = db.query(PaymentModel).filter(
        PaymentModel.to_user_id == user_id,
    ).order_by(PaymentModel.created_at.desc()).limit(10).all()

    recent_payments = []
    for p in payments:
        job = db.query(JobModel).join(AppModel, JobModel.id == AppModel.job_id).filter(AppModel.id == p.application_id).first()
        emp = db.query(UserModel).filter(UserModel.id == p.from_user_id).first()
        recent_payments.append({
            "id": p.id,
            "application_id": p.application_id,
            "amount": float(p.amount),
            "from_user_id": p.from_user_id,
            "to_user_id": p.to_user_id,
            "status": p.status,
            "created_at": p.created_at,
            "released_at": p.released_at,
            "job_title": job.title if job else "Unknown",
            "employer_name": emp.name if emp else "Unknown",
        })

    return {
        "total_earned": float(user.total_earned or 0),
        "pending_payment": pending_amount,
        "gigs_completed": user.gigs_completed or 0,
        "gigs_in_progress": in_progress,
        "recent_payments": recent_payments,
    }


# ═══════════════════════════════════════════════════════════════════════════════════
#  SIMULATION — Auto-accept applications for MVP demo
# ═══════════════════════════════════════════════════════════════════════════════════

@app.post("/api/simulate/accept-all", tags=["Simulation (Dev Only)"])
def simulate_accept_all(
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """
    DEV ONLY: Auto-accept all pending applications for the current user.
    Disabled in production (when JWT_SECRET_KEY env var is set).
    """
    # Block in production
    if os.environ.get("JWT_SECRET_KEY"):
        raise HTTPException(status_code=403, detail="This endpoint is disabled in production")

    user_id = int(current_user["sub"])
    pending = db.query(AppModel).filter(
        AppModel.user_id == user_id,
        AppModel.status == "pending"
    ).all()

    now = datetime.now(timezone.utc)
    count = 0
    for app in pending:
        app.status = "accepted"
        app.accepted_at = now
        app.employer_note = "Your application has been accepted! You can start working now."
        count += 1

    db.commit()
    return {"message": f"Accepted {count} pending applications", "accepted_count": count}


# ═══════════════════════════════════════════════════════════════════════════════════
#  RATINGS — View Ratings for a User
# ═══════════════════════════════════════════════════════════════════════════════════

@app.get("/api/users/{user_id}/ratings", response_model=List[RatingResponse], tags=["Ratings"])
def get_user_ratings(
    user_id: int,
    db: Session = Depends(get_db),
):
    """
    See someone's ratings.
    """
    user = db.query(UserModel).filter(UserModel.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
        
    ratings = db.query(RatingModel).filter(RatingModel.rated_id == user_id).order_by(RatingModel.created_at.desc()).all()
    return ratings


# ═══════════════════════════════════════════════════════════════════════════════════
#  PHASE 6: IN-APP NOTIFICATIONS
# ═══════════════════════════════════════════════════════════════════════════════════

# ─── AI Smart Notification: Auto-notify high-match students on new job ────────

def _notify_matching_students(db: Session, job, employer_id: int, threshold: int = 70, max_notify: int = 20):
    """
    When a new job is posted, find students with skill match > threshold
    and send them an AI-targeted notification.
    
    Args:
        db: Database session
        job: The newly created job object
        employer_id: ID of the employer (to exclude from notifications)
        threshold: Minimum match score to notify (default 70%)
        max_notify: Maximum students to notify per job (default 20)
    """
    from ai_engine import calculate_match_score

    job_skills = job.skills_required or "[]"

    # Get all students (non-employer users) who have skills set
    students = db.query(UserModel).filter(
        UserModel.id != employer_id,
        UserModel.skills_json.isnot(None),
        UserModel.skills_json != "[]",
        UserModel.skills_json != "",
    ).all()

    matched_students = []
    for student in students:
        score = calculate_match_score(student.skills_json, job_skills)
        if score >= threshold:
            matched_students.append((student, score))

    # Sort by highest match first, cap at max_notify
    matched_students.sort(key=lambda x: x[1], reverse=True)
    matched_students = matched_students[:max_notify]

    notified_count = 0
    for student, score in matched_students:
        # Build a compelling notification message
        if score >= 90:
            emoji = "🎯"
            intro = "Perfect match!"
        elif score >= 80:
            emoji = "⭐"
            intro = "Great match!"
        else:
            emoji = "✨"
            intro = "Good match!"

        title = f"{emoji} {intro} {score}% match for you"
        message = (
            f"A new gig \"{job.title}\" was just posted"
            f"{' in ' + job.location if job.location else ''}"
            f"{' — ₹' + str(int(job.pay_amount)) if job.pay_amount else ''}. "
            f"Your skills are a {score}% match. Apply now!"
        )

        _create_notification(
            db,
            user_id=student.id,
            title=title,
            message=message,
            notif_type="ai_job_match",
            job_id=job.id
        )
        notified_count += 1

    if notified_count > 0:
        db.commit()
        logger.info(f"🔔 AI Smart Notifications: Notified {notified_count} students for job '{job.title}' (ID={job.id})")

def _create_notification(db: Session, user_id: int, title: str, message: str,
                         notif_type: str, job_id: int = None, app_id: int = None):
    """Helper: Create an in-app notification for a user."""
    notif = NotifModel(
        user_id=user_id,
        title=title,
        message=message,
        type=notif_type,
        related_job_id=job_id,
        related_application_id=app_id
    )
    db.add(notif)
    # Don't commit here — let the caller commit
    return notif


@app.get("/api/notifications", response_model=List[NotificationResponse], tags=["Notifications"])
def get_notifications(
    limit: int = Query(20, le=50),
    skip: int = Query(0),
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """Get the current user's notifications (newest first)."""
    user_id = int(current_user["sub"])
    notifications = db.query(NotifModel).filter(
        NotifModel.user_id == user_id
    ).order_by(NotifModel.created_at.desc()).offset(skip).limit(limit).all()
    return notifications


@app.get("/api/notifications/unread-count", response_model=NotificationCountResponse, tags=["Notifications"])
def get_unread_count(
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """Get the number of unread notifications (for badge)."""
    user_id = int(current_user["sub"])
    count = db.query(NotifModel).filter(
        NotifModel.user_id == user_id,
        NotifModel.is_read == False
    ).count()
    return {"unread_count": count}


@app.post("/api/notifications/mark-read", tags=["Notifications"])
def mark_notifications_read(
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """Mark all notifications as read for the current user."""
    user_id = int(current_user["sub"])
    db.query(NotifModel).filter(
        NotifModel.user_id == user_id,
        NotifModel.is_read == False
    ).update({"is_read": True})
    db.commit()
    return {"message": "All notifications marked as read"}


# ═══════════════════════════════════════════════════════════════════════════════════
#  AI ENDPOINTS — Intelligence Layer
# ═══════════════════════════════════════════════════════════════════════════════════

@app.get("/api/ai/feed", response_model=List[AIJobResponse], tags=["AI"])
def get_ai_feed(
    skip: int = Query(0),
    limit: int = Query(50, le=100),
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """
    🤖 AI Smart Feed — Jobs ranked by personalized AI score.
    Uses skill match, category affinity, pay preference, urgency, recency, and location.
    """
    user_id = int(current_user["sub"])
    user = db.query(UserModel).filter(UserModel.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    # Get user's application history for affinity signals
    past_apps = db.query(AppModel).filter(AppModel.user_id == user_id).all()
    app_history = []
    for a in past_apps:
        job = db.query(JobModel).filter(JobModel.id == a.job_id).first()
        if job:
            app_history.append({
                "category": getattr(job, 'category', None),
                "location": job.location,
                "pay_amount": float(job.pay_amount),
            })

    # Get all open jobs
    jobs = db.query(JobModel).filter(
        (JobModel.status == None) | (JobModel.status == "open") | (JobModel.status == "")
    ).all()

    # Score each job with AI
    scored_jobs = []
    for job in jobs:
        ai_result = compute_smart_feed_score(job, user, app_history)
        match_score = calculate_match_score(user.skills_json or '', job.skills_required or '')

        job_dict = _job_to_dict(job, match_score, db)
        job_dict["ai_score"] = ai_result["ai_score"]
        job_dict["ai_reason"] = ai_result["reason"]
        job_dict["ai_breakdown"] = ai_result["breakdown"]
        scored_jobs.append(job_dict)

    # Sort by AI score (descending)
    scored_jobs.sort(key=lambda x: x.get("ai_score", 0), reverse=True)

    return scored_jobs[skip:skip + limit]


@app.get("/api/ai/skill-recommendations", response_model=AISkillRecommendationsResponse, tags=["AI"])
def get_skill_recommendations(
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """
    🎯 AI Skill Recommendations — Suggests skills to learn based on market demand.
    Analyzes all open jobs and tells the user which skills would unlock the most opportunities.
    """
    user_id = int(current_user["sub"])
    user = db.query(UserModel).filter(UserModel.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    # Get all open jobs
    all_jobs = db.query(JobModel).filter(
        (JobModel.status == None) | (JobModel.status == "open") | (JobModel.status == "")
    ).all()

    recommendations = recommend_skills(user.skills_json or '[]', all_jobs)
    current_skills = list(_parse_to_set_safe(user.skills_json))

    return {
        "recommendations": recommendations,
        "current_skills": current_skills,
        "total_open_jobs": len(all_jobs),
    }


def _parse_to_set_safe(skills_str):
    """Safely parse skills string to set."""
    if not skills_str:
        return set()
    try:
        from ai_engine import _parse_to_set
        return _parse_to_set(skills_str)
    except Exception:
        return set()


@app.get("/api/ai/applicants/{job_id}", response_model=List[AIApplicantResponse], tags=["AI"])
def get_ai_ranked_applicants(
    job_id: int,
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """
    📊 AI Applicant Ranking — Ranks job applicants by AI composite score.
    Shows AI Top Pick badge and detailed breakdown.
    """
    user_id = int(current_user["sub"])
    job = db.query(JobModel).filter(JobModel.id == job_id).first()
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")
    if job.employer_id != user_id:
        raise HTTPException(status_code=403, detail="Not authorized to view applicants")

    applications = db.query(AppModel).filter(AppModel.job_id == job_id).order_by(AppModel.applied_at.desc()).all()

    # Batch-fetch users
    user_ids = list({app_entry.user_id for app_entry in applications})
    users_map = {}
    if user_ids:
        users = db.query(UserModel).filter(UserModel.id.in_(user_ids)).all()
        users_map = {u.id: u for u in users}

    result = []
    for app_entry in applications:
        applicant = users_map.get(app_entry.user_id)
        match_score = None
        if applicant and applicant.skills_json:
            match_score = calculate_match_score(applicant.skills_json, job.skills_required)

        # AI ranking
        ai_result = rank_applicant(applicant, job, app_entry)

        result.append({
            "application_id": app_entry.id,
            "user_id": app_entry.user_id,
            "user_name": applicant.name if applicant else "Unknown",
            "user_phone": applicant.phone if applicant else "Unknown",
            "status": app_entry.status,
            "applied_at": app_entry.applied_at,
            "user_skills": applicant.skills_json if applicant else None,
            "match_score": match_score,
            "user_rating": float(applicant.rating or 0) if applicant else 0.0,
            "user_gigs_completed": (applicant.gigs_completed or 0) if applicant else 0,
            "trust_badge": _compute_trust_badge(applicant.rating, applicant.gigs_completed) if applicant else None,
            "ai_rank_score": ai_result["ai_rank_score"],
            "ai_badges": ai_result["badges"],
            "ai_breakdown": ai_result["breakdown"],
        })

    # Sort by AI rank score (highest first)
    result.sort(key=lambda x: x.get("ai_rank_score", 0), reverse=True)

    return result


@app.post("/api/ai/estimate-pay", response_model=AIPayEstimateResponse, tags=["AI"])
def ai_estimate_pay(
    request: AIPayEstimateRequest,
    db: Session = Depends(get_db),
):
    """
    💰 AI Pay Estimator — Suggests fair pay based on similar jobs in the database.
    """
    all_jobs = db.query(JobModel).all()
    result = estimate_pay(
        category=request.category,
        location=request.location,
        duration=request.duration,
        job_type=request.job_type,
        all_jobs=all_jobs,
    )
    return result


@app.post("/api/ai/generate-description", response_model=AIGenerateDescriptionResponse, tags=["AI"])
@limiter.limit("10/minute")
def ai_generate_description(
    body: AIGenerateDescriptionRequest,
    request: Request,
):
    """
    💬 AI Job Description Generator — Uses Gemini to write professional job descriptions.
    Input: rough title/notes → Output: polished description + suggested skills + pay range.
    """
    result = generate_job_description(
        title=body.title,
        category=body.category,
        rough_notes=body.rough_notes,
        location=body.location,
        duration=body.duration,
    )
    return result


@app.post("/api/ai/smart-search", response_model=AISmartSearchResponse, tags=["AI"])
@limiter.limit("15/minute")
def ai_smart_search(
    body: AISmartSearchRequest,
    request: Request,
    db: Session = Depends(get_db),
    current_user: Optional[dict] = Depends(get_optional_user),
):
    """
    🔍 AI Smart Search — Natural language search powered by Gemini NLP.
    Example: "weekend photography gigs near Hyderabad paying over 1000"
    """
    # Parse natural language query
    parsed = parse_natural_language_search(body.query)

    # Build DB query from parsed filters
    query = db.query(JobModel)
    query = query.filter(
        (JobModel.status == None) | (JobModel.status == "open") | (JobModel.status == "")
    )

    if parsed.get("search_text"):
        search = f"%{parsed['search_text']}%"
        query = query.filter(
            (JobModel.title.ilike(search)) | (JobModel.description.ilike(search))
        )
    if parsed.get("location"):
        query = query.filter(JobModel.location.ilike(f"%{parsed['location']}%"))
    if parsed.get("min_pay") is not None:
        query = query.filter(JobModel.pay_amount >= parsed["min_pay"])
    if parsed.get("max_pay") is not None:
        query = query.filter(JobModel.pay_amount <= parsed["max_pay"])
    if parsed.get("urgent_only"):
        query = query.filter(JobModel.is_urgent == True)
    if parsed.get("category"):
        query = query.filter(JobModel.category == parsed["category"])

    jobs = query.order_by(JobModel.created_at.desc()).limit(50).all()

    # Add match scores if logged in
    user = None
    if current_user:
        user = db.query(UserModel).filter(UserModel.id == int(current_user["sub"])).first()

    job_results = [_job_to_dict(j, _score_job(j, user, db), db) for j in jobs]

    return {
        "search_text": parsed.get("search_text"),
        "location": parsed.get("location"),
        "min_pay": parsed.get("min_pay"),
        "max_pay": parsed.get("max_pay"),
        "category": parsed.get("category"),
        "urgent_only": parsed.get("urgent_only", False),
        "interpretation": parsed.get("interpretation", ""),
        "ai_parsed": parsed.get("ai_parsed", False),
        "jobs": job_results,
    }


@app.post("/api/ai/generate-application-note", response_model=AIApplicationNoteResponse, tags=["AI"])
@limiter.limit("10/minute")
def ai_generate_application_note(
    body: AIApplicationNoteRequest,
    request: Request,
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """
    🤝 AI Application Note Generator — Writes personalized cover notes.
    """
    user_id = int(current_user["sub"])
    user = db.query(UserModel).filter(UserModel.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    job = db.query(JobModel).filter(JobModel.id == body.job_id).first()
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")

    match_score = calculate_match_score(user.skills_json or '', job.skills_required or '')

    result = generate_application_note(
        user_name=user.name,
        user_skills=user.skills_json or '',
        job_title=job.title,
        job_description=job.description or '',
        job_skills_required=job.skills_required or '',
        match_score=match_score,
    )
    return result


@app.get("/api/ai/match-explanation/{job_id}", response_model=AIMatchExplanationResponse, tags=["AI"])
def get_match_explanation(
    job_id: int,
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """
    🎯 AI Match Explanation — Detailed breakdown of why a match score is what it is.
    Shows matched skills, missing skills, and suggestions.
    """
    user_id = int(current_user["sub"])
    user = db.query(UserModel).filter(UserModel.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    job = db.query(JobModel).filter(JobModel.id == job_id).first()
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")

    return calculate_match_explanation(user.skills_json or '', job.skills_required or '')


@app.get("/api/ai/earnings-insights", response_model=AIEarningsInsightsResponse, tags=["AI"])
def get_earnings_insights(
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """
    📈 AI Earnings Insights — Analyzes earning patterns and generates predictions.
    """
    user_id = int(current_user["sub"])
    user = db.query(UserModel).filter(UserModel.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    # Get completed/paid applications
    completed_apps = db.query(AppModel).filter(
        AppModel.user_id == user_id,
        AppModel.status.in_(["paid", "completed", "confirmed"])
    ).all()

    # Build jobs map for analysis
    job_ids = list({a.job_id for a in completed_apps})
    jobs_map = {}
    if job_ids:
        jobs = db.query(JobModel).filter(JobModel.id.in_(job_ids)).all()
        jobs_map = {j.id: j for j in jobs}

    return analyze_earnings(user, completed_apps, jobs_map)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
