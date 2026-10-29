# Rendering in Minecraft 26.3

What drawing heroes, effects and HUD in 26.3's renderer taught. Up: [Techniques](../README.md) · engine facts:
[minecraft_26](../minecraft/minecraft_26.md)

## Models and meshes
- **Read the mesh before changing its shading.** The talisman's "grey back" was its own layer of triangles (368 on
  the front, 208 on the back) textured with Blizzard's baked shade, not a back face lit the wrong way round. A normal
  flip changed nothing there, and in the menu's mirrored orthographic preview it turned half her face dark.
- **Overwatch's shaders do work a glTF export loses.** A one-colour texture on a blended piece over the eyes is a
  shadow the shader fades; an eyeball image with only the iris opaque gets its white from the shader. Check which
  bones a piece follows and what its texture holds before drawing it as is.
- **Check an engine assumption in its source before building on it**: "the entity render types cull back faces" was
  wrong for the cutout pipeline (`withCull(false)` in RenderPipelines), and CPU culling built on it opened holes in
  her hair. A visual on/off comparison from several angles caught it.
- **When something draws nothing, test it alone and still:** summon it motionless (before `/tick freeze`, or it never
  reaches the client) and look from several sides. Two "fixes" for a paper that wasn't showing were aimed at the wrong
  cause: the projectile models had never drawn at all (a prop-hiding rule for hero models matched them).
- **Expensive per-frame geometry gets a coarse copy.** Her shadow from her full 67k-triangle mesh cost 6 ms. Vertices
  merged into 3.5 cm cells once per piece, keeping the triangles between three cells, look the same and cost 0.45 ms.
- **Overlapping flat triangles darken once in two passes:**
  - mark the pixels in the frame's alpha (blend MIN, alpha only);
  - then darken by the mark and reset it (colour times DST_ALPHA).

  Submit order is honoured across orders (`collector.order(n)`). A depth test alone speckles.
- **Custom geometry gets no outline from the entity's `outlineColor`:** submit the mesh again with
  `renderType.outline()`, as `DragonFireballRenderer` does.
- **An effect mesh loads only if `HeroModels.EXTRAS` names it** (2026-10-28). Jetpack Cat's torus and cone were in
  the pack, `Fx.mesh` returned null, and her rings and arm glow simply never drew. When an effect mesh draws nothing,
  check the list before the effect.
- **Held items, sized by measurement:** `ItemStackRenderState.getModelBoundingBox()` gives an item's box as held. Its
  thin side tells a tool (across) from a sprite (along the view), and its longest side makes a size cap.

## First person
- 26.3 draws the first-person hands in their own pass (`GameRenderer.renderItemInHand`). The pose stack starts as the
  inverse of the view rotation, so a point from the arms' pose is camera-relative in the world's axes, not view space.
  Log a suspect space at two headings: a point that turns with the view is in world axes.
- An effect that must sit on the first-person arms follows a point recorded while the arms are drawn
  (`Fx.Particle.follow`); placed once in the world, it stays behind as the hand moves on.
- The world's light reaches first-person models: at night the lightmap turned the arms near-black purple. A view model
  wants a light floor.

## Effects and glows
- **Minecraft's mipmapper treats any texture with transparency as a cutout:** it rescales alpha and adds 0.025 to the
  smaller mips. That's fine for alpha-tested models; soft masks drawn blended show faint boxes and swollen glows. Pass
  `MipmapStrategy.MEAN` for those (`HeroTexture`).
- **Additive light washes out over bright ground** (gold over grass turns lime): keep additive for teal and cyan
  glows, and blend warm colours (`Fx.soft`). Blend in a colour picked on screen, not from the game's linear ramp,
  whose bright end, gamma-encoded, is near-white cream.
- **Faint mask values become strong colour once gamma-encoded and added** (linear 0.17 is sRGB 115). Cut a mask's
  faint floor with a black point when only its core is meant to show.
- **A camera-facing glow centred on a hit surface sinks half into it:** lift it along the surface normal by r sin(angle
  between view and normal) (`Particle.lift`).
- **Near-camera fades must scale with the sprite:** a fixed 1.5 m fade let a 4.6 m glow two metres away cover the
  screen.
- **A full-screen tint that seems to come from the HUD may be world particles at the camera:** hide the HUD (F1) and
  measure again. Kitsune Rush's "mint grade" was eight smoke sprites spawned 0.6 m from the eye.
- **A drawn-twice glow (a translucent body, then its added glow) can be hidden by draw order.** Make both passes added
  light, or the order matters.
- **Check a mask's values before using it by its name** (Jetpack Cat, 2026-10-28): a "faint ring" (52E8) was bright in
  its corners and dark in the middle (a mask for a mesh, not a quad), and drawn on a quad it made a square; her crater
  crack (CBB9) is white where there's no crack, a decal meant to be multiplied, which additive and blended particles
  can't do (a 10 m white square). Print each mask's corner, edge and centre values first (`py tools/maskstat.py <effect texture dir> <names...>`).
- **A projectile's first frames are at the shooter's eyes in first person,** not at the view model's hand: a muzzle
  flare there filled the view, and a 0.5 m halo at 1 m was a bright ball beside her paw. Skip the muzzle for your own
  first-person shots and fade them in over a few metres (`JetpackCatFx.pawFlight`).
- **Masks the game multiplies on one mesh can be baked into one** when they share its UVs (`build_asset_pack.py`
  PRODUCTS; a particle draws one mask). Read the mesh's UVs first. On her ring torus (15A26) U runs round the ring and
  V round the tube, so the 1C7A strip (bright in the middle of V) lights the outer rim. The 299E3 noise alone filled
  the whole fat tube, and seven rings read as one solid disc (2026-10-28).
- **A soft `NATURAL` particle takes its shape from the mask's alpha.** A grey mask (solid alpha, its shape in its
  grey: the smoke) drew as white squares in Catnapper's landing. `EffectArt` now uses a solid-alpha mask's grey as its
  alpha; a texture with a real cut-out (Kiriko's climb dust) keeps its own.
- **A flat effect "on the ground" floats over any slope:** lay it in short pieces, each at the top of the first solid
  block under open space (`KirikoFx.groundY`).

### Lúcio's effects (2026-10-27)
- From [lucio-effects](../../tools/reference/lucio-effects.md) (a research agent read the emitters, the ESHT
  hardpoint tables and the colour strips). His song colour is in small sRGB strips, not the float ramps.
- His song shows on his skates, not in a ring on the ground: the trails are fed each frame from his posed foot bones
  in the world (`HeroModelRenderer.bone`).
- A round with no model is drawn by its flight effect alone (`ProjectileModelRenderer` with effectOnly), its item only
  without the pack.
- Look at effects shorter than a screenshot in slow motion (`fxslow 0.08`,
  [harness](../testing/harness.md)): at real speed Soundwave's 0.3 s blast never made it into a frame. And look from
  the side: rounds flying along the view stack their rings face-on and read as a glow on the target.
- Watch the white top of a palette: four rounds' glows stacked to a white ball. A glow gets its own palette without
  white (the data's own core colour).

## Fonts and text
- Minecraft's "ttf" font provider only takes TrueType outlines; CFF fonts (.otf "OTTO") are rejected. Fonts are placed
  by baseline 7 units below the draw position at any size.
- Font textures are sampled nearest by default (for the pixel font). Overwatch's faces shrunk about 2x get stair steps;
  sample the HUD's own fonts linearly (`HudFontTextureMixin`).
- A mixin that restyles every glyph of a line hides glyphs from other fonts as missing boxes: replace only
  `FontDescription.DEFAULT`. For icons in text, use a bitmap font provider for the glyph and a `space` provider for
  exact gaps.

## Overwatch's HUD head art shares one frame
Every hero's 512 px HUD head (HeroIcons, "head") is painted on the same frame template: the V's tip on row 412,
centred on column 273.5, the same edges for every hero. Map that template to the HUD's frame once, for all of them;
fitting each by its own outline (width, top of the hair) moved heroes whose outline differs (Jetpack Cat's ears: about
80 texels too high, #54). `HudShapes.cutHead`.
