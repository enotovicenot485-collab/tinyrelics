"""Builds the Chaos Assassin armor (same method as armor.py, see docs/ARMOR.md): paints every face of every box of the model from the
four concept views, and writes the matching Java model class."""
import sys
import numpy as np
from PIL import Image
from scipy import ndimage as ndi

SRC, OUT_TEX, OUT_JAVA = sys.argv[1], sys.argv[2], sys.argv[3]
K = 4                      # texels per model unit
SHEET = 64                 # logical texture size
FRAMES = 32                # frames of the glow animation; the game shows one every 2 ticks, a 3.2 s loop
im = np.array(Image.open(SRC).convert("RGB")).astype(np.float64)
H, W, _ = im.shape
# green background (the armor glows violet, so magenta would not do), and its fringe on the outline
BG = (im[..., 1] - im[..., 0] > 40) & (im[..., 1] - im[..., 2] > 40)

# Source rectangles (x0, y0, x1, y1) in the concept image for each face of each box.
# N = front, S = back, W = the character's right side, E = the character's left side.
# The hood is taller than a head, so the head takes only the face opening and the hood is built from plates.
FACE = dict(N=(185, 130, 372, 275), S=(645, 15, 880, 275), W=(1068, 22, 1258, 245), E=(1508, 22, 1700, 245))
HOOD = dict(N=(150, 15, 405, 275), S=(645, 15, 880, 300), W=(1068, 22, 1258, 245), E=(1508, 22, 1700, 245))
BROW = dict(N=(150, 55, 405, 135), S=(645, 15, 880, 100), W=(1068, 22, 1258, 100), E=(1508, 22, 1700, 100))
RUNE = dict(N=(232, 48, 326, 130), S=(232, 48, 326, 130), W=(232, 48, 326, 130), E=(232, 48, 326, 130))
BODY = dict(N=(183, 285, 393, 520), S=(668, 285, 850, 520), W=(1110, 290, 1265, 545), E=(1500, 290, 1665, 545))
RARM = dict(N=(78, 290, 180, 560), S=(858, 290, 950, 560), W=(1110, 290, 1265, 545), E=(1500, 290, 1665, 545))
LARM = dict(N=(397, 290, 500, 560), S=(575, 290, 665, 560), W=(1110, 290, 1265, 545), E=(1500, 290, 1665, 545))
RLEG = dict(N=(160, 520, 283, 865), S=(760, 520, 875, 865), W=(1115, 520, 1265, 865), E=(1510, 520, 1665, 865))
LLEG = dict(N=(287, 520, 410, 865), S=(645, 520, 755, 865), W=(1115, 520, 1265, 865), E=(1510, 520, 1665, 865))
# shoulder plates as seen from the side: the lower chevron with its gem, and the upper chevron
PAD_LOW = dict(N=(55, 285, 185, 372), S=(855, 285, 975, 372), W=(1090, 262, 1285, 372), E=(1490, 262, 1690, 372))
PAD_UP = dict(N=(75, 228, 185, 285), S=(855, 228, 960, 285), W=(1110, 205, 1265, 262), E=(1505, 205, 1665, 262))
BLADE = dict(N=(40, 180, 120, 310), S=(40, 180, 120, 310), W=(40, 180, 120, 310), E=(40, 180, 120, 310))
FIN_OUTER = dict(N=(140, 748, 172, 826), S=(872, 748, 906, 830), W=(140, 748, 172, 826), E=(140, 748, 172, 826))
FIN_HEEL = dict(N=(1090, 742, 1128, 815), S=(1090, 742, 1128, 815), W=(1090, 742, 1128, 815), E=(1672, 742, 1712, 815))
TABARD = dict(N=(220, 520, 345, 690), S=(695, 520, 800, 690), W=(220, 520, 345, 690), E=(220, 520, 345, 690))

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

def flip(rects, front_centre=285, back_centre=760):
    """The same rectangles for the other side of the body: mirrored in the front and back views, side views swapped."""
    (a, b, c, d), (e, f, g, h) = rects["N"], rects["S"]
    return dict(N=(2 * front_centre - c, b, 2 * front_centre - a, d), S=(2 * back_centre - g, f, 2 * back_centre - e, h), W=rects["E"], E=rects["W"])

def stepped(part, rects, xyz, depth, side, unit=26.67):
    """A solid shape that follows a drawing's outline: one box for every unit of height, as wide as
    the drawing is on that row, carrying its slice of the picture on the front and back.
    Built for the right side; for the left the mirrored picture is used and mirror() moves the box."""
    x0, y0, x1, y1 = rects["N"]
    out = []
    for row in range(int(round((y1 - y0) / unit))):
        ya, yb = int(y0 + row * unit), int(y0 + (row + 1) * unit)
        # the drawing's black outline is not part of the shape: it would only make the blade fatter
        metal = ~BG[ya:yb, x0:x1] & (im[ya:yb, x0:x1].mean(-1) > 60)
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

def pauldron(side):
    """A shoulder: two stacked plates painted from all four views, a blade curving up and a spike below it."""
    low, up = (PAD_LOW, PAD_UP) if side < 0 else (flip(PAD_LOW), flip(PAD_UP))
    boxes = [
        box("right_arm", low, (-4.6, -2.9, -3), (6, 3, 6), None),
        box("right_arm", up, (-4.4, -3.8, -2.5), (4, 1, 5), None),
        # the curved blade: one solid spike, built row by row along the outline of the drawing
        *stepped("right_arm", BLADE, (-6.5, -6.2, -0.5), 1, side),
    ]
    return boxes if side < 0 else [mirror(b) for b in boxes]

def boot_blades(side):
    """A boot's two fins, cut out of the concept like the shoulder blades: one on the outer side, one on the heel."""
    outer = FIN_OUTER if side < 0 else flip(FIN_OUTER)
    boxes = [
        relief("right_leg", outer, (-3.9, 7.9, 0.2), (1, 3, 1), (-3.9, 7.9, 0.2), (1, 3, 1), "N", ragged=True),
        relief("right_leg", FIN_HEEL, (-0.5, 7.7, 2.9), (1, 3, 1), (-0.5, 7.7, 2.9), (1, 3, 1), "W", ragged=True),
    ]
    return boxes if side < 0 else [mirror(b) for b in boxes]

# rows = the part of the box height (0 = top, 1 = bottom) that is painted; the rest stays see-through.
SLOTS = {
    "head": [
        # the face sits deep inside: the hood's plates reach 1.5 further forward than the mask
        dict(box("head", FACE, (-4, -8, -4), (8, 8, 8), (0, 0), 0.5), fx="eyes"),
        relief("head", HOOD, (-5.3, -9, -6), (1, 8, 11), (-5.3, -9, -6), (1, 8, 11), "W"),
        relief("head", HOOD, (4.3, -9, -6), (1, 8, 11), (4.3, -9, -6), (1, 8, 11), "E"),
        relief("head", HOOD, (-4.5, -8.8, 4.6), (9, 10, 1), (-4.5, -8.8, 4.6), (9, 10, 1), "S"),
        relief("head", BROW, (-4.5, -9.3, -6.2), (9, 3, 1), (-4.5, -9.3, -6.2), (9, 3, 1), "N"),
        relief("head", RUNE, (-1.5, -9.9, -6.6), (3, 3, 1), (-1.5, -9.9, -6.6), (3, 3, 1), "N"),
        # the top of the hood, stepped up into a peak
        tone_box("head", (-4.55, -9.6, -5.9), (9, 1, 11), CLOTH, 1.0),
        tone_box("head", (-3.5, -10.5, -5.5), (7, 1, 10), CLOTH, 1.12),
        tone_box("head", (-2, -11.3, -5.1), (4, 1, 8), CLOTH, 1.25),
    ],
    "chest": [
        box("body", BODY, (-4, 0, -2), (8, 12, 4), (0, 0), 0.8),
        box("right_arm", RARM, (-3, -2, -2), (4, 12, 4), (24, 0), 0.6, rows=(0.3, 1.0), lift=True),
        box("left_arm", LARM, (-1, -2, -2), (4, 12, 4), (40, 0), 0.6, rows=(0.3, 1.0), lift=True),
        *pauldron(-1), *pauldron(1),
        # the vortex emblem stands out from the chest; thick bracers with runes on the forearms
        dict(relief("body", BODY, *BODY_BOX, (-2, 0.6, -3.5), (4, 4, 1)), fx="vortex"),
        box("right_arm", part_of(RARM, *ARM_R, (-3, 4, -2), (4, 4, 4)), (-3, 4, -2), (4, 4, 4), None, 1.0),
        box("left_arm", part_of(LARM, *ARM_L, (-1, 4, -2), (4, 4, 4)), (-1, 4, -2), (4, 4, 4), None, 1.0),
    ],
    "legs": [
        box("body", BODY, (-4, 0, -2), (8, 12, 4), (0, 0), 0.5, rows=(0.72, 1.0), top=False, bottom=False),
        box("right_leg", RLEG, (-2, 0, -2), (4, 12, 4), (24, 0), 0.5, rows=(0.0, 0.9), top=False, bottom=False),
        box("left_leg", LLEG, (-2, 0, -2), (4, 12, 4), (40, 0), 0.5, rows=(0.0, 0.9), top=False, bottom=False),
        # the torn cloth hangs from the belt in two halves, one on each leg, so it moves with them
        relief("right_leg", TABARD, (-0.1, 0, -3), (4, 6, 1), (-0.1, 0, -3.1), (2, 6, 1), "N", ragged=True),
        relief("left_leg", TABARD, (-3.9, 0, -3), (4, 6, 1), (-1.9, 0, -3.1), (2, 6, 1), "N", ragged=True),
        relief("right_leg", TABARD, (-0.1, 0, 2), (4, 6, 1), (-0.1, 0, 2.1), (2, 6, 1), "S", ragged=True),
        relief("left_leg", TABARD, (-3.9, 0, 2), (4, 6, 1), (-1.9, 0, 2.1), (2, 6, 1), "S", ragged=True),
        # knee guards with runes, and the gem of the belt
        relief("right_leg", RLEG, *LEG, (-1.5, 4.6, -3.3), (3, 4, 1)),
        relief("left_leg", LLEG, *LEG, (-1.5, 4.6, -3.3), (3, 4, 1)),
        relief("body", BODY, *BODY_BOX, (-1, 8.8, -3.6), (2, 3, 1)),
    ],
    "feet": [
        box("right_leg", RLEG, (-2, 0, -2), (4, 12, 4), (0, 0), 1.0, rows=(0.68, 1.0), top=False),
        box("left_leg", LLEG, (-2, 0, -2), (4, 12, 4), (16, 0), 1.0, rows=(0.68, 1.0), top=False),
        relief("right_leg", RLEG, *LEG, (-2, 10.4, -3.9), (4, 2, 1)),
        relief("left_leg", LLEG, *LEG, (-2, 10.4, -3.9), (4, 2, 1)),
        *boot_blades(-1), *boot_blades(1),
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
 * The 3D shape of the Chaos Assassin armor: the usual body boxes plus a deep hood,
 * bladed shoulder plates, a torn tabard and boot blades. Generated by
 * tools/armor_chaos.py together with the textures. Do not edit by hand.
 */
public final class ChaosArmorModels {
    private static final Map<EquipmentSlot, HumanoidModel<LivingEntity>> MODELS = new EnumMap<>(EquipmentSlot.class);

    private ChaosArmorModels() {
    }

    public static HumanoidModel<LivingEntity> get(EquipmentSlot slot) {
        return MODELS.computeIfAbsent(slot, ChaosArmorModels::bake);
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
    Image.fromarray(sheet.clip(0, 255).astype(np.uint8), "RGBA").save(f"{OUT_TEX}/chaos_{slot}.png")
    r_, g_, b_, a_ = [sheet[..., i] for i in range(4)]
    cyan = (a_ > 0) & (b_ > 170) & (b_ - g_ > 70) & (r_ > 110) & (r_ - g_ > 15)      # the violet glow
    glow = np.zeros_like(sheet)
    halo = ndi.binary_dilation(cyan, iterations=1) & ~cyan & (a_ > 0)
    glow[halo] = (80, 30, 130, 255)
    glow[cyan, :3] = sheet[cyan, :3] * 0.35 + np.array([245.0, 225.0, 255.0]) * 0.65
    glow[cyan, 3] = 255
    Image.fromarray(glow.clip(0, 255).astype(np.uint8), "RGBA").save(f"{OUT_TEX}/chaos_{slot}_glow.png")
    yy, xx = np.indices(cyan.shape)
    # Every glowing detail has its own life. kind: 0 slow breath, 1 eyes, 2 vortex, 3 arm runes, 4 knee runes
    kind = np.zeros(cyan.shape, int)
    centre = (0.0, 0.0)
    for b in boxes:
        w, h, d = b["size"]; u, v = b["uv"]
        fx = b["fx"] or ("arm" if slot == "chest" and "arm" in b["part"] else "knee" if slot == "legs" and "leg" in b["part"] else "slow")
        kind[v * K:(v + d + h) * K, u * K:(u + 2 * (w + d)) * K] = {"slow": 0, "eyes": 1, "vortex": 2, "arm": 3, "knee": 4}[fx]
        if fx == "vortex":
            centre = ((u + d + w / 2) * K, (v + d + h / 2) * K)
    angle = np.arctan2(yy - centre[1], xx - centre[0]); radius = np.hypot(yy - centre[1], xx - centre[0])
    def bump(t, at, width):
        gap = np.abs((t - at + 0.5) % 1.0 - 0.5)
        return np.exp(-(gap / width) ** 2)
    for f in range(FRAMES):
        t = f / FRAMES
        wave = np.full(cyan.shape, 0.3 + 0.25 * np.sin(2 * np.pi * t))
        # the eyes flare up twice in a row, then smoulder
        wave[kind == 1] = 0.2 + 0.8 * max(bump(t, 0.10, 0.03), 0.75 * bump(t, 0.19, 0.035))
        # two bright arms turn round the centre of the vortex
        spin = 0.12 + 0.88 * np.clip(np.cos(2 * angle - radius * 0.3 - 2 * np.pi * t), 0, 1) ** 2
        wave[kind == 2] = spin[kind == 2]
        # the runes light up once a loop, the arms first, the knees later; the light runs down the rune
        run = (yy % (16 * K)) / (16.0 * K) * 0.08
        wave[kind == 3] = (0.12 + 0.88 * bump(t - run, 0.45, 0.07))[kind == 3]
        wave[kind == 4] = (0.12 + 0.88 * bump(t - run, 0.72, 0.07))[kind == 4]
        frame = sheet.copy()
        # the eyes are almost white in the drawing: at rest they are dimmed more, so the flare is seen
        rest = np.where(kind == 1, 0.3, 0.6)[..., None]
        lit = frame[..., :3] * (rest + (1 - rest) * wave[..., None])
        lit = lit * (1 - 0.75 * wave[..., None]) + np.array([250.0, 230.0, 255.0]) * 0.75 * wave[..., None]
        frame[cyan, :3] = lit[cyan]
        Image.fromarray(frame.clip(0, 255).astype(np.uint8), "RGBA").save(f"{OUT_TEX}/chaos_{slot}_{f}.png")
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
json.dump({"prefix": "chaos_", "sheet": SHEET, "texels": K, "poses": POSES,
           "slots": {slot: [{k: b[k] for k in ("part", "xyz", "size", "uv", "inflate")} for b in boxes] for slot, boxes in SLOTS.items()}},
          open(f"{OUT_TEX}/boxes.json", "w"))

# ---- a rough front / right / back / left preview of the assembled armor, to check the mapping
def view(direction):
    S = 12  # pixels per unit
    canvas = Image.new("RGBA", (24 * S, 40 * S), (60, 90, 60, 255))
    order = ["feet", "legs", "chest", "head"]
    for slot in order:
        tex = Image.open(f"{OUT_TEX}/chaos_{slot}.png")
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
