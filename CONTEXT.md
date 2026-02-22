# PROJECT: StudentGig (Local Stack)

## STACK
- **Mobile:** Kotlin, Jetpack Compose, Retrofit (Networking), Hilt (DI).
- **Backend:** Python (FastAPI), SQLAlchemy (ORM), Pydantic, PyJWT.
- **Database:** MySQL (XAMPP).
- **AI Engine:** scikit-learn (TF-IDF + Cosine Similarity).

## ARCHITECTURE

1. **Localhost Networking:** Physical device uses PC's Wi-Fi IP. Emulator uses `10.0.2.2`.
2. **Deferred Auth:** Home screen is open to all. Login is triggered only when user taps "Apply".
3. **AI Match Scoring:** When logged in, GET /api/jobs returns a `match_score` (0-100) per job.

## ENDPOINTS (Stage 3)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | / | None | Health check |
| GET | /api/jobs | Optional | List jobs (+ match_score if logged in) |
| POST | /api/jobs | None | Create a job |
| POST | /api/login | None | Phone login → JWT |
| POST | /api/apply | Required | Apply to a job |
| GET | /api/my-applications | Required | User's applications |
| PUT | /api/profile | Required | Update user skills |

## DATA MODEL (MySQL)

**jobs:** id, title, description, pay_amount, location, skills_required, is_urgent, created_at
**users:** id, phone, name, skills_json, role, created_at
**applications:** id, job_id (FK), user_id (FK), status, applied_at

## STAGES
- ✅ **Stage 0:** Project setup
- ✅ **Stage 1:** Android ↔ FastAPI handshake
- ✅ **Stage 2:** MySQL integration, CRUD, seed data
- ✅ **Stage 3:** Auth (JWT), Applications, AI Match Scoring
- ⬜ **Stage 4:** User profiles, skills editor, enhanced matching
- ⬜ **Stage 5:** Push notifications, job search/filter

## PROJECT STRUCTURE
```
Desktop/StudentGig/
├── settings.gradle.kts / build.gradle.kts / gradlew.bat
├── app/src/main/java/com/studentgig/app/
│   ├── StudentGigApp.kt           — Hilt Application
│   ├── MainActivity.kt            — Entry point
│   ├── data/
│   │   ├── model/Models.kt        — Data classes (Job, LoginRequest, etc.)
│   │   ├── local/TokenManager.kt  — JWT storage (SharedPreferences)
│   │   ├── remote/JobApiService.kt — Retrofit interface
│   │   └── repository/JobRepository.kt — Network calls + error handling
│   ├── di/NetworkModule.kt        — Hilt DI (OkHttp + Auth Interceptor)
│   └── ui/
│       ├── screens/HomeScreen.kt  — Job feed + LoginBottomSheet
│       ├── viewmodel/HomeViewModel.kt — Gatekeeper logic
│       ├── navigation/Navigation.kt
│       └── theme/Theme.kt
├── backend/
│   ├── main.py        — FastAPI endpoints
│   ├── database.py    — SQLAlchemy config
│   ├── models.py      — ORM models
│   ├── schemas.py     — Pydantic schemas
│   ├── auth.py        — JWT generation & validation
│   └── ai_engine.py   — TF-IDF match scoring
└── CONTEXT.md
```
