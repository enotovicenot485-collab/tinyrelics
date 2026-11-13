"""Cuts a row of item icons out of a concept sheet with a plain green background.

    python3 icons.py <sheet> <out folder> <name> <name> ..."""
import sys
import numpy as np
from PIL import Image
src, out, names = sys.argv[1], sys.argv[2], sys.argv[3:]
im = np.array(Image.open(src).convert("RGB")).astype(int)
bg = (im[..., 1] - im[..., 0] > 40) & (im[..., 1] - im[..., 2] > 40)
cols = np.where((~bg).sum(0) > 2)[0]
groups, start, prev = [], cols[0], cols[0]
for c in cols[1:]:
    if c - prev > 36:                      # the two boots stand closer than that, so they stay one icon
        groups.append((start, prev)); start = c
    prev = c
groups.append((start, prev))
assert len(groups) == len(names), (groups, names)
for (x0, x1), name in zip(groups, names):
    rows = np.where((~bg[:, x0:x1 + 1]).sum(1) > 0)[0]
    y0, y1 = rows[0], rows[-1]
    side = max(x1 - x0, y1 - y0) + 1
    cx, cy = (x0 + x1) // 2, (y0 + y1) // 2
    canvas = np.zeros((side, side, 4), np.uint8)
    for j in range(side):
        for i in range(side):
            x, y = cx - side // 2 + i, cy - side // 2 + j
            if 0 <= x < im.shape[1] and 0 <= y < im.shape[0] and not bg[y, x]:
                canvas[j, i] = (*im[y, x], 255)
    small = np.array(Image.fromarray(canvas, "RGBA").resize((60, 60), Image.BOX)).astype(float)
    small[..., 3] = np.where(small[..., 3] > 127, 255, 0)
    icon = np.zeros((64, 64, 4), np.uint8); icon[2:62, 2:62] = small.astype(np.uint8)
    Image.fromarray(icon, "RGBA").save(f"{out}/{name}.png")
    print(name, x0, x1, y0, y1)
