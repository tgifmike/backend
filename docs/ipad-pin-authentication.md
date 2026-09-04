# Native iPad PIN authentication requirements

The native iPad is part of the PIN authentication security boundary. Its implementation must follow these rules.

## Account-first employee flow

- OAuth-authenticate the manager/device enrollment flow online, then enroll an iPad with an `accountId`; `locationId` is optional for account-wide devices.
- Download `GET /ipad/devices/{deviceId}/pin-verifiers`. An account-wide device receives all active PIN users in that account, not only users assigned to one location.
- Call `POST /auth/pin/verify` with `locationId: null` after the employee enters their account PIN. The response includes `userId`, `userName`, and `accountId` alongside the restricted action token.
- Let the employee select a location after PIN verification. When a line check starts, the backend verifies that the employee has access to the selected location and attributes the line check to that employee.
- A device enrolled with a specific location remains restricted to that location.

## Enrollment and storage

- Generate the Ed25519 signing key in Secure Enclave when supported, and never export the private key.
- Send the Base64-encoded X.509 public key during online enrollment.
- Store the one-time device token in iOS Keychain. Clearing local application data must also remove the token and signing key, invalidating the enrollment on that installation.
- Store downloaded verifier bundles in Keychain or protected device storage. Never use `UserDefaults`, application logs, analytics payloads, crash metadata, or plaintext files.
- A verifier bundle expires 24 hours after generation by default. A revoked or replaced PIN can continue to verify offline until the previously downloaded bundle expires; the UI and operating procedures must account for this unavoidable offline window.

## Offline verification and lockout

- Verify the entered PIN locally against the supplied Argon2id PHC verifier. The `offlineVerifier` JSON field is an ASCII string in this exact shape:

  `$argon2id$v=19$m=19456,t=2,p=1$<standard-base64-salt>$<standard-base64-hash>`

  The salt and derived hash use the PHC string's standard Argon2 Base64 alphabet (`+` and `/` are valid). Do not URL-decode this verifier, add the online account/user HMAC pepper, or recompute it without parsing the embedded salt and parameters. Use an iOS Argon2id implementation that accepts PHC encoded strings (or parse this format and pass the decoded values to its low-level Argon2id API).
- Maintain `failedAttempts`, `lockoutLevel`, `lockedUntil`, and `lastFailedAt` independently for each account/user credential.
- Persist lockout state in Keychain so an application restart does not clear it.
- Apply this schedule: attempts 1–4 have no credential lock; attempt 5 locks for 5 minutes; attempt 6 for 15 minutes; attempt 7 for 1 hour; attempt 8 for 4 hours; attempt 9 for 8 hours; and attempt 10 or later for 24 hours.
- An attempt during a lock does not increment the failure count. The next incorrect attempt after expiry advances the schedule. A successful local verification resets that credential's local failure and lockout state.
- Use monotonic elapsed time while the process is running. After restart, detect a significant backward wall-clock change and require an online bundle refresh instead of shortening or clearing a lock.

## Offline line checks and synchronization

- Every offline line check records `deviceId`, `accountId`, `locationId`, `userId`, `credentialVersion`, verification timestamp, and a unique local event ID.
- Upload signed events in sequence through `POST /ipad/devices/{deviceId}/pin-events/batch`. Duplicate event IDs are safe to retry.
- Sign the UTF-8 bytes of this canonical pipe-delimited representation with Ed25519:

  `eventId|sequenceNumber|eventType|accountId|locationId|userId|credentialVersion|occurredAt|lockoutUntil|lineCheckId`

  Null `lockoutUntil` and `lineCheckId` values are empty strings. UUIDs and timestamps use their standard Java string representations. Encode the signature with standard or URL-safe Base64.
- Synchronize line-check data even when its credential version is stale. The server preserves the data and marks its authentication as `STALE_CREDENTIAL`.

## Server secrets

Production requires independent values for `PIN_LOOKUP_SECRET`, `PIN_HASH_PEPPER`, and `PIN_ACTION_TOKEN_SECRET`, each at least 32 characters. `PIN_OFFLINE_ENCRYPTION_KEY` is a Base64-encoded 32-byte AES key. These values must not reuse `jwt.secret` and must be managed in the deployment secret store.
