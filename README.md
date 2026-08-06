# TubeTube Phone

Klien YouTube open-source untuk **Android phone/tablet**, berbasis [SmartTube](https://github.com/yuliskov/SmartTube) (modul `smarttubephone` + dependency shared).

## Build

Butuh **JDK 17** dan Android SDK.

```bash
# buat local.properties
echo sdk.dir=C\:\\Users\\YOU\\AppData\\Local\\Android\\Sdk > local.properties

# Windows (PowerShell)
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
.\gradlew :smarttubephone:assembleStstableDebug
```

APK: `smarttubephone/build/outputs/apk/ststable/debug/smarttubephone-ststable-v.XXX.apk`

## Modul

| Path | Peran |
|------|--------|
| `smarttubephone/` | App UI phone |
| `common/` | Presenter, ExoPlayerController, prefs |
| `MediaServiceCore/` | YouTube API |
| `SharedModules/` | sharedutils, dll. |
| `exoplayer-amzn-2.10.6/` | ExoPlayer fork |
| `fragment-1.1.0/`, `slidableactivity/`, `filepicker-lib/`, `leanbackassistant/`, `leanback-1.0.0/` | Dependency `common` |

## applicationId

- `app.smarttube.phone` (flavor `ststable`)

## Lisensi

Kode inti mengikuti lisensi SmartTube / modul upstream masing-masing. Lihat `LICENSE`.
