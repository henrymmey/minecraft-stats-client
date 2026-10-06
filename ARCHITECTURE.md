# Client Architecture

## Design goals

1. Client-only: the mod must not require installation on the Minecraft server.
2. Generic: no project-specific hardcoding.
3. Non-blocking: Minecraft's main thread must never wait for HTTP or disk I/O.
4. Durable: temporary API/network outages must not lose events.
5. Minimal data collection: collect only explicitly supported statistics.
6. Protocol stability: client and server versions are decoupled.

Fabric's client entrypoint is the correct integration boundary for this project. The implementation should keep client-only classes isolated to avoid class-loading problems on dedicated servers.

## Components

```
Minecraft
  |
  +--> Event adapters / collectors
  |       |
  |       +--> VanillaStatsCollector
  |       +--> AdvancementCollector
  |       +--> SessionCollector
  |       +--> EventCollector
  |
  +--> Local data model
          |
          +--> Change detector
          +--> Durable queue
                  |
                  v
              BatchBuilder
                  |
                  v
              ApiClient
                  |
                  v
               HTTPS API
```

## Planned package structure

```
src/main/java/<base-package>/
├── client/
│   └── HMStatsClient.java
├── config/
│   ├── ClientConfig.java
│   └── ConfigRepository.java
├── api/
│   ├── ApiClient.java
│   ├── ApiRequest.java
│   └── ApiError.java
├── auth/
│   └── ApiKeyCredentials.java
├── ingest/
│   ├── Batch.java
│   ├── BatchBuilder.java
│   └── UploadScheduler.java
├── queue/
│   ├── EventQueue.java
│   └── QueueStore.java
├── stats/
│   ├── StatsCollector.java
│   └── VanillaStatsCollector.java
├── session/
│   └── SessionManager.java
├── events/
│   ├── ClientEvent.java
│   └── EventCollector.java
├── server/
│   └── ServerMatcher.java
└── ui/
    └── ConfigScreen.java
```

## Threading

Minecraft/client callbacks only enqueue small immutable work items.

Network and persistence work runs on dedicated executors.

Never perform:

- HTTP requests
- DNS resolution
- blocking file operations
- retries with sleeps
- JSON serialization of large queues

on the render/client thread.

## Upload strategy

The default flow is:

1. Detect a supported server.
2. Start or resume a session.
3. Collect absolute statistic values.
4. Detect changed values.
5. Collect events.
6. Add events/stat updates to a durable local queue.
7. At a configurable interval, build one batch.
8. Upload asynchronously.
9. Remove acknowledged items only after a successful response.

The server API accepts absolute statistic values, not `+1` deltas. This makes retries idempotent and prevents counter drift.

## Idempotency

Every event receives a stable client-generated event UUID.

The server must treat the event UUID as unique per workspace and reject/ignore duplicate deliveries safely.

## Server filtering

The client may have a local allow-list for user experience and privacy, but the server is authoritative.

A malicious client can modify its configuration; therefore no local check is a security boundary.

## Data collected by default

- Minecraft UUID
- Current username
- Minecraft version
- mod version
- configured server identity/hostname
- session timestamps
- supported vanilla statistics
- supported advancement/event metadata

Not collected by default:

- chat
- private messages
- screenshots
- local files
- coordinates
- inventory contents
- IP addresses

## Failure handling

HTTP 4xx:
- do not endlessly retry invalid credentials or invalid payloads
- surface a useful user-facing status
- keep recoverable queued data where safe

HTTP 429:
- honor Retry-After when present

HTTP 5xx/network failure:
- exponential backoff
- durable queue remains intact

## Privacy

All optional telemetry categories must have explicit configuration flags and documentation. Privacy-sensitive features default to disabled.

## Compatibility

The API has an independent protocol version. The Minecraft version, mod version and API protocol version are all transmitted separately.

See the server OpenAPI contract in the documentation repository:
https://github.com/henrymmey/minecraft-stats-docs
