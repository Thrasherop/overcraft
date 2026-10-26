"""Placeholder item textures (original pixel art, 16x16), until hero art comes from the player's own Overwatch install.

    py tools/gen_textures.py
"""
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/overcraft/textures/item"

T = (0, 0, 0, 0)
STEEL = (196, 204, 214, 255)
STEEL_DARK = (120, 128, 140, 255)
EDGE = (236, 242, 250, 255)
WRAP = (40, 34, 52, 255)
RING = (214, 168, 62, 255)
PAPER = (240, 232, 206, 255)
PAPER_SHADE = (214, 202, 168, 255)
INK = (176, 36, 40, 255)
GOLD = (232, 186, 72, 255)
GOLD_DARK = (168, 120, 40, 255)
GOLD_LIGHT = (255, 230, 150, 255)
CORD = (200, 40, 48, 255)


def draw(rows, palette):
    img = Image.new("RGBA", (16, 16), T)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), palette[ch])
    return img


KUNAI = [
    "...............E",
    "..............ES",
    ".............ESS",
    "............ESSd",
    "...........ESSd.",
    "..........ESSd..",
    ".........ESSd...",
    "........ESSd....",
    ".......dSSd.....",
    "......wdd.......",
    ".....ww.........",
    "....ww..........",
    "...ww...........",
    "..rr............",
    ".r.r............",
    "..r.............",
]

OFUDA = [
    "....pppppppp....",
    "....pPPPPPPp....",
    "....pPiiiiPp....",
    "....pPPiiPPp....",
    "....pPPiiPPp....",
    "....pPiPPiPp....",
    "....pPiiiiPp....",
    "....pPPiiPPp....",
    "....pPPPPPPp....",
    "....pPgggPPp....",
    "....pPPgPPPp....",
    "....pPgggPPp....",
    "....pPPPPPPp....",
    "....pPiPPiPp....",
    "....pPPiiPPp....",
    "....pppppppp....",
]

SUZU = [
    "......cc........",
    ".....c..c.......",
    ".....c..c.......",
    "......cc........",
    "......gg........",
    ".....gLLg.......",
    "....gLLggg......",
    "...gLggggGg.....",
    "...gLgggggG.....",
    "..gLgggggggG....",
    "..ggggggggGG....",
    "..GggggggGGG....",
    "...GGddddGG.....",
    "....GG..GG......",
    "......cc........",
    ".....c..c.......",
]

WEAPON = [
    "........pppp...E",
    "........pPiPp.ES",
    "........pPiiPESS",
    "........pPiPESSd",
    "........pPPESSd.",
    "........pPESSd..",
    "........pESSdp..",
    "........ESSdPp..",
    ".......dSSdPPp..",
    "......wddpgPPp..",
    ".....ww.pPgPPp..",
    "....ww..pppppp..",
    "...ww...........",
    "..rr............",
    ".r.r............",
    "..r.............",
]


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    blade = {"E": EDGE, "S": STEEL, "d": STEEL_DARK, "w": WRAP, "r": RING}
    paper = {"p": PAPER_SHADE, "P": PAPER, "i": INK, "g": GOLD}
    bell = {"c": CORD, "g": GOLD, "G": GOLD_DARK, "L": GOLD_LIGHT, "d": GOLD_DARK}
    draw(KUNAI, blade).save(OUT / "kunai.png")
    draw(OFUDA, paper).save(OUT / "ofuda.png")
    draw(SUZU, bell).save(OUT / "suzu.png")
    draw(WEAPON, {**blade, **paper}).save(OUT / "kiriko_weapon.png")
    print("wrote", OUT)


if __name__ == "__main__":
    main()
