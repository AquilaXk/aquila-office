# Security Policy

## Supported Versions

Security updates are applied to the latest release on the `main` branch and published via official GitHub Releases.

| Version | Supported |
| :--- | :--- |
| `0.1.x` (latest `main`) | Yes |

## Security Architecture & Invariants

Aquila Office enforces strict **Fail-Closed** security invariants across authentication, external integration credentials, workspace artifact access, and desktop IPC boundaries:

1. **Session & Token Verification**:
   - Access tokens and refresh sessions verify cryptographic signatures, expiration bounds, and workspace membership boundaries before granting access to workspace-scoped resources.
2. **Encrypted External Credentials**:
   - Workspace gateway tokens, bot tokens, and repository credentials are encrypted at rest with `AES-256-GCM` (`AQUILA_EXTERNAL_TOKEN_KEY`) and are never echoed in plaintext API responses or application logs.
3. **Artifact Path Traversal & Resource Ceiling Defense**:
   - Workspace artifact readers normalize paths against the isolated workspace root, reject `..` traversal and symbolic link escapes, and enforce explicit depth, entry count, and byte size ceilings before reading file contents.
4. **Signed Webhook & Replay Verification**:
   - External event webhooks validate cryptographic request signatures and timestamp freshness windows before processing payloads.
5. **Bounded Desktop Runtime IPC**:
   - Tauri desktop commands enforce strict byte, path, watcher count, and OS keychain size bounds and reject malformed or out-of-range inputs without silent fallbacks.

## Reporting a Vulnerability

Please report suspected vulnerabilities privately via [GitHub Security Advisories](https://github.com/AquilaXk/aquila-office/security/advisories/new) on the repository rather than opening a public issue containing exploit details or credentials.
