"""The curio shop banner of the creative tab: the stall at night, with the merchant rocking on his chair,
fireflies in one jar, lightning in the other, the stone idol's eyes lighting up, lanterns flickering.

    python3 banner_shop.py <concepts folder> <out folder>

Writes banner_shop.png: FRAMES frames of 648x144 one under another (TabBanners must be told the same number)."""
import sys
import numpy as np
from PIL import Image
from scipy import ndimage as ndi

SRC, OUT = sys.argv[1], sys.argv[2]
GW, GH, SCALE = 162, 36, 4
W, H = GW * SCALE, GH * SCALE
FRAMES = 40
Y0 = 130                                    # first row of the picture that the banner shows

picture = Image.open(f"{SRC}/banner_shop.png").convert("RGB")
K = W / picture.width                       # banner pixels per pixel of the picture
rows = round(picture.width * GH / GW)
base = np.array(picture.crop((0, Y0, picture.width, Y0 + rows)).resize((W, H), Image.LANCZOS)).astype(np.float64)

def at(x, y):
    """A point of the picture, in banner pixels."""
    return x * K, (y - Y0) * K

yy, xx = np.indices((H, W)).astype(np.float64)

def glow(frame, x, y, radius, colour, power):
    frame += np.exp(-((xx - x) ** 2 + (yy - y) ** 2) / (2 * radius * radius))[..., None] * np.array(colour, dtype=np.float64) * power

# ---- the merchant: mirrored so his boots lie on the counter to the left of him, cut out of his magenta sheet
sheet = np.array(Image.open(f"{SRC}/shop_merchant.png").convert("RGB")).astype(int)
solid = ~((sheet[..., 0] - sheet[..., 1] > 90) & (sheet[..., 2] - sheet[..., 1] > 90))
merchant = np.dstack([sheet, np.where(solid, 255, 0)]).astype(np.float64)[:, ::-1].copy()
M = 0.118                                   # his size on the banner
HEEL = (merchant.shape[1] - 1095, 992)        # where his boots rest, on the mirrored sheet
# He rocks on the chair's back legs: the chair and his body turn about the spot where those legs stand,
# so his head goes back and forth. His boots stay on the counter, and his legs bend between the two.
CHAIR_FOOT = (merchant.shape[1] - 560, 1180)
STILL_UNTIL, MOVING_FROM = 330.0, 620.0     # columns of the mirrored sheet: boots / body
my, mx = np.indices(merchant.shape[:2]).astype(np.float64)
weight = np.clip((mx - STILL_UNTIL) / (MOVING_FROM - STILL_UNTIL), 0, 1)
weight = weight * weight * (3 - 2 * weight)
HEEL_AT = (438.0, at(0, 577)[1])            # and where that point is on the banner
COUNTER = at(0, 560)[1]                     # the counter's top edge

def with_merchant(frame, angle):
    # every pixel looks back along its own share of the turn (the turn is small, so this is close enough)
    a = np.radians(angle)
    dx, dy = mx - CHAIR_FOOT[0], my - CHAIR_FOOT[1]
    sx = mx + weight * (dx * np.cos(a) + dy * np.sin(a) - dx)
    sy = my + weight * (-dx * np.sin(a) + dy * np.cos(a) - dy)
    turned = np.dstack([ndi.map_coordinates(merchant[..., c], [sy, sx], order=1, mode="constant") for c in range(4)])
    turned = Image.fromarray(turned.clip(0, 255).astype(np.uint8), "RGBA")
    small = turned.resize((round(merchant.shape[1] * M), round(merchant.shape[0] * M)), Image.LANCZOS)
    x0, y0 = int(round(HEEL_AT[0] - HEEL[0] * M)), int(round(HEEL_AT[1] - HEEL[1] * M))
    layer = np.zeros((H, W, 4))
    part = np.array(small).astype(np.float64)
    ys, xs = slice(max(0, y0), min(H, y0 + part.shape[0])), slice(max(0, x0), min(W, x0 + part.shape[1]))
    layer[ys, xs] = part[ys.start - y0:ys.stop - y0, xs.start - x0:xs.stop - x0]
    alpha = layer[..., 3:4] / 255.0
    # he sits behind the counter: it hides everything below his boots, and to the right of them its top as well
    behind = (yy >= HEEL_AT[1] + 1) | ((yy >= COUNTER) & (xx >= HEEL_AT[0] + 14))
    alpha[behind] = 0
    return frame * (1 - alpha) + layer[..., :3] * alpha

def bolt(rng, x, top, bottom, spread):
    points = [(x, top)]
    steps = 7
    for i in range(1, steps + 1):
        points.append((x + rng.uniform(-spread, spread) * (1 if i < steps else 0.4), top + (bottom - top) * i / steps))
    return points

def draw_line(frame, p, q, colour, power):
    n = int(max(abs(q[0] - p[0]), abs(q[1] - p[1])) * 2) + 1
    for i in range(n + 1):
        x, y = p[0] + (q[0] - p[0]) * i / n, p[1] + (q[1] - p[1]) * i / n
        xi, yi = int(round(x)), int(round(y))
        if 0 <= xi < W and 0 <= yi < H:
            frame[yi, xi] = frame[yi, xi] * (1 - power) + np.array(colour) * power

LANTERNS = [at(152, 268), at(852, 222), at(1822, 262), at(2022, 268)]
JAR_FLIES = (at(640, 395), at(768, 535))            # inside the first jar: corner to corner
JAR_STORM = (at(910, 395), at(1032, 535))           # inside the second
EYES = (at(1205, 348), at(1227, 378)), (at(1250, 348), at(1272, 378))
AMULET = at(372, 452)
STRIKES = {6: 1.0, 7: 0.55, 8: 0.2, 24: 1.0, 25: 0.6, 26: 0.25, 33: 0.5, 34: 0.2}

rng = np.random.default_rng(11)
flies = [dict(x=rng.uniform(0.2, 0.8), y=rng.uniform(0.2, 0.8), ax=rng.uniform(0.1, 0.25), ay=rng.uniform(0.08, 0.2),
              kx=int(rng.integers(1, 3)), ky=int(rng.integers(1, 4)), kb=int(rng.integers(2, 5)), p=rng.uniform(0, 6.28, 3)) for _ in range(7)]
frames = []
for f in range(FRAMES):
    t = f / FRAMES
    frame = base.copy()
    for i, (x, y) in enumerate(LANTERNS):           # the lanterns breathe, each on its own beat
        glow(frame, x, y, 16, (60, 34, 8), 0.5 * np.sin(2 * np.pi * (2 * t + i * 0.3)) + 0.3 * np.sin(2 * np.pi * (5 * t + i * 0.7)))
    frame = with_merchant(frame, 2.0 + 5.0 * np.sin(2 * np.pi * t))
    # fireflies wander in the first jar and blink
    (x0, y0), (x1, y1) = JAR_FLIES
    for fly in flies:
        x = x0 + (x1 - x0) * (fly["x"] + fly["ax"] * np.sin(2 * np.pi * fly["kx"] * t + fly["p"][0]))
        y = y0 + (y1 - y0) * (fly["y"] + fly["ay"] * np.sin(2 * np.pi * fly["ky"] * t + fly["p"][1]))
        blink = 0.35 + 0.65 * max(0.0, np.sin(2 * np.pi * fly["kb"] * t + fly["p"][2]))
        glow(frame, x, y, 2.6, (150, 190, 40), blink)
        glow(frame, x, y, 0.8, (255, 255, 190), blink)
    # lightning strikes inside the second jar
    (x0, y0), (x1, y1) = JAR_STORM
    if f in STRIKES:
        power = STRIKES[f]
        strike = np.random.default_rng(100 + (f if f in (6, 24, 33) else f - 1 if f in (7, 25, 34) else f - 2))
        middle = (x0 + x1) / 2 + strike.uniform(-6, 6)
        points = bolt(strike, middle, y0, y1, 9)
        glow(frame, (x0 + x1) / 2, (y0 + y1) / 2, 14, (70, 90, 170), power)
        for p, q in zip(points, points[1:]):
            draw_line(frame, p, q, (235, 242, 255), min(1.0, power * 1.2))
            draw_line(frame, (p[0] + 1, p[1]), (q[0] + 1, q[1]), (150, 180, 255), power * 0.6)
        branch = points[3]
        draw_line(frame, branch, (branch[0] + strike.uniform(-9, 9), branch[1] + 10), (200, 215, 255), power * 0.8)
    else:
        glow(frame, (x0 + x1) / 2 + 6 * np.sin(2 * np.pi * 3 * t), y1 - 4, 3, (50, 60, 130), 0.5 + 0.5 * np.sin(2 * np.pi * 7 * t))
    # the idol's eyes light up white for a moment
    lit = max(0.0, min(1.0, (f - 13) / 3.0, (23 - f) / 3.0))
    if lit > 0:
        for (ex0, ey0), (ex1, ey1) in EYES:
            box = (slice(int(round(ey0)), int(round(ey1)) + 1), slice(int(round(ex0)), int(round(ex1)) + 1))
            frame[box] = frame[box] * (1 - lit) + np.array((255, 255, 255)) * lit
            glow(frame, (ex0 + ex1) / 2, (ey0 + ey1) / 2, 5, (120, 125, 135), lit)
    # the amulet glints
    glint = max(0.0, 1 - abs(f - 31) / 4.0)
    if glint > 0:
        ax, ay = AMULET
        for d in range(-5, 6):
            w = glint * (1 - abs(d) / 6.0)
            for px, py in ((ax + d, ay), (ax, ay + d)):
                frame[int(round(py)), int(round(px))] = frame[int(round(py)), int(round(px))] * (1 - w) + np.array((255, 250, 210)) * w
    frames.append(frame.clip(0, 255).astype(np.uint8))
Image.fromarray(np.concatenate(frames, 0), "RGB").save(f"{OUT}/banner_shop.png")
Image.fromarray(np.concatenate([frames[k] for k in (0, 10, 20, 30)], 0), "RGB").save(f"{OUT}/banner_shop_preview.png")
print("frames", FRAMES)
