# PRD.md — Product Requirement Document (Déngé)

## Purpose

This document defines **what** Déngé is, **who** its target users are, and **what features** it provides. It acts as the product-level single source of truth. Technical architecture decisions are detailed in `Architecture.md`; visual design principles and color tokens are outlined in `Design.md`.

---

## 1. Product Summary

### 1.1 Vision
**Déngé** is a native, lightweight, and completely ad-free Android music streaming client. Déngé delivers a premium listening experience powered by high-quality YouTube Music audio streams with a warm *Brew & Bean Coffee* theme, seamless background playback, modern status bar integration, and absolute privacy without requiring an account, server, or subscription.

### 1.2 Target Users
| Attribute | Value |
|:---|:---|
| Primary Audience | Daily music lovers, students, and commuters |
| Platform | Android 12 to Android 16 (API 31 - 36) |
| Context | Commute, deep focus/study sessions, relaxation |
| Key Motivations | Ad-free listening across a massive catalog, minimal RAM/battery impact |

### 1.3 Goals & Success Metrics
- **M1 — 100% Ad-Free**: Pure audio streaming without video delays, banner popups, or audio ad interruptions.
- **M2 — Efficiency**: Significantly lower memory and battery usage compared to browser tabs or webview wrappers.
- **M3 — Background Playback & Island**: Reliable audio continuity when the screen turns off or while using other apps, integrated with media notifications and status bar islands.
- **M4 — Premium UX**: Cozy Brew & Bean aesthetic, fluid animations, and intuitive navigation.

---

## 2. Core Features

### 2.1 Home Screen
- **Personalized Greeting**: Dynamic time-of-day greeting with customizable user profile (e.g., "Hello, Asla ☕").
- **Your Top Plays**: Prominently highlights your top 2 most frequently played tracks with a real-time play counter (*"Played X times this month"*), automatically resetting on the 1st of every month.
- **Recently Played**: Horizontal carousel of recently played tracks for instant resumption.
- **Curated Genres**: Customizable genre shelves on Home that users can adjust anytime via Settings.

### 2.2 Now Playing Screen
- **HD Album Art & Preview**: Crisp album artwork with an interactive HD preview dialog on tap.
- **Quick Share**: One-tap share button inside the artwork preview that instantly copies the official track link to the clipboard.
- **Playback Controls**: Play, Pause, Next, Previous, Shuffle, Repeat (Off, All, One), and an accurate scrub slider.
- **Up Next & Radio Queue**: Full queue management with drag-to-reorder and automated radio recommendations based on current listening.

### 2.3 Library Screen
- **Liked Songs**: One-tap local bookmarking with the heart (♥) button.
- **Custom Playlists**: Create, name, edit, and delete local playlists seamlessly.
- **Listening History**: Chronological playback history log.

### 2.4 Search Screen
- **Debounced Live Search**: Rapid, responsive search across YouTube Music with instant track results.
- **Track Options Menu (⋮)**: Instant actions for "Play Now", "Play Next", "Add to Queue", and "View HD Artwork".

### 2.5 Settings Screen
- **Interactive Preferences**:
  - **Manage Home Genres**: Select up to 5 favorite music genres or add custom search keywords.
  - **Equalizer Presets**: Sound adjustments with instant presets (Bass Boost, Vocal, Rock, Flat, etc.).
- **Profile & Customization**:
  - **Edit Profile Name**: Local display name dialog with zero Google login requirement.
  - **Theme Info**: Brew & Bean Coffee style aesthetic.
  - **Audio Quality**: High Quality Opus Audio Stream indicator.
  - **App Version**: Current release info.

---

## 3. User Flows

### 3.1 First Launch Flow
1. User launches Déngé.
2. The app is immediately ready to play music (no splash screen delays or login walls).
3. The default profile name is safely initialized ("Music Lover"), customizable anytime in Settings.

### 3.2 Playback & Queue Management Flow
1. User taps a track from Home, Library, or Search.
2. If selecting the three-dots menu (⋮):
   - **Play Next**: Inserts the track right after the current song.
   - **Add to Queue**: Appends the track to the end of the active queue.
3. Media Foreground Service activates with lock screen controls and system notification support.

### 3.3 Share Track Flow
1. On the Now Playing screen, user taps the album cover to open HD Preview.
2. User taps **Share**.
3. The official track link is copied to the clipboard with a confirmation toast, ready to paste into chat or social apps.

---

## 4. Security & Compliance
- **Zero Server Footprint**: No user data is stored on remote servers.
- **Privacy-First**: No access requested to contacts, camera, or personal files.
- **Fair-Use & Educational**: Built as an alternative media client utilizing public client endpoints.
