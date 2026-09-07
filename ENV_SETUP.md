# Environment variables required to run OMARISE backend locally

None of the services will start without these — `application.yml` in each
service now references them via `${VAR_NAME}` instead of hardcoding real
values, so the repo is safe to make public.

Set these as environment variables (in your OS, or per-run in your IDE's Run
Configuration → Environment variables field) before starting each service.
Ask the project owner for the actual values — they are **not** stored in
this repository.

## Shared across all 5 business services (user, course, training, forum, notification)

- `MONGO_DB_USER` — MongoDB Atlas username
- `MONGO_DB_PASSWORD` — MongoDB Atlas password
- `JWT_SECRET` — must be the **same value** on all 5 services (shared JWT verification)

## user-service, course-service, training-service only

- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`

## user-service only

- `MAIL_USERNAME` — Gmail address used to send verification/reset emails
- `MAIL_PASSWORD` — Gmail app password (not your regular Gmail password)

## Optional (sensible defaults already in place)

- `RABBITMQ_HOST` (default `localhost`)
- `RABBITMQ_PORT` (default `5672`)
- `RABBITMQ_USERNAME` (default `guest`)
- `RABBITMQ_PASSWORD` (default `guest`)
- `MAIL_HOST` (default `smtp.gmail.com`)
- `MAIL_PORT` (default `587`)

`api-gateway` and `eureka-server` need no environment variables — they never
held any secrets.
