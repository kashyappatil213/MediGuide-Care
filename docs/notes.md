# MediGuide Care — Development & Learning Notes

## Project Overview
MediGuide Care is a Doctor–Patient Consultation and Follow-up Management System developed as an MCA academic project.

---

## Architecture Principles
1. **Separation of Concerns:**
   - **Frontend (`frontend/`):** React.js client developed in Visual Studio Code with Vite.
   - **Backend (`backend/`):** Java Spring Boot REST API developed in IntelliJ IDEA with Apache Maven.
   - **Database:** PostgreSQL database with Spring Data JPA / Hibernate ORM.
   - **Documentation (`docs/`):** Development notes, database schemas, and academic documentation.

2. **Backend Design Pattern (Controller → Service → Repository):**
   - **Controller:** Accepts incoming HTTP requests, performs input validation, and returns responses.
   - **Service:** Contains business logic, role-based checks, and transaction boundaries.
   - **Repository:** Interfaces with PostgreSQL using Spring Data JPA.

3. **Academic Best Practices:**
   - Keep code readable, simple, and beginner-friendly.
   - Never commit passwords or secret keys directly into code or version control.
   - Handle edge cases gracefully with clear error responses.
