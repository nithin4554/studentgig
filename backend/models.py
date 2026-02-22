"""
SQLAlchemy ORM Models — maps Python classes to MySQL tables.
Tables auto-created on startup via: Base.metadata.create_all()

Stage 3: Added User (phone-based) and Application tables.
"""

from sqlalchemy import Column, Integer, String, Text, Boolean, DECIMAL, DateTime, ForeignKey
from sqlalchemy.sql import func
from database import Base


class Job(Base):
    """Gig/Job listing posted by employers."""
    __tablename__ = "jobs"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    title = Column(String(255), nullable=False)
    description = Column(Text, nullable=True)
    pay_amount = Column(DECIMAL(10, 2), nullable=False)
    location = Column(String(255), nullable=False)
    skills_required = Column(Text, nullable=True)  # JSON string: '["hindi", "teamwork"]'
    is_urgent = Column(Boolean, default=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())


class User(Base):
    """Registered student user — phone-based auth (no password, OTP-ready)."""
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    phone = Column(String(15), unique=True, nullable=False, index=True)
    name = Column(String(255), nullable=False, default="Student")
    skills_json = Column(Text, nullable=True)  # JSON string: '["python", "hindi"]'
    role = Column(String(50), default="student")  # student | employer
    created_at = Column(DateTime(timezone=True), server_default=func.now())


class Application(Base):
    """Job application submitted by a student."""
    __tablename__ = "applications"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    job_id = Column(Integer, ForeignKey("jobs.id"), nullable=False)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    status = Column(String(50), default="pending")  # pending | accepted | rejected
    applied_at = Column(DateTime(timezone=True), server_default=func.now())
