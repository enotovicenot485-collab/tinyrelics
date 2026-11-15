"""The Charm of Keeping's own breaking effect: one 512x512 sheet for the screen animation
(CharmEffect.java knows where everything is on it) and the frames of the golden spark particle.

    python3 charm_effect.py <concepts folder> <resources/assets/tinyrelics folder>"""
import json
import os
import sys
import numpy as np
from PIL import Image

SRC, OUT = sys.argv[1], sys.argv[2]

def cut(name, box):
    a = np.array(Image.open(f"{SRC}/{name}.png").convert("RGB")).astype(int)
    part = a[box[1]:box[3], box[0]:box[2]]
    # magenta, and the pink fringe where the gold was blended into it
    solid = ~((part[..., 0] - part[..., 1] > 60) & (part[..., 2] - part[..., 1] > 60))
    rows, cols = np.where(solid.any(1))[0], np.where(solid.any(0))[0]
    part, solid = part[rows[0]:rows[-1] + 1, cols[0]:cols[-1] + 1], solid[rows[0]:rows[-1] + 1, cols[0]:cols[-1] + 1]
    return Image.fromarray(np.dstack([part, np.where(solid, 255, 0)]).astype(np.uint8), "RGBA")

def cell(img, size):
    k = min(size / img.width, size / img.height)
    small = np.array(img.resize((max(1, round(img.width * k)), max(1, round(img.height * k))), Image.BOX))
    small[..., 3] = np.where(small[..., 3] > 127, 255, 0)
    out = np.zeros((size, size, 4), np.uint8)
    y, x = (size - small.shape[0]) // 2, (size - small.shape[1]) // 2
    out[y:y + small.shape[0], x:x + small.shape[1]] = small
    return out

sheet = np.zeros((512, 512, 4), np.uint8)
def put(img, x, y):
    sheet[y:y + img.shape[0], x:x + img.shape[1]] = img

put(cell(cut("charm_circle", (0, 0, 1254, 1254)), 256), 0, 0)
for i, box in enumerate(((40, 40, 340, 470), (410, 40, 730, 470), (780, 40, 1130, 470), (1160, 30, 1525, 470))):
    put(cell(cut("charm_cracks", box), 64), 256 + i * 64, 0)
for i, box in enumerate(((60, 530, 320, 960), (390, 530, 760, 960), (770, 530, 1150, 960), (1170, 530, 1525, 960))):
    put(cell(cut("charm_cracks", box), 128), 256 + (i % 2) * 128, 64 + (i // 2) * 128)
SPARKS = ((140, 250, 245, 355), (410, 180, 640, 420), (750, 100, 1130, 490), (1200, 160, 1470, 450))
for i, box in enumerate(SPARKS):
    put(cell(cut("charm_particles", box), 32), i * 32, 256)
for i, box in enumerate(((50, 620, 255, 840), (300, 620, 500, 840), (545, 620, 755, 840), (810, 620, 1000, 840), (1060, 620, 1240, 840), (1290, 620, 1490, 840))):
    put(cell(cut("charm_particles", box), 32), i * 32, 288)
os.makedirs(f"{OUT}/textures/gui", exist_ok=True)
Image.fromarray(sheet, "RGBA").save(f"{OUT}/textures/gui/charm_effect.png")

# the spark as a particle in the world: it swells and fades
os.makedirs(f"{OUT}/textures/particle", exist_ok=True)
os.makedirs(f"{OUT}/particles", exist_ok=True)
order = (0, 1, 2, 1, 3)
for n, i in enumerate(order):
    Image.fromarray(cell(cut("charm_particles", SPARKS[i]), 16), "RGBA").save(f"{OUT}/textures/particle/charm_spark_{n}.png")
with open(f"{OUT}/particles/charm_spark.json", "w") as f:
    json.dump({"textures": [f"tinyrelics:charm_spark_{n}" for n in range(len(order))]}, f, indent=2)
