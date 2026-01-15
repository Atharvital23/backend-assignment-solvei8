# Factory Backend Assignment

A high-performance Spring Boot backend for processing machine sensor events.

## 1. Architecture
I followed a standard Layered Architecture:
* **Controller:** Handles HTTP requests and JSON parsing.
* **Service:** Contains the business logic (Deduplication, Validation, Batch Processing).
* **Repository:** Manages database interactions using Spring Data JPA.
* **Database:** MySQL for persistent storage.

## 2. Key Implementation Details

### Deduplication Logic
The requirement was to handle duplicates based on `eventId`.
* **Identical Payload:** If `eventId` exists and data matches -> **Ignore**.
* **Different Payload:** If eventId exists but data differs -> Update only if the incoming `receivedTime` is newer than the existing record. If it is older, the update is ignored (stale data)**.
* **New ID:** If `eventId` is new -> **Insert**.

I optimized this by fetching all relevant existing records in one batch query and mapping them to a `Map<String, MachineEvent>` for instant lookup.

### Thread Safety
The system is thread-safe because:
1.  **Stateless Components:** The Service and Controller do not hold shared state.
2.  **Database Transactions:** The `@Transactional` annotation ensures that the entire batch process is atomic. The database handles the row-level locking to prevent race conditions during concurrent updates.

### Performance Strategy
To process 1,000 events in under 1 second (Actual: 0.09s):
* I minimized database round-trips.
* Instead of 1,000 individual `SELECT` and `INSERT` queries, I use **Bulk Reads** and **Bulk Writes**.
* This reduces network overhead and database load significantly.

## 3. How to Run

### Prerequisites
* Java 17 or 21
* MySQL Database running on localhost:3306
* Database name: `factory_db`

### Setup
1.  Clone the repository.
2.  Update `src/main/resources/application.properties` with your MySQL username and password.
3.  Run the `BackendApplication.java` class.

### API Endpoints
* **POST /events/batch**: Ingest a JSON array of events.
* **GET /stats**: Get health statistics for a machine.
* **GET /stats/top-defect-lines**: Get the list of machines with the most defects.

## 4. Design Choices & Trade-offs
* **Database:** I chose MySQL for data integrity, though a NoSQL DB (like Cassandra) might scale better for massive write loads (millions/sec).
* **Validation:** Validation is done immediately upon receipt to reject invalid data before touching the database.