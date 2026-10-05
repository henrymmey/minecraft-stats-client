# Client Configuration

Canonical file:

`config/minecraft-stats.json`

Example:

```json
{
  "enabled": true,
  "api": {
    "url": "https://stats.example.com",
    "key": "mst_client_live_REDACTED"
  },
  "upload": {
    "intervalSeconds": 60,
    "batchSize": 100,
    "maxQueueSize": 5000
  },
  "servers": {
    "allow": [
      "play.example.net:25565"
    ]
  },
  "privacy": {
    "sendStatistics": true,
    "sendAdvancements": true,
    "sendEvents": true
  }
}
```

## Rules

- API URL is user-configurable.
- API key is user-configurable.
- No API credential is shipped in source code.
- No HMT-specific hostname or UUID is shipped in source code.
- The API URL must use HTTPS in production.
- Credentials must be redacted from logs.
- The API key is masked in the configuration UI.

The configuration UI may provide a "Test connection" action, but this must not expose the API key in chat, logs or crash reports.
