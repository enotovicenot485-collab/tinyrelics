"""Builds the Storm God armor for Thor's hammer (same method as armor_blood.py, see docs/ARMOR.md): paints every face of every box of the model from the
four concept views, and writes the matching Java model class."""
import sys
import numpy as np
from PIL import Image
from scipy import ndimage as ndi

SRC, OUT_TEX, OUT_JAVA = sys.argv[1], sys.argv[2], sys.argv[3]
K = 4                      # texels per model unit
SHEET = 96                 # logical texture size: the cloak and the fur need more room than 64
FRAMES = 32                # frames of the glow animation; the game shows one every 2 ticks, a 3.2 s loop
im = np.array(Image.open(SRC).convert("RGB")).astype(np.float64)
H, W, _ = im.shape
# magenta background (fur, blue cloth and blue glow would all be lost on green or blue)
BG = (im[..., 0] - im[..., 1] > 28) & (im[..., 2] - im[..., 1] > 28)

# Source rectangles (x0, y0, x1, y1) in the concept image for each face of each box.
# N = front, S = back, W = the character's right side, E = the character's left side.
# No helmet in this set. The back of the torso is under the cloak in the drawing, so it borrows scale mail from the front.
BODY = dict(N=(150, 290, 330, 545), S=(160, 405, 320, 480), W=(160, 405, 230, 480), E=(250, 405, 320, 480))
BELT = dict(N=(150, 485, 330, 548), S=(150, 488, 205, 545), W=(150, 488, 205, 545), E=(275, 488, 330, 545))
BUCKLE = dict.fromkeys("NSWE", (205, 480, 275, 550))
RUNE = dict.fromkeys("NSWE", (200, 352, 280, 458))
BROOCH_R = dict.fromkeys("NSWE", (140, 300, 190, 350))
BROOCH_L = dict.fromkeys("NSWE", (285, 300, 335, 350))
CHAIN = dict.fromkeys("NSWE", (188, 312, 290, 352))
# (the arm starts under the fur; its last rows are the mannequin's bare hand and are left unpainted)
RARM = dict(N=(58, 290, 135, 572), S=(765, 290, 822, 590), W=(960, 290, 1045, 572), E=(1318, 290, 1403, 572))
LARM = dict(N=(348, 290, 425, 572), S=(478, 290, 535, 590), W=(960, 290, 1045, 572), E=(1318, 290, 1403, 572))
RLEG = dict(N=(130, 545, 238, 935), S=(955, 545, 1065, 935), W=(955, 545, 1065, 935), E=(1300, 545, 1408, 935))
LLEG = dict(N=(243, 545, 351, 935), S=(1300, 545, 1408, 935), W=(955, 545, 1065, 935), E=(1300, 545, 1408, 935))
# the fur mantle: over a shoulder as seen from each side, and the whole collar from behind
FUR = dict(N=(46, 238, 196, 405), S=(672, 245, 826, 410), W=(893, 250, 1102, 395), E=(1262, 250, 1470, 395))
FUR_BACK = dict.fromkeys("NSWE", (530, 245, 770, 432))
FUR_CHEST = dict.fromkeys("NSWE", (150, 250, 215, 405))
CLOAK = dict.fromkeys("NSWE", (498, 372, 806, 866))
# the leather kilt with its silver points, front and side
KILT = dict(N=(138, 545, 240, 695), S=(138, 545, 240, 695), W=(952, 545, 1075, 700), E=(1290, 545, 1410, 700))
CUFF = dict(N=(128, 742, 240, 795), S=(128, 742, 240, 795), W=(950, 742, 1070, 795), E=(1296, 742, 1412, 795))

def box(part, src, xyz, size, uv, inflate=0.0, rows=(0.0, 1.0), top=True, bottom=True, solid=True, lift=False):
    """lift = the rows above the painted part repeat its first row (the shoulder under the pauldron)."""
    """solid = holes left by the concept's background are filled in, so the body never shows through."""
    return dict(part=part, src=src, xyz=xyz, size=size, uv=uv, inflate=inflate, rows=rows, top=top, bottom=bottom, solid=solid, bone=False, skull=None, relief=None, shade=1.0, ragged=False, lift=lift, colour=None, fx=None)

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

def relief(part, src, ref_xyz, ref_size, xyz, size, face="N", ragged=False):
    """A raised plate: one face shows its share of the concept picture, the thin sides continue its edges.
    ragged = keep the concept's own outline (see-through where it had background) instead of a full rectangle."""
    b = box(part, part_of(src, ref_xyz, ref_size, xyz, size), xyz, size, None)
    b["relief"] = face
    b["ragged"] = ragged
    return b

def tuft(*args, **kwargs):
    """A ragged plate of fur: the drawing's own tufts, without its black outline."""
    b = relief(*args, ragged=True, **kwargs); b["no_outline"] = True
    return b

STEEL = (178, 178, 196)
CLOTH = (44, 40, 54)
GOLD = (190, 146, 66)

def tone_box(part, xyz, size, colour, shade=1.0, inflate=0.0):
    """A box of one plain material: blades are steel, the top of the hood is cloth."""
    b = box(part, None, xyz, size, None, inflate); b["bone"] = True; b["shade"] = shade; b["colour"] = colour
    return b

def mirror(b):
    """The same box on the other arm or leg. Right and left boxes are mirror images about x = 0."""
    m = dict(b)
    x, y, z = b["xyz"]
    m["xyz"] = (-(x + b["size"][0]), y, z)
    m["part"] = b["part"].replace("right", "left")
    if b["relief"] in ("W", "E"):
        m["relief"] = "E" if b["relief"] == "W" else "W"
    return m

HEAD_BOX, ARM_R, ARM_L, LEG = ((-4, -8, -4), (8, 8, 8)), ((-3, -2, -2), (4, 12, 4)), ((-1, -2, -2), (4, 12, 4)), ((-2, 0, -2), (4, 12, 4))
BODY_BOX = ((-4, 0, -2), (8, 12, 4))

def flip(rects, front_centre=240, back_centre=649):
    """The same rectangles for the other side of the body: mirrored in the front and back views.
    The side views stay as they are: a mirrored box shows the left view on its left face by itself."""
    (a, b, c, d), (e, f, g, h) = rects["N"], rects["S"]
    return dict(N=(2 * front_centre - c, b, 2 * front_centre - a, d), S=(2 * back_centre - g, f, 2 * back_centre - e, h), W=rects["W"], E=rects["E"])

def stepped(part, rects, xyz, depth, side, unit=24.5, min_lum=30):
    """A solid shape that follows a drawing's outline: one box for every unit of height, as wide as
    the drawing is on that row, carrying its slice of the picture on the front and back.
    Built for the right side; for the left the mirrored picture is used and mirror() moves the box."""
    x0, y0, x1, y1 = rects["N"]
    out = []
    for row in range(int(round((y1 - y0) / unit))):
        ya, yb = int(y0 + row * unit), int(y0 + (row + 1) * unit)
        # the drawing's black outline is not part of the shape: it would only make the blade fatter
        # nor are the dark shadows drawn beside the blade: only the steel itself counts
        metal = ~BG[ya:yb, x0:x1] & (im[ya:yb, x0:x1].mean(-1) > min_lum)
        cols = np.where(metal.sum(0) > (yb - ya) * 0.25)[0]
        if len(cols) == 0:
            continue
        pa, pb = x0 + cols[0], x0 + cols[-1] + 1
        width = max(1, int(round((pb - pa) / unit)))
        centre = xyz[0] + ((pa + pb) / 2 - x0) / unit
        rect = dict(N=(pa, ya, pb, yb))
        if side > 0:
            rect = flip(dict(rect, S=rect["N"], W=rect["N"], E=rect["N"]))
        rect = {k: rect["N"] for k in "NSWE"}
        b = box(part, rect, (round(centre - width / 2, 2), xyz[1] + row, xyz[2]), (width, 1, depth), None)
        b["relief"] = "N"
        out.append(b)
    return out

FUR_WHITE = (222, 220, 214)

def fur_box(part, xyz, size, shade=1.0):
    """A lump of plain fur: light and dark clumps instead of one flat colour."""
    b = tone_box(part, xyz, size, FUR_WHITE, shade); b["fur"] = True
    return b

def fur(side):
    """A shoulder of the mantle. A thick roll of fur over the arm, painted from all four views; lumps of
    different height on top of it so it is not a brick; and a ragged fringe hanging from it at the
    front, the side and the back, cut out along the tufts of the drawing."""
    rects = FUR if side < 0 else flip(FUR)
    core = ((-5.4, -4.6, -3.6), (7, 5, 7))
    boxes = [
        # the body of the fur is plain fur; the drawing goes on plates laid over each side of it
        fur_box("right_arm", (-5.0, -4.2, -3.2), (6, 4, 6), 0.95),
        fur_box("right_arm", (-4.7, -5.1, -2.9), (4, 1, 3), 1.0),
        fur_box("right_arm", (-2.6, -5.0, -0.6), (3, 1, 3), 0.9),
        fur_box("right_arm", (-3.9, -5.9, -1.9), (2, 1, 2), 1.05),
        fur_box("right_arm", (-5.8, -3.3, -1.4), (1, 2, 3), 0.9),
        tuft("right_arm", rects, core[0], (7, 7, 7), (-5.4, -4.6, -4.2), (7, 7, 1), "N"),
        tuft("right_arm", rects, core[0], (7, 7, 7), (-6.0, -4.6, -3.6), (1, 7, 7), "W"),
        tuft("right_arm", rects, core[0], (7, 7, 7), (-5.4, -4.6, 3.2), (7, 7, 1), "S"),
    ]
    return boxes if side < 0 else [mirror(b) for b in boxes]

def bracer(side):
    rects, part, ref, x = (RARM, "right_arm", ARM_R, -3) if side < 0 else (LARM, "left_arm", ARM_L, -1)
    return dict(box(part, part_of(rects, *ref, (x, 4, -2), (4, 4, 4)), (x, 4, -2), (4, 4, 4), None, 1.0), fx="bracer")

def kilt(side):
    """Leather straps with silver points hanging from the belt: on the legs, so they swing with them."""
    rects = KILT if side < 0 else flip(KILT)
    boxes = [
        relief("right_leg", rects, (-2.6, 0, -3.2), (5, 7, 1), (-2.6, 0, -3.2), (5, 7, 1), "N", ragged=True),
        relief("right_leg", rects, (-3.3, 0, -3), (1, 7, 6), (-3.3, 0, -3), (1, 7, 6), "W", ragged=True),
    ]
    return boxes if side < 0 else [mirror(b) for b in boxes]

def cuff(side):
    """The fur round the top of a boot: a thick band, and a ragged fringe over the lacing."""
    rects, part, x = (CUFF, "right_leg", -3.0) if side < 0 else (flip(CUFF), "left_leg", -2.0)
    return [
        dict(box(part, rects, (x, 5.6, -2.5), (5, 2, 5), None, 0.25), bottom=True),
        tuft(part, rects, (x, 5.6, -3.1), (5, 3, 1), (x, 6.4, -3.1), (5, 2, 1), "N"),
    ]

HEAD_BOX, ARM_R, ARM_L, LEG = ((-4, -8, -4), (8, 8, 8)), ((-3, -2, -2), (4, 12, 4)), ((-1, -2, -2), (4, 12, 4)), ((-2, 0, -2), (4, 12, 4))
BODY_BOX = ((-4, 0, -2), (8, 12, 4))
# The cloak is its own part hanging from the back of the neck, in two pieces so that it can bend:
# ThorCloak turns them every frame. Their boxes are measured from their own hinges.
CLOAK_REF = ((-7, 0, 0), (14, 23, 1))
def cloak_piece(part, from_row, rows):
    b = relief(part, CLOAK, *CLOAK_REF, (-7, from_row, 0), (14, rows, 1), "S", ragged=True)
    b["xyz"] = (-7, 0, 0)
    return b

# rows = the part of the box height (0 = top, 1 = bottom) that is painted; the rest stays see-through.
SLOTS = {
    "chest": [
        box("body", BODY, (-4, 0, -2), (8, 12, 4), (0, 0), 0.8),
        box("right_arm", RARM, (-3, -2, -2), (4, 12, 4), (24, 0), 0.6, rows=(0.3, 0.9), lift=True, bottom=False),
        box("left_arm", LARM, (-1, -2, -2), (4, 12, 4), (40, 0), 0.6, rows=(0.3, 0.9), lift=True, bottom=False),
        *fur(-1), *fur(1),
        bracer(-1), bracer(1),
        # the mantle behind the neck: a roll of fur across the back and the long tufts hanging from it
        fur_box("body", (-5, -2.2, 1.3), (10, 3, 3), 0.95),
        fur_box("body", (-4, -3.0, 1.7), (3, 1, 2), 1.0),
        fur_box("body", (0.5, -2.9, 2.1), (3, 1, 2), 0.9),
        tuft("body", FUR_BACK, (-5.5, -2, 3.7), (11, 8, 1), (-5.5, -2, 3.7), (11, 8, 1), "S"),
        # fur coming down over the chest beside the neck, the two brooches and the chain between them
        tuft("body", FUR_CHEST, (-4.6, -1.6, -3.7), (3, 7, 1), (-4.6, -1.6, -3.7), (3, 7, 1), "N"),
        tuft("body", flip(FUR_CHEST), (1.6, -1.6, -3.7), (3, 7, 1), (1.6, -1.6, -3.7), (3, 7, 1), "N"),
        relief("body", BROOCH_R, (-4.5, 0.4, -4.4), (2, 2, 1), (-4.5, 0.4, -4.4), (2, 2, 1), "N", ragged=True),
        relief("body", BROOCH_L, (2.5, 0.4, -4.4), (2, 2, 1), (2.5, 0.4, -4.4), (2, 2, 1), "N", ragged=True),
        relief("body", CHAIN, (-2.5, 0.9, -4.0), (5, 2, 1), (-2.5, 0.9, -4.0), (5, 2, 1), "N", ragged=True),
        # the rune plate, the belt as a band of its own and the round buckle on it
        dict(relief("body", RUNE, (-2, 3, -3.5), (4, 5, 1), (-2, 3, -3.5), (4, 5, 1), "N"), fx="rune"),
        box("body", BELT, (-4.5, 9.2, -2.6), (9, 3, 5), None, 0.45),
        relief("body", BUCKLE, (-1.5, 9.2, -4.2), (3, 3, 1), (-1.5, 9.2, -4.2), (3, 3, 1), "N", ragged=True),
        cloak_piece("cloak", 0, 11), cloak_piece("cloak_low", 11, 12),
    ],
    "legs": [
        box("body", BODY, (-4, 0, -2), (8, 12, 4), (0, 0), 0.5, rows=(0.76, 1.0), top=False, bottom=False),
        box("right_leg", RLEG, (-2, 0, -2), (4, 12, 4), (24, 0), 0.5, rows=(0.0, 0.62), top=False, bottom=False),
        box("left_leg", LLEG, (-2, 0, -2), (4, 12, 4), (40, 0), 0.5, rows=(0.0, 0.62), top=False, bottom=False),
        *kilt(-1), *kilt(1),
    ],
    "feet": [
        box("right_leg", RLEG, (-2, 0, -2), (4, 12, 4), (0, 0), 1.0, rows=(0.5, 1.0), top=False),
        box("left_leg", LLEG, (-2, 0, -2), (4, 12, 4), (16, 0), 1.0, rows=(0.5, 1.0), top=False),
        *cuff(-1), *cuff(1),
        relief("right_leg", RLEG, *LEG, (-2, 10.4, -3.9), (4, 2, 1)),
        relief("left_leg", LLEG, *LEG, (-2, 10.4, -3.9), (4, 2, 1)),
    ],
}

def place_boxes(boxes):
    """Finds a free spot on the 64x64 sheet for every box that has no fixed place yet."""
    used = np.zeros((SHEET, SHEET), bool)
    def footprint(b):
        w, h, d = b["size"]
        return 2 * (w + d), d + h
    for b in boxes:
        if b["uv"] is not None:
            fw, fh = footprint(b); u, v = b["uv"]
            used[v:v + fh, u:u + fw] = True
    for b in boxes:
        if b["uv"] is not None:
            continue
        fw, fh = footprint(b)
        spot = None
        for v in range(SHEET - fh + 1):
            for u in range(SHEET - fw + 1):
                if not used[v:v + fh, u:u + fw].any():
                    spot = (u, v); break
            if spot:
                break
        assert spot, ("texture sheet is full", b["part"], b["size"])
        b["uv"] = spot
        used[spot[1]:spot[1] + fh, spot[0]:spot[0] + fw] = True

for _boxes in SLOTS.values():
    place_boxes(_boxes)

def check_planes():
    """Two faces of the same body part in the same plane flicker in game. Report them."""
    for slot, boxes in SLOTS.items():
        for i, a in enumerate(boxes):
            for b in boxes[i + 1:]:
                if a["part"] != b["part"]:
                    continue
                lo = lambda q: [q["xyz"][k] - q["inflate"] for k in range(3)]
                hi = lambda q: [q["xyz"][k] + q["size"][k] + q["inflate"] for k in range(3)]
                for axis in range(3):
                    others = [k for k in range(3) if k != axis]
                    overlap = all(min(hi(a)[k], hi(b)[k]) - max(lo(a)[k], lo(b)[k]) > 0.01 for k in others)
                    for pa in (lo(a)[axis], hi(a)[axis]):
                        for pb in (lo(b)[axis], hi(b)[axis]):
                            if overlap and abs(pa - pb) < 0.05:
                                print("  SAME PLANE", slot, a["part"], "xyz"[axis], round(pa, 2), a["xyz"], a["size"], "and", b["xyz"], b["size"])

check_planes()

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

BONE = (40, 36, 50)       # what fills a hole when nothing better is known: dark cloth

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
    """One plain material: light in the middle of each face, darker towards its edges."""
    w, h, d = b["size"]; u, v = b["uv"]
    blocks = [(u, v + d, d, h), (u + d, v + d, w, h), (u + d + w, v + d, d, h), (u + d + w + d, v + d, w, h),
              (u + d, v, w, d), (u + d + w, v, w, d)]
    light = np.array(b["colour"], dtype=np.float64) * b["shade"]
    for fx, fy, fw, fh in blocks:
        block = np.zeros((fh * K, fw * K, 4))
        for j in range(fh * K):
            for i in range(fw * K):
                edge = min(i, j, fw * K - 1 - i, fh * K - 1 - j)
                tone = 0.8 if edge == 0 else 0.92 if edge == 1 else 1.0
                if b["colour"] == STEEL:      # a blade: dark outline, bright metal inside, a glint across it
                    tone = 0.6 if edge == 0 else 1.12 if (i + j) % 7 == 3 else 0.95
                grain = 1.0
                if b.get("fur"):
                    # clumps two texels wide with a dark gap here and there, like the tufts in the drawing
                    n = ((i // 2) * 7 + (j // 2) * 13 + (i // 2) * (j // 2) * 3 + fx * 5 + fy * 11) % 9
                    tone = (0.72, 1.0, 0.9, 1.05, 0.82, 1.0, 0.95, 0.66, 1.02)[n]
                block[j, i] = (*(light * tone * grain), 255)
        sheet[fy * K:(fy + fh) * K, fx * K:(fx + fw) * K] = block

DARK = (24, 22, 26)

def paint_relief(sheet, b):
    """A raised plate: the concept picture on its main face (and its mirror image on the opposite one),
    and the colours of the picture's edges carried round the remaining thin sides."""
    w, h, d = b["size"]; u, v = b["uv"]; main_face = b["relief"]
    across = w if main_face in ("N", "S") else d          # width of the main face in model units
    main = np.zeros((h * K, across * K, 4))
    for j in range(h * K):
        for i in range(across * K):
            c = sample(b["src"][main_face], (i + 0.5) / (across * K), (j + 0.5) / (h * K))
            if c is not None:
                main[j, i] = (*c, 255)
    if not b["ragged"]:
        fill_holes(main, BONE)
    if b.get("no_outline"):
        # fur and other soft edges: the drawing's black outline would hang in the air as a wire frame
        dark = main[..., :3].mean(-1) < 46
        main[dark] = 0
    if b.get("bright_above"):
        # The points of the crown: above the band keep only gold and ruby. The drawing also shows
        # the far side's points and black outlines there, and with those the crown turns into a brush.
        rows = int(b["bright_above"] * h * K)
        r_, g_, b_ = main[:rows, :, 0], main[:rows, :, 1], main[:rows, :, 2]
        main[:rows][~((r_ > 105) & (r_ - b_ > 40))] = 0
    def put(block, fx, fy):
        sheet[fy * K:fy * K + block.shape[0], fx * K:fx * K + block.shape[1]] = block
    def strip(column, width):     # a thin side: every row takes the colour of the picture's edge, a little darker
        block = np.repeat(column[:, None, :], width * K, axis=1).copy(); block[..., :3] *= 0.78
        return block
    mirrored = main[:, ::-1]
    top = np.zeros((d * K, w * K, 4)); bottom = np.zeros((d * K, w * K, 4))
    if main_face in ("N", "S"):
        if main_face == "N":
            north, south, left_edge, right_edge, top_row, bottom_row = main, mirrored, main[:, 0], main[:, -1], main[0], main[-1]
        else:
            north, south, left_edge, right_edge, top_row, bottom_row = mirrored, main, main[:, -1], main[:, 0], main[0, ::-1], main[-1, ::-1]
        west, east = strip(left_edge, d), strip(right_edge, d)
        top[:] = top_row[None, :, :]; bottom[:] = bottom_row[None, :, :]
    else:
        # On the west face the picture runs back-to-front, on the east face front-to-back.
        if main_face == "W":
            west, east, back_edge, front_edge, top_row, bottom_row = main, mirrored, main[:, 0], main[:, -1], main[0], main[-1]
        else:
            west, east, back_edge, front_edge, top_row, bottom_row = mirrored, main, main[:, -1], main[:, 0], main[0, ::-1], main[-1, ::-1]
        north, south = strip(front_edge, w), strip(back_edge, w)
        top[:] = top_row[:, None, :]; bottom[:] = bottom_row[:, None, :]      # rows of the top run back to front
    top[..., :3] *= 0.9; bottom[..., :3] *= 0.7
    put(west, u, v + d); put(north, u + d, v + d); put(east, u + d + w, v + d); put(south, u + d + w + d, v + d)
    put(top, u + d, v); put(bottom, u + d + w, v)

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
    if b["relief"]:
        paint_relief(sheet, b)
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
        if b["lift"]:
            block[:j0] = block[j0][None, :, :]
        if name == "N" and (block[..., 3] > 0).any():
            front = tuple(block[block[..., 3] > 0][:, :3].mean(axis=0))
        if name == "N" and b.get("slots"):
            # The face opening, like on a vanilla helmet: the black part of the drawing (the band with
            # the eyes and the strip running down from it between the tears) is cut out of this shell,
            # so only the metal round it stands forward and the eyes sit deeper, on the box underneath.
            rows, cols = block.shape[:2]
            hole = np.zeros((rows, cols), bool)
            hole[int(0.26 * rows):int(0.50 * rows), int(0.09 * cols):int(0.91 * cols)] = True
            hole[int(0.26 * rows):, int(0.345 * cols):int(0.655 * cols)] = True      # between the tears, which stay on the metal
            rim = ndi.binary_dilation(hole, iterations=1) & ~hole
            block[rim, :3] = block[rim, :3] * 0.5 + np.array([58, 52, 56]) * 0.5
            block[hole] = 0
        if b.get("dark"):
            # the inside of the helm: darkness, and in front only the glowing eyes
            keep = np.zeros(block.shape[:2], bool)
            if name == "N":
                top, low = int(0.2 * fh * K), int(0.52 * fh * K)
                keep[top:low] = (block[top:low, :, 0] > 140) & (block[top:low, :, 0] - block[top:low, :, 2] > 60)
            block[~keep] = (9, 7, 9, 255)
        sheet[fy * K:(fy + fh) * K, fx * K:(fx + fw) * K] = block
    if (b["top"] or b["bottom"]) and b["solid"]:
        # the steel of this box: the average of its darker half on the front
        n = sheet[(v + d) * K:(v + d + h) * K, (u + d) * K:(u + d + w) * K]
        px = n[n[..., 3] > 0][:, :3]
        steel = px[px.sum(axis=1) <= np.median(px.sum(axis=1))].mean(axis=0) if len(px) else np.array(DARK)
        if b["top"]:
            paint_top(sheet, b, u + d, v, steel * 1.15, crest=False)
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
# parts that hang from another part: name -> (parent, hinge measured from the parent's own hinge)
CHILDREN = {"cloak": ("body", (0, 0.4, 3.0)), "cloak_low": ("cloak", (0, 11, 0))}

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
 * The 3D shape of the Storm God armor: the usual body boxes plus a fur mantle,
 * brooches on a chain, a belt, a kilt, fur-topped boots and a cloak in two hinged
 * pieces. Generated by tools/armor_thor.py together with the textures. Do not edit by hand.
 */
public final class ThorArmorModels {
    private static final Map<EquipmentSlot, HumanoidModel<LivingEntity>> MODELS = new EnumMap<>(EquipmentSlot.class);

    private ThorArmorModels() {
    }

    public static HumanoidModel<LivingEntity> get(EquipmentSlot slot) {
        return MODELS.computeIfAbsent(slot, ThorArmorModels::bake);
    }

    private static HumanoidModel<LivingEntity> bake(EquipmentSlot slot) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        switch (slot) {
''']
SLOT_ENUM = {"head": "HEAD", "chest": "CHEST", "legs": "LEGS", "feet": "FEET"}
# for the preview: where every hanging part ends up, measured from the model's origin
ABSOLUTE = dict(POSES, cloak=(0, 0.4, 3.0), cloak_low=(0, 11.4, 3.0))
for slot, boxes in SLOTS.items():
    sheet = np.zeros((SHEET * K, SHEET * K, 4), np.float64)
    for b in boxes:
        paint(sheet, b)
    Image.fromarray(sheet.clip(0, 255).astype(np.uint8), "RGBA").save(f"{OUT_TEX}/thor_{slot}.png")
    r_, g_, b_, a_ = [sheet[..., i] for i in range(4)]
    cyan = (a_ > 0) & (b_ > 200) & (b_ - r_ > 90) & (g_ > 105)      # the blue glow of the runes
    glow = np.zeros_like(sheet)
    halo = ndi.binary_dilation(cyan, iterations=1) & ~cyan & (a_ > 0)
    glow[halo] = (20, 60, 150, 255)
    glow[cyan, :3] = sheet[cyan, :3] * 0.35 + np.array([215.0, 240.0, 255.0]) * 0.65
    glow[cyan, 3] = 255
    Image.fromarray(glow.clip(0, 255).astype(np.uint8), "RGBA").save(f"{OUT_TEX}/thor_{slot}_glow.png")
    yy, xx = np.indices(cyan.shape)
    # Every glowing detail has its own life, and the two of a pair (shoulders, gauntlets, knees,
    # ankles) always light up together because they share a kind.
    KINDS = ["other", "rune", "bracer"]
    kind = np.zeros(cyan.shape, int)
    relx = np.zeros(cyan.shape); rely = np.zeros(cyan.shape)
    for b in boxes:
        w, h, d = b["size"]; u, v = b["uv"]
        fx = b.get("fx") or ("bracer" if "arm" in b["part"] else "other")
        area = (slice(v * K, (v + d + h) * K), slice(u * K, (u + 2 * (w + d)) * K))
        kind[area] = KINDS.index(fx)
        relx[area] = np.clip((xx[area] - (u + d) * K) / (w * K), 0, 1)
        rely[area] = np.clip((yy[area] - (v + d) * K) / (h * K), 0, 1)
    def bump(t, at, width):
        gap = np.abs((t - at + 0.5) % 1.0 - 0.5)
        return np.exp(-(gap / width) ** 2)
    for f in range(FRAMES):
        t = f / FRAMES
        waves = {
            # the rune on the chest crackles like lightning: two quick flashes running up it, then a slow glow
            "rune": np.maximum.reduce([bump(t + 0.06 * rely, 0.10, 0.02), 0.8 * bump(t + 0.06 * rely, 0.17, 0.02),
                                       0.45 * bump(t, 0.62, 0.14) * np.ones(cyan.shape)]),
            # the runes on both arms answer it a moment later, together
            "bracer": np.maximum(bump(t, 0.26, 0.05), 0.5 * bump(t, 0.78, 0.10)) * np.ones(cyan.shape),
            "other": 0.5 * bump(t, 0.5, 0.2) * np.ones(cyan.shape),
        }
        wave = np.zeros(cyan.shape)
        for k, name in enumerate(KINDS):
            wave[kind == k] = (0.14 + 0.86 * waves[name])[kind == k]
        frame = sheet.copy()
        # the eyes are almost white in the drawing: at rest they are dimmed more, so the flare is seen
        # at rest a detail keeps the drawing's own red (the eyes a little dimmer); a wave heats it towards white
        rest = np.full(cyan.shape, 0.7)[..., None]
        lit = frame[..., :3] * (rest + (1 - rest) * wave[..., None])
        heat = (np.clip((wave - 0.3) / 0.7, 0, 1) * 0.72)[..., None]
        lit = lit * (1 - heat) + np.array([225.0, 245.0, 255.0]) * heat
        frame[cyan, :3] = lit[cyan]
        Image.fromarray(frame.clip(0, 255).astype(np.uint8), "RGBA").save(f"{OUT_TEX}/thor_{slot}_{f}.png")
    print(slot, "glowing texels", int(cyan.sum()))
    java.append(f"            case {SLOT_ENUM[slot]} -> {{\n" if slot != "feet" else "            default -> {\n")
    def cubes_of(part):
        cubes = "CubeListBuilder.create()"
        for b in boxes:
            if b["part"] != part:
                continue
            x, y, z = b["xyz"]; w, h, d = b["size"]; u, v = b["uv"]
            cubes += (f"\n                        .texOffs({u}, {v}).addBox({num(x)}, {num(y)}, {num(z)}, {num(w)}, {num(h)}, {num(d)}, "
                      f"new CubeDeformation({num(b['inflate'])}))")
        return cubes
    for part, pose in POSES.items():
        pose_java = "PartPose.ZERO" if pose is None else f"PartPose.offset({num(pose[0])}, {num(pose[1])}, {num(pose[2])})"
        java.append(f'                PartDefinition {part} = root.addOrReplaceChild("{part}", {cubes_of(part)}, {pose_java});\n')
    for part, (parent, hinge) in CHILDREN.items():
        if any(b["part"] == part for b in boxes):
            java.append(f'                PartDefinition {part} = {parent}.addOrReplaceChild("{part}", {cubes_of(part)}, '
                        f'PartPose.offset({num(hinge[0])}, {num(hinge[1])}, {num(hinge[2])}));\n')
    java.append("            }\n")
java.append(f'''        }}
        return new HumanoidModel<>(LayerDefinition.create(mesh, {SHEET}, {SHEET}).bakeRoot());
    }}
}}
''')
open(OUT_JAVA, "w", encoding="utf-8").write("".join(java))

# every box with its place on the sheet, for tools/preview3d.py
import json
json.dump({"prefix": "thor_", "sheet": SHEET, "texels": K, "poses": ABSOLUTE,
           "slots": {slot: [{k: b[k] for k in ("part", "xyz", "size", "uv", "inflate")} for b in boxes] for slot, boxes in SLOTS.items()}},
          open(f"{OUT_TEX}/boxes.json", "w"))

# ---- a rough front / right / back / left preview of the assembled armor, to check the mapping
def view(direction):
    S = 12  # pixels per unit
    canvas = Image.new("RGBA", (24 * S, 40 * S), (60, 90, 60, 255))
    order = ["feet", "legs", "chest"]
    for slot in order:
        tex = Image.open(f"{OUT_TEX}/thor_{slot}.png")
        for b in SLOTS[slot]:
            pose = ABSOLUTE[b["part"]] or (0, 0, 0)
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
