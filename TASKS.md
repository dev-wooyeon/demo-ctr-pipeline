# Project Upgrade: Data Engineering Portfolio Optimization

This document outlines a roadmap to elevate this Flink project from a "functional prototype" to a "Senior Data Engineer Portfolio" level. It focuses on observability, reliability, testing, and operational excellence.

## Phase 1: Observability & Reliability - "You can't manage what you don't measure"

Senior engineers prioritize visibility and error handling over just "making it work".

- [x] **1.1. Full Observability Stack (Prometheus & Grafana)**
    -   **Why**: Prove you understand how to monitor distributed systems (Backpressure, Checkpoint duration, Lag).
    -   **Task**:
        -   Add `prometheus` and `grafana` services to `docker-compose.yml`.
        -   Configure Flink `flink-conf.yaml` (via env vars) to use the Prometheus Reporter.
        -   Import a standard Flink Dashboard into Grafana (JobManager/TaskManager metrics).
- [ ] **1.2. Dead Letter Queue (DLQ) Pattern**
    -   **Why**: Data Engineers *never* silently drop data. Bad data must be captured for analysis.
    -   **Task**:
        -   Modify the `filter(Event::isValid)` and deserialization logic to capture invalid records.
        -   Use Flink's `SideOutput` to route bad records to a separate stream.
        -   Sink the DLQ stream to a dedicated Kafka topic (e.g., `events.dlq`) or a file system.
- [ ] **1.3. Production-Grade State Management**
    -   **Why**: In-memory state is basic. Large-scale jobs use RocksDB.
    -   **Task**:
        -   Add `minio` (S3 compatible) to `docker-compose.yml` to simulate remote storage.
        -   Configure `RocksDBStateBackend` in the Flink job property.
        -   Verify checkpoints are actually written to MinIO.

## Phase 2: Testing & Data Quality - "Code without tests is legacy code"

backend developers transitioning to DE often neglect this. Bringing strong testing practices is a huge differentiator.

- [ ] **2.1. Integration Testing with TestContainers**
    -   **Why**: Unit tests aren't enough for streaming. You need to prove the pipeline works with real Kafka and ClickHouse interactions.
    -   **Task**:
        -   Set up `TestContainers` for Kafka, Flink (MiniCluster), and ClickHouse.
        -   Write an End-to-End test: Produce msg -> Flink Process -> Query ClickHouse -> Assert Result.
- [ ] **2.2. Data Contracts & Validation**
    -   **Why**: Garbage In, Garbage Out.
    -   **Task**:
        -   Implement stricter validation using a library (e.g., standard `Hibernate Validator` or manual checks) before the windowing phase.
        -   (Optional) Mock a Schema Registry interaction or strictly define the JSON schema in code.

## Phase 3: Engineering Standards & Operations

- [ ] **3.1. Infrastructure as Code (IaC) & CI/CD**
    -   **Why**: Manual `docker-compose up` is okay for dev, but automation shows maturity.
    -   **Task**:
        -   Create a GitHub Actions workflow (`.github/workflows/ci.yml`) that runs:
            -   Linting (ktlint)
            -   Build
            -   Unit & Integration Tests
- [ ] **3.2. Architecture Decision Records (ADR)**
    -   **Why**: Seniors document *why* decisions were made, not just *how*.
    -   **Task**:
        -   Create a `docs/adr` folder.
        -   Write ADRs for: "Why Flink?", "Why ClickHouse as Sink?", "Why RocksDB?", "Checkpointing Strategy".
- [ ] **3.3. Advanced Flink Configuration Documentation**
    -   **Why**: Show you understand the internals.
    -   **Task**:
        -   Document the rationale behind `SlotSharingGroup` usage (already in code, but needs docs).
        -   Explain the Watermark strategy and Allowed Lateness choice in `README.md`.

## Execution Plan
We will proceed systematically starting with **Phase 1.1**.
