# Patna 2047 — asset credits and licence notes

This branch uses original game code plus remote GLB models from Grab3D. The source model pages and package licence files identify these assets as CC0 1.0 Public Domain Dedication, including commercial game use and no attribution requirement. Some models are disclosed by Grab3D as AI-assisted, then cleaned up and textured in Blender.

| Asset | Source page | Runtime file |
|---|---|---|
| Casual Man (rigged, walk/run) | https://grab3d.com/models/characters/man/ | https://grab3d.com/dl/man-rigged/man-rigged.glb |
| Casual Woman (rigged, walk/run) | https://grab3d.com/models/characters/woman/ | https://grab3d.com/dl/woman-rigged/woman-rigged.glb |
| Businessman pedestrian (rigged, walk/run) | https://grab3d.com/models/characters/businessman/ | https://grab3d.com/dl/businessman-rigged/businessman-rigged.glb |
| Farmer / market worker (rigged, walk/run) | https://grab3d.com/models/characters/farmer/ | https://grab3d.com/dl/farmer-rigged/farmer-rigged.glb |
| Modern House | https://grab3d.com/models/buildings/modern-house/ | https://grab3d.com/dl/modern-house/modern-house.glb |
| 1990s SUV | https://grab3d.com/models/vehicles/1990s-suv/ | https://grab3d.com/dl/1990s-suv/1990s-suv.glb |
| Mangrove Tree | https://grab3d.com/models/nature/mangrove-tree/ | https://grab3d.com/dl/mangrove-tree/mangrove-tree.glb |
| Street Light | https://grab3d.com/models/city/street-light/ | https://grab3d.com/dl/street-light/street-light.glb |
| Bus Stop Shelter | https://grab3d.com/models/city/bus-stop-shelter/ | https://grab3d.com/dl/bus-stop-shelter/bus-stop-shelter.glb |
| Farm Stand (market / vegetable stall) | https://grab3d.com/models/buildings/farm-stand/ | https://grab3d.com/dl/farm-stand/farm-stand.glb |
| Mixed-use High-rise (134.6 m tower, textured GLB) | https://www.innerscene.com/tools/library/3d-parts/mixed-use-high-rise-27a41221 | https://www.innerscene.com/api/library/mixed-use-high-rise-27a41221/download |
| City Bank (civic building) | https://grab3d.com/models/buildings/bank/ | https://grab3d.com/dl/bank/bank.glb |
| University Building (clock-tower landmark) | https://grab3d.com/models/buildings/university-building/ | https://grab3d.com/dl/university-building/university-building.glb |
| Power Plant (industrial skyline) | https://grab3d.com/models/buildings/power-plant/ | https://grab3d.com/dl/power-plant/power-plant.glb |

## Human model dimensions and mobile budget (verified 2026-11-10)

The four exact Grab3D source pages publish these upright dimensions and rigged GLB sizes. The configured target stature is intentionally a little below the source height for adult variation; the scale factor shown is `target height ÷ published source height`.

| Actor / source asset | Publisher dimensions (W × D × H) | Rigged GLB size | Target stature | Approx. uniform scale |
|---|---:|---:|---:|---:|
| Aarav — Casual Man | 1.15 × 0.36 × 1.80 m | 12 MB | 1.72 m | 0.956 |
| Meera — Casual Woman | 0.91 × 0.37 × 1.70 m | 12 MB | 1.64 m | 0.965 |
| Bank employee — Businessman | 1.06 × 0.35 × 1.80 m | 12 MB | 1.76 m | 0.978 |
| Market farmer — Farmer | 1.22 × 0.48 × 1.90 m | 13 MB | 1.68 m | 0.884 |

Source pages: [Casual Man](https://grab3d.com/models/characters/man/), [Casual Woman](https://grab3d.com/models/characters/woman/), [Businessman](https://grab3d.com/models/characters/businessman/), [Farmer](https://grab3d.com/models/characters/farmer/). Grab3D's [licence statement](https://grab3d.com/license/) dedicates its models under CC0 1.0 for commercial use without attribution. The pages identify separate rigged Walk/Run versions.

These are publisher-listed source dimensions, not a claim that every animated pose has the same bounds. Debug builds now emit `Patna2047Scale` log entries for each loaded actor: native vertical AABB, applied scale, post-scale static world-Y range, root yaw and animation count. The static AABB does not include deformed skeletal-animation extremes; those still require runtime capture or real-device visual inspection.

The four rigged characters total approximately **49 MB** before other city assets. This is a meaningful mobile first-load/network budget risk. Keep the current remote-first behavior documented until a separately tested, licence-compatible local/cache and texture-LOD pipeline is ready; do not replace rigged models with static low-resolution files that would remove walking animation.
The Mixed-use High-rise is an original Innerscene model with a stated 56.2 × 42.3 m footprint and 134.6 m overall height; the publisher identifies it as CC0 and the downloaded GLB is about 23.1 MB. Its download API is used as a remote HTTPS GLB URL.

The additional Grab3D bank, university and power-plant models are lightweight PBR GLBs, each listed by the publisher as CC0 1.0. They broaden the skyline, but their art direction is stylized rather than photorealistic.

Grab3D licence: https://grab3d.com/license/
CC0 1.0: https://creativecommons.org/publicdomain/zero/1.0/

The four active human characters (Aarav, Meera, a bank employee and a market farmer) use distinct rigged, textured CC0 GLBs with walk/run takes. Their scale is now derived from each loaded model's vertical AABB (`2 × halfExtent.y`) so the configured adult stature targets (1.72 m, 1.64 m, 1.76 m and 1.68 m) describe actual model height rather than the longest side of a fit-to-cube. Meera follows a home → market → Ganga promenade route; the bank employee follows a separate bank-shift loop; the farmer makes a produce run around the market stall and pauses at the work stop. Vehicle/building scale continues to use the existing fit-to-cube sizing. The world adds three further parked SUVs; collision boxes are authored in game code for the main buildings and parked vehicles. The Golghar-inspired landmark, riverfront terrain, ghats, roads, bridge deck, sidewalks and street furniture are generated in the app code. Network access is currently required on first load because the GLB models are fetched by HTTPS at runtime.
