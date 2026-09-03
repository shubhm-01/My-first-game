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
out_dir = "app/src/main/assets/models"

# 1. Alchemy Table (Workbench)
# Rich aged dark oak tabletop, dark ironwood frame, brass corner braces, leather mat & bronze mortar
tbl_top_v, tbl_top_n, tbl_top_i = add_box((0, 0.85, 0), (2.0, 0.12, 1.0))

tbl_frame_parts = []
# 4 Sturdy legs
leg_w, leg_h, leg_d = 0.14, 0.8, 0.14
tbl_frame_parts.append(add_box((-0.85, 0.4, -0.38), (leg_w, leg_h, leg_d)))
tbl_frame_parts.append(add_box(( 0.85, 0.4, -0.38), (leg_w, leg_h, leg_d)))
tbl_frame_parts.append(add_box((-0.85, 0.4,  0.38), (leg_w, leg_h, leg_d)))
tbl_frame_parts.append(add_box(( 0.85, 0.4,  0.38), (leg_w, leg_h, leg_d)))
# Cross-braces
tbl_frame_parts.append(add_box((0, 0.25, -0.38), (1.6, 0.08, 0.08)))
tbl_frame_parts.append(add_box((0, 0.25,  0.38), (1.6, 0.08, 0.08)))
tbl_frm_v, tbl_frm_n, tbl_frm_i = merge_meshes(tbl_frame_parts)

# Brass corner protectors on all 4 corners
tbl_brass_parts = []
tbl_brass_parts.append(add_box((-0.94, 0.86, -0.44), (0.14, 0.14, 0.14)))
tbl_brass_parts.append(add_box(( 0.94, 0.86, -0.44), (0.14, 0.14, 0.14)))
tbl_brass_parts.append(add_box((-0.94, 0.86,  0.44), (0.14, 0.14, 0.14)))
tbl_brass_parts.append(add_box(( 0.94, 0.86,  0.44), (0.14, 0.14, 0.14)))
tbl_brs_v, tbl_brs_n, tbl_brs_i = merge_meshes(tbl_brass_parts)

# Alchemical equipment resting on table (leather work mat + bronze mortar & pestle)
tbl_equip_parts = []
tbl_equip_parts.append(add_box((-0.40, 0.92, 0.0), (0.50, 0.02, 0.45))) # Leather mat
tbl_equip_parts.append(add_box((-0.40, 0.98, 0.0), (0.18, 0.10, 0.18))) # Bronze mortar
tbl_equip_parts.append(add_box((-0.37, 1.05, 0.03), (0.04, 0.12, 0.04))) # Pestle
tbl_eqp_v, tbl_eqp_n, tbl_eqp_i = merge_meshes(tbl_equip_parts)

export_glb(f"{out_dir}/alchemy_table.glb", [
    {
        "name": "TableWoodTop",
        "vertices": tbl_top_v,
        "normals": tbl_top_n,
        "indices": tbl_top_i,
        "color": (0.32, 0.19, 0.12, 1.0), # Polished aged dark oak
        "metallic": 0.05,
        "roughness": 0.65
    },
    {
        "name": "TableWoodFrame",
        "vertices": tbl_frm_v,
        "normals": tbl_frm_n,
        "indices": tbl_frm_i,
        "color": (0.19, 0.12, 0.08, 1.0), # Heavy dark ironwood legs
        "metallic": 0.05,
        "roughness": 0.78
    },
    {
        "name": "TableBrassHardware",
        "vertices": tbl_brs_v,
        "normals": tbl_brs_n,
        "indices": tbl_brs_i,
        "color": (0.75, 0.58, 0.20, 1.0), # Polished brass brackets
        "metallic": 0.85,
        "roughness": 0.30
    },
    {
        "name": "TableEquipment",
        "vertices": tbl_eqp_v,
        "normals": tbl_eqp_n,
        "indices": tbl_eqp_i,
        "color": (0.45, 0.35, 0.18, 1.0), # Antique bronze tools & leather
        "metallic": 0.75,
        "roughness": 0.40
    }
])

# 2. Ancient Bronze Cauldron
cauldron_body_parts = []
cauldron_body_parts.append(add_box((0, 0.05, 0), (0.5, 0.1, 0.5))) # Base plate
cauldron_body_parts.append(add_box((0, 0.35, 0), (0.9, 0.5, 0.9))) # Main belly
cauldron_body_parts.append(add_box((0, 0.65, 0), (0.75, 0.15, 0.75))) # Upper neck
cld_body_v, cld_body_n, cld_body_i = merge_meshes(cauldron_body_parts)

cauldron_trim_parts = []
cauldron_trim_parts.append(add_box((0, 0.76, 0), (0.88, 0.08, 0.88))) # Flared rim
cauldron_trim_parts.append(add_box((-0.35, 0.08, -0.35), (0.12, 0.16, 0.12))) # Legs
cauldron_trim_parts.append(add_box(( 0.35, 0.08, -0.35), (0.12, 0.16, 0.12)))
cauldron_trim_parts.append(add_box(( 0.00, 0.08,  0.40), (0.12, 0.16, 0.12)))
cld_trm_v, cld_trm_n, cld_trm_i = merge_meshes(cauldron_trim_parts)

export_glb(f"{out_dir}/ancient_cauldron.glb", [
    {
        "name": "CauldronBody",
        "vertices": cld_body_v,
        "normals": cld_body_n,
        "indices": cld_body_i,
        "color": (0.18, 0.19, 0.18, 1.0), # Deep cast iron-bronze
        "metallic": 0.75,
        "roughness": 0.52
    },
    {
        "name": "CauldronPatinaTrim",
        "vertices": cld_trm_v,
        "normals": cld_trm_n,
        "indices": cld_trm_i,
        "color": (0.24, 0.38, 0.32, 1.0), # Verdigris oxidized bronze rim
        "metallic": 0.65,
        "roughness": 0.42
    }
])

# 3. Mystic Crystal Relic
crys_ped_v, crys_ped_n, crys_ped_i = add_box((0, 0.25, 0), (0.45, 0.5, 0.45))
crys_ring_v, crys_ring_n, crys_ring_i = add_box((0, 0.53, 0), (0.35, 0.06, 0.35))

crys_core_parts = []
crys_core_parts.append(add_box((0, 0.85, 0), (0.24, 0.5, 0.24)))
crys_core_parts.append(add_box((0, 0.85, 0), (0.32, 0.25, 0.16)))
crys_core_v, crys_core_n, crys_core_i = merge_meshes(crys_core_parts)

export_glb(f"{out_dir}/mystic_crystal.glb", [
    {
        "name": "PedestalBase",
        "vertices": crys_ped_v,
        "normals": crys_ped_n,
        "indices": crys_ped_i,
        "color": (0.09, 0.09, 0.11, 1.0), # Deep obsidian
        "metallic": 0.15,
        "roughness": 0.30
    },
    {
        "name": "PedestalRing",
        "vertices": crys_ring_v,
        "normals": crys_ring_n,
        "indices": crys_ring_i,
        "color": (0.70, 0.55, 0.18, 1.0), # Forged brass socket
        "metallic": 0.85,
        "roughness": 0.30
    },
    {
        "name": "CrystalCore",
        "vertices": crys_core_v,
        "normals": crys_core_n,
        "indices": crys_core_i,
        "color": (0.25, 0.85, 1.0, 1.0), # Luminous azure
        "metallic": 0.20,
        "roughness": 0.15,
        "emissive": (0.25, 0.75, 1.2)
    }
])

# 4. Wall Torch Sconce
trc_bracket_parts = []
trc_bracket_parts.append(add_box((0, 0, -0.05), (0.14, 0.4, 0.06)))
trc_bracket_parts.append(add_box((0, 0.08, 0.15), (0.06, 0.06, 0.35)))
trc_brk_v, trc_brk_n, trc_brk_i = merge_meshes(trc_bracket_parts)

trc_cup_v, trc_cup_n, trc_cup_i = add_box((0, 0.18, 0.30), (0.16, 0.14, 0.16))
trc_flame_v, trc_flame_n, trc_flame_i = add_box((0, 0.32, 0.30), (0.12, 0.18, 0.12))

export_glb(f"{out_dir}/wall_torch.glb", [
    {
        "name": "TorchBracket",
        "vertices": trc_brk_v,
        "normals": trc_brk_n,
        "indices": trc_brk_i,
        "color": (0.10, 0.10, 0.12, 1.0), # Forged black iron
        "metallic": 0.85,
        "roughness": 0.35
    },
    {
        "name": "TorchCup",
        "vertices": trc_cup_v,
        "normals": trc_cup_n,
        "indices": trc_cup_i,
        "color": (0.48, 0.38, 0.18, 1.0), # Antique brass sconce
        "metallic": 0.80,
        "roughness": 0.35
    },
    {
        "name": "TorchFlame",
        "vertices": trc_flame_v,
        "normals": trc_flame_n,
        "indices": trc_flame_i,
        "color": (1.0, 0.65, 0.12, 1.0), # Radiant fire ember
        "emissive": (1.8, 0.95, 0.20)
    }
])

# 5. Ancient Alchemy Academy Test Room
# Multi-mesh architecture: Floor, FloorBorder, Ceiling, Walls, Doorway Arch, Pillar Shafts, Pillar Plinths
rm_floor_parts = []
# Main chamber floor (10x12m)
rm_floor_parts.append(add_box((0, -0.05, 0), (9.4, 0.1, 11.4)))
# Doorway threshold paving connecting room to exterior
rm_floor_parts.append(add_box((0, -0.05, -5.95), (2.2, 0.1, 0.6)))
# Minimal safe exterior landing terrace for testing doorway traversal
rm_floor_parts.append(add_box((0, -0.05, -7.85), (4.6, 0.1, 3.2)))
rm_floor_v, rm_floor_n, rm_floor_i = merge_meshes(rm_floor_parts)

rm_border_parts = []
rm_border_parts.append(add_box((-4.85, 0.02, 0), (0.3, 0.06, 12.0)))
rm_border_parts.append(add_box(( 4.85, 0.02, 0), (0.3, 0.06, 12.0)))
rm_border_parts.append(add_box((0, 0.02,  5.85), (9.4, 0.06, 0.3)))
# North wall interior curbs flanking the doorway (opening from -1.05 to 1.05 kept completely flush)
rm_border_parts.append(add_box((-2.95, 0.02, -5.85), (3.8, 0.06, 0.3)))
rm_border_parts.append(add_box(( 2.95, 0.02, -5.85), (3.8, 0.06, 0.3)))
# Exterior landing safety balustrades (0.5m high stone perimeter)
rm_border_parts.append(add_box((-2.35, 0.25, -7.85), (0.25, 0.5, 3.2)))
rm_border_parts.append(add_box(( 2.35, 0.25, -7.85), (0.25, 0.5, 3.2)))
rm_border_parts.append(add_box((0.0, 0.25, -9.5), (4.95, 0.5, 0.25)))
# Balustrade returns connecting to outer north wall
rm_border_parts.append(add_box((-1.70, 0.25, -6.15), (1.1, 0.5, 0.25)))
rm_border_parts.append(add_box(( 1.70, 0.25, -6.15), (1.1, 0.5, 0.25)))
rm_brd_v, rm_brd_n, rm_brd_i = merge_meshes(rm_border_parts)

rm_ceil_v, rm_ceil_n, rm_ceil_i = add_box((0, 3.85, 0), (10.0, 0.1, 12.0))

rm_wall_parts = []
# North Wall with Doorway (-Z): Left segment, right segment, lintel above door
rm_wall_parts.append(add_box((-3.0, 1.9, -6.0), (4.0, 3.8, 0.4)))
rm_wall_parts.append(add_box(( 3.0, 1.9, -6.0), (4.0, 3.8, 0.4)))
rm_wall_parts.append(add_box(( 0.0, 3.15, -6.0), (2.0, 1.3, 0.4)))
# South Wall (+Z)
rm_wall_parts.append(add_box((0, 1.9, 6.0), (10.0, 3.8, 0.4)))
# West Wall (-X)
rm_wall_parts.append(add_box((-5.0, 1.9, 0), (0.4, 3.8, 12.0)))
# East Wall (+X)
rm_wall_parts.append(add_box(( 5.0, 1.9, 0), (0.4, 3.8, 12.0)))
rm_wll_v, rm_wll_n, rm_wll_i = merge_meshes(rm_wall_parts)

rm_arch_parts = []
# Doorway frame trim: Left jamb, right jamb, arched header lintel
rm_arch_parts.append(add_box((-1.05, 1.25, -6.0), (0.15, 2.5, 0.5)))
rm_arch_parts.append(add_box(( 1.05, 1.25, -6.0), (0.15, 2.5, 0.5)))
rm_arch_parts.append(add_box(( 0.00, 2.55, -6.0), (2.25, 0.15, 0.5)))
rm_arch_v, rm_arch_n, rm_arch_i = merge_meshes(rm_arch_parts)

rm_pillar_shaft_parts = []
rm_pillar_base_parts = []
for px, pz in [(-2.2, -2.0), (2.2, -2.0), (-2.2, 2.0), (2.2, 2.0)]:
    # Plinth base
    rm_pillar_base_parts.append(add_box((px, 0.18, pz), (0.72, 0.36, 0.72)))
    # Shaft
    rm_pillar_shaft_parts.append(add_box((px, 1.90, pz), (0.56, 3.08, 0.56)))
    # Capital
    rm_pillar_base_parts.append(add_box((px, 3.62, pz), (0.72, 0.36, 0.72)))

rm_pshaft_v, rm_pshaft_n, rm_pshaft_i = merge_meshes(rm_pillar_shaft_parts)
rm_pbase_v, rm_pbase_n, rm_pbase_i = merge_meshes(rm_pillar_base_parts)

export_glb(f"{out_dir}/academy_room.glb", [
    {
        "name": "FloorFlagstones",
        "vertices": rm_floor_v,
        "normals": rm_floor_n,
        "indices": rm_floor_i,
        "color": (0.17, 0.18, 0.21, 1.0), # Dark slate stone paving
        "metallic": 0.05,
        "roughness": 0.78
    },
    {
        "name": "FloorBorderCurb",
        "vertices": rm_brd_v,
        "normals": rm_brd_n,
        "indices": rm_brd_i,
        "color": (0.12, 0.12, 0.14, 1.0), # Dark perimeter border
        "metallic": 0.05,
        "roughness": 0.85
    },
    {
        "name": "CeilingVault",
        "vertices": rm_ceil_v,
        "normals": rm_ceil_n,
        "indices": rm_ceil_i,
        "color": (0.08, 0.07, 0.09, 1.0), # Vaulted subterranean ceiling shadow
        "metallic": 0.02,
        "roughness": 0.95
    },
    {
        "name": "WallsAshlar",
        "vertices": rm_wll_v,
        "normals": rm_wll_n,
        "indices": rm_wll_i,
        "color": (0.24, 0.23, 0.26, 1.0), # Weathered academy masonry
        "metallic": 0.05,
        "roughness": 0.88
    },
    {
        "name": "DoorwayPortalFrame",
        "vertices": rm_arch_v,
        "normals": rm_arch_n,
        "indices": rm_arch_i,
        "color": (0.14, 0.13, 0.16, 1.0), # Dark carved granite archway
        "metallic": 0.15,
        "roughness": 0.65
    },
    {
        "name": "PillarShafts",
        "vertices": rm_pshaft_v,
        "normals": rm_pshaft_n,
        "indices": rm_pshaft_i,
        "color": (0.27, 0.26, 0.29, 1.0), # Chiseled stone column shafts
        "metallic": 0.05,
        "roughness": 0.85
    },
    {
        "name": "PillarBases",
        "vertices": rm_pbase_v,
        "normals": rm_pbase_n,
        "indices": rm_pbase_i,
        "color": (0.13, 0.12, 0.15, 1.0), # Dark basalt column plinths & capitals
        "metallic": 0.15,
        "roughness": 0.60
    }
])

# 6. Academy Sanctum Door (Interactable Door)
door_slab_v, door_slab_n, door_slab_i = add_box((0, 1.25, 0), (1.9, 2.45, 0.12))

door_iron_parts = []
for z_sign in [1.0, -1.0]:
    z_pos = z_sign * 0.07
    door_iron_parts.append(add_box((0, 0.40, z_pos), (1.92, 0.10, 0.04)))
    door_iron_parts.append(add_box((0, 1.25, z_pos), (1.92, 0.10, 0.04)))
    door_iron_parts.append(add_box((0, 2.10, z_pos), (1.92, 0.10, 0.04)))
# Heavy iron door pull ring / knocker
door_iron_parts.append(add_box((0.55, 1.20, 0.10), (0.12, 0.16, 0.06)))
door_iron_parts.append(add_box((-0.55, 1.20, -0.10), (0.12, 0.16, 0.06)))
door_irn_v, door_irn_n, door_irn_i = merge_meshes(door_iron_parts)

export_glb(f"{out_dir}/academy_door.glb", [
    {
        "name": "DoorWoodSlab",
        "vertices": door_slab_v,
        "normals": door_slab_n,
        "indices": door_slab_i,
        "color": (0.25, 0.16, 0.10, 1.0), # Weathered dark oak planks
        "metallic": 0.08,
        "roughness": 0.70
    },
    {
        "name": "DoorIronHardware",
        "vertices": door_irn_v,
        "normals": door_irn_n,
        "indices": door_irn_i,
        "color": (0.10, 0.10, 0.12, 1.0), # Forged black iron bands & knocker
        "metallic": 0.85,
        "roughness": 0.35
    }
])

# 7. Silver Herb Cutting Collectible Prop (Interactable Pick-up)
dish_v, dish_n, dish_i = add_box((0, 0.02, 0), (0.32, 0.04, 0.32))

herb_foliage_parts = []
herb_foliage_parts.append(add_box((0.00, 0.08,  0.00), (0.20, 0.10, 0.20)))
herb_foliage_parts.append(add_box((0.05, 0.14, -0.02), (0.15, 0.12, 0.14)))
herb_foliage_parts.append(add_box((-0.04, 0.12, 0.03), (0.14, 0.10, 0.15)))
herb_fol_v, herb_fol_n, herb_fol_i = merge_meshes(herb_foliage_parts)

export_glb(f"{out_dir}/silver_herb.glb", [
    {
        "name": "OfferingDish",
        "vertices": dish_v,
        "normals": dish_n,
        "indices": dish_i,
        "color": (0.42, 0.33, 0.18, 1.0), # Antique bronze bowl
        "metallic": 0.85,
        "roughness": 0.35
    },
    {
        "name": "SilverHerbLeaves",
        "vertices": herb_fol_v,
        "normals": herb_fol_n,
        "indices": herb_fol_i,
        "color": (0.55, 0.95, 0.75, 1.0), # Luminous shimmering silver-emerald leaf
        "metallic": 0.25,
        "roughness": 0.20,
        "emissive": (0.35, 0.75, 0.50) # Mystical luminescence
    }
])

print("All GLB assets successfully generated!")
