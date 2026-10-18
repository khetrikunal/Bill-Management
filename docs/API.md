# REST API

All endpoints except login need `Authorization: Bearer <token>`. Errors: `{timestamp,status,message,fieldErrors?}`.
Billing requests also send `X-Company-Id` (`1` for SSET, `2` for PK Corporate); the frontend remembers the selected company. The server verifies company access and invoice ownership. Invoice ownership is assigned server-side and cannot be changed by editing a request body.
Roles: **ADMIN** everything · **BILLING** create/edit/finalize/duplicate/delete drafts, masters · **VIEWER** read/download only. Cancel and company settings/users are ADMIN only. Existing and newly created users start with access to both businesses; ADMIN can change a user's company access under Users.

| Method & path | Role | Purpose |
|---|---|---|
| POST /api/auth/login | public | `{username,password}` → `{token,user}` (5 failures/10 min lock per username) |
| GET /api/auth/me · POST /api/auth/change-password | any | current user / change own password |
| GET /api/document-types | any | the two document types |
| GET /api/dashboard | any | counts, recent bills, finalized totals this month, per-company billed/received/outstanding totals and overdue count; optional `companyFilter=ALL\|1\|2` |
| GET /api/bills | any | search: `q,from,to,type,status,paymentStatus,companyFilter,sort,dir,page,size` (sort: invoiceDate, grandTotal, createdAt, invoiceNumber, buyerName) |
| POST /api/bills | ADMIN, BILLING | create draft for the selected `X-Company-Id` |
| GET /api/bills/{id} | any | full bill; selected company must own the invoice |
| PUT /api/bills/{id} | ADMIN, BILLING | edit **draft** only (409 otherwise); optional `version` for stale-edit detection |
| POST /api/bills/{id}/finalize | ADMIN, BILLING | assigns invoice number, freezes seller details |
| POST /api/bills/{id}/cancel | ADMIN | `{reason}`; keeps number |
| POST /api/bills/{id}/duplicate | ADMIN, BILLING | new draft copy; original untouched |
| DELETE /api/bills/{id} | ADMIN, BILLING | delete a draft |
| GET /api/bills/{id}/history | any | audit trail |
| POST /api/bills/{id}/payments | ADMIN, BILLING | append a full or partial payment (`amount,paymentDate,method,transactionReference,notes`); overpayments are rejected |
| GET /api/bills/{id}/pdf?copy=ORIGINAL\|DUPLICATE\|TRIPLICATE&inline=false | any | PDF |
| GET /api/bills/export | any | Excel export (same filters as list) |
| GET /api/settings/companies | any | companies the user can access |
| GET/PUT /api/settings | any / ADMIN | selected company's profile (branding, addresses, tax IDs, contacts, bank/UPI, payment terms and independent numbering configuration) |
| GET/POST/PUT/DELETE /api/customers, /api/products | read any, write ADMIN+BILLING | company-scoped buyer and product masters |
| POST /api/products/import | ADMIN, BILLING | multipart `file`, `mode=legacy\|table`, `sheet`, `commit=false\|true` |
| GET/POST/PUT /api/users | ADMIN | user management |
| GET/PUT /api/users/{id}/companies | ADMIN | read/update company grants (`{companyIds:[1,2]}`) |
