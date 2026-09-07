# OMARISE — Backend

E-learning platform backend, Spring Boot microservices architecture (Spring
Boot 4.1.0 / Java 17 / spring-cloud 2025.1.2), MongoDB Atlas, RabbitMQ.

## Services

| Service | Port | Role |
|---|---|---|
| `eureka-server` | 8761 | Service discovery |
| `api-gateway` | 8080 | Single entry point, routes `/api/**` to each service |
| `user-service` | 8081 | Auth (JWT), user/admin management, Google sign-in |
| `course-service` | 8082 | Courses, chapters, videos, quizzes, enrollments, certificates |
| `notification-service` | 8083 | Notifications (RabbitMQ consumer + SSE) |
| `training-service` | 8084 | Trainings, live sessions, recordings, calendar |
| `forum-service` | 8085 | Discussion forum (posts, comments, bookmarks, upvotes) |

All services register with Eureka and are reached through the API Gateway
(`http://localhost:8080/api/...`). JWT auth uses a single shared secret
across the 5 business services.

## Running locally

1. Start `eureka-server` first, then `api-gateway`, then the 5 business
   services (any order).
2. Each business service needs environment variables set before it will
   start — see [`ENV_SETUP.md`](./ENV_SETUP.md) for the full list.
