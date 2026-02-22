"""
Database configuration for StudentGig.
Connects to XAMPP MySQL via PyMySQL driver.

Prerequisites:
  1. Start XAMPP → Start MySQL
  2. Open phpMyAdmin → Create database: studentgig_db
"""

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base

# ─── XAMPP MySQL Connection ──────────────────────────────────────────────────────
# Default XAMPP: user=root, password=empty, host=localhost, port=3306
SQLALCHEMY_DATABASE_URL = "mysql+pymysql://root:@localhost:3306/studentgig_db"

engine = create_engine(
    SQLALCHEMY_DATABASE_URL,
    echo=True,  # SQL query logging (disable in production)
    pool_pre_ping=True,  # Reconnect stale connections
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
