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
GW, GH, SCALE, FRAMES = 162, 36, 4, 12
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

frames = []
for f in range(FRAMES):
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
sheet = np.concatenate([frames[0], frames[2], frames[7]], 0).clip(0, 255).astype(np.uint8)
Image.fromarray(sheet, "RGB").save(out + "/banner_preview.png")
print("texture", strip.shape[1], strip.shape[0])
