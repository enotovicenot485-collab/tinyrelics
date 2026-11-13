"""Turns a wide concept picture into an animated creative-tab banner: 162x36 GUI pixels
(two rows of slots) drawn at 4x, with the frames stacked top to bottom in one texture."""
import sys
import numpy as np
from PIL import Image
from scipy import ndimage as ndi
src, out, y0 = sys.argv[1], sys.argv[2], int(sys.argv[3])
# banner.py <picture> <out dir> <first row of the crop> [name] [mode]
name = sys.argv[4] if len(sys.argv) > 4 else "necromancer"
mode = sys.argv[5] if len(sys.argv) > 5 else "eyes"
GW, GH, SCALE = 162, 36, 4
FRAMES = 24 if mode == "strike" else 12
im = Image.open(src).convert("RGB")
W, H = im.size
ch = round(W * GH / GW)
print("source", W, H, "crop rows", y0, y0 + ch)
base = np.array(im.crop((0, y0, W, y0 + ch)).resize((GW * SCALE, GH * SCALE), Image.BOX)).astype(np.float64)
r, g, b = base[..., 0], base[..., 1], base[..., 2]
cyan = (b > 150) & (g > 130) & (r < 140) & (g - r > 50)
mist = (b > 90) & (r > 60) & (g < 0.6 * b) & ~cyan
lab, n = ndi.label(ndi.binary_dilation(cyan, iterations=1))
sizes = ndi.sum(cyan, lab, range(1, n + 1))
centres = ndi.center_of_mass(cyan, lab, range(1, n + 1))
eyes = sorted([(centres[i][1], i + 1) for i in range(n) if sizes[i] >= 40])
sparks = [i + 1 for i in range(n) if 0 < sizes[i] < 40]
# eyes come in pairs, left to right: one pair per skull
skull_of = {}
k = 0
while k < len(eyes):
    if k + 1 < len(eyes) and eyes[k + 1][0] - eyes[k][0] < 70:
        skull_of[eyes[k][1]] = skull_of[eyes[k + 1][1]] = len(set(skull_of.values())); k += 2
    else:
        skull_of[eyes[k][1]] = len(set(skull_of.values())); k += 1
skulls = len(set(skull_of.values()))
print("eyes", len(eyes), "skulls", skulls, "sparks", len(sparks))
rng = np.random.default_rng(3)
xx = np.indices(cyan.shape)[1]
def storm(f):
    """Lightning: every bolt flares up at its own moment and lights the clouds around it."""
    width = base.shape[1]
    # how much a pixel is made of light: bolts fully, lit clouds a little, rock and leather not at all
    glow = np.clip((b - 70) / 110, 0, 1)
    level = np.zeros(base.shape[:2])
    total = np.zeros(base.shape[:2])
    # (where the bolt is, the frame it flares on, how bright it rests); the middle one hits the hammer
    for centre, flash, rest in ((0.13, 1, 0.7), (0.42, 8, 0.8), (0.53, 4, 0.92), (0.70, 10, 0.8), (0.88, 6, 0.7)):
        since = (f - flash) % FRAMES
        value = 1.4 if since == 0 else 1.15 if since == 1 else rest + 0.06 * np.sin(2 * np.pi * (f + flash) / FRAMES)
        weight = np.exp(-((xx / width - centre) / 0.07) ** 2)
        level += weight * value
        total += weight
    level = (level + 0.02 * 0.8) / (total + 0.02)
    return base * (1 + glow * (level - 1))[..., None]

def line(grid, p, q, power):
    n = int(max(abs(q[0] - p[0]), abs(q[1] - p[1]))) + 1
    for t in np.linspace(0, 1, n + 1):
        x, y = int(round(p[0] + (q[0] - p[0]) * t)), int(round(p[1] + (q[1] - p[1]) * t))
        if 0 <= y < grid.shape[0] and 0 <= x < grid.shape[1]:
            grid[y, x] = max(grid[y, x], power)

def bolt(rng, start, end, grid, power=1.0, rough=0.28, branches=True):
    """A jagged pixel bolt from start to end on a coarse grid: the line is broken in the
    middle again and again, and side branches fork off and die out."""
    pts = [np.array(start, float), np.array(end, float)]
    for _ in range(5):
        nxt = [pts[0]]
        for p, q in zip(pts, pts[1:]):
            d = q - p
            length = np.hypot(*d)
            mid = (p + q) / 2 + np.array([-d[1], d[0]]) / max(length, 1e-6) * rng.normal(0, rough) * length
            nxt += [mid, q] if length > 3 else [q]
        pts = nxt
    for p, q in zip(pts, pts[1:]):
        line(grid, p, q, power)
    if branches:
        for i in rng.choice(len(pts) - 2, size=min(3, len(pts) - 2), replace=False):
            p = pts[i + 1]
            d = np.array(end, float) - np.array(start, float)
            turn = rng.choice([-1, 1]) * rng.uniform(0.5, 1.1)
            c, sn = np.cos(turn), np.sin(turn)
            far_end = p + np.array([d[0] * c - d[1] * sn, d[0] * sn + d[1] * c]) * rng.uniform(0.25, 0.5)
            bolt(rng, p, far_end, grid, power * 0.55, rough, False)

def strike_frames():
    """A thunderstorm: bolts hit the hammer, each with its own shape, the sky flares, runes blaze, rain falls."""
    rng = np.random.default_rng(11)
    P = 2                                    # one bolt pixel is 2x2 texels, like the picture's own pixels
    h, w = base.shape[0] // P, base.shape[1] // P
    gem, head = (361 // P, 36 // P), (316 // P, 78 // P)
    # (frame it lands on, x at the top of the sky, where it hits)
    hits = [(2, gem[0] - 70, gem), (10, gem[0] + 85, gem), (17, head[0] - 95, (head[0] - 4, head[1] - 9))]
    far = [(6, 40, 34), (14, 285, 30), (21, 95, 26)]   # silent bolts deep in the clouds: frame, x, length
    shapes = {}
    arcs = {}
    for f, x, target in hits:
        g = np.zeros((h, w)); bolt(rng, (x, -2), target, g); shapes[f] = (g, (x + target[0]) / 2)
        arcs[f] = []
        for k in range(3):
            a = np.zeros((h, w))
            for _ in range(3):
                p = (head[0] + rng.integers(-16, 30), head[1] + rng.integers(-26, 10))
                bolt(rng, p, (p[0] + rng.integers(-12, 12), p[1] + rng.integers(4, 14)), a, 0.8, 0.35, False)
            arcs[f].append(a)
    for f, x, length in far:
        g = np.zeros((h, w)); bolt(rng, (x, -2), (x + rng.integers(-30, 30), length), g, 0.6); shapes[f] = (g, x)
    # how a strike lives: flash, flicker, second flash, thin afterimage, gone
    life = [1.0, 0.45, 0.85, 0.3, 0.12]
    rune = cyan.astype(np.float64)
    halo = ndi.gaussian_filter(rune, 5)
    halo = halo / halo.max()
    lum = base.mean(-1) / 255
    drops = [(rng.integers(0, w + 30), rng.integers(0, h), rng.choice([3, 6])) for _ in range(46)]
    cols = np.arange(w)[None, :]
    out_frames = []
    for f in range(FRAMES):
        fr = base.copy()
        core = np.zeros((h, w)); sky = np.zeros((h, w)); charge = 0.0
        for start, (g, x) in shapes.items():
            age = (f - start) % FRAMES
            if age >= len(life):
                continue
            level = life[age]
            core = np.maximum(core, g * level if age < 3 else (g > 0.9) * level)
            # the clouds light up around the bolt and fade further away
            sky = np.maximum(sky, level * g.max() * np.exp(-((cols - x) / 90.0) ** 2) * np.ones((h, 1)))
            if g.max() > 0.9:
                charge = max(charge, level)
        # after the hit small arcs crawl over the hammer, a new set every frame
        for start, sets in arcs.items():
            age = (f - start) % FRAMES
            if 1 <= age <= 6:
                core = np.maximum(core, sets[age % 3] * (1.0 if age < 5 else 0.5))
        # the charge stays in the hammer a little longer than the bolt lasts
        for start, _, _ in hits:
            age = (f - start) % FRAMES
            if 5 <= age < 9:
                charge = max(charge, 0.5 - 0.1 * (age - 5))
        up = lambda a: np.kron(a, np.ones((P, P)))
        glow = up(np.clip(ndi.gaussian_filter(core, 1.6) * 6, 0, 1))
        wide = up(np.clip(ndi.gaussian_filter(core, 7) * 14, 0, 1))
        skyl = up(sky)
        fr = fr * (1 + 2.2 * skyl * (0.3 + lum))[..., None] + (skyl * (0.15 + lum) * 70)[..., None] * np.array([0.3, 0.5, 1.0])
        fr += wide[..., None] * np.array([10, 40, 120.0]) + glow[..., None] * np.array([40, 130, 255.0])
        c = up(core)
        fr = fr * (1 - np.clip(c * 1.8, 0, 1))[..., None] + np.clip(c * 1.8, 0, 1)[..., None] * np.array([225, 245, 255.0])
        # runes and cracks: a slow pulse at rest, a blaze when charged
        rest = 0.8 + 0.12 * np.sin(2 * np.pi * f / FRAMES * 2)
        fr[cyan] = fr[cyan] * (rest + 0.9 * charge)
        fr += (halo * charge)[..., None] * np.array([20, 90, 200.0])
        # rain
        for x0, phase, speed in drops:
            t = (f * speed + phase) % h
            for k in range(4 if speed == 6 else 3):
                yy, xr = int(t + k), int(x0 - 0.35 * (t + k))
                if 0 <= yy < h and 0 <= xr < w:
                    fr[yy * P:(yy + 1) * P, xr * P:(xr + 1) * P] += np.array([10, 28, 70.0]) * (1.5 if speed == 6 else 0.9)
        out_frames.append(fr)
    return out_frames

frames = strike_frames() if mode == "strike" else []
for f in range(0 if mode == "strike" else FRAMES):
    if mode == "storm":
        frames.append(storm(f))
        continue
    fr = base.copy()
    # the mist breathes: a soft wave of light drifts from left to right
    wave = 0.82 + 0.36 * np.clip(np.cos(2 * np.pi * f / FRAMES - xx * 0.035), 0, 1) ** 2
    fr[mist] = fr[mist] * wave[mist][:, None]
    # each skull blinks at its own moment: eyes nearly out for one frame, half lit on the next
    for comp, skull in skull_of.items():
        blink = (skull * 5 + 2) % FRAMES
        level = 0.18 if f == blink else 0.55 if f == (blink + 1) % FRAMES else 1.0 + 0.12 * np.sin(2 * np.pi * (f + skull) / FRAMES)
        m = (lab == comp) & cyan
        fr[m] = fr[m] * level
    for comp in sparks:
        m = (lab == comp) & cyan
        fr[m] = fr[m] * rng.choice([0.35, 0.8, 1.0, 1.25])
    frames.append(fr)
strip = np.concatenate(frames, 0).clip(0, 255).astype(np.uint8)
Image.fromarray(strip, "RGB").save(out + "/banner_" + name + ".png")
sheet = np.concatenate([frames[k] for k in ((2, 3, 4, 7, 10, 17) if mode == "strike" else (0, 2, 7))], 0).clip(0, 255).astype(np.uint8)
Image.fromarray(sheet, "RGB").save(out + "/banner_preview.png")
print("texture", strip.shape[1], strip.shape[0])
