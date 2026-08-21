# PRD — TubeTube Phone

## 1. Product Overview

### Product Name

**TubeTube Phone**

### Product Type

Native Android application for smartphones and tablets.

### Product Positioning

TubeTube Phone adalah versi mobile dari TubeTube/SmartTube yang mempertahankan kemampuan inti SmartTube tetapi menggunakan **UX/UI yang dirancang khusus untuk Android phone**, dengan pengalaman penggunaan yang familiar bagi pengguna YouTube mobile.

Aplikasi tidak boleh sekadar melakukan scaling dari layout Android TV.

Target utama:

* Smartphone portrait
* Smartphone landscape
* Tablet
* Touch interaction
* Gesture-based navigation
* Mobile video playback
* Bottom navigation
* Responsive video/content layout

---

# 2. Existing Project Context

Repository:

`https://github.com/gitgitmiko/tubetube`

Upstream project:

`https://github.com/yuliskov/smarttube`

Current repository structure sudah memiliki:

```text
smarttubephone/
common/
MediaServiceCore/
SharedModules/
exoplayer-amzn-2.10.6/
fragment-1.1.0/
slidableactivity/
filepicker-lib/
leanbackassistant/
leanback-1.0.0/
```

Repository saat ini mendefinisikan `smarttubephone` sebagai app UI untuk phone, sementara:

* `common` → presenter, ExoPlayerController, preferences
* `MediaServiceCore` → YouTube API
* `SharedModules` → shared utilities
* `exoplayer-amzn-2.10.6` → ExoPlayer fork

Application ID:

```text
app.smarttube.phone
```

Build environment saat ini menggunakan:

```text
JDK 17
Android SDK
```

Jangan mengganti architecture secara besar-besaran jika existing implementation masih dapat digunakan.

Prioritaskan reuse terhadap:

* YouTube API
* Data model
* Presenter
* Player
* ExoPlayerController
* SponsorBlock
* Preferences
* Authentication/session logic
* Existing media loading logic

---

# 3. Product Vision

TubeTube Phone harus terasa seperti:

> **YouTube mobile experience + SmartTube capabilities**

Bukan:

> SmartTube Android TV yang dipaksa berjalan di smartphone.

UX harus familiar bagi pengguna aplikasi YouTube Android.

Prinsip desain:

1. Mobile first
2. Touch first
3. Portrait first
4. Fast browsing
5. Minimal visual clutter
6. Familiar interaction
7. Smooth scrolling
8. Responsive layout
9. Dark mode sebagai mode utama
10. Existing SmartTube functionality tetap dipertahankan

---

# 4. Design Goals

## 4.1 Familiarity

Pengguna yang pernah menggunakan YouTube Android harus langsung memahami:

* Home
* Shorts
* Subscriptions
* Library
* Search
* Video detail
* Channel
* Comments
* Player

Tanpa perlu mempelajari navigation baru.

## 4.2 Modern Mobile UI

Gunakan visual language yang mendekati aplikasi video modern:

* Rounded thumbnails
* Compact cards
* Bottom navigation
* Floating mini player
* Horizontal chip navigation
* Full-screen player
* Gesture interaction
* Bottom sheets
* Material-style dialogs
* Skeleton loading

## 4.3 Performance

UI harus tetap ringan pada:

* RAM 2 GB
* RAM 3 GB
* RAM 4 GB
* RAM 6+ GB

Hindari rendering seluruh feed sekaligus.

Gunakan:

* RecyclerView / equivalent virtualized list
* Pagination
* Image caching
* Lazy loading
* Thumbnail prefetch secukupnya
* View recycling

---

# 5. Non-Goals

Versi pertama tidak perlu:

* YouTube Studio
* Video upload
* YouTube live streaming creation
* Advanced creator analytics
* Community post creation
* Full YouTube Music replacement
* Social messaging
* Excessive animation
* TV remote navigation

Jangan mengubah project menjadi aplikasi TV + phone dengan UI yang sama.

Phone harus memiliki UX sendiri.

---

# 6. Target Devices

## Smartphone

Minimum target:

```text
Android 8+
```

Target utama:

```text
Android 10+
```

Orientasi:

```text
Portrait
Landscape
```

## Tablet

Support:

* 7 inch
* 8 inch
* 10 inch+
* Landscape
* Portrait

Tablet menggunakan responsive layout berbeda dari smartphone.

---

# 7. Navigation Architecture

Gunakan **Bottom Navigation** pada smartphone.

Primary navigation:

```text
┌───────────────────────────────┐
│                               │
│           CONTENT             │
│                               │
│                               │
├───────────────────────────────┤
│ Home │ Shorts │ + │ Subs │ You │
└───────────────────────────────┘
```

Recommended navigation:

1. Home
2. Shorts
3. Create/Search action
4. Subscriptions
5. Library

Jika fitur Shorts belum tersedia secara penuh, jangan membuat tab kosong.

Alternatif sementara:

```text
Home
Subscriptions
Library
Search
Settings
```

Namun target UX tetap harus menyerupai pola YouTube mobile.

---

# 8. Top App Bar

Home screen menggunakan top app bar.

Structure:

```text
┌──────────────────────────────────────┐
│ TubeTube       Cast  Search  Profile │
└──────────────────────────────────────┘
```

Elements:

### Left

TubeTube logo.

### Right

* Cast
* Search
* Profile/account

Optional:

* Notifications

Jangan menampilkan terlalu banyak icon.

Maximum:

```text
3–4 actions
```

---

# 9. Home Screen

Home merupakan screen paling penting.

## Layout

```text
Top App Bar

Category Chips
────────────────────

Video Card
Thumbnail
Title
Channel
Metadata
⋮

Video Card
Thumbnail
Title
Channel
Metadata
⋮

Video Card
...
```

## Category Chips

Contoh:

```text
All
Music
Gaming
News
Live
Mixes
Anime
Technology
Recently uploaded
```

Chips dapat di-scroll horizontal.

## Video Card

Portrait smartphone:

```text
┌─────────────────────────────┐
│                             │
│        THUMBNAIL            │
│                             │
│                       12:35 │
└─────────────────────────────┘

○  Video title that may span
   up to two lines

   Channel Name
   1.2M views • 2 days ago

                         ⋮
```

Thumbnail:

```text
16:9
```

Title maksimum:

```text
2 lines
```

Metadata maksimum:

```text
2 lines
```

---

# 10. Feed Behavior

Home feed harus infinite scrolling.

Behavior:

1. Load initial content
2. Render first page
3. When user approaches bottom:

   * request next page
4. Show loading indicator
5. Append result
6. Preserve scroll position

Jika API mendukung continuation token, gunakan continuation token.

Jangan melakukan request seluruh data sekaligus.

---

# 11. Pull To Refresh

Home harus mendukung:

```text
Pull down → refresh
```

Saat refresh:

* Reset continuation
* Reload feed
* Preserve user authentication
* Clear stale loading state

---

# 12. Video Detail Screen

Saat user memilih video:

```text
┌──────────────────────────────┐
│                              │
│          VIDEO PLAYER        │
│                              │
└──────────────────────────────┘

Video title

Channel
Subscribe

Views • Date

Like   Dislike   Share   Download

Description
────────────────────────────

Comments
────────────────────────────

Recommended videos
```

Pada portrait:

Player berada di bagian atas.

Pada landscape:

Player menjadi fullscreen.

---

# 13. Video Player

Player adalah komponen paling penting setelah Home.

Gunakan existing:

```text
ExoPlayerController
```

sebisa mungkin.

Jangan membuat player baru jika existing controller masih compatible.

## Player Controls

Tap video:

```text
show controls
```

Controls:

* Play/Pause
* Seek backward
* Seek forward
* Progress
* Duration
* Fullscreen
* Settings
* Subtitle
* Playback speed

Optional:

* Quality
* Audio track
* SponsorBlock
* Cast

---

# 14. Gesture Controls

Implement mobile gestures.

## Single Tap

Toggle player controls.

## Double Tap Left

Seek backward:

```text
-10 seconds
```

## Double Tap Right

Seek forward:

```text
+10 seconds
```

## Swipe Up

Optional:

Open next/recommended video.

## Swipe Down

Exit fullscreen / minimize player.

Gesture harus tidak mengganggu normal scrolling.

---

# 15. Fullscreen Player

Portrait:

```text
Portrait player
```

User dapat menekan fullscreen.

Fullscreen:

```text
Landscape preferred
```

Behavior:

* Hide system UI
* Hide navigation bar
* Landscape
* Immersive mode
* Keep screen awake

Back:

```text
Fullscreen → normal player
```

Bukan langsung keluar dari video.

---

# 16. Picture-in-Picture

Support Android PiP.

Behavior:

User menekan Home saat video sedang diputar.

Application:

```text
App → PiP
```

Video tetap berjalan.

Returning to app:

```text
PiP → Player
```

Gunakan existing SmartTube PiP functionality jika compatible.

---

# 17. Mini Player

Jika user menekan Back saat video sedang diputar:

```text
┌─────────────────────────────┐
│                              │
│         Feed                 │
│                              │
│                              │
├──────────────────────────────┤
│ ┌──────┐ Video title    ✕   │
│ │video │                    │
│ └──────┘                    │
├──────────────────────────────┤
│ Home Shorts Subs Library    │
└──────────────────────────────┘
```

Mini player:

* Thumbnail
* Title
* Play/Pause
* Close

Mini player dapat di-swipe down untuk close.

---

# 18. Search

Search harus terasa seperti aplikasi YouTube mobile.

## Search button

Tap search:

```text
┌──────────────────────────────┐
│ ←  Search videos...      🎙 │
└──────────────────────────────┘
```

Keyboard Android muncul otomatis.

## Search suggestions

Saat user mengetik:

```text
minecraft
minecraft survival
minecraft mod
minecraft tutorial
```

Suggestions berasal dari existing search functionality jika tersedia.

---

# 19. Search Result

Layout:

```text
Search result

[Filter chips]

Video
Thumbnail
Title
Channel
Metadata
⋮

Video
Thumbnail
Title
Channel
Metadata
⋮
```

Support:

* Videos
* Channels
* Playlists
* Live
* Shorts

Jika backend/API tidak mendukung suatu kategori, jangan membuat UI palsu.

---

# 20. Shorts

Jika backend mendukung Shorts, implement mobile-first vertical viewer.

Layout:

```text
┌──────────────────────────────┐
│                              │
│                              │
│          VIDEO               │
│                              │
│                              │
│                              │
│                       ♥      │
│                       💬     │
│                       ↗      │
│                              │
│ Channel                      │
│ Video title                  │
│ Description                  │
└──────────────────────────────┘
```

Behavior:

* Vertical 9:16
* Swipe up → next
* Swipe down → previous
* Tap → pause/play
* Double tap → like

Jika Shorts belum stabil di existing backend, Shorts tidak menjadi blocker MVP.

---

# 21. Subscriptions

Screen:

```text
Subscriptions

Latest
────────────────────

Channel A
Video
Video

Channel B
Video
Video
```

Alternative:

```text
Latest
Channels
```

Horizontal channel list:

```text
○ Channel A
○ Channel B
○ Channel C
○ Channel D
```

Video feed menggunakan layout yang sama dengan Home.

---

# 22. Library

Library berisi:

```text
History
Watch later
Playlists
Downloads
Liked videos
```

Jika fitur belum tersedia pada backend, hide item tersebut.

Jangan menampilkan fitur yang tidak berfungsi.

---

# 23. History

History:

```text
Watch history

Video
Thumbnail
Title
Channel
Watched X minutes ago
⋮
```

Actions:

* Remove from history
* Clear history

Clear history harus menggunakan confirmation dialog.

---

# 24. Watch Later

Watch Later:

```text
Watch later

Video list
```

Swipe action:

```text
Remove
```

---

# 25. Channel Screen

Channel:

```text
┌──────────────────────────────┐
│ Channel banner               │
├──────────────────────────────┤
│ ○ Avatar                     │
│                              │
│ Channel Name                 │
│ 1.2M subscribers             │
│                              │
│ SUBSCRIBE                    │
└──────────────────────────────┘

Home
Videos
Shorts
Live
Playlists
```

Tabs horizontal.

Channel content menggunakan existing API.

---

# 26. Comments

Comments berada setelah video description.

Layout:

```text
Comments 1.2K

○ User
  Comment text...

  Reply   Like   Dislike

○ User
  Comment text...
```

Comments dapat menggunakan bottom sheet untuk pengalaman mobile.

Jika existing comments functionality tidak stabil, implement read-only terlebih dahulu.

---

# 27. Bottom Sheets

Gunakan bottom sheet untuk:

* Sort comments
* Video quality
* Playback speed
* Audio track
* Subtitle
* Share options
* Add to playlist
* Video actions

Contoh:

```text
Playback speed

0.25x
0.5x
0.75x
Normal
1.25x
1.5x
1.75x
2x
```

---

# 28. Overflow Menu

Setiap video memiliki:

```text
⋮
```

Menu:

```text
Add to queue
Save to Watch later
Add to playlist
Share
Download
Don't recommend
Not interested
```

Hanya tampilkan action yang benar-benar didukung.

---

# 29. Share

Gunakan Android native share sheet.

Jangan membuat custom share screen jika tidak diperlukan.

---

# 30. Download

Jika existing SmartTube download functionality tersedia dan compatible:

Support:

* Download video
* Select quality
* Select audio
* Download progress
* Download manager

Download screen:

```text
Downloads

Video title

████████████░░ 72%

720p
145 MB / 200 MB

Pause    Cancel
```

---

# 31. SponsorBlock

Pertahankan SponsorBlock.

Existing SmartTube functionality harus diprioritaskan.

UI settings:

```text
SponsorBlock

☑ Enable SponsorBlock

Categories

☑ Sponsor
☑ Intro
☑ Outro
☑ Self promotion
☑ Interaction
```

Player harus dapat skip segment secara otomatis.

---

# 32. Theme

Primary theme:

```text
Dark
```

Secondary:

```text
Light
System
```

## Dark Theme

Background:

```text
#0F0F0F
```

Surface:

```text
#181818
```

Primary text:

```text
#FFFFFF
```

Secondary text:

```text
#AAAAAA
```

Accent:

```text
#FF0000
```

Jangan menggunakan terlalu banyak warna.

TubeTube harus terasa seperti aplikasi video, bukan dashboard.

---

# 33. Typography

Gunakan Android system typography / Roboto.

Hierarchy:

```text
Video title:
16sp

Secondary title:
14sp

Metadata:
12–13sp

Screen title:
20sp

Navigation:
12sp
```

Video title harus readable dan tidak terlalu bold.

---

# 34. Thumbnail Design

Default:

```text
16:9
```

Border radius:

```text
8dp
```

Jangan menggunakan border berat.

Duration badge:

```text
bottom-right
black translucent background
white text
```

Example:

```text
12:35
```

---

# 35. Avatar

Avatar:

```text
40dp
```

Circular.

Channel avatar pada video card:

```text
36dp
```

---

# 36. Responsive Layout

## Phone Portrait

```text
1-column feed
```

## Phone Landscape

```text
2-column feed
```

Jika screen width cukup besar:

```text
2–3 columns
```

## Tablet

Portrait:

```text
2 columns
```

Landscape:

```text
3–4 columns
```

Gunakan adaptive calculation berdasarkan available width, bukan hardcoded device model.

---

# 37. Tablet Navigation

Untuk tablet landscape:

Bottom navigation dapat diganti dengan:

```text
Navigation rail
```

Layout:

```text
┌──────┬────────────────────────────┐
│ Home │                            │
│ Short│       CONTENT              │
│ Subs │                            │
│ Lib  │                            │
│      │                            │
└──────┴────────────────────────────┘
```

Jangan memaksakan bottom navigation jika screen terlalu lebar.

---

# 38. Loading State

Gunakan skeleton loading.

Contoh:

```text
┌──────────────────────────┐
│                          │
│      thumbnail           │
│                          │
└──────────────────────────┘

██████████████████
██████████
```

Hindari loading spinner besar sebagai satu-satunya loading state.

---

# 39. Empty State

Contoh:

```text
No videos found

Try another search.
```

Actions:

```text
Search again
```

Untuk history:

```text
Your watch history is empty.
```

---

# 40. Error State

Jika API gagal:

```text
Something went wrong.

Check your connection and try again.

[ Retry ]
```

Jangan menampilkan stack trace kepada user.

Developer log tetap harus memiliki detail error.

---

# 41. Offline Behavior

Application harus gracefully menangani:

* No internet
* Timeout
* API unavailable
* Partial response

Jika cached data tersedia:

```text
Showing cached content
```

Jika tidak:

```text
No connection
Retry
```

---

# 42. Network Strategy

Gunakan existing network/API layer.

Jangan membuat API client baru jika existing `MediaServiceCore` dapat digunakan.

Implement:

* Timeout
* Retry
* Pagination
* Continuation
* Cancellation
* Request deduplication

Saat user meninggalkan screen:

```text
cancel unnecessary request
```

---

# 43. Image Loading

Thumbnail harus menggunakan image cache.

Requirement:

* Memory cache
* Disk cache
* Placeholder
* Error image
* Resize berdasarkan target ImageView

Jangan load thumbnail original resolution jika hanya ditampilkan 360dp.

---

# 44. Scroll Performance

Target:

```text
60 FPS
```

Hindari:

* Nested RecyclerView berlebihan
* Heavy layouts
* Blocking main thread
* Synchronous image decoding
* Full feed recreation

Saat pagination:

```text
append data
```

bukan:

```text
reload entire list
```

---

# 45. Back Navigation

Back behavior harus konsisten.

Priority:

1. Close dialog/bottom sheet
2. Exit fullscreen
3. Close keyboard
4. Collapse mini player
5. Navigate previous screen
6. Exit application

Jangan langsung menutup aplikasi jika user sedang berada di nested screen.

---

# 46. Android System Integration

Support:

* Back gesture
* Edge-to-edge
* Status bar
* Navigation bar
* Orientation
* Picture-in-picture
* Media session
* Lock screen controls
* Headset/Bluetooth controls

Media session:

```text
Play
Pause
Next
Previous
Seek
```

Jika applicable.

---

# 47. Accessibility

Minimum:

* Content descriptions
* Touch target >= 48dp
* Text readable
* Sufficient contrast
* TalkBack support
* Keyboard navigation untuk tablet/Chromebook jika possible

---

# 48. Settings

Settings harus mempertahankan fitur SmartTube yang sudah ada.

Organisasi:

```text
Settings

Playback
  Default quality
  Default speed
  Auto play
  Background playback
  Picture-in-picture

SponsorBlock
  Enable
  Categories

Appearance
  Theme
  Dynamic colors
  Thumbnail settings

Player
  Controls
  Gestures
  Double tap seek

Downloads
  Download location
  Default quality

Account
  Account/session

Advanced
  Debug
  Cache
  Reset settings

About
  Version
  Open source licenses
```

---

# 49. Appearance Settings

Theme:

```text
System
Light
Dark
```

Default:

```text
System
```

Dark theme harus menjadi first-class experience.

---

# 50. Account

Account screen:

```text
Profile avatar

Account name

Email

Manage account
Sign out
```

Jika authentication/session existing SmartTube berbeda, jangan merusak mekanisme existing.

---

# 51. UX Rules

## Rule 1

Jangan menggunakan focus-based TV navigation.

Hindari:

```text
D-pad focus
Leanback UI
TV-only navigation
```

untuk phone screens.

## Rule 2

Gunakan touch-first interaction.

## Rule 3

Clickable item minimum:

```text
48dp
```

## Rule 4

Jangan menggunakan hover.

## Rule 5

Jangan membuat user membuka banyak halaman untuk action sederhana.

---

# 52. Architecture Direction

Pertahankan existing architecture sebanyak mungkin.

Target:

```text
smarttubephone
        │
        ├── UI
        ├── Navigation
        ├── Mobile ViewModels/Presenters
        │
        ▼
common
        │
        ├── Media logic
        ├── Player logic
        ├── Preferences
        │
        ▼
MediaServiceCore
        │
        ▼
YouTube / media services
```

UI-specific code untuk phone harus berada di:

```text
smarttubephone/
```

Jangan memasukkan phone-specific UI ke `common` kecuali memang reusable.

---

# 53. Suggested UI Package Structure

Contoh:

```text
smarttubephone/
└── src/main/
    └── java/
        └── app/smarttube/phone/
            ├── ui/
            │   ├── home/
            │   ├── search/
            │   ├── player/
            │   ├── shorts/
            │   ├── subscriptions/
            │   ├── library/
            │   ├── channel/
            │   ├── comments/
            │   ├── settings/
            │   └── common/
            │
            ├── navigation/
            ├── player/
            ├── downloads/
            ├── account/
            └── utils/
```

Jangan memindahkan semua existing code sekaligus.

Refactor secara incremental.

---

# 54. Navigation Routes

Define centralized routes.

Contoh:

```text
home
search
search/{query}
video/{videoId}
channel/{channelId}
playlist/{playlistId}
subscriptions
library
history
watch-later
downloads
settings
```

Jangan menyebarkan string route ke seluruh project.

---

# 55. MVP

MVP wajib memiliki:

### Navigation

* Home
* Search
* Subscriptions
* Library
* Settings

### Home

* Feed
* Categories
* Infinite scrolling
* Pull to refresh

### Search

* Search input
* Search result
* Search suggestions jika tersedia

### Video

* Player
* Play/pause
* Seek
* Fullscreen
* Quality
* Speed
* Subtitle

### Player

* Portrait
* Landscape
* PiP
* Mini player

### Account

* Existing authentication/session support

### SmartTube

* SponsorBlock
* Existing playback functionality

---

# 56. Phase 2

Setelah MVP stabil:

* Shorts
* Comments
* Downloads
* Playlists
* Watch later
* Advanced gestures
* Channel pages
* Better recommendations
* Casting
* Advanced player settings

---

# 57. Phase 3

Optional:

* Offline caching
* Advanced download manager
* Multiple accounts
* Tablet optimized UI
* Android Auto compatibility investigation
* Chromebook optimization
* TV + phone shared settings

---

# 58. UI Priority

Prioritas implementasi:

```text
P0
Home
Video player
Search
Navigation

P1
Subscriptions
Library
Channel
Mini player
PiP

P2
Comments
Shorts
Downloads
Playlists

P3
Advanced customization
Tablet optimization
Additional integrations
```

---

# 59. Design Reference

UI harus mengambil **inspirasi interaction pattern dari YouTube Android**, tetapi jangan menyalin asset proprietary, logo, source code, atau elemen branding secara ilegal.

Yang ditiru adalah:

* Information hierarchy
* Navigation pattern
* Mobile interaction pattern
* Layout conventions
* Familiar user expectations

TubeTube tetap menggunakan:

```text
TubeTube branding
TubeTube icon
TubeTube colors
```

Jangan menggunakan logo YouTube sebagai logo aplikasi.

---

# 60. Visual Direction

Target visual:

```text
Modern
Clean
Dark
Content-first
Minimal
Fast
Familiar
```

Avoid:

```text
TV UI
Oversized cards
Huge text
Excessive shadows
Excessive gradients
Desktop UI
Dashboard-like UI
```

---

# 61. Important Technical Rule

Jangan rewrite seluruh SmartTube hanya untuk mendapatkan UI baru.

Sebelum membuat implementation baru:

1. Cari existing implementation.
2. Tentukan apakah dapat digunakan kembali.
3. Pisahkan business logic dari UI.
4. Reuse existing presenter/service.
5. Baru buat mobile UI.

Contoh:

Jika existing:

```text
ExoPlayerController
```

sudah bekerja:

```text
JANGAN membuat PlayerController baru.
```

Buat adapter/UI mobile di atasnya.

---

# 62. Migration Strategy

Implement secara bertahap.

## Step 1

Pastikan:

```text
smarttubephone
```

dapat build dan launch.

## Step 2

Buat mobile shell:

```text
MainActivity
BottomNavigation
Navigation
```

## Step 3

Migrasikan Home.

## Step 4

Migrasikan Search.

## Step 5

Migrasikan Video Player.

## Step 6

Migrasikan Subscriptions.

## Step 7

Migrasikan Library.

## Step 8

Migrasikan Settings.

## Step 9

Implement mini player.

## Step 10

Implement responsive/tablet.

---

# 63. Acceptance Criteria

## Home

* [ ] Feed dapat ditampilkan
* [ ] Thumbnail tampil dengan benar
* [ ] Title maksimal 2 lines
* [ ] Metadata tampil
* [ ] Infinite scroll berjalan
* [ ] Pull-to-refresh berjalan
* [ ] Loading state tersedia
* [ ] Error state tersedia

## Navigation

* [ ] Bottom navigation bekerja
* [ ] Back navigation konsisten
* [ ] Navigation state dipertahankan
* [ ] Tab tidak membuat duplicate screen

## Search

* [ ] Search input bekerja
* [ ] Keyboard muncul otomatis
* [ ] Search result tampil
* [ ] Search dapat dibatalkan
* [ ] Pagination bekerja

## Player

* [ ] Video dapat diputar
* [ ] Play/pause bekerja
* [ ] Seek bekerja
* [ ] Fullscreen bekerja
* [ ] Landscape bekerja
* [ ] Picture-in-picture bekerja
* [ ] Mini player bekerja
* [ ] Playback speed bekerja
* [ ] Quality selector bekerja
* [ ] Subtitle bekerja jika tersedia

## Performance

* [ ] Scrolling smooth
* [ ] Tidak ada blocking operation pada main thread
* [ ] Image cache bekerja
* [ ] Pagination tidak reload seluruh feed
* [ ] Memory usage reasonable

## Responsive

* [ ] Portrait phone
* [ ] Landscape phone
* [ ] Portrait tablet
* [ ] Landscape tablet

---

# 64. Definition of Done

Sebuah screen dianggap selesai apabila:

* UI mobile-first
* Touch interaction bekerja
* Loading state tersedia
* Error state tersedia
* Empty state tersedia
* Dark mode bekerja
* Back navigation bekerja
* Rotation tidak menyebabkan crash
* State tidak hilang secara tidak perlu
* Tidak ada hardcoded device dimensions
* Tidak ada blocking network operation di main thread
* Existing SmartTube functionality tidak rusak

---

# 65. Cursor Development Rules

Saat mengimplementasikan PRD ini dengan Cursor:

### Rule 1

**Jangan melakukan massive rewrite.**

### Rule 2

Sebelum mengubah file:

```text
Inspect existing implementation first.
```

### Rule 3

Reuse existing:

```text
MediaServiceCore
common
ExoPlayerController
Presenter
Preferences
```

sebisa mungkin.

### Rule 4

Phone UI harus berada pada module:

```text
smarttubephone
```

kecuali shared logic.

### Rule 5

Jangan menghapus functionality SmartTube hanya karena belum digunakan oleh UI.

### Rule 6

Jangan membuat mock data jika API existing dapat digunakan.

### Rule 7

Jangan membuat fake loading/content untuk menggantikan API.

### Rule 8

Jangan menggunakan desktop/TV layout pada phone.

### Rule 9

Jangan menggunakan TV focus navigation sebagai primary interaction.

### Rule 10

Semua clickable UI harus nyaman disentuh.

Minimum touch target:

```text
48dp
```

---

# 66. Final Product Experience

Ketika user membuka TubeTube Phone, experience yang diharapkan:

```text
                    TubeTube

┌─────────────────────────────────────┐
│ TubeTube       🔍        👤         │
├─────────────────────────────────────┤
│ All  Music  Gaming  News  Live ... │
├─────────────────────────────────────┤
│                                     │
│ ┌─────────────────────────────────┐ │
│ │                                 │ │
│ │          VIDEO THUMBNAIL        │ │
│ │                                 │ │
│ └─────────────────────────────────┘ │
│                                     │
│ ○  Amazing Video Title              │
│    Channel Name                     │
│    1.2M views • 2 days ago      ⋮   │
│                                     │
│ ┌─────────────────────────────────┐ │
│ │                                 │ │
│ │          VIDEO THUMBNAIL        │ │
│ │                                 │ │
│ └─────────────────────────────────┘ │
│                                     │
│ ○  Another Video Title              │
│    Channel Name                     │
│    500K views • 1 day ago       ⋮   │
│                                     │
├─────────────────────────────────────┤
│ Home  Shorts  +  Subs  Library     │
└─────────────────────────────────────┘
```

Saat video dibuka:

```text
┌─────────────────────────────────────┐
│                                     │
│             VIDEO PLAYER             │
│                                     │
│        ▶              ⚙             │
│                                     │
├─────────────────────────────────────┤
│ Video title                         │
│ Channel Name                        │
│                                     │
│ 👍   👎   Share   Download          │
│                                     │
│ Description                         │
│                                     │
│ Comments                            │
│                                     │
│ Recommended                         │
└─────────────────────────────────────┘
```

Goal akhirnya:

> **User merasa seperti sedang menggunakan aplikasi YouTube Android modern, tetapi seluruh playback/media capabilities dari SmartTube tetap tersedia di belakang layar.**

---

# 67. Development Principle

**Do not optimize for “making SmartTube run on a phone”.**

Optimize for:

> **“Making SmartTube functionality feel native on a phone.”**

Ini adalah prinsip utama seluruh development TubeTube Phone.
