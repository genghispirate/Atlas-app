# Atlas - Personal Travel Companion

A beautiful, interactive 3D globe travel app. Remember where you've been, plan where you're going, and relive your memories.

## 🌍 Features

- **Interactive 3D Globe** — Three.js-powered earth with your places pinned
- **Map View** — OpenStreetMap (CartoDB dark tiles) for a flat map perspective
- **Place Management** — Add, edit, rate, tag your destinations
- **Visited vs Planned** — Color-coded pins (gold = visited, blue = bucket list)
- **Trip Organization** — Group places into trips (coming soon)
- **Profile Stats** — See your travel numbers at a glance
- **Works Offline** — IndexedDB storage, no cloud required
- **Dark Premium Theme** — Glassmorphism, smooth animations

## 📱 Installation

### Option A: Android APK (recommended)

**Requirements:** A computer with Node.js 18+ and Android SDK

```bash
git clone <repo-url>
cd atlas-app
chmod +x build-apk.sh
./build-apk.sh
```

Then install on your phone:
```bash
adb install android/app/build/outputs/apk/debug/app-debug.apk
```

Or just copy the APK file to your phone and tap it.

### Option B: PWA (Progressive Web App)

The built `dist/` folder is a complete PWA. Serve it with any static file server:

```bash
npx serve dist/
# or
python3 -m http.server 8080 --directory dist
```

Then open on your phone's browser and tap "Add to Home Screen" for app-like experience.

### Option C: Dev Server

```bash
cd atlas-app
npm run dev
```

Open the URL on your phone (same WiFi network) for live preview.

## 🛠 Tech Stack

| Layer | Technology | Why |
|-------|-----------|-----|
| 3D Globe | Three.js + React Three Fiber | Open source, no API keys, runs everywhere |
| 2D Map | Leaflet + OpenStreetMap | Free, no Google tracking, dark tiles |
| Storage | IndexedDB (via idb) | Offline-first, no backend |
| State | React hooks | Simple, no Redux overhead |
| Build | Vite | Fast, modern |
| Mobile | Capacitor | Wraps web app as native APK |
| Styling | CSS variables | Dark theme, glassmorphism |

## 🔧 Project Structure

```
atlas-app/
├── src/
│   ├── components/
│   │   ├── Globe.tsx        # 3D globe with Three.js
│   │   ├── MapView.tsx      # 2D map with Leaflet
│   │   └── PlaceDetail.tsx  # Add/edit place form
│   ├── lib/
│   │   └── db.ts            # IndexedDB layer + seed data
│   ├── App.tsx              # Main app + navigation
│   ├── App.css              # Dark theme styles
│   └── main.tsx             # Entry point
├── android/                 # Capacitor Android project
├── dist/                    # Built web assets (PWA-ready)
├── build-apk.sh             # One-command APK builder
└── package.json
```

## 🌐 Open Source Replacements

| Instead of | We use | Reason |
|-----------|--------|--------|
| Google Maps | Leaflet + OpenStreetMap | Free, no API key, no tracking |
| Google Earth | Three.js globe | No token needed, fully customizable |
| Firebase | IndexedDB | Offline, no server, no account |
| Google Play | Capacitor | Self-hosted, no store fees |
| Mapbox | CartoDB Dark tiles | Free tier, beautiful dark theme |

## 📄 License

MIT
