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

The Mixed-use High-rise is an original Innerscene model with a stated 56.2 × 42.3 m footprint and 134.6 m overall height; the publisher identifies it as CC0 and the downloaded GLB is about 23.1 MB. Its download API is used as a remote HTTPS GLB URL.

The additional Grab3D bank, university and power-plant models are lightweight PBR GLBs, each listed by the publisher as CC0 1.0. They broaden the skyline, but their art direction is stylized rather than photorealistic.

Grab3D licence: https://grab3d.com/license/
CC0 1.0: https://creativecommons.org/publicdomain/zero/1.0/

The four active human characters (Aarav, Meera, a businessman and a farmer) use rigged, textured CC0 GLBs with walk/run takes. Human node scale targets are set to human-height ranges (about 1.6–1.8 m), while vehicles and buildings use their metre-scale asset bounds. The world adds three further parked SUVs; collision boxes are authored in game code for the main buildings and parked vehicles. The Golghar-inspired landmark, riverfront terrain, ghats, roads, bridge deck, sidewalks and street furniture are generated in the app code. Network access is currently required on first load because the GLB models are fetched by HTTPS at runtime.
