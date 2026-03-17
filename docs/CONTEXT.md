# PROJECT: StudentGig (Local Stack)

## STACK
- **Mobile:** Kotlin, Jetpack Compose, Retrofit (Networking), Hilt (DI).
- **Backend:** Python (FastAPI), SQLAlchemy (ORM), Pydantic, PyJWT.
- **Database:** MySQL (XAMPP).
- **AI Engine (Local):** Smart Feed Ranking, Skill Recommendations, Applicant Ranking, Pay Estimation, Earnings Insights.
- **AI Engine (LLM):** Google Gemini API — Job Description Generator, Smart Search NLP, Application Note Generator.

## ARCHITECTURE

1. **Localhost Networking:** Physical device uses PC's Wi-Fi IP. Emulator uses `10.0.2.2`.
2. **Deferred Auth:** Home screen is open to all. Login is triggered only when user taps "Apply".
3. **AI Intelligence Layer:** Smart Feed ranking, Skill Recommendations, Match Explanations, Pay Estimation, NLP Search, LLM Job Description Generation.
4. **AI Match Scoring:** When logged in, GET /api/jobs returns a `match_score` (0-100) per job.
4. **Application Lifecycle:** pending → accepted → in_progress → completed → paid

## ENDPOINTS (Stage 5)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | / | None | Health check |
| GET | /api/jobs | Optional | List jobs (+ match_score if logged in) |
| GET | /api/jobs/search | Optional | Search/filter jobs |
| GET | /api/jobs/{id} | None | Single job detail |
| POST | /api/jobs | None | Create a job |
| POST | /api/login | None | Phone login → JWT |
| POST | /api/auth/google | None | Google login → JWT |
| GET | /api/profile | Required | Get user profile |
| PUT | /api/profile | Required | Update user profile |
| POST | /api/apply | Required | Apply to a job |
| GET | /api/my-applications | Required | User's applications (with lifecycle) |
| PUT | /api/applications/{id}/status | Required | Update application status |
| POST | /api/applications/{id}/start-work | Required | Start working on gig |
| POST | /api/applications/{id}/complete | Required | Mark work as done |
| POST | /api/applications/{id}/confirm-payment | Required | Confirm payment received |
| GET | /api/earnings | Required | Earnings summary |
| POST | /api/simulate/accept-all | Required | Dev: auto-accept pending apps |
| GET | /api/ai/feed | Required | 🤖 AI Smart Feed — personalized job ranking |
| GET | /api/ai/skill-recommendations | Required | 🎯 AI Skill Suggestions based on market demand |
| GET | /api/ai/applicants/{id} | Required | 📊 AI-ranked applicants for employers |
| POST | /api/ai/estimate-pay | None | 💰 AI Pay Estimator — fair pay suggestion |
| POST | /api/ai/generate-description | None | 💬 Gemini-powered job description writer |
| POST | /api/ai/smart-search | Optional | 🔍 NLP natural language job search |
| POST | /api/ai/generate-application-note | Required | 🤝 AI cover note generator |
| GET | /api/ai/match-explanation/{id} | Required | 🎯 Detailed match score breakdown |
| GET | /api/ai/earnings-insights | Required | 📈 AI earnings analysis & predictions |

## DATA MODEL (MySQL)

**jobs:** id, title, description, pay_amount, location, skills_required, is_urgent, employer_id, max_applicants, deadline, created_at
**users:** id, phone, name, skills_json, role, total_earned, gigs_completed, rating, created_at
**applications:** id, job_id (FK), user_id (FK), status, applied_at, accepted_at, started_at, completed_at, paid_at, employer_note, rating

## APPLICATION LIFECYCLE
```
pending → accepted → in_progress → completed → paid
pending → rejected
```

## STAGES
- ✅ **Stage 0:** Project setup
- ✅ **Stage 1:** Android ↔ FastAPI handshake
- ✅ **Stage 2:** MySQL integration, CRUD, seed data
- ✅ **Stage 3:** Auth (JWT), Applications, AI Match Scoring
- ✅ **Stage 4:** User profiles, skills editor, enhanced matching
- ✅ **Stage 5:** Full application lifecycle, earnings tracking
- ✅ **Stage 7:** AI Intelligence Layer — Smart Feed, Skill Recommendations, Gemini LLM, Pay Estimation

## PROJECT STRUCTURE
```
Desktop/StudentGig/
├── settings.gradle.kts / build.gradle.kts / gradlew.bat
├── app/src/main/java/com/studentgig/app/
│   ├── StudentGigApp.kt           — Hilt Application
│   ├── MainActivity.kt            — Entry point
│   ├── data/
│   │   ├── model/Models.kt        — Data classes (Job, User, Application, Earnings)
│   │   ├── local/TokenManager.kt  — JWT storage (SharedPreferences)
│   │   ├── remote/JobApiService.kt — Retrofit interface (full lifecycle)
│   │   └── repository/JobRepository.kt — Network calls + error handling
│   ├── di/NetworkModule.kt        — Hilt DI (OkHttp + Auth Interceptor)
│   └── ui/
│       ├── screens/
│       │   ├── HomeScreen.kt          — Job feed + LoginBottomSheet
│       │   ├── JobDetailScreen.kt     — Full job detail view
│       │   ├── MyApplicationsScreen.kt — Full lifecycle tracking + actions
│       │   ├── SearchScreen.kt        — Job search + filters
│       │   ├── ProfileScreen.kt       — User profile + earnings
│       │   └── SplashScreen.kt        — App launch screen
│       ├── viewmodel/
│       │   ├── HomeViewModel.kt        — Job feed + apply logic
│       │   ├── JobDetailViewModel.kt   — Job detail + apply
│       │   ├── ApplicationsViewModel.kt — Full lifecycle actions
│       │   ├── SearchViewModel.kt      — Search + filters
│       │   └── ProfileViewModel.kt     — Profile management
│       ├── navigation/Navigation.kt
│       ├── components/GigComponents.kt
│       ├── animations/GigAnimations.kt
│       └── theme/Colors.kt + Theme.kt
├── backend/
│   ├── main.py        — FastAPI endpoints (full lifecycle)
│   ├── database.py    — SQLAlchemy config
│   ├── models.py      — ORM models (with lifecycle fields)
│   ├── schemas.py     — Pydantic schemas (with lifecycle + AI)
│   ├── auth.py        — JWT generation & validation
│   ├── ai_engine.py   — AI Intelligence: Smart Feed, Skills, Ranking, Pay, Insights
│   └── ai_llm.py      — Gemini LLM: Description Generator, NLP Search, Cover Notes
└── CONTEXT.md
```
