# ShiftSync Backend

ShiftSync is a Spring Boot-based REST API backend for managing workforce scheduling across multiple locations. It handles employee availability, shift assignments, leave requests, notifications, and audit logging, while enforcing role-based access control.

---

## Project Overview

Horizon Hospitality Group (HHG) operates multiple restaurants and retail locations, and shift scheduling is a challenge. ShiftSync provides a clean, well-documented API backend to streamline scheduling, enforce business rules, and prevent conflicts.

Key features:

- Employee and user account management
- Role-based authentication with JWT
- Multi-location and department support
- Employee availability management (recurring & one-off)
- Leave request workflow
- Shift creation, assignment, and conflict detection
- Shift swap requests
- Asynchronous notifications
- Audit logging via Spring AOP
- Reporting endpoints (JSON & CSV)
- OpenAPI/Swagger documentation

---

## Tech Stack

- **Language:** Java 17+  
- **Framework:** Spring Boot 3.x  
- **Auth:** Spring Security + JWT  
- **Database:** PostgreSQL (with Flyway/Liquibase migrations)  
- **Caching:** Spring Cache + Caffeine  
- **Async:** Spring @Async  
- **Testing:** JUnit 5 + Mockito + Spring Boot Test  
- **API Docs:** SpringDoc OpenAPI (Swagger UI at `/swagger-ui.html`)  
- **Build Tool:** Maven  

---

## Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/eugenenaokye99/shiftsync.git
   cd shiftsync

2. Create and switch to a feature branch:

   ```bash
   git checkout -b feature/<feature-name>
   ```

3. Build and run locally:

   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

4. Access API documentation:

   ```
   http://localhost:8080/swagger-ui.html
   ```

---

## Branching & Workflow

* **Main branches:** `main` (production-ready), `develop` (integration branch)
* **Feature branches:** `feature/<feature-name>`
* **Pull Requests:** Create a PR from your feature branch to `develop` for review and CI validation
* CI/CD is configured to run build and tests on `main`, `develop`, and `feature/**` branches

---

## Milestones

This project is organized into weekly milestones:

1. Foundation & Authentication
2. Availability & Leave
3. Shift Scheduling & Conflict Detection
4. Async Notifications & Swap Requests
5. Audit Logging, Caching & Reporting
6. Testing, Documentation & Polish

Each milestone includes specific features and checkpoints.

---

## Notes

* All passwords are hashed with BCrypt
* JWTs are used for role-based access control
* Audit logs are append-only and stored for all write operations
* Shift conflict detection is enforced before persisting assignments

