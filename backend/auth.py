"""
JWT Authentication — Token generation & validation.
Uses PyJWT for lightweight JWT handling.

Usage in endpoints:
  - Protected: Depends(get_current_user)
  - Optional:  Depends(get_optional_user)
"""

import jwt
from datetime import datetime, timedelta, timezone
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from typing import Optional

# ─── Config ──────────────────────────────────────────────────────────────────────

SECRET_KEY = "studentgig-local-dev-secret-key-2026"  # Change in production!
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_HOURS = 72  # 3 days for dev convenience

security = HTTPBearer(auto_error=False)


# ─── Token Creation ──────────────────────────────────────────────────────────────

def create_access_token(user_id: int, phone: str) -> str:
    """Generate a JWT token with user_id and phone embedded."""
    payload = {
        "sub": str(user_id),
        "phone": phone,
        "exp": datetime.now(timezone.utc) + timedelta(hours=ACCESS_TOKEN_EXPIRE_HOURS),
        "iat": datetime.now(timezone.utc),
    }
    return jwt.encode(payload, SECRET_KEY, algorithm=ALGORITHM)


def decode_token(token: str) -> dict:
    """Decode and validate a JWT token. Raises on expiry or tampering."""
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        return payload
    except jwt.ExpiredSignatureError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token expired — please login again",
        )
    except jwt.InvalidTokenError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token",
        )


# ─── FastAPI Dependencies ────────────────────────────────────────────────────────

async def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security),
) -> dict:
    """
    REQUIRED auth dependency — use on protected endpoints.
    Raises 401 if no token or invalid token.
    """
    if credentials is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Login required",
        )
    return decode_token(credentials.credentials)


async def get_optional_user(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(security),
) -> Optional[dict]:
    """
    OPTIONAL auth dependency — returns user dict if logged in, None otherwise.
    Used on GET /api/jobs to optionally calculate match scores.
    """
    if credentials is None:
        return None
    try:
        return decode_token(credentials.credentials)
    except HTTPException:
        return None
