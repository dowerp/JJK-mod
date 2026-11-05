#!/usr/bin/env python3
"""Генератор моделей и текстур мода (Mahoraga, сферы Годжо, экипировка персонажей).
Запуск из корня проекта: python3 tools/modelgen.py
Требуется Pillow. Результат записывается в src/main/resources/assets/sukuna."""
import json, math, os, random
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'sukuna')
TEX = os.path.join(ROOT, 'textures', 'entity')
GEO = os.path.join(ROOT, 'geckolib', 'models', 'entity')
ANI = os.path.join(ROOT, 'geckolib', 'animations', 'entity')
for d in (TEX, os.path.join(TEX, 'gear'), GEO, ANI):
    os.makedirs(d, exist_ok=True)


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (255,)


def hexc(h):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


class Atlas:
    def __init__(self, w, h, reserved=()):
        self.w, self.h = w, h
        self.img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
        self.occ = [[False] * w for _ in range(h)]
        for (x, y, rw, rh) in reserved:
            self.mark(x, y, rw, rh)

    def mark(self, x, y, rw, rh):
        for yy in range(y, min(self.h, y + rh)):
            for xx in range(x, min(self.w, x + rw)):
                self.occ[yy][xx] = True

    def free(self, x, y, rw, rh):
        if x + rw > self.w or y + rh > self.h:
            return False
        for yy in range(y, y + rh):
            row = self.occ[yy]
            for xx in range(x, x + rw):
                if row[xx]:
                    return False
        return True

    def alloc(self, bw, bh, bd):
        rw = int(math.ceil(2 * (bd + bw)))
        rh = int(math.ceil(bd + bh))
        for y in range(0, self.h - rh + 1):
            for x in range(0, self.w - rw + 1):
                if self.free(x, y, rw, rh):
                    self.mark(x, y, rw, rh)
                    return x, y
        raise RuntimeError('atlas full')

    def paint(self, u, v, bw, bh, bd, color_fn):
        W, H, D = int(math.ceil(bw)), int(math.ceil(bh)), int(math.ceil(bd))
        faces = {
            'top': (u + D, v, W, D), 'bottom': (u + D + W, v, W, D),
            'right': (u, v + D, D, H), 'front': (u + D, v + D, W, H),
            'left': (u + D + W, v + D, D, H), 'back': (u + 2 * D + W, v + D, W, H),
        }
        for name, (fx, fy, fw, fh) in faces.items():
            for yy in range(fh):
                for xx in range(fw):
                    px, py = fx + xx, fy + yy
                    if 0 <= px < self.w and 0 <= py < self.h:
                        self.img.putpixel((px, py), color_fn(name, xx, yy, fw, fh))

    def save(self, path):
        self.img.save(path)


FACE_LIGHT = {'top': 1.12, 'front': 1.0, 'right': 0.92, 'left': 0.88, 'back': 0.84, 'bottom': 0.7}


def mat(base, noise=0.05, edge=0.82, seed=1, stripes=None):
    rnd = random.Random(seed)
    cache = {}

    def fn(face, x, y, w, h):
        key = (face, x, y)
        if key not in cache:
            k = FACE_LIGHT[face] * (1.0 + (rnd.random() - 0.5) * 2 * noise)
            if x == 0 or y == 0 or x == w - 1 or y == h - 1:
                k *= edge
            c = shade(base, k)
            if stripes and (y % stripes[0] == 0):
                c = shade(c, stripes[1])
            cache[key] = c
        return cache[key]
    return fn


def face_mat(base, features, front_only=False):
    inner = mat(base, 0.04, 0.8)

    def fn(face, x, y, w, h):
        if face == 'front' or not front_only:
            c = features(x, y, w, h)
            if c:
                return c
        return inner(face, x, y, w, h)
    return fn


class Geo:
    def __init__(self, ident, tw, th, reserved=()):
        self.ident, self.tw, self.th = ident, tw, th
        self.atlas = Atlas(tw, th, reserved)
        self.bones = {}
        self.order = []
        self.cache = {}

    def bone(self, name, pivot, parent=None, rotation=None):
        b = {'name': name, 'pivot': list(pivot), 'cubes': []}
        if parent:
            b['parent'] = parent
        if rotation:
            b['rotation'] = list(rotation)
        self.bones[name] = b
        self.order.append(name)
        return b

    def cube(self, bone, origin, size, color, rotation=None, pivot=None, inflate=None):
        key = (tuple(round(s_, 3) for s_ in size), id(color))
        if key in self.cache:
            u, v = self.cache[key]
        else:
            u, v = self.atlas.alloc(*size)
            self.atlas.paint(u, v, size[0], size[1], size[2], color)
            self.cache[key] = (u, v)
        c = {'origin': list(origin), 'size': list(size), 'uv': [u, v]}
        if rotation:
            c['rotation'] = list(rotation)
            c['pivot'] = list(pivot if pivot else [origin[0] + size[0] / 2, origin[1] + size[1] / 2, origin[2] + size[2] / 2])
        if inflate:
            c['inflate'] = inflate
        self.bones[bone]['cubes'].append(c)

    def write(self, name, extra_bones=()):
        bones = [self.bones[n] for n in self.order] + list(extra_bones)
        data = {'format_version': '1.12.0', 'minecraft:geometry': [{
            'description': {'identifier': 'geometry.' + self.ident, 'texture_width': self.tw, 'texture_height': self.th,
                            'visible_bounds_width': 8, 'visible_bounds_height': 8, 'visible_bounds_offset': [0, 2, 0]},
            'bones': bones}]}
        with open(os.path.join(GEO, name + '.geo.json'), 'w') as f:
            json.dump(data, f, indent=1)
        self.atlas.save(os.path.join(TEX, name + '.png'))


def sphere_cluster(g, bone, c, r, color):
    """Сфера из пересекающихся повёрнутых кубов."""
    s = r * 1.45
    rots = [(0, 0, 0), (45, 0, 0), (0, 45, 0), (0, 0, 45), (45, 45, 0), (0, 45, 45), (45, 0, 45), (45, 45, 45)]
    for rot in rots:
        g.cube(bone, [c[0] - s / 2, c[1] - s / 2, c[2] - s / 2], [s, s, s], color, rotation=rot, pivot=list(c))


def ring(g, bone, c, radius, thick, color, n=18, axis='y'):
    for i in range(n):
        a = i * 360.0 / n
        sx = 2 * math.pi * radius / n * 1.25
        if axis == 'y':
            g.cube(bone, [c[0] + radius - sx / 2 * 0.0 - thick / 2, c[1] - thick / 2, c[2] - sx / 2], [thick, thick, sx], color,
                   rotation=[0, a, 0], pivot=list(c))


def anim_file(name, bones_spec, length=4.0):
    bones = {}
    for b, (axis, speed) in bones_spec.items():
        end = [0, 0, 0]
        end[axis] = 360 * speed
        bones[b] = {'rotation': {'0': [0, 0, 0], str(length): end}}
    d = {'format_version': '1.8.0', 'animations': {'animation.orb.spin': {'loop': True, 'animation_length': length, 'bones': bones}}}
    with open(os.path.join(ANI, name + '.animation.json'), 'w') as f:
        json.dump(d, f, indent=1)


# ------------------------------------------------------------------ сферы Годжо
def orb_blue():
    g = Geo('orb_blue', 128, 128)
    c = (0, 11.2, 0)
    g.bone('core', c)
    swirl = lambda x, y, w, h: hexc('#e8fbff') if ((x * 3 + y * 5) % 11 < 2) else None
    sphere_cluster(g, 'core', c, 8.0, face_mat(hexc('#1c6bff'), swirl))
    g.bone('inner_glow', c, 'core')
    g.cube('inner_glow', [-1.6, 9.6, -1.6], [3.2, 3.2, 3.2], mat(hexc('#ffffff'), 0.0, 1.0), rotation=[45, 45, 0], pivot=list(c))
    g.bone('ring_a', c)
    ring(g, 'ring_a', c, 11.0, 1.4, mat(hexc('#9fd8ff'), 0.05), 20)
    g.bone('ring_b', c, rotation=[60, 0, 0])
    ring(g, 'ring_b', c, 10.0, 1.2, mat(hexc('#0a1d6e'), 0.05), 20)
    g.bone('ring_c', c, rotation=[0, 0, -60])
    ring(g, 'ring_c', c, 12.0, 1.0, mat(hexc('#ffffff'), 0.03), 22)
    g.write('orb_blue')
    anim_file('orb_blue', {'core': (1, 1), 'ring_a': (1, 2), 'ring_b': (1, -2), 'ring_c': (1, 3)})


def orb_red():
    g = Geo('orb_red', 128, 128)
    c = (0, 11.2, 0)
    g.bone('core', c)
    streak = lambda x, y, w, h: hexc('#ffb0a0') if ((x + y * 2) % 13 < 1) else None
    sphere_cluster(g, 'core', c, 8.0, face_mat(hexc('#e5141e'), streak))
    g.bone('spikes', c)
    for i, rot in enumerate([(0, 0, 0), (0, 60, 0), (0, 120, 0), (90, 0, 0), (0, 0, 90), (45, 0, 45)]):
        g.cube('spikes', [-0.6, 11.2 - 14, -0.6], [1.2, 28, 1.2], mat(hexc('#ff4a3a'), 0.05), rotation=list(rot), pivot=list(c))
    g.bone('ring_a', c)
    ring(g, 'ring_a', c, 11.5, 1.6, mat(hexc('#14040a'), 0.05), 22)
    g.bone('ring_b', c, rotation=[0, 0, 70])
    ring(g, 'ring_b', c, 10.5, 1.2, mat(hexc('#ff7a6a'), 0.05), 22)
    g.write('orb_red')
    anim_file('orb_red', {'core': (1, -1), 'spikes': (1, 1), 'ring_a': (1, 2), 'ring_b': (1, -3)})


def orb_purple():
    g = Geo('orb_purple', 256, 256)
    c = (0, 32.0, 0)
    g.bone('core', c)
    streak = lambda x, y, w, h: hexc('#f0d8ff') if ((x * 2 + y * 3) % 17 < 2) else None
    sphere_cluster(g, 'core', c, 20.0, face_mat(hexc('#8a2cff'), streak))
    g.bone('inner_glow', c, 'core')
    g.cube('inner_glow', [-5, 27, -5], [10, 10, 10], mat(hexc('#ffffff'), 0.0, 1.0), rotation=[45, 45, 0], pivot=list(c))
    g.bone('ring_a', c)
    ring(g, 'ring_a', c, 28.0, 3.0, mat(hexc('#12002e'), 0.05), 28)
    g.bone('ring_b', c, rotation=[60, 0, 0])
    ring(g, 'ring_b', c, 27.0, 2.4, mat(hexc('#d6a8ff'), 0.05), 28)
    g.bone('ring_c', c, rotation=[0, 0, -60])
    ring(g, 'ring_c', c, 30.0, 2.0, mat(hexc('#ffffff'), 0.03), 30)
    g.bone('bolts', c)
    for rot in [(0, 0, 0), (0, 70, 0), (0, 140, 0), (80, 0, 0), (0, 0, 80), (40, 30, 40), (-40, 60, 20), (20, 100, -50)]:
        g.cube('bolts', [-1, 32 - 36, -1], [2, 72, 2], mat(hexc('#c07bff'), 0.06), rotation=list(rot), pivot=list(c))
    g.write('orb_purple')
    anim_file('orb_purple', {'core': (1, 1), 'ring_a': (1, 1), 'ring_b': (1, -2), 'ring_c': (1, 2), 'bolts': (1, -1)}, 5.0)


def orb_purple_small():
    g = Geo('orb_purple_small', 128, 128)
    c = (0, 11.2, 0)
    g.bone('core', c)
    streak = lambda x, y, w, h: hexc('#f0d8ff') if ((x * 2 + y * 3) % 17 < 2) else None
    sphere_cluster(g, 'core', c, 5.2, face_mat(hexc('#8a2cff'), streak))
    g.bone('inner_glow', c, 'core')
    g.cube('inner_glow', [-1.4, 9.8, -1.4], [2.8, 2.8, 2.8], mat(hexc('#ffffff'), 0.0, 1.0), rotation=[45, 45, 0], pivot=list(c))
    g.bone('ring_a', c)
    ring(g, 'ring_a', c, 7.6, 1.1, mat(hexc('#12002e'), 0.05), 18)
    g.bone('ring_b', c, rotation=[60, 0, 0])
    ring(g, 'ring_b', c, 7.2, 0.9, mat(hexc('#d6a8ff'), 0.05), 20)
    g.write('orb_purple_small')
    anim_file('orb_purple_small', {'core': (1, 1), 'ring_a': (1, 2), 'ring_b': (1, -2)})


def orb_small(name, base, core_hex, ring_a, ring_b, radius):
    g = Geo(name, 128, 128)
    c = (0, 11.2, 0)
    g.bone('core', c)
    pattern = lambda x, y, w, h: hexc('#ffffff') if ((x * 3 + y * 5) % 13 < 1) else None
    sphere_cluster(g, 'core', c, radius, face_mat(hexc(base), pattern))
    g.bone('ring_a', c)
    ring(g, 'ring_a', c, radius + 2.4, 1.0, mat(hexc(ring_a), 0.05), 16)
    g.bone('ring_b', c, rotation=[60, 0, 0])
    ring(g, 'ring_b', c, radius + 2.0, 0.9, mat(hexc(ring_b), 0.05), 16)
    g.write(name)
    anim_file(name, {'core': (1, 1), 'ring_a': (1, 2), 'ring_b': (1, -2)})


# ------------------------------------------------------------------ Махорага (Божественный генерал)
def mahoraga():
    old = json.load(open(os.path.join(GEO, 'mahoraga.geo.json')))
    wheel_bones = [b for b in old['minecraft:geometry'][0]['bones'] if b['name'].startswith('wheel')]
    old_tex = Image.open(os.path.join(TEX, 'mahoraga.png')).convert('RGBA')
    wheel_region = old_tex.crop((96, 0, 128, 32))

    g = Geo('mahoraga', 256, 256, reserved=[(96, 0, 32, 32)])
    skin = mat(hexc('#dcdcd8'), 0.03, 0.85, 3)
    skin_d = mat(hexc('#b8b8b6'), 0.03, 0.85, 4)
    cloth = mat(hexc('#e6e2d6'), 0.03, 0.88, 5, stripes=(4, 0.94))
    gold = mat(hexc('#c9a24a'), 0.05, 0.8, 6)
    metal = mat(hexc('#8c9098'), 0.04, 0.85, 7)
    dark = mat(hexc('#15131a'), 0.04, 0.85, 8)
    rope = mat(hexc('#6b5a3a'), 0.06, 0.8, 9)

    def stripes_body(x, y, w, h):
        if x in (1, w - 2) and y % 2 == 0:
            return hexc('#16141b')
        if y in (3, 8) and 2 <= x <= w - 3:
            return hexc('#2a2630')
        return None
    chest = face_mat(hexc('#dcdcd8'), stripes_body, True)

    def face(x, y, w, h):
        if w >= 8 and h >= 8:
            if y == 3 and (1 <= x <= 2 or w - 3 <= x <= w - 2):
                return hexc('#d02a2a')
            if y == 5 and (1 <= x <= 2 or w - 3 <= x <= w - 2):
                return hexc('#d02a2a')
            if y == 2 and (x == 0 or x == w - 1):
                return hexc('#15131a')
            if y >= h - 3 and 2 <= x <= w - 3 and (x % 2 == 0):
                return hexc('#15131a')
            if x in (w // 2 - 1, w // 2) and 1 <= y <= 3:
                return hexc('#15131a')
        return None
    mask = face_mat(hexc('#ecece8'), face, True)

    g.bone('head', (0, 25, 0))
    g.cube('head', [-4, 25, -4], [8, 9, 8], mask)
    g.cube('head', [-4.6, 31.8, -4.6], [9.2, 3.2, 9.2], skin_d)
    for i in range(5):
        h = 4.5 + (2 - abs(i - 2)) * 1.6
        g.cube('head', [-4.0 + i * 1.9 - 0.6, 35.0, -0.8], [1.2, h, 1.6], skin if i % 2 == 0 else metal)
    g.cube('head', [-9.5, 27, -0.8], [5.5, 1.8, 1.6], skin_d, rotation=[0, 0, 24], pivot=[-4, 28, 0])
    g.cube('head', [4, 27, -0.8], [5.5, 1.8, 1.6], skin_d, rotation=[0, 0, -24], pivot=[4, 28, 0])
    g.cube('head', [-4.3, 22.8, -4.3], [8.6, 2.4, 8.6], cloth)

    g.bone('body', (0, 24, 0))
    g.cube('body', [-6.4, 12, -3.8], [12.8, 12.5, 7.6], chest)
    for i in range(3):
        g.cube('body', [-4.6, 12.4 + i * 3.4, -4.3], [9.2, 2.8, 0.6], skin_d)
    g.cube('body', [-6.9, 20.5, -4.4], [13.8, 2.0, 8.8], rope, rotation=[0, 0, 20], pivot=[0, 21, 0])
    g.cube('body', [-6.9, 19.0, -4.4], [13.8, 2.0, 8.8], rope, rotation=[0, 0, -20], pivot=[0, 20, 0])
    g.cube('body', [-5.8, 10, -3.8], [11.6, 2.6, 7.6], dark)
    g.cube('body', [-6.2, 0.5, -4.4], [12.4, 10.0, 8.8], cloth)
    g.cube('body', [-3.4, 0.0, -5.0], [6.8, 8.0, 0.8], dark)
    for i in range(5):
        g.cube('body', [-0.9, 12.5 + i * 2.2, 3.7], [1.8, 1.4, 1.6], skin_d)

    for side, sx in (('right_arm', -1), ('left_arm', 1)):
        g.bone(side, (sx * 7.4, 22, 0))
        x0 = sx * 7.4
        g.cube(side, [x0 - 3.6 + (-0.8 if sx < 0 else 0.0), 22.0, -3.8], [7.2, 3.6, 7.6], metal)
        for k in range(3):
            g.cube(side, [x0 - 0.5 + sx * (k - 1) * 1.6, 25.6, -0.5], [1.0, 3.2, 1.0], metal)
        g.cube(side, [x0 - 2.8, 14.0, -2.8], [5.6, 9.0, 5.6], skin)
        g.cube(side, [x0 - 3.1, 6.2, -3.0], [6.2, 8.6, 6.0], skin)
        g.cube(side, [x0 - 3.4, 8.0, -3.4], [6.8, 2.6, 6.8], dark)
        g.cube(side, [x0 - 3.4, 12.0, -3.4], [6.8, 1.6, 6.8], cloth)
        g.cube(side, [x0 - 2.8, 3.6, -2.8], [5.6, 3.2, 5.6], skin_d)
        for k in range(3):
            g.cube(side, [x0 - 2.2 + k * 1.6, 0.8, -2.2], [1.0, 3.0, 1.0], dark)
    g.cube('right_arm', [-8.4, 3.6, -26.0], [1.4, 3.0, 22.0], metal)
    g.cube('right_arm', [-8.2, 3.8, -28.0], [1.0, 2.6, 2.6], metal, rotation=[45, 0, 0])
    g.cube('right_arm', [-9.0, 3.2, -5.0], [2.6, 4.2, 1.0], gold)
    g.cube('right_arm', [-8.6, 3.8, -6.4], [1.8, 2.8, 1.4], gold)

    for side, sx in (('right_leg', -1), ('left_leg', 1)):
        g.bone(side, (sx * 3.4, 12, 0))
        x0 = sx * 3.4 - 3.1
        g.cube(side, [x0, 5.5, -3.1], [6.2, 7.0, 6.2], skin)
        g.cube(side, [x0 + 0.2, 0.8, -2.8], [5.8, 5.2, 5.6], skin)
        g.cube(side, [x0 - 0.3, 6.0, -3.5], [6.8, 2.2, 7.0], cloth)
        g.cube(side, [x0 - 0.2, 1.8, -3.2], [6.6, 1.6, 6.4], rope)
        g.cube(side, [x0 - 0.3, 0.0, -4.8], [6.8, 1.6, 7.8], dark)

    g.write('mahoraga', extra_bones=wheel_bones)
    # восстановить область колеса в текстуре
    tex_path = os.path.join(TEX, 'mahoraga.png')
    img = Image.open(tex_path).convert('RGBA')
    img.paste(wheel_region, (96, 0))
    img.save(tex_path)


# ------------------------------------------------------------------ экипировка (текстуры под ModelPart-раскладку)
def gear():
    def box_set(w, h, boxes):
        a = Atlas(w, h)
        for (u, v, bw, bh, bd, fn) in boxes:
            a.paint(u, v, bw, bh, bd, fn)
        return a

    red = mat(hexc('#c8141c'), 0.05, 0.78, 11)
    red_d = mat(hexc('#8e0d14'), 0.05, 0.8, 12)
    a = box_set(64, 64, [
        (0, 0, 10.0, 2.6, 6.0, red), (0, 12, 8.4, 6.5, 2.6, red_d), (0, 24, 5.2, 2.4, 1.8, red),
        (0, 32, 0.8, 4.0, 0.8, mat(hexc('#e8e0d0'), 0.03, 0.9, 21)), (8, 32, 0.8, 4.0, 0.8, mat(hexc('#e8e0d0'), 0.03, 0.9, 22))])
    a.save(os.path.join(TEX, 'gear', 'hood.png'))

    blk = mat(hexc('#0c0c12'), 0.03, 0.85, 13)
    a = box_set(64, 32, [(0, 0, 9.2, 2.2, 9.2, blk), (0, 22, 3.0, 1.6, 0.8, blk), (10, 22, 1.2, 3.6, 0.6, blk), (16, 22, 1.2, 3.6, 0.6, blk)])
    a.save(os.path.join(TEX, 'gear', 'blindfold.png'))

    cl = mat(hexc('#5a4f45'), 0.05, 0.82, 14, stripes=(5, 0.93))
    cl_d = mat(hexc('#3a322c'), 0.05, 0.82, 15)
    cl_l = mat(hexc('#7a6c5e'), 0.05, 0.82, 16)
    a = box_set(64, 64, [
        (0, 0, 13.2, 3.8, 6.8, cl), (0, 12, 10.4, 2.4, 6.4, cl_d), (0, 22, 2.0, 6.0, 7.0, cl_l),
        (18, 22, 2.0, 6.0, 7.0, cl_l), (0, 36, 11.6, 21.0, 1.3, cl), (30, 36, 4.6, 17.0, 1.2, cl), (44, 36, 4.6, 17.0, 1.2, cl)])
    a.save(os.path.join(TEX, 'gear', 'choso_cloak.png'))

    shaft = mat(hexc('#17141c'), 0.03, 0.85, 17, stripes=(6, 1.5))
    gold = mat(hexc('#e0b040'), 0.04, 0.8, 18)
    blade = mat(hexc('#dff6ff'), 0.03, 0.85, 19)
    prong = mat(hexc('#9fe0ff'), 0.03, 0.85, 20)
    a = box_set(64, 64, [
        (0, 0, 1.6, 30.0, 1.6, shaft), (8, 0, 2.8, 2.4, 2.8, gold), (0, 34, 4.8, 1.6, 4.8, gold),
        (20, 0, 3.6, 11.0, 1.0, blade), (20, 12, 1.2, 7.0, 1.0, prong), (20, 20, 1.2, 7.0, 1.0, prong),
        (32, 0, 1.0, 2.6, 0.8, gold), (32, 6, 1.0, 2.6, 0.8, gold), (0, 44, 3.2, 1.0, 3.2, gold), (0, 50, 3.2, 1.0, 3.2, gold)])
    a.save(os.path.join(TEX, 'gear', 'kamutoke.png'))

    bone = mat(hexc('#e8dccb'), 0.04, 0.85, 40)
    f_a = mat(hexc('#b0101f'), 0.06, 0.8, 41)
    f_b = mat(hexc('#7e0a16'), 0.06, 0.8, 42)
    boxes = [(0, 0, 1.6, 14.0, 1.6, bone), (16, 0, 1.4, 12.0, 1.4, bone)]
    for i, L in enumerate((13.0, 16.0, 19.0, 16.0, 12.0)):
        boxes.append((i * 8, 20, 1.0, L, 0.6, f_a if i % 2 == 0 else f_b))
    box_set(64, 64, boxes).save(os.path.join(TEX, 'gear', 'blood_wings.png'))


# ------------------------------------------------------------------ блоки территорий
def blocks():
    from PIL import ImageDraw, ImageFilter
    bt = os.path.join(ROOT, 'textures', 'block')
    bm = os.path.join(ROOT, 'models', 'block')
    bs = os.path.join(ROOT, 'blockstates')
    for d in (bt, bm, bs):
        os.makedirs(d, exist_ok=True)
    S = 32

    def save(name, img):
        img.save(os.path.join(bt, name + '.png'))
        with open(os.path.join(bm, name + '.json'), 'w') as f:
            json.dump({'parent': 'minecraft:block/cube_all', 'textures': {'all': 'sukuna:block/' + name}}, f)
        with open(os.path.join(bs, name + '.json'), 'w') as f:
            json.dump({'variants': {'': {'model': 'sukuna:block/' + name}}}, f)

    def glow(img, radius=1.2):
        g = img.filter(ImageFilter.GaussianBlur(radius))
        return Image.blend(img, Image.alpha_composite(img, g), 0.6)

    # бесконечная пустота: тёмный космос, звёзды и световые потоки
    for i in range(4):
        rnd = random.Random(100 + i)
        img = Image.new('RGBA', (S, S), (3, 4, 14, 255))
        px = img.load()
        for y in range(S):
            for x in range(S):
                k = 0.5 + 0.5 * math.sin((x + y * 0.6 + i * 7) * 0.45)
                px[x, y] = (int(4 + 10 * k), int(5 + 12 * k), int(18 + 40 * k), 255)
        d = ImageDraw.Draw(img)
        for _ in range(14 + i * 4):
            x, y = rnd.randrange(S), rnd.randrange(S)
            c = rnd.choice([(255, 255, 255), (170, 220, 255), (200, 170, 255)])
            d.point((x, y), fill=c + (255,))
            if rnd.random() < 0.3:
                d.point((x + 1, y), fill=c + (160,))
                d.point((x, y + 1), fill=c + (160,))
        for _ in range(2 + i):
            x0 = rnd.randrange(-8, S)
            y0 = rnd.randrange(S)
            ln = rnd.randrange(12, 30)
            c = rnd.choice([(235, 250, 255), (120, 200, 255), (190, 140, 255)])
            d.line((x0, y0, x0 + ln, y0 - ln // 3), fill=c + (255,), width=1)
        img = glow(img)
        if i == 3:
            for y in range(S):
                for x in range(S):
                    r, g, b, a = img.getpixel((x, y))
                    k = max(0.0, 1.0 - math.hypot(x - 16, y - 16) / 17.0)
                    img.putpixel((x, y), (min(255, int(r + 150 * k)), min(255, int(g + 190 * k)), min(255, int(b + 230 * k)), 255))
        save('void_wall_%d' % i, img)

    img = Image.new('RGBA', (S, S), (2, 2, 8, 255))
    rnd = random.Random(7)
    d = ImageDraw.Draw(img)
    for _ in range(9):
        x, y = rnd.randrange(S), rnd.randrange(S)
        d.point((x, y), fill=rnd.choice([(120, 190, 255, 255), (230, 245, 255, 255)]))
    for k in (0, 16):
        d.line((k, 0, k, S), fill=(8, 16, 44, 255))
        d.line((0, k, S, k), fill=(8, 16, 44, 255))
    save('void_floor', glow(img, 0.8))

    img = Image.new('RGBA', (S, S), (4, 4, 6, 255))
    px = img.load()
    for y in range(S):
        for x in range(S):
            v = 4 + int(3 * (x + y) / (2.0 * S))
            px[x, y] = (v, v, v + 2, 255)
    save('mirror_floor', img)

    for i in range(3):
        rnd = random.Random(700 + i)
        img = Image.new('RGBA', (S, S), (8, 4, 8, 255))
        d = ImageDraw.Draw(img)
        for _ in range(3 + i):
            x0, y0 = rnd.randrange(-6, S), rnd.randrange(-6, S)
            ln = rnd.randrange(16, 40)
            d.line((x0, y0, x0 + ln, y0 + ln * rnd.choice((-1, 1)) // 2), fill=(150, 10, 25, 255), width=2)
        for _ in range(2):
            x0, y0 = rnd.randrange(S), rnd.randrange(S)
            d.line((x0, y0, x0 + 14, y0 - 7), fill=(235, 220, 215, 255), width=1)
        for _ in range(5):
            d.point((rnd.randrange(S), rnd.randrange(S)), fill=(190, 20, 35, 255))
        save('sukuna_wall_%d' % i, glow(img, 0.8))

    # небо городской территории Юдзи: три полосы по высоте
    bands = [((255, 150, 80), (255, 90, 110)), ((200, 90, 150), (110, 70, 170)), ((50, 50, 130), (12, 14, 50))]
    for i, (c0, c1) in enumerate(bands):
        rnd = random.Random(300 + i)
        img = Image.new('RGBA', (S, S))
        px = img.load()
        for y in range(S):
            for x in range(S):
                k = y / (S - 1.0)
                px[x, y] = tuple(int(c0[j] + (c1[j] - c0[j]) * k) for j in range(3)) + (255,)
        d = ImageDraw.Draw(img)
        for _ in range(3):
            cx, cy = rnd.randrange(S), rnd.randrange(S)
            d.ellipse((cx - 7, cy - 2, cx + 7, cy + 2), fill=(255, 235, 225, 90) if i < 2 else (150, 160, 220, 80))
        if i == 2:
            for _ in range(10):
                d.point((rnd.randrange(S), rnd.randrange(S)), fill=(255, 255, 255, 255))
        save('city_sky_%d' % i, img.filter(ImageFilter.GaussianBlur(0.5)))


# ------------------------------------------------------------------ скин Сукуны (раскладка 64x64) и причёска
def sukuna_skin():
    SK = hexc('#e9bf9f')
    PINK = hexc('#e9889f')
    PINK_D = hexc('#c9647f')
    BLK = hexc('#14111a')
    WHT = hexc('#ece9e2')
    RED = hexc('#9c1f2c')
    EYE = hexc('#b3141f')
    skin = mat(SK, 0.025, 0.93, 31)
    a = Atlas(64, 64)

    def head(face, x, y, w, h):
        if face == 'top':
            return shade(PINK, 1.05)
        if face in ('back',):
            return shade(PINK, 0.9 + 0.1 * ((x + y) % 2))
        if face in ('left', 'right'):
            if y <= 2 or x >= w - 3:
                return shade(PINK_D if (x + y) % 3 == 0 else PINK, 0.95)
            return skin(face, x, y, w, h)
        if face == 'front':
            if y <= 1:
                return shade(PINK if (x + y) % 4 else PINK_D, 1.0)
            if y == 2 and x in (0, 7):
                return shade(PINK, 0.95)
            if y == 3 and x in (1, 2, 5, 6):
                return EYE if x in (2, 5) else WHT
            if y == 4 and x in (1, 2, 5, 6):
                return BLK
            if y == 5 and x in (1, 6):
                return EYE
            if y == 6 and x in (1, 6):
                return BLK
            if y == 7 and x in (1, 6):
                return BLK
            if y == 2 and x in (3, 4):
                return BLK
            if y == 6 and x in (3, 4):
                return shade(SK, 0.8)
            if y == 7 and 3 <= x <= 4:
                return RED
        return skin(face, x, y, w, h)
    a.paint(0, 0, 8, 8, 8, head)

    def body(face, x, y, w, h):
        if face == 'front':
            if y >= 10:
                return BLK if y == 10 else shade(WHT, 0.96)
            if y == 1 and (1 <= x <= 3 or 4 <= x <= 6):
                return BLK
            if y == 2 and x in (0, 3, 4, 7):
                return BLK
            if y in (4, 5) and x in (3, 4):
                return BLK
            if y == 6 and x in (2, 5):
                return BLK
            if 7 <= y <= 9 and 2 <= x <= 5:
                if y == 7 or y == 9:
                    return RED if x in (2, 5) else WHT
                return RED if x in (3, 4) else shade(BLK, 1.2)
            if y == 3 and x in (0, 7):
                return BLK
        if face == 'back':
            if y >= 10:
                return BLK if y == 10 else shade(WHT, 0.9)
            if y in (2, 6) and 1 <= x <= 6:
                return BLK
            if x in (3, 4) and y < 10:
                return BLK
        if face in ('left', 'right') and y >= 10:
            return shade(WHT, 0.9)
        return skin(face, x, y, w, h)
    a.paint(16, 16, 8, 12, 4, body)

    def arm(face, x, y, w, h):
        if y in (1, 2) or y in (7, 8):
            return BLK
        if face in ('front', 'back') and x == 1 and y in (4, 5, 10, 11):
            return BLK
        return skin(face, x, y, w, h)
    a.paint(40, 16, 4, 12, 4, arm)
    a.paint(32, 48, 4, 12, 4, arm)

    def leg(face, x, y, w, h):
        if y >= 9:
            return BLK if (y + x) % 2 == 0 else shade(WHT, 0.7)
        if y in (3, 4) and face in ('front', 'back'):
            return shade(WHT, 0.75) if x % 2 else BLK
        return shade(WHT, 0.93 if face != 'bottom' else 0.7)
    a.paint(0, 16, 4, 12, 4, leg)
    a.paint(16, 48, 4, 12, 4, leg)
    a.save(os.path.join(TEX, 'sukuna_skin.png'))

    hair_mat = mat(PINK, 0.06, 0.8, 32)
    hair_d = mat(PINK_D, 0.06, 0.8, 33)
    h = Atlas(64, 64)
    for (u, v, bw, bh, bd, fn) in [
        (0, 0, 8.8, 2.0, 8.8, hair_mat), (0, 12, 2, 3, 2, hair_d), (10, 12, 2, 3.6, 2, hair_mat), (20, 12, 2, 3.4, 2, hair_d),
        (0, 20, 1.6, 2.4, 2.6, hair_mat), (12, 20, 1.6, 2.4, 2.6, hair_mat), (0, 30, 6.8, 6, 1.6, hair_d),
        (24, 30, 1.4, 2.6, 0.8, hair_mat), (30, 30, 1.4, 2.6, 0.8, hair_mat), (36, 30, 1.4, 2.2, 0.8, hair_d)]:
        h.paint(u, v, bw, bh, bd, fn)
    h.save(os.path.join(TEX, 'gear', 'sukuna_hair.png'))


# ------------------------------------------------------------------ 2D-эффекты техник (спрайты)
FX_PALETTES = [
    ('#ffffff', '#cfd2e6', '#07030a'),
    ('#ffd9d0', '#e0112b', '#1a0206'),
    ('#e6f8ff', '#2a8cff', '#04123c'),
    ('#f4e6ff', '#a24cff', '#16002e'),
    ('#fffbe0', '#ffae1a', '#3a1400'),
]


def fx_textures():
    from PIL import ImageDraw, ImageFilter, ImageChops
    out = os.path.join(TEX, 'fx')
    os.makedirs(out, exist_ok=True)
    SS = 4
    N = 128 * SS

    def finish(img, name, pal):
        img = img.resize((128, 128), Image.LANCZOS)
        full = Image.new('RGBA', (256, 128), (0, 0, 0, 0))
        full.paste(img, (0, 0))
        full.paste(img.transpose(Image.FLIP_LEFT_RIGHT), (128, 0))
        full.save(os.path.join(out, '%s_%d.png' % (name, pal)))

    def lens(draw, rnd, scale, color, jag):
        pts_top, pts_bot = [], []
        xs = [4 + i * 120 / 40.0 for i in range(41)]
        for x in xs:
            u = (x - 64) / 60.0
            hw = 30 * max(0.0, 1 - abs(u) ** 1.7) ** 0.85 * scale
            n = (rnd.random() - 0.5) * jag * (1 - abs(u))
            pts_top.append((x * SS, (64 - hw - n) * SS))
            pts_bot.append((x * SS, (64 + hw + n) * SS))
        draw.polygon(pts_top + pts_bot[::-1], fill=color)

    for pal, (core, mid, edge) in enumerate(FX_PALETTES):
        core, mid, edge = hexc(core), hexc(mid), hexc(edge)
        rnd = random.Random(500 + pal)

        img = Image.new('RGBA', (N, N), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        lens(d, rnd, 1.0, edge, 5)
        lens(d, rnd, 0.72, mid, 3)
        lens(d, rnd, 0.3, core, 1)
        for off in (-40, 38):
            d.line((20 * SS, (64 + off) * SS, 100 * SS, (64 + off * 0.9) * SS), fill=mid[:3] + (160,), width=2 * SS)
        finish(img, 'streak', pal)

        def crescent(scale, color, shift):
            m = Image.new('L', (N, N), 0)
            md = ImageDraw.Draw(m)
            R = 58 * scale * SS
            md.ellipse((64 * SS - R, 64 * SS - R, 64 * SS + R, 64 * SS + R), fill=255)
            c = Image.new('L', (N, N), 0)
            cd = ImageDraw.Draw(c)
            R2 = 50 * scale * SS
            cx = (64 + shift) * SS
            cd.ellipse((cx - R2, 64 * SS - R2, cx + R2, 64 * SS + R2), fill=255)
            m = ImageChops.subtract(m, c)
            layer = Image.new('RGBA', (N, N), color)
            layer.putalpha(m)
            return layer
        img = Image.new('RGBA', (N, N), (0, 0, 0, 0))
        img = Image.alpha_composite(img, crescent(1.0, edge, 14))
        img = Image.alpha_composite(img, crescent(0.93, mid, 16))
        img = Image.alpha_composite(img, crescent(0.86, core, 18))
        finish(img, 'crescent', pal)

        img = Image.new('RGBA', (N, N), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        pts = [(4 * SS, 64 * SS)]
        for i in range(1, 10):
            pts.append(((4 + i * 12) * SS, (64 + rnd.randrange(-22, 23)) * SS))
        pts.append((124 * SS, 64 * SS))
        for width, col in ((16, edge), (9, mid), (3, core)):
            d.line(pts, fill=col, width=width * SS, joint='curve')
        for i in (3, 6):
            bx, by = pts[i]
            d.line([(bx, by), (bx + 14 * SS, by + rnd.choice((-26, 26)) * SS)], fill=mid, width=5 * SS)
        finish(img, 'bolt', pal)

        img = Image.new('RGBA', (N, N), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        for r, width, col in ((56, 12, edge), (56, 7, mid), (56, 2.5, core)):
            R = r * SS
            d.ellipse((64 * SS - R, 64 * SS - R, 64 * SS + R, 64 * SS + R), outline=col, width=int(width * SS))
        for i in range(14):
            a = i * math.pi * 2 / 14 + rnd.random() * 0.2
            r0, r1 = 50 * SS, (58 + rnd.random() * 5) * SS
            d.line((64 * SS + math.cos(a) * r0, 64 * SS + math.sin(a) * r0, 64 * SS + math.cos(a) * r1, 64 * SS + math.sin(a) * r1), fill=edge, width=3 * SS)
        finish(img, 'ring', pal)

        img = Image.new('RGBA', (N, N), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        for scale, col in ((1.0, edge), (0.74, mid), (0.4, core)):
            pts = []
            for i in range(24):
                a = i * math.pi * 2 / 24
                r = (60 if i % 2 == 0 else 22 + rnd.random() * 8) * scale
                pts.append((64 * SS + math.cos(a) * r * SS, 64 * SS + math.sin(a) * r * SS))
            d.polygon(pts, fill=col)
        finish(img, 'burst', pal)


def fx_extra():
    from PIL import ImageDraw, ImageFilter
    out = os.path.join(TEX, 'fx')
    SS = 4
    N = 128 * SS

    def finish(img, name, pal):
        img = img.resize((128, 128), Image.LANCZOS)
        full = Image.new('RGBA', (256, 128), (0, 0, 0, 0))
        full.paste(img, (0, 0))
        full.paste(img.transpose(Image.FLIP_LEFT_RIGHT), (128, 0))
        full.save(os.path.join(out, '%s_%d.png' % (name, pal)))

    rnd = random.Random(900)
    dark, mid, light = hexc('#2a0006'), hexc('#a80e1c'), hexc('#ff5a64')
    img = Image.new('RGBA', (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for r, col, off in ((46, dark, 0), (40, mid, 0), (30, hexc('#d01a2a'), -3)):
        R = r * SS
        cy = 70 * SS + off * SS
        d.ellipse((64 * SS - R, cy - R, 64 * SS + R, cy + R), fill=col)
    d.polygon([(64 * SS, 6 * SS), (44 * SS, 40 * SS), (84 * SS, 40 * SS)], fill=dark)
    d.polygon([(64 * SS, 12 * SS), (50 * SS, 40 * SS), (78 * SS, 40 * SS)], fill=mid)
    d.ellipse((42 * SS, 52 * SS, 62 * SS, 72 * SS), fill=light)
    d.ellipse((46 * SS, 56 * SS, 54 * SS, 63 * SS), fill=hexc('#ffd8d8'))
    finish(img, 'blob', 1)

    img = Image.new('RGBA', (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for scale, col in ((1.0, dark), (0.82, mid), (0.5, hexc('#d01a2a'))):
        pts = []
        for i in range(36):
            a = i * math.pi * 2 / 36
            r = (34 + rnd.random() * 26 + (14 if i % 6 == 0 else 0)) * scale
            pts.append((64 * SS + math.cos(a) * r * SS, 64 * SS + math.sin(a) * r * SS))
        d.polygon(pts, fill=col)
    for _ in range(9):
        a = rnd.random() * math.pi * 2
        r = 58 + rnd.random() * 6
        x, y = 64 + math.cos(a) * r, 64 + math.sin(a) * r
        R = (3 + rnd.random() * 4) * SS
        d.ellipse((x * SS - R, y * SS - R, x * SS + R, y * SS + R), fill=mid)
    finish(img, 'splash', 1)

    img = Image.new('RGBA', (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    top, bot = [], []
    for i in range(41):
        x = 2 + i * 124 / 40.0
        wob = math.sin(i * 0.7) * 4 + rnd.random() * 2
        hw = 40 + math.sin(i * 0.45) * 6
        top.append((x * SS, (64 - hw + wob) * SS))
        bot.append((x * SS, (64 + hw + wob) * SS))
    d.polygon(top + bot[::-1], fill=dark)
    top2 = [(x, 64 * SS + (y - 64 * SS) * 0.82) for x, y in top]
    bot2 = [(x, 64 * SS + (y - 64 * SS) * 0.82) for x, y in bot]
    d.polygon(top2 + bot2[::-1], fill=mid)
    top3 = [(x, 64 * SS + (y - 64 * SS) * 0.4 - 8 * SS) for x, y in top]
    bot3 = [(x, 64 * SS + (y - 64 * SS) * 0.14 - 8 * SS) for x, y in top]
    d.polygon(top3 + bot3[::-1], fill=light)
    for _ in range(10):
        x, y = 8 + rnd.random() * 112, 64 + (rnd.random() - 0.5) * 70
        R = (2 + rnd.random() * 3) * SS
        d.ellipse((x * SS - R, y * SS - R, x * SS + R, y * SS + R), fill=hexc('#ff9aa0'))
    finish(img, 'torrent', 1)

    img = Image.new('RGBA', (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for i, r in enumerate((54, 42, 30, 18)):
        R = r * SS
        d.ellipse((64 * SS - R, 64 * SS - R, 64 * SS + R, 64 * SS + R), outline=(200, 225, 255, 70 - i * 12), width=3 * SS)
    img = img.filter(ImageFilter.GaussianBlur(2.5 * SS))
    finish(img, 'distort', 0)


def fx_arrow():
    from PIL import ImageDraw, ImageFilter
    out = os.path.join(TEX, 'fx')
    SS = 4
    N = 128 * SS
    edge, mid, core = hexc('#3a1400'), hexc('#ff7a10'), hexc('#fff3b0')
    img = Image.new('RGBA', (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    glow = Image.new('RGBA', (N, N), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.polygon([(8 * SS, 64 * SS), (96 * SS, 40 * SS), (126 * SS, 64 * SS), (96 * SS, 88 * SS)], fill=(255, 120, 20, 120))
    glow = glow.filter(ImageFilter.GaussianBlur(7 * SS))
    img = Image.alpha_composite(img, glow)
    d = ImageDraw.Draw(img)
    d.polygon([(2 * SS, 64 * SS), (30 * SS, 40 * SS), (22 * SS, 64 * SS), (30 * SS, 88 * SS)], fill=edge)
    d.polygon([(6 * SS, 64 * SS), (28 * SS, 46 * SS), (23 * SS, 64 * SS), (28 * SS, 82 * SS)], fill=mid)
    d.polygon([(14 * SS, 56 * SS), (34 * SS, 30 * SS), (30 * SS, 58 * SS)], fill=mid)
    d.polygon([(14 * SS, 72 * SS), (34 * SS, 98 * SS), (30 * SS, 70 * SS)], fill=mid)
    d.rectangle((22 * SS, 59 * SS, 100 * SS, 69 * SS), fill=edge)
    d.rectangle((24 * SS, 61 * SS, 98 * SS, 67 * SS), fill=mid)
    d.rectangle((30 * SS, 63 * SS, 96 * SS, 65 * SS), fill=core)
    d.polygon([(96 * SS, 44 * SS), (126 * SS, 64 * SS), (96 * SS, 84 * SS), (104 * SS, 64 * SS)], fill=edge)
    d.polygon([(98 * SS, 49 * SS), (121 * SS, 64 * SS), (98 * SS, 79 * SS), (105 * SS, 64 * SS)], fill=mid)
    d.polygon([(101 * SS, 57 * SS), (116 * SS, 64 * SS), (101 * SS, 71 * SS)], fill=core)
    img = img.resize((128, 128), Image.LANCZOS)
    full = Image.new('RGBA', (256, 128), (0, 0, 0, 0))
    full.paste(img, (0, 0))
    full.paste(img.transpose(Image.FLIP_LEFT_RIGHT), (128, 0))
    full.save(os.path.join(out, 'arrow_4.png'))


if __name__ == '__main__':
    orb_blue()
    orb_red()
    orb_purple()
    orb_purple_small()
    orb_small('orb_blue_small', '#1c6bff', '#e8fbff', '#9fd8ff', '#0a1d6e', 4.5)
    orb_small('orb_red_small', '#e5141e', '#ffb0a0', '#14040a', '#ff7a6a', 4.5)
    mahoraga()
    gear()
    blocks()
    sukuna_skin()
    fx_textures()
    fx_extra()
    fx_arrow()
    print('ok')
