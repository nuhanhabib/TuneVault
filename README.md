# TuneVault

A personal Android music player that saves songs and playlists from YouTube as **audio only**, so I can listen **offline**, **with no ads**, and **with the screen locked**.

## Features

- Paste a YouTube or YouTube Music link: a single song or a whole playlist
- Search for songs inside the app
- Share a link from the YouTube app → **TuneVault** to save it straight away
- Saves only the audio stream (no video), in the background, with a progress notification
- Offline library with songs and playlists, filtering, shuffle and repeat
- Background playback with lock-screen and notification controls
- Pauses automatically when headphones are unplugged
- Sync a playlist to pick up songs added on YouTube later
- The download engine updates itself once a day, and you can also update it from Settings

## Tech stack

| Part | Library |
|---|---|
| UI | Kotlin, Jetpack Compose, Material 3 (dynamic colors) |
| Playback | AndroidX Media3 (ExoPlayer + MediaSessionService) |
| Downloading | [yt-dlp](https://github.com/yt-dlp/yt-dlp) via [youtubedl-android](https://github.com/JunkFood02/youtubedl-android) |
| Storage | Room database, app-private music folder |
| Images | Coil |

## Project structure

```
app/src/main/java/com/nuhanhabib/tunevault/
├── TuneVaultApp.kt          App setup and daily yt-dlp update
├── data/
│   ├── YtDlp.kt             Search, metadata and audio-only download
│   ├── MusicRepository.kt   Library logic (add, sync, delete)
│   └── db/                  Room entities, DAO and database
├── download/
│   └── DownloadService.kt   Foreground download queue
├── playback/
│   ├── PlaybackService.kt   Background player and media session
│   └── PlayerConnection.kt  UI ↔ player bridge
└── ui/                      Compose screens, view model and theme
```

## Build and install

Requirements: [Android Studio](https://developer.android.com/studio) (free), or JDK 17+ and the Android SDK.

1. Open the project in Android Studio and let Gradle sync.
2. Connect your phone with USB debugging turned on and press **Run**.

Or build an APK from the command line:

```bash
./gradlew assembleDebug
```

The APK is created at `app/build/outputs/apk/debug/app-debug.apk`. Copy it to the phone and open it to install. You'll need to allow "Install unknown apps".

The build only includes `arm64-v8a`, which covers almost every modern phone. To support other devices, add more ABIs in `app/build.gradle.kts`.

### Release build (optional)

Create a `keystore.properties` file in the project root. Git ignores it.

```properties
storeFile=my-release-key.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Then run `./gradlew assembleRelease`.

## Tips

- **Samsung / One UI:** Settings → Apps → TuneVault → Battery → **Unrestricted**. This stops the system from pausing music or downloads while the screen is off.
- If downloads start failing, open **Settings → Update now** to get the latest yt-dlp.
- Songs are stored in the app's private folder, so uninstalling the app deletes them.

## Disclaimer

This is a personal project for private, non-commercial use. Downloading content from YouTube may go against YouTube's Terms of Service. Respect copyright and support the artists you listen to.

## License

[MIT](LICENSE) © Nuhan Habib
