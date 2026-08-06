# SmartTube Phone (Core+)

Android phone/tablet UI module for SmartTube. Reuses business logic from `common` + MediaServiceCore; does **not** depend on Leanback TV UI (`smarttubetv`).

## Build

```bash
./gradlew :smarttubephone:assembleStstableDebug
```

Other flavors matching `common`: `stbeta`, `ststable`, `stfdroid`.

## What this module includes (Core+)

- Browse (sections drawer + video shelves/grids)
- Search
- Playback (ExoPlayer + suggestions + quality/speed dialogs)
- Sign-in (device code + QR)
- Channel / channel uploads
- Material `AppDialog` (settings, SponsorBlock prefs opened via existing presenters, quality/speed)

## Extracting to a separate repository

Copy or submodule these paths together with `smarttubephone`:

| Path | Why |
|------|-----|
| `smarttubephone/` | Phone app |
| `common/` | Presenters, prefs, ExoPlayerController |
| `MediaServiceCore/` | YouTube API (`youtubeapi`, interfaces) |
| `SharedModules/` | sharedutils, appupdatechecker2, constants |
| `exoplayer-amzn-2.10.6/` | ExoPlayer fork |
| `fragment-1.1.0/` | MotherActivity base (FragmentActivity fork) |
| `filepicker-lib/` | Transitive via common |
| `slidableactivity/` | MotherActivity slide gesture |
| `leanbackassistant/` | Currently required by `common` (ATV channels); keep until decoupled |

Root files: `settings.gradle`, `build.gradle`, `gradle.properties`, Gradle wrapper.

**Do not require** for phone-only: `smarttubetv/`, `leanback-1.0.0/`, `chatkit/`, `doubletapplayerview/` (unless you add those features).

When splitting, point `settings.gradle` `include` at the modules above and keep flavor names `stbeta` / `ststable` / `stfdroid` aligned with `common`.

## Package / applicationId

- Package: `com.liskovsoft.smartyoutubetv2.phone`
- Default applicationId (ststable): `app.smarttube.phone`
