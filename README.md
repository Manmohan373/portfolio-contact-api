# Portfolio Contact API

Spring Boot 3 / Java 21 service that receives the portfolio contact form and emails it to you via SMTP.

```
Browser (GitHub Pages)  --POST /api/contact (JSON)-->  contact-api  --SMTP-->  your Gmail inbox
```

**What it does:** validates input, ignores bots (hidden honeypot field), rate-limits per IP
(5 requests / 10 min), blocks mail-header injection, allows only your portfolio origin via CORS, and
sets **Reply-To = the visitor**, so hitting *Reply* in Gmail answers them directly.

## 1. Create a Gmail App Password (one time)
1. Turn on **2-Step Verification**: https://myaccount.google.com/security
2. Create an app password: https://myaccount.google.com/apppasswords → name it `portfolio`.
3. Copy the 16-character password. This is `MAIL_PASSWORD` (never your normal password; never commit it).

## 2. Run locally
```bash
export MAIL_USERNAME=your.gmail@gmail.com
export MAIL_PASSWORD=xxxxxxxxxxxxxxxx
export CONTACT_TO=pattnaikmanmohan373@gmail.com
mvn spring-boot:run

curl -i -X POST http://localhost:8080/api/contact \
  -H 'Content-Type: application/json' \
  -d '{"name":"Test","email":"test@example.com","message":"Hello from curl, testing!"}'
```
Expect `200 {"success":true,...}` and an email in your inbox. Run the tests with `mvn verify`
(they use an in-memory SMTP server, so no real email is sent).

## 3. Deploy (free-tier options: Render, Railway, Fly.io)
GitHub Pages only serves static files, so the API is deployed separately. On **Render**:
1. Push this folder to its own GitHub repo (e.g. `portfolio-contact-api`).
2. Render → New → **Web Service** → pick the repo → Runtime **Docker**.
3. Environment variables:

| Variable | Value |
|---|---|
| `MAIL_USERNAME` | your Gmail address |
| `MAIL_PASSWORD` | the app password |
| `CONTACT_TO` | `pattnaikmanmohan373@gmail.com` |
| `ALLOWED_ORIGINS` | `https://manmohan373.github.io` |

4. Health check path: `/actuator/health`. Note the URL Render gives you.
   (Free instances sleep when idle, so the first message after a quiet period can take ~30–60 s;
   the form shows "Sending…" and falls back to the visitor's mail app if it times out.)

## 4. Point the portfolio at it
In `Portfolio/script.js` set:
```js
var API_URL = 'https://<your-service>.onrender.com/api/contact';
```
Commit and push the portfolio. Done.

## API
`POST /api/contact` — `{ "name", "email", "message", "website" }` (`website` must stay empty; it's the honeypot)

| Status | Meaning |
|---|---|
| 200 | sent (or honeypot tripped, deliberately indistinguishable) |
| 400 | validation failed – body has `errors` per field |
| 429 | rate limited – `Retry-After` header |
| 502 | SMTP delivery failed |

## Notes
- Gmail rewrites the From address to the authenticated account; that's why Reply-To carries the visitor.
- The rate limiter is in-memory: fine for one instance; use Redis/Bucket4j if you scale out.
- Prefer a transactional provider later (Resend, Brevo, SES)? Only `MailService` needs to change.
