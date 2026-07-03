# Pact 🛡

**Give the key to someone who cares.**

Pact is an Android app for people fighting phone / social-media addiction. It locks the apps
that pull you in, and the *only* way to unlock them is a 6-digit code from a trusted person
you chose — a partner, parent, or close friend. Willpower stops being the weak link.

<p align="center"><em>Ready-to-install APK: <a href="release/Pact-v1.0.apk"><code>release/Pact-v1.0.apk</code></a></em></p>

---

## How it works

1. **Choose your keyholder.** During setup you name a trusted person.
2. **Hand over the key.** Pact generates a standard TOTP secret and shows it **once** as a
   QR code. Your keyholder scans it into **their** phone with Google Authenticator (or Aegis,
   Authy, any RFC 6238 authenticator). The key never appears again on your phone.
3. **Pick your apps.** Instagram, TikTok, YouTube… anything installed.
4. **Raise the shield.** An accessibility service watches for locked apps reaching the
   foreground and instantly covers them with a calm, full-screen lock.
5. **Want back in?** Ask your keyholder for the 6-digit code currently showing in their
   authenticator. Enter it, choose a break length (5 min / 15 min / 1 h / until midnight),
   and the app relocks automatically afterwards. Ending a break early is always free.

### Why TOTP means it works offline

Unlock codes are the same technology as 2FA codes: both phones derive the same 6-digit code
from a shared secret plus the clock. **Neither phone needs internet — ever.** Your keyholder
can read you a code over a phone call, SMS, or across the kitchen table.

## Design principles

- **Asymmetric friction.** Locking more apps is one tap. Unlocking, removing an app, changing
  keyholder, or resetting all require a fresh code from your keyholder.
- **Calm, not punitive.** The block screen is a quiet dark gradient with an encouraging line,
  not a red alarm. The app is dark-only, low-stimulation by design.
- **Private by architecture.** No account, no server, no analytics, no network permission
  use. The TOTP secret is stored encrypted with an AES-256-GCM key in the Android Keystore.

## Anti-cheat measures

| Threat | Defense |
|---|---|
| Guessing codes | 5 wrong attempts → 5-minute cooldown |
| Reusing an overheard code | Each 30-second code slot is accepted only once (replay protection) |
| Reading the secret from storage | Encrypted via Android Keystore; plaintext shown only once at setup |
| Quietly disabling the accessibility service | Optional **Strict mode** also locks system Settings behind a code |
| Uninstalling Pact | Honest limitation: sideloaded apps can be uninstalled. Strict mode adds friction (uninstall confirmation lives in Settings), but a determined user can remove the app. Pact is a commitment device, not a prison. |

## Install

1. Copy `release/Pact-v1.0.apk` to your phone (or download it from this repo).
2. Open it and allow "install from unknown sources" when prompted.
3. Follow the in-app setup — you'll need your keyholder (or their phone) present once.

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
vectors, guaranteeing Google Authenticator compatibility.

## Project structure

```
app/src/main/java/com/pact/app/
├── MainActivity.kt          # navigation: onboarding / home / add-apps / settings
├── core/
│   ├── Totp.kt              # RFC 6238 TOTP + Base32 (pure JVM, unit-tested)
│   ├── Vault.kt             # Android Keystore AES-GCM encryption for the secret
│   ├── PactState.kt         # persisted state, rate limiting, replay protection
│   ├── Apps.kt              # installed-app listing, labels, icons
│   └── Qr.kt                # offline QR rendering (ZXing)
├── service/BlockerService.kt # accessibility service — the shield
├── block/BlockActivity.kt    # full-screen lock + code entry + break durations
└── ui/                       # Compose screens: onboarding, home, picker, settings
```

## Tech

Kotlin · Jetpack Compose · Material 3 · single external runtime dependency (ZXing core, for
offline QR rendering) · min SDK 26, target SDK 35.
