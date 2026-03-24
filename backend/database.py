"""
Database configuration for StudentGig.
Connects to XAMPP MySQL via PyMySQL driver.

Prerequisites:
  1. Start XAMPP → Start MySQL
  2. Open phpMyAdmin → Create database: studentgig_db
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
#         pass  # Already wrapped or non-standard environment

import os
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base

# ─── XAMPP MySQL Connection ──────────────────────────────────────────────────────
# Credentials from environment variables (defaults: XAMPP localhost, no password)
DB_USER = os.environ.get("DB_USER", "root")
DB_PASS = os.environ.get("DB_PASS", "")
DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_PORT = os.environ.get("DB_PORT", "3306")
DB_NAME = os.environ.get("DB_NAME", "studentgig_db")
SQLALCHEMY_DATABASE_URL = f"mysql+pymysql://{DB_USER}:{DB_PASS}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

engine = create_engine(
    SQLALCHEMY_DATABASE_URL,
    echo=False,              # Disable verbose SQL logging (prevents Unicode crashes)
    pool_pre_ping=True,      # Test connections before use — recovers stale ones
    pool_recycle=1800,       # Recycle connections every 30 min (MySQL wait_timeout safe)
    pool_size=10,            # Keep 10 connections in the pool
    max_overflow=20,         # Allow up to 20 extra connections under burst load
    pool_timeout=30,         # Wait up to 30s for a free connection before error
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


def get_db():
    """Dependency injection: yields a DB session per request, auto-closes."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
