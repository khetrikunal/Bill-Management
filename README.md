# Bill Manager – GST Tax Invoice system

Replaces the Excel billing sheets (see `docs/ANALYSIS.md`) with a web app that works on phones and computers.
React + Vite + TypeScript + Tailwind · Spring Boot 3 (Java 21) · PostgreSQL · PDF via OpenPDF · Excel via Apache POI.

> **Honest status:** this code was written without internet access, so the backend and frontend were **not compiled or run** in the authoring environment. Only the calculation and amount-in-words logic was executed and checked (client TypeScript and server Java). Expect to fix the occasional small compile error on first build; see `docs/TESTING.md`.

## What it does
Two document types (from your two sheets), automatic GST calculation, live preview, PDF download/print/share, Excel export, searchable records, auto invoice numbers (`SSE-2026-27-0001`, April–March year), draft → finalize → cancel workflow with audit history, buyers and product lists, company settings, roles (Admin / Billing staff / Viewer), dashboard.

## Run on Windows (development)
1. Install: **JDK 21**, **Maven** (`winget install Apache.Maven`), **Node.js 22 LTS**, **PostgreSQL 16**.
2. Create the database (PowerShell, `psql` on PATH):
   ```
   psql -U postgres -c "CREATE USER billing WITH PASSWORD 'your-db-password';"
   psql -U postgres -c "CREATE DATABASE billing OWNER billing;"
   ```
   Tables are created automatically by Flyway migrations on first start.
3. Backend (new terminal):
   ```
   cd backend
   $env:DB_PASSWORD="your-db-password"
   $env:JWT_SECRET="paste-at-least-32-random-characters-here"
   $env:ADMIN_PASSWORD="choose-a-strong-password"
   mvn spring-boot:run
   ```
4. Frontend (another terminal):
   ```
   cd frontend
   npm install
   npm run dev
   ```
5. Open http://localhost:5173 and sign in as `admin` with your `ADMIN_PASSWORD`. To use it from a phone on the same Wi-Fi, run `npm run dev -- --host` and open `http://<PC-IP>:5173`.

## Run with Docker (also the easiest way to deploy)
```
copy .env.example .env      # then edit: JWT_SECRET, ADMIN_PASSWORD, POSTGRES_PASSWORD
docker compose up -d --build
```
App: http://localhost:8081 (nginx serves the frontend and proxies `/api` to the backend).

## Deploy online
Use any VPS (or a PaaS that runs Docker). Put the stack behind HTTPS (Caddy, Nginx + Let's Encrypt, or the host's TLS). Set strong values in `.env`, keep PostgreSQL port closed to the internet, back up the `pgdata` volume (`pg_dump`), change the admin password after first login. Logging in from any device then works on the same data.

## Using it
* **Create bill:** Create Bill → choose type → buyer, date, P.O., items (type a product name to pick a saved product) → Save draft → check preview → **Finalize** (assigns the permanent number).
* **Download / print:** open the bill → *Download PDF*, *Print* (browser dialog, A4) or *Share / Save to phone* (phone share sheet lets you print or send on WhatsApp). Choose Original/Duplicate/Triplicate first.
* **Find old bills:** Bill Records → search, filter, sort, *Export to Excel*.
* **Corrections:** finalized bills are locked. Use *Duplicate* to make a corrected draft; an Admin can *Cancel* the wrong one (reason is recorded).

## Configuration
Backend environment variables: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (≥32 chars, required), `JWT_EXPIRY_MINUTES`, `ADMIN_USERNAME`, `ADMIN_PASSWORD` (used only when no users exist), `CORS_ORIGINS`, `PORT`. See `.env.example`. Company details, prefix, digits, financial-year option, GST defaults, page size, logo, bank and declaration are edited in **Company Settings**.

## Layout
`backend/` Spring Boot (`domain`, `repo`, `service`, `web`, `security`, Flyway SQL in `resources/db/migration`) · `frontend/` React app · `docs/` analysis, API, test plan.

## Known limits
* PDF uses the built-in Helvetica font: English/Latin text only. Marathi/Hindi names would need an embedded Unicode font (e.g. Noto Sans) in `PdfService`.
* Only CGST+SGST (as in your sheets); no IGST for inter-state sales.
* No historical invoices imported: the workbook has none (see analysis). No Testcontainers integration tests; use the manual checklist.
* Passwords are not recoverable by email; an Admin resets them in Users.
