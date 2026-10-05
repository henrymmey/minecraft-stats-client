# Minecraft Stats Client

A generic, client-side Fabric mod for collecting Minecraft statistics and sending them to a self-hosted Minecraft Stats Server.

## Scope

The client is intentionally generic. It must not contain HMT-specific URLs, UUIDs, server addresses, API keys, or workspace identifiers.

Configuration is provided by the user at runtime through the mod configuration.

## Planned features

- Fabric client-only mod
- Configurable API URL and API key
- In-game configuration screen
- Server allow-list
- Minecraft UUID and username detection
- Vanilla statistics collection
- Advancement/event collection
- Session tracking and heartbeats
- Batched HTTPS uploads
- Local retry queue for temporary connectivity failures
- Idempotent event delivery
- Privacy controls
- API/protocol compatibility checks

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) and [CONFIGURATION.md](CONFIGURATION.md).

## Compatibility

Minecraft compatibility is released per supported Minecraft/Fabric version. The network protocol is versioned independently through the server API.

## Security model

API keys are credentials, but a client-side credential can never be treated as a secret. The server therefore enforces scopes, player UUID restrictions, server restrictions, expiry, rate limits and revocation.

The client must never send chat, coordinates, files, screenshots or other unrelated data unless a feature explicitly requires it and the user has opted in.

## Development

The project will use Gradle, Fabric Loom, Java, GitHub Actions and release artifacts.

## Related projects

- Server: https://github.com/henrymmey/minecraft-stats-server
- Dashboard: https://github.com/henrymmey/minecraft-stats-dashboard
- Documentation: https://github.com/henrymmey/minecraft-stats-docs

## License

MIT.