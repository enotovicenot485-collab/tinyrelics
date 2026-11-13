"""Builds the Blood Blade from two drawings of the same sword on green, one clean and one
with blood running off the blade:

  * a 128x128 item texture as a strip of frames with its .mcmeta: the drops of blood grow,
    let go and fall one after another, once a loop light runs along the blood channel, and
    once a loop the gems light up red;
  * the item model. It is not the usual flat-sprite model: on a fine sprite that one shows
    slits of sky along the contour, because the game shrinks the picture on the big front
    face a little and its edge no longer meets the thin side faces. Here every run of
    texels in a row is its own small box, so the faces meet exactly.

    python3 blade.py <sword with blood> <clean sword> <out folder>"""
import sys, json
import numpy as np
from PIL import Image
from scipy import ndimage as ndi

src, clean_src, out = sys.argv[1], sys.argv[2], sys.argv[3]
N = 128
FRAMES = 40                     # three ticks each: a six-second loop

def load(path):
    a = np.array(Image.open(path).convert("RGB")).astype(int)
    solid = ~((a[..., 1] - a[..., 0] > 40) & (a[..., 1] - a[..., 2] > 40))
    # weigh the colours by coverage, or the green background leaks into the edge texels
    pre = np.dstack([a * solid[..., None], solid * 255.0])
    small = np.array(Image.fromarray(pre.astype(np.uint8), "RGBA").resize((N, N), Image.BOX)).astype(float)
    alpha = small[..., 3:4] / 255.0
    rgb = np.where(alpha > 0, small[..., :3] / np.maximum(alpha, 1e-6), 0)
    keep = small[..., 3] > 127
    return np.dstack([rgb * keep[..., None], keep * 255.0])

full = load(src)
clean = load(clean_src)
opaque = full[..., 3] > 0
# the blood that hangs below the blade: there in one drawing, not in the other
hanging = opaque & ~ndi.binary_dilation(clean[..., 3] > 0, iterations=1)
labels, count = ndi.label(hanging, structure=np.ones((3, 3)))
drips = []
for k in range(1, count + 1):
    ys, xs = np.where(labels == k)
    if len(ys) >= 4:
        drips.append((ys, xs))
print("drips:", len(drips), [len(d[0]) for d in drips])
base = full.copy()
for ys, xs in drips:
    base[ys, xs] = 0                                           # the sword with nothing hanging from it

r, g, b = base[..., 0], base[..., 1], base[..., 2]
red = (base[..., 3] > 0) & (r > 150) & (g < 80) & (b < 80)
yy, xx = np.indices((N, N))
along = (xx + (N - 1 - yy)) / (2.0 * (N - 1))                 # 0 at the pommel corner, 1 at the tip corner
clean_red = (clean[..., 3] > 0) & (clean[..., 0] > 150) & (clean[..., 1] < 80) & (clean[..., 2] < 80)
lab, cnt = ndi.label(ndi.binary_dilation(clean_red, iterations=1))
sizes = ndi.sum(clean_red, lab, range(1, cnt + 1))
channel = (lab == 1 + int(np.argmax(sizes))) & red            # the long red line is the biggest red thing
gems = clean_red & ~(lab == 1 + int(np.argmax(sizes))) & (along < 0.45) & red
lo, hi = along[channel].min(), along[channel].max()
print("channel texels", int(channel.sum()), "gem texels", int(gems.sum()))

def frame(f):
    fr = base.copy()
    if 4 <= f < 14:                                            # light runs up the blood channel
        at = lo + (hi - lo) * (f - 4) / 9.0
        wave = np.exp(-((along - at) / 0.07) ** 2)
        hot = np.clip(wave, 0, 1)[..., None]
        fr[channel, :3] = (base[..., :3] * (1 - hot * 0.75) + np.array([255.0, 205.0, 190.0]) * hot * 0.75)[channel]
        near = ndi.binary_dilation(channel, iterations=2) & ~channel & (base[..., 3] > 0)
        fr[near, :3] = (base[..., :3] + np.array([70.0, 8.0, 10.0]) * wave[..., None])[near]
    if 24 <= f < 29:                                           # the gems light up from inside
        power = [0.35, 0.7, 1.0, 0.7, 0.35][f - 24]
        fr[gems, :3] = (base[..., :3] * (1 - 0.6 * power) + np.array([255.0, 70.0, 60.0]) * 0.6 * power)[gems]
        halo = ndi.binary_dilation(gems, iterations=1) & ~gems & (base[..., 3] > 0)
        fr[halo, :3] = np.minimum(255, base[..., :3] + np.array([120.0, 10.0, 10.0]) * power)[halo]
    for n, (ys, xs) in enumerate(drips):
        # every drop has its own moment: it grows for half of the loop, hangs, then lets go
        p = (f / FRAMES + n * 0.37) % 1.0
        top, bottom = ys.min(), ys.max()
        height = bottom - top + 1
        if p < 0.5:
            shown = ys < top + max(1, int(round(height * p / 0.5)))
            fr[ys[shown], xs[shown]] = full[ys[shown], xs[shown]]
        elif p < 0.68:
            fr[ys, xs] = full[ys, xs]
        else:
            fall = (p - 0.68) / 0.32
            stem = ys < top + int(round(height * 0.35 * (1 - fall)))     # what stays on the blade shrinks back
            fr[ys[stem], xs[stem]] = full[ys[stem], xs[stem]]
            bead = ys >= bottom - 3
            drop = int(round(fall * fall * 10))
            if fall < 0.85:
                for y, x in zip(ys[bead], xs[bead]):
                    if y + drop < N:
                        fr[y + drop, x] = full[y, x]
    return fr

frames = [frame(f) for f in range(FRAMES)]
Image.fromarray(np.concatenate(frames, 0).clip(0, 255).astype(np.uint8), "RGBA").save(out + "/blood_blade.png")
json.dump({"animation": {"frametime": 3}}, open(out + "/blood_blade.png.mcmeta", "w"))

# ---- the model
def rot(rx, ry, rz):
    rx, ry, rz = np.radians([rx, ry, rz])
    X = np.array([[1, 0, 0], [0, np.cos(rx), -np.sin(rx)], [0, np.sin(rx), np.cos(rx)]])
    Y = np.array([[np.cos(ry), 0, np.sin(ry)], [0, 1, 0], [-np.sin(ry), 0, np.cos(ry)]])
    Z = np.array([[np.cos(rz), -np.sin(rz), 0], [np.sin(rz), np.cos(rz), 0], [0, 0, 1]])
    return X @ Y @ Z

DEPTH = 0.6
GRIP = np.array([-5.3, -5.3, 0.0])       # where the hand is on the 16x16 sprite, from its middle
def view(rotation, translation, old_scale, new_scale):
    # the game does translate, rotate, scale: keep translate + rotate(scale * grip) where it was
    moved = np.array(translation) + rot(*rotation) @ ((old_scale - new_scale) * GRIP)
    # Held the usual way the edge with the blood on it would point at the sky. The sword is turned
    # half a turn about its own length (the grip is on that line, so the hand stays where it was).
    from scipy.spatial.transform import Rotation
    axis = np.array([1.0, 1.0, 0.0]) / np.sqrt(2.0)
    turned = rot(*rotation) @ Rotation.from_rotvec(axis * np.pi).as_matrix()
    import warnings
    with warnings.catch_warnings():
        warnings.simplefilter("ignore")                      # "gimbal lock": any of the equal answers will do
        angles = Rotation.from_matrix(turned).as_euler("XYZ", degrees=True)
    assert np.allclose(rot(*angles), turned, atol=1e-6)
    angles = [round(float(v), 2) for v in angles]
    right = {"rotation": angles, "translation": [round(float(v), 2) for v in moved], "scale": [new_scale, new_scale, DEPTH]}
    left = dict(right, rotation=[angles[0], -angles[1], -angles[2]])
    return right, left

third_r, third_l = view([0, -90, 55], [0, 4.0, 0.5], 0.85, 1.55)
first_r, first_l = view([0, -90, 25], [1.13, 3.2, 1.13], 0.68, 1.05)

# one box for every run of texels in a row; a texel counts if it is there in any frame
ever = np.zeros((N, N), bool)
for fr in frames:
    ever |= fr[..., 3] > 0
U = 16.0 / N
elements = []
for row in range(N):
    col = 0
    while col < N:
        if not ever[row, col]:
            col += 1
            continue
        end = col
        while end + 1 < N and ever[row, end + 1]:
            end += 1
        x0, x1 = col * U, (end + 1) * U
        v0, v1 = row * U, (row + 1) * U
        y0, y1 = 16 - v1, 16 - v0
        mid_v = [round(v0 + U * 0.3, 4), round(v0 + U * 0.7, 4)]
        faces = {
            "south": {"uv": [x0, v0, x1, v1], "texture": "#layer0"},
            "north": {"uv": [x1, v0, x0, v1], "texture": "#layer0"},
            # the thin sides take the colour of the texels they belong to, sampled inside them
            "up": {"uv": [x0, mid_v[0], x1, mid_v[1]], "texture": "#layer0"},
            "down": {"uv": [x0, mid_v[0], x1, mid_v[1]], "texture": "#layer0"},
            "west": {"uv": [round(x0 + U * 0.3, 4), v0, round(x0 + U * 0.7, 4), v1], "texture": "#layer0"},
            "east": {"uv": [round(x1 - U * 0.7, 4), v0, round(x1 - U * 0.3, 4), v1], "texture": "#layer0"},
        }
        # sides that lie against a neighbouring box are never seen: leave them out
        if row > 0 and ever[row - 1, col:end + 1].all():
            del faces["up"]
        if row < N - 1 and ever[row + 1, col:end + 1].all():
            del faces["down"]
        elements.append({"from": [x0, y0, 7.5], "to": [x1, y1, 8.5], "faces": faces})
        col = end + 1
model = {"gui_light": "front", "textures": {"layer0": "tinyrelics:item/blood_blade", "particle": "tinyrelics:item/blood_blade"},
         "display": {"thirdperson_righthand": third_r, "thirdperson_lefthand": third_l,
                     "firstperson_righthand": first_r, "firstperson_lefthand": first_l,
                     "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
                     "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
                     "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]}},
         "elements": elements}
json.dump(model, open(out + "/blood_blade.json", "w"), separators=(",", ":"))
print("boxes in the model:", len(elements), third_r, first_r)
picks = [0, 8, 14, 20, 26, 32, 37]
prev = np.concatenate([frames[k] for k in picks], 1).clip(0, 255).astype(np.uint8)
bg = Image.new("RGBA", (prev.shape[1] * 2, prev.shape[0] * 2), (70, 70, 70, 255)); bg.alpha_composite(Image.fromarray(prev, "RGBA").resize(bg.size, Image.NEAREST)); bg.convert("RGB").save(out + "/blade_preview.png")
