# HomeSafe Spring Backend

This module is the Java + Spring Boot backend for HomeSafe.

Current state:

- Spring Boot application scaffolded
- PostgreSQL + JPA configuration added
- session-based authentication flow ported
- admin and user API controllers added
- source sync endpoint now wired to Java services
- OLX and DIM.RIA provider clients added
- listing fingerprint, scoring, duplicate detection, import runs, and price history logic added
- legacy Node backend still remains in `../backend` as a temporary reference during migration

Recommended next step in IntelliJ IDEA:

1. Open `C:\Users\Anna\Documents\HomeSafe\backend-java\pom.xml` as a Maven project
2. Let IntelliJ download Spring dependencies
3. Make sure your environment variables are present:
   - `DATABASE_URL`
   - `DATABASE_USERNAME`
   - `DATABASE_PASSWORD`
   - `OLX_API_KEY`
   - `DIM_RIA_API_KEY`
4. Run `ua.homesafe.HomesafeBackendJavaApplication`
5. Point frontend `VITE_API_URL` to `http://localhost:8080/api`

Important:

- The frontend now builds from `frontend` with React + TypeScript.
- The Java backend contains the new sync and scoring flow, but I could not run a Maven build in this environment because Maven is not installed here.
- If IntelliJ highlights any dependency issue, use Maven reload inside IDEA and it should resolve the Spring libraries there.
- A test admin account is created on startup for local testing:
  - email: `admin@homesafe.local`
  - password: `Admin12345!`
