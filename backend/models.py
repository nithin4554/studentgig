"""
SQLAlchemy ORM Models — maps Python classes to MySQL tables.
Tables auto-created on startup via: Base.metadata.create_all()

Stage 5: Full application lifecycle + earnings tracking.
"""

from sqlalchemy import Column, Integer, String, Text, Boolean, DECIMAL, DateTime, ForeignKey
from sqlalchemy.sql import func
from database import Base


class Job(Base):
    """Gig/Job listing posted by employers or students."""
    __tablename__ = "jobs"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    title = Column(String(255), nullable=False)
    description = Column(Text, nullable=True)
    pay_amount = Column(DECIMAL(10, 2), nullable=False)
    location = Column(String(255), nullable=False)
    skills_required = Column(Text, nullable=True)  # JSON string: '["hindi", "teamwork"]'
    is_urgent = Column(Boolean, default=False)
    employer_id = Column(Integer, ForeignKey("users.id"), nullable=True)  # Who posted it
    max_applicants = Column(Integer, default=1)  # How many can be accepted
    deadline = Column(DateTime(timezone=True), nullable=True)  # Application deadline
    # ─── New fields for real job posting ──────────────────────────────────────
    company_name = Column(String(255), nullable=True)  # e.g. "TechCorp" or poster's name
    category = Column(String(100), nullable=True)  # Tutoring, Delivery, Events, Tech, etc.
    job_type = Column(String(50), default="one-time")  # one-time | part-time | recurring
    duration = Column(String(100), nullable=True)  # e.g. "2 hours", "1 week", "ongoing"
    status = Column(String(50), default="open")  # open | closed | paused | expired
    contact_info = Column(String(255), nullable=True)  # Phone or email for direct contact
    # ─── Phase 1: Scheduling ──────────────────────────────────────────────────
    job_date = Column(String(50), nullable=True)   # "March 26, 2026"
    start_time = Column(String(20), nullable=True)  # "10:00 AM"
    end_time = Column(String(20), nullable=True)    # "06:00 PM"
    address = Column(Text, nullable=True)          # Full street address for directions
    created_at = Column(DateTime(timezone=True), server_default=func.now())


class User(Base):
    """Registered student user — phone-based auth (no password, OTP-ready)."""
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    phone = Column(String(255), unique=True, nullable=False, index=True)
    name = Column(String(255), nullable=False, default="Student")
    hashed_password = Column(String(255), nullable=True)  # Salted bcrypt hash
    security_question = Column(String(255), nullable=True) # e.g. "My first school?"
    hashed_security_answer = Column(String(255), nullable=True) # Also hashed for security
    skills_json = Column(Text, nullable=True)  # JSON string: '["python", "hindi"]'
    role = Column(String(50), default="student")  # student | employer
    total_earned = Column(DECIMAL(10, 2), default=0.00)  # Lifetime earnings
    gigs_completed = Column(Integer, default=0)  # Number of completed gigs
    rating = Column(DECIMAL(3, 2), default=0.00)  # Average rating (0-5)
    created_at = Column(DateTime(timezone=True), server_default=func.now())


class Application(Base):
    """
    Job application with full lifecycle tracking.
    Status flow: pending → accepted → in_progress → completed → paid
                 pending → rejected
    """
    __tablename__ = "applications"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    job_id = Column(Integer, ForeignKey("jobs.id"), nullable=False)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    # pending | accepted | rejected | in_progress | completed | paid
    status = Column(String(50), default="pending")
    applied_at = Column(DateTime(timezone=True), server_default=func.now())
    accepted_at = Column(DateTime(timezone=True), nullable=True)
    started_at = Column(DateTime(timezone=True), nullable=True)   # When work began
    completed_at = Column(DateTime(timezone=True), nullable=True)  # When work finished
    paid_at = Column(DateTime(timezone=True), nullable=True)       # When payment released
    employer_note = Column(Text, nullable=True)  # Message from employer
    rating = Column(Integer, nullable=True)  # 1-5 star rating after completion
    # ─── Phase 2: Two-Sided Confirmation ─────────────────────────────────
    checked_in_at = Column(DateTime(timezone=True), nullable=True)  # Student: "I'm on my way"
    work_done_at = Column(DateTime(timezone=True), nullable=True)   # Student: "Work is done"
    confirmed_at = Column(DateTime(timezone=True), nullable=True)   # Employer: "Confirmed, pay them"

class Payment(Base):
    """
    Phase 3: Payment record tracking.
    """
    __tablename__ = "payments"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    application_id = Column(Integer, ForeignKey("applications.id"), nullable=False)
    amount = Column(DECIMAL(10, 2), nullable=False)
    from_user_id = Column(Integer, ForeignKey("users.id"), nullable=False)  # Employer
    to_user_id = Column(Integer, ForeignKey("users.id"), nullable=False)    # Student
    status = Column(String(50), default="pending")  # pending → released → completed
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    released_at = Column(DateTime(timezone=True), nullable=True)

class Rating(Base):
    """
    Phase 4: Mutual Rating System.
    """
    __tablename__ = "ratings"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    application_id = Column(Integer, ForeignKey("applications.id"), nullable=False)
    rater_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    rated_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    score = Column(Integer, nullable=False)  # 1-5 stars
    review = Column(Text, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())


class Notification(Base):
    """
    Phase 6: In-App Notification System.
    Stores notifications for status changes, application updates, etc.
    """
    __tablename__ = "notifications"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    title = Column(String(255), nullable=False)
    message = Column(Text, nullable=False)
    type = Column(String(50), nullable=False)  # application_accepted, check_in, work_done, payment, rating
    related_job_id = Column(Integer, nullable=True)
    related_application_id = Column(Integer, nullable=True)
    is_read = Column(Boolean, default=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
