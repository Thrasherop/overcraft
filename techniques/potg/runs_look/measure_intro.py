"""Bounding boxes of the intro text in a screenshot, as fractions of the screen (Refs #44, critique 4).
Usage: py -I measure_intro.py <png>   (no PIL: a small PNG reader, 8-bit RGB/RGBA, not interlaced)"""
import struct, sys, zlib


def read(path):
    data = open(path, "rb").read()
    pos, w, h, ctype, idat = 8, 0, 0, 0, b""
    while pos < len(data):
        n, kind = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + n]
        if kind == b"IHDR":
            w, h, depth, ctype = struct.unpack(">IIBB", body[:10])
        elif kind == b"IDAT":
            idat += body
        pos += 12 + n
    bpp = 4 if ctype == 6 else 3
    raw = zlib.decompress(idat)
    rows, prev, i = [], bytearray(w * bpp), 0
    for _ in range(h):
        f = raw[i]; i += 1
        line = bytearray(raw[i:i + w * bpp]); i += w * bpp
        for x in range(len(line)):
            a = line[x - bpp] if x >= bpp else 0
            b = prev[x]
            c = prev[x - bpp] if x >= bpp else 0
            if f == 1: line[x] = (line[x] + a) & 255
            elif f == 2: line[x] = (line[x] + b) & 255
            elif f == 3: line[x] = (line[x] + (a + b) // 2) & 255
            elif f == 4:
                p = a + b - c; pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        rows.append(line); prev = line
    return w, h, bpp, rows


w, h, bpp, rows = read(sys.argv[1])
def box(test, x1, y0, y1):
    xs, ys = [], []
    for y in range(int(y0 * h), int(y1 * h)):
        r = rows[y]
        for x in range(0, int(x1 * w)):
            p = r[x * bpp:x * bpp + 3]
            if test(p[0], p[1], p[2]):
                xs.append(x); ys.append(y)
    return (min(xs) / w, min(ys) / h, max(xs) / w, max(ys) / h) if xs else None
orange = lambda r, g, b: r > 200 and 110 < g < 190 and b < 90
light = lambda r, g, b: r > 175 and g > 190 and b > 200 and abs(r - b) < 40
for name, test, y0, y1 in [("title", light, 0.15, 0.24), ("name", orange, 0.22, 0.33), ("hero line", light, 0.33, 0.40)]:
    b = box(test, 0.35, y0, y1)
    print(name, "left %.3f W, top %.3f H, right %.3f W, bottom %.3f H, cap %.3f H" % (b[0], b[1], b[2], b[3], b[3] - b[1]) if b else "none")
