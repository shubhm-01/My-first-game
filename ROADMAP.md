# Patna 2047 — living development roadmap

Last updated: 2026-11-13

Patna 2047 is being developed as a mobile-first, third-person Indian city exploration and driving game. Work is delivered in small reviewable layers; this roadmap is intentionally maintained as the prototype grows.

## Current playable foundation

- Fictional near-future Patna riverfront with Ganga water, ghats, a bridge/road network and a Golghar-inspired landmark.
- On-foot movement now accelerates and brakes through a frame-rate-independent planar velocity ramp (4.8 m/s² acceleration, 6.5 m/s² braking); walk animation speed follows actual movement speed. Direction changes decelerate before reversing. Regression coverage for acceleration, braking, reversal and invalid velocity inputs passed in CI run #57 (65 JVM tests total). The 360-degree camera orbit/pitch, SUV driving and enter/exit interaction remain in place; player and pedestrian facing ease along the shortest yaw arc.
- Four distinct rigged human GLBs (Aarav, Meera, bank employee and market farmer), plus two worker routes and two moving traffic vehicles.
- Meera's daily route now passes the market frontage and Ganga promenade without crossing the market stall, riverside home or promenade bench. All three pedestrian loops are checked against the player collision AABBs with a conservative 0.45 m body-clearance margin.
- Meera, the bank employee and the market farmer now prefer a clear waypoint step, sidestep around the player/other pedestrians/traffic when a safe lateral step exists, reject moves into static world colliders, and pause their walk cycle when every candidate is blocked. Five focused JVM tests cover direct movement, sidestepping, yielding, static obstacles and invalid inputs; Actions run #61 passed the complete unit-test suite and Android Lint.
- Pedestrian yielding/sidestep behaviour is implemented and test-covered; Actions run #61 passed its five focused cases plus Android Lint, and run #62 verified the refreshed CI actions. Real-device crowd behaviour remains pending.
- Character scale now prefers the union of each Filament renderable's object-space culling AABB after applying the full child-node hierarchy transform into ModelNode-root space. This replaces the older ModelNode accessor-level AABB shortcut for sizing and foot anchoring; off-centre child pivots/transforms are included. Adult targets remain 1.72 m / 1.64 m / 1.76 m / 1.68 m. Debug logs identify whether hierarchy bounds or the fail-safe GLB accessor fallback was used. The culling boxes are conservative static envelopes; deformed skeleton extremes and visible stature still require device-side capture.
- CI builds a Debug APK, runs JVM unit tests and Android Lint on pull requests. Latest gameplay revision passed all three plus APK verification/upload in Actions run #57; extracted APK is 52,728,016 bytes (SHA-256 `49a6df8635945ea6e71e0117e7b6b25995fcaa9fdc8acde3c5010692e31969e5`). Aarav/Meera remain the required on-foot actors; their validated GLBs are cached in a pinned app-cache folder. The SUV now loads after on-foot controls are live, and a missing SUV degrades safely to walking. Optional city models stream in with visible progress and a bounded 72 MB LRU cache.

## In progress

1. **Human proportions and NPC routines** — PR #2 measures each loaded character's vertical bounds for scaling, separates bank/market routes, and keeps the full off-center model bottom anchor aligned as characters move and turn. JVM tests now cover rotated foot-anchor reconstruction. The daily pedestrian loops now also pass a geometry regression test against the static world collision map, including the market stall and Ganga promenade bench. Still needs real-device or emulator visual verification, especially visible stature beside the SUV/buildings and animated skeleton bounds.
2. **Save/resume foundation** — implemented on PR #2 with Preferences DataStore: player/vehicle location, camera orientation, driving mode and the first story flag are saved locally on a five-second cadence, app stop, vehicle transitions and mission completion. Unit tests and Debug APK build passed on Actions run #49; real-device process-death/relaunch verification is still pending. This is not cloud sync or a multi-slot system.
3. **Progressive world-asset loading** — Aarav and Meera are resolved from a pinned app-cache folder (which Android may still clear) or downloaded with a 12 s connect / 30 s read timeout and GLB v2 validation. Optional models use a 72 MB LRU cache; incomplete downloads are written to `.part` files and never reused. The player SUV now loads after walking controls become live, and saved driving mode is restored only when the SUV is ready. The 17 city assets still stream sequentially. Cold-start offline fallback, cache eviction on low storage and device memory/time-to-play still need real-device profiling.

## Latest update — 2026-11-12: hierarchy-aware human bounds and camera framing

- Replaced human sizing/grounding's asset-level ModelNode AABB shortcut with a union of each Filament renderable's object-space AABB transformed through its complete node-parent hierarchy into ModelNode-root coordinates. The same union drives uniform adult-height scaling and the bottom-centre foot anchor. If no valid renderable box exists, the previous accessor AABB remains a logged fail-safe.
- Third-person on-foot framing now aims at 1.25 m (torso level), clamps saved/dragged targets to 1.0–2.0 m, reduces vertical-drag sensitivity from 0.034 to 0.009 world metres per screen pixel, and increases on-foot follow distance from 6.0 to 6.7 m. Old saves at 0.6 m or 2.8 m are clamped into the new human framing range.
- Added transformed-eight-corner AABB and camera-save migration regression coverage. This fixes the known source-level framing/bounds logic; device screenshots and deformed skeleton measurements remain pending.
- Previous workflow migration verification: Actions run #62 passed all unit tests, Android Lint, Debug APK assembly, APK existence verification and artifact upload.

## Latest update — 2026-11-12: skinned GLB rest-pose bounds

- Added a lightweight cached-GLB rest-pose bounds reader for skinned character meshes. It applies each skin's weighted `world(joint) × inverseBindMatrix` transform to POSITION vertices before measuring the model, and supports a conservative joint-transformed accessor-box fallback when influences are unavailable.
- Character sizing/grounding now prefers this skin-aware rest-pose box, then the transformed renderable hierarchy box, then the original accessor box. The calculation runs on `Dispatchers.Default` before SceneView 2.2.1 releases source data. Non-skinned city assets and their existing cube sizing are unchanged.
- Added a regression rig matching the upstream centimetre-export pattern: 189-unit mesh vertices under an armature scaled by 0.01 must measure 1.89 m after skinning. Added unreadable-influence fallback and invalid-GLB tests.
- This handles rest-pose skin transforms, not every animation clip or morph target. Device-side stature, foot-contact and camera-framing screenshots remain mandatory before considering the reported oversized-human issue visually resolved.

## Latest update — 2026-11-13: glTF default-scene bounds isolation

- The skin-aware rest-pose parser now measures only the glTF scene selected by the top-level `scene` property, matching Filament's documented default-scene-only loading behavior. It no longer unions alternate/editor scenes into a character's native height.
- When a GLB contains multiple scenes but declares no default scene, the parser declines to guess and returns to the already-measured live renderable-hierarchy fallback. A single-scene file without an explicit index remains unambiguous.
- Added synthetic GLB regression coverage with an alternate scene containing a static mesh translated 500 m upward; the declared default character scene still measures 1.89 m. Added a second test proving ambiguous multi-scene files fail closed.
- This is a parser correctness improvement, not proof that the four existing Grab3D downloads contain alternate scenes. Their actual scene metadata and on-device animated bounds still need capture.

## Prioritized next milestones

- **Day/night foundation:** a session-local 24-minute clock now drives the sun and three warm street lamps; next persist the clock and validate exposure, readability and frame time on a real Android device.

1. **Character and camera validation:** inspect the new `Patna2047Scale` debug logs, compare measured static bounds to the SUV and street furniture, then verify screenshots on a real Android device or emulator. The current static AABB does not prove animated-skeleton extremes; capture deformed bounds across the walk cycle before declaring scale verified. Add true idle/work animation clips only after confirming the actual GLB clip names.
2. **Movement and collision polish:** shortest-arc character turning is frame-rate limited (player 540°/s, pedestrians 360°/s). On-foot planar acceleration (4.8 m/s²), braking (6.5 m/s²), direction reversal and velocity-linked walk animation are covered by JVM tests. The first local pedestrian-yielding/sidestep layer now checks the player, other pedestrians, traffic and static colliders before moving; Actions run #61 passed the new avoidance tests, full JVM suite and Android Lint. Real-device validation remains pending; next improve vehicle exit points and broader traffic awareness.
3. **Vehicle expansion:** introduce one licensed motorcycle asset, then add a selectable bike, steering/braking, wheel rotation and reliable enter/exit. Keep the SUV as the regression baseline.
4. **Living market block:** add a small coherent market lane with distinct shopfronts, stalls, signboards and a few role-specific NPC routines before expanding the map footprint.
5. **Reliable asset delivery:** the core character cache and bounded optional-model LRU are implemented. Next validate warm offline launches and low-storage eviction on devices, then add a locally generated low-poly character fallback so a first-ever offline install can still move around the city. Evaluate texture/mesh LODs that preserve rigs and add cancellation/progress for long downloads.
6. **World depth:** add interiors and meaningful POIs, mission progression, weather and atmospheric variation; persist the session day/night clock across process death.
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
- Keep GitHub Actions on supported Node 24 action majors; checkout, Java setup and artifact upload use actions/checkout@v7, actions/setup-java@v6 and actions/upload-artifact@v7. Actions run #62 passed the migration.


## Latest verification — 2026-11-12 (Actions run #61)

- Pedestrian yielding/avoidance commit `f43a6fe828b2873c4da8a6a2f56cee3755c64f7a` passed `testDebugUnitTest`, `lintDebug`, `assembleDebug`, APK existence verification and artifact upload.
- Extracted Debug APK: **52,744,400 bytes**; SHA-256 `ea8dd1b06a803352973e92c03d0d0144f45ac54969c1600db1d177247c56e649`.
- Run #61 emitted GitHub Actions Node 20 / `setup-java@v4` deprecation warnings. The workflow action majors have now been updated to Node 24-compatible releases; run #62 must verify that workflow-only migration.
- No Android emulator or physical-device visual QA was available. Pedestrian avoidance behaviour is unit-tested, not yet visually validated in gameplay.
- No new third-party game assets were imported.


## Latest update — 2026-11-13: collision-aware vehicle exits

- Vehicle exit now requires the SUV to be effectively stopped (≤ 0.8 m/s). The passenger-side step-out point is preferred; the opposite side is used only when its expanded static-collider box and dynamic pedestrian/worker/traffic clearances are safe.
- If both sides are obstructed or collision data is invalid, the interaction refuses to exit and asks the player to reposition the vehicle rather than teleporting Aarav through geometry.
- Seven JVM regression cases cover preferred-side selection, opposite-side fallback, both-sides-blocked refusal, pedestrian/traffic occupancy, rotated vehicle heading, invalid collision data and the near-stop speed gate. Device-side exit animation and collision feel still need visual/play testing.


## Latest update — 2026-11-13: session day/night lighting

- Added a 24-minute real-time world day: the simulation starts at 08:00 and advances one in-game minute per real second, with a 24-hour HUD clock and morning/afternoon/evening/night phase labels.
- The main directional sun now changes intensity, colour temperature and direction through sunrise, midday, sunset and night. Its direction is normalized to Filament's required unit vector, with a regression test across night/dawn/noon/dusk. Three shadow-free warm point lights fade in around the existing procedural street lamps after dusk; no new downloads or third-party assets are required.
- Lighting updates are throttled to 4 Hz and the clock label to 1 Hz, avoiding per-frame Compose recomposition. The night fill is intentionally non-zero for mobile readability.
- World time is currently session-local and resets to 08:00 after a fresh process; save persistence, device brightness tuning, visual screenshots and frame-time profiling remain pending.
- This is a first lighting pass, not a full skybox, physically based atmosphere, weather system or verified day/night visual result.

## Latest verification — 2026-11-13 (GitHub Actions run #73)

- `./gradlew testDebugUnitTest --no-daemon` passed, including the nine `PatnaWorldClockTest` cases.
- `./gradlew lintDebug --no-daemon` passed.
- `./gradlew assembleDebug --no-daemon --stacktrace` passed.
- APK existence check passed. `app-debug.apk` is **52,777,168 bytes**; `PATNA-2047-DEBUG-APK` uploaded successfully (compressed artifact: 40,603,571 bytes). [Actions run #71](https://github.com/shubhm-01/My-first-game/actions/runs/43441856911).
- No Android emulator or physical device was available. Day/night exposure, streetlamp falloff, camera composition, pedestrian/vehicle feel, frame time, GPU/memory and thermal behaviour remain unverified in rendered gameplay. The CI run validates source compilation and JVM logic, not visual correction.
- The four Grab3D runtime URLs were checked with read-only HTTP HEAD requests for exact byte sizes and package provenance; their binary vertex/joint data was not downloaded or parsed in this run. Static/runtime character bounds and animated visible heights remain pending device capture.
