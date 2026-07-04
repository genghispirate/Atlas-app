# Pact 🛡

**A private, encrypted trust network for beating phone addiction.**

Pact locks the apps that pull you in. The only way back in is your **circle** — the people you
trust — approving your request. There are no passwords, no accounts, no servers, and no codes to
type. Just people who care, holding your locks.

<p align="center"><em>Ready-to-install APK: <a href="release/Pact-v4.0.apk"><code>release/Pact-v4.0.apk</code></a></em></p>

---

## How it works

Pact has two sides, chosen on first launch:

**If you're locking your apps:**

1. **Choose your name and build your circle.** Add trusted people — a partner, a parent, a
   friend, a therapist — by having them install Pact, pick "I'm a trusted person", and scan
   your code. One scan pairs you. Add as many as you like.
2. **Pick your apps and raise the shield.** A guided, one-permission setup; the accessibility
   service then covers any locked app with a full-screen wall the moment it opens.
3. **Want back in?** Each app has a difficulty:
   - **Red** (default): the wall sends a request to your circle. They see the app, how long
     you asked for, and why. If they approve — per your rule — the time starts automatically,
     even if you walked away.
   - **Yellow**: a 30-second mindful pause and a quick "what's pulling you?", then a short
     break — no approval needed, but a cooldown stops back-to-back unlocks.
   - (Green is simply an app you never added.)
4. **The circle decides the hard stuff too.** Removing an app, relaxing a red lock to yellow,
   turning off strict mode, or ending your Pact are all requests your circle approves.

**If you're a trusted person:**

Install the same app, pick "I'm a trusted person", scan their code. Their requests land on your
home screen — Approve, Not now, or grant a custom amount with a note. You can also just message
them. One device can hold the trust of several people.

## The trust model — public-key cryptography, zero setup

There is **no shared secret and no TOTP**. Instead, every device generates an **Ed25519**
signing key and an **X25519** encryption key on first launch, wrapped by the hardware-backed
Android Keystore. Public keys are exchanged once, by QR. From then on:

- Every request, approval, and message is **signed** by its author and **sealed** (encrypted)
  to the recipient's public key. The transport only ever carries opaque ciphertext.
- Incoming messages are rejected unless they decrypt, carry a valid signature from a **pinned**
  key you actually trust, haven't expired, and use a **nonce** never seen before.

You never see a key, a code, or a protocol name. You see people, messages, and requests.

### Approval rules

Configurable per circle: **any one** person can approve, a **majority** must, or **everyone**
must. The rule engine is a pure, unit-tested function — approvals are deterministic.

### Permissions

Each trusted person is scoped: approve requests, view your stats, or **chat only**. You can
change this, or remove someone, any time.

## Security properties

| Threat | Defense |
|---|---|
| Forged approval | Every payload is Ed25519-signed; a signature from anyone but a pinned key is rejected |
| Replay / duplicate approval | Per-sender nonce ledger + one-decision-per-approver + short request expiry |
| Tampering | AES-256-GCM authenticated encryption; any bit-flip fails to open |
| Eavesdropping | Sealed to the recipient's X25519 key; the relay sees only ciphertext on a random inbox |
| Key substitution | Keys are pinned at pairing; only the initial pair-accept may come from an unpinned key |
| Reading keys at rest | Private keys wrapped by the Android Keystore; chat history encrypted at rest |
| Chaining self-unlocks | Yellow tier has a 30-minute cooldown |
| Quietly disabling the shield | Strict mode also locks system Settings and the package installer |
| Losing a trusted person's device | Revoke them — their key is unpinned and everything from it is rejected |

## Offline & delivery

Messages queue locally and drain with exponential backoff, so nothing is lost when you're
offline. Delivery is **instant while either app is open** (a fast in-app sync loop) and syncs
on a ~15-minute schedule in the background via WorkManager. Honest limitation: with no
app-owned servers there is no push, so background delivery isn't instantaneous.

## Transport is pluggable

Business logic talks only to a `Transport` interface, so the carrier can be swapped —
public relay → Nostr → custom — **without touching the crypto, the approval engine, or the UI**.
The default is an open public relay (no accounts) that carries only end-to-end-encrypted
payloads on unguessable inbox topics.

## Also included

Difficulty tiers · urge tracking · post-break reflection · an Insights screen (streak, walls
per day, most tempting apps, craving hours, triggers) · a home-screen widget · quiet,
actionable notifications (requests, approvals, messages) · encrypted passphrase backup + stats
CSV.

## Install

Copy `release/Pact-v4.0.apk` to **both** phones — yours and each trusted person's — allow
"install from unknown sources", and follow the in-app setup. Requires Android 8.0+ (API 26).
No Google services needed.

## Build from source

```bash
cd pact
# Point local.properties at your Android SDK, then:
./gradlew assembleRelease   # → app/build/outputs/apk/release/app-release.apk
./gradlew test              # crypto + approval-policy unit tests
```

The release build is signed with the checked-in keystore (`keystore/pact-release.keystore`,
passwords in `app/build.gradle.kts`) so anyone can produce an installable build. **This keystore
is intentionally public — generate your own to distribute.**

## Architecture

```
app/src/main/java/com/pact/app/
├── MainActivity.kt / PactApp.kt   # navigation, live-sync lifecycle
├── core/
│   ├── CryptoBox.kt      # Ed25519 sign/verify + X25519 sealed boxes (pure JVM, tested)
│   ├── Wire.kt           # signed+sealed payload protocol, inbound verification
│   ├── Transport.kt      # swappable transport interface + relay impl + offline outbox
│   ├── TrustNetwork.kt   # contacts, chat, request/approval engine, policy (tested)
│   ├── SyncWorker.kt     # WorkManager background sync
│   ├── PactState.kt      # blocking state, tiers, stats
│   ├── Notifications.kt / Backup.kt / Apps.kt / Qr.kt / Vault.kt
├── service/              # accessibility shield + service-drawn lock overlay
├── PactWidget.kt         # home-screen widget
└── ui/                   # Compose: onboarding, home, circle, chat, requests,
                          #   lock wall, insights, settings, scanner
```

Kotlin · Jetpack Compose · Material 3 · BouncyCastle · OkHttp · WorkManager · StateFlow ·
10 languages · min SDK 26, target SDK 35.
