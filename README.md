# Synapse

Synapse is a real-time trade reconciliation demo built as a small distributed system. It generates synthetic multi-leg options strategies, streams them through Kafka, reconciles exchange and ledger views, stores the results in H2, and exposes them through a React dashboard for live monitoring.

At a glance, the project demonstrates:

- event-driven data flow with Kafka
- reconciliation of exchange vs ledger trade legs
- persistence of reconciled strategy results in H2
- a Spring Boot API layer for serving results
- a React dashboard that visualizes live reconciliation output

## Architecture

The repository is organized into three main applications:

### `core-engine`

The reconciliation engine is responsible for:

- generating synthetic option strategies every 2 seconds
- publishing exchange and ledger trade-leg messages to Kafka
- listening to both Kafka topics
- reconciling each completed strategy
- storing the final status in the shared H2 database

Important behaviors:

- strategy types currently include butterfly-style (`BF_`) and iron condor (`IC_`) strategies
- statuses are classified as `MATCHED`, `PRICE_BREAK`, or `QUANTITY_BREAK`
- quantity and price breaks are intentionally introduced to make the pipeline and dashboard meaningful during development

### `api-service`

The API service is a thin Spring Boot layer that:

- connects to the same H2 database used by `core-engine`
- reads reconciled strategy rows
- exposes them over HTTP for the frontend

Current endpoint:

- `GET /results`

Default port:

- `8081`

### `frontend-dashboard`

The frontend is a React application that:

- polls the API every 2 seconds
- shows live KPI cards and reconciliation feed data
- highlights newly arrived rows
- supports filtering by `strategyId`
- displays connection state for the backend API

Default frontend URL:

- `http://localhost:3000`

## Data Flow

The end-to-end system flow looks like this:

1. `core-engine` generates a synthetic strategy.
2. Exchange-leg messages are published to `exchange-feed-topic`.
3. Ledger-leg messages are published to `ledger-feed-topic`.
4. `ReconciliationService` consumes both streams and groups legs by strategy.
5. Once all expected legs arrive, the strategy is reconciled.
6. A `ReconciledStrategy` row is written to the shared H2 database.
7. `api-service` reads those rows and serves them through `GET /results`.
8. `frontend-dashboard` polls `/results` and renders the live terminal UI.

## Repository Structure

```text
synapse/
|-- api-service/
|-- core-engine/
|-- frontend-dashboard/
|-- docker-compose.yml
`-- README.md
```

## Tech Stack

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Spring Kafka
- H2 Database
- Kafka + Zookeeper via Docker Compose
- React 19
- Axios

## Prerequisites

To run the whole system locally, install:

- Java 21
- Maven
- Node.js and npm
- Docker Desktop or a compatible Docker runtime

## Local Development Setup

Start the services in this order from the repository root.

### 1. Start Kafka and Zookeeper

```bash
docker-compose up
```

This brings up:

- Zookeeper on `2181`
- Kafka on `9092`

### 2. Start the core engine

```bash
cd core-engine
mvn spring-boot:run
```

What it does:

- schedules synthetic trade generation every 2 seconds
- publishes to Kafka
- listens for both topic streams
- reconciles strategies into the database

Health endpoint:

- `GET http://localhost:8080/status`

### 3. Start the API service

```bash
cd api-service
mvn spring-boot:run
```

This service exposes:

- `GET http://localhost:8081/results`

### 4. Start the frontend dashboard

```bash
cd frontend-dashboard
npm install
npm start
```

Then open:

- `http://localhost:3000`

## Configuration Notes

### Shared database

Both Spring Boot services use the same file-based H2 database:

```properties
jdbc:h2:file:~/synapsedb;AUTO_SERVER=TRUE
```

This allows:

- `core-engine` to write reconciled results
- `api-service` to read those results

### Frontend proxy

The React app proxies API requests to the backend service during development:

```json
"proxy": "http://localhost:8081"
```

That is why the frontend code can call `/results` directly without hardcoding the full backend URL.

## Key Endpoints

### Core engine

- `GET /status`

Sample response:

```text
Synapse Core Engine is running. Trade generator is active.
```

### API service

- `GET /results`

Sample payload:

```json
[
  {
    "id": 1,
    "strategyId": "IC_SPX_4500",
    "status": "MATCHED",
    "legCount": 4,
    "netPriceDifference": 0.0,
    "quantityDifference": 0
  }
]
```

## Reconciliation Logic

The current reconciliation flow works like this:

- exchange and ledger legs are cached separately by `strategyId`
- the engine expects 4 legs for iron condors and 3 legs for butterfly-style strategies
- once all expected legs are available on both sides, it calculates:
  - total net price difference
  - total quantity difference
- the strategy is classified as:
  - `PRICE_BREAK` if price difference is greater than `0.001`
  - `QUANTITY_BREAK` if quantity difference is greater than `0`
  - `MATCHED` otherwise

## Frontend Features

The dashboard currently includes:

- a terminal-inspired dark UI
- live throughput, break-rate, and latency-style KPI cards
- a searchable reconciliation feed
- row highlighting for newly received results
- connection status feedback
- placeholder tabs for Analytics and Settings

## Running Individual Test/Build Commands

### Frontend

```bash
cd frontend-dashboard
npm test
npm run build
```

### Backend

```bash
cd core-engine
mvn test
```

```bash
cd api-service
mvn test
```

## Known Limitations

- the frontend latency metric is currently derived in the UI, not supplied by the backend
- the analytics and settings screens are placeholders
- the API layer currently exposes raw reconciliation rows with no pagination or filtering
- the system is optimized for local demonstration rather than production deployment

## Future Improvement Ideas

- add historical analytics and charting
- expose stronger health and metrics endpoints across services
- add filtering, sorting, and pagination on the API side
- add automated integration tests for the end-to-end Kafka to UI flow
- package the services for easier one-command local startup
