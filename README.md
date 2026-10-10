# MediGuide Care 🩺

> **Doctor–Patient Consultation and Follow-up Management System**  
> MCA Academic Project

---

## 📌 Technology Stack

- **Backend:** Java 17, Spring Boot 3.2.5 (Maven)
- **Frontend:** React 19, Vite (Vanilla CSS design system)
- **Database:** PostgreSQL 16
- **ORM / Persistence:** Spring Data JPA / Hibernate
- **API Documentation & Testing:** Swagger / OpenAPI 3 & Postman
- **Version Control:** Git

---

## 📁 Project Architecture & Directory Structure

```text
MediGuide Care/
├── .gitignore                      # Git ignore rules for Maven, Node, and IDE artifacts
├── README.md                       # Master project documentation
│
├── backend/                        # Java + Spring Boot Backend (IntelliJ IDEA)
│   ├── pom.xml                     # Maven dependencies (Web, Data JPA, PostgreSQL, Swagger)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/mediguide/
│   │   │   │   ├── MediGuideApplication.java    # Main Spring Boot entry point
│   │   │   │   ├── controller/
│   │   │   │   │   └── HealthController.java    # REST API: /api/v1/health
│   │   │   │   ├── service/
│   │   │   │   │   └── HealthService.java       # DB connectivity & system health checks
│   │   │   │   └── config/
│   │   │   │       ├── CorsConfig.java          # Cross-Origin resource sharing (5173/3000)
│   │   │   │       └── OpenApiConfig.java       # Swagger / OpenAPI documentation bean
│   │   │   └── resources/
│   │   │       ├── application.properties       # Spring Boot config (reads DB env vars)
│   │   │       └── application.properties.example # Sample dev configuration
│   │   └── test/
│   │       └── java/com/mediguide/
│   │           └── MediGuideApplicationTests.java # Spring context integration test
│
└── frontend/                       # React.js Frontend (Visual Studio Code)
    ├── package.json                # React dependencies and scripts
    ├── vite.config.js              # Vite configuration
    ├── index.html                  # HTML entry point with medical typography
    └── src/
        ├── main.jsx                # React root mount
        ├── index.css               # Design system & CSS custom properties
        ├── App.jsx                 # Live system health diagnostics UI & dashboard
        └── App.css                 # Premium clinical styling and micro-animations
```

---

## 🚀 Running the Application Locally

### 1. Database Setup (PostgreSQL)
Ensure PostgreSQL is running on `localhost:5432` and create the database:
```sql
CREATE DATABASE mediguide_db;
```

### 2. Backend (Spring Boot)
Open the `backend/` folder in IntelliJ IDEA or run via Maven in the terminal:
```bash
cd backend
# Set database password via environment variable (never commit passwords to git)
$env:DB_PASSWORD="your_postgres_password"    # On Windows PowerShell
# or: export DB_PASSWORD="your_postgres_password" # On Linux/macOS

mvn spring-boot:run
```
- **Backend URL:** `http://localhost:8080`
- **Health Diagnostic Endpoint:** `http://localhost:8080/api/v1/health`
- **Interactive Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI Schema:** `http://localhost:8080/api-docs`

### 3. Frontend (React)
Open the `frontend/` folder in Visual Studio Code or run via terminal:
```bash
cd frontend
npm install
npm run dev
```
- **Frontend URL:** `http://localhost:5173`

---

## 📋 Implementation Roadmap

| Phase | Module | Status |
| :--- | :--- | :--- |
| **Phase 1** | **Project Setup, Spring Boot + PostgreSQL, React, Health Verification** | **Completed** ✅ |
| **Phase 2** | Doctor & Patient Registration/Login, Spring Security + JWT, RBAC | Upcoming ⏳ |
| **Phase 3** | Patient Profiles, Appointments, Doctor Consultations, Prescriptions | Upcoming ⏳ |
| **Phase 4** | Medical Reports, Follow-up Records, Care Timeline, QR-based sharing | Upcoming ⏳ |
| **Later** | Docker, Redis, Jenkins, and Optional Clinical AI feature | Future ⏳ |
