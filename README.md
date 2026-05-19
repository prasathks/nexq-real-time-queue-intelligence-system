# NexQ — Enterprise Queue Management System

> A production-grade, distributed queue management platform built with a microservices-inspired architecture. Designed for high-concurrency environments with real-time updates, AI-powered wait time prediction, and multi-branch enterprise support.

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────────┐
│                       NexQ System                        │
│                                                          │
│  ┌─────────────┐   WebSocket/STOMP   ┌───────────────┐   │
│  │  Browser    │◄───────────────────►│  Spring Boot  │   │
│  │  (HTML/JS)  │   REST API (JWT)    │  (Port 8080)  │   │
│  └─────────────┘                     └───────┬───────┘   │
│                                              │           │
│                              ┌───────────────┼─────────┐ │
│                              │               │         │ │
│                        ┌─────▼─────┐   ┌─────▼───┐     │ │
│                        │  MySQL 8  │   │ Redis 7 │     │ │
│                        │(Persist.) │   │(Queue   │     │ │
│                        └───────────┘   │ Engine) │     │ │
│                                        └─────────┘     │ │
│                        ┌─────────────────────────┐     │ │
│                        │  Python FastAPI         │◄────┘ │
│                        │  AI Wait Predictor      │       │
│                        │  (Port 8000)            │       │
│                        └─────────────────────────┘       │
└──────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Backend Core** | Java 17, Spring Boot 3.x, Spring Security (JWT) |
| **Data** | Spring Data JPA (Hibernate), MySQL 8.0 |
| **Queue Engine** | Redis 7 (atomic `INCR` for token generation) |
| **Real-Time** | Spring WebSockets (STOMP + SockJS) |
| **AI Microservice** | Python 3.11, FastAPI, scikit-learn (LinearRegression) |
| **Frontend** | HTML5, CSS3, Vanilla JavaScript, Bootstrap 5 |
| **Infrastructure** | Docker, Docker Compose |
| **API Docs** | Swagger / OpenAPI 3 |
| **Async Mail** | Spring `@Async` + JavaMailSender |

---

## Key Engineering Decisions

### 1. Redis-Backed Token Generation
Instead of using `SELECT ... FOR UPDATE` (MySQL pessimistic write lock), token numbers are now generated using Redis atomic `INCR`. This allows thousands of concurrent queue joins per second without any database row-locking.

```java
// One atomic operation — no race conditions
Long nextToken = redisTemplate.opsForValue().increment("queue:" + queueId + ":counter");
```

### 2. WebSockets (STOMP) over SSE
Replaced Server-Sent Events (SSE) with a bidirectional STOMP WebSocket broker. When a Staff member calls "Serve Next", the event is broadcast to all subscribed clients (User Dashboards, Lobby Displays) instantly.

### 3. Priority Tiers with Smart Routing
Queues support three tiers: `Normal (0)`, `Priority (1)`, and `Emergency (2)`. The JPA repository query sorts by `priorityWeight DESC, tokenNumber ASC`, meaning higher priority tokens bubble to the front without disrupting the existing FIFO numbering.

### 4. Python AI Microservice
A separate FastAPI microservice uses a `LinearRegression` model trained on historical queue data to dynamically predict wait times. The Spring Boot core calls this service via `RestTemplate` on every token generation.

### 5. Multi-Branch SaaS Architecture
The data model supports multi-tenancy: a `Branch` entity groups `Queue` entities under physical locations. This is the foundation for serving multiple enterprise clients from a single deployment.

---

## Features

- **Role-Based Dashboards:** Separate views for `USER`, `STAFF`, and `ADMIN` roles.
- **QR Code Joining:** Users can scan a QR code to auto-join a queue.
- **Lobby Display Board:** A full-screen TV display (`/lobby-display.html`) for waiting rooms with live updates, large token numbers, and a chime notification.
- **Real-Time Analytics:** Admin dashboard with Chart.js graphs (daily volume, peak hours, avg wait time).
- **Token Expiry:** A `@Scheduled` cron job auto-expires stale tokens every minute.
- **Capacity Limits:** Queues can be hard-capped at a maximum number of waiting tokens.
- **Priority Emergency Routing:** Emergency tokens get immediately routed to the front of the queue.

---

## Running Locally

### Option A: Docker (Full Stack — Recommended)
> Requires Docker Desktop.

```bash
git clone <your-repo-url>
cd NexQ
docker compose up --build
```
This spins up **MySQL**, **Redis**, the **Spring Boot app**, and the **Python AI service** together.

Access at: `http://localhost:8080`

---

### Option B: Manual (Without Docker)

**Prerequisites:** Java 17, Maven, Python 3.11+, a running MySQL server, a running Redis server.

**1. Start the Python AI Service:**
```bash
cd ai-service
pip install -r requirements.txt
uvicorn main:app --port 8000
```

**2. Start the Spring Boot App:**
```bash
# From the project root
mvn spring-boot:run
```

**3.** Access at `http://localhost:8080`.

**Swagger API Docs:** `http://localhost:8080/swagger-ui.html`

---

## Project Structure

```
NexQ/
├── src/main/java/com/nexq/
│   ├── config/         # Security, WebSocket, CORS, Redis config
│   ├── controller/     # REST & WebSocket controllers
│   ├── dto/            # Request/Response DTOs
│   ├── exception/      # Global exception handling
│   ├── model/          # JPA entities (User, Queue, Token, Branch)
│   ├── repository/     # Spring Data JPA repositories
│   └── service/        # Business logic
├── src/main/resources/
│   └── static/         # HTML, CSS, JS frontend
│       ├── index.html
│       ├── admin-dashboard.html
│       ├── staff-dashboard.html
│       ├── user-dashboard.html
│       └── lobby-display.html  ← TV display board
├── ai-service/         # Python FastAPI microservice
│   ├── main.py
│   ├── requirements.txt
│   └── Dockerfile
├── Dockerfile          # Spring Boot multi-stage build
└── docker-compose.yml  # Orchestrates all 4 containers
```

---

## API Endpoints (Summary)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register a new user |
| `POST` | `/api/auth/login` | Login and receive JWT |
| `GET` | `/api/queues` | List all active queues |
| `POST` | `/api/queues` | Create a queue (Admin/Staff) |
| `POST` | `/api/queues/{id}/tokens` | Join a queue (get a token) |
| `PUT` | `/api/queues/{id}/tokens/serve-next` | Serve next token (Staff) |
| `PUT` | `/api/tokens/{id}/complete` | Mark token complete (Staff) |
| `GET` | `/api/branches` | List all branches |
| `POST` | `/api/branches` | Create a branch (Admin) |
| `GET` | `/api/analytics/queues/{id}/daily-stats` | Analytics data |

---

## Tests

```bash
mvn clean test
```
