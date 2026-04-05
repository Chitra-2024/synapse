# Synapse Frontend Dashboard

This app is the UI for the Synapse trade reconciliation demo. It presents a live terminal-style dashboard for monitoring reconciled option strategies produced by the `core-engine` service and exposed by the `api-service`.

The dashboard is built with React and polls the backend every 2 seconds to display:

- live throughput, break rate, and latency-style KPIs
- a searchable reconciliation feed
- status labels for `MATCHED`, `PRICE_BREAK`, and `QUANTITY_BREAK`
- connection state for the backend API

## How It Fits Into The Project

Synapse is split into three parts:

1. `core-engine`
   Generates synthetic strategy legs, publishes them to Kafka, reconciles exchange vs ledger data, and stores the result in H2.
2. `api-service`
   Reads reconciled strategies from the shared H2 database and exposes them over HTTP.
3. `frontend-dashboard`
   Polls the API and renders the real-time monitoring interface.

Data flow:

`core-engine` -> Kafka topics -> reconciliation -> H2 database -> `api-service` `/results` -> `frontend-dashboard`

## Tech Stack

- React 19
- Create React App / `react-scripts`
- Axios
- CSS-based custom terminal UI

## Backend Contract

The frontend expects the API service to be available on `http://localhost:8081`.

In development, this is handled by the proxy defined in `package.json`:

```json
"proxy": "http://localhost:8081"
```

The dashboard calls:

- `GET /results`

Expected response shape:

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

## Prerequisites

Before running the dashboard end to end, make sure you have:

- Node.js and npm
- Java 21
- Maven
- Docker Desktop or another Docker runtime for Kafka/Zookeeper

## Running The Full Project Locally

Start services in this order from the repository root:

1. Start Kafka and Zookeeper:

```bash
docker-compose up
```

2. Start the reconciliation engine:

```bash
cd core-engine
mvn spring-boot:run
```

3. Start the API service:

```bash
cd api-service
mvn spring-boot:run
```

4. Start the frontend:

```bash
cd frontend-dashboard
npm install
npm start
```

Then open `http://localhost:3000`.

## Frontend Scripts

From `frontend-dashboard/`:

### `npm start`

Runs the dashboard in development mode at `http://localhost:3000`.

### `npm test`

Runs the React test suite.

### `npm run build`

Creates a production build in `build/`.

## UI Notes

The current interface includes:

- a left navigation rail with Dashboard, Analytics, and Settings views
- KPI cards summarizing current reconciliation output
- a live feed table sorted by newest reconciled strategy first
- row highlighting for newly arrived records
- local search by `strategyId`

The Analytics and Settings tabs are currently placeholders for future expansion.

## Operational Notes

- The frontend polls `/results` every 2 seconds.
- A disconnected backend shows a disconnected status badge and loading/empty states.
- Both Spring services point at the same file-based H2 database (`~/synapsedb`), which is how the API reads results written by the engine.
- The core engine currently generates both matched and intentionally broken strategies to make the dashboard visually useful during development.

## Project Structure

```text
frontend-dashboard/
|-- public/
|-- src/
|   |-- App.js
|   |-- App.css
|   |-- index.js
|   |-- index.css
|   |-- setupTests.js
|   `-- reportWebVitals.js
|-- package.json
`-- README.md
```

## Next Areas To Improve

- replace the placeholder latency metric with a real backend-supplied value
- add charts or historical analytics for break trends
- document or expose health endpoints for the full stack
- add stronger automated frontend tests around API states and filtering
