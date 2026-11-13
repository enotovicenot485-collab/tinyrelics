"""Builds the blood altar's pictures from the concept sheets: three block faces and the
window (background with the slots baked in, plus the parts that move).

    python3 altar.py <concepts folder> <out folder>

The window texture is 512x512 at two texels per GUI pixel:
  (0, 0)     352x332  the window: 176x166 GUI pixels
  (0, 340)   104x20   the channel full of blood
  (112, 340) 28x76    the vial full of water
  (160, 340) 4 x 52x52 (step 56) the basin, four frames
  (0, 420)   36x36    a glowing slot
The slot positions here must match BloodAltarMenu, the rest BloodAltarScreen."""
import sys
import numpy as np
from PIL import Image
from scipy import ndimage as ndi

src, out = sys.argv[1], sys.argv[2]

def green(a):
    a = a.astype(int)
    return (a[..., 1] - a[..., 0] > 40) & (a[..., 1] - a[..., 2] > 40)

def pieces(path):
    """Every separate thing on a green sheet, as RGBA crops, in reading order."""
    a = np.array(Image.open(path).convert("RGB"))
    solid = ~green(a)
    labels, _ = ndi.label(ndi.binary_dilation(solid, iterations=6))
    found = []
    for box in ndi.find_objects(labels):
        ys, xs = box
        if (ys.stop - ys.start) * (xs.stop - xs.start) < 3000:
            continue
        mask = solid[box]
        rows, cols = np.where(mask.any(1))[0], np.where(mask.any(0))[0]
        y0, y1, x0, x1 = ys.start + rows[0], ys.start + rows[-1] + 1, xs.start + cols[0], xs.start + cols[-1] + 1
        rgba = np.dstack([a[y0:y1, x0:x1], np.where(solid[y0:y1, x0:x1], 255, 0).astype(np.uint8)])
        found.append(((y0 + y1) // 2, x0, Image.fromarray(rgba, "RGBA")))
    found.sort(key=lambda p: (p[0] // 250, p[1]))      # rows by the height of their middle
    return [p[2] for p in found]

def fit(img, w, h):
    small = np.array(img.resize((w, h), Image.BOX)).astype(float)
    small[..., 3] = np.where(small[..., 3] > 127, 255, 0)
    return Image.fromarray(small.astype(np.uint8), "RGBA")

# ---- block faces: opaque 64x64 tiles
for name, tile in zip(("top", "side", "bottom"), pieces(src + "/altar_block.png")):
    t = np.array(tile.resize((64, 64), Image.BOX))
    hole = t[..., 3] < 200
    if hole.any():      # the drawing's edge is not perfectly straight: fill from the nearest wood
        idx = ndi.distance_transform_edt(hole, return_distances=False, return_indices=True)
        t = t[idx[0], idx[1]]
    t[..., 3] = 255
    Image.fromarray(t, "RGBA").save(f"{out}/blood_altar_{name}.png")

# ---- the window
S = 2
parts = pieces(src + "/altar_gui_parts.png")
slot, slot_glow, channel, channel_full, vial, vial_full = parts[:6]
basins = parts[6:10]
assert len(parts) == 10, len(parts)
bg = Image.open(src + "/altar_gui_bg.png").convert("RGBA")
W, H = bg.size
split = int(H * 0.478)                  # the gold line between the stone and the plain panel
sheet = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
# the stone takes the top 70 GUI pixels, the panel the rest, so the inventory label sits just under the line
sheet.paste(bg.crop((0, 0, W, split)).resize((176 * S, 70 * S), Image.BOX), (0, 0))
sheet.paste(bg.crop((0, split, W, H)).resize((176 * S, 96 * S), Image.BOX), (0, 70 * S))

def put(img, x, y, w, h):
    sheet.alpha_composite(fit(img, w * S, h * S), (x * S, y * S))

SLOTS = {"water": (14, 17), "input": (56, 35), "output": (130, 35), "bottles": (14, 53)}
for x, y in SLOTS.values():
    put(slot, x - 1, y - 1, 18, 18)
for row in range(3):
    for col in range(9):
        put(slot, 7 + col * 18, 83 + row * 18, 18, 18)
for col in range(9):
    put(slot, 7 + col * 18, 141, 18, 18)
put(channel, 74, 38, 52, 10)
put(vial, 34, 24, 14, 38)
put(basins[0], 87, 30, 26, 26)
# the moving parts
sheet.alpha_composite(fit(channel_full, 52 * S, 10 * S), (0, 340))
full = fit(vial_full, 14 * S, 38 * S)
sheet.alpha_composite(full, (112, 340))
for i, basin in enumerate(basins):
    sheet.alpha_composite(fit(basin, 26 * S, 26 * S), (160 + i * 56, 340))
sheet.alpha_composite(fit(slot_glow, 18 * S, 18 * S), (0, 420))
sheet.save(f"{out}/blood_altar.png")
f = np.array(full).astype(int)
blue = (f[..., 2] > 150) & (f[..., 2] - f[..., 0] > 80)
rows = np.where(blue.sum(1) > 4)[0]
print("water rows in the vial, GUI pixels from its top:", rows[0] / S, (rows[-1] + 1) / S)
Image.fromarray(np.array(sheet)[:332, :352], "RGBA").resize((704, 664), Image.NEAREST).save(f"{out}/window_preview.png")
