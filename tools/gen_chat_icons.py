"""Hero chat's channel icon (original art: drawn here, not taken from Overwatch, whose icon isn't in the UI texture dump):
three people, the middle one in front and larger, as Overwatch's team chat icon reads at 20 px. White, tinted by the
chat line's colour. Drawn 4x and scaled down for soft edges.

    py tools/gen_chat_icons.py
"""
from pathlib import Path

from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/overcraft/textures/font"
W, H, K = 38, 28, 4


def team():
    big = Image.new("L", (W * K, H * K), 0)
    d = ImageDraw.Draw(big)

    def disc(cx, cy, r, fill):
        d.ellipse([(cx - r) * K, (cy - r) * K, (cx + r) * K, (cy + r) * K], fill=fill)

    # the two behind: heads and rounded shoulders
    for cx in (8.5, 29.5):
        disc(cx, 8.5, 3.8, 255)
        d.rounded_rectangle([(cx - 6.5) * K, 13.5 * K, (cx + 6.5) * K, 23 * K], radius=4 * K, fill=255)
    # a gap round the one in front, so the three read apart
    disc(19, 6.5, 6.2, 0)
    d.polygon([(9 * K, 11 * K), (29 * K, 11 * K), (29 * K, 17 * K), (19 * K, 28 * K), (9 * K, 17 * K)], fill=0)
    # the one in front: head, then shoulders narrowing to a point
    disc(19, 6.5, 4.6, 255)
    d.rounded_rectangle([11 * K, 12.5 * K, 27 * K, 18 * K], radius=3 * K, fill=255)
    d.polygon([(11 * K, 16 * K), (27 * K, 16 * K), (19 * K, 27 * K)], fill=255)
    alpha = big.resize((W, H), Image.LANCZOS)
    icon = Image.new("RGBA", (W, H), (255, 255, 255, 0))
    icon.putalpha(alpha)
    return icon


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    team().save(OUT / "chat_team.png")
    print("wrote", OUT / "chat_team.png")


if __name__ == "__main__":
    main()
