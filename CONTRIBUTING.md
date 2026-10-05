# Contributing

Thank you for contributing to Minecraft Stats Platform.

## General rules

- Keep the platform generic; do not add HMT-specific defaults to the client or server.
- Never commit API keys, OIDC client secrets, production database credentials or other secrets.
- Changes that modify the HTTP contract must update the OpenAPI specification.
- Changes that modify client configuration or protocol behavior must update the client documentation.
- Security-sensitive changes require tests.

## Pull requests

Use focused pull requests with a clear description.

Before opening a pull request, run the repository's documented checks.

## Compatibility

Minecraft client compatibility and Stats API protocol compatibility are separate concerns. Do not couple the two without a specific reason.

## Security reports

Do not publish exploitable security details in a normal issue. Follow SECURITY.md or GitHub's private vulnerability reporting.
