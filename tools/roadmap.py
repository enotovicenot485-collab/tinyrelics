"""The roadmap in the guide book: the map, and one atlas with every sprite that moves on it (heroes and their
parts, the ship, clouds, gulls, waves, petals, the marker, the plaques). Writes RoadmapArt.java with where each
sprite is in the atlas and the points the code hangs things on (a hero's feet, the pirate's shoulder...).

    python3 roadmap.py <concepts folder> <resources/assets/tinyrelics folder> <RoadmapArt.java>

Everything is stored at K texels per GUI pixel. The map is drawn MAP_W x MAP_H GUI pixels."""
import os
import sys
import numpy as np
from PIL import Image
from scipy import ndimage as ndi

SRC, OUT, JAVA = sys.argv[1], sys.argv[2], sys.argv[3]
K = 4
MAP_W, MAP_H = 332, 166
MAP_SCALE = MAP_W / 1774.0                       # GUI pixels per pixel of the map picture

def load(name):
    return np.array(Image.open(f"{SRC}/{name}.png").convert("RGB")).astype(int)

def cut(a, box, largest=False):
    x0, y0, x1, y1 = box
    part = a[y0:y1, x0:x1]
    solid = np.abs(part - a[2, 2]).sum(-1) > 90
    # the pink fringe where a sprite was blended into the magenta sheet
    solid &= ~((part[..., 0] - part[..., 1] > 60) & (part[..., 2] - part[..., 1] > 60) & (np.abs(part[..., 0] - part[..., 2]) < 90))
    if largest:
        labels, n = ndi.label(ndi.binary_dilation(solid, iterations=6))
        sizes = ndi.sum(solid, labels, range(1, n + 1))
        solid &= labels == 1 + int(np.argmax(sizes))
    rgba = np.dstack([part, np.where(solid, 255, 0)]).astype(np.uint8)
    hole = ~solid                                  # see-through pixels take the nearest colour: no magenta at the edges
    idx = ndi.distance_transform_edt(hole, return_distances=False, return_indices=True)
    rgba[..., :3] = rgba[idx[0], idx[1], :3]
    return rgba

def shrink(rgba, w, h):
    small = np.array(Image.fromarray(rgba, "RGBA").resize((max(1, w), max(1, h)), Image.BOX))
    small[..., 3] = np.where(small[..., 3] > 110, 255, 0)
    return small

# ---- the map
os.makedirs(f"{OUT}/textures/gui", exist_ok=True)
Image.open(f"{SRC}/roadmap_map.png").convert("RGB").resize((MAP_W * K, MAP_H * K), Image.BOX).save(f"{OUT}/textures/gui/roadmap_map.png")

# ---- the sprites: name, sheet, box, GUI pixels per pixel of the sheet, mirrored, only the biggest piece, points
P = 24.0 / 983          # the pirate
A = 29.0 / 850          # the archer
S = 26.0 / 865          # the shogun
SPRITES = [
    ("ARCHER", "hero_archer", (121, 12, 625, 862), A, False, False, {"FEET": (373, 860), "WING_L": (350, 365), "WING_R": (515, 365)}),
    ("WING_L", "hero_archer", (736, 144, 1228, 787), A * 0.8, False, False, {"ROOT": (1205, 405)}),
    ("WING_R", "hero_archer", (1257, 144, 1749, 787), A * 0.8, False, False, {"ROOT": (1280, 405)}),
    ("PIRATE", "hero_pirate", (37, 19, 517, 1002), P, True, False, {"FEET": (280, 1000), "SHOULDER": (440, 378), "PERCH": (135, 600)}),
    ("ARM", "hero_pirate", (597, 144, 1394, 335), P, False, False, {"PIVOT": (628, 248), "MUZZLE": (1392, 212)}),
    ("FLASH0", "hero_pirate", (643, 410, 762, 502), P * 1.6, False, False, {}),
    ("FLASH1", "hero_pirate", (845, 375, 1039, 529), P * 1.6, False, False, {}),
    ("FLASH2", "hero_pirate", (1108, 373, 1327, 531), P * 1.6, False, False, {}),
    ("SMOKE0", "hero_pirate", (630, 585, 800, 710), P * 1.8, False, False, {}),
    ("SMOKE1", "hero_pirate", (845, 560, 1065, 740), P * 1.8, False, False, {}),
    ("SMOKE2", "hero_pirate", (1110, 550, 1385, 730), P * 1.8, False, False, {}),
    ("PARROT0", "hero_pirate", (603, 800, 771, 981), P * 1.25, False, False, {"FEET": (687, 978)}),
    ("PARROT1", "hero_pirate", (840, 797, 1118, 992), P * 1.25, False, False, {}),
    ("PARROT2", "hero_pirate", (1156, 776, 1498, 989), P * 1.25, False, False, {}),
    ("SHOGUN", "hero_shogun", (20, 99, 435, 958), S, True, True, {"FEET": (228, 955), "EYE_L": (203, 210), "EYE_R": (262, 210), "BACK": (95, 430)}),
    ("STRIKE", "hero_shogun", (464, 93, 1428, 958), S, True, True, {"FEET": (655, 955)}),
    ("BANNER0", "hero_shogun", (1008, 32, 1143, 406), S, False, False, {"POLE": (1022, 404)}),
    ("BANNER1", "hero_shogun", (1198, 32, 1345, 406), S, False, False, {"POLE": (1212, 404)}),
    ("BANNER2", "hero_shogun", (1372, 33, 1514, 406), S, False, False, {"POLE": (1386, 404)}),
    ("SLASH0", "hero_shogun", (909, 559, 1141, 935), S * 1.1, True, False, {}),
    ("SLASH1", "hero_shogun", (1110, 583, 1338, 929), S * 1.1, True, False, {}),
    ("SLASH2", "hero_shogun", (1314, 597, 1507, 906), S * 1.1, True, False, {}),
    ("SHIP", "roadmap_parts", (49, 86, 489, 534), 58.0 / 440, False, False, {"WATER": (268, 512), "DECK": (290, 448)}),
    ("CLOUD0", "roadmap_parts", (482, 135, 829, 321), 0.1, False, False, {}),
    ("CLOUD1", "roadmap_parts", (502, 351, 887, 513), 0.1, False, False, {}),
    ("PETAL0", "roadmap_parts", (1395, 178, 1497, 290), 0.035, False, False, {}),
    ("PETAL1", "roadmap_parts", (1229, 188, 1317, 280), 0.035, False, False, {}),
    ("PETAL2", "roadmap_parts", (1261, 301, 1369, 393), 0.035, False, False, {}),
    ("PETAL3", "roadmap_parts", (1383, 377, 1497, 456), 0.035, False, False, {}),
    ("GULL0", "roadmap_gull", (87, 17, 369, 231), 0.032, False, False, {}),
    ("GULL1", "roadmap_gull", (469, 78, 748, 225), 0.032, False, False, {}),
    ("GULL2", "roadmap_gull", (809, 115, 1120, 225), 0.032, False, False, {}),
    ("GULL3", "roadmap_gull", (1188, 127, 1463, 345), 0.032, False, False, {}),
    ("GLIDE", "roadmap_gull", (99, 385, 672, 472), 0.032, False, False, {}),
    ("GULL_HIT", "roadmap_gull", (730, 300, 1205, 535), 0.032, False, False, {}),
    ("BURST0", "roadmap_gull", (70, 600, 270, 780), 0.032, False, False, {}),
    ("BURST1", "roadmap_gull", (360, 570, 700, 812), 0.032, False, False, {}),
    ("BURST2", "roadmap_gull", (750, 565, 1165, 825), 0.032, False, False, {}),
    ("BURST3", "roadmap_gull", (1235, 590, 1495, 775), 0.032, False, False, {}),
    ("FEATHER0", "roadmap_gull", (148, 868, 249, 955), 0.032, False, False, {}),
    ("FEATHER1", "roadmap_gull", (447, 858, 508, 972), 0.032, False, False, {}),
    ("FEATHER2", "roadmap_gull", (694, 880, 798, 950), 0.032, False, False, {}),
    ("FEATHER3", "roadmap_gull", (977, 868, 1074, 955), 0.032, False, False, {}),
    ("FEATHER4", "roadmap_gull", (1282, 874, 1385, 966), 0.032, False, False, {}),
    ("COON_FALL", "roadmap_raccoon", (142, 24, 437, 262), 0.052, False, False, {}),
    ("COON_LAND", "roadmap_raccoon", (588, 78, 891, 295), 0.052, False, False, {}),
    ("COON_DAZED", "roadmap_raccoon", (991, 23, 1236, 301), 0.052, False, False, {}),
    ("COON_UP", "roadmap_raccoon", (37, 356, 327, 524), 0.052, False, False, {}),
    ("COON_RUN0", "roadmap_raccoon", (385, 367, 645, 520), 0.052, False, False, {}),
    ("COON_RUN1", "roadmap_raccoon", (681, 373, 945, 524), 0.052, False, False, {}),
    ("COON_RUN2", "roadmap_raccoon", (992, 376, 1232, 524), 0.052, False, False, {}),
    ("COON_RUN3", "roadmap_raccoon", (1252, 363, 1502, 523), 0.052, False, False, {}),
    ("COON_SLIP", "roadmap_raccoon", (73, 571, 380, 800), 0.052, False, False, {}),
    ("COON_DIVE", "roadmap_raccoon", (506, 570, 821, 794), 0.052, False, False, {}),
    ("SPLASH0", "roadmap_raccoon", (37, 873, 190, 965), 0.052, False, False, {}),
    ("SPLASH1", "roadmap_raccoon", (241, 861, 414, 971), 0.052, False, False, {}),
    ("SPLASH2", "roadmap_raccoon", (475, 855, 595, 968), 0.052, False, False, {}),
    ("DUST0", "roadmap_raccoon", (665, 880, 815, 975), 0.052, False, False, {}),
    ("DUST1", "roadmap_raccoon", (873, 866, 1033, 977), 0.052, False, False, {}),
    ("DUST2", "roadmap_raccoon", (1085, 873, 1231, 977), 0.052, False, False, {}),
    ("COON_CLOUD", "roadmap_raccoon", (1306, 832, 1499, 977), 0.09, False, False, {}),
    ("WAVE0", "roadmap_parts", (858, 700, 1032, 844), 0.05, False, False, {}),
    ("WAVE1", "roadmap_parts", (1056, 672, 1262, 844), 0.05, False, False, {}),
    ("WAVE2", "roadmap_parts", (1285, 663, 1498, 845), 0.05, False, False, {}),
    ("PLAQUE0", "roadmap_map", (138, 0, 462, 156), MAP_SCALE * 1.35, False, False, {"TOP": (300, 0)}),
    ("PLAQUE1", "roadmap_map", (732, 0, 1038, 156), MAP_SCALE * 1.35, False, False, {"TOP": (885, 0)}),
    ("PLAQUE2", "roadmap_map", (1328, 0, 1642, 156), MAP_SCALE * 1.35, False, False, {"TOP": (1485, 0)}),
]

def plaque(a, box):
    """A plaque cut out of the map with its chains. The plaque is the rectangle inside its black outline; the
    chains are the dark columns above it. Everything else (sky, clouds) goes."""
    x0, y0, x1, y1 = box
    part = a[y0:y1, x0:x1]
    dark = part.mean(-1) < 60
    rows = np.where(dark.mean(1) > 0.55)[0]                     # the top and bottom outline of the board
    top, bottom = rows.min(), rows.max()
    cols = np.where(dark[top:bottom + 1].mean(0) > 0.55)[0]     # its left and right outline
    left, right = cols.min(), cols.max()
    keep = np.zeros(dark.shape, bool)
    keep[top:bottom + 1, left:right + 1] = True
    chain = np.where(dark[:top].mean(0) > 0.45)[0]              # columns that are dark all the way up: the chains
    for c in chain:
        keep[:top, max(0, c - 3):c + 4] |= part[:top, max(0, c - 3):c + 4].mean(-1) < 120
    keep &= part[..., 2] <= part[..., 0] + 25                     # sky is bluer than wood, gold and iron
    rgba = np.dstack([part, np.where(keep, 255, 0)]).astype(np.uint8)
    idx = ndi.distance_transform_edt(~keep, return_distances=False, return_indices=True)
    rgba[..., :3] = rgba[idx[0], idx[1], :3]
    return rgba

sheets = {}
images = []
for name, sheet, box, scale, largest, mirror, points in SPRITES:
    if sheet not in sheets:
        sheets[sheet] = load(sheet)
    a = sheets[sheet]
    rgba = plaque(a, box) if name.startswith("PLAQUE") else cut(a, box, largest)
    w, h = max(1, round((box[2] - box[0]) * scale * K)), max(1, round((box[3] - box[1]) * scale * K))
    img = shrink(rgba, w, h)
    pts = {}
    for key, (px, py) in points.items():
        fx = (px - box[0]) / (box[2] - box[0])
        fy = (py - box[1]) / (box[3] - box[1])
        pts[key] = ((1 - fx if mirror else fx) * w / K, fy * h / K)
    if mirror:
        img = img[:, ::-1].copy()
    images.append((name, img, pts))

# the treasure island cut from the map as drawn: drawn again over the gull, it hides her until she rises above it
drawn = np.array(Image.open(f"{OUT}/textures/gui/roadmap_map.png").convert("RGB")).astype(int)
ix0, iy0 = int(590 * MAP_SCALE * K), int(290 * MAP_SCALE * K)
ix1, iy1 = int(935 * MAP_SCALE * K), int(560 * MAP_SCALE * K)
island = drawn[iy0:iy1, ix0:ix1]
land = ~((island[..., 2] > island[..., 0] + 22) & (island[..., 2] >= island[..., 1]))
labels, n = ndi.label(land)
sizes = ndi.sum(land, labels, range(1, n + 1))
land = ndi.binary_fill_holes(labels == 1 + int(np.argmax(sizes)))
images.append(("ISLAND", np.dstack([island, np.where(land, 255, 0)]).astype(np.uint8), {}))
ISLAND_AT = (ix0 / K, iy0 / K)

# ---- shelf packing into one atlas
ATLAS = 1024
atlas = np.zeros((ATLAS, ATLAS, 4), np.uint8)
x = y = shelf = 0
placed = {}
for name, img, pts in sorted(images, key=lambda t: -t[1].shape[0]):
    h, w = img.shape[:2]
    if x + w + 2 > ATLAS:
        x, y, shelf = 0, y + shelf + 2, 0
    assert y + h <= ATLAS, "atlas full"
    atlas[y:y + h, x:x + w] = img
    placed[name] = (x, y, w, h, pts)
    x += w + 2
    shelf = max(shelf, h)
Image.fromarray(atlas, "RGBA").save(f"{OUT}/textures/gui/roadmap_sprites.png")

# ---- the path the marker walks, in GUI pixels of the map: from the archer's island over the sea to the castle
a = sheets["roadmap_map"] if "roadmap_map" in sheets else load("roadmap_map")
gold = (a[..., 0] > 200) & (a[..., 1] > 140) & (a[..., 2] < 110)
gold[:540] = False; gold[690:] = False; gold[:, :455] = False; gold[:, 1325:] = False
labels, n = ndi.label(gold)
dashes = []
for i in range(1, n + 1):
    size = (labels == i).sum()
    if 150 < size < 4000:
        cy, cx = ndi.center_of_mass(labels == i)
        dashes.append((cx, cy))
dashes.sort()
path = dashes                              # from the first golden dash to the last

with open(JAVA, "w") as f:
    f.write("package dev.tinyrelics.client;\n\n")
    f.write("/** Made by tools/roadmap.py: where each sprite of the roadmap is in its atlas, and its points. Do not edit by hand. */\n")
    f.write("final class RoadmapArt {\n")
    f.write(f"    static final int K = {K};\n    static final int ATLAS = {ATLAS};\n")
    f.write(f"    static final int MAP_W = {MAP_W};\n    static final int MAP_H = {MAP_H};\n")
    f.write(f"    /** GUI pixels of the map per pixel of the picture it was drawn from: places on the map are given in picture pixels. */\n")
    f.write(f"    static final float MAP_SCALE = {MAP_SCALE:.6f}F;\n\n")
    f.write("    /** u, v, width, height in texels of the atlas. */\n")
    for name, (x, y, w, h, pts) in sorted(placed.items()):
        f.write(f"    static final int[] {name} = {{{x}, {y}, {w}, {h}}};\n")
        for key, (px, py) in pts.items():
            f.write(f"    static final float[] {name}_{key} = {{{px:.2f}F, {py:.2f}F}};\n")
    f.write(f"    /** Where the island cut-out goes, in GUI pixels from the map's corner. */\n")
    f.write(f"    static final float[] ISLAND_AT = {{{ISLAND_AT[0]:.2f}F, {ISLAND_AT[1]:.2f}F}};\n")
    f.write("\n    /** The marker's path, in picture pixels of the map. */\n")
    f.write("    static final float[][] PATH = {" + ", ".join(f"{{{px:.0f}F, {py:.0f}F}}" for px, py in path) + "};\n\n")
    f.write("    private RoadmapArt() {\n    }\n}\n")
print("sprites", len(placed), "atlas rows used", y + shelf, "path points", len(path))
