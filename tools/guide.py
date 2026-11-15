"""The guide book's pictures: the open spread, the sheet of parts (arrows, chapter tabs, the crafting
panel, the divider, the banner frame) and the item icon.

    python3 guide.py <concepts folder> <resources/assets/tinyrelics folder>

Everything is drawn at two texels per GUI pixel; GuideScreen.java holds the same positions."""
import colorsys
import json
import os
import sys
import numpy as np
from PIL import Image
from scipy import ndimage as ndi

SRC, OUT = sys.argv[1], sys.argv[2]

def load(name):
    return np.array(Image.open(f"{SRC}/{name}.png").convert("RGB")).astype(int)

def cut(a, x0, y0, x1, y1, purple=False):
    """Background: anything pinkish-purple, where the drawing was blended into the magenta sheet too. The purple
    tab itself is such a colour, so for it only what is nearly the sheet's corner colour counts as background."""
    part = a[y0:y1, x0:x1]
    if purple:
        solid = np.abs(part - a[2, 2]).sum(-1) > 110
    else:
        solid = ~((part[..., 0] - part[..., 1] > 45) & (part[..., 2] - part[..., 1] > 45))
    return np.dstack([part, np.where(solid, 255, 0)]).astype(np.uint8)

def size(rgba, w, h):
    # see-through pixels take the colour of the nearest drawn one, so shrinking blends no magenta into the edge
    rgba = rgba.copy()
    hole = rgba[..., 3] == 0
    if hole.any() and (~hole).any():
        idx = ndi.distance_transform_edt(hole, return_distances=False, return_indices=True)
        rgba[..., :3] = rgba[idx[0], idx[1], :3]
    small = np.array(Image.fromarray(rgba, "RGBA").resize((w, h), Image.BOX))
    small[..., 3] = np.where(small[..., 3] > 127, 255, 0)
    return small

def save(a, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    Image.fromarray(a, "RGBA").save(path)

# ---- the spread: 400 x 233 GUI pixels
spread = load("guide_spread")
book = size(cut(spread, 83, 58, 1535, 904), 800, 466)
c = book[..., :3].astype(int)
book[(c[..., 0] - c[..., 1] > 35) & (c[..., 2] - c[..., 1] > 35), :3] = (38, 20, 12)     # the book has no purple: what is left of the sheet goes dark brown
save(book, f"{OUT}/textures/gui/guide_book.png")

# ---- the parts
parts = load("guide_parts")
sheet = np.zeros((512, 512, 4), np.uint8)
def put(img, x, y):
    sheet[y:y + img.shape[0], x:x + img.shape[1]] = img

for i, box in enumerate(((219, 38, 411, 218), (544, 38, 731, 217), (809, 38, 999, 217), (1122, 38, 1316, 216))):
    put(size(cut(parts, *box), 40, 32), i * 40, 0)                      # arrows: left, right, left lit, right lit

def recolour(tab, hue):
    """The blue tab painted another colour: the contents get a green one."""
    out = tab.copy()
    for y in range(tab.shape[0]):
        for x in range(tab.shape[1]):
            r, g, b = tab[y, x, :3] / 255.0
            h, l, s = colorsys.rgb_to_hls(r, g, b)
            if s > 0.35 and b > r:
                r, g, b = colorsys.hls_to_rgb(hue, l * 0.85, s * 0.8)
                out[y, x, :3] = (r * 255, g * 255, b * 255)
    return out

TABS = ((69, 261, 277, 417), (315, 261, 518, 417), (553, 261, 758, 417), (794, 261, 1000, 417), (1039, 261, 1243, 417))
tabs = [size(cut(parts, *box, purple=True), 60, 44) for box in TABS]
tabs = [recolour(tabs[0], 0.30)] + tabs + [recolour(tabs[0], 0.47)]   # contents, necromancer, assassin, blood, thor, shop, roadmap
for i, tab in enumerate(tabs):
    put(tab, i * 64, 40)
    lit = tab.astype(float)
    lit[..., :3] = np.clip(lit[..., :3] * 1.25 + 18, 0, 255)
    put(lit.astype(np.uint8), i * 64, 88)

put(size(cut(parts, 252, 460, 680, 851), 132, 130), 0, 140)            # the 3 x 3 grid
put(size(cut(parts, 710, 597, 866, 704), 32, 22), 140, 190)            # its arrow
put(size(cut(parts, 898, 528, 1197, 796), 60, 56), 180, 160)           # the result
put(size(cut(parts, 70, 872, 878, 966), 272, 32), 0, 280)              # the divider

# the banner frame, with its parchment middle cut out so the banner shows through
frame = cut(parts, 952, 860, 1463, 988)
c = frame[..., :3].astype(int)
paper = (c[..., 0] > 200) & (c[..., 1] > 165) & (c[..., 2] > 110)
labels, _ = ndi.label(paper)
frame[labels == labels[frame.shape[0] // 2, frame.shape[1] // 2], 3] = 0
# 136 x 40 GUI pixels: the opening then has the banners' own shape (162 x 36), so they fill it without squashing.
# What is left of the parchment inside the frame's dark inner line is cleared, so the banner reaches the line.
frame = size(frame, 272, 80)
dark = (frame[..., :3].astype(int).mean(-1) < 60) & (frame[..., 3] > 0)
row, col = dark[40], dark[:, 136]
inner_x = max(i for i in range(60) if row[i]) + 1
inner_y = max(i for i in range(30) if col[i]) + 1
inner_y2 = min(i for i in range(50, 80) if col[i])
frame[inner_y:inner_y2, inner_x:272 - inner_x] = 0
print("banner opening, in texels:", inner_x, inner_y, 272 - inner_x, inner_y2)
put(frame, 0, 320)
save(sheet, f"{OUT}/textures/gui/guide_parts.png")

# ---- the item
icon = cut(load("guide_icon"), 113, 32, 1126, 1179)
img = Image.fromarray(icon, "RGBA"); k = 62 / max(img.size)
small = np.array(img.resize((round(img.width * k), round(img.height * k)), Image.BOX)); small[..., 3] = np.where(small[..., 3] > 127, 255, 0)
item = np.zeros((64, 64, 4), np.uint8)
item[(64 - small.shape[0]) // 2:(64 - small.shape[0]) // 2 + small.shape[0], (64 - small.shape[1]) // 2:(64 - small.shape[1]) // 2 + small.shape[1]] = small
save(item, f"{OUT}/textures/item/guide_book.png")
os.makedirs(f"{OUT}/models/item", exist_ok=True)
with open(f"{OUT}/models/item/guide_book.json", "w") as f:
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "tinyrelics:item/guide_book"}}, f, indent=1)
