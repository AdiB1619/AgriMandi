# AgriMandi AI — Full-Stack Engineering Specification v1.0

## 1. Product Definition

AgriMandi AI is an APMC operational transaction and settlement platform for physical mandi auctions. The physical auction remains on the mandi floor. The commission agent/operator records farmer lots, weighment, buyer bids, winner confirmation, settlement, payments, and returnable-asset movements. Farmers and buyers primarily receive read-only visibility. AI is advisory and never determines the auction winner.

## 2. Source-of-Truth Rule

The supplied Stitch prototype is the visual source of truth for the frontend. Preserve its visual language, layout hierarchy, terminology, density, responsive behavior, and interaction intent while converting static HTML/mock data into production React components.

Do not copy Stitch pages into production as independent HTML documents. Refactor them into a shared React design system and feature modules.

## 3. Technology

Frontend: React 19, Vite, Tailwind CSS v4, React Router, Lucide React, Recharts.
Backend: Spring Boot, Spring Security, JWT, Spring Data JPA/Hibernate, Bean Validation.
Database: PostgreSQL.
Database migrations: Flyway.
AI: Python FastAPI + scikit-learn.
Infrastructure: Docker Compose for development; production containers for frontend, API, PostgreSQL, and ML service.

## 4. Runtime Architecture

Browser → React → Spring Boot REST API → PostgreSQL.
Spring Boot → FastAPI ML service for advisory AI only.

React must never connect directly to PostgreSQL or the ML service.
Core transaction workflows must continue if the ML service is unavailable.

## 5. Frontend Information Architecture

Agent:
- /agent/dashboard
- /agent/auction
- /agent/auction/:lotId
- /agent/lots
- /agent/lots/new
- /agent/lots/:id
- /agent/settlements
- /agent/settlements/:id
- /agent/payments
- /agent/assets
- /agent/farmers
- /agent/farmers/:id
- /agent/buyers
- /agent/buyers/:id
- /agent/reports
- /agent/ai-insights
- /agent/corrections

Farmer:
- /farmer/dashboard
- /farmer/passbook
- /farmer/lots
- /farmer/payments

Buyer:
- /buyer/dashboard
- /buyer/purchases
- /buyer/payments
- /buyer/assets

Admin:
- /admin/dashboard
- /admin/users
- /admin/agents
- /admin/farmers
- /admin/buyers
- /admin/reports
- /admin/audit

Gate / weighbridge:
- /gate/entry
- /gate/weighment

Documents:
- /documents/form-j/:transactionId
- /documents/takpatti/:transactionId
- /documents/weigh-slip/:lotId

## 6. Frontend Structure

src/
  app/
    App.jsx
    router.jsx
    providers/
    config/
  components/
    ui/
    layout/
    data-table/
    forms/
    charts/
    status/
  features/
    auth/
    dashboard/
    farmers/
    buyers/
    commodities/
    lots/
    auction/
    settlements/
    payments/
    assets/
    reports/
    corrections/
    ai/
    documents/
    admin/
    gate/
  services/
  hooks/
  utils/
  types/
  styles/

## 7. Stitch Design System

Use the supplied Stitch design tokens as the starting point.
Primary institutional green: #003622 / primary-container #134e35.
Secondary green: #246a4b.
Canvas: #f8f9ff.
Surface: #ffffff.
Structural border: #c0c9c1 / #e2e8f0 where the Stitch page implementation uses the lighter structural grid.
Main text: #0b1c30.
Muted text: #404943 / #64748b.
Error: #ba1a1a / critical semantic red variants.

Typography:
- Plus Jakarta Sans for headings.
- Inter for body, tables, financial values, and operational numerals.
- Use tabular numerals for currency, weights, rates, counts, and balances.

Layout:
- Desktop: dense 12-column operational grid.
- Tablet: 8-column layout with secondary table metadata collapsible.
- Mobile: 4-column execution-card layouts for primary actions.

Visual principles:
- Enterprise operational ledger, not consumer marketplace.
- Minimal decorative effects.
- Crisp borders and low-contrast elevation.
- High information density without sacrificing scanability.
- Keyboard-friendly and touch-friendly primary actions.
- Primary action is green; warning is amber; overdue/error is red; informational/telemetry is blue.

## 8. Shared Components

AppShell, Sidebar, Topbar, PageHeader, Breadcrumbs, Button, IconButton, Input, Select, DatePicker, Modal, Drawer, Tabs, Badge, StatusBadge, Toast, Tooltip, Card, StatCard, DataTable, FilterBar, Pagination, EmptyState, LoadingState, ErrorState, Timeline, AuditTimeline, Currency, Weight, Rate, LotCard, BidRow, BidBoard, PaymentLedger, AssetLedger, TransactionTimeline, DocumentPreview.

## 9. Core Domain Model

User
Role
Market
Farmer
Buyer
CommissionAgent
Commodity
Lot
Weighment
Auction
Bid
Transaction
Payment
AssetType
AssetTransaction
AuditLog
CorrectionRequest
TransactionRevision

Recommended central object: Lot.

Lot contains references to farmer, commodity, quantity/unit, packaging configuration, weighment, auction, and resulting transaction.

## 10. Suggested PostgreSQL Tables

users
roles
user_roles
markets
farmers
buyers
commission_agents
commodities
lots
weighments
auctions
bids
transactions
transaction_charges
payments
asset_types
asset_transactions
audit_logs
correction_requests
transaction_revisions
refresh_tokens

## 11. Key Fields

users: id, username, password_hash, role/status, market_id, created_at, updated_at.

farmers: id, farmer_code, name, village, phone, address, identity_reference_if_required, status, market_id.

buyers: id, buyer_code, name, business_name, phone, license_reference, status, market_id.

commodities: id, name, grade_options, default_unit, active.

lots: id, lot_number, farmer_id, commodity_id, quantity, quantity_unit, grade, packaging_type, packaging_returnable, status, market_id, created_by, created_at.

weighments: id, lot_id, gross_weight, tare_weight, net_weight, weighing_device_reference, weighed_at, operator_id.

auctions: id, lot_id, status, started_at, closed_at, winning_bid_id, winner_confirmation_by, winner_confirmed_at.

bids: id, auction_id, buyer_id, rate, price_unit, recorded_at, recorded_by, valid, notes.

transactions: id, transaction_number, lot_id, buyer_id, gross_amount, commission_amount, cess_amount, other_charges, net_payable, status, completed_at, created_by.

payments: id, transaction_id, amount, payment_method, reference_number, paid_at, recorded_by, notes.

asset_types: id, name, returnable, active.

asset_transactions: id, transaction_id, asset_type_id, issued_quantity, returned_quantity, outstanding_quantity, issue_date, return_date, status, notes.

## 12. Business State Machines

Lot:
DRAFT → INWARD → READY_FOR_AUCTION → AUCTION_IN_PROGRESS → BIDS_RECORDED → WINNER_PENDING_CONFIRMATION → WINNER_CONFIRMED → TRANSACTION_CREATED → SETTLED.

Auction:
DRAFT → OPEN → BIDDING → CLOSED → WINNER_CONFIRMED.
Tie state: TIE_REQUIRES_AGENT_DECISION.

Payment state:
UNPAID → PARTIALLY_PAID → PAID.

Asset state:
NOT_APPLICABLE → ISSUED → PARTIALLY_RETURNED → RETURNED.
If outstanding beyond configured threshold: OVERDUE.

## 13. Auction Rules

The physical auction occurs outside the software. The software records bids.

The backend is authoritative for highest-bid determination.

If one valid bid is the unique maximum, it becomes the winning candidate.

If multiple buyers share the maximum rate, do not auto-select. Set TIE_REQUIRES_AGENT_DECISION and require explicit agent selection.

AI must never select or override the winner.

Completed transactions cannot be edited directly.

## 14. Settlement Rules

Gross sale value = quantity × winning rate, using the lot's price unit.

Where configured, commission, cess, weighment fee, and other charges are calculated server-side.

The frontend displays calculated values; it does not become the authoritative calculator.

Settlement creates the transaction record and links it to the original lot and winning auction.

## 15. Payment Rules

Multiple partial payments are supported.

Paid amount = sum(valid payment entries).

Outstanding = transaction total payable − paid amount.

Payment amount cannot exceed the remaining payable amount unless an explicit overpayment rule is configured.

Every payment requires timestamp, method, and recorded-by information. Reference number should be required for digital/traceable methods.

## 16. Returnable Asset Rules

Do not assume every commodity uses crates.

Support packaging choices such as None/Loose, Crates, Sacks/Bags, Baskets, Bundles, and Other, while marking each asset type as returnable or non-returnable.

For returnable assets:
Outstanding = issued − returned.

The UI must show Issued, Returned, Outstanding, and Overdue where applicable.

## 17. Correction/Reversal

Completed records are immutable.

Correction flow:
Completed → Correction Requested → Review/Approval → Reversal/Revision → Corrected Record.

Preserve the original transaction and create a revision/correction record. Never silently overwrite historical financial data.

## 18. API Contract

Authentication:
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
GET /api/auth/me

Farmers:
GET /api/farmers
GET /api/farmers/{id}
POST /api/farmers
PUT /api/farmers/{id}

Buyers:
GET /api/buyers
GET /api/buyers/{id}
POST /api/buyers
PUT /api/buyers/{id}

Lots:
GET /api/lots
GET /api/lots/{id}
POST /api/lots
PUT /api/lots/{id}

Auction:
GET /api/lots/{lotId}/bids
POST /api/lots/{lotId}/bids
POST /api/lots/{lotId}/close-auction
POST /api/lots/{lotId}/confirm-winner

Transactions:
GET /api/transactions
GET /api/transactions/{id}
POST /api/transactions/{id}/settle

Payments:
GET /api/transactions/{id}/payments
POST /api/transactions/{id}/payments

Assets:
GET /api/assets
GET /api/transactions/{id}/assets
POST /api/transactions/{id}/assets/issue
POST /api/transactions/{id}/assets/return

Reports:
GET /api/reports/daily-settlement
GET /api/reports/payments
GET /api/reports/assets
GET /api/reports/audit

Corrections:
POST /api/corrections
GET /api/corrections
POST /api/corrections/{id}/approve
POST /api/corrections/{id}/reject

## 19. API Conventions

Use JSON DTOs, not JPA entities, as public API contracts.
Use UUID or opaque identifiers externally.
Return consistent error envelopes.
Validate all input server-side.
Use pagination for list endpoints.
Support filtering, sorting, and date ranges where the UI requires them.
Never trust client-calculated totals, winner IDs, permissions, or ownership.

## 20. RBAC

ADMIN: system/market oversight, user management, reports, audit, corrections/approvals according to policy.

AGENT: farmer/buyer management, lot creation, auction recording, winner confirmation, settlement, payments, assets, reports available to agent.

FARMER: read-only access to own lots, sales, payment status, passbook.

BUYER: read-only access to own purchases, payments, assets, transaction history.

GATE_OPERATOR: gate entry and weighment operations only.

## 21. Farmer/Buyer Visibility

Farmers and buyers do not conduct the physical auction through the portal.

Farmer portal displays only that farmer's own records.
Buyer portal displays only that buyer's own purchases, payment status, and applicable asset ledger.

Enforce ownership at the backend, not only through route guards.

## 22. AI Service

FastAPI endpoints:
POST /ml/price-prediction
POST /ml/asset-anomaly-score
GET /ml/model-info
GET /ml/health

Price intelligence is advisory. It can return predicted value and reference range, model version, data period, and validation metadata.

Asset anomaly detection identifies unusual retention patterns. It does not accuse or automatically penalize a buyer.

If ML service is unavailable, core transaction operations remain usable.

## 23. Document Generation

Generate Form J, Takpatti, and Weigh Slip from authoritative transaction/lot/weighment data.

Do not create a second manual document dataset.

Support print-friendly and PDF-friendly rendering.

## 24. Dashboard Metrics

Dashboard metrics must be calculated from actual records:
- lots today
- auctions active/completed
- transaction value
- amount collected
- outstanding payments
- assets issued
- assets outstanding
- overdue assets
- pending approvals/corrections

Avoid storing dashboard totals unless there is a demonstrated performance requirement.

## 25. Mock Data Migration

During frontend migration, place all temporary data in a dedicated mock layer.

Do not scatter literal demo values across components.

Every page should consume a service interface. The mock service can later be replaced by the REST service without rewriting presentation components.

## 26. Development Order

1. Create React/Vite project and migrate Stitch design tokens.
2. Build shared AppShell and reusable components.
3. Recreate Stitch screens using React with mock services.
4. Create Spring Boot project and PostgreSQL schema.
5. Implement Flyway migrations and seed data.
6. Implement authentication/JWT/RBAC.
7. Implement Farmer + Buyer + Commodity + Lot.
8. Implement Auction + Bids + Winner confirmation.
9. Implement Transaction + Settlement.
10. Implement Payments.
11. Implement Returnable Assets.
12. Implement Farmer/Buyer read-only portals.
13. Implement Reports/Audit.
14. Implement Form J/Takpatti/Weigh Slip.
15. Implement Corrections/Reversals.
16. Implement Gate/Weighbridge workflow.
17. Implement FastAPI AI services.
18. Integrate AI Insights UI.
19. Add automated tests.
20. Add Docker/deployment/security hardening.

## 27. Testing Requirements

Unit tests: business rules, calculations, state transitions, tie handling, payment balances, asset balances.

Integration tests: API + PostgreSQL workflows.

Security tests: RBAC, ownership isolation, invalid JWT, expired token, unauthorized endpoints.

End-to-end tests: login → farmer → lot → bids → winner → settlement → payment → asset return.

Regression test the visual implementation against the Stitch prototype for the major screens.

## 28. Demo Seed Dataset

Seed realistic but clearly synthetic APMC data:
- multiple farmers
- multiple buyers
- tomato, onion, coriander/leafy produce, banana or similar commodities
- multiple lots
- 3–5 bids per auction
- at least one tied highest bid for tie-handling demo
- partial payment cases
- fully paid cases
- outstanding asset cases
- overdue asset case
- correction/reversal case
- AI prediction examples
- anomaly examples

Do not present synthetic data as real market statistics.

## 29. Production Quality Rules

No unrelated refactors.
No fake backend success responses once the real API is connected.
No direct database access from React.
No client-authoritative financial calculations.
No client-authoritative winner selection.
No silent mutation of completed transactions.
No generic AI chatbot added merely for appearance.
No fake external integrations presented as live.
No redesign of the Stitch visual system unless required for accessibility, usability, or technical correctness.

## 30. Acceptance Criteria

The project is considered functionally complete when an agent can:

1. Log in.
2. Find or create a farmer.
3. Create a lot and record weighment.
4. Record multiple buyer bids from the physical auction.
5. See the highest valid bid.
6. Resolve ties manually.
7. Confirm the winner.
8. Generate the transaction and calculate the payable amount.
9. Record one or more partial payments.
10. See outstanding payment accurately.
11. Issue and return returnable assets when applicable.
12. See outstanding/overdue assets.
13. Generate/print transaction documents.
14. Correct a completed transaction through the controlled correction workflow.
15. Produce reports from the same transactional database.
16. Allow a farmer to view only their own passbook.
17. Allow a buyer to view only their own purchase history.
18. View advisory AI insights without making AI authoritative over transactions.
19. Continue core operations if the AI service is unavailable.
20. Preserve the Stitch-derived visual system across desktop, tablet, and mobile experiences.
