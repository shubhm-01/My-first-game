#!/usr/bin/env python3
import struct
import json
import os
import math

def export_glb(filepath, meshes_data):
    """
    meshes_data: list of dicts with:
      'name': str
      'vertices': list of (x, y, z)
      'normals': list of (nx, ny, nz)
      'indices': list of int
      'color': (r, g, b, a)
      'metallic': float
      'roughness': float
      'emissive': (r, g, b) optional
    """
    # Combine buffers
    bin_chunks = bytearray()
    buffer_views = []
    accessors = []
    materials = []
    meshes = []
    nodes = []

    for i, m in enumerate(meshes_data):
        verts = m['vertices']
        normals = m['normals']
        indices = m['indices']
        color = m.get('color', (0.7, 0.7, 0.7, 1.0))
        metallic = m.get('metallic', 0.1)
        roughness = m.get('roughness', 0.8)
        emissive = m.get('emissive', (0.0, 0.0, 0.0))

        # 1. Vertex positions
        pos_bytes = bytearray()
        min_x = min(v[0] for v in verts)
        max_x = max(v[0] for v in verts)
        min_y = min(v[1] for v in verts)
        max_y = max(v[1] for v in verts)
        min_z = min(v[2] for v in verts)
        max_z = max(v[2] for v in verts)
        for v in verts:
            pos_bytes.extend(struct.pack('<fff', v[0], v[1], v[2]))
        
        # Align to 4 bytes
        while len(bin_chunks) % 4 != 0:
            bin_chunks.append(0)
        pos_offset = len(bin_chunks)
        bin_chunks.extend(pos_bytes)

        # 2. Normals
        norm_bytes = bytearray()
        for n in normals:
            norm_bytes.extend(struct.pack('<fff', n[0], n[1], n[2]))
        while len(bin_chunks) % 4 != 0:
            bin_chunks.append(0)
        norm_offset = len(bin_chunks)
        bin_chunks.extend(norm_bytes)

        # 3. Indices
        idx_bytes = bytearray()
        for idx in indices:
            idx_bytes.extend(struct.pack('<H', idx))
        while len(bin_chunks) % 4 != 0:
            bin_chunks.append(0)
        idx_offset = len(bin_chunks)
        bin_chunks.extend(idx_bytes)

        # Buffer views
        bv_pos_idx = len(buffer_views)
        buffer_views.append({
            "buffer": 0,
            "byteOffset": pos_offset,
            "byteLength": len(pos_bytes),
            "target": 34962 # ARRAY_BUFFER
        })
        bv_norm_idx = len(buffer_views)
        buffer_views.append({
            "buffer": 0,
            "byteOffset": norm_offset,
            "byteLength": len(norm_bytes),
            "target": 34962 # ARRAY_BUFFER
        })
        bv_idx_idx = len(buffer_views)
        buffer_views.append({
            "buffer": 0,
            "byteOffset": idx_offset,
            "byteLength": len(idx_bytes),
            "target": 34963 # ELEMENT_ARRAY_BUFFER
        })

        # Accessors
        acc_pos_idx = len(accessors)
        accessors.append({
            "bufferView": bv_pos_idx,
            "byteOffset": 0,
            "componentType": 5126, # FLOAT
            "count": len(verts),
            "type": "VEC3",
            "max": [max_x, max_y, max_z],
            "min": [min_x, min_y, min_z]
        })
        acc_norm_idx = len(accessors)
        accessors.append({
            "bufferView": bv_norm_idx,
            "byteOffset": 0,
            "componentType": 5126, # FLOAT
            "count": len(normals),
            "type": "VEC3"
        })
        acc_idx_idx = len(accessors)
        accessors.append({
            "bufferView": bv_idx_idx,
            "byteOffset": 0,
            "componentType": 5123, # UNSIGNED_SHORT
            "count": len(indices),
            "type": "SCALAR"
        })

        # Material
        mat_idx = len(materials)
        mat_def = {
            "name": m.get('name', f"Mat_{i}"),
            "pbrMetallicRoughness": {
                "baseColorFactor": list(color),
                "metallicFactor": float(metallic),
                "roughnessFactor": float(roughness)
            },
            "doubleSided": True
        }
        if emissive != (0.0, 0.0, 0.0):
            mat_def["emissiveFactor"] = list(emissive)
        materials.append(mat_def)

        # Mesh
        mesh_idx = len(meshes)
        meshes.append({
            "name": m.get('name', f"Mesh_{i}"),
            "primitives": [{
                "attributes": {
                    "POSITION": acc_pos_idx,
                    "NORMAL": acc_norm_idx
                },
                "indices": acc_idx_idx,
                "material": mat_idx
            }]
        })

        # Node
        node_idx = len(nodes)
        node_def = {"mesh": mesh_idx, "name": m.get('name', f"Node_{i}")}
        if 'translation' in m:
            node_def['translation'] = list(m['translation'])
        if 'scale' in m:
            node_def['scale'] = list(m['scale'])
        nodes.append(node_def)

    gltf = {
        "asset": {"version": "2.0", "generator": "AlchemistGLBExporter"},
        "scene": 0,
        "scenes": [{"nodes": list(range(len(nodes)))}],
        "nodes": nodes,
        "meshes": meshes,
        "materials": materials,
        "accessors": accessors,
        "bufferViews": buffer_views,
        "buffers": [{"byteLength": len(bin_chunks)}]
    }

    json_str = json.dumps(gltf, separators=(',', ':'))
    json_bytes = json_str.encode('utf-8')
    # Pad JSON chunk to 4-byte alignment with space (0x20)
    while len(json_bytes) % 4 != 0:
        json_bytes += b' '

    # Pad BIN chunk to 4-byte alignment with null (0x00)
    while len(bin_chunks) % 4 != 0:
        bin_chunks.append(0)

    total_len = 12 + 8 + len(json_bytes) + 8 + len(bin_chunks)
    header = struct.pack('<4sII', b'glTF', 2, total_len)
    json_header = struct.pack('<II', len(json_bytes), 0x4E4F534A)
    bin_header = struct.pack('<II', len(bin_chunks), 0x004E4942)

    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    with open(filepath, 'wb') as f:
        f.write(header)
        f.write(json_header)
        f.write(json_bytes)
        f.write(bin_header)
        f.write(bin_chunks)
    print(f"Exported {filepath} ({total_len} bytes)")

def add_box(center, size):
    cx, cy, cz = center
    w, h, d = size[0] / 2.0, size[1] / 2.0, size[2] / 2.0
    faces = [
        ([(-w,-h, d), ( w,-h, d), ( w, h, d), (-w, h, d)], (0, 0, 1)),   # front
        ([( w,-h,-d), (-w,-h,-d), (-w, h,-d), ( w, h,-d)], (0, 0, -1)),  # back
        ([(-w, h, d), ( w, h, d), ( w, h,-d), (-w, h,-d)], (0, 1, 0)),   # top
        ([(-w,-h,-d), ( w,-h,-d), ( w,-h, d), (-w,-h, d)], (0, -1, 0)),  # bottom
        ([( w,-h, d), ( w,-h,-d), ( w, h,-d), ( w, h, d)], (1, 0, 0)),   # right
        ([(-w,-h,-d), (-w,-h, d), (-w, h, d), (-w, h,-d)], (-1, 0, 0)),  # left
    ]
    verts = []
    normals = []
    indices = []
    for face_verts, norm in faces:
        base_idx = len(verts)
        for vx, vy, vz in face_verts:
            verts.append((cx + vx, cy + vy, cz + vz))
            normals.append(norm)
        indices.extend([base_idx, base_idx + 1, base_idx + 2, base_idx, base_idx + 2, base_idx + 3])
    return verts, normals, indices

def merge_meshes(mesh_list):
    all_verts = []
    all_normals = []
    all_indices = []
    for v, n, idxs in mesh_list:
        base = len(all_verts)
        all_verts.extend(v)
        all_normals.extend(n)
        all_indices.extend([base + i for i in idxs])
    return all_verts, all_normals, all_indices

# --- Generate Models ---
out_dir = "/app/applet/app/src/main/assets/models"

# 1. Alchemy Table
# Dark aged oak top with carved stone legs and brass corner braces
parts = []
# Table top (width 2.0, height 0.12, depth 1.0) at height 0.85
parts.append(add_box((0, 0.85, 0), (2.0, 0.12, 1.0)))
# 4 Sturdy legs
leg_w, leg_h, leg_d = 0.14, 0.8, 0.14
parts.append(add_box((-0.85, 0.4, -0.38), (leg_w, leg_h, leg_d)))
parts.append(add_box(( 0.85, 0.4, -0.38), (leg_w, leg_h, leg_d)))
parts.append(add_box((-0.85, 0.4,  0.38), (leg_w, leg_h, leg_d)))
parts.append(add_box(( 0.85, 0.4,  0.38), (leg_w, leg_h, leg_d)))
# Cross-braces
parts.append(add_box((0, 0.25, -0.38), (1.6, 0.08, 0.08)))
parts.append(add_box((0, 0.25,  0.38), (1.6, 0.08, 0.08)))
v_tbl, n_tbl, i_tbl = merge_meshes(parts)

export_glb(f"{out_dir}/alchemy_table.glb", [{
    "name": "AlchemyTable",
    "vertices": v_tbl,
    "normals": n_tbl,
    "indices": i_tbl,
    "color": (0.32, 0.22, 0.15, 1.0), # Aged dark oak
    "metallic": 0.15,
    "roughness": 0.75
}])

# 2. Ancient Bronze Cauldron
# Octagonal faceted cauldron body with rim and base
cauldron_parts = []
# Base plate
cauldron_parts.append(add_box((0, 0.05, 0), (0.5, 0.1, 0.5)))
# Main belly (segmented box stack)
cauldron_parts.append(add_box((0, 0.35, 0), (0.9, 0.5, 0.9)))
# Upper neck
cauldron_parts.append(add_box((0, 0.65, 0), (0.75, 0.15, 0.75)))
# Flared rim
cauldron_parts.append(add_box((0, 0.76, 0), (0.88, 0.08, 0.88)))
# 3 stubby legs
cauldron_parts.append(add_box((-0.35, 0.08, -0.35), (0.12, 0.16, 0.12)))
cauldron_parts.append(add_box(( 0.35, 0.08, -0.35), (0.12, 0.16, 0.12)))
cauldron_parts.append(add_box(( 0.00, 0.08,  0.40), (0.12, 0.16, 0.12)))
v_cld, n_cld, i_cld = merge_meshes(cauldron_parts)

export_glb(f"{out_dir}/ancient_cauldron.glb", [{
    "name": "AncientCauldron",
    "vertices": v_cld,
    "normals": n_cld,
    "indices": i_cld,
    "color": (0.22, 0.25, 0.24, 1.0), # Dark oxidized bronze
    "metallic": 0.85,
    "roughness": 0.45
}])

# 3. Mystic Crystal Relic (Interactable)
# Octagonal glowing alchemical crystal on an obsidian pedestal
crystal_parts = []
# Pedestal base
crystal_parts.append(add_box((0, 0.25, 0), (0.45, 0.5, 0.45)))
# Pedestal brass ring
crystal_parts.append(add_box((0, 0.53, 0), (0.35, 0.06, 0.35)))
# Floating glowing crystal core
crystal_parts.append(add_box((0, 0.85, 0), (0.24, 0.5, 0.24)))
crystal_parts.append(add_box((0, 0.85, 0), (0.32, 0.25, 0.16)))
v_crys, n_crys, i_crys = merge_meshes(crystal_parts)

export_glb(f"{out_dir}/mystic_crystal.glb", [{
    "name": "MysticCrystal",
    "vertices": v_crys,
    "normals": n_crys,
    "indices": i_crys,
    "color": (0.2, 0.85, 0.95, 1.0), # Luminous aether cyan
    "metallic": 0.2,
    "roughness": 0.1,
    "emissive": (0.1, 0.5, 0.7) # Glow
}])

# 4. Wall Torch Sconce
torch_parts = []
# Wall plate
torch_parts.append(add_box((0, 0, -0.05), (0.14, 0.4, 0.06)))
# Iron arm angled outward
torch_parts.append(add_box((0, 0.08, 0.15), (0.06, 0.06, 0.35)))
# Cup / sconce
torch_parts.append(add_box((0, 0.18, 0.30), (0.16, 0.14, 0.16)))
# Glowing flame ember
torch_parts.append(add_box((0, 0.32, 0.30), (0.12, 0.18, 0.12)))
v_trc, n_trc, i_trc = merge_meshes(torch_parts)

export_glb(f"{out_dir}/wall_torch.glb", [{
    "name": "WallTorch",
    "vertices": v_trc,
    "normals": n_trc,
    "indices": i_trc,
    "color": (0.95, 0.55, 0.15, 1.0), # Warm golden flame
    "metallic": 0.3,
    "roughness": 0.5,
    "emissive": (0.9, 0.4, 0.05) # Radiant flame warmth
}])

# 5. Ancient Alchemy Academy Test Room
# Room dimensions: Width 10.0m (-5 to +5), Depth 12.0m (-6 to +6), Height 3.8m
# Includes stone floor, 4 surrounding stone walls, arched doorway passage, stone pillars
room_parts = []
# Floor (10x12m)
room_parts.append(add_box((0, -0.05, 0), (10.0, 0.1, 12.0)))
# Ceiling (10x12m)
room_parts.append(add_box((0, 3.85, 0), (10.0, 0.1, 12.0)))
# North Wall with Doorway (-Z): Left segment, right segment, lintel above door
# Doorway is at center X in [-1.0, 1.0], height 2.5m
room_parts.append(add_box((-3.0, 1.9, -6.0), (4.0, 3.8, 0.4))) # West part of North wall
room_parts.append(add_box(( 3.0, 1.9, -6.0), (4.0, 3.8, 0.4))) # East part of North wall
room_parts.append(add_box(( 0.0, 3.15, -6.0), (2.0, 1.3, 0.4))) # Lintel above doorway
# Doorway frame trim
room_parts.append(add_box((-1.05, 1.25, -6.0), (0.15, 2.5, 0.5)))
room_parts.append(add_box(( 1.05, 1.25, -6.0), (0.15, 2.5, 0.5)))
room_parts.append(add_box(( 0.00, 2.55, -6.0), (2.25, 0.15, 0.5)))

# South Wall (+Z): Solid
room_parts.append(add_box((0, 1.9, 6.0), (10.0, 3.8, 0.4)))
# West Wall (-X): Solid
room_parts.append(add_box((-5.0, 1.9, 0), (0.4, 3.8, 12.0)))
# East Wall (+X): Solid
room_parts.append(add_box(( 5.0, 1.9, 0), (0.4, 3.8, 12.0)))

# 2 Vaulted Stone Pillars flanking the center aisle
pillar_size = (0.6, 3.8, 0.6)
room_parts.append(add_box((-2.2, 1.9, -2.0), pillar_size))
room_parts.append(add_box(( 2.2, 1.9, -2.0), pillar_size))
room_parts.append(add_box((-2.2, 1.9,  2.0), pillar_size))
room_parts.append(add_box(( 2.2, 1.9,  2.0), pillar_size))

v_rm, n_rm, i_rm = merge_meshes(room_parts)

export_glb(f"{out_dir}/academy_room.glb", [{
    "name": "AcademyRoom",
    "vertices": v_rm,
    "normals": n_rm,
    "indices": i_rm,
    "color": (0.35, 0.33, 0.38, 1.0), # Mystic weathered dark stone
    "metallic": 0.05,
    "roughness": 0.90
}])

# 6. Academy Sanctum Door (Interactable Door)
# Sits in North archway at Z = -5.9. Width 1.9m, height 2.45m
door_parts = []
# Heavy dark oak slab
door_parts.append(add_box((0, 1.25, 0), (1.9, 2.45, 0.12)))
# 3 Iron reinforcement bands front & back
for z_sign in [1.0, -1.0]:
    z_pos = z_sign * 0.07
    door_parts.append(add_box((0, 0.40, z_pos), (1.92, 0.10, 0.04)))
    door_parts.append(add_box((0, 1.25, z_pos), (1.92, 0.10, 0.04)))
    door_parts.append(add_box((0, 2.10, z_pos), (1.92, 0.10, 0.04)))
# Iron knocker / handle on front
door_parts.append(add_box((0.55, 1.20, 0.10), (0.12, 0.16, 0.06)))
v_door, n_door, i_door = merge_meshes(door_parts)

export_glb(f"{out_dir}/academy_door.glb", [{
    "name": "AcademyDoor",
    "vertices": v_door,
    "normals": n_door,
    "indices": i_door,
    "color": (0.24, 0.16, 0.11, 1.0), # Heavy aged dark ironwood
    "metallic": 0.25,
    "roughness": 0.70
}])

# 7. Silver Herb Cutting Collectible Prop (Interactable Pick-up)
# Sits on the alchemy table at (0.7, 0.91, -3.2)
herb_parts = []
# Bronze offering dish
herb_parts.append(add_box((0, 0.02, 0), (0.32, 0.04, 0.32)))
# Shimmering silver leaf clusters
herb_parts.append(add_box((0.00, 0.08,  0.00), (0.20, 0.10, 0.20)))
herb_parts.append(add_box((0.05, 0.14, -0.02), (0.15, 0.12, 0.14)))
herb_parts.append(add_box((-0.04, 0.12, 0.03), (0.14, 0.10, 0.15)))
v_herb, n_herb, i_herb = merge_meshes(herb_parts)

export_glb(f"{out_dir}/silver_herb.glb", [{
    "name": "SilverHerb",
    "vertices": v_herb,
    "normals": n_herb,
    "indices": i_herb,
    "color": (0.75, 0.95, 0.88, 1.0), # Luminous shimmering silver leaf
    "metallic": 0.35,
    "roughness": 0.25,
    "emissive": (0.20, 0.45, 0.35) # Soft alchemical luminescence
}])

print("All GLB assets successfully generated!")
