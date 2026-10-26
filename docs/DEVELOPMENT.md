# MediGuide Care — Developer & Setup Guide

This guide provides instructions for setting up, configuring, running, and testing MediGuide Care on a local development workstation.

---

## 🛠️ Prerequisites

Before getting started, ensure the following tools are installed:

| Tool | Recommended Version | Verification Command |
|---|---|---|
| **Java Development Kit (JDK)** | OpenJDK 17 or higher | `java -version` |
| **Apache Maven** | 3.8.x or higher | `mvn -version` |
| **Node.js & npm** | Node 18+ (npm 9+) | `node -v` / `npm -v` |
| **PostgreSQL Database** | 15.x or 16.x | `psql -U postgres -V` |
| **Git** | 2.30+ | `git --version` |

---

## 📦 Local Setup Instructions

### 1. Database Creation (PostgreSQL)

Open your PostgreSQL CLI (`psql`) or graphical client (pgAdmin / DBeaver) and create the development database:

```sql
CREATE DATABASE mediguide_db;
```

---

### 2. Backend Configuration & Startup (Spring Boot)

The Spring Boot backend is located in the `backend/` directory.

#### Configuration Variables
Configure environment variables via CLI or IDE run configurations:

| Variable | Required | Default Value | Description |
|---|---|---|---|
| `DB_URL` | No | `jdbc:postgresql://localhost:5432/mediguide_db` | JDBC connection string |
| `DB_USERNAME` | No | `postgres` | Database username |
| `DB_PASSWORD` | **Yes** | *(Empty)* | Database user password |
| `JWT_SECRET` | **Yes** | *(Empty)* | HMAC-SHA256 signing key (min 32 characters) |
| `JWT_EXPIRATION_MINUTES` | No | `15` | Access token lifespan |
| `SMTP_HOST` | Optional | `smtp.gmail.com` | Outgoing SMTP mail server host |
| `SMTP_PORT` | Optional | `587` | SMTP server port (587 for TLS, 465 for SSL) |
| `SMTP_USERNAME` | Optional | *(Empty)* | SMTP account username (e.g., your-email@gmail.com) |
| `SMTP_PASSWORD` | Optional | *(Empty)* | SMTP account password or 16-char Google App Password |
| `SMTP_AUTH` | Optional | `true` | Enable SMTP authentication |
| `SMTP_STARTTLS` | Optional | `true` | Enable STARTTLS command encryption |
| `SMTP_FROM` | Optional | `no-reply@mediguide.local` | Default sender address on verification emails |
| `FRONTEND_URL` | Optional | `http://localhost:5173` | Client URL used to construct verification links |
| `EMAIL_VERIFICATION_COOLDOWN_SECONDS` | Optional | `60` | Cooldown period between resend verification requests |
| `EMAIL_VERIFICATION_EXPIRY_HOURS` | Optional | `24` | Token lifespan before expiration |
| `AI_API_KEY` | Optional | *(Empty)* | OpenAI or compatible API key for summarizer |
| `AI_BASE_URL` | Optional | `https://api.openai.com/v1` | LLM provider endpoint |
| `AI_MODEL` | Optional | `gpt-4o-mini` | LLM model identifier |

#### Starting the Backend

**Windows PowerShell:**
```powershell
cd backend

# Set required environment variables
$env:DB_PASSWORD="your_postgres_password"
$env:JWT_SECRET="your_secure_development_jwt_secret_key_32_chars_min"

# Optional: Real email delivery via Gmail SMTP (requires App Password)
# $env:SMTP_USERNAME="your-clinic@gmail.com"
# $env:SMTP_PASSWORD="your_16_char_app_password"
# $env:SMTP_FROM="your-clinic@gmail.com"

# Optional: Set AI summarizer key if testing live LLM generation
# $env:AI_API_KEY="sk-..."

# Run Spring Boot
mvn spring-boot:run
```

**Linux / macOS (Bash):**
```bash
cd backend
export DB_PASSWORD="your_postgres_password"
export JWT_SECRET="your_secure_development_jwt_secret_key_32_chars_min"
mvn spring-boot:run
```

- **Backend Port:** `http://localhost:8080`
- **Health Check Endpoint:** `http://localhost:8080/api/v1/health`
- **Interactive Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI Schema Definition:** `http://localhost:8080/api-docs`

---

### 4. Configuring Real Email Delivery (e.g. Gmail SMTP)

For local development and automated testing, mock email dispatch (`TestMailConfig`) is active by default in the test suite so no external accounts are required.

To test real email delivery to your inbox during manual testing:

1. **Enable 2-Step Verification** on your Google Account (`myaccount.google.com/security`).
2. **Generate an App Password:**
   - Navigate to **Security** → **2-Step Verification** → **App passwords**.
   - Create an app password with name "MediGuide Care".
   - Copy the generated 16-character password (e.g. `abcd efgh ijkl mnop`).
3. **Launch Backend with SMTP Credentials:**
   ```powershell
   $env:SMTP_USERNAME="yourname@gmail.com"
   $env:SMTP_PASSWORD="abcdefghijklmnop"
   $env:SMTP_FROM="yourname@gmail.com"
   mvn spring-boot:run
   ```
4. **Provider Limits & Considerations:**
   - Gmail imposes sending limits (typically 500 emails/day for standard Gmail accounts).
   - If SMTP credentials are omitted, the backend runs normally; token generation and verification endpoints function, and dispatch failures log a warning without crashing or rolling back user registration.

---

### 5. Frontend Configuration & Startup (React + Vite)

The frontend client is located in the `frontend/` directory.

#### Configuration Variables
Create a local environment file `frontend/.env.local` if you need to override default settings (see `frontend/.env.example`):

```bash
VITE_API_BASE_URL=http://localhost:8080
```

> ⚠️ **Important Security Rule:** Never store `JWT_SECRET`, database passwords, or private API keys in `frontend/.env` or client code.

#### Starting the Frontend

```powershell
cd frontend
npm install
npm run dev
```

- **Frontend Application URL:** `http://localhost:5173`

---

## 🧪 Testing Guide

MediGuide Care maintains extensive test suites across both backend and frontend layers.

### Backend Automated Tests
Backend tests run against an isolated in-memory H2 database configured in PostgreSQL compatibility mode (`backend/src/test/resources/application.properties`). No running PostgreSQL instance is required to execute the test suite.

```powershell
cd backend

# Run the complete test suite (298 tests)
mvn test

# Run a specific test class
mvn test -Dtest=EmailVerificationServiceTest
mvn test -Dtest=EmailVerificationControllerTest
mvn test -Dtest=PatientProfileControllerTest
mvn test -Dtest=DoctorProfileControllerTest
mvn test -Dtest=CompleteWorkflowEndToEndTest
```

### Frontend Linting & Production Build Verification

```powershell
cd frontend

# Run linter
npm run lint

# Verify production build bundle
npm run build
```

---

## 🔧 Common Troubleshooting & Pitfalls

### 1. `JWT_SECRET must be at least 256 bits (32 characters)`
- **Symptom:** Backend fails startup with `IllegalArgumentException` from `JwtService`.
- **Solution:** Ensure `$env:JWT_SECRET` has at least 32 characters (e.g. `your_secure_development_jwt_secret_key_32_chars_min`).

### 2. `Connection to localhost:5432 refused`
- **Symptom:** Backend fails with `PSQLException` on startup.
- **Solution:** Verify the PostgreSQL service is running:
  - Windows: Check Services (`services.msc`) for `postgresql-x64-16`.
  - Ensure database `mediguide_db` has been created.

### 3. Port Conflicts (`Port 8080 or 5173 already in use`)
- **Solution:**
  - If port 8080 is occupied, set `server.port=8081` in `application.properties` and update `VITE_API_BASE_URL=http://localhost:8081` in `frontend/.env.local`.
  - If Vite picks port 5174 due to 5173 being busy, CORS on the backend already supports both `http://localhost:5173` and `http://localhost:3000`. If running on another port, add it to `CorsConfig.java`.
