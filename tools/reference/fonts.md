# Overwatch HUD fonts

Extracted 2026-10-26 from the archived offline build 154088 (Overwatch closed, read-only, nothing fetched online).
The font files are Overwatch assets: they stay on the owner's machine (`E:\Overwatch Archive\extracted\154088\Fonts\`)
and reach the game only through the local asset pack, never the repo or the jar. Full notes and scripts:
`E:\Overwatch Archive\research\fonts\NOTES.md`.

## Where they are in the game data

Asset type `0x050` (font) holds a complete OpenType/TrueType file at offset 0x60 (size at offset 4), stored as is.
Type `0x051` (`STUFontFamily`) groups them: style slots -> `{typeface 0x050, weight, stretch}`.

## The Latin fonts

| File (in `Fonts\`) | Family / style (name table) | Use |
|---|---|---|
| `BigNoodleTooOblique.ttf` | BigNoodleTooOblique / Italic | The condensed oblique HUD font (matched by eye: the data doesn't say which UI uses which family): health "199 \| 225", ammo "10 \| 10", ult %, key labels. Proportional digits, caps only |
| `ttf-converted\BigNoodleTooOblique-Monospace.ttf` | BigNoodleTooOblique / Italic (PS name BigNoodleTooOblique-Monospace) | Same glyphs with tabular digits (all 387/1000 em), so changing numbers don't jitter. Most likely the face for the ticking HUD numbers |
| `FuturaNo2D-DemiBold.ttf` | Futura No2 D Demi Bold / Regular | Secondary/body text |
| `ttf-converted\Industry-Black.ttf`, `Industry-Light.ttf` | Industry / Black, Light | OW2 headings and labels |
| `ttf-converted\Config-Regular-Monospace.ttf` | Config Monospace / Regular-Monospace | Monospace text |
| `BlizzardGlobal-Regular.ttf` | BlizzardGlobal / Regular | 7 MB CJK fallback, not needed |

## Minecraft 26.3 needs TrueType outlines

`TrueTypeGlyphProviderDefinition` rejects any font FreeType doesn't report as `"TrueType"`. Industry, Config and the
Monospace BigNoodle ship as CFF `.otf` (FreeType: `"CFF"`), so they were converted to TrueType outlines
(`ttf-converted\`; per-glyph check through FreeType: same advances, bounds within 1/1000 em, renders match). Use those,
not the `.otf` files. The three `.ttf` in `Fonts\` load as they are.

Font definition (`assets/overcraft/font/hud.json`, font file in `assets/overcraft/font/`):

```json
{"providers": [{"type": "ttf", "file": "overcraft:bignoodletoooblique.ttf", "size": 11, "oversample": 4, "shift": [0, 0]}]}
```

Metrics for sizing (1000 units/em): BigNoodleTooOblique cap height 718, ascender 894, descender -182, slant ~14.5 deg.
