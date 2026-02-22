"""
StudentGig API — FastAPI Entry Point (Stage 4: Profiles, Search, Rich Apps)
Run with: uvicorn main:app --reload --host 0.0.0.0 --port 8000

Features:
  - GET  /api/jobs            → List jobs (+ AI match score if logged in)
  - GET  /api/jobs/search     → Search/filter jobs
  - POST /api/jobs            → Create a job
  - POST /api/login           → Phone-based login, returns JWT
  - GET  /api/profile         → Get current user profile
  - PUT  /api/profile         → Update name / skills
  - POST /api/apply           → Apply to a job (requires JWT)
  - GET  /api/my-applications → See your applications with job details
"""

from fastapi import FastAPI, HTTPException, Depends, Query
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from typing import List, Optional

from database import engine, get_db
from models import Base, Job as JobModel, User as UserModel, Application as AppModel
from schemas import (
    JobCreate, JobResponse, LoginRequest, TokenResponse, UserResponse,
    ApplicationCreate, ApplicationResponse, ApplicationDetailResponse,
    ProfileUpdate,
)
from auth import create_access_token, get_current_user, get_optional_user
from ai_engine import calculate_match_score

# ─── Create all tables on startup ────────────────────────────────────────────────
Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="StudentGig API",
    description="Backend for StudentGig — Auth, Jobs, Applications, AI Matching, Profiles.",
    version="0.4.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


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


@app.on_event("startup")
async def seed_database():
    """Insert seed jobs if the table is empty (first run only)."""
    from database import SessionLocal
    db = SessionLocal()
    try:
        if db.query(JobModel).count() == 0:
            for job_data in SEED_JOBS:
                db.add(JobModel(**job_data))
            db.commit()
            print(f"✅ Seeded {len(SEED_JOBS)} jobs into database")
        else:
            print(f"ℹ️  Database already has {db.query(JobModel).count()} jobs")
    except Exception as e:
        print(f"❌ Seed error: {e}")
        db.rollback()
    finally:
        db.close()


# ─── Health Check ────────────────────────────────────────────────────────────────

@app.get("/", tags=["Health"])
async def root():
    return {"status": "online", "service": "StudentGig API", "version": "0.4.0"}


# ═══════════════════════════════════════════════════════════════════════════════════
#  JOBS — with optional AI match scoring + search
# ═══════════════════════════════════════════════════════════════════════════════════

def _job_to_dict(job, match_score=None):
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
    }


def _score_job(job, user, db):
    """Calculate AI match score for a job given the current user."""
    if user and user.skills_json:
        return calculate_match_score(user.skills_json, job.skills_required)
    return None


@app.get("/api/jobs", response_model=List[JobResponse], tags=["Jobs"])
def get_jobs(
    skip: int = Query(0, description="Number of records to skip for pagination"),
    limit: int = Query(50, description="Maximum number of records to return"),
    db: Session = Depends(get_db),
    current_user: Optional[dict] = Depends(get_optional_user),
):
    """Retrieve all jobs. If user is logged in, each job gets an AI match_score."""
    jobs = db.query(JobModel).order_by(JobModel.created_at.desc()).offset(skip).limit(limit).all()

    user = None
    if current_user:
        user = db.query(UserModel).filter(
            UserModel.id == int(current_user["sub"])
        ).first()

    return [_job_to_dict(j, _score_job(j, user, db)) for j in jobs]


@app.get("/api/jobs/search", response_model=List[JobResponse], tags=["Jobs"])
def search_jobs(
    q: Optional[str] = Query(None, description="Search query for title/description"),
    location: Optional[str] = Query(None, description="Filter by location"),
    min_pay: Optional[float] = Query(None, description="Minimum pay amount"),
    max_pay: Optional[float] = Query(None, description="Maximum pay amount"),
    urgent_only: bool = Query(False, description="Only show urgent jobs"),
    skip: int = Query(0, description="Number of records to skip for pagination"),
    limit: int = Query(50, description="Maximum number of records to return"),
    db: Session = Depends(get_db),
    current_user: Optional[dict] = Depends(get_optional_user),
):
    """Search and filter jobs."""
    query = db.query(JobModel)

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

    jobs = query.order_by(JobModel.created_at.desc()).offset(skip).limit(limit).all()

    user = None
    if current_user:
        user = db.query(UserModel).filter(
            UserModel.id == int(current_user["sub"])
        ).first()

    return [_job_to_dict(j, _score_job(j, user, db)) for j in jobs]


@app.post("/api/jobs", response_model=JobResponse, status_code=201, tags=["Jobs"])
def create_job(job: JobCreate, db: Session = Depends(get_db)):
    """Insert a new job into the database."""
    db_job = JobModel(
        title=job.title,
        description=job.description,
        pay_amount=job.pay_amount,
        location=job.location,
        skills_required=job.skills_required,
        is_urgent=job.is_urgent,
    )
    db.add(db_job)
    db.commit()
    db.refresh(db_job)
    return db_job


@app.get("/api/jobs/{job_id}", response_model=JobResponse, tags=["Jobs"])
def get_job(job_id: int, db: Session = Depends(get_db)):
    """Get a single job by ID."""
    job = db.query(JobModel).filter(JobModel.id == job_id).first()
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")
    return job


# Legacy endpoint
@app.get("/jobs", response_model=List[JobResponse], tags=["Jobs (Legacy)"])
def get_jobs_legacy(db: Session = Depends(get_db)):
    return [_job_to_dict(j) for j in db.query(JobModel).all()]


# ═══════════════════════════════════════════════════════════════════════════════════
#  AUTH — Phone-based login (OTP mocked for MVP)
# ═══════════════════════════════════════════════════════════════════════════════════

@app.post("/api/login", response_model=TokenResponse, tags=["Auth"])
def login(request: LoginRequest, db: Session = Depends(get_db)):
    """
    Phone-based login. Creates user if new, returns JWT token.
    For MVP: No OTP verification — accepts any phone number.
    """
    # Find or create user
    user = db.query(UserModel).filter(UserModel.phone == request.phone).first()
    if not user:
        user = UserModel(
            phone=request.phone,
            name=request.name or "Student",
        )
        db.add(user)
        db.commit()
        db.refresh(user)
        print(f"✅ New user registered: {user.phone}")

    # Generate JWT
    token = create_access_token(user_id=user.id, phone=user.phone)

    return {
        "access_token": token,
        "token_type": "bearer",
        "user": user,
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
    return user


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
    print(f"✅ Profile updated for user {user.id}: name={user.name}, skills={user.skills_json}")
    return user


# ═══════════════════════════════════════════════════════════════════════════════════
#  APPLICATIONS — Apply to jobs (requires auth)
# ═══════════════════════════════════════════════════════════════════════════════════

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

    # Check if job exists
    job = db.query(JobModel).filter(JobModel.id == application.job_id).first()
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")

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

    print(f"✅ User {user_id} applied to job {application.job_id}")
    return db_app


@app.get("/api/my-applications", response_model=List[ApplicationDetailResponse], tags=["Applications"])
def get_my_applications(
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """Get all applications for the current user — enriched with job details."""
    user_id = int(current_user["sub"])
    apps = db.query(AppModel).filter(AppModel.user_id == user_id).order_by(AppModel.applied_at.desc()).all()

    result = []
    for a in apps:
        job = db.query(JobModel).filter(JobModel.id == a.job_id).first()
        result.append({
            "id": a.id,
            "job_id": a.job_id,
            "user_id": a.user_id,
            "status": a.status,
            "applied_at": a.applied_at,
            "job_title": job.title if job else "Unknown",
            "job_pay_amount": float(job.pay_amount) if job else 0,
            "job_location": job.location if job else "Unknown",
            "job_is_urgent": job.is_urgent if job else False,
        })

    return result


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
