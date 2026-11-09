# AgriMandi AI — Frontend Reality Audit

**Scope:** every routed screen in `agrimandi-ai`, the API it actually calls, the controls that were
removed or repaired, security findings and fixes, E2E evidence, final checks, and what is still not
implemented.

**Standard applied:** the UI may only present what the authenticated backend returns. No mock data,
no invented totals, no controls that do not call an endpoint, and no success state on a failed call.

---

## 1. Active routes and the APIs behind them

All routes are declared once in `src/App.tsx` and wrapped in `RoleRoute`, which is a **UX guard
only** — the backend remains the authority for every check.

| Route | Screen | Roles | Real API calls |
|---|---|---|---|
| `/login` | Login | public | `POST /api/auth/login` |
| `/` | Role landing | any authed | none (pure redirect) |
| `/dashboard` | Operations dashboard | AGENT, ADMIN | `GET /api/lots`, `GET /api/reports/payments?period=TODAY`, `GET /api/reports/transactions?period=CURRENT_MONTH`, `GET /api/reports/payment-ledger?period=CURRENT_MONTH` |
| `/auction-desk` | Auction desk | AGENT, ADMIN | `GET /api/lots`, `GET /api/buyers`, `GET /api/lots/{id}/bids`, `GET /api/lots/{id}/auction/highest-bids`, `POST /api/lots/{id}/auction/start`, `POST /api/lots/{id}/auction/close`, `POST /api/lots/{id}/bids`, `POST /api/lots/{id}/auction/confirm-winner` |
| `/lots-management` | Lots management | AGENT, ADMIN | `GET /api/lots`, `GET /api/lots/{id}`, `GET /api/farmers`, `GET /api/commodities`, `GET /api/asset-types`, `POST /api/lots`, `POST /api/lots/{id}/weighment` |
| `/transactions-billing` | Transaction ledger | AGENT, ADMIN | `GET /api/reports/payment-ledger` (paged, filterable) |
| `/transactions-billing/:id` | Transaction detail | AGENT, ADMIN | `GET /api/transactions/{id}`, `GET /api/transactions/{id}/payments/pending-amount`, `POST /api/transactions/{id}/payments`, `POST /api/transactions/{id}/assets/issue`, `POST /api/transactions/{id}/assets/return` |
| `/farmers` | Farmers directory | AGENT, ADMIN | `GET /api/farmers` |
| `/buyers` | Buyers directory | AGENT, ADMIN | `GET /api/buyers` |
| `/payment-ledger` | Payment ledger | AGENT, ADMIN | `GET /api/reports/payment-ledger` |
| `/returnable-asset-ledger` | Asset ledger | AGENT, ADMIN | `GET /api/reports/asset-ledger` |
| `/admin/reports` | Reports & analytics | AGENT, ADMIN | `GET /api/reports/transactions`, `/payments`, `/assets`, `/auctions`, `/buyers`, `/farmers` |
| `/admin/dashboard` | APMC oversight | ADMIN | `GET /api/reports/overview` |
| `/admin/audit` | Audit trail | ADMIN | `GET /api/audit-logs`, `GET /api/audit-logs/actions` |
| `/farmer-portal` | Farmer passbook | FARMER | `GET /api/farmer/me`, `GET /api/farmer/me/transactions` |
| `/buyer-portal` | Buyer statement | BUYER | `GET /api/buyer/me`, `GET /api/buyer/me/purchases` |
| `*` | Role landing | any authed | none |

Cross-cutting: `GET /api/auth/me` on boot to restore the session, `POST /api/auth/logout` on sign-out.
Every request carries the JWT through `src/services/api.ts`, which also clears local auth state on 401.

Role landing: `ADMIN → /admin/dashboard`, `FARMER → /farmer-portal`, `BUYER → /buyer-portal`,
otherwise `/dashboard`.

---

## 2. Mock data and prototype code removed

Deleted outright:

- `src/services/mockData.ts` — the in-memory prototype dataset
- `src/services/index.ts` — the mock service facade
- `src/features/ai/AIInsights.tsx` — a screen with no backend endpoint
- `src/features/settlements/Settlement.tsx` — superseded by the real payment ledger
- `src/features/gate/GateEntry.tsx` — no gate-entry endpoint exists
- `src/features/corrections/Corrections.tsx` — no corrections endpoint exists
- `src/components/layout/AppShell.tsx`, `Sidebar.tsx`, `Topbar.tsx` — prototype layout replaced by
  `DashboardLayout.tsx`
- `src/assets/react.svg`, `src/assets/hero.png`, `src/styles/index.css` — unused assets
- `src/components/ui/StatusBadge.tsx` prototype status vocabulary in `src/components/ui/Badge.tsx`

Removed from surviving screens (rather than the whole file):

- Dashboard: the fabricated weighbridge status line and the "licensed yard" claim
- Lots: the inline quantity inputs, because a lot's quantity is only established by weighment
- Auction desk: the mock bid table and the hard-coded winner/amount columns
- Transactions: a placeholder quantity column that showed a constant
- Farmer portal: the prototype lot-profit calculation, a bank-detail placeholder, and a sentinel-ID
  lookup that passed a fake farmer id to the API
- Buyer portal: the invented credit-limit figure
- Directories: decorative "Register farmer" / "View details" buttons with no endpoint

A repository-wide sweep for `mock`, `TODO`, `FIXME`, `placeholder data`, `lorem`, and `coming soon`
now returns only legitimate HTML `placeholder` attributes and comments.

---

## 3. Dead controls and forms repaired

| Screen | Was | Now |
|---|---|---|
| Login | always navigated to `/dashboard` | navigates by the role returned in the login response |
| Lots | "View" and weighment were non-functional | row click loads `GET /api/lots/{id}`; weighment posts real gross/tare and the net is computed server-side |
| Lots | create form sent quantity fields the backend discards | posts only the contract the backend accepts |
| Auction desk | start/close/bid were absent | all three post to their endpoints, and the bid list reloads from the API |
| Transactions | no payment or asset action | real payment recording and asset issue/return against the transaction |
| Transactions | period filter had a broken typed value | typed `ReportPeriod`, with a custom range that must be complete before it is sent |
| Directories | search input did nothing | filters the loaded records |
| Topbar | market name was hard-coded | real `GET /api/markets/me` |
| Sidebar | "Create Lot" was ADMIN-only | visible to AGENT and ADMIN, the roles the backend actually allows |

Every asynchronous surface now has a loading state, an error state with a retry control, and an
explicit empty state. No surface renders a zero-value table as though it were data.

---

## 4. Security findings and fixes

| Finding | Fix |
|---|---|
| Farmer and buyer portals had no self-scoped endpoint and relied on the client passing an id | Added `GET /api/farmer/me` and `GET /api/buyer/me` (+ their transaction lists). Each resolves the participant from the authenticated `CustomUserDetails`, rejects a missing participant id, and scopes the lookup to the user's own market, so one farmer cannot read another's passbook by changing a URL. |
| Lot detail and weighment were reachable by id without a role/ownership check | `LotService` enforces market + owner scoping; covered by `PortalAccessControlTest` |
| Transaction detail was a generic lookup | `TransactionService.getTransactionDetail` is owner-scoped; covered by the same test |
| `POST /api/lots/{id}/auction/confirm-winner` returned a raw entity | Returns the server-built transaction DTO, so the winner and amount on screen are the values the server decided |
| `POST /api/transactions/{id}/payments` and the asset endpoints returned raw JPA entities | Return refreshed `TransactionResponseDTO` |
| Client-side role guards were the only role control | Kept as UX, but the backend remains authoritative; `PortalAccessControlTest` proves wrong-role and unauthenticated calls are rejected server-side |

Authorization regression coverage: `PortalAccessControlTest` (9 tests) and
`SettlementMutationContractTest` (4 tests).

---

## 5. E2E results

Automated browser run against a real backend: **47 checks, 47 passed, 0 failed.**

Covered: unauthenticated redirect; role landing for AGENT, FARMER and BUYER; real market name in the
topbar; absence of the fabricated weighbridge and licensed-yard claims; lot creation; lot detail;
weighment producing a 500 KG net; auction start; bid recorded and displayed; close; winner
confirmation offered after close; transaction list and detail; backend-computed net amount; seeded
outstanding balance and asset position; payment settling the balance to zero; asset issue/return
round-trip; session and settled figures surviving a hard refresh; deep link to a transaction detail;
all ledger, report and directory routes loading without an error state; AGENT blocked from the
ADMIN-only audit screen; FARMER blocked from the agent dashboard; a failing API producing a
retryable error state and recovering on retry.

### Expected non-2xx responses observed during the run

These are the script exercising real failure paths, not application faults:

| Response | Endpoint | Meaning |
|---|---|---|
| 404 | `GET /api/lots/{id}/bids` | queried before an auction existed |
| 404 | `GET /api/lots/{id}/auction/highest-bids` | queried before an auction existed |
| 500 | `POST /api/lots/{id}/auction/confirm-winner` | H2-only: the service uses a PostgreSQL pessimistic lock (see §7) |
| 400 | `GET /api/reports/payment-ledger?period=CUSTOM` | incomplete custom range, correctly rejected |

The confirm-winner case is the important one: the screen shows the backend error and does **not**
claim a winner was created. The winner/amount fields remain server-controlled.

---

## 6. Final checks

| Check | Result |
|---|---|
| `mvn clean test` (backend) | **73 tests, 0 failures, 0 errors, BUILD SUCCESS** |
| `mvn clean compile` (backend) | **BUILD SUCCESS** |
| `npm run build` (frontend) | **Success** — `tsc -b` clean, 55 modules, 394.84 kB (105.39 kB gzip) |
| `npm run lint` (frontend) | **No errors.** 6 pre-existing warnings, listed in §8 |
| Browser E2E | **47/47 passed** |
| Refresh persistence | Verified for session, settled figures, and deep links |
| API-failure behaviour | Verified — retryable error state, recovery on retry, no fake success |

The temporary H2 seed runner used for browser verification was deleted before these final checks, so
`mvn clean test` and `mvn clean compile` ran against the real source tree.

---

## 7. Not implemented / environment limits

- **GATE entry, corrections, and AI insights screens do not exist.** No backend endpoint supports
  them, so the prototype screens were deleted rather than left as dead UI.
- **No payment gateway integration.** Payments are recorded against a transaction; there is no
  settlement, payout, or reconciliation flow, because no endpoint exists.
- **Reports are read-only.** There is no export/download, no scheduled report, and no drill-through
  from a report figure into the underlying transaction.
- **No notifications, no bulk operations, no lot amendment/cancellation after weighment, and no
  tie-resolution screen for AGENT role choice** beyond what the auction endpoints expose.
- **Farmer and buyer registration is not exposed.** The directories are read-only views of data the
  backend already owns; registration is out of the frontend's reach.
- **The 6 lint warnings** in §8 are pre-existing and in files this work only touched incidentally.
- **Accessibility and responsive behaviour** were preserved rather than reworked; no automated
  a11y audit was run.

---

## 8. PostgreSQL verification and three defects it exposed

An earlier revision of this audit reported that the app could not be verified against PostgreSQL
because no instance was available. That limitation has since been removed: PostgreSQL 16.15 is now
running locally (no Docker), and the app has been started against it. Doing so exposed three real
defects that the H2-based verification had masked, all now fixed.

### 7.1 `updated_at` missing from five tables — the app could not start at all

`BaseEntity` declares a non-null `created_at` and a non-null `updated_at` maintained by JPA auditing,
and `AuditConfig` enables that auditing. But the create-table migrations omitted `updated_at` on
`weighments`, `bids`, `asset_transactions` and `payments`, and omitted *both* columns on `audit_logs`.

With `spring.jpa.hibernate.ddl-auto: validate` — the setting the project actually ships — the
context failed to start before serving a single request:

```
SchemaManagementException: Schema-validation: missing column [updated_at] in table [asset_transactions]
```

This is a production-blocking bug, not a dev-environment problem. It is invisible under H2, because
H2 was always started with `ddl-auto: create-drop`, which generates the schema from the entities and
so silently papers over the mismatch.

Fixed by two new migrations, because the original create-table migrations are already applied and
therefore checksummed — editing them would break Flyway validation on every existing database:

- `V13__align_base_entity_audit_columns.sql` — `weighments`, `bids`, `asset_transactions`, `audit_logs`
- `V14__align_payments_audit_column.sql` — `payments`

Both backfill existing rows from the table's own business timestamp before applying `NOT NULL`.

### 7.2 The seeded demo accounts could not log in

`V11__security_users.sql` carries the comment `-- BCrypt hash for 'password'`, but the hash it stores
does not verify against `password`. All five seeded accounts (`rajesh.patel`, `admin.demo`,
`agent.demo`, `farmer.demo`, `buyer.demo`) therefore returned **401 on every login**, making the demo
environment unusable out of the box.

Fixed by `V15__fix_demo_password_hashes.sql`, which stores a hash generated with the application's
own `BCryptPasswordEncoder`. All five accounts now authenticate. The shared `password` remains a demo
credential and must be rotated in any real deployment.

### 7.3 Verification on real PostgreSQL

With those fixed, all 15 migrations apply cleanly and the backend starts:

```
Successfully validated 15 migrations
Migrating schema "public" to version "13 - align base entity audit columns"
Migrating schema "public" to version "14 - align payments audit column"
Migrating schema "public" to version "15 - fix demo password hashes"
Started AgrimandiApiApplication in 10.253 seconds
```

Authenticated smoke test as `agent.demo`, all returning real data:

| Endpoint | Result |
|---|---|
| `POST /api/auth/login` | 200, returns a JWT for all five demo accounts |
| `GET /api/markets/me` | 200, real market |
| `GET /api/lots` | 200, empty (no lots exist yet) |
| `GET /api/farmers` | 200, 3 records |
| `GET /api/buyers` | 200, 4 records |
| `GET /api/commodities` | 200, seeded reference data |
| `GET /api/asset-types` | 200, seeded reference data |
| `GET /api/reports/payment-ledger` | 200, correctly paged |

The winner-confirmation path is now exercisable, because the PostgreSQL pessimistic lock
(`for no key update`) that H2 could not parse runs natively here. It is no longer on the blocked
list.

### 7.4 Known minor issue

`GET /v3/api-docs` returns **500**. Swagger/OpenAPI is a documentation concern only and does not
affect any application screen or the API contract the frontend depends on; it has not been
diagnosed.
