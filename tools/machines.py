"""Builds everything the bone crusher and the soul press show, from the concept sheets:
block textures and 3D block models, the two windows with their moving parts, and the item icons.

    python3 machines.py <concepts folder> <resources/assets/tinyrelics folder>

Both window textures are 512x512 at two texels per GUI pixel; the numbers in LAYOUT below
must match MachineMenu (slots) and MachineScreen (moving parts)."""
import json
import os
import sys
import numpy as np
from PIL import Image
from scipy import ndimage as ndi

SRC, OUT = sys.argv[1], sys.argv[2]

def load(name):
    return np.array(Image.open(f"{SRC}/{name}.png").convert("RGB")).astype(int)

def magenta(a):
    return (a[..., 0] - a[..., 1] > 90) & (a[..., 2] - a[..., 1] > 90)

def cut(a, x0, y0, x1, y1):
    """A piece of a sheet as RGBA, the magenta background see-through, trimmed to what is drawn."""
    part = a[y0:y1, x0:x1]
    solid = ~magenta(part)
    # a pink fringe is left where the drawing was blended into the background
    fringe = (part[..., 0] - part[..., 1] > 45) & (part[..., 2] - part[..., 1] > 45)
    solid &= ~fringe
    rows, cols = np.where(solid.any(1))[0], np.where(solid.any(0))[0]
    part, solid = part[rows[0]:rows[-1] + 1, cols[0]:cols[-1] + 1], solid[rows[0]:rows[-1] + 1, cols[0]:cols[-1] + 1]
    return np.dstack([part, np.where(solid, 255, 0)]).astype(np.uint8)

def fit(rgba, w, h, keep_shape=True):
    """Scaled down into a w x h cell (centred, proportions kept), hard-edged."""
    img = Image.fromarray(rgba, "RGBA")
    if keep_shape:
        k = min(w / img.width, h / img.height)
        tw, th = max(1, round(img.width * k)), max(1, round(img.height * k))
    else:
        tw, th = w, h
    small = np.array(img.resize((tw, th), Image.BOX)).astype(float)
    small[..., 3] = np.where(small[..., 3] > 127, 255, 0)
    cell = np.zeros((h, w, 4), np.uint8)
    y, x = (h - th) // 2, (w - tw) // 2
    cell[y:y + th, x:x + tw] = small.astype(np.uint8)
    return cell

def paste(sheet, cell, x, y):
    sheet[y:y + cell.shape[0], x:x + cell.shape[1]] = cell

def save(a, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA").save(path)

# =====================================================================================
# The windows
# =====================================================================================
def inventory(sheet, cell, top, width=352):
    """The player's slots, drawn in the panel's own colours under the machine: 3 rows and the hotbar.
    top = first texel row; returns the texel row where the window ends."""
    height = 6 + 3 * 36 + 8 + 36 + 14
    area = sheet[top:top + height, :width]
    area[:] = (30, 29, 27, 255)
    area[:, :4] = area[:, -4:] = (8, 8, 8, 255); area[-4:] = (8, 8, 8, 255)
    area[:, 4:10] = area[:, -10:-4] = (86, 83, 76, 255); area[-10:-4, 4:-4] = (86, 83, 76, 255)
    area[:, 10:12] = area[:, -12:-10] = (52, 50, 46, 255); area[-12:-10, 10:-10] = (52, 50, 46, 255)
    for row in range(4):
        y = 6 + row * 36 + (8 if row == 3 else 0)
        for column in range(9):
            paste(area, cell, 14 + column * 36, y)
    return top + height

def window(name, panel_box, machine_px):
    a = load(name)
    x0, y0, x1, y1 = panel_box
    scale = 352.0 / (x1 - x0)                               # texels per pixel of the drawing
    top = cut(a, x0, y0, x1, y0 + machine_px)
    rows = int(round(machine_px * scale / 2)) * 2
    sheet = np.zeros((512, 512, 4), np.uint8)
    paste(sheet, fit(top, 352, rows, keep_shape=False), 0, 0)
    return a, sheet, scale, rows

def region(sheet, scale, px_box):
    """A rectangle of the drawing's panel, in texels of the sheet (even numbers: whole GUI pixels)."""
    return [int(round(v * scale / 2)) * 2 for v in px_box]

def dim(sheet, box):
    """The gauge when it is empty: the same picture without its fire."""
    x0, y0, x1, y1 = box
    part = sheet[y0:y1, x0:x1, :3].astype(float)
    grey = part.mean(-1, keepdims=True)
    green = (part[..., 1] - part[..., 2] > 30)[..., None]
    sheet[y0:y1, x0:x1, :3] = np.where(green, grey * 0.22 + 8, part)

LAYOUT = {}

def crusher_window():
    a, sheet, scale, rows = window("crusher_gui", (18, 53, 781, 974), 548)
    cell = fit(cut(a, 18 + 49, 53 + 559, 18 + 127, 53 + 637), 36, 36, keep_shape=False)
    end = inventory(sheet, cell, rows)
    gauge = region(sheet, scale, (90, 135, 152, 285))
    paste(sheet, sheet[gauge[1]:gauge[3], gauge[0]:gauge[2]].copy(), 360, 0)
    dim(sheet, gauge)
    # the roller, turned through an eighth of a turn in six steps (it has eight spikes)
    roller = Image.fromarray(cut(a, 797, 189, 1084, 480), "RGBA")
    side = max(roller.size)
    square = Image.new("RGBA", (side, side)); square.paste(roller, ((side - roller.width) // 2, (side - roller.height) // 2))
    for i in range(6):
        turned = np.array(square.rotate(-45.0 * i / 6, resample=Image.BICUBIC))
        paste(sheet, fit(turned, 68, 68), i * 72, 440)
    for i, box in enumerate(((1107, 286, 1214, 434), (1209, 352, 1282, 434), (1296, 316, 1372, 434))):
        paste(sheet, fit(cut(a, *box), 28, 28), 400 + i * 36, 0)
    for i, box in enumerate(((825, 520, 925, 742), (950, 520, 1048, 742), (1080, 520, 1190, 742), (1228, 520, 1340, 742))):
        paste(sheet, fit(cut(a, *box), 32, 48), 360 + i * 36, 80)
    b = load("soul_parts")
    for i, box in enumerate(((100, 40, 395, 335), (455, 40, 730, 355), (780, 45, 1065, 345))):
        paste(sheet, fit(cut(b, *box), 28, 28), 400 + i * 36, 36)
    for i, box in enumerate(((280, 465, 540, 815), (620, 430, 870, 835), (975, 375, 1250, 835), (1340, 350, 1580, 830))):
        paste(sheet, fit(cut(b, *box), 32, 56), 360 + i * 36, 140)
    save(sheet, f"{OUT}/textures/gui/bone_crusher.png")
    LAYOUT["crusher"] = dict(machine_rows=rows // 2, height=end // 2, gauge=[v // 2 for v in gauge])

def press_window():
    a, sheet, scale, rows = window("press_gui", (18, 96, 785, 920), 448)
    cell = fit(cut(a, 18 + 58, 96 + 472, 18 + 128, 96 + 542), 36, 36, keep_shape=False)
    end = inventory(sheet, cell, rows)
    gauge = region(sheet, scale, (172, 288, 213, 392))
    paste(sheet, sheet[gauge[1]:gauge[3], gauge[0]:gauge[2]].copy(), 360, 0)
    dim(sheet, gauge)
    # the head and its screw leave the panel and become parts that move; an empty shaft stays behind
    head, screw, shaft = region(sheet, scale, (325, 143, 455, 240)), region(sheet, scale, (355, 55, 425, 143)), region(sheet, scale, (326, 52, 454, 242))
    paste(sheet, sheet[head[1]:head[3], head[0]:head[2]].copy(), 390, 0)
    paste(sheet, sheet[screw[1]:screw[3], screw[0]:screw[2]].copy(), 460, 0)
    sheet[shaft[1]:shaft[3], shaft[0]:shaft[2]] = (14, 14, 14, 255)
    paste(sheet, fit(cut(a, 846, 550, 1037, 670), 28, 20), 360, 60)
    paste(sheet, fit(cut(a, 1106, 556, 1301, 669), 28, 20), 392, 60)
    for i, box in enumerate(((810, 700, 985, 890), (992, 700, 1175, 890), (1185, 700, 1345, 895))):
        paste(sheet, fit(cut(a, *box), 44, 44), 360 + i * 48, 90)
    save(sheet, f"{OUT}/textures/gui/soul_press.png")
    LAYOUT["press"] = dict(machine_rows=rows // 2, height=end // 2, gauge=[v // 2 for v in gauge],
                           head=[v // 2 for v in head], screw=[v // 2 for v in screw])

# =====================================================================================
# The blocks
# =====================================================================================
T = 64          # block textures are 64x64: four texels to a sixteenth of a block

def tiles(name, boxes):
    a = load(name)
    out = []
    for box in boxes:
        t = fit(cut(a, *box), T, T, keep_shape=False)
        hole = t[..., 3] < 200
        if hole.any():      # a chipped corner or a gap in the drawing: take the mirror image, else the nearest pixel
            mirrored = t[:, ::-1]
            use = hole & (mirrored[..., 3] >= 200)
            t[use] = mirrored[use]
            hole = t[..., 3] < 200
            if hole.any():
                idx = ndi.distance_transform_edt(hole, return_distances=False, return_indices=True)
                t = t[idx[0], idx[1]]
        t[..., 3] = 255
        out.append(t)
    return out

def is_fire(t):
    c = t[..., :3].astype(int)
    return (c[..., 1] - c[..., 0] > 40) & (c[..., 1] - c[..., 2] > 40)

def cold(t, box):
    """The furnace door with the fire out."""
    out = t.copy(); x0, y0, x1, y1 = box
    part = out[y0:y1, x0:x1, :3].astype(float)
    fire = is_fire(out[y0:y1, x0:x1])[..., None]
    out[y0:y1, x0:x1, :3] = np.where(fire, part.mean(-1, keepdims=True) * 0.18 + 10, part)
    return out

def burning(t, box, frames=8):
    """The fire alive: every tongue of flame brightens and sinks on its own beat."""
    x0, y0, x1, y1 = box
    strip = []
    for f in range(frames):
        frame = t.copy().astype(float)
        part = frame[y0:y1, x0:x1]
        fire = is_fire(t[y0:y1, x0:x1])
        for i in range(part.shape[1]):
            beat = 0.5 + 0.5 * np.sin(2 * np.pi * (f / frames + (i // 3) * 0.37))
            height = part.shape[0]
            for j in range(height):
                if fire[j, i]:
                    lift = (height - j) / height                      # the tips flicker most
                    tone = 0.75 + 0.55 * beat * (0.4 + lift)
                    part[j, i, :3] = np.clip(part[j, i, :3] * tone + np.array([30, 40, 0]) * beat * lift, 0, 255)
        strip.append(frame)
    return np.concatenate(strip, axis=0)

def animated(strip, path, frametime=2):
    save(strip, path)
    with open(path + ".mcmeta", "w") as f:
        json.dump({"animation": {"frametime": frametime}}, f)

def only(t, box, keep):
    """A raised detail on its own: everything outside it see-through."""
    out = np.zeros_like(t); x0, y0, x1, y1 = box
    mask = np.zeros(t.shape[:2], bool)
    mask[y0:y1, x0:x1] = keep(t[y0:y1, x0:x1, :3].astype(int))
    mask = ndi.binary_fill_holes(ndi.binary_closing(mask, iterations=1))
    labels, count = ndi.label(mask)
    if count > 1:
        sizes = ndi.sum(mask, labels, range(1, count + 1))
        mask = labels == 1 + int(np.argmax(sizes))
    out[mask] = t[mask]
    return out

def plain(colour, grain=6, seed=1):
    rng = np.random.default_rng(seed)
    t = np.zeros((16, 16, 4), np.uint8)
    t[..., :3] = np.clip(np.array(colour)[None, None, :] + rng.integers(-grain, grain + 1, (16, 16, 1)), 0, 255)
    t[..., 3] = 255
    return t

def element(frm, to, faces, rotation=None):
    """faces: {direction: texture} or {direction: (texture, [u0, v0, u1, v1])}. Without a rectangle the game
    projects the texture straight onto the face, so a tile drawn as a whole side lands where it was drawn."""
    out = {"from": list(frm), "to": list(to), "faces": {}}
    for direction, spec in faces.items():
        if isinstance(spec, tuple):
            out["faces"][direction] = {"texture": "#" + spec[0], "uv": [round(v, 3) for v in spec[1]]}
            if len(spec) > 2:
                out["faces"][direction]["rotation"] = spec[2]
        else:
            out["faces"][direction] = {"texture": "#" + spec}
    if rotation:
        out["rotation"] = rotation
    return out

def all_faces(texture, but=None):
    faces = {d: texture for d in ("north", "south", "west", "east", "up", "down")}
    faces.update(but or {})
    return faces

def write_model(name, textures, elements):
    model = {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
             "textures": dict({k: "tinyrelics:block/" + v for k, v in textures.items()}, particle="tinyrelics:block/" + textures["side"]),
             "elements": elements}
    path = f"{OUT}/models/block/{name}.json"
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(model, f, indent=1)

def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=1)

FACING = {"north": 0, "east": 90, "south": 180, "west": 270}

def crusher_block():
    top, front, side = tiles("crusher_block", ((60, 60, 670, 662), (764, 60, 1410, 662), (1513, 60, 2121, 662)))
    door = (18, 44, 46, 62)
    save(cold(front, door), f"{OUT}/textures/block/bone_crusher_front.png")
    animated(burning(front, door), f"{OUT}/textures/block/bone_crusher_front_lit.png")
    save(side, f"{OUT}/textures/block/bone_crusher_side.png")
    cog = np.zeros_like(side)                 # the cog wheel as a raised disc of its own
    yy, xx = np.indices(side.shape[:2])
    disc = (xx - 23.5) ** 2 + (yy - 22.5) ** 2 <= 17.0 ** 2
    cog[disc] = side[disc]
    save(cog, f"{OUT}/textures/block/bone_crusher_cog.png")
    save(only(front, (22, 14, 44, 38), lambda c: (c[..., 0] > 120) & (c[..., 0] - c[..., 2] > 12)), f"{OUT}/textures/block/bone_crusher_skull.png")
    save(plain((58, 56, 52)), f"{OUT}/textures/block/machine_iron.png")
    # the top: at rest, and working - the rollers turn towards each other and the bones between them jump
    still = top.copy(); glow = is_fire(still)
    still[glow, :3] = (still[glow, :3].astype(float).mean(-1, keepdims=True) * 0.3 + 8).astype(np.uint8)
    save(still, f"{OUT}/textures/block/bone_crusher_top.png")
    frames = []
    left, right, gap, rows = (14, 30), (36, 52), (30, 36), (12, 54)
    for f in range(8):
        frame = top.copy()
        frame[rows[0]:rows[1], left[0]:left[1]] = np.roll(top[rows[0]:rows[1], left[0]:left[1]], 2 * f, axis=1)
        frame[rows[0]:rows[1], right[0]:right[1]] = np.roll(top[rows[0]:rows[1], right[0]:right[1]], -2 * f, axis=1)
        frame[rows[0]:rows[1], gap[0]:gap[1]] = np.roll(top[rows[0]:rows[1], gap[0]:gap[1]], (0, 2, 4, 2, 0, -2, -4, -2)[f], axis=0)
        frames.append(frame)
    animated(np.concatenate(frames, axis=0), f"{OUT}/textures/block/bone_crusher_top_lit.png")
    I = "iron"
    elements = [
        # the body, a little inside the corner posts, and the hopper's rim on top of it
        element((0.5, 0, 0.5), (15.5, 12, 15.5), {"north": "front", "south": "side", "west": "side", "east": "side", "down": I, "up": "top"}),
        element((0.5, 12, 0.5), (15.5, 16, 2.5), {"north": "front", "south": I, "up": "top", "west": "side", "east": "side"}),
        element((0.5, 12, 13.5), (15.5, 16, 15.5), {"south": "side", "north": I, "up": "top", "west": "side", "east": "side"}),
        element((0.5, 12, 2.5), (2.5, 16, 13.5), {"west": "side", "east": I, "up": "top"}),
        element((13.5, 12, 2.5), (15.5, 16, 13.5), {"east": "side", "west": I, "up": "top"}),
        # two rollers lying in the hopper
        element((3.5, 12, 3), (7.5, 15, 13.5), {"up": "top", "north": I, "south": I, "west": ("top", [3.5, 3, 6.5, 13.5], 90), "east": ("top", [4.5, 3, 7.5, 13.5], 90)}),
        element((9, 12, 3), (13, 15, 13.5), {"up": "top", "north": I, "south": I, "west": ("top", [9, 3, 12, 13.5], 90), "east": ("top", [10, 3, 13, 13.5], 90)}),
    ]
    # corner posts stand out of every side
    for x in (0, 13):
        for z in (0, 13):
            faces = {"north": "front" if z == 0 else I, "south": "side" if z == 13 else I,
                     "west": "side" if x == 0 else I, "east": "side" if x == 13 else I, "up": "top", "down": I}
            elements.append(element((x, 0, z), (x + 3, 16, z + 3), faces))
    # raised on the front: the skull and the frame of the furnace door
    elements.append(element((5, 6.5, 0), (11, 12.5, 0.5), all_faces("skull")))
    elements.append(element((3.5, 0, 0), (12.5, 6, 0.5), {"north": "front", "up": I, "west": I, "east": I}))
    # raised on the other three sides: the cog
    # (the disc covers tile columns 1.75..10.25 and rows 1.5..10; the game projects the tile onto each side)
    elements.append(element((1.75, 6, 15.5), (10.25, 14.5, 16), {"south": "cog"}))
    elements.append(element((0, 6, 1.75), (0.5, 14.5, 10.25), {"west": "cog"}))
    elements.append(element((15.5, 6, 5.75), (16, 14.5, 14.25), {"east": "cog"}))
    textures = dict(top="bone_crusher_top", front="bone_crusher_front", side="bone_crusher_side", cog="bone_crusher_cog",
                    skull="bone_crusher_skull", iron="machine_iron")
    write_model("bone_crusher", textures, elements)
    write_model("bone_crusher_lit", dict(textures, top="bone_crusher_top_lit", front="bone_crusher_front_lit"), elements)
    variants = {}
    for facing, turn in FACING.items():
        for lit in (False, True):
            variants[f"facing={facing},lit={str(lit).lower()}"] = dict(model="tinyrelics:block/bone_crusher" + ("_lit" if lit else ""), **({"y": turn} if turn else {}))
    write_json(f"{OUT}/blockstates/bone_crusher.json", {"variants": variants})
    write_json(f"{OUT}/models/item/bone_crusher.json", {"parent": "tinyrelics:block/bone_crusher"})

def press_block():
    top, front = tiles("press_block", ((56, 57, 687, 666), (777, 25, 1395, 683)))
    door = (18, 46, 46, 63)
    # the three sides without a door: the corners of the front with plain boards between them
    side = front.copy()
    tone = front[46:60, 12:16, :3].reshape(-1, 3).mean(0)
    for x in range(14, 50):
        for y in range(42, 62):
            shade = 0.6 if (x - 14) % 6 == 0 else 1.1 if (x - 14) % 6 == 1 else 0.9 + 0.1 * ((x * 7 + y * 13) % 3)
            side[y, x, :3] = np.clip(tone * shade, 0, 255)
    save(cold(front, door), f"{OUT}/textures/block/soul_press_front.png")
    animated(burning(front, door), f"{OUT}/textures/block/soul_press_front_lit.png")
    save(side, f"{OUT}/textures/block/soul_press_side.png")
    save(top, f"{OUT}/textures/block/soul_press_top.png")
    save(plain((112, 78, 50), 10, 3), f"{OUT}/textures/block/soul_press_grip.png")
    save(plain((206, 196, 172), 8, 5), f"{OUT}/textures/block/bone_plain.png")
    I = "iron"
    POST, BEAM, PLATE, SCREW, MOULD = [0.7, 3.2, 3.7, 10.5], [0, 0.8, 16, 3.3], [3.7, 5.3, 12.3, 7], [6.3, 1, 9.7, 5.3], [4, 9, 12, 10.5]
    def build(pressed):
        y = 7.5 if pressed else 9.0
        elements = [
            # the base with the furnace in it
            element((0, 0, 0), (16, 5, 16), {"north": "front", "south": "side", "west": "side", "east": "side", "down": I, "up": "top"}),
            # the mould on it
            element((4, 5, 4.5), (12, 6.5, 11.5), {"north": ("front", MOULD), "south": ("front", MOULD), "west": ("front", MOULD), "east": ("front", MOULD), "up": I}),
            # the frame: two posts and the beam across them
            element((1, 5, 6), (4, 12.5, 10), {d: ("front", POST) for d in ("north", "south", "west", "east")}),
            element((12, 5, 6), (15, 12.5, 10), {d: ("front", POST) for d in ("north", "south", "west", "east")}),
            element((0, 12.5, 5.5), (16, 15.5, 10.5), {"north": ("front", BEAM), "south": ("front", BEAM), "west": ("front", [0, 0.8, 3.2, 3.3]),
                                                        "east": ("front", [0, 0.8, 3.2, 3.3]), "up": "top", "down": I}),
            element((6, 15.5, 6), (10, 16, 10), all_faces(I, {"up": "top"})),
            # the screw and the plate it drives
            element((6.5, y + 1.5, 6.5), (9.5, 12.5, 9.5), {d: ("front", SCREW) for d in ("north", "south", "west", "east")}),
            element((3.5, y, 4), (12.5, y + 1.5, 12), {"north": ("front", PLATE), "south": ("front", PLATE), "west": ("front", PLATE), "east": ("front", PLATE),
                                                       "up": I, "down": I}),
            # the lever on the side: a pivot, an iron arm and a wooden grip
            element((15, 6, 7), (16, 8, 9), all_faces(I)),
        ]
        turn = {"origin": [15.5, 7, 8], "axis": "x", "angle": -45 if pressed else 45}
        elements.append(element((15.2, 7, 7.5), (15.8, 12, 8.5), all_faces(I), turn))
        elements.append(element((15, 12, 7.3), (16, 15, 8.7), all_faces("grip"), turn))
        if pressed:
            elements.append(element((5.5, 6.5, 6), (10.5, 7.5, 10), all_faces("bone")))
        return elements
    textures = dict(top="soul_press_top", front="soul_press_front", side="soul_press_side", iron="machine_iron",
                    grip="soul_press_grip", bone="bone_plain")
    for pressed in (False, True):
        for lit in (False, True):
            name = "soul_press" + ("_down" if pressed else "") + ("_lit" if lit else "")
            write_model(name, dict(textures, front="soul_press_front_lit") if lit else textures, build(pressed))
    variants = {}
    for facing, turn in FACING.items():
        for lit in (False, True):
            for pressed in (False, True):
                name = "soul_press" + ("_down" if pressed else "") + ("_lit" if lit else "")
                variants[f"facing={facing},lit={str(lit).lower()},pressed={str(pressed).lower()}"] = dict(model="tinyrelics:block/" + name, **({"y": turn} if turn else {}))
    write_json(f"{OUT}/blockstates/soul_press.json", {"variants": variants})
    write_json(f"{OUT}/models/item/soul_press.json", {"parent": "tinyrelics:block/soul_press"})

def icons():
    a = load("bone_icons")
    for name, box in (("bone_powder", (30, 240, 400, 520)), ("bone_ingot", (432, 212, 817, 509)), ("soul", (884, 135, 1148, 595))):
        icon = np.zeros((64, 64, 4), np.uint8)
        paste(icon, fit(cut(a, *box), 60, 60), 2, 2)
        save(icon, f"{OUT}/textures/item/{name}.png")
        write_json(f"{OUT}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "tinyrelics:item/" + name}})

crusher_window(); press_window(); crusher_block(); press_block(); icons()
print(json.dumps(LAYOUT))
