"""Builds everything the shadow loom and the phantom spooler show, from the concept sheets:
block textures and 3D block models (with the parts the game turns and slides), the two windows
with their moving parts, and the item icons.

    python3 shadow_machines.py <concepts folder> <resources/assets/tinyrelics folder>

The numbers here must match ShadowMachineMenu (slots), ShadowMachineScreen (moving parts) and
ShadowMachineRenderer (where the shuttle and the reel sit)."""
import json
import numpy as np
from PIL import Image
from scipy import ndimage as ndi
import machines as m
from machines import OUT, fit, paste, save, element, all_faces, write_model, write_json, plain, animated, FACING

def background(a):
    """These sheets are full of purple, which is close to the magenta they are drawn on:
    only what is nearly the colour of the sheet's corner is background."""
    return np.abs(a - a[2, 2]).sum(-1) < 70

def cut(a, x0, y0, x1, y1):
    part = a[y0:y1, x0:x1]
    solid = ~(np.abs(part - a[2, 2]).sum(-1) < 70)
    rows, cols = np.where(solid.any(1))[0], np.where(solid.any(0))[0]
    part, solid = part[rows[0]:rows[-1] + 1, cols[0]:cols[-1] + 1], solid[rows[0]:rows[-1] + 1, cols[0]:cols[-1] + 1]
    return np.dstack([part, np.where(solid, 255, 0)]).astype(np.uint8)

m.magenta, m.cut = background, cut          # the shared helpers (tiles, window) now cut these sheets correctly

def inventory(sheet, top):
    """The player's slots under the machine, in the machine's own dark wood and steel."""
    height = 6 + 3 * 36 + 8 + 36 + 14
    area = sheet[top:top + height, :352]
    area[:] = (36, 31, 30, 255)
    for y in range(0, height, 12):                                   # planks
        area[y:y + 1] = (24, 20, 20, 255)
    area[:, :4] = area[:, -4:] = (8, 8, 10, 255); area[-4:] = (8, 8, 10, 255)
    area[:, 4:10] = area[:, -10:-4] = (92, 90, 100, 255); area[-10:-4, 4:-4] = (92, 90, 100, 255)
    area[:, 10:12] = area[:, -12:-10] = (52, 50, 58, 255); area[-12:-10, 10:-10] = (52, 50, 58, 255)
    cell = np.zeros((36, 36, 4), np.uint8); cell[:] = (16, 14, 16, 255)
    cell[2:34, 2:34] = (98, 96, 106, 255); cell[4:34, 4:34] = (64, 62, 70, 255); cell[4:32, 4:32] = (26, 24, 27, 255)
    for row in range(4):
        y = 6 + row * 36 + (8 if row == 3 else 0)
        for column in range(9):
            paste(area, cell, 14 + column * 36, y)
    return top + height

def window(name, box):
    a = m.load(name)
    scale = 352.0 / (box[2] - box[0])
    rows = int(round((box[3] - box[1]) * scale / 2)) * 2
    sheet = np.zeros((512, 512, 4), np.uint8)
    paste(sheet, fit(cut(a, *box), 352, rows, keep_shape=False), 0, 0)
    return a, sheet, rows, inventory(sheet, rows)

def turned(sprite, steps, step_degrees):
    img = Image.fromarray(sprite, "RGBA")
    side = max(img.size)
    square = Image.new("RGBA", (side, side)); square.paste(img, ((side - img.width) // 2, (side - img.height) // 2))
    return [np.array(square.rotate(-step_degrees * i, resample=Image.BICUBIC)) for i in range(steps)]

def loom_window():
    a, sheet, rows, end = window("loom_gui", (15, 59, 917, 648))
    cloth = cut(a, 1002, 328, 1616, 634)                    # inside the ragged edge
    paste(sheet, fit(cloth, 122, 104, keep_shape=False), 360, 0)
    paste(sheet, fit(cut(a, 981, 101, 1635, 258), 60, 16), 360, 110)
    paste(sheet, fit(cut(a, 1697, 72, 2041, 412), 40, 40), 430, 110)
    for i, box in enumerate(((1723, 440, 1854, 581), (1948, 448, 2037, 535), (1833, 521, 1942, 653), (1968, 583, 2006, 630))):
        paste(sheet, fit(cut(a, *box), 16, 16), 360 + i * 20, 160)
    save(sheet, f"{OUT}/textures/gui/shadow_loom.png")
    return dict(machine_rows=rows // 2, height=end // 2)

def spooler_window():
    a, sheet, rows, end = window("spooler_gui", (13, 122, 772, 590))
    for i, frame in enumerate(turned(cut(a, 793, 223, 1052, 484), 4, 15)):      # six spokes: the same again after 60 degrees
        paste(sheet, fit(frame, 80, 80), i * 84, end + 4)
    for i, frame in enumerate(turned(cut(a, 1073, 223, 1330, 484), 4, 15)):
        paste(sheet, fit(frame, 80, 80), 360, i * 84)
    paste(sheet, fit(cut(a, 1363, 193, 1656, 577), 28, 40), 446, 0)
    paste(sheet, fit(cut(a, 1662, 221, 1923, 487), 44, 44), 446, 50)
    for i, box in enumerate(((1958, 221, 2035, 352), (2009, 308, 2085, 429), (1937, 371, 2016, 505))):
        paste(sheet, fit(cut(a, *box), 16, 24), 446 + i * 20, 100)
    save(sheet, f"{OUT}/textures/gui/phantom_spooler.png")
    return dict(machine_rows=rows // 2, height=end // 2, empty_reels_at=end + 4)

def moved(elements, dx, dy, dz):
    out = json.loads(json.dumps(elements))
    for e in out:
        for key in ("from", "to"):
            e[key] = [e[key][0] + dx, e[key][1] + dy, e[key][2] + dz]
        if "rotation" in e:
            o = e["rotation"]["origin"]; e["rotation"]["origin"] = [o[0] + dx, o[1] + dy, o[2] + dz]
    return out

def solid(frm, to, texture, rotation=None):
    """A box of a moving part, every face given its own rectangle of the texture. Left to the game, a box that
    reaches outside its block (the crank does) takes its picture from outside the texture - from other textures."""
    size = [to[i] - frm[i] for i in range(3)]
    def rect(a, b):
        return (texture, [0, 0, min(16, max(1, a)), min(16, max(1, b))])
    faces = {"north": rect(size[0], size[1]), "south": rect(size[0], size[1]), "west": rect(size[2], size[1]),
             "east": rect(size[2], size[1]), "up": rect(size[0], size[2]), "down": rect(size[0], size[2])}
    return element(frm, to, faces, rotation)

def eight(frm, to, texture, axis):
    """A box and the same box turned an eighth of a turn about the block's middle: an eight-sided roll."""
    return [solid(frm, to, texture), solid(frm, to, texture, {"origin": [8, 8, 8], "axis": axis, "angle": 45})]

def wound(colour, seed):
    """Thread wound on a reel: rows of lighter and darker turns."""
    t = plain(colour, 6, seed)
    for y in range(16):
        t[y, :, :3] = np.clip(t[y, :, :3].astype(int) + (14, -4, -16)[y % 3], 0, 255)
    return t

def boards(tile, box, tone_box):
    """Plain boards painted over a part of a tile."""
    out = tile.copy(); x0, y0, x1, y1 = box; tx0, ty0, tx1, ty1 = tone_box
    tone = tile[ty0:ty1, tx0:tx1, :3].reshape(-1, 3).mean(0)
    for x in range(x0, x1):
        for y in range(y0, y1):
            shade = 0.6 if (x - x0) % 7 == 0 else 1.1 if (x - x0) % 7 == 1 else 0.9 + 0.1 * ((x * 7 + y * 13) % 3)
            out[y, x, :3] = np.clip(tone * shade, 0, 255)
    return out

def loom_block():
    top, front, side = m.tiles("loom_block", ((26, 25, 719, 682), (778, 24, 1408, 680), (1494, 24, 2133, 680)))
    save(top, f"{OUT}/textures/block/shadow_loom_top.png")
    save(front, f"{OUT}/textures/block/shadow_loom_front.png")
    # the drawing's sides show a painted spool; the real spools sit in niches, so the sides are plain boards
    save(boards(side, (12, 12, 52, 52), (2, 14, 10, 50)), f"{OUT}/textures/block/shadow_loom_side.png")
    save(plain((44, 37, 34), 5, 11), f"{OUT}/textures/block/shadow_wood.png")
    save(plain((118, 84, 52), 8, 12), f"{OUT}/textures/block/shuttle_wood.png")
    save(wound((138, 62, 214), 13), f"{OUT}/textures/block/shuttle_thread.png")
    save(plain((104, 102, 112), 7, 22), f"{OUT}/textures/block/shadow_steel.png")
    # the weaving itself, stretched across the open top: threads and cloth, nothing between the threads
    c = top[..., :3].astype(int)
    keep = ((c[..., 2] > c[..., 1] + 25) & (c[..., 2] > 70)) | ((c[..., 0] > 120) & (c[..., 0] > c[..., 2] + 20))
    keep[:10] = keep[-10:] = False; keep[:, :10] = keep[:, -10:] = False
    weave = np.zeros_like(top); weave[keep] = top[keep]
    save(weave, f"{OUT}/textures/block/shadow_loom_weave.png")
    frames = []
    for f in range(8):          # at work a glow runs down the threads
        frame = weave.copy().astype(float)
        for y in range(64):
            wave = max(0.0, np.cos(2 * np.pi * (f / 8.0 - y / 40.0))) ** 3
            frame[y, :, :3] = np.clip(frame[y, :, :3] * (1 + 0.55 * wave) + 28 * wave, 0, 255)
        frame[~keep] = 0
        frames.append(frame)
    animated(np.concatenate(frames, axis=0), f"{OUT}/textures/block/shadow_loom_weave_lit.png")
    W = "wood"
    elements = [
        # the foot, and above it a narrower middle: what is left open at the two sides are the niches for the spools
        element((0.5, 0, 0.5), (15.5, 5, 15.5), {"north": "front", "south": "front", "west": "side", "east": "side", "down": W, "up": W}),
        element((4, 5, 0.5), (12, 11, 15.5), {"north": "front", "south": "front", "west": W, "east": W, "up": W}),
        element((0.5, 11, 0.5), (15.5, 16, 2.5), {"north": "front", "south": W, "up": "top", "west": "side", "east": "side"}),
        element((0.5, 11, 13.5), (15.5, 16, 15.5), {"south": "front", "north": W, "up": "top", "west": "side", "east": "side"}),
        element((0.5, 11, 2.5), (2.5, 16, 13.5), {"west": "side", "east": W, "up": "top"}),
        element((13.5, 11, 2.5), (15.5, 16, 13.5), {"east": "side", "west": W, "up": "top"}),
        # the beam with the amethyst stands out of the front and of the back
        element((0.5, 6.25, 0), (15.5, 9, 0.5), {"north": "front", "up": W, "down": W}),
        element((0.5, 6.25, 15.5), (15.5, 9, 16), {"south": "front", "up": W, "down": W}),
        # the threads and the cloth
        element((2.5, 14.5, 2.5), (13.5, 14.5, 13.5), {"up": "weave", "down": "weave"}),
    ]
    for x0, x1 in ((0.5, 4), (12, 15.5)):       # the walls and the ceiling of each niche
        elements.append(element((x0, 5, 0.5), (x1, 11, 4.5), {"north": "front", "south": W, "west": "side", "east": "side"}))
        elements.append(element((x0, 5, 11.5), (x1, 11, 15.5), {"south": "front", "north": W, "west": "side", "east": "side"}))
        elements.append(element((x0, 10.5, 4.5), (x1, 11, 11.5), {"down": W, "up": W, "west": "side", "east": "side"}))
    for x in (0, 13):
        for z in (0, 13):
            elements.append(element((x, 0, z), (x + 3, 16, z + 3), {"north": "front" if z == 0 else W, "south": "front" if z == 13 else W,
                                                                     "west": "side" if x == 0 else W, "east": "side" if x == 13 else W, "up": "top", "down": W}))
    textures = dict(top="shadow_loom_top", front="shadow_loom_front", side="shadow_loom_side", wood="shadow_wood", weave="shadow_loom_weave")
    write_model("shadow_loom", textures, elements)
    write_model("shadow_loom_lit", dict(textures, weave="shadow_loom_weave_lit"), elements)
    # The shuttle, in the middle of a block of its own: ShadowMachineRenderer slides it along the edge of the cloth.
    shuttle = [solid((5.5, 7.6, 7.3), (10.5, 8.4, 8.7), "shuttle"), solid((6.5, 7.5, 7.5), (9.5, 8.5, 8.5), "thread")]
    # A spool of thread lying along z through the middle of a block of its own: the renderer puts one in each niche and turns it.
    spool = (eight((6.75, 6.75, 5.5), (9.25, 9.25, 10.5), "thread", "z") + eight((6.2, 6.2, 4.8), (9.8, 9.8, 5.5), "shuttle", "z")
             + eight((6.2, 6.2, 10.5), (9.8, 9.8, 11.2), "shuttle", "z") + [solid((7.6, 7.6, 4.5), (8.4, 8.4, 11.5), "steel")])
    part_textures = dict(shuttle="shuttle_wood", thread="shuttle_thread", steel="shadow_steel", side="shuttle_wood")
    write_model("loom_shuttle", part_textures, shuttle)
    write_model("loom_spool", part_textures, spool)
    for name in ("loom_shuttle", "loom_spool"):
        write_json(f"{OUT}/blockstates/{name}.json", {"variants": {"": {"model": "tinyrelics:block/" + name}}})
    in_hand = elements + moved(shuttle, -2, 7.3, 0.5) + moved(spool, -6, -0.25, 0) + moved(spool, 6, -0.25, 0)
    write_model("shadow_loom_inventory", dict(textures, **dict(part_textures, side="shadow_loom_side")), in_hand)
    variants = {}
    for facing, turn in FACING.items():
        for lit in (False, True):
            variants[f"facing={facing},lit={str(lit).lower()}"] = dict(model="tinyrelics:block/shadow_loom" + ("_lit" if lit else ""), **({"y": turn} if turn else {}))
    write_json(f"{OUT}/blockstates/shadow_loom.json", {"variants": variants})
    write_json(f"{OUT}/models/item/shadow_loom.json", {"parent": "tinyrelics:block/shadow_loom_inventory"})

def spooler_block():
    top, front, side = m.tiles("spooler_block", ((41, 54, 657, 675), (753, 54, 1370, 675), (1463, 54, 2062, 675)))
    save(top, f"{OUT}/textures/block/phantom_spooler_top.png")
    save(front, f"{OUT}/textures/block/phantom_spooler_front.png")
    # the crank wheel is a real part that turns, so the sides themselves are plain boards
    save(boards(side, (12, 12, 52, 52), (14, 4, 50, 10)), f"{OUT}/textures/block/phantom_spooler_side.png")
    # the reel seen from above, full of thread: the drawing's own round reel, on a flat disc that turns with it
    a = m.load("spooler_gui")
    save(fit(cut(a, 1073, 223, 1330, 484), 64, 64, keep_shape=False), f"{OUT}/textures/block/spooler_reel_top.png")
    save(wound((168, 194, 208), 21), f"{OUT}/textures/block/spooler_thread.png")
    save(plain((150, 70, 226), 10, 31), f"{OUT}/textures/block/spooler_grip.png")
    W = "wood"
    elements = [
        element((0, 0, 0), (16, 3, 16), {"north": "front", "south": "front", "west": "side", "east": "side", "up": W, "down": W}),
        # the top is a frame round an opening: the reel stands in it and shows from above
        element((0, 13, 0), (16, 16, 3), {"north": "front", "south": W, "west": "side", "east": "side", "up": "top", "down": W}),
        element((0, 13, 13), (16, 16, 16), {"south": "front", "north": W, "west": "side", "east": "side", "up": "top", "down": W}),
        element((0, 13, 3), (3, 16, 13), {"west": "side", "east": W, "up": "top", "down": W}),
        element((13, 13, 3), (16, 16, 13), {"east": "side", "west": W, "up": "top", "down": W}),
        # boarded sides; the front and the back stay open, the reel shows through
        element((0.5, 3, 3), (1.5, 13, 13), {"west": "side", "east": "side"}),
        element((14.5, 3, 3), (15.5, 13, 13), {"west": "side", "east": "side"}),
    ]
    for x in (0, 13):
        for z in (0, 13):
            elements.append(element((x, 3, z), (x + 3, 13, z + 3), {"north": "front" if z == 0 else W, "south": "front" if z == 13 else W,
                                                                     "west": "side" if x == 0 else W, "east": "side" if x == 13 else W}))
    textures = dict(top="phantom_spooler_top", front="phantom_spooler_front", side="phantom_spooler_side", wood="shadow_wood")
    write_model("phantom_spooler", textures, elements)
    # The reel stands upright in the middle of a block of its own; ShadowMachineRenderer turns it about its axle.
    reel = (eight((4.5, 3.2, 4.5), (11.5, 4, 11.5), "steel", "y") + eight((5.75, 4, 5.75), (10.25, 13.4, 10.25), "thread", "y")
            + eight((4.5, 13.4, 4.5), (11.5, 14.2, 11.5), "steel", "y") + [solid((7.5, 14.2, 7.5), (8.5, 15, 8.5), "steel")])
    reel.append(element((3.5, 14.3, 3.5), (12.5, 14.3, 12.5), {"up": ("disc", [0, 0, 16, 16])}))
    # The crank on the side, turning about x through the middle of its block: a hub, spokes, an eight-sided rim and a grip.
    X0, X1 = 16, 17
    crank = [solid((14.5, 7.5, 7.5), (16.2, 8.5, 8.5), "steel"), solid((X0, 7, 7), (X1 + 0.3, 9, 9), "steel"),
             solid((16.2, 7.6, 4), (16.8, 8.4, 12), "steel"), solid((16.2, 4, 7.6), (16.8, 12, 8.4), "steel"),
             solid((X1, 7.4, 11.4), (X1 + 2.4, 8.6, 12.6), "grip")]
    for frm, to in (((X0, 11.5, 6.2), (X1, 12.5, 9.8)), ((X0, 3.5, 6.2), (X1, 4.5, 9.8)), ((X0, 6.2, 3.5), (X1, 9.8, 4.5)), ((X0, 6.2, 11.5), (X1, 9.8, 12.5))):
        crank += eight(frm, to, "steel", "x")
    part_textures = dict(thread="spooler_thread", steel="shadow_steel", disc="spooler_reel_top", grip="spooler_grip", side="shadow_steel")
    write_model("spooler_reel", part_textures, reel)
    write_model("spooler_crank", part_textures, crank)
    for name in ("spooler_reel", "spooler_crank"):
        write_json(f"{OUT}/blockstates/{name}.json", {"variants": {"": {"model": "tinyrelics:block/" + name}}})
    write_model("phantom_spooler_inventory", dict(textures, **dict(part_textures, side="phantom_spooler_side")), elements + reel + crank)
    variants = {f"facing={facing}": dict(model="tinyrelics:block/phantom_spooler", **({"y": turn} if turn else {})) for facing, turn in FACING.items()}
    write_json(f"{OUT}/blockstates/phantom_spooler.json", {"variants": variants})
    write_json(f"{OUT}/models/item/phantom_spooler.json", {"parent": "tinyrelics:block/phantom_spooler_inventory"})

def icons():
    a = m.load("shadow_icons")
    for name, box in (("ender_skin", (39, 117, 478, 636)), ("chaos_cloth", (533, 130, 1019, 610)), ("phantom_wing", (1084, 117, 1537, 636)), ("phantom_thread", (1607, 130, 2047, 623))):
        icon = np.zeros((64, 64, 4), np.uint8)
        paste(icon, fit(cut(a, *box), 60, 60), 2, 2)
        save(icon, f"{OUT}/textures/item/{name}.png")
        write_json(f"{OUT}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "tinyrelics:item/" + name}})

print(json.dumps(dict(loom=loom_window(), spooler=spooler_window())))
loom_block(); spooler_block(); icons()
