# Patna 2047 — living development roadmap

Last updated: 2026-11-11

Patna 2047 is being developed as a mobile-first, third-person Indian city exploration and driving game. Work is delivered in small reviewable layers; this roadmap is intentionally maintained as the prototype grows.

## Current playable foundation

- Fictional near-future Patna riverfront with Ganga water, ghats, a bridge/road network and a Golghar-inspired landmark.
- On-foot movement now accelerates and brakes through a frame-rate-independent planar velocity ramp (4.8 m/s² acceleration, 6.5 m/s² braking); walk animation speed follows actual movement speed. Direction changes decelerate before reversing. Regression coverage for acceleration, braking, reversal and invalid velocity inputs passed in CI run #57 (65 JVM tests total). The 360-degree camera orbit/pitch, SUV driving and enter/exit interaction remain in place; player and pedestrian facing ease along the shortest yaw arc.
- Four distinct rigged human GLBs (Aarav, Meera, bank employee and market farmer), plus two worker routes and two moving traffic vehicles.
- Character scale is calculated from each loaded model's vertical AABB against adult stature targets. The full transformed AABB foot anchor (X/Y/Z) is now preserved and re-rotated during player, Meera and worker movement. Debug builds emit `Patna2047Scale` records with native height, scale and static world-Y bounds; publisher pages list source heights of 1.80 m, 1.70 m, 1.80 m and 1.90 m respectively. Actual device-side visible-height and animated-skeleton verification is still pending.
- CI builds a Debug APK, runs JVM unit tests and Android Lint on pull requests. Latest gameplay revision passed all three plus APK verification/upload in Actions run #57; extracted APK is 52,728,016 bytes (SHA-256 `49a6df8635945ea6e71e0117e7b6b25995fcaa9fdc8acde3c5010692e31969e5`). Aarav/Meera remain the required on-foot actors; their validated GLBs are cached in a pinned app-cache folder. The SUV now loads after on-foot controls are live, and a missing SUV degrades safely to walking. Optional city models stream in with visible progress and a bounded 72 MB LRU cache.

## In progress

1. **Human proportions and NPC routines** — PR #2 measures each loaded character's vertical bounds for scaling, separates bank/market routes, and keeps the full off-center model bottom anchor aligned as characters move and turn. JVM tests now cover rotated foot-anchor reconstruction. Still needs real-device or emulator visual verification, especially visible stature beside the SUV/buildings and animated skeleton bounds.
2. **Save/resume foundation** — implemented on PR #2 with Preferences DataStore: player/vehicle location, camera orientation, driving mode and the first story flag are saved locally on a five-second cadence, app stop, vehicle transitions and mission completion. Unit tests and Debug APK build passed on Actions run #49; real-device process-death/relaunch verification is still pending. This is not cloud sync or a multi-slot system.
3. **Progressive world-asset loading** — Aarav and Meera are resolved from a pinned app-cache folder (which Android may still clear) or downloaded with a 12 s connect / 30 s read timeout and GLB v2 validation. Optional models use a 72 MB LRU cache; incomplete downloads are written to `.part` files and never reused. The player SUV now loads after walking controls become live, and saved driving mode is restored only when the SUV is ready. The 17 city assets still stream sequentially. Cold-start offline fallback, cache eviction on low storage and device memory/time-to-play still need real-device profiling.

## Prioritized next milestones

1. **Character and camera validation:** inspect the new `Patna2047Scale` debug logs, compare measured static bounds to the SUV and street furniture, then verify screenshots on a real Android device or emulator. The current static AABB does not prove animated-skeleton extremes; capture deformed bounds across the walk cycle before declaring scale verified. Add true idle/work animation clips only after confirming the actual GLB clip names.
2. **Movement and collision polish:** shortest-arc character turning is frame-rate limited (player 540°/s, pedestrians 360°/s). On-foot planar acceleration (4.8 m/s²), braking (6.5 m/s²), direction reversal and velocity-linked walk animation are now covered by JVM tests and passed in Actions run #57 (65 tests total). Next improve pedestrian yielding/avoidance and validate vehicle exit points on device.
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
- Prefer offline-capable, low-size assets with verified permissive licences; document exact source and attribution in ASSET_CREDITS.md.
- Do not merge, release, deploy or change production settings without explicit approval.
- State clearly when emulator/device visual QA was not available; never substitute code inspection for a visual test.
