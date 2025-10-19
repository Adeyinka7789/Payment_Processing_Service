# Payment Processing Service
Payment Processing Service (PPS) - Spring Boot 3.x
Welcome to the Simple Payment Processing Service (PPS), a mission-critical backend application built with Java 17 and Spring Boot 3.x. This project serves as a secure, insulating middleware layer, enabling internal merchant applications to initiate and manage financial transactions seamlessly without tight coupling to specific Nigerian Payment Gateways (e.g., Paystack or Flutterwave). Designed with scalability and resilience in mind, PPS is an outstanding solution for modern payment processing needs.

🚀 Project Overview & Value Proposition
PPS abstracts the complexities of external payment gateways into a unified interface, ensuring transactional integrity, decoupling, and resilience. Whether you're a merchant looking to integrate payment flows or a developer seeking a robust open-source foundation, PPS offers:

Transactional Integrity: Guarantees accurate status tracking and reconciliation for every payment.
Decoupling: Provides a single API layer, insulating your application from gateway-specific implementations.
Resilience: Handles asynchronous updates via secure webhooks, ensuring real-time reliability.

This project is ideal for developers, startups, or educational purposes, showcasing best practices in Spring Boot development, database management, and event-driven architecture.

🎯 Core Design Principles
1. Idempotency Guarantee (Critical)
To prevent errors like double-charging, PPS enforces strong idempotency:

Mechanism: Each initiate request requires a unique Idempotency-Key header.
Process: The service checks this key against the database. If a matching PENDING or SUCCESS transaction is found, the original status is returned, avoiding re-execution.

2. Layered Monolith Architecture
PPS follows a clean, scalable architecture:

Controller: Manages API routing, input validation, and security.
Service: Houses core business logic, including idempotency and status transitions.
Gateway: Translates internal requests into external API calls for gateways like Paystack.
Repository: Leverages Spring Data JPA for PostgreSQL persistence.

This structure ensures separation of concerns and paves the way for future microservice evolution.

🛠️ Technology Stack



Component
Technology
Role



Backend
Java 17 / Spring Boot 3.x
Core API framework


Database
PostgreSQL
Primary persistence for transactions


ORM
Spring Data JPA / Hibernate
Data access and entity mapping


Security
Spring Security
API key authentication and endpoint protection


Messaging
Apache Kafka / Spring Kafka
Asynchronous event processing


Build Tool
Maven
Project management and compilation



💻 Local Development & Setup
Prerequisites

Java Development Kit (JDK) 17+
Maven 3.6+
Docker (Recommended for PostgreSQL and Kafka)

Database Setup (PostgreSQL)
Run a local PostgreSQL instance using Docker:
docker run --name pps-postgres -e POSTGRES_USER=ppsuser -e POSTGRES_PASSWORD=ppspass -e POSTGRES_DB=ppsdb -p 5432:5432 -d postgres

Environment Variables
Configure sensitive keys via environment variables (never hardcode them):



Variable Name
Description
Example Value



DB_USERNAME
PostgreSQL database user
ppsuser


DB_PASSWORD
PostgreSQL database password
ppspass


PAYSTACK_SECRET_KEY
Secret API key for Paystack
sk_live_xyz123abc


MERCHANT_API_KEY
Internal key for merchant apps
merchant123_api_secret


KAFKA_BROKERS
Kafka cluster connection string
localhost:9092


Build & Run

Clone the Repository:git clone [your-repository-url]
cd simple-payment-processing-service


Build the Project:mvn clean install


Run with Production Profile (Ensure Docker DB is running):java -jar target/pps-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

The application will be available at http://localhost:8080.


🌐 API Specification
All synchronous endpoints require API Key Authentication via the Authorization: Basic header (derived from MERCHANT_API_KEY).
1. POST /api/v1/transactions/initiate
Initiates a payment request and returns an authorization URL.

Headers:
Authorization: Basic base64key
Idempotency-Key: order-A92B3C-202501 (unique per request)


Request Body (JSON):{
  "amount": 15500.00,
  "currency": "NGN",
  "merchantRef": "ORDER-99342",
  "customerEmail": "customer.name@example.com",
  "paymentMethod": "CARD"
}


Successful Response (200 OK):{
  "transactionId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
  "status": "PENDING",
  "authorizationUrl": "https://paystack.co/pay/a1b2c3d4e5f6"
}



2. GET /api/v1/transactions/{id}
Retrieves the current transaction status.

Successful Response (200 OK):{
  "transactionId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
  "status": "SUCCESS",
  "amount": 15500.00,
  "pgReference": "T67890FGHIJ"
}



3. POST /api/v1/webhooks/paystack (External)
Receives real-time status updates from Paystack.

Security: Validates payload signatures using the PAYSTACK_SECRET_KEY.
Processing: Logs the raw payload to the webhook_events table and triggers asynchronous status updates.


✅ Testing & Quality Assurance

Unit Tests: JUnit 5 ensures reliability for service logic (e.g., checkIdempotency(), updateStatus()).
Integration Tests: Testcontainers spins up a PostgreSQL instance for true integration testing.
Mocking: Mockito isolates external dependencies (e.g., PaystackClient) for robust testing.


🌟 Future Roadmap
PPS is built for growth:

Multi-Gateway Support: Extend with a GatewayProvider interface for new gateways (e.g., Flutterwave).
Admin Dashboard: Develop a Spring Boot Admin module for transaction monitoring and reconciliation.


🤝 Contributing
Contributions are welcome! Please:

Fork the repository.
Create a feature branch (git checkout -b feature/awesome-feature).
Commit your changes (git commit -m "Add awesome feature").
Push to the branch (git push origin feature/awesome-feature).
Open a Pull Request.


📜 License
This project is licensed under the MIT License - see the LICENSE file for details.

🙌 Acknowledgments

Inspired by the need for robust payment solutions in the Nigerian market.
Built with love using the Spring ecosystem and community support.

Contact me: Dotunm95@gmail.com
+2347030834157
