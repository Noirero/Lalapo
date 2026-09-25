# Lalapo v2 UI/UX Specification — Frozen Baseline

Date: 25 September 2026  
Product principle: **Simple by default, powerful when needed.**  
Visual language: **Modern-simple**, selective Material 3 Expressive, dark/AMOLED first-class.

## Navigation shell

Primary destinations are **Library / Discover / Activity / More**. Phone uses a standard bottom navigation. Tablet keeps adaptive navigation rail/two-pane behavior. Reselect/long-press may remain as power-user shortcuts, but no important feature may depend on them.

Navigation state must remain explicit. Deep links and shortcuts must map to a visible destination and preserve intended sub-state. Extension manager stays inside Discover and does not hide primary navigation merely because administration UI is open.

## Library

Purpose: choose and continue watching.

Primary actions: search, filter, continue/play. Secondary actions: refresh, random, category and selection actions. Continue Watching is the preferred resume surface when present and must disappear cleanly when empty.

Keep current library engine, grouping, sorting, filtering, layouts, category selection and bulk actions. Filter/Sort/Layout/Group should converge into one scannable sheet. Empty/loading/error states and phone/tablet behavior must remain supported.

## Discover

Purpose: find anime and enter content sources.

Search is the dominant action. Sources remain the content-discovery surface; extensions are administration reachable via a clear action with update badge. Migration stays visible through tools/overflow rather than a hidden gesture. Pinned and recently used sources should receive higher visual priority when presentation work reaches that phase.

## Activity

Purpose: show what already happened or is newly happening.

Visible hierarchy is **History / Updates / Downloads**, with **History as default**. Upcoming remains an Updates action. Downloads must have a visible route to queue/progress controls. Resume gestures may remain shortcuts, but Continue Watching in Library remains the primary resume surface.

## More

Purpose: utilities and contextual status, not a permanent settings dashboard.

Primary items: **Private Session, Offline Mode, Downloads, Sync & Backup, Statistics, Settings, About**. Download/sync/storage/extension notices should only become prominent when relevant.

Private Session is the presentation name for the existing incognito/privacy behavior. Offline Mode presents downloaded-only/local behavior as an explicit user mode.

## Anime detail

Primary CTA is Continue/Resume/Play. Season and episode list move ahead of long metadata. Secondary row is Library / Track / Share / More. Edit, migration, notes, WebView and uncommon source tools move to secondary/overflow surfaces. Related/About/Notes/Tracking details remain available below the viewing flow.

## Player

Keep mpv and all technical capability. Basic overlay shows back/title, previous/play/next, seekbar/time. Quick controls are contextual. Player settings are grouped into Playback / Video / Timing / Tools / Advanced. Advanced keeps mpv config, decoder/GPU, torrent, scripts and custom controls.

## Settings

Keep search and tablet two-pane. Target grouping: App / Watching / Services / System / About. Network gets its own place. Tracking and Sync/Backup remain services. Advanced remains power-user territory. Theme presentation is curated first, with the complete theme engine retained behind More Themes.

## State and QA contract

Every touched screen must define loading, empty, error and offline behavior where applicable. Selection/search/filter state should survive rotation/navigation when users reasonably expect it. Each implementation phase must keep the build green and preserve source, playback, library, downloads, tracking and backup behavior.

This file is the frozen baseline for the Lalapo v2 presentation redesign. Domain/data/player/source engines are not to be rewritten unless a concrete implementation requirement proves it necessary.
