# StudentGig

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()

StudentGig is a state-of-the-art **AI-driven Gig Matching Platform** designed to connect students with local opportunities using semantic intelligence. Unlike traditional keyword-based job boards, StudentGig leverages **Large Language Models (LLMs)** to understand student potential and job requirements contextually.

## 🏗 Architecture

The project is organized as a **Monorepo** following industry-standard patterns for scalability and clarity:

- 📱 **[mobile/](./mobile)**: Android application built with Jetpack Compose, Kotlin, and Hilt.
- ⚙️ **[backend/](./backend)**: High-performance Python API powered by FastAPI, SQLAlchemy (MySQL), and Groq AI (Llama 3.3).
- 🌐 **[web/](./web)**: React-based administrative and employer dashboard using Vite and Tailwind-inspired CSS.
- 📜 **[docs/](./docs)**: Comprehensive documentation including architecture blueprints and UI/UX design specs.
- 🛠 **[scripts/](./scripts)**: Automation utilities for deployment, database migrations, and development testing.

## 🚀 Why StudentGig?

Traditional job matching fails students because their experience is often "latent" (hobbies, class projects, soft skills). StudentGig solves this using:

1. **Semantic Matching**: Uses the Groq AI API to perform zero-shot matching between conversational job descriptions and varied student profiles.
2. **Real-time Lifecycle**: Manages the entire gig lifecycle from "Applied" and "In-Progress" to "Payment Confirmed."
3. **Cross-Platform**: Seamless experience across Mobile (for students) and Web (for employers).

## 🛠 Tech Stack

| Layer | Technologies |
| :--- | :--- |
| **Mobile** | Kotlin, Jetpack Compose, Hilt, Retrofit, Firebase Auth |
| **Backend** | Python 3.10+, FastAPI, PyMySQL, SQLAlchemy |
| **Frontend** | React 18, Vite, Lucide Icons, Framer Motion |
| **AI Engine** | Groq (Llama 3.3 70B), Semantic Reasoning |
| **Database** | MySQL (XAMPP / Production) |

## 🏁 Getting Started

### Prerequisites
- Android Studio (for mobile)
- Python 3.10+
- Node.js & npm
- XAMPP (for local MySQL)

### Setup
1. **Clone the repo**
2. **Backend**: 
   ```bash
   cd backend
   pip install -r requirements.txt
   # Start server via scripts/START_SERVER.bat
   ```
3. **Web**:
   ```bash
   cd web
   npm install
   npm run dev
   ```
4. **Mobile**: 
   - Open the `mobile` folder in Android Studio.
   - Update `BASE_URL` in `local.properties` or `build.gradle.kts` if needed.

## 📄 License
This project is licensed under the MIT License - see the LICENSE file for details.

---
*Built with ❤️ for students everywhere.*
