# Patna 2047 — living development roadmap

Last updated: 2026-11-10

Patna 2047 is being developed as a mobile-first, third-person Indian city exploration and driving game. Work is delivered in small reviewable layers; this roadmap is intentionally maintained as the prototype grows.

## Current playable foundation

- Fictional near-future Patna riverfront with Ganga water, ghats, a bridge/road network and a Golghar-inspired landmark.
- On-foot movement, 360-degree camera orbit/pitch, SUV driving and enter/exit interaction.
- Four distinct rigged human GLBs (Aarav, Meera, bank employee and market farmer), plus two worker routes and two moving traffic vehicles.
- Character scale is calculated from each loaded model's vertical AABB against adult stature targets. The full transformed AABB foot anchor (X/Y/Z) is now preserved and re-rotated during player, Meera and worker movement; actual device-side visible-height verification is still pending.
- CI builds a Debug APK and runs JVM unit tests on pull requests.

## In progress

1. **Human proportions and NPC routines** — PR #2 measures each loaded character's vertical bounds for scaling, separates bank/market routes, and keeps the full off-center model bottom anchor aligned as characters move and turn. JVM tests now cover rotated foot-anchor reconstruction. Still needs real-device or emulator visual verification, especially visible stature beside the SUV/buildings and animated skeleton bounds.
2. **Save/resume foundation** — implemented on PR #2 with Preferences DataStore: player/vehicle location, camera orientation, driving mode and the first story flag are saved locally on a five-second cadence, app stop, vehicle transitions and mission completion. CI and device relaunch verification are pending; this is not cloud sync or a multi-slot system.

## Prioritized next milestones

1. **Character and camera validation:** add a development-only world measurement overlay/logging, compare model bounds to the SUV and street furniture, then verify screenshots on a real Android device or emulator. The current static AABB does not prove animated-skeleton extremes; measure those at runtime or with a GLB inspection tool before declaring scale verified. Add true idle/work animation clips only after confirming the actual GLB clip names.
2. **Movement and collision polish:** smooth acceleration/deceleration on foot, improve pedestrian avoidance and ensure vehicle exit points are always clear of walls/traffic.
3. **Vehicle expansion:** introduce one licensed motorcycle asset, then add a selectable bike, steering/braking, wheel rotation and reliable enter/exit. Keep the SUV as the regression baseline.
4. **Living market block:** add a small coherent market lane with distinct shopfronts, stalls, signboards and a few role-specific NPC routines before expanding the map footprint.
5. **Reliable asset delivery:** stage/cancel remote GLB loads, cache approved assets locally, show per-asset fallback states and keep first playable geometry independent of network availability.
6. **World depth:** add interiors and meaningful POIs, mission progression, day/night and weather in separate testable increments.
7. **Mobile performance:** measure frame time, memory and thermal behaviour; use distance-based visibility/LOD and graphics presets before adding dense crowds or large districts.

## Quality gates for every gameplay PR

- Preserve the existing Alchemist source content and keep Patna 2047 changes on a dedicated branch/PR.
- Run ./gradlew testDebugUnitTest and ./gradlew assembleDebug; verify the APK exists and report its size.
- Check collision, touch controls, camera orbit/pitch, human grounding, vehicle enter/exit and loading regressions.
- Prefer offline-capable, low-size assets with verified permissive licences; document exact source and attribution in ASSET_CREDITS.md.
- Do not merge, release, deploy or change production settings without explicit approval.
- State clearly when emulator/device visual QA was not available; never substitute code inspection for a visual test.
