# CyPay - Distributed Actor-Based Crypto Platform

> A scalable, modular cryptocurrency transaction platform built with an actor-based architecture inspired by Akka and modern microservices patterns.


## About CyPay

CyPay is a **distributed actor-based platform** designed for managing cryptocurrency transactions with enterprise-grade reliability, supervision, and scalability. Built as a multi-module Spring Boot application with a modern React frontend, it demonstrates advanced concepts in distributed systems: actor model patterns, dynamic scaling, circuit breakers, and resilient error handling.

The project serves as a complete reference implementation of an actor framework featuring supervision trees, dynamic actor pools, message-driven communication, and end-to-end transaction orchestration.


## Key Features

### Actor Framework Core
- **Actor Model Implementation** — Message-driven asynchronous processing with typed mailboxes
- **Dynamic Actor Pools** — Auto-scaling pools that adjust instance count based on message queue depth
- **Supervision Trees** — Hierarchical supervision with automatic restart strategies
- **Centralized Logging** — Professional-grade logging with database persistence for audit trails

### Transaction Management
- **Multi-Currency Support** — Bitcoin (BTC), Ethereum (ETH), EUR, USD wallets
- **Transaction Types** — Buy, Sell, Transfer between users
- **Blockchain Recording** — Every operation recorded with immutable ledger entries
- **Balance Validation** — Real-time balance checks with rollback on failure

### Resilience & Performance
- **Circuit Breakers** — Resilience4j integration for fault tolerance
- **Rate Limiting** — Request throttling to prevent overload
- **Retry Mechanisms** — Automatic retry with exponential backoff
- **Graceful Degradation** — Fallback strategies for external service failures

### Modern Frontend
- **Cyberpunk UI Design** — Dark theme with cyan/purple accents and glassmorphism effects
- **Real-Time Market Data** — Live cryptocurrency prices via CoinGecko API
- **Interactive Dashboard** — Wallet management, transaction history, and trading interface
- **Responsive Design** — Tailwind CSS with mobile-first approach

## Tech Stack

### Backend
| Technology | Purpose |
|------------|---------|
| **Java 17** | Core language |
| **Spring Boot 3.5** | Microservices framework |
| **PostgreSQL** | Primary database |
| **Resilience4j** | Circuit breakers & rate limiting |
| **JWT** | Authentication & authorization |
| **Maven** | Build & dependency management |

### Frontend
| Technology | Purpose |
|------------|---------|
| **React 18** | UI framework |
| **TypeScript** | Type-safe development |
| **Vite** | Build tool & dev server |
| **Tailwind CSS** | Utility-first styling |
| **SweetAlert2** | Toast notifications & modals |

### Architecture Patterns
- **Actor Model** — Message-passing concurrency
- **Microservices** — Domain-driven service separation
- **Event-Driven** — Asynchronous message processing
- **CQRS** — Command Query Responsibility Segregation (for logs)

## Architecture

### System Overview

```
┌─────────────────────────────────────────────────────────────┐
│                        CyPay Platform                       │
├─────────────────────────────────────────────────────────────┤
│  Frontend (React + Vite)                                    |
│  └── Dashboard │ Login │ Market Data │ Transaction Terminal │
├─────────────────────────────────────────────────────────────┤
│  Microservices (Spring Boot)                                │
│  ┌──────────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐  │
│  │ Transactions │ │  Wallet  │ │   User   │ │ Supervisor │  │
│  │   :8080      │ │  :8081   │ │  :8082   │ │   :8083    │  │
│  └──────┬───────┘ └────┬─────┘ └────┬─────┘ └─────┬──────┘  │
│         │              │            │             │         │
│  ┌──────┴──────────────┴────────────┴─────────────┴──────┐  │
│  │              Actor Framework (Core)                   │  │
│  │  ┌────────┐ ┌────────┐ ┌──────────┐ ┌──────────────┐  │  │
│  │  │  Buy   │ │  Sell  │ │ Transfer │ │  Blockchain  │  │  │
│  │  │ Agent  │ │ Agent  │ │  Agent   │ │    Agent     │  │  │
│  │  └───┬────┘ └───┬────┘ └────┬─────┘ └──────┬───────┘  │  │
│  │      └──────────┴───────────┴──────────────┘          │  │
│  │                      │                                │  │
│  │              SupervisorAgent                          │  │
│  │         (Orchestration & Error Handling)              │  │
│  └──────────────────────┬────────────────────────────────┘  │
│                         │                                   │
│  ┌──────────────────────┴────────────────────────────────┐  │
│  │              Dynamic Actor Pools                      │  │
│  │    (Auto-scaling: min/max actors, watermarks)         │  │
│  └───────────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│  Infrastructure                                             │
│  ├── PostgreSQL (Data persistence)                          │
│  ├── CoinGecko API (Market data)                            │
│  └── JWT (Authentication)                                   │
└─────────────────────────────────────────────────────────────┘
```

### Actor System Design

Each critical operation is handled by specialized agents managed through dynamic pools:

| Agent | Responsibility | Pool Strategy |
|-------|---------------|---------------|
| **BuyAgent** | Process cryptocurrency purchases | Scale 1-10 based on queue |
| **SellAgent** | Handle crypto-to-fiat conversions | Scale 1-10 based on queue |
| **TransferAgent** | Execute P2P transfers | Scale 1-8 based on queue |
| **CreateBlockchainAgent** | Record operations on blockchain | Scale 1-5 based on queue |
| **SupervisorAgent** | Orchestrate transaction flows | Single instance |


## Project Structure

```
CyPay/
├── front/                      # React Frontend
│   ├── src/
│   │   ├── pages/
│   │   │   ├── Dashboard.tsx   # Main trading interface
│   │   │   └── Login.tsx       # Authentication page
│   │   ├── App.tsx             # Router & protected routes
│   │   └── index.tsx           # Entry point
│   ├── package.json            # Dependencies
│   └── vite.config.ts          # Build configuration
│
├── framework/                  # Actor Framework Core
│   └── src/main/java/com/cypay/framework/
│       ├── acteur/
│       │   ├── Acteur.java              # Base actor class
│       │   ├── DynamicActorPool.java    # Auto-scaling pool manager
│       │   ├── ActeurLogger.java        # Centralized logging
│       │   ├── Message.java             # Typed message wrapper
│       │   └── MicroserviceManagerActeur.java
│       └── security/
│           ├── JwtValidator.java
│           └── ActeurJwtAuthenticationFilter.java
│
├── transactions/               # Transaction Microservice
│   └── src/main/java/com/example/transactions/
│       ├── agent/              # Actor implementations
│       │   ├── BuyAgent.java
│       │   ├── SellAgent.java
│       │   ├── TransferAgent.java
│       │   ├── CreateBlockchainAgent.java
│       │   ├── SupervisorAgent.java
│       │   └── *Pool.java      # Dynamic pool configurations
│       ├── message/            # Message types
│       │   ├── BuyMessage.java
│       │   ├── SellMessage.java
│       │   └── ...
│       ├── model/              # JPA entities
│       │   ├── Transaction.java
│       │   └── TransactionStatus.java
│       └── service/            # Business logic
│
├── wallet/                     # Wallet Microservice
│   └── Wallet management REST API
│
├── user/                       # User & Authentication Microservice
│   └── JWT-based auth & user management
│
├── supervisor/                 # Supervision & Monitoring
│   └── Error handling & health checks
│
├── logs/                       # Centralized Logging Service
│   └── Audit trail & log aggregation
│
└── init-db.sql                 # Database initialization script
```

## Getting Started

### Prerequisites
- Java 17+
- Node.js 18+
- PostgreSQL 14+
- Maven 3.8+

### Database Setup

```sql
-- Run init-db.sql to create the database schema
psql -U postgres -f init-db.sql
```

### Backend Services

Start each microservice (in separate terminals):

```bash
# 1. Framework (build first - dependency for others)
cd framework && mvn clean install

# 2. User Service (Port 8082)
cd ../user && mvn spring-boot:run

# 3. Wallet Service (Port 8081)
cd ../wallet && mvn spring-boot:run

# 4. Transaction Service (Port 8080)
cd ../transactions && mvn spring-boot:run

# 5. Supervisor Service (Port 8083)
cd ../supervisor && mvn spring-boot:run
```

### Frontend

```bash
cd front
npm install
npm run dev
```

Access the application at `http://localhost:5173`

## API Endpoints

### User Service (Port 8082)
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/auth/register` | POST | Create new account |
| `/api/auth/login` | POST | Authenticate & receive JWT |

### Wallet Service (Port 8081)
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/wallets` | POST | Create wallet |
| `/api/wallets/{userId}` | GET | List user wallets |
| `/api/wallets/{id}` | DELETE | Remove wallet |

### Transaction Service (Port 8080)
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/transactions/buy` | POST | Buy cryptocurrency |
| `/transactions/sell` | POST | Sell cryptocurrency |
| `/transactions/transfer` | POST | Transfer to another user |
| `/transactions/history/{userId}` | GET | Transaction history |
| `/transactions/prices` | GET | Current crypto prices |

### Example: Buy Bitcoin

```bash
curl -X POST http://localhost:8080/transactions/buy \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{
    "userId": 1,
    "cryptoUnit": "BTC",
    "amount": 0.5,
    "paymentUnit": "EUR"
  }'
```

## Transaction Flow

```
User Request
     │
     ▼
┌─────────────┐
│   REST API  │  ← JWT Validation
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│ SupervisorAgent │  ← Orchestrates the transaction
└────────┬────────┘
         │
    ┌────┴────┬──────────┬──────────────┐
    ▼         ▼          ▼              ▼
┌───────┐ ┌────────┐ ┌──────────┐ ┌──────────────┐
│Wallet │ │Wallet  │ │ External │ │  Blockchain  │
│Debit  │ │Credit  │ │  Price   │ │  Record      │
│(Check)│ │(Update)│ │ Service  │ │  (Audit)     │
└───────┘ └────────┘ └──────────┘ └──────────────┘
    │         │          │              │
    └────┬────┴──────────┴──────────────┘
         ▼
┌─────────────────┐
│  Transaction    │  ← Persisted to PostgreSQL
│    Record       │
└─────────────────┘
```

## Dynamic Scaling

The platform automatically scales actor instances based on load:

```java
// Configuration in Pool classes
new DynamicActorPool<>(
    1,                      // minActors: Always keep 1 instance
    10,                     // maxActors: Scale up to 10
    50,                     // highWatermark: Add actor if queue > 50
    10,                     // lowWatermark: Remove actor if queue < 10
    () -> new BuyAgent(...) // Factory for new instances
);
```

**Scaling Behavior:**
- Every 2 seconds, the pool checks total queue depth
- If `queue > highWatermark` and `current < max` → Add actor
- If `queue < lowWatermark` and `current > min` → Remove actor
- All actors process messages concurrently with thread-safe mailboxes

## Testing

### Unit Tests
```bash
cd framework && mvn test
cd transactions && mvn test
```

### Integration Tests
End-to-end tests simulate complete transaction flows:

```bash
cd transactions
mvn test -Dtest=*IntegrationTest
```

**Test Scenarios:**
- Complete buy flow (auth → wallet → transaction → blockchain)
- Transfer between users
- Insufficient balance handling
- Concurrent transaction processing
- Actor pool scaling under load

## Monitoring & Observability

### Health Endpoints
Each service exposes Spring Boot Actuator endpoints:
- `/actuator/health` — Service health status
- `/actuator/metrics` — JVM & application metrics
- `/actuator/info` — Build information

### Logging
- **Console**: Development-friendly output with log levels
- **Database**: Audit trail of all transactions (via ActeurLogger)
- **File**: Rotated logs in `logs/` directory

## Design Decisions

### Why Actor Model?
- **Concurrency**: Lock-free message passing vs shared state
- **Resilience**: Isolated failure domains with supervision
- **Scalability**: Horizontal scaling through dynamic pools
- **Composability**: Easy to add new transaction types

### Why Microservices?
- **Independent Deployment**: Scale services individually
- **Technology Diversity**: Best tool for each domain
- **Fault Isolation**: Failure in one service doesn't cascade
- **Team Autonomy**: Clear bounded contexts

### Why Custom Framework?
While Akka exists, this implementation demonstrates:
- Deep understanding of actor internals
- Educational value for distributed systems concepts
- Full control over threading and memory models
- No external dependencies for core functionality

## License

MIT License - See [LICENSE](LICENSE) file

## Author

**Adam Terrak** — [@NayJi7](https://github.com/NayJi7)
**Anthony Voisin** — [@anthonyvsn](https://github.com/anthonyvsn)

Built as a comprehensive demonstration of distributed systems, actor model architecture, and modern Java/Spring ecosystem capabilities.
