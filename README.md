# HomeSafe

HomeSafe is a master's thesis project: an intelligent information system for aggregating and evaluating rental apartments for refugees and internally displaced persons without intermediaries.

The repository currently contains:

- `frontend` - React + TypeScript client
- `backend-java` - Spring Boot backend

The target architecture is:

- React + Vite + TypeScript frontend
- Java + Spring Boot backend API
- PostgreSQL database
- PostgreSQL / JPA persistence
- Docker Compose for local database startup

## Main Idea

The system does not only display apartment listings. It evaluates each apartment using:

- Trust Score: reliability and fraud risk
- Value Score: price advantage compared with market averages
- Comfort Score: infrastructure, transport, and family suitability

## Quick Start

1. Install dependencies:

```bash
npm install
```

If Windows blocks the default npm cache, use the project-local cache:

```bash
npm install --cache .\.npm-cache
```

2. Start PostgreSQL:

```bash
docker compose up -d
```

Docker Desktop must be running before this command.

3. Database note:

```bash
npm run db:migrate
npm run db:seed
```

These commands are now informational only. The old Node backend workspace was removed, so there is no npm-based migration pipeline anymore.

For local development, the Spring Boot backend now starts with the `dev` profile by default and automatically creates or updates the PostgreSQL schema.

4. Run the backend:

- open `backend-java/pom.xml` in IntelliJ IDEA as a Maven project
- let IntelliJ download dependencies
- run `ua.homesafe.HomesafeBackendJavaApplication`

If you want the strict mode without automatic schema updates, start the backend with the `prod` profile.

5. Run the frontend:

```bash
npm run dev
```

Frontend: http://localhost:5173  
Backend API: http://localhost:8080/api

## Demo Accounts

Administrator:

- Email: `admin@homesafe.ua`
- Password: `Admin123!`

Approved user:

- Email: `user@homesafe.ua`
- Password: `User123!`

Pending user:

- Email: `pending@homesafe.ua`
- Password: `User123!`

The administrator can approve or reject pending user registrations from the administrator dashboard.

## Real Listing Integrations

The backend includes provider adapters for DIM.RIA and OLX, raw listing staging, normalization, cross-provider deduplication, market-segment scoring, moderation statuses, import history, and scheduled synchronization.

Important integration note:

- `DIM.RIA` can be used for real public listing search by filters.
- `OLX` is currently imported through a public listing parser flow, because the supplied partner API is not used for full public marketplace search in this project.

Real synchronization requires private API keys in backend environment variables:

```env
DIM_RIA_API_KEY="your-key"
```

Both sources are disabled until keys are configured and an administrator enables them. See `docs/data-ingestion.md` for the ingestion architecture and administrator endpoints.

## Current Demo Scope

The first version includes:

- responsive React interface
- apartment search filters
- apartment recommendation cards
- Trust, Value, and Comfort scoring
- price history chart
- fraud risk explanation
- infrastructure distance analysis
- user dashboard preview
- admin analytics preview
- registration and login
- administrator approval workflow
- role-based user and administrator dashboards
- protected apartment favorites
- PostgreSQL persistence for local development

## Thesis Focus

Recommended thesis formulation:

> Intelligent system for aggregation and evaluation of rental housing for internally displaced persons based on analysis of open data sources.
