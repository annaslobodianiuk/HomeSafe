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
- DIM.RIA uses the official API, and OLX is imported through a public listing parser into the same moderation flow

Recommended next step in IntelliJ IDEA:

1. Open `C:\Users\Anna\Documents\HomeSafe\backend-java\pom.xml` as a Maven project
2. Let IntelliJ download Spring dependencies
3. Make sure your environment variables are present:
   - `DATABASE_URL`
   - `DATABASE_USERNAME`
   - `DATABASE_PASSWORD`
   - `DIM_RIA_API_KEY`
4. Run `ua.homesafe.HomesafeBackendJavaApplication`
5. Keep PostgreSQL running from `docker compose up -d`
6. Point frontend `VITE_API_URL` to `http://localhost:8080/api` if you do not use the default local address

Local startup note:

- the backend uses the `dev` profile by default;
- in `dev`, Hibernate runs with `ddl-auto: update`, so the schema is created or updated automatically for a local PostgreSQL database;
- if you want strict mode, run with the `prod` profile, where `ddl-auto` stays disabled.

Important:

- The frontend now builds from `frontend` with React + TypeScript.
- The Java backend contains the new sync and scoring flow, but I could not run a Maven build in this environment because Maven is not installed here.
- If IntelliJ highlights any dependency issue, use Maven reload inside IDEA and it should resolve the Spring libraries there.
- Local demo data is seeded on application startup for sources and the administrator account.
