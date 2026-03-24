"""
Pydantic v2 Schemas — request/response validation & serialization.
Stage 6: Real Job Posting + Full application lifecycle, earnings, status updates.
"""

from pydantic import BaseModel, Field, field_validator, model_validator, ValidationInfo
from typing import Optional, List, Literal
from datetime import datetime
import re
import html


def _sanitize_text(v: str, max_len: int = 5000) -> str:
    """Strip HTML tags, normalize whitespace, and limit length."""
    if not v:
        return v
    # Remove HTML tags
    v = re.sub(r'<[^>]+>', '', v)
    # Unescape HTML entities
    v = html.unescape(v)
    # Normalize whitespace
    v = ' '.join(v.split())
    return v[:max_len].strip()


# ─── Job Categories (shared constant) ────────────────────────────────────────────

JOB_CATEGORIES = [
    "Tutoring", "Delivery", "Events", "Tech", "Content Creation",
    "Design", "Marketing", "Data Entry", "Photography", "Volunteering",
    "Writing", "Translation", "Hospitality", "Fitness", "Other"
]

JOB_TYPES = ["one-time", "part-time", "recurring"]


# ─── Job Schemas ─────────────────────────────────────────────────────────────────

class JobBase(BaseModel):
    title: str
    description: Optional[str] = None
    pay_amount: float
    location: str
    skills_required: Optional[str] = None
    is_urgent: bool = False


class JobCreate(BaseModel):
    """POST /api/jobs — create a new job (requires JWT auth)."""
    title: str = Field(min_length=3, max_length=200)
    description: Optional[str] = Field(default=None, max_length=5000)
    pay_amount: float = Field(gt=0, le=1000000)
    location: str = Field(min_length=1, max_length=255)
    skills_required: Optional[str] = None  # JSON: '["python", "hindi"]'
    is_urgent: bool = False
    company_name: Optional[str] = Field(default=None, max_length=255)
    category: Optional[str] = None  # From JOB_CATEGORIES
    job_type: str = "one-time"  # one-time | part-time | recurring
    duration: Optional[str] = Field(default=None, max_length=100)
    max_applicants: int = Field(default=1, ge=1, le=100)
    contact_info: Optional[str] = Field(default=None, max_length=255)
    # ─── Phase 1: Scheduling ──────────────────────────────────
    job_date: Optional[str] = None    # "YYYY-MM-DD" — NULL = flexible
    start_time: Optional[str] = None  # "HH:MM" 24h, e.g. "09:00"
    end_time: Optional[str] = None    # "HH:MM" 24h, e.g. "14:00"
    address: Optional[str] = Field(default=None, max_length=500)
    deadline: Optional[datetime] = None  # Application deadline

    @field_validator("title")
    @classmethod
    def sanitize_title(cls, v):
        return _sanitize_text(v, max_len=200)

    @field_validator("description")
    @classmethod
    def sanitize_description(cls, v):
        if v is None:
            return v
        return _sanitize_text(v, max_len=5000)

    @field_validator("category")
    @classmethod
    def validate_category(cls, v):
        if v is not None and v not in JOB_CATEGORIES:
            raise ValueError(f"Invalid category. Must be one of: {', '.join(JOB_CATEGORIES)}")
        return v


class JobUpdate(BaseModel):
    """PUT /api/jobs/{id} — update your own job."""
    title: Optional[str] = None
    description: Optional[str] = None
    pay_amount: Optional[float] = Field(default=None, gt=0, le=1000000)
    location: Optional[str] = None
    skills_required: Optional[str] = None
    is_urgent: Optional[bool] = None
    company_name: Optional[str] = None
    category: Optional[str] = None
    job_type: Optional[str] = None
    duration: Optional[str] = None
    max_applicants: Optional[int] = None
    contact_info: Optional[str] = None
    status: Optional[Literal["open", "closed", "paused"]] = None
    job_date: Optional[str] = None
    start_time: Optional[str] = None
    end_time: Optional[str] = None
    address: Optional[str] = None
    deadline: Optional[datetime] = None


class JobResponse(BaseModel):
    """GET /api/jobs — includes DB fields + optional AI match score."""
    id: int
    title: str
    description: Optional[str] = None
    pay_amount: float
    location: str
    skills_required: Optional[str] = None
    is_urgent: bool = False
    created_at: Optional[datetime] = None
    match_score: Optional[int] = Field(default=None)  # 0-100, from AI engine
    employer_id: Optional[int] = None
    max_applicants: int = 1
    company_name: Optional[str] = None
    category: Optional[str] = None
    job_type: str = "one-time"
    duration: Optional[str] = None
    status: str = "open"
    contact_info: Optional[str] = None
    employer_name: Optional[str] = None  # Joined from users table
    applicant_count: Optional[int] = None  # Number of applications
    # ─── Phase 1: Scheduling ──────────────────────────────────
    job_date: Optional[str] = None
    start_time: Optional[str] = None
    end_time: Optional[str] = None
    address: Optional[str] = None
    deadline: Optional[datetime] = None

    class Config:
        from_attributes = True


# ─── User Schemas (Phone-based Auth) ────────────────────────────────────────────

class LoginRequest(BaseModel):
    """Returning user login (Phone + Password)."""
    phone: str = Field(min_length=10, max_length=15)
    password: str = Field(min_length=6, max_length=50)

    @field_validator("phone")
    @classmethod
    def validate_phone(cls, v):
        cleaned = re.sub(r'[\s\-\(\)\+]', '', v)
        if not cleaned.isdigit():
            raise ValueError("Phone number must contain only digits")
        
        # Professional validation: Prevent sequential or repetitive "junk" numbers
        if len(cleaned) == 10:
            if len(set(cleaned)) <= 2: # e.g. 9999999999 or 9898989898
                raise ValueError("Please enter a valid phone number (no repetitive digits)")
            if cleaned in "01234567890" or cleaned in "9876543210":
                raise ValueError("Sequential phone numbers are not allowed")
                
        return cleaned

class DemoLoginRequest(BaseModel):
    """Demo login (Phone + Name) — no password required for quick testing."""
    phone: str = Field(min_length=10, max_length=15)
    name: str = Field(min_length=2, max_length=100)

    @field_validator("phone")
    @classmethod
    def validate_phone(cls, v):
        cleaned = re.sub(r'[\s\-\(\)\+]', '', v)
        if not cleaned.isdigit():
            raise ValueError("Phone number must contain only digits")
        return cleaned

class RegisterRequest(BaseModel):
    """New user sign-up (Phone + Name + Password + Recovery Q/A)."""
    phone: str = Field(min_length=10, max_length=15)
    name: str = Field(min_length=2, max_length=100)
    password: str = Field(min_length=6, max_length=50)
    security_question: str = Field(min_length=5, max_length=255)
    security_answer: str = Field(min_length=2, max_length=255)
    role: str = "student"

    @field_validator("name")
    @classmethod
    def validate_name(cls, v):
        # Ensure no numbers or special symbols in name
        if any(char.isdigit() for char in v):
            raise ValueError("Name cannot contain numbers")
        
        # Check for repetitive characters e.g. "aaaa"
        if len(v) >= 4 and len(set(v.lower().replace(" ", ""))) <= 1:
            raise ValueError("Repetitive characters in name is not allowed")

        # Basic dummy list
        dummies = ["abc", "xyz", "test", "demo", "asdf", "qwerty"]
        if v.lower().strip() in dummies:
            raise ValueError("Common dummy names are not allowed")

        return v

    @field_validator("phone")
    @classmethod
    def validate_phone(cls, v):
        cleaned = re.sub(r'[\s\-\(\)\+]', '', v)
        if not cleaned.isdigit():
            return v # fallback to let Pydantic handle min_length error
            
        # Professional validation: Prevent sequential or repetitive "junk" numbers
        if len(cleaned) == 10:
            if len(set(cleaned)) <= 2:
                raise ValueError("Please enter a valid phone number (no repetitive digits)")
            if cleaned in "01234567890" or cleaned in "9876543210":
                raise ValueError("Sequential phone numbers are not allowed")

        return cleaned

class ResetPasswordCheck(BaseModel):
    """Step 1: Get security question by phone."""
    phone: str

class ResetPasswordFinal(BaseModel):
    """Step 2: Answer the question and set new password."""
    phone: str
    security_answer: str
    new_password: str = Field(min_length=6, max_length=50)


class GoogleLoginRequest(BaseModel):
    """POST /api/auth/google — expects a Google ID token."""
    idToken: str
    role: str = "student"

class FirebaseLoginRequest(BaseModel):
    """POST /api/auth/firebase — expects a Firebase ID token and optional name."""
    idToken: str
    name: Optional[str] = "Student"
    role: str = "student"


class UserResponse(BaseModel):
    id: int
    phone: str
    name: str
    skills_json: Optional[str] = None
    role: str = "student"
    total_earned: Optional[float] = 0.0
    gigs_completed: Optional[int] = 0
    rating: Optional[float] = 0.0
    trust_badge: Optional[str] = None  # Phase 4: "⭐ Trusted", "🆕 New", "⚡ Fast"

    class Config:
        from_attributes = True


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserResponse


class ProfileUpdate(BaseModel):
    """PUT /api/profile — update user profile."""
    name: Optional[str] = None
    skills_json: Optional[str] = None  # JSON string: '["python", "hindi"]'


# ─── Application Schemas ─────────────────────────────────────────────────────────

class ApplicationCreate(BaseModel):
    """POST /api/apply — requires JWT auth."""
    job_id: int


class ApplicationResponse(BaseModel):
    id: int
    job_id: int
    user_id: int
    status: str
    applied_at: Optional[datetime] = None

    class Config:
        from_attributes = True


class ApplicationDetailResponse(BaseModel):
    """GET /api/my-applications — enriched with job details + lifecycle timestamps."""
    id: int
    job_id: int
    user_id: int
    status: str
    applied_at: Optional[datetime] = None
    accepted_at: Optional[datetime] = None
    started_at: Optional[datetime] = None
    completed_at: Optional[datetime] = None
    paid_at: Optional[datetime] = None
    employer_note: Optional[str] = None
    rating: Optional[int] = None
    # Phase 2: Two-Sided Confirmation timestamps
    checked_in_at: Optional[datetime] = None
    work_done_at: Optional[datetime] = None
    confirmed_at: Optional[datetime] = None
    # Joined job fields
    job_title: str
    job_description: Optional[str] = None
    job_pay_amount: float
    job_location: str
    job_is_urgent: bool = False
    job_employer_id: Optional[int] = None
    # Phase 1: Schedule info
    job_date: Optional[str] = None
    job_start_time: Optional[str] = None
    job_end_time: Optional[str] = None

    class Config:
        from_attributes = True


class ConflictCheckResponse(BaseModel):
    """GET /api/jobs/{id}/check-conflict — schedule conflict detection."""
    has_conflict: bool = False
    conflicting_job_title: Optional[str] = None
    conflicting_time: Optional[str] = None
    message: str = "No conflict"


class ApplicationStatusUpdate(BaseModel):
    """PUT /api/applications/{id}/status — transition application state."""
    status: Literal["accepted", "rejected"]  # Only accept/reject via this endpoint
    note: Optional[str] = Field(default=None, max_length=1000)


class ApplicantResponse(BaseModel):
    """GET /api/jobs/{id}/applicants — employer view of who applied."""
    application_id: int
    user_id: int
    user_name: str
    user_phone: str
    status: str
    applied_at: Optional[datetime] = None
    user_skills: Optional[str] = None
    match_score: Optional[int] = None
    user_rating: Optional[float] = 0.0       # Phase 4: user's average rating
    user_gigs_completed: Optional[int] = 0   # Phase 4: number of completed gigs
    trust_badge: Optional[str] = None        # Phase 4: "⭐ Trusted", "🆕 New", "⚡ Fast"

    class Config:
        from_attributes = True


class PaymentResponse(BaseModel):
    id: int
    application_id: int
    amount: float
    from_user_id: int
    to_user_id: int
    status: str
    created_at: Optional[datetime] = None
    released_at: Optional[datetime] = None
    
    # Joined extra details optionally
    job_title: Optional[str] = None
    employer_name: Optional[str] = None
    
    class Config:
        from_attributes = True

class EarningsResponse(BaseModel):
    """GET /api/earnings — student's earnings summary."""
    total_earned: float = 0.0
    pending_payment: float = 0.0
    gigs_completed: int = 0
    gigs_in_progress: int = 0
    recent_payments: List[PaymentResponse] = []

class RatingCreate(BaseModel):
    """POST /api/applications/{id}/rate"""
    rated_id: int
    score: int = Field(ge=1, le=5)  # 1-5 stars
    review: Optional[str] = Field(default=None, max_length=500)

class RatingResponse(BaseModel):
    id: int
    application_id: int
    rater_id: int
    rated_id: int
    score: int
    review: Optional[str] = None
    created_at: Optional[datetime] = None

    class Config:
        from_attributes = True


# ─── Phase 6: Notification Schemas ────────────────────────────────────────────

class NotificationResponse(BaseModel):
    """GET /api/notifications — in-app notification."""
    id: int
    user_id: int
    title: str
    message: str
    type: str  # application_accepted, check_in, work_done, payment, rating
    related_job_id: Optional[int] = None
    related_application_id: Optional[int] = None
    is_read: bool = False
    created_at: Optional[datetime] = None

    class Config:
        from_attributes = True


class NotificationCountResponse(BaseModel):
    """GET /api/notifications/unread-count — badge count."""
    unread_count: int = 0


# ═══════════════════════════════════════════════════════════════════════════════════
#  AI SCHEMAS — Intelligence Layer
# ═══════════════════════════════════════════════════════════════════════════════════

class AIJobResponse(BaseModel):
    """Job with AI ranking metadata — used for Smart Feed."""
    id: int
    title: str
    description: Optional[str] = None
    pay_amount: float
    location: str
    skills_required: Optional[str] = None
    is_urgent: bool = False
    created_at: Optional[datetime] = None
    match_score: Optional[int] = None
    employer_id: Optional[int] = None
    max_applicants: int = 1
    company_name: Optional[str] = None
    category: Optional[str] = None
    job_type: str = "one-time"
    duration: Optional[str] = None
    status: str = "open"
    contact_info: Optional[str] = None
    employer_name: Optional[str] = None
    applicant_count: Optional[int] = None
    job_date: Optional[str] = None
    start_time: Optional[str] = None
    end_time: Optional[str] = None
    address: Optional[str] = None
    # ─── AI Fields ────────────────────────────────────────────
    ai_score: Optional[int] = None          # 0-100 composite ranking
    ai_reason: Optional[str] = None         # Why AI ranked it this way
    ai_breakdown: Optional[dict] = None     # Detailed breakdown

    class Config:
        from_attributes = True


class AISkillRecommendation(BaseModel):
    """Individual skill recommendation."""
    skill: str
    demand_count: int
    new_matches: int
    categories: List[str] = []
    reason: str


class AISkillRecommendationsResponse(BaseModel):
    """GET /api/ai/skill-recommendations — AI skill suggestions."""
    recommendations: List[AISkillRecommendation] = []
    current_skills: List[str] = []
    total_open_jobs: int = 0


class AIApplicantResponse(BaseModel):
    """Applicant with AI ranking — employer view."""
    application_id: int
    user_id: int
    user_name: str
    user_phone: str
    status: str
    applied_at: Optional[datetime] = None
    user_skills: Optional[str] = None
    match_score: Optional[int] = None
    user_rating: Optional[float] = 0.0
    user_gigs_completed: Optional[int] = 0
    trust_badge: Optional[str] = None
    # ─── AI Fields ────────────────────────────────────────────
    ai_rank_score: Optional[int] = None     # 0-100 composite ranking
    ai_badges: List[str] = []               # AI-generated badges
    ai_breakdown: Optional[dict] = None

    class Config:
        from_attributes = True


class AIPayEstimateRequest(BaseModel):
    """POST /api/ai/estimate-pay"""
    category: Optional[str] = None
    location: Optional[str] = None
    duration: Optional[str] = None
    job_type: str = "one-time"


class AIPayEstimateResponse(BaseModel):
    """Pay estimation result."""
    min_pay: Optional[float] = None
    avg_pay: Optional[float] = None
    max_pay: Optional[float] = None
    sample_size: int = 0
    confidence: str = "low"
    reasoning: str = ""


class AIGenerateDescriptionRequest(BaseModel):
    """POST /api/ai/generate-description"""
    title: str
    category: Optional[str] = None
    rough_notes: Optional[str] = None
    location: Optional[str] = None
    duration: Optional[str] = None


class AIGenerateDescriptionResponse(BaseModel):
    """AI-generated job description."""
    description: str
    suggested_skills: Optional[str] = None   # JSON string
    suggested_category: Optional[str] = None
    suggested_pay_min: Optional[float] = None
    suggested_pay_max: Optional[float] = None
    ai_generated: bool = False


class AISmartSearchRequest(BaseModel):
    """POST /api/ai/smart-search"""
    query: str


class AISmartSearchResponse(BaseModel):
    """Parsed search filters + results."""
    search_text: Optional[str] = None
    location: Optional[str] = None
    min_pay: Optional[float] = None
    max_pay: Optional[float] = None
    category: Optional[str] = None
    urgent_only: bool = False
    interpretation: str = ""
    ai_parsed: bool = False
    jobs: List[JobResponse] = []


class AIApplicationNoteRequest(BaseModel):
    """POST /api/ai/generate-application-note"""
    job_id: int


class AIApplicationNoteResponse(BaseModel):
    """AI-generated application cover note."""
    note: str
    ai_generated: bool = False


class AIMatchExplanationResponse(BaseModel):
    """GET /api/ai/match-explanation/{job_id} — detailed match breakdown."""
    score: int = 0
    matched_skills: List[str] = []
    missing_skills: List[str] = []
    extra_skills: List[str] = []
    explanation: str = ""


class AIEarningsInsightsResponse(BaseModel):
    """GET /api/ai/earnings-insights — AI analysis of earning patterns."""
    insights: List[str] = []
    best_category: Optional[str] = None
    projected_monthly: int = 0
    tips: List[str] = []

