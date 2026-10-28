import sys
from PIL import Image
d = sys.argv[1]
for n in sys.argv[2:]:
    im = Image.open(f'{d}/{n}.png')
    print(n, im.mode, im.size, end=' ')
    bands = im.getbands()
    for b in bands:
        ch = im.getchannel(b)
        w, h = ch.size
        corner = ch.getpixel((0, 0)); edge = ch.getpixel((w // 2, 0)); centre = ch.getpixel((w // 2, h // 2))
        print(f'{b}: min {ch.getextrema()[0]} max {ch.getextrema()[1]} corner {corner} edge {edge} centre {centre};', end=' ')
    print()
