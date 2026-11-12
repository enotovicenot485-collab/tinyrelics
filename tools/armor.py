"""Builds the necromancer armor: paints every face of every box of the model from the
four concept views, and writes the matching Java model class."""
import sys
import numpy as np
from PIL import Image
from scipy import ndimage as ndi

SRC, OUT_TEX, OUT_JAVA = sys.argv[1], sys.argv[2], sys.argv[3]
K = 4                      # texels per model unit
SHEET = 64                 # logical texture size
im = np.array(Image.open(SRC).convert("RGB")).astype(np.float64)
H, W, _ = im.shape
# magenta background, and the purple fringe where it blends into the black outline
BG = (im[..., 0] - im[..., 1] > 30) & (im[..., 2] - im[..., 1] > 30)

# Source rectangles (x0, y0, x1, y1) in the concept image for each face of each box.
# N = front, S = back, W = the character's right side, E = the character's left side.
HEAD = dict(N=(118, 105, 353, 298), S=(560, 105, 785, 298), W=(723, 105, 779, 298), E=(566, 105, 622, 298))
BODY = dict(N=(136, 298, 336, 610), S=(572, 298, 775, 610), W=(955, 298, 1135, 610), E=(1300, 298, 1483, 610))
RARM = dict(N=(36, 450, 136, 610), S=(775, 450, 872, 610), W=(955, 450, 1135, 610), E=(1300, 450, 1483, 610))
LARM = dict(N=(336, 450, 435, 610), S=(475, 450, 572, 610), W=(955, 450, 1135, 610), E=(1300, 450, 1483, 610))
RLEG = dict(N=(115, 610, 235, 962), S=(673, 610, 791, 962), W=(960, 610, 1128, 962), E=(1308, 610, 1472, 962))
LLEG = dict(N=(236, 610, 356, 962), S=(556, 610, 673, 962), W=(960, 610, 1128, 962), E=(1308, 610, 1472, 962))
RHORN = dict(N=(46, 45, 130, 215), S=(785, 45, 852, 215), W=(943, 43, 1030, 215), E=(1408, 43, 1494, 215))
LHORN = dict(N=(341, 45, 425, 215), S=(493, 45, 560, 215), W=(943, 43, 1030, 215), E=(1408, 43, 1494, 215))
RPAD = dict(N=(24, 290, 136, 445), S=(775, 290, 877, 445), W=(950, 290, 1080, 445), E=(1358, 290, 1488, 445))
LPAD = dict(N=(336, 290, 447, 445), S=(470, 290, 572, 445), W=(950, 290, 1080, 445), E=(1358, 290, 1488, 445))
SKIRT = dict(N=(112, 610, 360, 775), S=(556, 610, 791, 775), W=(950, 610, 1126, 775), E=(1310, 610, 1482, 775))

def box(part, src, xyz, size, uv, inflate=0.0, rows=(0.0, 1.0), top=True, bottom=True, solid=True):
    """solid = holes left by the concept's background are filled in, so the body never shows through."""
    return dict(part=part, src=src, xyz=xyz, size=size, uv=uv, inflate=inflate, rows=rows, top=top, bottom=bottom, solid=solid, bone=False, skull=None)

def part_of(src, ref_xyz, ref_size, xyz, size):
    """The pictures for a small box that is a piece of a bigger reference box (a step of a horn)."""
    (rx, ry, rz), (rw, rh, rd) = ref_xyz, ref_size
    (x, y, z), (w, h, d) = xyz, size
    clamp = lambda t: min(max(t, 0.0), 1.0)
    v0, v1 = clamp((y - ry) / rh), clamp((y + h - ry) / rh)
    spans = {
        "N": (clamp((x - rx) / rw), clamp((x + w - rx) / rw)),
        "S": (clamp((rx + rw - x - w) / rw), clamp((rx + rw - x) / rw)),
        "W": (clamp((rz + rd - z - d) / rd), clamp((rz + rd - z) / rd)),
        "E": (clamp((z - rz) / rd), clamp((z + d - rz) / rd)),
    }
    out = {}
    for face, (a, b) in spans.items():
        x0, y0, x1, y1 = src[face]
        if b - a < 0.05:
            a, b = max(a - 0.2, 0.0), min(b + 0.2, 1.0)
        out[face] = (x0 + a * (x1 - x0), y0 + v0 * (y1 - y0), x0 + b * (x1 - x0), y0 + max(v1, v0 + 0.05) * (y1 - y0))
    return out

def skull(part, side, row):
    """A 3D skull sitting on the shoulder and bulging outwards: a cranium and a jaw, facing forward."""
    if side < 0:
        cranium, jaw = (-6, -6, -3.5), (-5, -2, -3.5)
    else:
        cranium, jaw = (1, -6, -3.5), (2, -2, -3.5)
    a = box(part, None, cranium, (5, 4, 5), (0, row)); a["skull"] = "cranium"
    b = box(part, None, jaw, (3, 1, 3), (20, row)); b["skull"] = "jaw"
    return [a, b]

def horn(src, ref_x, side):
    """A horn in four steps: out of the temple, up the side, in over the top, and a tip."""
    ref_xyz, ref_size = (ref_x, -10, -1), (3, 7, 2)
    if side < 0:   # the character's right
        steps = [((-8, -6, -1.5), (3, 3, 3)), ((-9, -9, -1), (2, 3, 2)), ((-8, -11, -1), (2, 2, 2)), ((-6.5, -13, -0.5), (1, 2, 1))]
        row = 16
    else:
        steps = [((5, -6, -1.5), (3, 3, 3)), ((7, -9, -1), (2, 3, 2)), ((6, -11, -1), (2, 2, 2)), ((5.5, -13, -0.5), (1, 2, 1))]
        row = 22
    columns = [0, 12, 20, 28]
    boxes = [box("head", part_of(src, ref_xyz, ref_size, xyz, size), xyz, size, (columns[k], row)) for k, (xyz, size) in enumerate(steps)]
    for k, b in enumerate(boxes):
        b["bone"] = True
        b["shade"] = 1.0 - 0.07 * (len(boxes) - 1 - k)     # a little darker towards the base
    return boxes

# rows = the part of the box height (0 = top, 1 = bottom) that is painted; the rest stays see-through.
SLOTS = {
    "head": [
        box("head", HEAD, (-4, -8, -4), (8, 8, 8), (0, 0), 1.0),
        *horn(RHORN, -7, -1),
        *horn(LHORN, 4, 1),
    ],
    "chest": [
        box("body", BODY, (-4, 0, -2), (8, 12, 4), (0, 0), 1.0),
        box("right_arm", RARM, (-3, -2, -2), (4, 12, 4), (24, 0), 1.0),
        box("left_arm", LARM, (-1, -2, -2), (4, 12, 4), (40, 0), 1.0),
        *skull("right_arm", -1, 16),
        *skull("left_arm", 1, 26),
    ],
    "legs": [
        box("body", BODY, (-4, 0, -2), (8, 12, 4), (0, 0), 0.5, rows=(0.72, 1.0), top=False, bottom=False),
        box("right_leg", RLEG, (-2, 0, -2), (4, 12, 4), (24, 0), 0.5, rows=(0.0, 0.54), top=False, bottom=False),
        box("left_leg", LLEG, (-2, 0, -2), (4, 12, 4), (40, 0), 0.5, rows=(0.0, 0.54), top=False, bottom=False),
        box("body", SKIRT, (-5, 12, -3), (10, 6, 6), (0, 16), 0.0, top=False, bottom=False, solid=False),
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

BONE = (214, 203, 172)

def fill_holes(block, fallback):
    """Every see-through texel of the block takes the colour of the nearest painted one."""
    painted = block[..., 3] > 0
    if painted.all():
        return
    if not painted.any():
        block[...] = (*fallback, 255)
        return
    idx = ndi.distance_transform_edt(~painted, return_distances=False, return_indices=True)
    block[...] = block[idx[0], idx[1]]

def edge_colour(rect, fu, from_top):
    """The first colour met going into the picture from its top (or bottom) edge."""
    steps = [t / 40.0 for t in range(1, 30)]
    for t in steps:
        c = sample(rect, fu, t if from_top else 1.0 - t)
        if c is not None and c.max() > 45:      # skip the black outline
            return c
    return None

def paint_bone(sheet, b):
    """Plain carved bone for the horns: light in the middle of each face, darker towards its edges."""
    w, h, d = b["size"]; u, v = b["uv"]
    blocks = [(u, v + d, d, h), (u + d, v + d, w, h), (u + d + w, v + d, d, h), (u + d + w + d, v + d, w, h),
              (u + d, v, w, d), (u + d + w, v, w, d)]
    light = np.array(BONE, dtype=np.float64) * b["shade"]
    for fx, fy, fw, fh in blocks:
        block = np.zeros((fh * K, fw * K, 4))
        for j in range(fh * K):
            for i in range(fw * K):
                edge = min(i, j, fw * K - 1 - i, fh * K - 1 - j)
                tone = 0.8 if edge == 0 else 0.92 if edge == 1 else 1.0
                # faint grain along the horn
                grain = 0.96 if (i + 2 * j) % 5 == 0 else 1.0
                block[j, i] = (*(light * tone * grain), 255)
        sheet[fy * K:(fy + fh) * K, fx * K:(fx + fw) * K] = block

DARK = (24, 22, 26)
SOCKET_RIM = (120, 110, 92)
CYAN = (40, 225, 235)

def paint_skull(sheet, b):
    """Draws the shoulder skull by hand: eye sockets, nose and teeth on the front, plain bone elsewhere."""
    w, h, d = b["size"]; u, v = b["uv"]
    bone = np.array(BONE, dtype=np.float64)
    def blank(fw, fh, shade=1.0):
        block = np.zeros((fh * K, fw * K, 4)); block[..., :3] = bone * shade; block[..., 3] = 255
        # darker rim so the edges of the skull read as rounded
        block[0, :, :3] *= 0.86; block[-1, :, :3] *= 0.8; block[:, 0, :3] *= 0.84; block[:, -1, :3] *= 0.84
        return block
    def put(block, fx, fy):
        sheet[fy * K:fy * K + block.shape[0], fx * K:fx * K + block.shape[1]] = block
    front = blank(w, h); left = blank(d, h, 0.9); right = blank(d, h, 0.9); back = blank(w, h, 0.82)
    top = blank(w, d, 1.04); under = blank(w, d, 0.7)
    if b["skull"] == "cranium":           # front is 20 x 16 texels
        for x0 in (3, 12):                # eye sockets
            front[5:11, x0 - 1:x0 + 6, :3] = SOCKET_RIM
            front[6:11, x0:x0 + 5, :3] = DARK
        front[11:14, 9:11, :3] = DARK     # nose
        front[11, 9:11, :3] = SOCKET_RIM
        front[2, 4:16, :3] *= 0.93        # brow line
        for block in (left, right):       # temple hollow
            block[6:11, 6:13, :3] *= 0.78
        top[:, 9:11, :3] *= 0.9           # suture along the top
        # ghost fire licking out from under the skull
        for block in (front, left, right, back):
            width = block.shape[1]
            for i in range(width):
                if i % 4 in (0, 1):
                    block[-1, i, :3] = CYAN
                if i % 8 == 0:
                    block[-2, i, :3] = CYAN
        front[-1, 6:14, :3] = bone * 0.8  # ...but not across the mouth, where the jaw hangs
        front[-2, 6:14, :3] = bone
    else:                                 # jaw: front is 12 x 4 texels, all teeth
        for i in range(0, w * K, 2):
            front[0:3, i, :3] = DARK
        front[3, :, :3] = bone * 0.75
    put(right, u, v + d); put(front, u + d, v + d); put(left, u + d + w, v + d); put(back, u + d + w + d, v + d)
    put(top, u + d, v); put(under, u + d + w, v)

def paint_top(sheet, b, fx, fy, front, crest):
    """A plain top instead of a smear: the box's own darkest tone, with the helmet's bone crest."""
    w, h, d = b["size"]
    block = np.zeros((d * K, w * K, 4))
    block[..., :3] = front; block[..., 3] = 255
    block[0, :, :3] *= 0.8; block[-1, :, :3] *= 0.8; block[:, 0, :3] *= 0.8; block[:, -1, :3] *= 0.8
    if crest:
        mid = w * K // 2
        block[:, mid - 5:mid + 5, :3] = np.array(BONE) * 0.78
        block[:, mid - 3:mid + 3, :3] = BONE
    sheet[fy * K:(fy + d) * K, fx * K:(fx + w) * K] = block

def paint(sheet, b):
    if b["bone"]:
        paint_bone(sheet, b)
        return
    if b["skull"]:
        paint_skull(sheet, b)
        return
    w, h, d = b["size"]; u, v = b["uv"]; r0, r1 = b["rows"]
    faces = {"W": (u, v + d, d, h), "N": (u + d, v + d, w, h), "E": (u + d + w, v + d, d, h), "S": (u + d + w + d, v + d, w, h)}
    front = None
    for name, (fx, fy, fw, fh) in faces.items():
        block = np.zeros((fh * K, fw * K, 4))
        j0, j1 = int(round(r0 * fh * K)), int(round(r1 * fh * K))
        for j in range(j0, j1):
            fv = (j + 0.5) / (fh * K)
            for i in range(fw * K):
                c = sample(b["src"][name], (i + 0.5) / (fw * K), fv)
                if c is not None:
                    block[j, i] = (*c, 255)
        if b["solid"]:
            fill_holes(block[j0:j1], front if front is not None else BONE)
        if name == "N" and (block[..., 3] > 0).any():
            front = tuple(block[block[..., 3] > 0][:, :3].mean(axis=0))
        sheet[fy * K:(fy + fh) * K, fx * K:(fx + fw) * K] = block
    if b["top"] and b["solid"]:
        # the steel of this box: the average of its darker half on the front
        n = sheet[(v + d) * K:(v + d + h) * K, (u + d) * K:(u + d + w) * K]
        px = n[n[..., 3] > 0][:, :3]
        steel = px[px.sum(axis=1) <= np.median(px.sum(axis=1))].mean(axis=0) if len(px) else np.array(DARK)
        paint_top(sheet, b, u + d, v, steel * 1.15, crest=(b["part"] == "head"))
        if b["bottom"]:
            paint_top(sheet, b, u + d + w, v, steel * 0.8, crest=False)
    # bottom: the colour just inside the front picture's lower edge, drawn across the whole depth
    for flag, (fx, fy), from_top in ((b["top"] and not b["solid"], (u + d, v), True), (b["bottom"] and not b["solid"], (u + d + w, v), False)):
        if not flag:
            continue
        block = np.zeros((d * K, w * K, 4))
        for i in range(w * K):
            c = edge_colour(b["src"]["N"], (i + 0.5) / (w * K), from_top)
            if c is not None:
                block[:, i] = (*c, 255)
        if b["solid"]:
            fill_holes(block, front if front is not None else BONE)
        sheet[fy * K:(fy + d) * K, fx * K:(fx + w) * K] = block

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
    r_, g_, b_, a_ = [sheet[..., i] for i in range(4)]
    cyan = (a_ > 0) & (g_ > 135) & (b_ > 135) & (b_ - r_ > 55) & (g_ - r_ > 40)
    glow = np.zeros_like(sheet)
    halo = ndi.binary_dilation(cyan, iterations=1) & ~cyan & (a_ > 0)
    glow[halo] = (30, 110, 120, 255)
    glow[cyan, :3] = sheet[cyan, :3] * 0.35 + np.array([235.0, 255.0, 255.0]) * 0.65
    glow[cyan, 3] = 255
    Image.fromarray(glow.clip(0, 255).astype(np.uint8), "RGBA").save(f"{OUT_TEX}/necromancer_{slot}_glow.png")
    print(slot, "glowing texels", int(cyan.sum()))
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
