# HomeSafe Architecture

HomeSafe is organized as a full-stack React application with a separate API and PostgreSQL database.

## Frontend

- React + Vite
- Responsive SaaS-style interface
- Apartment search and filtering
- Recommendation cards
- Apartment details panel
- Price history chart
- User dashboard preview
- Admin analytics preview

## Backend

- Java + Spring Boot
- REST API under `/api`
- Spring Data JPA / Hibernate
- PostgreSQL database
- Bean Validation and typed DTOs

## Database

Main entities:

- `Apartment`: rental listing, location, price, scores, source and risk data
- `Infrastructure`: nearby school, hospital, transport, supermarket and kindergarten distances
- `PriceHistory`: apartment price changes over time

## Scoring Model

The system evaluates every apartment by three main indicators:

- Trust Score: source reliability, verification status, fraud risk, listing consistency
- Value Score: price difference compared with average market price
- Comfort Score: family suitability and nearby infrastructure

The recommendation explanation is generated from the same factors that influence the scores.

## Data Flow

1. Listings are collected from open sources or partner APIs.
2. Data is normalized into a unified apartment model.
3. The backend calculates supporting indicators and stores them in PostgreSQL.
4. The frontend displays ranked apartments and explains every recommendation.
5. Admin analytics highlights suspicious listings and source quality.
