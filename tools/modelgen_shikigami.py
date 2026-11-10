#!/usr/bin/env python3
"""3D-модели эффектов Мегуми и Юты (GeckoLib): сикигами, пул теней, Рика, волны высвобождения.
Запуск из корня проекта: python3 tools/modelgen_shikigami.py (нужен Pillow). Не трогает существующие модели."""
import json, math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from modelgen import Geo, mat, face_mat, hexc, sphere_cluster, ring, ANI  # noqa: E402


def sway_anim(name, bones, length=2.0):
    """bones: имя -> (ось 0/1/2, амплитуда в градусах, периодов за цикл). Записывается как animation.orb.spin."""
    out = {}
    for b, (axis, amp, periods) in bones.items():
        frames = {}
        steps = int(4 * periods)
        for i in range(steps + 1):
            t = length * i / steps
            ang = amp * math.sin(2 * math.pi * i / 4.0)
            v = [0, 0, 0]
            v[axis] = round(ang, 3)
            frames[format(t, '.3f')] = v
        out[b] = {'rotation': frames}
    d = {'format_version': '1.8.0', 'animations': {'animation.orb.spin': {'loop': True, 'animation_length': length, 'bones': out}}}
    with open(os.path.join(ANI, name + '.animation.json'), 'w') as f:
        json.dump(d, f, indent=1)


def dog(name, fur, dark, eye):
    g = Geo(name, 256, 256)
    body = mat(hexc(fur), 0.05)
    shade_m = mat(hexc(dark), 0.05)
    g.bone('body', (0, 10, 0))
    g.cube('body', [-4, 8, -8], [8, 7, 16], body)
    g.bone('head', (0, 13, -8), 'body')
    g.cube('head', [-3.5, 11, -14], [7, 6, 7], body)
    g.cube('head', [-2, 11, -17], [4, 3, 3], shade_m)
    g.cube('head', [-3.5, 17, -10], [2, 3, 1], shade_m)
    g.cube('head', [1.5, 17, -10], [2, 3, 1], shade_m)
    eye_m = mat(hexc(eye), 0.0, 1.0)
    g.cube('head', [-3.0, 14.5, -14.3], [1.6, 1.0, 0.5], eye_m)
    g.cube('head', [1.4, 14.5, -14.3], [1.6, 1.0, 0.5], eye_m)
    for i, (x, z) in enumerate([(-4, -7.5), (1.5, -7.5), (-4, 5), (1.5, 5)]):
        g.bone('leg%d' % i, (x + 1.25, 8, z + 1.25), 'body')
        g.cube('leg%d' % i, [x, 0, z], [2.5, 8, 2.5], shade_m)
    g.bone('tail', (0, 14, 8), 'body')
    g.cube('tail', [-1, 13, 8], [2, 2, 8], body, rotation=[-30, 0, 0], pivot=[0, 14, 8])
    g.write(name)
    sway_anim(name, {'tail': (1, 22, 2), 'head': (0, 4, 1)}, 1.0)


def nue():
    g = Geo('shikigami_nue', 256, 256)
    body = mat(hexc('#2b3a67'), 0.05, stripes=(3, 0.8))
    gold = mat(hexc('#f4d03f'), 0.04)
    spark = mat(hexc('#7fe8ff'), 0.0, 1.0)
    g.bone('body', (0, 12, 0))
    g.cube('body', [-3, 10, -6], [6, 6, 12], body)
    g.bone('head', (0, 14, -6), 'body')
    g.cube('head', [-2.5, 13, -11], [5, 5, 5], body)
    g.cube('head', [-1, 13, -14], [2, 1.5, 3], gold)
    g.cube('head', [-2.2, 15.2, -11.3], [1.2, 1.0, 0.5], spark)
    g.cube('head', [1.0, 15.2, -11.3], [1.2, 1.0, 0.5], spark)
    g.bone('wing_l', (3, 14, 0), 'body')
    g.cube('wing_l', [3, 13.5, -5], [20, 1, 10], body, rotation=[0, 0, 8], pivot=[3, 14, 0])
    g.cube('wing_l', [20, 13.2, -3], [8, 0.8, 6], gold, rotation=[0, 0, 12], pivot=[3, 14, 0])
    g.bone('wing_r', (-3, 14, 0), 'body')
    g.cube('wing_r', [-23, 13.5, -5], [20, 1, 10], body, rotation=[0, 0, -8], pivot=[-3, 14, 0])
    g.cube('wing_r', [-28, 13.2, -3], [8, 0.8, 6], gold, rotation=[0, 0, -12], pivot=[-3, 14, 0])
    g.bone('tail', (0, 12, 6), 'body')
    g.cube('tail', [-2, 11, 6], [4, 1, 10], body)
    g.bone('sparks', (0, 14, 0))
    for rot in [(0, 0, 0), (0, 60, 0), (0, 120, 0)]:
        g.cube('sparks', [-0.4, 14, -14], [0.8, 0.8, 28], spark, rotation=list(rot), pivot=[0, 14, 0])
    g.write('shikigami_nue')
    sway_anim('shikigami_nue', {'wing_l': (2, 28, 3), 'wing_r': (2, -28, 3), 'sparks': (1, 40, 1)}, 0.8)


def toad():
    g = Geo('shikigami_toad', 256, 256)
    skin = mat(hexc('#4f8a3c'), 0.07)
    belly = mat(hexc('#cfe29a'), 0.05)
    eye = mat(hexc('#f0d23c'), 0.03)
    dark = mat(hexc('#14240e'), 0.02)
    g.bone('body', (0, 0, 0))
    g.cube('body', [-7, 0, -7], [14, 8, 14], skin)
    g.cube('body', [-6, 0, -6.5], [12, 2, 13], belly)
    g.bone('head', (0, 6, -7), 'body')
    g.cube('head', [-6, 6, -13], [12, 6, 8], skin)
    g.cube('head', [-5.2, 6.5, -13.3], [10.4, 1, 0.5], dark)
    g.cube('head', [-5.5, 11.5, -11], [3, 3, 3], eye)
    g.cube('head', [2.5, 11.5, -11], [3, 3, 3], eye)
    g.cube('head', [-4.6, 12.3, -11.4], [1, 1.6, 0.5], dark)
    g.cube('head', [3.6, 12.3, -11.4], [1, 1.6, 0.5], dark)
    for sx in (-1, 1):
        x = 8 if sx > 0 else -12
        g.cube('body', [x, 0, 1], [4, 5, 8], skin)
        g.cube('body', [x * 0.9 if sx < 0 else x, 0, -9], [4, 4, 3], skin)
    g.write('shikigami_toad')
    sway_anim('shikigami_toad', {'head': (0, 6, 1)}, 1.2)


def rabbit():
    g = Geo('shikigami_rabbit', 128, 128)
    fur = mat(hexc('#f4f1ea'), 0.04)
    pink = mat(hexc('#f5b5c2'), 0.04)
    dark = mat(hexc('#222222'), 0.0, 1.0)
    g.bone('body', (0, 0, 0))
    g.cube('body', [-2.5, 0, -4], [5, 5, 8], fur)
    g.bone('head', (0, 4, -4), 'body')
    g.cube('head', [-2, 4, -7], [4, 4, 4], fur)
    g.cube('head', [-1.6, 5.8, -7.3], [0.8, 0.8, 0.5], dark)
    g.cube('head', [0.8, 5.8, -7.3], [0.8, 0.8, 0.5], dark)
    g.bone('ear_l', (1, 8, -5), 'head')
    g.cube('ear_l', [0.4, 8, -5.6], [1.2, 5, 1], fur)
    g.cube('ear_l', [0.7, 8.4, -5.7], [0.6, 4, 0.4], pink)
    g.bone('ear_r', (-1, 8, -5), 'head')
    g.cube('ear_r', [-1.6, 8, -5.6], [1.2, 5, 1], fur)
    g.cube('ear_r', [-1.3, 8.4, -5.7], [0.6, 4, 0.4], pink)
    g.cube('body', [-1, 1, 4], [2, 2, 2], fur)
    g.cube('body', [-2.5, 0, 1], [1.5, 2, 3], fur)
    g.cube('body', [1, 0, 1], [1.5, 2, 3], fur)
    g.write('shikigami_rabbit')
    sway_anim('shikigami_rabbit', {'ear_l': (2, 14, 2), 'ear_r': (2, -14, 2), 'body': (0, 8, 2)}, 0.6)


def elephant():
    g = Geo('shikigami_elephant', 256, 256)
    skin = mat(hexc('#9aa4b0'), 0.05, stripes=(5, 0.92))
    light = mat(hexc('#d9e6f2'), 0.04)
    water = mat(hexc('#4aa8ff'), 0.04)
    dark = mat(hexc('#141a22'), 0.02)
    g.bone('body', (0, 12, 0))
    g.cube('body', [-14, 10, -12], [28, 24, 26], skin)
    g.cube('body', [-14, 30, -12], [28, 3, 26], water)
    g.bone('head', (0, 24, -12), 'body')
    g.cube('head', [-10, 16, -26], [20, 18, 14], skin)
    g.cube('head', [-6.5, 24.5, -26.4], [3, 3, 0.5], dark)
    g.cube('head', [3.5, 24.5, -26.4], [3, 3, 0.5], dark)
    g.cube('head', [-9, 14, -34], [2.5, 2.5, 12], light)
    g.cube('head', [6.5, 14, -34], [2.5, 2.5, 12], light)
    g.bone('trunk', (0, 22, -26), 'head')
    g.cube('trunk', [-3, 14, -38], [6, 8, 13], skin)
    g.cube('trunk', [-2.5, 17, -50], [5, 5, 13], skin, rotation=[28, 0, 0], pivot=[0, 20, -38])
    g.cube('trunk', [-2.0, 25, -58], [4, 4, 10], water, rotation=[55, 0, 0], pivot=[0, 22, -50])
    g.bone('ear_l', (10, 30, -14), 'head')
    g.cube('ear_l', [10, 20, -16], [2, 16, 12], skin, rotation=[0, 0, -10], pivot=[10, 30, -14])
    g.bone('ear_r', (-10, 30, -14), 'head')
    g.cube('ear_r', [-12, 20, -16], [2, 16, 12], skin, rotation=[0, 0, 10], pivot=[-10, 30, -14])
    for i, (x, z) in enumerate([(-13, -11), (6, -11), (-13, 6), (6, 6)]):
        g.cube('body', [x, 0, z], [7, 12, 7], skin)
    g.write('shikigami_elephant')
    sway_anim('shikigami_elephant', {'trunk': (0, 7, 1), 'ear_l': (1, 10, 2), 'ear_r': (1, -10, 2)}, 1.6)


def shadow_pool():
    g = Geo('shadow_pool', 128, 128)
    black = mat(hexc('#07050c'), 0.03)
    violet = mat(hexc('#3b1a66'), 0.05)
    g.bone('pool', (0, 0.4, 0))
    g.cube('pool', [-13, 0, -13], [26, 0.6, 26], black)
    ring(g, 'pool', (0, 0.5, 0), 12.5, 1.2, violet, 28)
    g.bone('inner', (0, 0.5, 0))
    ring(g, 'inner', (0, 0.9, 0), 7.5, 0.9, violet, 20)
    g.write('shadow_pool')
    sway_anim('shadow_pool', {'inner': (1, 40, 1)}, 3.0)


def rika():
    """Рика ближе к аниме: высокая бледная фигура в рваном белом платье, длинные чёрные волосы, зубастая улыбка, длинные руки с когтями."""
    g = Geo('rika', 256, 256)
    dress = mat(hexc('#ece7df'), 0.04, stripes=(6, 0.93))
    dress_dark = mat(hexc('#cfc9c0'), 0.04)
    black = mat(hexc('#0b090e'), 0.03)
    skin = mat(hexc('#dcd6ce'), 0.04)
    corset = mat(hexc('#17202c'), 0.04)
    claw = mat(hexc('#15121a'), 0.02)
    bone = mat(hexc('#f4f1ea'), 0.03)

    def face(x, y, w, h):
        # глаза: чёрные овалы с белыми точками; рот: красные губы и ряд зубов через всё лицо
        if y in (4, 5, 6) and (1 <= x <= 3 or 7 <= x <= 9):
            if y == 5 and x in (2, 8):
                return hexc('#f5f5f5')
            return hexc('#050505')
        if y == 8 and 0 <= x <= 10:
            return hexc('#b01020')
        if y == 9 and 0 <= x <= 10:
            return hexc('#fafafa') if x % 2 == 0 else hexc('#2a0508')
        if y == 10 and 1 <= x <= 9:
            return hexc('#8a0c18')
        if y == 7 and x in (0, 10):
            return hexc('#3a3a3a')
        return None
    face_m = face_mat(hexc('#e6e0d8'), face, front_only=True)

    g.bone('body', (0, 28, 0))
    g.cube('body', [-10, 0, -7], [20, 10, 14], dress)
    g.cube('body', [-8.5, 10, -6], [17, 12, 12], dress_dark)
    g.cube('body', [-7, 22, -5], [14, 8, 10], dress)
    for i in range(10):
        ang = i * 36.0
        rad = math.radians(ang)
        g.cube('body', [-1.5, -3.5, -1.5], [3, 7, 3], dress, rotation=[0, ang, 0], pivot=[0, 0, 0]) if False else None
        x = math.sin(rad) * 10.5
        z = math.cos(rad) * 8.5
        g.cube('body', [x - 1.8, -3.5, z - 1.8], [3.6, 7, 3.6], dress_dark)
    g.cube('body', [-5.5, 30, -3.2], [11, 16, 6.4], corset)
    for i, yy in enumerate((33, 36, 39, 42)):
        g.cube('body', [-5.0, yy, -3.6], [10, 0.8, 0.6], bone)
    g.cube('body', [-9, 43, -3.2], [18, 3, 6.4], dress)
    g.cube('body', [-1.8, 46, -1.8], [3.6, 5, 3.6], skin)

    g.bone('head', (0, 51, 0), 'body')
    g.cube('head', [-5.5, 51, -5.5], [11, 13, 11], face_m)
    g.cube('head', [-6.5, 34, 5.5], [13, 28, 3], black)
    g.cube('head', [-6.2, 62, -6], [12.4, 3, 12], black)
    g.cube('head', [-5.8, 59.5, -6.4], [11.6, 3.5, 2], black)
    g.cube('head', [5.2, 38, -5.6], [2.6, 26, 2.6], black)
    g.cube('head', [-7.8, 38, -5.6], [2.6, 26, 2.6], black)
    for i, x in enumerate((-4, -2, 0, 2, 4)):
        g.cube('head', [x - 0.8, 64, -1], [1.6, 6 + (2 if i % 2 == 0 else 0), 1.6], bone, rotation=[0, 0, (i - 2) * 8], pivot=[x, 64, 0])

    for side, sx in (('arm_l', 1), ('arm_r', -1)):
        px = 8.5 * sx
        g.bone(side, (px, 45, 0), 'body')
        ux = px - 2 if sx > 0 else px - 2
        g.cube(side, [ux, 27, -2], [4, 18, 4], skin)
        g.cube(side, [ux + 0.3, 9, -1.7], [3.4, 18, 3.4], skin)
        g.cube(side, [ux - 1.0, 3, -3], [6, 7, 6], skin)
        for k in range(4):
            g.cube(side, [ux - 0.6 + k * 1.5, -6, -2.2 + (k % 2) * 2.2], [0.9, 9.5, 0.9], claw)
    g.write('rika')
    sway_anim('rika', {'arm_l': (0, 10, 1), 'arm_r': (0, -10, 1), 'head': (1, 6, 1), 'body': (0, 2, 2)}, 3.0)


def serpent():
    g = Geo('shikigami_serpent', 256, 256)
    skin = mat(hexc('#3a2f55'), 0.06, stripes=(4, 0.9))
    belly = mat(hexc('#cfc3e6'), 0.05)
    red = mat(hexc('#d02030'), 0.0, 1.0)
    fang = mat(hexc('#fafafa'), 0.02)
    g.bone('body', (0, 4, 0))
    for i in range(14):
        z = 6 + i * 6.2
        x = math.sin(i * 0.7) * 5
        sz = 7.0 - i * 0.28
        g.cube('body', [x - sz / 2, 0, z - 3.6], [sz, sz * 0.85, 7.2], skin)
        g.cube('body', [x - sz / 2 + 0.6, -0.2, z - 3.4], [sz - 1.2, 1.2, 6.8], belly)
    g.bone('head', (0, 5, 0), 'body')
    g.cube('head', [-4.5, 0, -10], [9, 7, 10], skin)
    g.cube('head', [-3.5, -0.5, -12.5], [7, 3.5, 3], skin)
    g.cube('head', [-3.4, 6.2, -7.5], [2, 1.2, 0.6], red)
    g.cube('head', [1.4, 6.2, -7.5], [2, 1.2, 0.6], red)
    g.cube('head', [-2.6, -2.8, -12.2], [1, 3, 1], fang)
    g.cube('head', [1.6, -2.8, -12.2], [1, 3, 1], fang)
    g.write('shikigami_serpent')
    sway_anim('shikigami_serpent', {'body': (1, 6, 3), 'head': (1, 8, 3)}, 1.0)


def deer():
    g = Geo('shikigami_deer', 256, 256)
    fur = mat(hexc('#f3efe6'), 0.04)
    pale = mat(hexc('#fffaf0'), 0.03)
    antler = mat(hexc('#e8dcc0'), 0.04)
    dark = mat(hexc('#201a1a'), 0.02)
    flower = mat(hexc('#ff9ec4'), 0.05)
    g.bone('body', (0, 14, 0))
    g.cube('body', [-4.5, 12, -9], [9, 9, 18], fur)
    g.cube('body', [-4, 12, -8.5], [8, 2, 17], pale)
    for x, z in ((-4, -8), (1.5, -8), (-4, 5.5), (1.5, 5.5)):
        g.cube('body', [x, 0, z], [2.6, 12, 2.6], fur)
    g.bone('head', (0, 20, -9), 'body')
    g.cube('head', [-2, 19, -12], [4, 10, 4], fur)
    g.cube('head', [-3, 27, -17], [6, 6, 9], fur)
    g.cube('head', [-2.6, 30, -17.3], [1.3, 1.3, 0.5], dark)
    g.cube('head', [1.3, 30, -17.3], [1.3, 1.3, 0.5], dark)
    for sx in (-1, 1):
        g.cube('head', [sx * 2.2 - 0.5, 33, -11], [1, 8, 1], antler, rotation=[0, 0, sx * -0.25], pivot=[sx * 2.2, 33, -11])
        g.cube('head', [sx * 5.0 - 0.5, 38, -11], [1, 6, 1], antler, rotation=[0, 0, sx * -0.6], pivot=[sx * 4.0, 38, -11])
        g.cube('head', [sx * 3.0 - 0.5, 36, -13], [1, 5, 1], antler, rotation=[0.5, 0, sx * -0.3], pivot=[sx * 3.0, 36, -11])
        g.cube('head', [sx * 6.6 - 1.2, 43, -11.5], [2.4, 2.4, 2.4], flower)
    g.bone('tail', (0, 20, 9), 'body')
    g.cube('tail', [-1.5, 19, 9], [3, 3, 3], pale)
    g.write('shikigami_deer')
    sway_anim('shikigami_deer', {'head': (0, 4, 1), 'tail': (1, 14, 2)}, 1.6)


def ox():
    g = Geo('shikigami_ox', 256, 256)
    hide = mat(hexc('#4a3326'), 0.06)
    dark = mat(hexc('#1c140f'), 0.04)
    horn = mat(hexc('#eadfc8'), 0.03)
    red = mat(hexc('#e03030'), 0.0, 1.0)
    g.bone('body', (0, 14, 0))
    g.cube('body', [-7, 10, -12], [14, 13, 24], hide)
    g.cube('body', [-6, 21, -9], [12, 6, 10], hide)
    for x, z in ((-7, -11), (3, -11), (-7, 8), (3, 8)):
        g.cube('body', [x, 0, z], [4, 11, 4], dark)
    g.bone('head', (0, 18, -12), 'body')
    g.cube('head', [-5, 12, -23], [10, 11, 11], hide)
    g.cube('head', [-3, 12, -25.5], [6, 4, 3], dark)
    g.cube('head', [-3.8, 19.5, -23.3], [2, 1.5, 0.6], red)
    g.cube('head', [1.8, 19.5, -23.3], [2, 1.5, 0.6], red)
    for sx in (-1, 1):
        g.cube('head', [sx * 6.5 - 1, 20, -20], [2, 2, 8], horn, rotation=[0, sx * 0.3, 0], pivot=[sx * 5, 20, -20])
        g.cube('head', [sx * 9.5 - 1, 20, -28], [2, 2, 7], horn, rotation=[0, sx * 0.8, 0], pivot=[sx * 8, 20, -26])
    g.write('shikigami_ox')
    sway_anim('shikigami_ox', {'head': (0, 6, 2)}, 1.0)


def tiger():
    g = Geo('shikigami_tiger', 256, 256)
    fur = mat(hexc('#e98a1c'), 0.06, stripes=(3, 0.55))
    belly = mat(hexc('#f7e6c4'), 0.04)
    dark = mat(hexc('#1a120a'), 0.03)
    eye = mat(hexc('#ffe35a'), 0.0, 1.0)
    g.bone('body', (0, 12, 0))
    g.cube('body', [-5, 10, -11], [10, 10, 22], fur)
    g.cube('body', [-4.5, 10, -10], [9, 2, 20], belly)
    for x, z in ((-5, -10), (2, -10), (-5, 6.5), (2, 6.5)):
        g.cube('body', [x, 0, z], [3, 10, 3.5], fur)
    g.bone('head', (0, 16, -11), 'body')
    g.cube('head', [-4.5, 12, -20], [9, 9, 9], fur)
    g.cube('head', [-3, 12, -22.5], [6, 4, 3], belly)
    g.cube('head', [-4.4, 21, -17], [2.4, 2.4, 1.4], fur)
    g.cube('head', [2.0, 21, -17], [2.4, 2.4, 1.4], fur)
    g.cube('head', [-3.4, 17, -20.3], [2, 1.4, 0.6], eye)
    g.cube('head', [1.4, 17, -20.3], [2, 1.4, 0.6], eye)
    g.cube('head', [-0.7, 12, -22.8], [1.4, 1.6, 0.8], dark)
    g.bone('tail', (0, 16, 11), 'body')
    g.cube('tail', [-1.5, 15, 11], [3, 3, 16], fur, rotation=[-0.6, 0, 0], pivot=[0, 16, 11])
    g.write('shikigami_tiger')
    sway_anim('shikigami_tiger', {'tail': (1, 16, 2), 'head': (0, 4, 1)}, 1.0)


def yuta_sword():
    g = Geo('yuta_sword', 128, 128)
    steel = mat(hexc('#dfe6f2'), 0.04)
    edge = mat(hexc('#ffffff'), 0.02)
    gold = mat(hexc('#d9b24c'), 0.04)
    grip = mat(hexc('#17141d'), 0.03)
    g.bone('sword', (0, 20, 0))
    g.cube('sword', [-1.1, 0, -0.5], [2.2, 34, 1.0], steel)
    g.cube('sword', [-0.3, 0, -0.6], [0.6, 33, 0.4], edge)
    g.cube('sword', [-3.0, 34, -1.6], [6, 1.4, 3.2], gold)
    g.cube('sword', [-1.1, 35.4, -1.1], [2.2, 10, 2.2], grip)
    g.write('yuta_sword')
    sway_anim('yuta_sword', {'sword': (1, 12, 1)}, 0.8)


def wave(name, radius, base, core, ring_a, ring_b, bolt, atlas=256):
    g = Geo(name, atlas, atlas)
    c = (0, radius * 1.0, 0)
    g.bone('core', c)
    streak = lambda x, y, w, h: hexc(core) if ((x * 3 + y * 5) % 11 < 2) else None
    sphere_cluster(g, 'core', c, radius, face_mat(hexc(base), streak))
    g.bone('ring_a', c)
    ring(g, 'ring_a', c, radius * 1.45, max(1.0, radius * 0.12), mat(hexc(ring_a), 0.05), 24)
    g.bone('ring_b', c, rotation=[70, 0, 0])
    ring(g, 'ring_b', c, radius * 1.3, max(0.8, radius * 0.1), mat(hexc(ring_b), 0.05), 24)
    g.bone('bolts', c)
    for rot in [(0, 0, 0), (0, 60, 0), (0, 120, 0), (90, 0, 0), (0, 0, 90)]:
        g.cube('bolts', [-0.6, c[1] - radius * 1.7, -0.6], [1.2, radius * 3.4, 1.2], mat(hexc(bolt), 0.05), rotation=list(rot), pivot=list(c))
    g.write(name)
    sway_anim(name, {'ring_a': (1, 180, 1), 'ring_b': (1, -180, 1), 'bolts': (1, 90, 1)}, 1.0)


if __name__ == '__main__':
    dog('shikigami_dog', '#f1f1f1', '#cfd2d8', '#ff5a5a')
    dog('shikigami_dog_black', '#17171c', '#050507', '#ffd24a')
    nue()
    toad()
    rabbit()
    elephant()
    shadow_pool()
    rika()
    serpent()
    deer()
    ox()
    tiger()
    yuta_sword()
    wave('release_wave_s', 6.0, '#bfe6ff', '#ffffff', '#6cc4ff', '#ffffff', '#e9f6ff')
    wave('release_wave_m', 10.0, '#a9d8ff', '#ffffff', '#4aa8ff', '#ffffff', '#d6efff')
    wave('release_wave_l', 16.0, '#8cc8ff', '#ffffff', '#2f8cff', '#e9f6ff', '#bfe6ff')
    wave('water_surge', 12.0, '#2f8cff', '#e9f6ff', '#9fd8ff', '#0a3d99', '#6cc4ff')
    print('ok')
