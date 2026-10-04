# Latch — High-Performance In-Memory Persistent Database

[![Java 25+](https://img.shields.io/badge/Java-25%2B-orange.svg)](https://openjdk.org/projects/jdk/25/)
[![JUnit 5](https://img.shields.io/badge/Testing-JUnit%205-green.svg)](https://junit.org/junit5/)
[![Docker](https://img.shields.io/badge/Container-Docker-blue.svg)](https://www.docker.com/)

**Latch** is a lightweight, concurrent, persistent, Redis-inspired in-memory key-value database built in **Java 25+**. 

Built entirely using low-level Java primitives—without relying on Spring Boot or framework magic—Latch demonstrates how production database systems handle **non-blocking I/O (NIO)**, **lock-striped memory storage**, **scalable key expiration min-heaps**, **append-only file persistence**, **optimistic concurrency transactions**, and **latency profiling**.

---

## Technical Architecture & Core Subsystems

```
                                      Client
                                        ↓
                                 [SocketChannel]
                                        ↓
                                  [NIO Selector]
                                        ↓
                              [RESP Protocol Decoder]
                                        ↓
                             [Command Execution Engine]
                                        ↓
                ┌───────────────────────┼───────────────────────┐
                ↓                       ↓                       ↓
         [Sharded Storage]      [Expiration Manager]     [AOF & Snapshots]
         (N-Shard Locks)        (PriorityQueue Heap)     (NIO FileChannel)
```

### 1. Networking & Protocol (`com.latch.server` & `com.latch.protocol`)
- **Non-blocking I/O (`NIO`)**: Uses `ServerSocketChannel`, `SocketChannel`, and `Selector` event loops to handle thousands of concurrent client socket connections without thread-per-connection overhead.
- **RESP Protocol Engine**: Full RESP2/RESP3 serializer and buffer parser supporting both raw binary RESP Arrays (`*N\r\n$M\r\n...`) and inline terminal commands (`PING\r\n`, `SET k v\r\n`).

### 2. Lock-Striped Storage Engine (`com.latch.storage`)
- **Java 25 Sealed Interfaces & Records**: Strongly typed representation of in-memory values:
  ```java
  public sealed interface Value permits StringValue, ListValue, HashValue, SetValue {}
  ```
- **Sharded Locking (`ShardedDatabase`)**: Eliminates global read-write lock bottlenecks by partitioning keys across $N$ independent shards (`hash(key) % N`), each protected by a dedicated `ReentrantReadWriteLock`.

### 3. TTL & Key Expiration (`com.latch.expiration`)
- **Active & Passive Eviction**: Combines $O(1)$ lazy eviction on key lookup with an active `PriorityQueue` min-heap evaluated periodically by a background worker thread.
- **Scalable Architecture**: Avoids scheduling one thread per key, scaling effortlessly to millions of expiring keys with minimal CPU overhead.

### 4. Persistence & Crash Recovery (`com.latch.persistence`)
- **Append-Only File (AOF)**: Logs every mutating operation to disk using Java NIO `FileChannel` with configurable `fsync` policies (`ALWAYS`, `EVERYSEC`, `NO`).
- **Atomic Snapshots**: Writes state to temporary `.tmp` snapshot files, executes `FileChannel.force(true)`, and performs an atomic rename (`Files.move(..., ATOMIC_MOVE)`).

### 5. Transactions (`com.latch.transaction`)
- **`MULTI` / `EXEC` / `DISCARD`**: Queues transaction steps per client session and executes them atomically.
- **Optimistic Concurrency Control (`WATCH`)**: Tracks atomic key version counters, aborting `EXEC` if a watched key is modified by another client during transaction assembly.

---

## Supported Commands

| Data Type | Commands |
|---|---|
| **Server & Info** | `PING`, `INFO` |
| **String** | `SET key value`, `GET key`, `DEL key`, `EXISTS key` |
| **List** | `LPUSH key value [value ...]`, `LPOP key`, `LRANGE key start stop` |
| **Hash** | `HSET key field value`, `HGET key field`, `HDEL key field`, `HGETALL key` |
| **Set** | `SADD key member`, `SREM key member`, `SMEMBERS key`, `SISMEMBER key member` |
| **TTL Expiration** | `EXPIRE key seconds`, `TTL key`, `PERSIST key`, `SETEX key seconds value` |
| **Transactions** | `MULTI`, `EXEC`, `DISCARD`, `WATCH key`, `UNWATCH` |

---

## Getting Started

### Prerequisites
- **Java 25+** (JDK 25 or higher)
- **Maven 3.9+** (or use included `./mvnw` wrapper)

### Build & Run Tests
```bash
# Run all unit, concurrency, persistence, and E2E integration tests
./mvnw test

# Package executable fat JAR
./mvnw package -DskipTests
```

### Start Latch Server
```bash
# Launch server on default port 6380
java -jar target/latch-1.0.0-SNAPSHOT.jar 6380
```

### Test with `nc` (Netcat) or `redis-cli`
```bash
$ nc localhost 6380
PING
+PONG

SET name alice
+OK

GET name
$5
alice
```

---

## Containerization with Docker

```bash
# Build and run with Docker Compose
docker compose up -d

# Test containerized server
nc localhost 6380
```

---

## Performance & Microbenchmarking

Latch includes both **JMH microbenchmarks** and a **custom multi-threaded latency benchmarking client**:

```bash
# Run JMH Storage Engine Benchmark
java -cp target/latch-1.0.0-SNAPSHOT.jar com.latch.storage.StorageBenchmark
```

### Benchmark Summary (5 Concurrent Clients, 50/50 GET/SET Workload)
- **Throughput**: ~4,700+ ops/sec
- **P50 (Median Latency)**: `0.66 ms`
- **P95 Latency**: `1.46 ms`
- **P99 Latency**: `2.57 ms`

---

## Project Structure

```
latch/
├── src/main/java/com/latch/
│   ├── LatchServer.java          # Main CLI entry point
│   ├── server/                   # Java NIO TCP event loop & connection management
│   ├── protocol/                 # RESP2 protocol encoder and decoder
│   ├── command/                  # Command handlers & registry
│   ├── storage/                  # Sharded lock-striped storage engine & sealed values
│   ├── expiration/               # ExpirationManager with PriorityQueue heap
│   ├── persistence/              # FileChannel AOF writer & atomic snapshot manager
│   ├── transaction/              # Transaction control & watched key versioning
│   └── metrics/                  # Observability metrics registry
├── src/test/java/com/latch/      # Comprehensive JUnit 5 test suites
├── Dockerfile                    # Multi-stage container image
├── compose.yaml                  # Docker Compose configuration
├── pom.xml                       # Dependencies & plugins (JaCoCo, Surefire)
└── README.md
```
