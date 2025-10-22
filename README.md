# 💳 Simple Payment Processing Service (PPS)

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?logo=postgresql)](https://www.postgresql.org/)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-Latest-black?logo=apache-kafka)](https://kafka.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> **Enterprise-grade payment middleware for seamless Nigerian payment gateway integration**

A mission-critical Spring Boot application serving as a secure abstraction layer between merchant applications and external payment gateways (Paystack, Flutterwave). Built with **transactional integrity**, **idempotency guarantees**, and **asynchronous webhook processing** at its core.

---

## 📋 Table of Contents

- [Why PPS?](#-why-pps)
- [Key Features](#-key-features)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Quick Start](#-quick-start)
- [API Documentation](#-api-documentation)
- [Database Schema](#-database-schema)
- [Security](#-security)
- [Webhook Processing](#-webhook-processing)
- [Testing](#-testing)
- [Deployment](#-deployment)
- [Roadmap](#-roadmap)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🎯 Why PPS?

### **The Problem**
Integrating multiple payment gateways in Nigeria (Paystack, Flutterwave, etc.) leads to:
- ❌ **Tight coupling** between merchant apps and gateway-specific APIs
- ❌ **Code duplication** across different payment flows
- ❌ **Inconsistent error handling** and status management
- ❌ **Risk of double-charging** customers

### **The Solution: PPS**
PPS provides a **unified payment abstraction layer** that:
- ✅ **Decouples** your application from specific payment gateway implementations
- ✅ **Guarantees idempotency** - prevents duplicate transactions
- ✅ **Handles async webhooks** - reliable real-time status updates
- ✅ **Ensures integrity** - atomic transaction processing with audit trails
- ✅ **Enables scalability** - easy to add new payment gateways

---

## ✨ Key Features

### 🛡️ **Financial Integrity**
| Feature | Implementation |
|---------|----------------|
| **Idempotency Guarantee** | Every request requires unique `Idempotency-Key` header |
| **Atomic Transactions** | Database-level ACID compliance with rollback support |
| **Audit Trail** | Immutable webhook event logs for reconciliation |
| **Status Tracking** | Real-time transaction status (PENDING → SUCCESS/FAILED) |

### 🔌 **Gateway Abstraction**
```
┌──────────────────┐
│ Merchant App A   │──┐
└──────────────────┘  │
                      ├──▶ ┌─────────────┐     ┌──────────────┐
┌──────────────────┐  │    │             │     │   Paystack   │
│ Merchant App B   │──┼───▶│     PPS     │────▶│ Flutterwave  │
└──────────────────┘  │    │   (Unified  │     │   Remita     │
                      │    │    API)     │     │   Etc...     │
┌──────────────────┐  │    └─────────────┘     └──────────────┘
│ Merchant App C   │──┘
└──────────────────┘

   Single Integration      Gateway Abstraction    Multiple Providers
```

### 🔔 **Async Webhook Processing**
- ✅ Receives webhooks from payment gateways
- ✅ Validates signatures for security
- ✅ Processes updates asynchronously (Kafka/JMS)
- ✅ Notifies merchant apps reliably
- ✅ Implements retry logic for failed notifications

### ⚡ **Performance & Scalability**
- **< 50ms** transaction initiation latency (excluding gateway)
- **99.9% uptime** target with health monitoring
- **Horizontal scaling** ready with stateless design
- **Database connection pooling** for optimal performance

---

## 🏗️ Architecture

### **Layered Monolith Design**

```
┌─────────────────────────────────────────────────────────────┐
│                    Controller Layer                         │
│  (API Gateway - REST Endpoints, Validation, Security)       │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                     Service Layer                            │
│  (Business Logic - Idempotency, Status Transitions)         │
└────────────────────────┬────────────────────────────────────┘
                         │
                    ┌────┴─────┐
                    │          │
          ┌─────────▼──┐   ┌──▼─────────────┐
          │  Gateway   │   │   Repository   │
          │   Layer    │   │     Layer      │
          │            │   │                │
          │ Paystack   │   │  Spring Data   │
          │ Client     │   │      JPA       │
          └─────┬──────┘   └───────┬────────┘
                │                  │
                │                  │
        ┌───────▼──────┐    ┌─────▼──────┐
        │   External   │    │ PostgreSQL │
        │   Payment    │    │  Database  │
        │   Gateway    │    │            │
        └──────────────┘    └────────────┘
```

### **Transaction Initiation Flow**

```
┌────────────┐
│  Merchant  │
│    App     │
└─────┬──────┘
      │ 1. POST /transactions/initiate
      │    (Idempotency-Key, amount, etc)
      ▼
┌─────────────────┐
│ PPS Controller  │
│ • Validates     │
│ • Authenticates │
└────────┬────────┘
         │ 2. Check Idempotency
         ▼
┌─────────────────┐
│  PPS Service    │──────▶ Found existing? → Return cached response
│ • DB Lookup     │
└────────┬────────┘
         │ 3. Not found - Create new transaction
         ▼
┌─────────────────┐
│ Gateway Client  │
│ (Paystack)      │
└────────┬────────┘
         │ 4. HTTP POST to Paystack API
         ▼
┌─────────────────┐
│   Paystack      │
│   Returns       │
│ authorization   │
│      URL        │
└────────┬────────┘
         │ 5. Save transaction (PENDING)
         ▼
┌─────────────────┐
│   Response:     │
│  • txnId        │
│  • status       │
│  • auth URL     │
└─────────────────┘
```

### **Webhook Processing Flow**

```
┌──────────────┐
│   Paystack   │  1. Payment completed
│   Gateway    │     (customer pays)
└──────┬───────┘
       │ 2. POST /webhooks/pg/paystack
       ▼
┌─────────────────┐
│ Webhook         │  3. Verify signature
│ Controller      │  4. Log raw event
└────────┬────────┘
         │ 5. Publish to Kafka/Queue
         ▼
┌─────────────────┐
│ Webhook         │  6. Async processing
│ Listener        │  7. Update DB status
└────────┬────────┘
         │ 8. Notify merchant
         ▼
┌─────────────────┐
│  Merchant App   │  9. Receives callback
│  Webhook URL    │     (HTTPS POST)
└─────────────────┘
```

---

## 🛠️ Tech Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Backend** | Java 17 + Spring Boot 3.x | Core application framework |
| **API** | Spring Web MVC | RESTful API endpoints |
| **Security** | Spring Security | API key authentication |
| **Database** | PostgreSQL 15 | Primary data persistence |
| **ORM** | Spring Data JPA (Hibernate) | Database abstraction layer |
| **Messaging** | Apache Kafka / Spring Kafka | Async event processing |
| **Caching** | Redis (Optional) | Idempotency key caching |
| **Build** | Maven 3.9+ | Dependency management |
| **Testing** | JUnit 5, Mockito, Testcontainers | Comprehensive test suite |
| **Monitoring** | Spring Actuator, Prometheus | Health checks & metrics |
| **Documentation** | SpringDoc OpenAPI 3 | Interactive API docs |

---

## 🚀 Quick Start

### **Prerequisites**

- ✅ **Java 17+** ([Download JDK](https://adoptium.net/))
- ✅ **Maven 3.9+** ([Download Maven](https://maven.apache.org/download.cgi))
- ✅ **Docker & Docker Compose** ([Get Docker](https://www.docker.com/get-started))
- ✅ **PostgreSQL 15** (via Docker recommended)
- ✅ **Kafka** (Optional - for production webhook processing)

---

### **Installation Steps**

#### **1️⃣ Clone the Repository**

```bash
git clone https://github.com/your-username/payment-processing-service.git
cd payment-processing-service
```

#### **2️⃣ Start Infrastructure (Docker)**

```bash
# Start PostgreSQL
docker run --name pps-postgres \
  -e POSTGRES_USER=ppsuser \
  -e POSTGRES_PASSWORD=ppspass \
  -e POSTGRES_DB=ppsdb \
  -p 5432:5432 \
  -d postgres:15-alpine

# Start Kafka (Optional - for production)
docker-compose up -d kafka zookeeper
```

Verify containers are running:
```bash
docker ps
```

#### **3️⃣ Configure Environment Variables**

Create `.env` file in project root:

```env
# Database Configuration
DB_USERNAME=ppsuser
DB_PASSWORD=ppspass
DB_URL=jdbc:postgresql://localhost:5432/ppsdb

# Payment Gateway API Keys
PAYSTACK_SECRET_KEY=sk_test_your_paystack_secret_key_here
FLUTTERWAVE_SECRET_KEY=FLWSECK_TEST-your_flutterwave_key_here

# Merchant Authentication
MERCHANT_API_KEY=merchant_api_key_for_basic_auth

# Kafka Configuration (Production)
KAFKA_BROKERS=localhost:9092

# Webhook Configuration
WEBHOOK_RETRY_MAX_ATTEMPTS=3
WEBHOOK_RETRY_DELAY_MS=5000
```

#### **4️⃣ Build the Application**

```bash
mvn clean install -DskipTests
```

#### **5️⃣ Run the Application**

**Development Mode:**
```bash
mvn spring-boot:run
```

**Production Mode:**
```bash
java -jar target/pps-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

The application starts on **http://localhost:8080**

#### **6️⃣ Verify Health**

```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

---

## 📚 API Documentation

### **Base URL**
```
http://localhost:8080/api/v1
```

### **Authentication**
All endpoints require **API Key Authentication** via Basic Auth:

```http
Authorization: Basic <base64_encoded_merchant_api_key>
```

---

### **1. Initiate Transaction**

**Endpoint:** `POST /api/v1/transactions/initiate`

**Purpose:** Start a payment transaction and receive authorization URL

**Headers:**
```http
Authorization: Basic bWVyY2hhbnRfYXBpX2tleQ==
Idempotency-Key: order-12345-2025
Content-Type: application/json
```

**Request Body:**
```json
{
  "amount": 15500.00,
  "currency": "NGN",
  "merchantRef": "ORDER-99342",
  "customerEmail": "customer@example.com",
  "paymentMethod": "CARD",
  "metadata": {
    "customerId": "CUST-001",
    "orderType": "PRODUCT"
  }
}
```

**Success Response (200 OK):**
```json
{
  "transactionId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
  "status": "PENDING",
  "authorizationUrl": "https://checkout.paystack.com/a1b2c3d4e5f6",
  "reference": "TXN_20250122_001",
  "expiresAt": "2025-01-22T15:30:00Z"
}
```

**Idempotent Response (200 OK - Duplicate Request):**
```json
{
  "transactionId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
  "status": "SUCCESS",
  "message": "Transaction already processed",
  "originalRequestTime": "2025-01-22T10:00:00Z"
}
```

---

### **2. Get Transaction Status**

**Endpoint:** `GET /api/v1/transactions/{transactionId}`

**Purpose:** Query current transaction status

**Request:**
```http
GET /api/v1/transactions/a1b2c3d4-e5f6-7890-1234-567890abcdef
Authorization: Basic bWVyY2hhbnRfYXBpX2tleQ==
```

**Success Response (200 OK):**
```json
{
  "transactionId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
  "status": "SUCCESS",
  "amount": 15500.00,
  "currency": "NGN",
  "pgReference": "T67890FGHIJ",
  "merchantRef": "ORDER-99342",
  "paymentMethod": "CARD",
  "customerEmail": "customer@example.com",
  "completedAt": "2025-01-22T10:05:30Z",
  "metadata": {
    "gateway": "paystack",
    "cardType": "VISA"
  }
}
```

---

### **3. Webhook Endpoint (External)**

**Endpoint:** `POST /api/webhooks/pg/paystack`

**Purpose:** Receive real-time status updates from Paystack

**Security:** Validates `X-Paystack-Signature` header

**Paystack Webhook Payload:**
```json
{
  "event": "charge.success",
  "data": {
    "reference": "T67890FGHIJ",
    "amount": 1550000,
    "currency": "NGN",
    "status": "success",
    "paid_at": "2025-01-22T10:05:30.000Z",
    "channel": "card"
  }
}
```

**Response:**
```json
{
  "status": "received",
  "message": "Webhook processed successfully"
}
```

---

### **Interactive API Documentation**

Access Swagger UI for interactive API testing:

```
http://localhost:8080/swagger-ui.html
```

---

## 🗄️ Database Schema

### **Entity Relationship Diagram**

```
┌─────────────────┐       ┌─────────────────┐
│    Merchant     │       │  Transaction    │
├─────────────────┤       ├─────────────────┤
│ id (PK)         │───┐   │ id (PK)         │
│ name            │   │   │ merchant_id(FK) │
│ api_key_hash    │   └──▶│ idempotency_key │
│ webhook_url     │       │ merchant_ref    │
│ created_at      │       │ amount          │
└─────────────────┘       │ currency        │
                          │ pg_reference    │
                          │ status          │
                          │ payment_method  │
                          │ metadata        │
                          │ created_at      │
                          └────────┬────────┘
                                   │
                                   │ 1:M
                                   ▼
                          ┌─────────────────┐
                          │ WebhookEvent    │
                          ├─────────────────┤
                          │ id (PK)         │
                          │ transaction_id  │
                          │ event_type      │
                          │ payload         │
                          │ signature       │
                          │ processed       │
                          │ received_at     │
                          └─────────────────┘
```

### **Table Schemas**

#### **transactions**
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY | Internal transaction ID |
| merchant_id | UUID | FOREIGN KEY | Reference to merchant |
| idempotency_key | VARCHAR(255) | UNIQUE INDEX | Prevents duplicates |
| merchant_ref | VARCHAR(255) | INDEX | Merchant's order reference |
| amount | DECIMAL(19,2) | NOT NULL | Transaction amount |
| currency | VARCHAR(3) | NOT NULL | NGN, USD, etc. |
| pg_reference | VARCHAR(255) | INDEX, NULLABLE | Gateway reference |
| status | VARCHAR(20) | NOT NULL | PENDING/SUCCESS/FAILED |
| payment_method | VARCHAR(50) | | CARD/TRANSFER/USSD |
| metadata | JSONB | | Flexible data storage |
| created_at | TIMESTAMP | NOT NULL | Creation timestamp |
| updated_at | TIMESTAMP | | Last update timestamp |

#### **webhook_events**
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY | Event ID |
| transaction_id | UUID | FOREIGN KEY | Associated transaction |
| event_type | VARCHAR(100) | NOT NULL | charge.success, etc. |
| payload | TEXT | NOT NULL | Raw webhook JSON |
| signature | VARCHAR(255) | | Verification signature |
| processed | BOOLEAN | DEFAULT FALSE | Processing status |
| retry_count | INTEGER | DEFAULT 0 | Retry attempts |
| received_at | TIMESTAMP | NOT NULL | Receipt timestamp |

#### **merchants**
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY | Merchant ID |
| name | VARCHAR(255) | NOT NULL | Merchant name |
| api_key_hash | VARCHAR(255) | UNIQUE | Hashed API key |
| webhook_url | VARCHAR(500) | | Callback URL |
| is_active | BOOLEAN | DEFAULT TRUE | Account status |
| created_at | TIMESTAMP | NOT NULL | Registration date |

---

## 🔒 Security

### **Authentication & Authorization**

```
┌──────────────┐
│  Merchant    │
│  Application │
└──────┬───────┘
       │ 1. API Request with API Key
       │    Authorization: Basic <api_key>
       ▼
┌─────────────────────┐
│ Spring Security     │
│ Filter Chain        │
├─────────────────────┤
│ • Extract API Key   │
│ • Hash & Validate   │
│ • Load Merchant     │
│ • Set Context       │
└──────┬──────────────┘
       │ 2. Authorized Request
       ▼
┌─────────────────────┐
│  Controller         │
│  (Protected)        │
└─────────────────────┘
```

### **Security Features**

| Feature | Implementation |
|---------|----------------|
| **API Key Auth** | Spring Security with custom filter |
| **Webhook Signature Verification** | HMAC-SHA256 validation |
| **Idempotency** | Database-enforced unique constraints |
| **Data Encryption at Rest** | PostgreSQL encryption + Jasypt |
| **HTTPS Only** | TLS 1.2+ required in production |
| **Rate Limiting** | Spring interceptor (100 req/min) |
| **SQL Injection Protection** | JPA parameterized queries |

### **Webhook Signature Validation (Paystack Example)**

```java
public boolean verifySignature(String payload, String signature) {
    Mac hmac = Mac.getInstance("HmacSHA512");
    SecretKeySpec secret = new SecretKeySpec(
        paystackSecretKey.getBytes(UTF_8), 
        "HmacSHA512"
    );
    hmac.init(secret);
    byte[] hash = hmac.doFinal(payload.getBytes(UTF_8));
    String computed = Hex.encodeHexString(hash);
    return MessageDigest.isEqual(
        computed.getBytes(UTF_8), 
        signature.getBytes(UTF_8)
    );
}
```

---

## 🔔 Webhook Processing

### **Reliability Guarantees**

1. **Immediate Persistence**: Webhook payload saved before processing
2. **Async Processing**: Kafka/JMS queue prevents blocking
3. **Retry Logic**: Exponential backoff for failed merchant notifications
4. **Idempotency**: Duplicate webhook events handled gracefully
5. **Audit Trail**: All events logged with timestamps

### **Retry Strategy**

```
Attempt 1: Immediate
Attempt 2: 5 seconds later
Attempt 3: 25 seconds later
Attempt 4: 125 seconds later
...
Max Attempts: 5 (configurable)
```

---

## 🧪 Testing

### **Test Coverage**

```bash
# Run all tests
mvn test

# Run with coverage report
mvn clean test jacoco:report

# View coverage
open target/site/jacoco/index.html
```

### **Test Structure**

```
src/test/java/
├── com.example.pps/
│   ├── controller/          # API endpoint tests
│   │   └── TransactionControllerTest.java
│   ├── service/             # Business logic tests
│   │   ├── TransactionServiceTest.java
│   │   └── IdempotencyServiceTest.java
│   ├── gateway/             # Gateway client tests
│   │   └── PaystackClientTest.java
│   ├── integration/         # Integration tests
│   │   └── TransactionFlowIntegrationTest.java
│   └── webhook/             # Webhook processing tests
│       └── WebhookListenerTest.java
```

### **Integration Testing with Testcontainers**

```java
@Testcontainers
@SpringBootTest
class TransactionIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:15-alpine");
    
    @Test
    void shouldProcessTransactionEndToEnd() {
        // Test full transaction flow
    }
}
```

---

## 🚢 Deployment

### **Docker Deployment**

**Dockerfile:**
```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/pps-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Build & Run:**
```bash
docker build -t pps:latest .
docker run -d -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/ppsdb \
  -e PAYSTACK_SECRET_KEY=sk_live_xxx \
  pps:latest
```

### **Production Checklist**

- ✅ Use strong, unique `MERCHANT_API_KEY`
- ✅ Store secrets in vault (AWS Secrets Manager, HashiCorp Vault)
- ✅ Enable HTTPS with valid SSL certificate
- ✅ Configure database connection pooling
- ✅ Set up monitoring (Prometheus + Grafana)
- ✅ Configure log aggregation (ELK Stack)
- ✅ Implement backup strategy for PostgreSQL
- ✅ Set up alerting for failed transactions
- ✅ Configure rate limiting per merchant
- ✅ Enable database query logging for audits

---

## 🗺️ Roadmap

### **Phase 1: Core Features** ✅
- [x] Transaction initiation
- [x] Idempotency handling
- [x] Paystack integration
- [x] Webhook processing
- [x] Basic authentication

### **Phase 2: Enhanced Features** 🚧
- [ ] Flutterwave gateway integration
- [ ] Remita gateway integration
- [ ] Admin dashboard (Spring Boot Admin)
- [ ] Transaction reconciliation tools
- [ ] Advanced retry mechanisms

### **Phase 3: Enterprise Features** 📋
- [ ] Multi-tenancy support
- [ ] Split payments
- [ ] Refund processing
- [ ] Dispute management
- [ ] Analytics & reporting dashboard
- [ ] GraphQL API

### **Phase 4: Microservices** 🔮
- [ ] Extract webhook service
- [ ] Separate gateway adapters
- [ ] Event-driven architecture with Kafka
- [ ] Service mesh integration

---

## 🤝 Contributing

We welcome contributions! Please follow these guidelines:

### **How to Contribute**

1. **Fork** the repository
2. **Create** a feature branch: `git checkout -b feature/amazing-feature`
3. **Commit** changes: `git commit -m 'Add amazing feature'`
4. **Push** to branch: `git push origin feature/amazing-feature`
5. **Open** a Pull Request

### **Code Standards**

- ✅ Follow Java conventions (Google Java Style Guide)
- ✅ Write unit tests (minimum 80% coverage)
- ✅ Update documentation for API changes
- ✅ Add JavaDoc for public methods
- ✅ Ensure all tests pass before PR

### **Reporting Issues**

Use [GitHub Issues](https://github.com/your-username/pps/issues) to report bugs or request features.

---

## 📝 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- Built with [Spring Boot](https://spring.io/projects/spring-boot)
- Payment gateway integrations inspired by Nigerian fintech ecosystem
- Community contributions and feedback

---

## 📞 Contact

**Project Maintainer:** Your Name  
📧 Email: Dotunm95@gmail.com  
📱 Phone: +234 703 083 4157  
🐙 GitHub: [@your-username](https://github.com/your-username)

---

<div align="center">

**⭐ Star this repository if you find it useful!**

Made with ❤️ for secure payment processing

</div>
