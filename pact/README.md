# Pact 🛡

**Give the key to someone who cares.**

Pact is an Android app for people fighting phone / social-media addiction. It locks the apps
that pull you in, and the *only* way to unlock them is a 6-digit code from a trusted person
you chose — your **sponsor**: a partner, parent, or close friend. Willpower stops being the
weak link.

<p align="center"><em>Ready-to-install APK: <a href="release/Pact-v2.2.apk"><code>release/Pact-v2.2.apk</code></a></em></p>

---

## How it works

Pact has two sides, chosen on first launch:

**If you're locking your apps:**

1. **Choose your sponsor.** During setup you name a trusted person.
2. **Hand over the key.** Pact generates a standard TOTP secret and shows it **once** as a
   QR code. Your sponsor scans it on **their** phone — easiest with Pact itself in sponsor
   mode, or with Google Authenticator / Aegis / any RFC 6238 authenticator. The key never
   appears again on your phone.
3. **Pick your apps.** Instagram, TikTok, YouTube… anything installed.
4. **Raise the shield.** Guided, step-by-step setup of the one Android permission Pact
   needs. The accessibility service then watches for locked apps reaching the foreground and
   instantly covers them with the lock wall — drawn as an accessibility overlay by the
   service itself, so it cannot be suppressed by Android's background-activity restrictions.
   Code entry uses a built-in PIN pad.
5. **Want back in?** Ask your sponsor for the current code. Enter it, choose a break length
   (5 min / 15 min / 1 h / until midnight), and the app relocks automatically afterwards.
   Ending a break early is always free — and you can also unlock straight from Pact's home
   screen without opening the blocked app.

**If you're the sponsor:**

Install the same APK, choose **"I'm the sponsor"** during setup, and scan the QR from their
phone with the built-in portrait scanner (torch included). Pact then shows the live 6-digit
code with a 30-second countdown ring — no separate 2FA app needed. One sponsor phone can
hold keys for several people.

### Why TOTP means it works offline

Unlock codes are the same technology as 2FA codes: both phones derive the same 6-digit code
from a shared secret plus the clock. **Neither phone needs internet — ever.** Your sponsor
can read you a code over a phone call, SMS, or across the kitchen table.

## Design principles

- **Asymmetric friction.** Locking more apps is one tap. Unlocking, removing an app, changing
  sponsor, or resetting all require a fresh code from your sponsor.
- **Calm, not punitive.** The block screen is a quiet dark gradient with an encouraging line,
  not a red alarm. The app is dark-only, low-stimulation by design.
- **Private by architecture.** No account, no server, no analytics. The TOTP secret is stored
  encrypted with an AES-256-GCM key in the Android Keystore.

## Anti-cheat measures

| Threat | Defense |
|---|---|
| Guessing codes | 5 wrong attempts → 5-minute cooldown |
| Reusing an overheard code | Each 30-second code slot is accepted only once (replay protection) |
| Reading the secret from storage | Encrypted via Android Keystore; plaintext shown only once at setup |
| Quietly disabling the accessibility service | Optional **Strict mode** also locks system Settings behind a code |
| Uninstalling Pact | Honest limitation: sideloaded apps can be uninstalled. Strict mode adds friction (uninstall confirmation lives in Settings), but a determined user can remove the app. Pact is a commitment device, not a prison. |

## Install

1. Copy `release/Pact-v2.2.apk` to **both** phones (yours and your sponsor's), or download it
   from this repo.
2. Open it and allow "install from unknown sources" when prompted.
3. Follow the in-app introduction — each phone picks its role during setup.

Requires Android 8.0+ (API 26). No Google services needed.

## Build from source

```bash
cd pact
# Point local.properties at your Android SDK, then:
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

The release build is signed with the checked-in keystore (`keystore/pact-release.keystore`,
passwords in `app/build.gradle.kts`) so anyone can produce an installable build. **This
keystore is intentionally public — do not reuse it for anything else**, and generate your own
if you plan to distribute.

Unit tests (`./gradlew test`) verify the TOTP engine against the official RFC 6238 test
vectors, guaranteeing compatibility with Pact's sponsor mode and every standard
authenticator app.

## Project structure

```
app/src/main/java/com/pact/app/
├── MainActivity.kt          # routing: onboarding / user home / sponsor home
├── core/
│   ├── Totp.kt              # RFC 6238 TOTP + Base32 + otpauth parsing (pure JVM, tested)
│   ├── Vault.kt             # Android Keystore AES-GCM encryption for secrets
│   ├── PactState.kt         # persisted state, roles, rate limiting, replay protection
│   ├── Apps.kt              # installed-app listing, labels, icons
│   └── Qr.kt                # offline QR rendering (ZXing)
├── service/
│   ├── BlockerService.kt    # accessibility service — the shield
│   └── BlockOverlay.kt      # the lock wall as a service-drawn overlay window
└── ui/                       # Compose: illustrated intro, role select, setup flows,
                              #   sponsor codes, QR scanner, lock wall + PIN pad,
                              #   home dashboard, app picker, settings
```

## Languages

Pact follows your phone’s system language. Included: English, Spanish, French, German,
Portuguese, Arabic (RTL), Hindi, Russian, Japanese, and Chinese (Simplified).

## Tech

Kotlin · Jetpack Compose · Material 3 · ZXing (offline QR render + camera scan) ·
all user-facing text in string resources · min SDK 26, target SDK 35.
