"""Builds the Blood Blade: a 128x128 item texture with a strip of frames, its .mcmeta
(mostly still, now and then a shimmer along the blood channel, now and then the gems
light up red) and the item model that makes the sword long in the hand.

    python3 blade.py <concept on green> <out folder>"""
import sys, json
import numpy as np
from PIL import Image
from scipy import ndimage as ndi

src, out = sys.argv[1], sys.argv[2]
N = 128
a = np.array(Image.open(src).convert("RGB")).astype(int)
solid = ~((a[..., 1] - a[..., 0] > 40) & (a[..., 1] - a[..., 2] > 40))
rgba = np.dstack([a, np.where(solid, 255, 0)]).astype(np.uint8)
small = np.array(Image.fromarray(rgba, "RGBA").resize((N, N), Image.BOX)).astype(float)
small[..., 3] = np.where(small[..., 3] > 127, 255, 0)
small[small[..., 3] == 0] = 0
base = small
r, g, b = base[..., 0], base[..., 1], base[..., 2]
red = (base[..., 3] > 0) & (r > 150) & (g < 80) & (b < 80)
yy, xx = np.indices((N, N))
# along the sword: 0 at the pommel corner, 1 at the tip corner
along = (xx + (N - 1 - yy)) / (2.0 * (N - 1))
labels, count = ndi.label(ndi.binary_dilation(red, iterations=1))
sizes = ndi.sum(red, labels, range(1, count + 1))
channel = (labels == 1 + int(np.argmax(sizes))) & red            # the long red line is the biggest red thing
gems = red & ~channel & (along < 0.45)
gem_labels, gem_count = ndi.label(ndi.binary_dilation(gems, iterations=2))
centres = [c for c, n in zip(ndi.center_of_mass(gems, gem_labels, range(1, gem_count + 1)), ndi.sum(gems, gem_labels, range(1, gem_count + 1))) if n >= 6]
print("channel texels", int(channel.sum()), "gems at", [(round(x), round(y)) for y, x in centres])
lo, hi = along[channel].min(), along[channel].max()

def frame(shimmer=None, glint=None):
    fr = base.copy()
    if shimmer is not None:
        at = lo + (hi - lo) * shimmer
        wave = np.exp(-((along - at) / 0.07) ** 2)
        hot = np.clip(wave, 0, 1)[..., None]
        lit = base[..., :3] * (1 - hot * 0.75) + np.array([255.0, 205.0, 190.0]) * hot * 0.75
        fr[channel, :3] = lit[channel]
        # the steel next to the line catches a little of the light
        near = ndi.binary_dilation(channel, iterations=2) & ~channel & (base[..., 3] > 0)
        fr[near, :3] = (base[..., :3] + np.array([70.0, 8.0, 10.0]) * wave[..., None])[near]
    if glint is not None:
        # the gems light up from inside: no sparkle, just a red glow that swells and fades
        power = [0.35, 0.7, 1.0, 0.7, 0.35][glint]
        fr[gems, :3] = (base[..., :3] * (1 - 0.6 * power) + np.array([255.0, 70.0, 60.0]) * 0.6 * power)[gems]
        halo = ndi.binary_dilation(gems, iterations=1) & ~gems & (base[..., 3] > 0)
        fr[halo, :3] = np.minimum(255, base[..., :3] + np.array([120.0, 10.0, 10.0]) * power)[halo]
    return fr

frames = [frame()] + [frame(shimmer=k / 9.0) for k in range(10)] + [frame(glint=k) for k in range(5)]
Image.fromarray(np.concatenate(frames, 0).clip(0, 255).astype(np.uint8), "RGBA").save(out + "/blood_blade.png")
# still for a few seconds, a shimmer, still again, the gems glow: "now and then", never both at once
order = [{"index": 0, "time": 90}] + list(range(1, 11)) + [{"index": 0, "time": 60}] + list(range(11, 16)) + [{"index": 0, "time": 40}]
json.dump({"animation": {"frametime": 2, "frames": order}}, open(out + "/blood_blade.png.mcmeta", "w"))

# ---- the model: a bigger sword, moved so that the hand still holds the grip
def rot(rx, ry, rz):
    rx, ry, rz = np.radians([rx, ry, rz])
    X = np.array([[1, 0, 0], [0, np.cos(rx), -np.sin(rx)], [0, np.sin(rx), np.cos(rx)]])
    Y = np.array([[np.cos(ry), 0, np.sin(ry)], [0, 1, 0], [-np.sin(ry), 0, np.cos(ry)]])
    Z = np.array([[np.cos(rz), -np.sin(rz), 0], [np.sin(rz), np.cos(rz), 0], [0, 0, 1]])
    return X @ Y @ Z

GRIP = np.array([-5.3, -5.3, 0.0])       # where the hand is on the 16x16 sprite, from its middle
def view(rotation, translation, old_scale, new_scale):
    # the game does translate, rotate, scale: keep translate + rotate(scale * grip) where it was
    moved = np.array(translation) + rot(*rotation) @ ((old_scale - new_scale) * GRIP)
    right = {"rotation": rotation, "translation": [round(float(v), 2) for v in moved], "scale": [new_scale, new_scale, old_scale]}
    left = dict(right, rotation=[rotation[0], -rotation[1], -rotation[2]])
    return right, left

third_r, third_l = view([0, -90, 55], [0, 4.0, 0.5], 0.85, 1.55)
first_r, first_l = view([0, -90, 25], [1.13, 3.2, 1.13], 0.68, 1.05)
model = {"parent": "minecraft:item/handheld", "textures": {"layer0": "tinyrelics:item/blood_blade"},
         "display": {"thirdperson_righthand": third_r, "thirdperson_lefthand": third_l,
                     "firstperson_righthand": first_r, "firstperson_lefthand": first_l}}
json.dump(model, open(out + "/blood_blade.json", "w"), indent=2)
print(third_r, first_r)
prev = np.concatenate([frames[0], frames[4], frames[8], frames[13]], 1).clip(0, 255).astype(np.uint8)
bg = Image.new("RGBA", (prev.shape[1] * 2, prev.shape[0] * 2), (70, 70, 70, 255)); bg.alpha_composite(Image.fromarray(prev, "RGBA").resize(bg.size, Image.NEAREST)); bg.convert("RGB").save(out + "/blade_preview.png")
