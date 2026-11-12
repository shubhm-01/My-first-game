# Patna 2047 — living development roadmap

Last updated: 2026-11-12

Patna 2047 is being developed as a mobile-first, third-person Indian city exploration and driving game. Work is delivered in small reviewable layers; this roadmap is intentionally maintained as the prototype grows.

## Current playable foundation

- Fictional near-future Patna riverfront with Ganga water, ghats, a bridge/road network and a Golghar-inspired landmark.
- On-foot movement now accelerates and brakes through a frame-rate-independent planar velocity ramp (4.8 m/s² acceleration, 6.5 m/s² braking); walk animation speed follows actual movement speed. Direction changes decelerate before reversing. Regression coverage for acceleration, braking, reversal and invalid velocity inputs passed in CI run #57 (65 JVM tests total). The 360-degree camera orbit/pitch, SUV driving and enter/exit interaction remain in place; player and pedestrian facing ease along the shortest yaw arc.
- Four distinct rigged human GLBs (Aarav, Meera, bank employee and market farmer), plus two worker routes and two moving traffic vehicles.
- Meera's daily route now passes the market frontage and Ganga promenade without crossing the market stall, riverside home or promenade bench. All three pedestrian loops are checked against the player collision AABBs with a conservative 0.45 m body-clearance margin.
- Meera, the bank employee and the market farmer now prefer a clear waypoint step, sidestep around the player/other pedestrians/traffic when a safe lateral step exists, reject moves into static world colliders, and pause their walk cycle when every candidate is blocked. Five focused JVM tests cover direct movement, sidestepping, yielding, static obstacles and invalid inputs; Actions run #61 passed the complete unit-test suite and Android Lint.
- Meera, the bank employee and the market farmer now use a local route-step avoidance check: they prefer the direct waypoint path, try shallow/lateral sidesteps around the player, other pedestrians and traffic, reject steps through static colliders, and pause their walk cycle when no safe step exists. Five JVM tests cover clear paths, yielding, sidestepping, static obstacles and invalid data; CI verification is pending.
- Character scale is calculated from each loaded model's vertical AABB against adult stature targets. The full transformed AABB foot anchor (X/Y/Z) is now preserved and re-rotated during player, Meera and worker movement. Debug builds emit `Patna2047Scale` records with native height, scale and static world-Y bounds; publisher pages list source heights of 1.80 m, 1.70 m, 1.80 m and 1.90 m respectively. Actual device-side visible-height and animated-skeleton verification is still pending.
- CI builds a Debug APK, runs JVM unit tests and Android Lint on pull requests. Latest gameplay revision passed all three plus APK verification/upload in Actions run #57; extracted APK is 52,728,016 bytes (SHA-256 `49a6df8635945ea6e71e0117e7b6b25995fcaa9fdc8acde3c5010692e31969e5`). Aarav/Meera remain the required on-foot actors; their validated GLBs are cached in a pinned app-cache folder. The SUV now loads after on-foot controls are live, and a missing SUV degrades safely to walking. Optional city models stream in with visible progress and a bounded 72 MB LRU cache.

## In progress

1. **Human proportions and NPC routines** — PR #2 measures each loaded character's vertical bounds for scaling, separates bank/market routes, and keeps the full off-center model bottom anchor aligned as characters move and turn. JVM tests now cover rotated foot-anchor reconstruction. The daily pedestrian loops now also pass a geometry regression test against the static world collision map, including the market stall and Ganga promenade bench. Still needs real-device or emulator visual verification, especially visible stature beside the SUV/buildings and animated skeleton bounds.
2. **Save/resume foundation** — implemented on PR #2 with Preferences DataStore: player/vehicle location, camera orientation, driving mode and the first story flag are saved locally on a five-second cadence, app stop, vehicle transitions and mission completion. Unit tests and Debug APK build passed on Actions run #49; real-device process-death/relaunch verification is still pending. This is not cloud sync or a multi-slot system.
3. **Progressive world-asset loading** — Aarav and Meera are resolved from a pinned app-cache folder (which Android may still clear) or downloaded with a 12 s connect / 30 s read timeout and GLB v2 validation. Optional models use a 72 MB LRU cache; incomplete downloads are written to `.part` files and never reused. The player SUV now loads after walking controls become live, and saved driving mode is restored only when the SUV is ready. The 17 city assets still stream sequentially. Cold-start offline fallback, cache eviction on low storage and device memory/time-to-play still need real-device profiling.

## Prioritized next milestones

1. **Character and camera validation:** inspect the new `Patna2047Scale` debug logs, compare measured static bounds to the SUV and street furniture, then verify screenshots on a real Android device or emulator. The current static AABB does not prove animated-skeleton extremes; capture deformed bounds across the walk cycle before declaring scale verified. Add true idle/work animation clips only after confirming the actual GLB clip names.
2. **Movement and collision polish:** shortest-arc character turning is frame-rate limited (player 540°/s, pedestrians 360°/s). On-foot planar acceleration (4.8 m/s²), braking (6.5 m/s²), direction reversal and velocity-linked walk animation are covered by JVM tests. The first local pedestrian-yielding/sidestep layer now checks the player, other pedestrians, traffic and static colliders before moving; Actions run #61 passed the new avoidance tests, full JVM suite and Android Lint. Real-device validation remains pending; next improve vehicle exit points and broader traffic awareness.
3. **Vehicle expansion:** introduce one licensed motorcycle asset, then add a selectable bike, steering/braking, wheel rotation and reliable enter/exit. Keep the SUV as the regression baseline.
4. **Living market block:** add a small coherent market lane with distinct shopfronts, stalls, signboards and a few role-specific NPC routines before expanding the map footprint.
5. **Reliable asset delivery:** the core character cache and bounded optional-model LRU are implemented. Next validate warm offline launches and low-storage eviction on devices, then add a locally generated low-poly character fallback so a first-ever offline install can still move around the city. Evaluate texture/mesh LODs that preserve rigs and add cancellation/progress for long downloads.
6. **World depth:** add interiors and meaningful POIs, mission progression, day/night and weather in separate testable increments.
7. **Mobile performance:** measure frame time, memory and thermal behaviour; use distance-based visibility/LOD and graphics presets before adding dense crowds or large districts.

## Quality gates for every gameplay PR

- Preserve the existing Alchemist source content and keep Patna 2047 changes on a dedicated branch/PR.
- Run ./gradlew testDebugUnitTest, ./gradlew lintDebug and ./gradlew assembleDebug; verify the APK exists and report its size.
- Validate cached GLB magic/version/length, atomic `.part` handling, cache budget and offline fallback state transitions.
- Check collision, touch controls, camera orbit/pitch, human grounding, vehicle enter/exit and loading regressions.
- Keep every autonomous pedestrian waypoint loop outside static collision bounds with at least the configured body-clearance margin.
- Prefer offline-capable, low-size assets with verified permissive licences; document exact source and attribution in ASSET_CREDITS.md.
- Do not merge, release, deploy or change production settings without explicit approval.
- State clearly when emulator/device visual QA was not available; never substitute code inspection for a visual test.
- Keep GitHub Actions on supported Node 24 action majors; checkout, Java setup and artifact upload were migrated to actions/checkout@v7, actions/setup-java@v6 and actions/upload-artifact@v7 in this iteration. The migration's CI run is pending.


## Latest verification — 2026-11-12 (Actions run #61)

- Pedestrian yielding/avoidance commit `f43a6fe828b2873c4da8a6a2f56cee3755c64f7a` passed `testDebugUnitTest`, `lintDebug`, `assembleDebug`, APK existence verification and artifact upload.
- Extracted Debug APK: **52,744,400 bytes**; SHA-256 `ea8dd1b06a803352973e92c03d0d0144f45ac54969c1600db1d177247c56e649`.
- Run #61 emitted GitHub Actions Node 20 / `setup-java@v4` deprecation warnings. The workflow action majors have now been updated to Node 24-compatible releases; run #62 must verify that workflow-only migration.
- No Android emulator or physical-device visual QA was available. Pedestrian avoidance behaviour is unit-tested, not yet visually validated in gameplay.
- No new third-party game assets were imported.
