"""Builds the Blood Master armor (same method as armor_chaos.py, see docs/ARMOR.md): paints every face of every box of the model from the
four concept views, and writes the matching Java model class."""
import sys
import numpy as np
from PIL import Image
from scipy import ndimage as ndi

SRC, OUT_TEX, OUT_JAVA = sys.argv[1], sys.argv[2], sys.argv[3]
K = 4                      # texels per model unit
SHEET = 64                 # logical texture size
FRAMES = 8                 # frames of the glow: for now one slow breath, the game shows a frame every 2 ticks
im = np.array(Image.open(SRC).convert("RGB")).astype(np.float64)
H, W, _ = im.shape
# green background (the armor glows red), and its fringe on the outline
BG = (im[..., 1] - im[..., 0] > 40) & (im[..., 1] - im[..., 2] > 40)

# Source rectangles (x0, y0, x1, y1) in the concept image for each face of each box.
# N = front, S = back, W = the character's right side, E = the character's left side.
# The crown stands on top of the helm, so the helm takes the picture below the crown's band.
# the sides of the helm are plain, like its back: in the side views the eyes wrap round the corner
FACE = dict(N=(160, 138, 390, 272), S=(648, 140, 862, 245), W=(648, 140, 862, 245), E=(648, 140, 862, 245))
CROWN = dict(N=(153, 18, 397, 165), S=(643, 18, 867, 140), W=(1062, 22, 1258, 140), E=(1494, 22, 1693, 140))
# the side walls of the crown run between the front and the back wall: the middle eight units of the side views
CROWN = dict(CROWN, W=(1062 + 20, 22, 1258 - 20, 135), E=(1494 + 20, 22, 1693 - 20, 135), S=(643, 18, 867, 135))
BODY = dict(N=(178, 272, 372, 512), S=(672, 250, 842, 512), W=(1100, 290, 1245, 545), E=(1520, 290, 1665, 545))
ORB = dict(N=(236, 300, 314, 390), S=(236, 300, 314, 390), W=(236, 300, 314, 390), E=(236, 300, 314, 390))
GEM = dict(N=(248, 452, 302, 532), S=(248, 452, 302, 532), W=(248, 452, 302, 532), E=(248, 452, 302, 532))
RARM = dict(N=(72, 290, 172, 570), S=(852, 290, 946, 570), W=(1100, 290, 1245, 560), E=(1520, 290, 1665, 560))
LARM = dict(N=(378, 290, 478, 570), S=(568, 290, 662, 570), W=(1100, 290, 1245, 560), E=(1520, 290, 1665, 560))
RLEG = dict(N=(150, 512, 268, 860), S=(762, 512, 874, 860), W=(1105, 512, 1235, 860), E=(1525, 512, 1655, 860))
LLEG = dict(N=(282, 512, 400, 860), S=(640, 512, 752, 860), W=(1105, 512, 1235, 860), E=(1525, 512, 1655, 860))
# the shoulder as seen from each side, and the claw that curves up from its outer edge
PAD = dict(N=(42, 240, 165, 405), S=(855, 240, 975, 405), W=(1058, 228, 1265, 400), E=(1492, 228, 1700, 400))
CLAW = dict(N=(40, 196, 114, 292), S=(40, 196, 114, 292), W=(40, 196, 114, 292), E=(40, 196, 114, 292))
# the coat: the torn strip in the middle, the tails that flare out beside it, the panel at the hip
TABARD = dict(N=(226, 512, 324, 692), S=(708, 512, 806, 702), W=(226, 512, 324, 692), E=(226, 512, 324, 692))
TAIL = dict(N=(126, 512, 226, 715), S=(806, 512, 890, 715), W=(1058, 512, 1278, 722), E=(1482, 512, 1702, 722))
ANKLE = dict(N=(126, 752, 160, 824), S=(872, 752, 906, 824), W=(126, 752, 160, 824), E=(126, 752, 160, 824))
HEEL = dict(N=(1084, 760, 1122, 814), S=(1084, 760, 1122, 814), W=(1084, 760, 1122, 814), E=(1655, 764, 1702, 816))

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

def flip(rects, front_centre=275, back_centre=757):
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

def crown_wall(face, rect, band_px, origin, band_height, points):
    """One side of the crown: a solid band painted from the drawing, and on it solid gold points
    with gaps between them. points = {position along the wall: height}. Cutting the points out of
    the drawing as a thin sheet gave a brush of needles, and a point on every unit gave a castle
    wall; a few separate boxes of different height read as a crown from every side."""
    x0, y0, x1, y1 = rect
    ox, oy, oz = origin                      # the corner of the band where the wall starts
    along_x = face in ("N", "S")
    n = 12 if along_x else 10                # front and back walls cover the corners, so the ring is closed
    size = (n, band_height, 1) if along_x else (1, band_height, n)
    band = dict.fromkeys("NSWE", (x0, band_px, x1, y1))
    out = [relief("head", band, origin, size, origin, size, face)]
    for k, height in points.items():
        xyz = (ox + k, oy - height, oz) if along_x else (ox, oy - height, oz + k)
        out.append(tone_box("head", xyz, (1, height, 1), GOLD, 0.8 + 0.07 * height))
    return out

def pauldron(side):
    """A shoulder: a plate painted from all four views, a second plate standing off its outer face,
    a gem on that, and the claw that curves up from the outer edge."""
    pad = PAD if side < 0 else flip(PAD)
    plate = ((-4.9, -3.4, -3), (5, 5, 6))
    boxes = [
        box("right_arm", pad, *plate, None),
        relief("right_arm", pad, *plate, (-5.7, -2.4, -2), (1, 3, 4), "W"),
        relief("right_arm", pad, *plate, (-6.3, -1.9, -1), (1, 2, 2), "W"),
        *stepped("right_arm", CLAW, (-5.3, -7.2, -0.5), 1, side, min_lum=14),
    ]
    return boxes if side < 0 else [mirror(b) for b in boxes]

def coat(side):
    """The coat below the belt, all of it on the legs so it moves with them: half of the torn strip
    in the middle, the tail flaring beside it, the same at the back, and a panel over the hip."""
    tail = TAIL if side < 0 else flip(TAIL)
    boxes = [
        relief("right_leg", tail, (-3.2, 0, -3.2), (3, 7, 1), (-3.2, 0, -3.2), (3, 7, 1), "N", ragged=True),
        relief("right_leg", tail, (-3.2, 0, 2.2), (3, 7, 1), (-3.2, 0, 2.2), (3, 7, 1), "S", ragged=True),
        relief("right_leg", tail, (-3.4, 0.1, -3), (1, 7, 6), (-3.4, 0.1, -3), (1, 7, 6), "W", ragged=True),
    ]
    boxes = boxes if side < 0 else [mirror(b) for b in boxes]
    # the strip in the middle is one picture shared by both legs: each takes its own half
    ref_x, x, leg = (-0.1, -0.1, "right_leg") if side < 0 else (-3.9, -1.9, "left_leg")
    return boxes + [
        relief(leg, TABARD, (ref_x, 0, -3.1), (4, 6, 1), (x, 0, -3.1), (2, 6, 1), "N", ragged=True),
        relief(leg, TABARD, (ref_x, 0, 2.1), (4, 6, 1), (x, 0, 2.1), (2, 6, 1), "S", ragged=True),
    ]

def boot_gems(side):
    """The crystals on a boot, cut out of the concept: one on the outer side of the ankle, one on the heel."""
    ankle = ANKLE if side < 0 else flip(ANKLE)
    boxes = [
        relief("right_leg", ankle, (-3.9, 8.0, -0.5), (1, 3, 1), (-3.9, 8.0, -0.5), (1, 3, 1), "N", ragged=True),
        relief("right_leg", HEEL, (-0.5, 8.3, 2.9), (1, 2, 1), (-0.5, 8.3, 2.9), (1, 2, 1), "W", ragged=True),
    ]
    return boxes if side < 0 else [mirror(b) for b in boxes]

HEAD_BOX, ARM_R, ARM_L, LEG = ((-4, -8, -4), (8, 8, 8)), ((-3, -2, -2), (4, 12, 4)), ((-1, -2, -2), (4, 12, 4)), ((-2, 0, -2), (4, 12, 4))
BODY_BOX = ((-4, 0, -2), (8, 12, 4))
CROWN_FRONT = ((-6, -13.6, -6), (12, 6, 1))

# rows = the part of the box height (0 = top, 1 = bottom) that is painted; the rest stays see-through.
SLOTS = {
    "head": [
        # two shells: the eyes are painted on the inner one and seen through slits cut in the outer one
        box("head", FACE, (-4, -8, -4), (8, 8, 8), (0, 0), 0.1),
        dict(box("head", FACE, (-4, -8, -4), (8, 8, 8), None, 0.9), slots=True),
        # the crown: a closed ring a little wider than the helm, with solid points, and the ruby standing out in front
        *crown_wall("N", CROWN["N"], 108, (-6, -9.7, -6), 2, {0: 2, 2: 3, 5: 4, 6: 4, 9: 3, 11: 2}),
        *crown_wall("S", CROWN["S"], 108, (-6, -9.7, 5), 1, {0: 2, 2: 3, 5: 4, 6: 4, 9: 3, 11: 2}),
        *crown_wall("W", CROWN["W"], 108, (-6, -9.7, -5), 1, {2: 2, 4: 3, 5: 3, 7: 2}),
        *crown_wall("E", CROWN["E"], 108, (5, -9.7, -5), 1, {2: 2, 4: 3, 5: 3, 7: 2}),
        relief("head", CROWN, *CROWN_FRONT, (-1, -10.7, -6.6), (2, 3, 1), "N"),
    ],
    "chest": [
        box("body", BODY, (-4, 0, -2), (8, 12, 4), (0, 0), 0.8),
        box("right_arm", RARM, (-3, -2, -2), (4, 12, 4), (24, 0), 0.6, rows=(0.4, 1.0), lift=True),
        box("left_arm", LARM, (-1, -2, -2), (4, 12, 4), (40, 0), 0.6, rows=(0.4, 1.0), lift=True),
        *pauldron(-1), *pauldron(1),
        # the blood orb in its gold frame; thick gauntlets on the forearms
        relief("body", ORB, (-2, 1.3, -3.6), (4, 4, 1), (-2, 1.3, -3.6), (4, 4, 1), "N", ragged=True),
        box("right_arm", part_of(RARM, *ARM_R, (-3, 4, -2), (4, 5, 4)), (-3, 4, -2), (4, 5, 4), None, 1.0),
        box("left_arm", part_of(LARM, *ARM_L, (-1, 4, -2), (4, 5, 4)), (-1, 4, -2), (4, 5, 4), None, 1.0),
    ],
    "legs": [
        box("body", BODY, (-4, 0, -2), (8, 12, 4), (0, 0), 0.5, rows=(0.76, 1.0), top=False, bottom=False),
        box("right_leg", RLEG, (-2, 0, -2), (4, 12, 4), (24, 0), 0.5, rows=(0.0, 0.9), top=False, bottom=False),
        box("left_leg", LLEG, (-2, 0, -2), (4, 12, 4), (40, 0), 0.5, rows=(0.0, 0.9), top=False, bottom=False),
        *coat(-1), *coat(1),
        # knee guards, and the gem of the belt
        relief("right_leg", RLEG, *LEG, (-1.5, 4.4, -3.4), (3, 4, 1)),
        relief("left_leg", LLEG, *LEG, (-1.5, 4.4, -3.4), (3, 4, 1)),
        relief("body", GEM, (-1, 9.2, -3.7), (2, 3, 1), (-1, 9.2, -3.7), (2, 3, 1), "N", ragged=True),
    ],
    "feet": [
        box("right_leg", RLEG, (-2, 0, -2), (4, 12, 4), (0, 0), 1.0, rows=(0.68, 1.0), top=False),
        box("left_leg", LLEG, (-2, 0, -2), (4, 12, 4), (16, 0), 1.0, rows=(0.68, 1.0), top=False),
        relief("right_leg", RLEG, *LEG, (-2, 10.4, -3.9), (4, 2, 1)),
        relief("left_leg", LLEG, *LEG, (-2, 10.4, -3.9), (4, 2, 1)),
        *boot_gems(-1), *boot_gems(1),
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
            # Eye slits: where the drawing has the glowing eyes the helm is cut through, so the eyes
            # painted on the box underneath are seen deep inside. A lighter rim marks the cut edge.
            top, low = int(0.12 * fh * K), int(0.52 * fh * K)
            red = np.zeros(block.shape[:2], bool)
            red[top:low] = (block[top:low, :, 0] > 165) & (block[top:low, :, 1] < 120)
            labels, count = ndi.label(ndi.binary_dilation(red, iterations=1))
            hole = np.zeros_like(red)
            for where in ndi.find_objects(labels):
                if red[where].sum() >= 6:
                    hole[where] = True
            rim = ndi.binary_dilation(hole, iterations=1) & ~hole
            block[rim, :3] = (92, 84, 88)
            block[hole] = 0
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
 * The 3D shape of the Blood Master armor: the usual body boxes plus a crown,
 * bladed shoulder plates, a torn tabard and boot blades. Generated by
 * tools/armor_chaos.py together with the textures. Do not edit by hand.
 */
public final class BloodArmorModels {
    private static final Map<EquipmentSlot, HumanoidModel<LivingEntity>> MODELS = new EnumMap<>(EquipmentSlot.class);

    private BloodArmorModels() {
    }

    public static HumanoidModel<LivingEntity> get(EquipmentSlot slot) {
        return MODELS.computeIfAbsent(slot, BloodArmorModels::bake);
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
    Image.fromarray(sheet.clip(0, 255).astype(np.uint8), "RGBA").save(f"{OUT_TEX}/blood_{slot}.png")
    r_, g_, b_, a_ = [sheet[..., i] for i in range(4)]
    cyan = (a_ > 0) & (r_ > 185) & (g_ < 120) & (b_ < 110) & (r_ - g_ > 95)      # the red glow
    glow = np.zeros_like(sheet)
    halo = ndi.binary_dilation(cyan, iterations=1) & ~cyan & (a_ > 0)
    glow[halo] = (130, 20, 20, 255)
    glow[cyan, :3] = sheet[cyan, :3] * 0.35 + np.array([255.0, 215.0, 190.0]) * 0.65
    glow[cyan, 3] = 255
    Image.fromarray(glow.clip(0, 255).astype(np.uint8), "RGBA").save(f"{OUT_TEX}/blood_{slot}_glow.png")
    yy, xx = np.indices(cyan.shape)
    for f in range(FRAMES):
        # for now every glowing detail breathes together; each will get its own life later
        wave = np.full(cyan.shape, 0.3 + 0.25 * np.sin(2 * np.pi * f / FRAMES))
        kind = np.zeros(cyan.shape, int)
        frame = sheet.copy()
        # the eyes are almost white in the drawing: at rest they are dimmed more, so the flare is seen
        rest = np.where(kind == 1, 0.3, 0.75)[..., None]
        lit = frame[..., :3] * (rest + (1 - rest) * wave[..., None])
        lit = lit * (1 - 0.75 * wave[..., None]) + np.array([255.0, 220.0, 195.0]) * 0.75 * wave[..., None]
        frame[cyan, :3] = lit[cyan]
        Image.fromarray(frame.clip(0, 255).astype(np.uint8), "RGBA").save(f"{OUT_TEX}/blood_{slot}_{f}.png")
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

# every box with its place on the sheet, for tools/preview3d.py
import json
json.dump({"prefix": "blood_", "sheet": SHEET, "texels": K, "poses": POSES,
           "slots": {slot: [{k: b[k] for k in ("part", "xyz", "size", "uv", "inflate")} for b in boxes] for slot, boxes in SLOTS.items()}},
          open(f"{OUT_TEX}/boxes.json", "w"))

# ---- a rough front / right / back / left preview of the assembled armor, to check the mapping
def view(direction):
    S = 12  # pixels per unit
    canvas = Image.new("RGBA", (24 * S, 40 * S), (60, 90, 60, 255))
    order = ["feet", "legs", "chest", "head"]
    for slot in order:
        tex = Image.open(f"{OUT_TEX}/blood_{slot}.png")
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
