<div align="center">

# ⚖️ LegalAid
### Legal Services Marketplace Platform

**A full-stack platform connecting clients with verified lawyers in Bangladesh**

[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.x-brightgreen?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![React](https://img.shields.io/badge/React-18-61DAFB?style=flat-square&logo=react)](https://react.dev/)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

[Features](#-features) • [Architecture](#-architecture) • [API Docs](#-api-endpoints) • [Setup](#-getting-started) • [Frontend Integration](#-frontend-integration)

</div>

---

## 📌 What is LegalAid?

LegalAid is a **two-sided legal marketplace** built specifically for Bangladesh. Think of it as a combination of Fiverr (hire lawyers), Clio (case management), and a secure document vault — all in one platform.

**Clients** can browse verified lawyers, hire them through a structured contract flow, manage their legal cases, and pay securely via bKash escrow.

**Lawyers** can list their services, accept hire requests, manage active contracts and cases, communicate with clients, and withdraw their earnings.

**Admins** verify lawyer credentials, handle payment disputes, and monitor platform activity.

---

## ✨ Features

### For Clients
- 🔍 Browse and search verified lawyers by practice area, location, and rating
- 📋 Hire lawyers through a structured contract flow
- 💳 Pay via bKash with escrow protection
- 📁 Manage legal cases with milestones and document uploads
- 💬 Message lawyers directly within contracts
- ⭐ Leave reviews after contract completion
- ⚖️ Raise disputes if work is unsatisfactory

### For Lawyers
- 🧑‍💼 Create and manage service listings
- ✅ Get verified through document submission (bar cert + NID)
- 📊 Track earnings, contracts, and reviews
- 📂 Access case files shared by clients
- 💰 Request payouts to bKash or bank account

### For Admins
- 🔐 Review and approve/reject lawyer verifications
- 💵 Verify bKash transaction IDs and activate escrow
- ⚖️ Resolve disputes with full evidence and message history
- 📈 Monitor platform-wide statistics

---

## 🏗️ Architecture

### System Overview
```
┌─────────────────────────────────────────────────┐
│                   Frontend                       │
│         React 18 + Vite + Tailwind CSS          │
│              (Port 5173 in dev)                  │
└─────────────────┬───────────────────────────────┘
                  │ HTTP / REST
                  ▼
┌─────────────────────────────────────────────────┐
│               Spring Boot Backend                │
│          Modular Monolith (Port 8080)            │
│                                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐  │
│  │  auth/   │  │  user/   │  │   lawyer/    │  │
│  │  JWT +   │  │ profiles │  │ verification │  │
│  │  OAuth   │  └──────────┘  └──────────────┘  │
│  └──────────┘                                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐  │
│  │ service/ │  │ contract/│  │   payment/   │  │
│  │listings  │  │ escrow   │  │ bKash TxnID  │  │
│  └──────────┘  └──────────┘  └──────────────┘  │
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐  │
│  │  case_/  │  │ dispute/ │  │   review/    │  │
│  │management│  │resolution│  │   ratings    │  │
│  └──────────┘  └──────────┘  └──────────────┘  │
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐  │
│  │document/ │  │ message/ │  │notification/ │  │
│  │Cloudinary│  │ threads  │  │  in-app      │  │
│  └──────────┘  └──────────┘  └──────────────┘  │
│  ┌──────────┐  ┌──────────┐                     │
│  │  search/ │  │  admin/  │                     │
│  │ unified  │  │dashboard │                     │
│  └──────────┘  └──────────┘                     │
└─────────────────┬───────────────────────────────┘
                  │
       ┌──────────┴──────────┐
       ▼                     ▼
┌─────────────┐      ┌──────────────┐
│ PostgreSQL  │      │  Cloudinary  │
│  (Primary   │      │ (Documents,  │
│  Database)  │      │  Images,     │
│             │      │  Videos)     │
└─────────────┘      └──────────────┘
```

### Design Patterns Used
| Pattern | Where Applied |
|---|---|
| Modular Monolith | Whole application |
| Layered Architecture | All 16 packages (Controller → Service → Repository) |
| Repository Pattern | All data access via Spring Data JPA |
| DTO Pattern | Separate request/response shapes from DB entities |
| State Machine | Contract lifecycle, Dispute flow |
| Soft Delete | All major tables via `deleted_at` |
| Snapshot Pattern | Service data copied to contract at hire time |
| Observer + @Async | Notification system decoupled from business logic |
| Guard Clause | Fail-fast validation at top of every service method |

---

## 🛠️ Tech Stack

### Backend
| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Primary language |
| Spring Boot | 3.3.x | Application framework |
| Spring Security | 6.x | Auth + endpoint guards |
| Spring Data JPA | 3.x | ORM and repositories |
| PostgreSQL | 15+ | Primary database |
| JWT (JJWT) | 0.12.6 | Stateless auth tokens |
| Google OAuth 2.0 | — | Social login |
| Cloudinary | 1.39.0 | File storage |
| HikariCP | Built-in | Connection pooling |
| JUnit 5 + Mockito | — | Testing |

### Frontend
| Technology | Version | Purpose |
|---|---|---|
| React | 18 | UI framework |
| Vite | Latest | Build tool |
| Tailwind CSS | 3.x | Styling |
| React Router DOM | 6 | Client-side routing |
| Lucide React | — | Icons |

---

## 📁 Project Structure

```
backend/
└── src/main/java/com/legalaid/
    ├── LegalAidApplication.java
    ├── config/
    │   ├── SecurityConfig.java       # JWT filter chain, public endpoints
    │   ├── JwtConfig.java            # Token expiry + secret
    │   ├── CorsConfig.java           # Allowed origins
    │   └── CloudinaryConfig.java     # Cloudinary bean
    ├── common/
    │   ├── response/ApiResponse.java # Standard { success, data, message }
    │   └── exception/GlobalExceptionHandler.java
    ├── auth/                         # Google OAuth + JWT
    ├── user/                         # User profiles + role management
    ├── lawyer/                       # Profiles, verification, search
    ├── service/                      # Service listings + media
    ├── contract/                     # Hire flow + state machine
    ├── case_/                        # Case management + access control
    ├── payment/                      # bKash escrow + payouts
    ├── message/                      # Contract messaging
    ├── notification/                 # In-app notifications
    ├── document/                     # Cloudinary file uploads
    ├── review/                       # Ratings + helpful votes
    ├── dispute/                      # Dispute resolution
    ├── search/                       # Unified search
    └── admin/                        # Admin dashboard
```

---

## 🚀 Getting Started

### Prerequisites
- Java 21+
- Maven 3.8+
- PostgreSQL 15+
- A Google Cloud project with OAuth 2.0 credentials
- A Cloudinary account (free tier works)

### 1. Clone the repository
```bash
git clone https://github.com/chatok-jnr/legalaid-marketplace.git
cd legalaid-marketplace
```

### 2. Set up the database
```bash
# Create the database
psql -U postgres -c "CREATE DATABASE legalaid;"

# Run the schema
psql -U postgres -d legalaid -f legalaid_schema.sql
psql -U postgres -d legalaid -f legalaid_migration_v2.sql
```

### 3. Configure environment variables

Create a `.env` file in the `backend/` directory:
```env
# Database
DB_URL=jdbc:postgresql://localhost:5432/legalaid
DB_USERNAME=postgres
DB_PASSWORD=yourpassword

# JWT
JWT_SECRET=your-256-bit-secret-at-least-32-characters-long

# Google OAuth
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com

# Cloudinary
CLOUDINARY_CLOUD_NAME=your-cloud-name
CLOUDINARY_API_KEY=your-api-key
CLOUDINARY_API_SECRET=your-api-secret
```

Or update `src/main/resources/application.yml` directly:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/legalaid
    username: postgres
    password: yourpassword
  servlet:
    multipart:
      enabled: true
      max-file-size: 50MB
      max-request-size: 55MB

app:
  cors:
    allowed-origins: http://localhost:5173
  jwt:
    secret: your-256-bit-secret
    access-token-expiry: 900000       # 15 minutes
    refresh-token-expiry: 604800000   # 7 days
  google:
    client-id: your-google-client-id
  cloudinary:
    cloud-name: your-cloud-name
    api-key: your-api-key
    api-secret: your-api-secret
```

### 4. Run the backend
```bash
cd backend
./mvnw spring-boot:run
```

Backend will start at `http://localhost:8080`

### 5. Run the frontend
```bash
cd frontend
npm install
npm run dev
```

Frontend will start at `http://localhost:5173`

---

## 🔗 Frontend Integration

### How Auth Works

The frontend uses **Google Sign-In**. After the user authenticates with Google, the frontend receives a Google ID token and sends it to the backend:

```javascript
// After Google Sign-In success
const response = await fetch('http://localhost:8080/api/auth/google', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ idToken: googleIdToken }),
  credentials: 'include'  // important — needed for refresh token cookie
});

const { data } = await response.json();
// data.accessToken  → store in memory (NOT localStorage)
// data.role         → CLIENT, LAWYER, or ADMIN
// data.isNewUser    → true if first login (redirect to onboarding)
// refresh token is automatically stored in HttpOnly cookie
```

### Sending Authenticated Requests

Include the access token in every protected request:

```javascript
const response = await fetch('http://localhost:8080/api/contracts', {
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  }
});
```

### Refreshing the Access Token

Access tokens expire in 15 minutes. Refresh automatically:

```javascript
// POST /api/auth/refresh
// No body needed — refresh token is sent automatically via cookie
const response = await fetch('http://localhost:8080/api/auth/refresh', {
  method: 'POST',
  credentials: 'include'
});
const { data } = await response.json();
// data.accessToken → new access token
```

### Standard API Response Format

Every endpoint returns the same shape:

```json
{
  "success": true,
  "data": { },
  "message": null,
  "timestamp": "2026-04-01T10:00:00Z"
}
```

On error:
```json
{
  "success": false,
  "data": null,
  "message": "Contract cannot be cancelled — current status: COMPLETED",
  "timestamp": "2026-04-01T10:00:00Z"
}
```

### File Uploads

Use `multipart/form-data` for document uploads:

```javascript
const formData = new FormData();
formData.append('file', selectedFile);
formData.append('folderName', 'case-documents');  // optional
formData.append('caseId', caseId);                 // optional

const response = await fetch('http://localhost:8080/api/documents/upload', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${accessToken}` },
  body: formData
  // DO NOT set Content-Type — browser sets it automatically with boundary
});
```

---

## 📡 API Endpoints

### Auth
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/auth/google` | Public | Login with Google ID token |
| POST | `/api/auth/refresh` | Cookie | Refresh access token |
| POST | `/api/auth/logout` | Auth | Clear refresh token cookie |
| GET | `/api/auth/me` | Auth | Get current user ID |

### Users
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/users/me` | Auth | Own full profile |
| PUT | `/api/users/me` | Auth | Update profile |
| GET | `/api/users/:id` | Public | Public profile |
| PUT | `/api/users/me/request-lawyer` | CLIENT | Request lawyer role |
| DELETE | `/api/users/me` | Auth | Soft delete account |

### Lawyers
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/lawyers` | Public | Browse verified lawyers |
| GET | `/api/lawyers/:id` | Public | Lawyer public profile |
| GET | `/api/lawyers/me` | LAWYER | Own profile |
| GET | `/api/lawyers/me/stats` | LAWYER | Earnings + stats |
| PUT | `/api/lawyers/me/onboarding` | CLIENT/LAWYER | Complete onboarding |
| POST | `/api/lawyers/me/verify` | CLIENT/LAWYER | Submit verification docs |
| GET | `/api/lawyers/:id/reviews` | Public | Lawyer reviews |

### Services
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/services` | Public | Browse services |
| GET | `/api/services/:id` | Public | Service detail |
| GET | `/api/services/mine` | LAWYER | Own services |
| GET | `/api/services/by-lawyer/:id` | Public | Services by lawyer |
| POST | `/api/services` | LAWYER | Create service |
| PUT | `/api/services/:id` | LAWYER | Update service |
| DELETE | `/api/services/:id` | LAWYER | Delete service |

### Contracts
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/contracts` | Auth | Own contracts |
| GET | `/api/contracts/:id` | Participant | Contract detail |
| POST | `/api/contracts` | CLIENT | Hire a lawyer |
| PUT | `/api/contracts/:id/accept` | LAWYER | Accept request |
| PUT | `/api/contracts/:id/decline` | LAWYER | Decline request |
| PUT | `/api/contracts/:id/complete` | CLIENT | Mark complete |
| PUT | `/api/contracts/:id/cancel` | Participant | Cancel contract |
| PUT | `/api/contracts/:id/milestones/:mId` | Participant | Toggle milestone |

### Cases
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/cases` | Auth | Own + accessible cases |
| GET | `/api/cases/:id` | Access | Case detail |
| POST | `/api/cases` | Auth | Create case |
| PUT | `/api/cases/:id` | Owner/Editor | Update case |
| DELETE | `/api/cases/:id` | Owner | Delete case |
| POST | `/api/cases/:id/access` | Owner | Invite user |
| DELETE | `/api/cases/:id/access/:userId` | Owner | Remove access |
| PUT | `/api/cases/:id/milestones/:mId` | Owner/Editor | Toggle milestone |

### Payments
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/payments` | CLIENT | Submit bKash TxnID |
| GET | `/api/payments` | CLIENT | Payment history |
| GET | `/api/payments/:id/invoice` | CLIENT | Download invoice |
| GET | `/api/payments/balance` | LAWYER | Available balance |
| POST | `/api/payouts/request` | LAWYER | Request withdrawal |
| GET | `/api/payouts` | LAWYER | Payout history |

### Messages
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/contracts/:id/messages` | Participant | Message thread |
| POST | `/api/contracts/:id/messages` | Participant | Send message |
| GET | `/api/messages/unread-count` | Auth | Unread badge count |

### Notifications
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/notifications` | Auth | Paginated notifications |
| GET | `/api/notifications/unread-count` | Auth | Unread count |
| PUT | `/api/notifications/:id/read` | Auth | Mark as read |
| PUT | `/api/notifications/read-all` | Auth | Mark all read |
| DELETE | `/api/notifications/:id` | Auth | Delete notification |

### Documents
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/documents/upload` | Auth | Upload file |
| GET | `/api/documents` | Auth | Own documents |
| GET | `/api/cases/:id/documents` | Auth | Case documents |
| GET | `/api/contracts/:id/documents` | Auth | Contract documents |
| PUT | `/api/documents/:id` | Owner | Update metadata |
| PUT | `/api/documents/:id/star` | Owner | Toggle starred |
| DELETE | `/api/documents/:id` | Owner | Delete document |

### Reviews
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/reviews` | CLIENT | Submit review |
| PUT | `/api/reviews/:id/reply` | LAWYER | Reply to review |
| POST | `/api/reviews/:id/helpful` | Auth | Toggle helpful vote |

### Disputes
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/disputes` | Auth | Own disputes |
| GET | `/api/disputes/:id` | Participant | Dispute detail |
| POST | `/api/disputes` | Participant | Raise dispute |
| POST | `/api/disputes/:id/evidence` | Participant | Upload evidence |
| POST | `/api/disputes/:id/messages` | Participant | Send message |
| PUT | `/api/disputes/:id/respond` | Other party | Respond to dispute |

### Search
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/search` | Public | Search lawyers + services |

### Admin
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/admin/lawyers/pending` | ADMIN | Verification queue |
| PUT | `/api/admin/lawyers/:id/verify` | ADMIN | Approve/reject lawyer |
| GET | `/api/admin/payments/pending` | ADMIN | Pending TxnID verifications |
| PUT | `/api/admin/payments/:id/verify` | ADMIN | Verify payment |
| PUT | `/api/admin/payments/:id/reject` | ADMIN | Reject payment |
| GET | `/api/admin/payouts/pending` | ADMIN | Pending payouts |
| PUT | `/api/admin/payouts/:id/process` | ADMIN | Process payout |
| PUT | `/api/admin/payouts/:id/reject` | ADMIN | Reject payout |
| GET | `/api/admin/disputes` | ADMIN | All disputes |
| PUT | `/api/admin/disputes/:id/resolve` | ADMIN | Resolve dispute |
| GET | `/api/admin/stats` | ADMIN | Platform statistics |

---

## 🔐 Contract State Machine

```
Client hires lawyer
        │
        ▼
  PENDING_LAWYER ──── lawyer declines ──► CANCELLED
        │                    ▲
        │ lawyer accepts      │ lawyer cancels (only here)
        ▼                     │
  PENDING_PAYMENT ────────────┘
        │                    ▲
        │ payment verified    │ client cancels
        ▼                     │
      ACTIVE ─────────────────┘
        │
        ├── client confirms ──► COMPLETED ──► payment RELEASED to lawyer
        │
        ├── client disputes ──► DISPUTED ──► payment FROZEN
        │                           │
        │                     admin resolves
        │                    ╱              ╲
        │              REFUND           RELEASE
        │             (CANCELLED)      (COMPLETED)
        │
        └── client cancels ──► CANCELLED ──► payment REFUNDED
```

---

## 💳 bKash Payment Flow

```
1. Client sends money to LegalAid bKash merchant number manually
2. Client submits TxnID via POST /api/payments
3. Payment saved with status = PENDING_VERIFICATION
4. Admin verifies TxnID against bKash records manually
5. Admin calls PUT /api/admin/payments/:id/verify
6. Payment → HELD, Contract → ACTIVE
7. Work completed → client calls PUT /api/contracts/:id/complete
8. Payment → RELEASED, lawyer balance updated
9. Lawyer calls POST /api/payouts/request
10. Admin processes payout manually and calls PUT /api/admin/payouts/:id/process
```

---

## 🧪 Running Tests

```bash
# All tests
./mvnw test

# Specific test class
./mvnw test -Dtest=JwtServiceTest
./mvnw test -Dtest=AuthServiceTest
./mvnw test -Dtest=LawyerServiceTest

# All tests with coverage report
./mvnw test jacoco:report
# Report at: target/site/jacoco/index.html
```

---

## 🌍 Environment Variables Reference

| Variable | Required | Description |
|---|---|---|
| `DB_URL` | ✅ | PostgreSQL connection URL |
| `DB_USERNAME` | ✅ | Database username |
| `DB_PASSWORD` | ✅ | Database password |
| `JWT_SECRET` | ✅ | Min 32 chars — used to sign tokens |
| `GOOGLE_CLIENT_ID` | ✅ | From Google Cloud Console |
| `CLOUDINARY_CLOUD_NAME` | ✅ | From Cloudinary dashboard |
| `CLOUDINARY_API_KEY` | ✅ | From Cloudinary dashboard |
| `CLOUDINARY_API_SECRET` | ✅ | From Cloudinary dashboard |

---

## 🗄️ Database Schema Overview

35 tables across 10 groups:

```
Users          → users, client_profiles, lawyer_profiles, admin_profiles
               → lawyer_locations, lawyer_practice_areas, lawyer_courts
               → lawyer_languages, lawyer_availability, lawyer_verification_docs

Services       → services, service_media, service_highlights
               → service_features, service_faqs

Contracts      → contracts, contract_milestones

Cases          → cases, case_access, case_milestones, case_tags

Documents      → documents

Messages       → messages

Notifications  → notifications

Payments       → payments, payouts

Reviews        → reviews, review_aspects, review_tags, review_helpful_votes

Disputes       → disputes, dispute_evidence, dispute_messages, dispute_timeline
```

---

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 👨‍💻 Author

**Md. Sakib Hosen**
- GitHub: [@chatok-jnr](https://github.com/chatok-jnr)
- LinkedIn: [chatok-junior](https://linkedin.com/in/chatok-junior)
- Portfolio: [iamchatokjunior.netlify.app](https://iamchatokjunior.netlify.app)

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

<div align="center">
Built with ❤️ for Bangladesh's legal community
</div>
