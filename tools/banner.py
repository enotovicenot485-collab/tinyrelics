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
FRAMES = {"strike": 24, "slash": 36, "throne": 36}.get(mode, 12)
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

def slash_frames():
    """The assassin: eyes flare, a blade's flash crosses the whole banner behind the head,
    the cut it leaves bleeds, lanterns flicker in the temples."""
    rng = np.random.default_rng(5)
    P = 2
    hh, ww = base.shape[0] // P, base.shape[1] // P
    k = base.shape[1] / W                       # source pixels to texels
    # the outline of the hood in the source picture: the flash passes behind it
    hood = [(1085, 165), (1005, 198), (940, 255), (920, 330), (890, 450), (900, 475), (982, 555), (1075, 548),
            (1178, 558), (1200, 550), (1270, 472), (1275, 440), (1245, 320), (1230, 250), (1165, 198)]
    from PIL import ImageDraw
    m = Image.new("L", (base.shape[1], base.shape[0]), 0)
    ImageDraw.Draw(m).polygon([(x * k, (y - y0) * k) for x, y in hood], fill=255)
    head = ndi.binary_dilation(np.array(m) > 0, iterations=2)
    eye = head & (b > 200) & (g > 110) & (r > 150)
    eye_halo = ndi.gaussian_filter(eye.astype(float), 4); eye_halo /= max(eye_halo.max(), 1e-6)
    rune = head & (b > 150) & (b - g > 70) & (r > 90) & ~eye
    lamp = (r > 150) & (g > 80) & (b < 110) & (r - b > 70) & ~head
    lamps, count = ndi.label(ndi.binary_dilation(lamp, iterations=6))
    # the cut: a slightly rising line, on the coarse grid
    cut_y = lambda x: 50.5 - x * 0.043
    xs = np.arange(ww)
    thick = 0.6 + 1.6 * np.sin(np.pi * xs / ww) ** 0.7          # a blade's flash is fat in the middle
    yy2, xx2 = np.indices((hh, ww))
    dist = np.abs(yy2 - cut_y(xx2))
    blade = np.clip(thick[None, :] - dist + 0.5, 0, 1)
    sparks = [(rng.integers(8, ww - 8), rng.normal(0, 3.0), rng.integers(0, 3)) for _ in range(34)]
    drips = [(x, start) for x, start in zip((18, 47, 83, 112, 206, 233, 262, 291, 311), (13, 17, 12, 19, 15, 12, 18, 14, 16))]
    up = lambda a: np.kron(a, np.ones((P, P)))
    SWEEP0, SWEEP1 = 4, 8                        # frames during which the tip crosses the banner
    out_frames = []
    for f in range(FRAMES):
        fr = base.copy()
        # lanterns: every building has its own slow flicker
        for c in range(1, count + 1):
            sel = (lamps == c) & lamp
            fr[sel] *= 0.86 + 0.2 * np.sin(2 * np.pi * (f * (1 + c % 2) / FRAMES + c * 0.37))
        # the flash
        tip = (f - SWEEP0 + 1) / (SWEEP1 - SWEEP0) * ww
        if f >= SWEEP0:
            age = max(0, f - SWEEP1)
            drawn = (xx2 <= tip).astype(float)
            fade = max(0.0, 1 - age / 5.0)
            core = blade * drawn * fade
            # close to the tip it burns white, further back it is already cooling
            hot = np.clip(1 - (tip - xx2) / 60.0, 0, 1) * drawn if f <= SWEEP1 else 0 * drawn
            glow = ndi.gaussian_filter(core, 2.5) * 2.2
            layer = np.zeros(base.shape)
            layer += up(np.clip(glow, 0, 1))[..., None] * np.array([120, 70, 200.0])
            c = up(np.clip(core * (0.8 + 0.6 * hot), 0, 1))[..., None]
            behind = (~head)[..., None]
            fr = np.where(behind, fr + layer, fr)
            fr = np.where(behind, fr * (1 - c) + c * np.array([245, 235, 255.0]), fr)
            # sparks fly off the line while it is hot
            for sx, sy, delay in sparks:
                t = f - SWEEP0 - (sx / ww) * (SWEEP1 - SWEEP0) - delay
                if 0 <= t < 4 and sx <= tip:
                    y = int(cut_y(sx) + sy * (0.4 + 0.35 * t) + 0.25 * t * t); x = int(sx + t * 1.5)
                    if 0 <= y < hh and 0 <= x < ww and not head[y * P, x * P]:
                        fr[y * P:(y + 1) * P, x * P:(x + 1) * P] = np.array([255, 240, 255.0]) if t < 2 else np.array([190, 120, 255.0])
            # where the blade bit, a short tapered nick stays for a while, and a little blood sprays out
            for dx, start in drips:
                if dx > tip:
                    continue
                since = f - SWEEP0 - (dx / ww) * (SWEEP1 - SWEEP0)
                strength = np.clip(since / 2.0, 0, 1) * np.clip((FRAMES - 4 - f) / 6.0, 0, 1)
                for o in range(-4, 5):
                    x, y = dx + o, int(round(cut_y(dx + o)))
                    rows = (y, y + 1) if abs(o) <= 1 else (y,)
                    for yy3 in rows:
                        if 0 <= x < ww and 0 <= yy3 < hh and not head[yy3 * P, x * P]:
                            a = strength * (1.0 if abs(o) <= 2 else 0.55)
                            tone = np.array([150, 10, 24.0]) if abs(o) <= 2 else np.array([90, 6, 18.0])
                            fr[yy3 * P:(yy3 + 1) * P, x * P:(x + 1) * P] = fr[yy3 * P:(yy3 + 1) * P, x * P:(x + 1) * P] * (1 - a) + a * tone
                for n in range(4):
                    t = since - 0.5
                    if 0 <= t < 4:
                        ang = (dx * 7 + n * 37) % 100 / 100.0 * np.pi + np.pi   # upwards, fanned out
                        x = int(dx + np.cos(ang) * (1.5 + 2.2 * t)); y = int(cut_y(dx) + np.sin(ang) * (1.5 + 2.0 * t) + 0.35 * t * t)
                        if 0 <= x < ww and 0 <= y < hh and not head[y * P, x * P]:
                            fr[y * P:(y + 1) * P, x * P:(x + 1) * P] = np.array([190, 14, 28.0])
        # blood: a bead swells on the cut, falls faster and faster, leaves a short trail
        for dx, start in drips:
            t = f - start
            if t < 0:
                continue
            y_top = cut_y(dx) + 1
            if t < 3:
                cells = [(0, 0), (1, 0)] if t < 2 else [(0, 0), (1, 0), (0, 1), (1, 1)]
            else:
                fall = 0.32 * (t - 2) ** 2
                cells = [(0, fall), (1, fall), (0, fall + 1), (1, fall + 1), (0, fall + 2), (1, fall + 2), (0, fall - 1), (0, fall - 2.5), (0, fall - 4.5)]
            for i, (ox, oy) in enumerate(cells):
                y, x = int(y_top + oy), dx + ox
                if 0 <= y < hh and not head[min(y * P, base.shape[0] - 1), x * P]:
                    tone = np.array([205, 16, 30.0]) if i < 6 else np.array([125, 8, 20.0])
                    fr[y * P:(y + 1) * P, x * P:(x + 1) * P] = tone
        # eyes: dim at rest, a double flare just before the blade goes by
        flare = max(np.exp(-((f - 2) / 1.2) ** 2), 0.7 * np.exp(-((f - 6) / 1.4) ** 2))
        fr[eye] = fr[eye] * (0.55 + 0.45 * flare) + flare * np.array([40, 30, 40.0])
        fr += (eye_halo * flare)[..., None] * np.array([70, 30, 130.0]) * head[..., None]
        fr[rune] *= 0.85 + 0.25 * np.sin(2 * np.pi * f / FRAMES)
        out_frames.append(fr)
    return out_frames

def throne_frames():
    """The Blood Master: blood rains behind the throne, a flash runs over the helm's eyes, light climbs
    the sword, the blood in the floor pulses away from the throne, the clouds glow from inside."""
    rng = np.random.default_rng(9)
    P = 2
    hh, ww = base.shape[0] // P, base.shape[1] // P
    k = base.shape[1] / W
    yy, xs = np.indices(base.shape[:2])
    sx, sy = xs / k, yy / k + y0                                # every texel's place in the source picture
    def box(x0, y_0, x1, y1):
        return (sx >= x0) & (sx < x1) & (sy >= y_0) & (sy < y1)
    red = (r > 150) & (g < 80) & (b < 80)
    eyes = box(1362, 248, 1448, 292) & (r > 190) & (g < 110)
    gems = red & (box(1388, 222, 1422, 252) | box(1388, 358, 1418, 388) | box(1068, 195, 1100, 235) | box(990, 285, 1015, 315) | box(1155, 285, 1180, 315))
    blade = box(1378, 400, 1428, 628) & (r > 160) & (g < 70)
    runes = box(1040, 262, 1132, 482) & red
    floor = (sy >= 588) & (r > 140) & (g < 60) & ~box(1010, 0, 1150, 2000) & ~box(1370, 0, 1435, 2000)
    sky = (sy < 470) & (r - g > 25)
    # what stands in front of the rain: the grey stone and the sword, not the red sky
    front = ndi.binary_dilation(box(840, 140, 1480, 640) & (r - g < 28), iterations=1) | (sy >= 596)
    lum = base.mean(-1) / 255
    drops = [(rng.integers(0, ww + 20), rng.integers(0, hh), rng.choice([4, 6])) for _ in range(90)]
    hot = np.array([255.0, 190.0, 170.0])
    def bump(t, at, width):
        gap = np.abs((t - at + 0.5) % 1.0 - 0.5)
        return np.exp(-(gap / width) ** 2)
    def glow(fr, mask, amount):
        a = np.clip(amount, 0, 1)
        a = a[..., None] if np.ndim(a) else a
        fr[mask] = (fr * (1 - a) + hot * a)[mask]
    out_frames = []
    for f in range(FRAMES):
        t = f / FRAMES
        fr = base.copy()
        # clouds: two soft red glows wander through them, the moon breathes
        for centre, at in ((0.30, 0.2), (0.74, 0.65)):
            power = bump(t, at, 0.12)
            near = np.exp(-((xs / base.shape[1] - centre) / 0.16) ** 2)
            fr[sky] *= (1 + 0.55 * power * near * (0.3 + lum))[sky][:, None]
        moon = box(318, 40, 525, 262) & (r > 120)
        fr[moon] *= 1.0 + 0.10 * np.sin(2 * np.pi * t)
        # rain, behind everything that stands on the floor
        for x0, phase, speed in drops:
            fall = (f * speed + phase) % hh
            long = 5 if speed == 6 else 3
            for n in range(long):
                y, x = int(fall + n), int(x0 - 0.3 * (fall + n))
                if 0 <= y < hh and 0 <= x < ww and not front[y * P, x * P]:
                    a = (0.35 + 0.5 * n / long) * (1.0 if speed == 6 else 0.6)
                    cell = fr[y * P:(y + 1) * P, x * P:(x + 1) * P]
                    cell[:] = cell * (1 - a) + np.array([235.0, 28.0, 36.0]) * a
        # the floor: a pulse of light runs from the throne out to both edges, twice a loop
        away = np.abs(sx - 1085) / 1100.0
        glow(fr, floor, 0.55 * np.maximum(bump(t - away * 0.5, 0.05, 0.07), bump(t - away * 0.5, 0.55, 0.07)))
        # the sword: light climbs the blade from the floor to the guard
        height = (628 - sy) / 228.0
        glow(fr, blade, 0.7 * bump(t - height * 0.25, 0.30, 0.06))
        # the helm: a flash runs across the eyes, then again weaker; the gems light up after it
        across = (sx - 1362) / 86.0
        flash = np.maximum(bump(t - across * 0.10, 0.62, 0.035), 0.6 * bump(t - across * 0.10, 0.74, 0.035))
        fr[eyes] *= 0.72
        glow(fr, eyes, 0.9 * flash)
        halo = ndi.binary_dilation(eyes, iterations=2) & ~eyes
        fr[halo] += (np.array([150.0, 15.0, 15.0]) * flash[..., None])[halo]
        glow(fr, gems, 0.6 * bump(t, 0.82, 0.07))
        fr[runes] *= 0.75 + 0.45 * bump(t, 0.45, 0.2)
        out_frames.append(fr)
    return out_frames

frames = strike_frames() if mode == "strike" else slash_frames() if mode == "slash" else throne_frames() if mode == "throne" else []
for f in range(0 if mode in ("strike", "slash", "throne") else FRAMES):
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
sheet = np.concatenate([frames[k] for k in ((2, 3, 4, 7, 10, 17) if mode == "strike" else (2, 5, 7, 10, 16, 22) if mode == "slash" else (2, 11, 20, 23, 27, 30) if mode == "throne" else (0, 2, 7))], 0).clip(0, 255).astype(np.uint8)
Image.fromarray(sheet, "RGB").save(out + "/banner_preview.png")
print("texture", strip.shape[1], strip.shape[0])
