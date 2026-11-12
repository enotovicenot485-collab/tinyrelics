"""Builds the necromancer armor: paints every face of every box of the model from the
four concept views, and writes the matching Java model class."""
import sys
import numpy as np
from PIL import Image

SRC, OUT_TEX, OUT_JAVA = sys.argv[1], sys.argv[2], sys.argv[3]
K = 4                      # texels per model unit
SHEET = 64                 # logical texture size
im = np.array(Image.open(SRC).convert("RGB")).astype(np.float64)
H, W, _ = im.shape
BG = (im[..., 0] > 120) & (im[..., 2] > 120) & (im[..., 1] < 110) & (im[..., 0] - im[..., 1] > 70)

# Source rectangles (x0, y0, x1, y1) in the concept image for each face of each box.
# N = front, S = back, W = the character's right side, E = the character's left side.
HEAD = dict(N=(118, 105, 353, 298), S=(560, 105, 785, 298), W=(943, 105, 1161, 298), E=(1274, 105, 1493, 298))
BODY = dict(N=(136, 298, 336, 610), S=(572, 298, 775, 610), W=(955, 298, 1135, 610), E=(1300, 298, 1483, 610))
RARM = dict(N=(36, 298, 136, 610), S=(775, 298, 872, 610), W=(955, 298, 1135, 610), E=(1300, 298, 1483, 610))
LARM = dict(N=(336, 298, 435, 610), S=(475, 298, 572, 610), W=(955, 298, 1135, 610), E=(1300, 298, 1483, 610))
RLEG = dict(N=(115, 610, 235, 962), S=(673, 610, 791, 962), W=(960, 610, 1128, 962), E=(1308, 610, 1472, 962))
LLEG = dict(N=(236, 610, 356, 962), S=(556, 610, 673, 962), W=(960, 610, 1128, 962), E=(1308, 610, 1472, 962))
RHORN = dict(N=(46, 45, 130, 215), S=(785, 45, 852, 215), W=(943, 43, 1030, 215), E=(1408, 43, 1494, 215))
LHORN = dict(N=(341, 45, 425, 215), S=(493, 45, 560, 215), W=(943, 43, 1030, 215), E=(1408, 43, 1494, 215))
RPAD = dict(N=(24, 290, 136, 445), S=(775, 290, 877, 445), W=(950, 290, 1080, 445), E=(1358, 290, 1488, 445))
LPAD = dict(N=(336, 290, 447, 445), S=(470, 290, 572, 445), W=(950, 290, 1080, 445), E=(1358, 290, 1488, 445))
SKIRT = dict(N=(112, 610, 360, 775), S=(556, 610, 791, 775), W=(950, 610, 1126, 775), E=(1310, 610, 1482, 775))

def box(part, src, xyz, size, uv, inflate=0.0, rows=(0.0, 1.0), top=True, bottom=True):
    return dict(part=part, src=src, xyz=xyz, size=size, uv=uv, inflate=inflate, rows=rows, top=top, bottom=bottom)

# rows = the part of the box height (0 = top, 1 = bottom) that is painted; the rest stays see-through.
SLOTS = {
    "head": [
        box("head", HEAD, (-4, -8, -4), (8, 8, 8), (0, 0), 1.0),
        box("head", RHORN, (-7, -10, -1), (3, 7, 2), (0, 16)),
        box("head", LHORN, (4, -10, -1), (3, 7, 2), (10, 16)),
    ],
    "chest": [
        box("body", BODY, (-4, 0, -2), (8, 12, 4), (0, 0), 1.0),
        box("right_arm", RARM, (-3, -2, -2), (4, 12, 4), (24, 0), 1.0),
        box("left_arm", LARM, (-1, -2, -2), (4, 12, 4), (40, 0), 1.0),
        box("right_arm", RPAD, (-5, -4, -3), (5, 6, 6), (0, 16), 0.25),
        box("left_arm", LPAD, (0, -4, -3), (5, 6, 6), (22, 16), 0.25),
    ],
    "legs": [
        box("body", BODY, (-4, 0, -2), (8, 12, 4), (0, 0), 0.5, rows=(0.72, 1.0), top=False, bottom=False),
        box("right_leg", RLEG, (-2, 0, -2), (4, 12, 4), (24, 0), 0.5, rows=(0.0, 0.54), top=False, bottom=False),
        box("left_leg", LLEG, (-2, 0, -2), (4, 12, 4), (40, 0), 0.5, rows=(0.0, 0.54), top=False, bottom=False),
        box("body", SKIRT, (-5, 12, -3), (10, 6, 6), (0, 16), 0.0, top=False, bottom=False),
    ],
    "feet": [
        box("right_leg", RLEG, (-2, 0, -2), (4, 12, 4), (0, 0), 1.0, rows=(0.46, 1.0), top=False),
        box("left_leg", LLEG, (-2, 0, -2), (4, 12, 4), (16, 0), 1.0, rows=(0.46, 1.0), top=False),
    ],
}

def sample(rect, fu, fv):
    """Colour of the concept art at the fraction (fu, fv) of a rectangle, or None on background."""
    x0, y0, x1, y1 = rect
    px = int(min(max(x0 + fu * (x1 - x0), 0), W - 1)); py = int(min(max(y0 + fv * (y1 - y0), 0), H - 1))
    # look at a small neighbourhood so one stray pixel does not decide
    ys = slice(max(py - 1, 0), py + 2); xs = slice(max(px - 1, 0), px + 2)
    if BG[ys, xs].mean() > 0.5:
        return None
    win = im[ys, xs].reshape(-1, 3)[~BG[ys, xs].reshape(-1)]
    return win.mean(axis=0)

def paint(sheet, b):
    w, h, d = b["size"]; u, v = b["uv"]; r0, r1 = b["rows"]
    faces = {"W": (u, v + d, d, h), "N": (u + d, v + d, w, h), "E": (u + d + w, v + d, d, h), "S": (u + d + w + d, v + d, w, h)}
    for name, (fx, fy, fw, fh) in faces.items():
        for j in range(fh * K):
            fv = (j + 0.5) / (fh * K)
            if fv < r0 or fv > r1:
                continue
            for i in range(fw * K):
                c = sample(b["src"][name], (i + 0.5) / (fw * K), fv)
                if c is not None:
                    sheet[fy * K + j, fx * K + i] = (*c, 255)
    # top and bottom: the edge row of the front face, drawn across the whole depth
    for flag, (fx, fy), fv in ((b["top"], (u + d, v), 0.04), (b["bottom"], (u + d + w, v), 0.97)):
        if not flag:
            continue
        for i in range(w * K):
            c = sample(b["src"]["N"], (i + 0.5) / (w * K), fv)
            if c is None:
                continue
            sheet[fy * K: (fy + d) * K, fx * K + i] = (*c, 255)

POSES = {"head": None, "hat": None, "body": None, "right_arm": (-5, 2, 0), "left_arm": (5, 2, 0),
         "right_leg": (-1.9, 12, 0), "left_leg": (1.9, 12, 0)}

def num(x):
    return f"{float(x)}F"

java = ['''package dev.tinyrelics.client;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * The 3D shape of the necromancer armor: the usual body boxes plus horns, skull
 * shoulder pads and a skirt. Generated by tools/armor.py together with the four
 * textures, so the boxes and the pictures always match. Do not edit by hand.
 */
public final class NecroArmorModels {
    private static final Map<EquipmentSlot, HumanoidModel<LivingEntity>> MODELS = new EnumMap<>(EquipmentSlot.class);

    private NecroArmorModels() {
    }

    public static HumanoidModel<LivingEntity> get(EquipmentSlot slot) {
        return MODELS.computeIfAbsent(slot, NecroArmorModels::bake);
    }

    private static HumanoidModel<LivingEntity> bake(EquipmentSlot slot) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        switch (slot) {
''']
SLOT_ENUM = {"head": "HEAD", "chest": "CHEST", "legs": "LEGS", "feet": "FEET"}
for slot, boxes in SLOTS.items():
    sheet = np.zeros((SHEET * K, SHEET * K, 4), np.float64)
    for b in boxes:
        paint(sheet, b)
    Image.fromarray(sheet.clip(0, 255).astype(np.uint8), "RGBA").save(f"{OUT_TEX}/necromancer_{slot}.png")
    java.append(f"            case {SLOT_ENUM[slot]} -> {{\n" if slot != "feet" else "            default -> {\n")
    for part, pose in POSES.items():
        cubes = "CubeListBuilder.create()"
        for b in boxes:
            if b["part"] != part:
                continue
            x, y, z = b["xyz"]; w, h, d = b["size"]; u, v = b["uv"]
            cubes += (f"\n                        .texOffs({u}, {v}).addBox({num(x)}, {num(y)}, {num(z)}, {num(w)}, {num(h)}, {num(d)}, "
                      f"new CubeDeformation({num(b['inflate'])}))")
        pose_java = "PartPose.ZERO" if pose is None else f"PartPose.offset({num(pose[0])}, {num(pose[1])}, {num(pose[2])})"
        java.append(f'                root.addOrReplaceChild("{part}", {cubes}, {pose_java});\n')
    java.append("            }\n")
java.append(f'''        }}
        return new HumanoidModel<>(LayerDefinition.create(mesh, {SHEET}, {SHEET}).bakeRoot());
    }}
}}
''')
open(OUT_JAVA, "w", encoding="utf-8").write("".join(java))

# ---- a rough front / right / back / left preview of the assembled armor, to check the mapping
def view(direction):
    S = 12  # pixels per unit
    canvas = Image.new("RGBA", (24 * S, 40 * S), (60, 90, 60, 255))
    order = ["feet", "legs", "chest", "head"]
    for slot in order:
        tex = Image.open(f"{OUT_TEX}/necromancer_{slot}.png")
        for b in SLOTS[slot]:
            pose = POSES[b["part"]] or (0, 0, 0)
            x, y, z = b["xyz"]; w, h, d = b["size"]; u, v = b["uv"]
            gx, gy, gz = pose[0] + x, pose[1] + y, pose[2] + z
            if direction == "N":
                face = (u + d, v + d, w, h); sx = gx
            elif direction == "S":
                face = (u + d + w + d, v + d, w, h); sx = -(gx + w)
            elif direction == "W":
                face = (u, v + d, d, h); sx = -(gz + d)
            else:
                face = (u + d + w, v + d, d, h); sx = gz
            fx, fy, fw, fh = face
            piece = tex.crop((fx * K, fy * K, (fx + fw) * K, (fy + fh) * K)).resize((int(fw * S), int(fh * S)), Image.NEAREST)
            canvas.alpha_composite(piece, (int((12 + sx) * S), int((12 + gy) * S)))
    return canvas
sheet = Image.new("RGBA", (4 * 24 * 12, 40 * 12))
for k, dname in enumerate("NWSE"):
    sheet.paste(view(dname), (k * 24 * 12, 0))
sheet.save(f"{OUT_TEX}/preview.png")
print("ok")
