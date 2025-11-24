# (Demo)실시간 클릭율 및 조회수 파이프라인

이 프로젝트는 스트리밍 조회(impression) 및 클릭(click) 이벤트로부터 CTR(Click-Through-Rate)을 처리하는 파이프라인을 실습합니다.  이 파이프라인은 Kafka, Flink, Redis 를 사용하여 구축되었으며, 결과를 제공하기 위한 FastAPI 백엔드를 포함합니다.  
또한 분석용 싱크(Sink)로 **ClickHouse**와 **DuckDB**를 포함하며, 모니터링을 위한 **Prometheus/Grafana** 스택도 갖추고 있습니다.

## 🏛️ 아키텍처

데이터는 다음과 같이 시스템을 통해 흐릅니다:

```
                                      ┌──────────────────┐
                                      │   Serving API    │
                                      │    (FastAPI)     │
                                      └──────────────────┘
                                               ▲
                                               │ (GET /ctr/...)
                                               │
┌──────────┐   ┌───────┐   ┌───────────┐   ┌───────┐
│ Producers│──>│ Kafka │──>│ Flink App │──>│ Redis │
└──────────┘   └───────┘   └───────────┘   └───────┘
     (Python)   (Events)    (10s Window)   │ (Hashes)
                                           │
                                           ├──> ┌────────────┐
                                           │    │ ClickHouse │
                                           │    └────────────┘
                                           │
                                           └──> ┌────────────┐
                                                │   DuckDB   │
                                                └────────────┘
                                                      
                                             ┌──────────────┐
                                             │ RedisInsight │
                                             └──────────────┘
```

## ✨ 주요 기능

-   **실시간 처리:** **10초** 텀블링 윈도우(Tumbling Window)에 걸쳐 CTR을 계산합니다.
-   **상태 기반 분석:** 비교 분석을 위해 **최신** CTR 결과와 **직전** CTR 결과를 모두 유지합니다.
-   **멀티 싱크(Multi-Sink) 저장소:**
    -   **Redis:** 저지연(Low-latency) 서빙용.
    -   **ClickHouse:** 고성능 OLAP 분석용.
    -   **DuckDB:** 임베디드/로컬 파일 기반 분석용.
-   **관측 가능성(Observability):** **Prometheus**(메트릭 수집)와 **Grafana**(대시보드)를 통한 포괄적인 모니터링.
-   **RESTful API:** 계산된 CTR 데이터에 접근할 수 있는 엔드포인트를 제공하는 FastAPI 서버.
-   **컨테이너화:** 전체 환경은 Docker를 사용하여 컨테이너화되어 있으며, Docker Compose와 쉘 스크립트로 관리됩니다.

## 🧩 구성 요소

| 컴포넌트 | 디렉토리 | 설명 |
| --- | --- | --- |
| **Data Producers** | `producers/` | 사용자 노출 및 클릭을 시뮬레이션하여 Kafka 토픽으로 이벤트를 전송하는 Python 스크립트입니다. |
| **Event Stream** | `docker-compose` | 이벤트 스트림 수집을 위한 3개의 브로커로 구성된 Kafka 클러스터입니다. |
| **Flink Processor** | `flink-app/` | 이벤트를 소비하고, 상태 기반 CTR 계산을 수행하며, 결과를 Redis, ClickHouse, DuckDB로 전송하는 Java/Flink 애플리케이션입니다. |
| **Data Stores** | `docker-compose` | Redis (Hot data), ClickHouse (OLAP), DuckDB (Local analysis). |
| **Serving API** | `serving-api/` | Redis에서 데이터를 읽어 REST API를 통해 CTR 데이터를 노출하는 Python/FastAPI 애플리케이션입니다. Docker Compose로 관리됩니다. |
| **Monitoring** | `docker-compose` | Prometheus (Metrics) 및 Grafana (Visualization). |
| **Monitoring UIs** | `docker-compose` | 시스템 컴포넌트를 관찰하고 관리하기 위한 Kafka UI, Flink Dashboard, RedisInsight, Grafana입니다. |
| **Performance Test**| `performance-test/`| API 부하 테스트 및 윈도우 로직 검증을 위한 Python 스크립트입니다. |

## 🛠️ 기술 스택

-   **Streaming:** Apache Kafka, Apache Flink
-   **Backend & Serving:** FastAPI, Uvicorn
-   **Databases:** Redis, ClickHouse, DuckDB
-   **Monitoring:** Prometheus, Grafana
-   **Languages:** Java 11, Python 3.8
-   **Containerization:** Docker, Docker Compose
-   **Build Tools:** Maven, uv

## 🚀 시작하기

로컬 머신에서 전체 파이프라인을 실행하려면 다음 단계를 따르세요.

### 사전 요구 사항

-   Docker 및 Docker Compose
-   Docker 이미지를 가져오기 위한 인터넷 연결

### 실행 방법

단 하나의 스크립트로 인프라 시작, 데이터베이스 초기화, Flink 작업 배포, 데이터 생성기 시작까지 모든 과정을 수행합니다.

```shell
./scripts/setup.sh
```

스크립트가 완료되면 전체 파이프라인이 실행 중인 상태가 됩니다!

## 📊 확인 방법

시스템을 관찰하고 데이터에 접근하는 방법은 다음과 같습니다.

### 모니터링 대시보드

| 서비스 | URL | 설명 |
| --- | --- | --- |
| **Serving API** | `http://localhost:8000/docs` | 대화형 API 문서 (Swagger UI). |
| **Flink UI** | `http://localhost:8081` | Flink 작업 및 클러스터 상태 모니터링. |
| **Kafka UI** | `http://localhost:8080` | Kafka 토픽 및 메시지 탐색. |
| **RedisInsight** | `http://localhost:5540` | Redis에 저장된 데이터 검사를 위한 GUI. |
| **Grafana** | `http://localhost:3000` | Flink 메트릭 시각화 (User/Pass: admin/admin). |

### API 엔드포인트

Serving API는 다음과 같은 주요 엔드포인트를 제공합니다:

| 메서드 | 엔드포인트 | 설명 |
| --- | --- | --- |
| `GET` | `/ctr/latest` | 모든 상품에 대한 최신 CTR 데이터를 가져옵니다. |
| `GET` | `/ctr/previous` | 모든 상품에 대한 이전 CTR 데이터를 가져옵니다. |
| `GET` | `/ctr/{product_id}` | 특정 상품에 대한 최신 및 이전 CTR 통합 뷰를 가져옵니다. |

### 데이터 검증

**ClickHouse:**
```bash
docker compose exec -it clickhouse clickhouse-client --query "SELECT count() FROM ctr_results"
```

**DuckDB:**
```bash
# Copy DuckDB file and WAL file from container
docker cp flink-taskmanager:/tmp/ctr.duckdb ./ctr_check.duckdb
docker cp flink-taskmanager:/tmp/ctr.duckdb.wal ./ctr_check.duckdb.wal

# Query with duckdb cli (requires duckdb installed locally)
duckdb ctr_check.duckdb "SELECT * FROM ctr_results LIMIT 10"
```

**Redis:**
```bash
docker compose exec -it redis redis-cli HGETALL ctr:latest
```

## 🛑 중지 방법

제공된 스크립트를 사용하여 애플리케이션의 각 부분을 중지할 수 있습니다.

```bash
# 데이터 생성기 중지
./scripts/stop-producers.sh

# Flink 작업 중지
./scripts/stop-flink-job.sh

# 모든 인프라 서비스 중지 및 제거
docker-compose down
```

## 📜 스크립트 개요

이 프로젝트는 관리를 단순화하기 위해 `/scripts` 디렉토리에 여러 헬퍼 스크립트를 포함하고 있습니다:

| 스크립트 | 설명 |
| --- | --- |
| `setup.sh` | 전체 파이프라인(인프라, DB 초기화, Flink Job, Producer)을 한 번에 설정하고 실행합니다. |
| `create-topics.sh` | Kafka에 필요한 `impressions` 및 `clicks` 토픽을 생성합니다. |
| `deploy-flink-job.sh` | Flink 애플리케이션을 클러스터에 배포합니다. |
| `stop-flink-job.sh` | 실행 중인 Flink 작업을 찾아 취소합니다. |
| `start-producers.sh` | Python 데이터 생성기를 백그라운드에서 시작합니다. |
| `stop-producers.sh` | 백그라운드 생성기 프로세스를 중지합니다. |

*참고: 스크립트를 실행 가능하게 만들어야 할 수도 있습니다: `chmod +x scripts/*.sh`*

## ⚙️ 구성 상세

### Flink 처리 로직

-   **Window:** 10초 텀블링 윈도우 (Tumbling Window)
-   **Time Characteristic:** 이벤트 시간 (Event Time)
-   **Watermark:** 2초 (최대 2초 늦게 도착하는 이벤트 처리)
-   **Allowed Lateness:** 5초 (매우 늦은 이벤트로 윈도우 업데이트 허용)

### Redis 데이터 구조

-   **`ctr:latest`**: 각 상품의 가장 최근 CTR 결과를 저장하는 Redis Hash.
    -   `field`: `product_id`
    -   `value`: CTR 데이터가 포함된 JSON 문자열
-   **`ctr:previous`**: 최신 윈도우 바로 직전 윈도우의 결과를 저장하는 Redis Hash. 구조는 `ctr:latest`와 동일합니다.

---

*이 프로젝트는 AI 어시스턴트 **Codex** 및 **Gemini**를 활용하여 반복적으로 개발하고 개선했습니다.*
