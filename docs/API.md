# REST API

All endpoints except login need `Authorization: Bearer <token>`. Errors: `{timestamp,status,message,fieldErrors?}`.
Roles: **ADMIN** everything · **BILLING** create/edit/finalize/duplicate/delete drafts, masters · **VIEWER** read/download only. Cancel and settings/users are ADMIN only.

| Method & path | Role | Purpose |
|---|---|---|
| POST /api/auth/login | public | `{username,password}` → `{token,user}` (5 failures/10 min lock per username) |
| GET /api/auth/me · POST /api/auth/change-password | any | current user / change own password |
| GET /api/document-types | any | the two document types |
| GET /api/dashboard | any | counts, recent bills, finalized totals this month |
| GET /api/bills | any | search: `q,from,to,type,status,sort,dir,page,size` (sort: invoiceDate, grandTotal, createdAt, invoiceNumber, buyerName) |
| POST /api/bills | ADMIN, BILLING | create draft |
| GET /api/bills/{id} | any | full bill |
| PUT /api/bills/{id} | ADMIN, BILLING | edit **draft** only (409 otherwise); optional `version` for stale-edit detection |
| POST /api/bills/{id}/finalize | ADMIN, BILLING | assigns invoice number, freezes seller details |
| POST /api/bills/{id}/cancel | ADMIN | `{reason}`; keeps number |
| POST /api/bills/{id}/duplicate | ADMIN, BILLING | new draft copy; original untouched |
| DELETE /api/bills/{id} | ADMIN, BILLING | delete a draft |
| GET /api/bills/{id}/history | any | audit trail |
| GET /api/bills/{id}/pdf?copy=ORIGINAL\|DUPLICATE\|TRIPLICATE&inline=false | any | PDF |
| GET /api/bills/export | any | Excel export (same filters as list) |
| GET/PUT /api/settings | any / ADMIN | company settings |
| GET/POST/PUT/DELETE /api/customers, /api/products | read any, write ADMIN+BILLING | masters |
| POST /api/products/import | ADMIN, BILLING | multipart `file`, `mode=legacy\|table`, `sheet`, `commit=false\|true` |
| GET/POST/PUT /api/users | ADMIN | user management |
