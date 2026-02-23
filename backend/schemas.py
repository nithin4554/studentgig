"""
Pydantic v2 Schemas — request/response validation & serialization.
Stage 4: Profile update, enriched application responses, search params.
"""

from pydantic import BaseModel, Field
from typing import Optional, List
from datetime import datetime


# ─── Job Schemas ─────────────────────────────────────────────────────────────────

class JobBase(BaseModel):
    title: str
    description: Optional[str] = None
    pay_amount: float
    location: str
    skills_required: Optional[str] = None
    is_urgent: bool = False


class JobCreate(JobBase):
    """POST /api/jobs — create a new job."""
    pass


class JobResponse(JobBase):
    """GET /api/jobs — includes DB fields + optional AI match score."""
    id: int
    created_at: Optional[datetime] = None
    match_score: Optional[int] = Field(default=None)  # 0-100, from AI engine

    class Config:
        from_attributes = True


# ─── User Schemas (Phone-based Auth) ────────────────────────────────────────────

class LoginRequest(BaseModel):
    """POST /api/login — phone-based (OTP-ready, mocked for MVP)."""
    phone: str
    name: Optional[str] = "Student"


class GoogleLoginRequest(BaseModel):
    """POST /api/auth/google — expects a Google ID token."""
    idToken: str


class UserResponse(BaseModel):
    id: int
    phone: str
    name: str
    skills_json: Optional[str] = None
    role: str = "student"

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
    """GET /api/my-applications — enriched with job details."""
    id: int
    job_id: int
    user_id: int
    status: str
    applied_at: Optional[datetime] = None
    # Joined job fields
    job_title: str
    job_pay_amount: float
    job_location: str
    job_is_urgent: bool = False

    class Config:
        from_attributes = True
