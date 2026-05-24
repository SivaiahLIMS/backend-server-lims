# LIMS - Laboratory Information Management System

Enterprise-grade, AI-powered, multi-tenant LIMS platform by Siva Ya Health.

## Stack

- **Java 17** + **Spring Boot 3.4.5**
- **Spring Security 6** + **JWT** (JJWT 0.12.6)
- **PostgreSQL** (via Spring Data JPA + Hibernate)
- **Flyway** (database migrations)
- **Lombok** + **SpringDoc OpenAPI** (Swagger UI)

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 14+ running on `localhost:5432`

## Database Setup

```sql
CREATE DATABASE lims;
```

Username: `postgres` / Password: `postgres` (configurable in `application.yml`)

## Build & Run

```bash
cd lims-backend
mvn clean package -DskipTests
java -jar target/lims-1.0.0.jar
```

Or for development:

```bash
mvn spring-boot:run
```

## API Documentation

Swagger UI: http://localhost:8080/api/v1/swagger-ui.html

## Default Admin Login

- **Username**: `admin`
- **Password**: `Admin@123`
- **Tenant**: `SIVAYA`

## Security Features

- JWT authentication (access + refresh tokens)
- **3 invalid password attempts → account locked**
- **1 month inactivity → account locked automatically (nightly scheduler)**
- BCrypt password hashing (cost 12)
- Role-Based Access Control (RBAC) with 100+ granular permissions
- JWT claims: `tenant_id`, `branch_id`, `user_id`, `permissions[]`
- Multi-tenant isolation at data layer

## Modules

| Module | Description |
|---|---|
| Auth | Login, logout, token refresh, password management |
| User & RBAC | User management, roles, 100+ permissions |
| Tenant & Branch | Multi-tenant, multi-branch architecture |
| Chemical | Master, registration, stock, issuance, destruction, labels |
| Instrument | Master, calibration workflow, templates, maintenance, downtime |
| Supplier | Master, documents, ratings |
| OMS | Purchase orders (approval workflow), goods receipt |
| QA/QC | Deviations, OOS/OOT, CAPA |
| Sample & Test | Registration, test assignment, result entry, COA |
| Document | Worksheet upload, parse, versioning, execution |
| AI | Inventory forecast, OOS risk, instrument trend, workload |
| Dashboard | Widget-level permissions, role-based KPIs |
| Audit | Full audit trail (21 CFR Part 11 compliant) |

## API Endpoints Summary

```
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout

POST   /api/v1/users
GET    /api/v1/users/{id}
POST   /api/v1/users/{id}/lock
POST   /api/v1/users/{id}/unlock
POST   /api/v1/users/{id}/roles

GET    /api/v1/chemicals/masters
POST   /api/v1/chemicals/registrations
POST   /api/v1/chemicals/{id}/issue
POST   /api/v1/chemicals/{id}/destroy

GET    /api/v1/instruments
POST   /api/v1/instruments/{id}/calibrations
POST   /api/v1/instruments/calibrations/{id}/approve

GET    /api/v1/oms/orders
POST   /api/v1/oms/orders
POST   /api/v1/oms/grn

GET    /api/v1/qa/deviations
POST   /api/v1/qa/oos
POST   /api/v1/qa/capa

POST   /api/v1/samples
POST   /api/v1/samples/{id}/coa/generate

POST   /api/v1/documents
POST   /api/v1/worksheets/{id}/submit

GET    /api/v1/ai/inventory-forecast
GET    /api/v1/ai/oos-risk

GET    /api/v1/dashboard/widgets
GET    /api/v1/audit
```

## Permissions (100+)

All permissions are seeded in `V2__seed_permissions_roles.sql`. JWT token includes the full list for the authenticated user's roles.

Categories: AUTH/ADMIN, CHEMICAL, INSTRUMENT, INVENTORY, SUPPLIER, OMS, QA/QC, SAMPLE/TEST, AI, DASHBOARD

## Compliance

- 21 CFR Part 11 audit trail on all entities
- ALCOA+ data integrity
- Full version history for documents
- E-signature ready (extend with e-signature table)
