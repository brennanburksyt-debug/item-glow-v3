# Item Glow (Fabric, Minecraft 1.21.11)

Animated glowing outline on the items you hold, with an in-game menu.

## Build

1. Install Java 21 or newer.
2. In this folder run `./gradlew build` (Windows: `gradlew.bat build`).
3. Your mod is `build/libs/item-glow-1.0.0.jar` (ignore the `-sources` jar).
4. Put it in `.minecraft/mods` with Fabric Loader and Fabric API for 1.21.11.

## Build without a computer (GitHub)

1. Make a free account at github.com and create a new repository.
2. Upload every file from this project to it (Add file > Upload files).
   If the hidden `.github` folder does not upload, use Add file > Create new file, type
   `.github/workflows/build.yml` as the name, and paste in the contents of that file.
3. Open the repository's **Actions** tab. The "Build mod jar" run starts by itself
   (or press "Run workflow"). It takes a few minutes.
4. Click the finished run and download **item-glow-jar** at the bottom. Unzip it to get the `.jar`.
5. If the run fails, open it and copy the red error text to Claude.

## Use

- Press **G** in game to open the menu. Rebind it in Options > Controls > Key Binds > Item Glow.
- Drag on the color wheel (angle = color, distance from center = saturation), then use the Brightness slider.
- Animation modes: Steady, Pulse, Ripple, Flicker, Rainbow, Strobe. Plus Speed, Intensity and Thickness sliders.
- Settings are saved in `.minecraft/config/itemglow.json`.

## Where to tweak things

- `GlowAnimator.java`: what each animation mode does.
- `GlowRenderer.java`: how the glow layers are built (`PASSES` = number of layers).
- `GlowScreen.java`: the menu.

## Known limits

- Works on normal model-based items (tools, blocks, food, ...). Items with special renderers
  (shields, chests, banners, heads, ...) won't glow.
- The glow is made from enlarged, tinted copies of the item, so it follows the item's own texture
  shape and is not a perfectly uniform outline.
