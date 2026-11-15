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
    save(side, f"{OUT}/textures/block/shadow_loom_side.png")
    save(plain((44, 37, 34), 5, 11), f"{OUT}/textures/block/shadow_wood.png")
    save(plain((118, 84, 52), 8, 12), f"{OUT}/textures/block/shuttle_wood.png")
    save(plain((150, 70, 226), 14, 13), f"{OUT}/textures/block/shuttle_thread.png")
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
        element((0.5, 0, 0.5), (15.5, 11, 15.5), {"north": "front", "south": "front", "west": "side", "east": "side", "down": W, "up": W}),
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
    for x in (0, 13):
        for z in (0, 13):
            elements.append(element((x, 0, z), (x + 3, 16, z + 3), {"north": "front" if z == 0 else W, "south": "front" if z == 13 else W,
                                                                     "west": "side" if x == 0 else W, "east": "side" if x == 13 else W, "up": "top", "down": W}))
    textures = dict(top="shadow_loom_top", front="shadow_loom_front", side="shadow_loom_side", wood="shadow_wood", weave="shadow_loom_weave")
    write_model("shadow_loom", textures, elements)
    write_model("shadow_loom_lit", dict(textures, weave="shadow_loom_weave_lit"), elements)
    # The shuttle, in the middle of a block of its own: ShadowMachineRenderer slides it along the edge of the cloth.
    shuttle = [element((5.5, 7.6, 7.3), (10.5, 8.4, 8.7), all_faces("shuttle")), element((6.5, 7.5, 7.5), (9.5, 8.5, 8.5), all_faces("thread"))]
    shuttle_textures = dict(shuttle="shuttle_wood", thread="shuttle_thread", side="shuttle_wood")
    write_model("loom_shuttle", shuttle_textures, shuttle)
    write_json(f"{OUT}/blockstates/loom_shuttle.json", {"variants": {"": {"model": "tinyrelics:block/loom_shuttle"}}})
    write_model("shadow_loom_inventory", dict(textures, **dict(shuttle_textures, side="shadow_loom_side")), elements + moved(shuttle, -2, 7.3, 0.5))
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
    a = m.load("spooler_block")
    wheel = np.array(Image.fromarray(cut(a, 1590, 185, 1945, 540), "RGBA").resize((64, 64), Image.BOX))
    yy, xx = np.indices((64, 64))
    steel = (wheel[..., :3].astype(int).mean(-1) > 92) & ((xx - 31.5) ** 2 + (yy - 31.5) ** 2 <= 31.5 ** 2)
    crank = np.zeros_like(wheel); crank[steel] = wheel[steel]; crank[steel, 3] = 255
    save(crank, f"{OUT}/textures/block/spooler_crank.png")
    save(plain((168, 194, 208), 12, 21), f"{OUT}/textures/block/spooler_thread.png")
    save(plain((104, 102, 112), 7, 22), f"{OUT}/textures/block/shadow_steel.png")
    W = "wood"
    elements = [
        element((0, 0, 0), (16, 3, 16), {"north": "front", "south": "front", "west": "side", "east": "side", "up": W, "down": W}),
        element((0, 13, 0), (16, 16, 16), {"north": "front", "south": "front", "west": "side", "east": "side", "up": "top", "down": W}),
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
    # The reel with its crank, lying along x through the middle of a block of its own: ShadowMachineRenderer turns it.
    def eight(frm, to, texture):
        return [element(frm, to, all_faces(texture)), element(frm, to, all_faces(texture), {"origin": [8, 8, 8], "axis": "x", "angle": 45})]
    reel = eight((4.5, 5.5, 5.5), (11.5, 10.5, 10.5), "thread") + eight((3.5, 4.5, 4.5), (4.5, 11.5, 11.5), "steel") + eight((11.5, 4.5, 4.5), (12.5, 11.5, 11.5), "steel")
    reel.append(element((1.5, 7.5, 7.5), (16, 8.5, 8.5), all_faces("steel")))
    reel.append(element((16, 3, 3), (16.4, 13, 13), {"east": ("crank", [0, 0, 16, 16]), "west": ("crank", [0, 0, 16, 16])}))
    reel.append(element((16.4, 7.4, 11.3), (18.6, 8.6, 12.5), all_faces("grip")))
    reel_textures = dict(thread="spooler_thread", steel="shadow_steel", crank="spooler_crank", grip="shuttle_wood", side="shadow_steel")
    write_model("spooler_reel", reel_textures, reel)
    write_json(f"{OUT}/blockstates/spooler_reel.json", {"variants": {"": {"model": "tinyrelics:block/spooler_reel"}}})
    write_model("phantom_spooler_inventory", dict(textures, **dict(reel_textures, side="phantom_spooler_side")), elements + reel)
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
