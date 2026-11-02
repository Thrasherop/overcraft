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
- **`Fx.mesh` hands back every effect mesh one metre wide, standing on y = 0** (2026-10-31). The size you give it is
  the mesh's width in metres, and the point you give it is its base, not its centre. So an effect's size of 1 means the
  mesh's real width (read it from the .glb's bounds), and an offset to its centre (the effect's offset track) means
  lowering the base by half its height. Life Grip's bud was drawn at 1 m with its base on the ally's feet: a ball round
  their legs (the owner: "too small and centers on the feet"). Data: 1.96 m wide, centred 1.005 m up
  ([Lifeweaver](../heroes/lifeweaver.md)).
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
- **A view model's bone names don't say where its hands show (#78, 2026-11-02).** Kiriko's "_L" hand is on the right
  of the view; Juno's, Brigitte's, Sombra's and Lifeweaver's "_R" hand is, and their other hand is behind the camera.
  Mercy's "_R" weapon bone is out at her staff while her "_R" hand is at the camera. Measure with DevServer `viewbones`
  (the hand bones as drawn in Minecraft mode, plus each hand bone's turn and knuckles in its grip) instead of
  assuming. Minecraft mode now judges each hand by its knuckles' place in the view, at rest (a swing carries a hand
  out of the view's edge), and Minecraft draws an item on a side with no hand of hers in view.
- **One hero's hand-tuned grip carries to the others.** Kiriko's grips were set by eye. Every other view model's hand
  bone gets the turn that puts it, at rest, the way her bone sits in the view, and is shifted so its knuckles sit where
  hers do in the grip (`HELD_*_REF`, `HELD_*_KNUCKLES`, measured with `viewbones`). One measured reference, then
  no per-hero tuning, and the item still moves with the hand.

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
- **Effect meshes can tilt** (`Particle.mesh(triangles, side, up)`): a mesh's Y axis goes along up, and it stands on its
  base along up, so centre it by going back half its height. Her muzzle tori face along the shot; Purr's torus is
  stored standing like a wheel and is laid flat; the hook's cone is turned over, as the data does (2026-10-29).
- **The bottom fade of meshes is opt-in** (`fadeBase()`): it was on for every mesh and hid the lower 0.6 m of small
  ones (a 0.36 m cone, a muzzle torus). Only the torii's open posts need it.
- **Masks the game multiplies on one mesh can be baked into one** when they share its UVs (`build_asset_pack.py`
  PRODUCTS; a particle draws one mask). Read the mesh's UVs first. On her ring torus (15A26) U runs round the ring and
  V round the tube, so the 1C7A strip (bright in the middle of V) lights the outer rim. The 299E3 noise alone filled
  the whole fat tube, and seven rings read as one solid disc (2026-10-28).
- **A soft `NATURAL` particle takes its shape from the mask's alpha.** A grey mask (solid alpha, its shape in its
  grey: the smoke) drew as white squares in Catnapper's landing. `EffectArt` now uses a solid-alpha mask's grey as its
  alpha; a texture with a real cut-out (Kiriko's climb dust) keeps its own.
- **A flat effect "on the ground" floats over any slope:** lay it in short pieces, each at the top of the first solid
  block under open space (`KirikoFx.groundY`).
- **A compressed mask's floor lights the whole quad** (Lifeweaver, 2026-10-29): his burst, star, sparkle and hit masks
  read 1-15 of 255 over a fifth to a third of their area. Gamma-encoded and added, that floor drew the platform's 5 m
  bloom and Superbloom's flash as grainy squares. Look at the histogram's low end, not only the border, and give such
  masks a black point (`EffectArt.BLACK_POINT`: 0.04-0.06 was enough).
- **Many additive copies of a pale colour on one spot sum to white:** thirty pink leaf shards bursting from one point
  were a white cloud. Blend (`Fx.soft`) anything spawned densely in a light colour.
- **Match a ribbon's mask to its kind:** the data's tube ribbons (075D) are soft across, dark at both sides, as a tube
  seen from the side; a wispy mask that stays bright to its sides (his 2FCA7, made for the cross ribbons 07F5) drew
  his platform's 0.95 m rise trails as grainy rectangles. Tubes take a soft band (`LifeweaverFx.TUBE`).
- **A sphere round the camera must be mostly dark:** the tree's wave shell drawn with its bokeh mask alone (grey all
  over) tinted the whole view white-gold from inside it. The data multiplies three masks (bokeh x vertical fade x
  wisps); baked into one (`PRODUCTS` `lw_tree_wave`) it reads as a faint shell.
- **A projectile with no model draws its item sprite** (`ProjectileModelRenderer`) unless its case in
  `extractRenderState` sets `drawnByEffect`: feed the flight effect from there each frame (positions per frame, counts
  per second by dt), and return false without the local pack so the sprite still stands in.

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

## Effects and translucent terrain (2026-11-02, #80)
- **Blended custom geometry draws before water.** In 26.3, `SubmitNodeCollection.submitCustomGeometry` puts a
  RenderType with blending in `translucentCustomGeometry`, which `LevelRenderer.executeClassicTransparency` draws
  *before* the translucent terrain; Minecraft's own particles go to `afterTerrain`, drawn after it. Our effects write no
  depth, so water, stained glass and ice drew straight over them: Orbital Ray's circle on a pool showed only as a
  faint blue ghost.
- `Fx.afterTerrain` now submits the effects into `SubmitNodeStorage.order(0).afterTerrain` with a
  `CustomFeatureRenderer.Submit`, like vanilla particles. The circle then shows on water at full strength
  (`run2/screenshots/fix2_*.png` on fix/juno-liquids). An effect under a water surface is now hidden by it, as
  vanilla's particles are.
- **A liquid's surface is lower than the block's top** (a source with air above: 8/9 of a block). A decal laid on it
  sits under the grass round the pool, and the grass cut the ring off at the bank. On a liquid, the ground point is
  the block's top (`OrbitalRay.onTop`).
- **The price: effects behind glass, water or ice are hidden** (the 0.3.29 release play,
  [hand_play](../testing/hand_play.md#release-play-0329-2026-11-02)). In the after-terrain phase they're depth-tested
  against the translucent terrain drawn before them, as vanilla particles are without Improved Transparency: Brigitte's
  barrier seen through a stained-glass window isn't drawn.
  Since e73fe79 only effects at a liquid's surface go there (`Fx.atLiquid`); the rest draw before the translucent
  terrain again and the glass tints them (second pass: Suzu, the barrier, Sound Barrier, Purr, Ana's grenade,
  Valkyrie, Orbital Ray behind glass, water and ice all tinted, not gone).
- **Improved Transparency (F3+X, or the video option) and our render types** (Minecraft's `SubmitNodeCollection`, 26.3):
  with it on, every blended phase is one `oitTranslucent` phase drawn through each type's `OitPipelineSet`
  (`PreparedRenderType.drawFromBufferOit`), so:
  - a blended type without an OIT set crashes the frame ("does not have OIT pipelines set up": the hero shadow, fixed
    in 955e13c);
  - **a type counts as blended only if its pipeline has a colour target with a blend function**
    (`RenderType.hasBlending`). `ENTITY_SHADOW_SNIPPET` has none (vanilla's `ENTITY_SHADOW` adds
    `BlendFunction.TRANSLUCENT`), so a pipeline built from the bare snippet went to the solid pass and crashed it
    ("Render pass color attachment count must match pipeline color target state count", the first third-person frame
    with a hero's shadow: the 0.3.29 second pass). `HERO_SHADOW_PLAIN` now sets it;
  - **additive effects need Minecraft's additive OIT form**: an `OitPipelineSet` built with the `OIT_ADDITIVE` define
    (as its lightning, energy swirl and dragon rays are), which adds light and covers nothing. With the alpha-blended
    `OIT_ENTITY_EMISSIVE` every effect quad's black texels covered the scene: Orbital Ray was a black square with a
    ring. `FxRender.ADDITIVE_OIT` is the entity shader with the effect pipeline's defines plus `OIT_ADDITIVE`.
  Played with it on: a world joined with it saved on, seven heroes' effects through glass and water, third person and
  body shadows, then off again (`sheet_b_it_on_fixed.png`).
- **Attached particles and slow frames:** `Fx.attach` holds a particle at `life x (1 - fadeOut)` each client tick, but
  `simulate` ages it by the frame's time; at 10 fps a short life (the barrier's 0.2 s) ran out between feeds and the
  field showed in half the frames. A particle fed in the last 0.1 s now stays lit (`Particle.fedAt`).
