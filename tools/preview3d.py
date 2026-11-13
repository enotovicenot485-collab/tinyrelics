"""Draws an armor made by armor.py / armor_chaos.py the way the game would: every box with
its texture, hidden surfaces removed, seen from several sides. Not exact (no game lighting,
standing pose only) but it shows shapes, overlaps and mapping mistakes before a build.

    python3 preview3d.py <texture folder with boxes.json> <out.png> [yaw,pitch ...]"""
import sys, json
import numpy as np
from PIL import Image

folder, out = sys.argv[1], sys.argv[2]
views = [tuple(map(float, v.split(","))) for v in sys.argv[3:]] or [(0, 8), (35, 12), (90, 8), (145, 12), (180, 8), (-35, 12), (-90, 8), (25, 40)]
data = json.load(open(folder + "/boxes.json"))
K, POSES = data["texels"], data["poses"]
SIZE, SCALE = (360, 560), 14.0          # picture of one view, pixels per model unit
SKIN = np.array([196.0, 150, 120, 255])

DEBUG = True
boxes = []
# the player underneath, so that gaps in the armor show as skin
for part, xyz, size in (("head", (-4, -8, -4), (8, 8, 8)), ("body", (-4, 0, -2), (8, 12, 4)), ("right_arm", (-3, -2, -2), (4, 12, 4)),
                        ("left_arm", (-1, -2, -2), (4, 12, 4)), ("right_leg", (-2, 0, -2), (4, 12, 4)), ("left_leg", (-2, 0, -2), (4, 12, 4))):
    boxes.append(dict(part=part, xyz=xyz, size=size, inflate=0, tex=None, uv=(0, 0)))
for slot, items in data["slots"].items():
    tex = np.array(Image.open(f"{folder}/{data['prefix']}{slot}.png").convert("RGBA")).astype(np.float64)
    for b in items:
        boxes.append(dict(b, tex=tex))

def render(yaw, pitch):
    ya, pi = np.radians(yaw), np.radians(pitch)
    # the camera stands in front of the character (which looks towards -z) and is turned round it
    fwd = np.array([-np.sin(ya) * np.cos(pi), np.sin(pi), np.cos(ya) * np.cos(pi)])
    right = np.array([np.cos(ya), 0, np.sin(ya)])
    up = np.cross(fwd, right)            # points down the screen, like the model's y axis
    W, H = SIZE
    px, py = np.meshgrid((np.arange(W) - W / 2 + 0.5) / SCALE, (np.arange(H) - H * 0.5 + 0.5) / SCALE)
    origin = right[None, None] * px[..., None] + up[None, None] * py[..., None] + np.array([0, 6.5, 0]) - fwd * 200
    img = np.zeros((H, W, 4)); img[...] = (58, 84, 60, 255)
    depth = np.full((H, W), np.inf)
    ident = np.zeros((H, W), int) - 1; names = []; where = np.zeros((H, W), int)
    light = -np.array([0.35, -0.8, -0.5]); light /= np.linalg.norm(light)
    for b in boxes:
        pose = POSES.get(b["part"]) or (0, 0, 0)
        lo = np.array(b["xyz"], float) + pose - b["inflate"]
        hi = np.array(b["xyz"], float) + np.array(b["size"], float) + pose + b["inflate"]
        w, h, d = b["size"]; u, v = b["uv"]
        for axis in range(3):
            for side in (0, 1):
                if abs(fwd[axis]) < 1e-9:
                    continue
                plane = hi[axis] if side else lo[axis]
                t = (plane - origin[..., axis]) / fwd[axis]
                p = origin + fwd * t[..., None]
                f = (p - lo) / (hi - lo)                      # position inside the box, 0..1 on each axis
                a1, a2 = [k for k in range(3) if k != axis]
                inside = (f[..., a1] >= 0) & (f[..., a1] < 1) & (f[..., a2] >= 0) & (f[..., a2] < 1) & (t < depth)
                if not inside.any():
                    continue
                fx, fy, fz = f[..., 0], f[..., 1], f[..., 2]
                if axis == 2 and not side:   tu, tv, ox, oy, fw, fh = fx, fy, u + d, v + d, w, h              # front
                elif axis == 2:              tu, tv, ox, oy, fw, fh = 1 - fx, fy, u + d + w + d, v + d, w, h  # back
                elif axis == 0 and not side: tu, tv, ox, oy, fw, fh = 1 - fz, fy, u, v + d, d, h              # the character's right
                elif axis == 0:              tu, tv, ox, oy, fw, fh = fz, fy, u + d + w, v + d, d, h          # the character's left
                elif not side:               tu, tv, ox, oy, fw, fh = fx, 1 - fz, u + d, v, w, d              # top
                else:                        tu, tv, ox, oy, fw, fh = fx, 1 - fz, u + d + w, v, w, d          # bottom
                normal = np.zeros(3); normal[axis] = 1 if side else -1
                shade = 0.62 + 0.38 * max(float(normal @ light), 0.0)
                if b["tex"] is None:
                    colour = np.broadcast_to(SKIN, img.shape).copy()
                else:
                    tx = np.clip(((ox + tu * fw) * K).astype(int), 0, b["tex"].shape[1] - 1)
                    ty = np.clip(((oy + tv * fh) * K).astype(int), 0, b["tex"].shape[0] - 1)
                    colour = b["tex"][ty, tx]
                hit = inside & (colour[..., 3] > 25)
                img[hit, :3] = colour[hit, :3] * shade
                depth[hit] = t[hit]
                ident[hit] = len(names)
                where[hit] = axis * 2 + side
        names.append((b['part'], b['xyz'], b['size'], b['tex'] is None))
    if DEBUG:
        for k, n in enumerate(names):
            if n[3] and (ident == k).any():
                ys, xs = np.where(ident == k)
                print('SKIN SHOWS', n[0], int((ident == k).sum()), 'px, rows', ys.min(), ys.max(), 'cols', xs.min(), xs.max(), 'faces', np.unique(where[ident == k]))
    return img

sheet = np.concatenate([render(*v) for v in views], axis=1)
Image.fromarray(sheet.clip(0, 255).astype(np.uint8), "RGBA").save(out)
print("views", views)
