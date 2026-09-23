# RideLink — Backend Microservices (Java 17 + Spring Boot)

Complies with the lecturer's mandatory technology requirement: **all four microservices
use Java + Spring Boot** (no Node.js/Express/MERN). MongoDB is used per-service.

> ⚠️ This code was written and organised carefully, but **could not be compiled or
> run in the environment it was generated in** (no Maven Central access there). You
> must build and test it on your own machine (which has internet access) as the very
> first step — see below — and fix any small compile issues that come up (wrong
> import, a typo) before relying on it. Treat this as a strong first draft, not a
> guaranteed-working build.

## Services & Ownership

| # | Service | Port | Owner | Responsibility |
|---|---------|------|-------|-----------------|
| 1 | account-service | 4001 | IT24100222| Register/login, JWT issuance, roles, profile |
| 2 | driver-service | 4002 | IT23712386 | Driver profile, vehicle, availability, location |
| 3 | ride-service | 4003 |IT24100160| Ride requests, driver assignment, status lifecycle |
| 4 | payment-service | 4004 | IT24100253 | Fare estimate/final fare, simulated payment, receipts |

Fill in the actual member names before submission.

## Architecture

- Each service has **its own MongoDB database** (`account_db`, `driver_db`, `ride_db`,
  `payment_db`) on the same local MongoDB instance by default — separate persistence
  boundaries, no service queries another service's database directly.
- **Auth**: account-service issues JWTs signed with a shared `jwt.secret` (set in each
  service's `application.yml` — treat this as the "approved configuration" for a
  shared secret; nothing is hardcoded as a literal password or committed as a real
  production secret). Every other service verifies the JWT itself via a custom
  `JwtAuthFilter` (a `OncePerRequestFilter`) rather than calling account-service on
  every request.
- **Interservice communication** (both synchronous REST via `RestTemplate` — justify +
  compare with an async alternative in your report):
  1. `ride-service → driver-service`: `GET /drivers?available=true&serviceArea=...`
     (`DriverServiceClient`) when assigning a driver.
  2. `ride-service → payment-service`: `POST /payments` (`PaymentServiceClient`),
     fired automatically when a ride transitions to `COMPLETED`.
- **Ride lifecycle**: `REQUESTED → ASSIGNED → ACCEPTED → IN_PROGRESS → COMPLETED`,
  with `CANCELLED` reachable from any non-terminal state. Invalid transitions → `400`.
- **Negative scenarios implemented**: no available driver (`409`), invalid status
  transition (`400`), missing/invalid auth token (`401`), forbidden role/ownership
  (`403`), duplicate payment for a ride (`409`), failed simulated payment (`422`).
- **Error responses**: a `@RestControllerAdvice` (`GlobalExceptionHandler`) in each
  service returns a consistent `{ "error": "..." }` JSON shape for every failure.

## Fare Rule (documented, for your report)

```
finalFare = BASE_FARE + (distanceKm * PER_KM_RATE)
```

Defaults: `BASE_FARE = 100`, `PER_KM_RATE = 50` (fictional currency "Rs"),
configurable in payment-service's `application.yml`. `distanceKm` is currently a
placeholder (`5.2`) passed from ride-service — a good extension is computing it with
the Haversine formula from `ride.pickup` / `ride.destination` before calling
payment-service.

## Prerequisites

- **JDK 17** ([Adoptium Temurin](https://adoptium.net/) is a good free distribution)
- **Maven** (or use each project's `mvnw` wrapper if you add one)
- **MongoDB Community Server** running locally on the default port `27017`
  ([download here](https://www.mongodb.com/try/download/community)) — or MongoDB
  Compass if you want a GUI to inspect the databases

Verify installs:
```powershell
java -version     # should show 17.x
mvn -version
```

## Setup & Start-up Order

No `.env` files here — configuration lives in each service's
`src/main/resources/application.yml`. Start MongoDB first, then the services (driver +
payment before ride, since ride-service calls them):

```powershell
# Terminal 1 - make sure MongoDB is running (mongod), or it's already running as a service

# Terminal 2
cd account-service
mvn spring-boot:run       # port 4001

# Terminal 3
cd driver-service
mvn spring-boot:run       # port 4002

# Terminal 4
cd payment-service
mvn spring-boot:run       # port 4004

# Terminal 5
cd ride-service
mvn spring-boot:run       # port 4003
```

First run of each service downloads its dependencies from Maven Central — needs
internet access, may take a minute or two.

Health check each service: `GET http://localhost:400x/health`

## Swagger / OpenAPI

springdoc-openapi is already wired in via each service's `pom.xml` — no extra config
needed. Once a service is running:

- Swagger UI: `http://localhost:400x/swagger-ui.html`
- Raw OpenAPI JSON: `http://localhost:400x/v3/api-docs`

This satisfies the assignment's "Swagger UI/OpenAPI... official interface for
development, testing and demonstration" requirement directly — you can exercise every
endpoint from the browser without Postman if you prefer.

## Running Tests

```powershell
cd account-service && mvn test
cd driver-service  && mvn test
cd ride-service    && mvn test   # mocks the interservice REST clients (Mockito)
cd payment-service && mvn test
```

Tests use Mockito to mock the repository/HTTP-client layer — no real MongoDB needed
to run them.

## Sample Test Data / Credentials

- Passenger: `nimal@test.com` / `pass123`
- Driver: `kamal@test.com` / `pass123`
- Roles accepted at registration: `PASSENGER`, `DRIVER`, `ADMIN`

## Postman Collection

`postman/RideLink.postman_collection.json` — same requests, same ports, same JSON
field names as before, so the collection works unchanged against these Java services.
Import it into Postman and follow the same 14-request sequence, setting the
`passengerToken`, `driverToken`, `driverId`, `rideId` collection variables as you go.

## Endpoint Reference

**account-service** (4001)
- `POST /auth/register`, `POST /auth/login`
- `GET /users/{id}`, `PATCH /users/{id}`, `PATCH /users/{id}/status` (ADMIN)

**driver-service** (4002)
- `POST /drivers`, `GET /drivers/{id}`
- `PATCH /drivers/{id}/availability`, `PATCH /drivers/{id}/location`
- `GET /drivers?available=true&serviceArea=...`

**ride-service** (4003)
- `POST /rides`, `GET /rides`, `GET /rides/{id}`
- `POST /rides/{id}/assign`
- `PATCH /rides/{id}/status`

**payment-service** (4004)
- `POST /fares/estimate`
- `POST /payments`, `GET /payments/{id}`, `GET /payments?rideId=...`

## What This Starter Does NOT Yet Cover (for you to build out)

This is a working-in-design skeleton covering every required workflow end-to-end on
paper — it is **not yet a verified, finished submission**. You still need to:

1. **Build + fix compile errors first** — this was written without a Maven Central
   connection to compile against, so treat the first `mvn compile` on your machine as
   step zero.
2. Run each service, exercise it via Swagger UI/Postman, and fix any runtime issues
3. Expand unit test coverage (aim for meaningful coverage per the rubric)
4. Real distance calculation (Haversine) instead of the placeholder `5.2` in
   `RideController.updateStatus`
5. Consider one asynchronous interaction (a message queue) for at least one
   interaction, so you can compare sync vs async in the report (both interservice
   calls here are currently synchronous REST)
6. Set up CI (GitHub Actions) to build + test all four services (Maven, not npm)
7. Architecture diagram + at least one sequence diagram for the report
8. Move `jwt.secret` to an environment variable before treating this as
   production-like, and don't commit real secrets
