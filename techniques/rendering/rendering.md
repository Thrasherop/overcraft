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
- **A flat effect "on the ground" floats over any slope:** lay it in short pieces, each at the top of the first solid
  block under open space (`KirikoFx.groundY`).

## Fonts and text
- Minecraft's "ttf" font provider only takes TrueType outlines; CFF fonts (.otf "OTTO") are rejected. Fonts are placed
  by baseline 7 units below the draw position at any size.
- Font textures are sampled nearest by default (for the pixel font). Overwatch's faces shrunk about 2x get stair steps;
  sample the HUD's own fonts linearly (`HudFontTextureMixin`).
- A mixin that restyles every glyph of a line hides glyphs from other fonts as missing boxes: replace only
  `FontDescription.DEFAULT`. For icons in text, use a bitmap font provider for the glyph and a `space` provider for
  exact gaps.
