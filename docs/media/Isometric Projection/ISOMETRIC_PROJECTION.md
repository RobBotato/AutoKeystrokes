# Isometric Block Generation

This directory contains the tools necessary to generate mathematically perfect, high-resolution 3D isometric representations of Minecraft blocks from flat 16x16 2D textures.

## The Mathematics of the Render

Most basic isometric renders use a **2:1 Dimetric Projection** (also known as pixel-art isometric) where lines rise by 1 pixel for every 2 pixels across. 

However, this toolkit uses a **True 30-Degree Isometric Orthographic Projection**. This means:
- The camera is angled down exactly ~35.264° and rotated 45°.
- The top, left, and right faces of the block are identically proportioned parallelograms/rhombuses.
- It produces a slightly "taller", more realistic 3D look that matches modern, high-quality Minecraft item renders (like the official `GRASS_BLOCK.png`).

To eliminate transparent seams between the faces (a common artifact of affine anti-aliasing), the toolkit uses **Texture Clamping** (padding the 16x16 texture to 18x18 before projection) to ensure perfect, flawless intersections without black lines.

---

## Tool 1: `isometric_block_generator.py`

This script takes the flat 2D textures for the top and sides of a block and projects them into a 500x574 isometric render. It automatically applies standard Minecraft lighting (Top=100%, Left=80%, Right=60%).

### Usage

Run the script from the command line, providing the input textures and the desired output path:

```powershell
python isometric_block_generator.py --top <top_texture> --side <side_texture> --out <output_file>
```

**Example (Generating a Sandstone Block):**
```powershell
python "docs/media/Isometric Projection/isometric_block_generator.py" `
  --top docs/media/sandstone_top.png `
  --side docs/media/sandstone.png `
  --out docs/media/SANDSTONE_BLOCK.png
```

---

## Tool 2: `texture_tinter.py`

Some blocks in Minecraft (like Grass Blocks, Leaves, and Water) use a greyscale template texture that is dynamically tinted in-game based on the biome. This script allows you to mathematically apply a biome tint color to a greyscale texture before generating the block.

It specifically targets pixels that are purely greyscale, leaving pre-colored pixels (like the dirt on the side of a grass block) completely untouched.

### Usage

```powershell
python texture_tinter.py --input <input_texture> --out <output_texture> --color <HEX_COLOR>
```

**Example (Tinting a Grass Block Top to Lush Green):**
```powershell
python "docs/media/Isometric Projection/texture_tinter.py" `
  --input docs/media/grass_block_top.png `
  --out "docs/media/Isometric Projection/tinted_grass_top.png" `
  --color 99C867
```

---

## Full Workflow: Generating a Biome-Tinted Grass Block

If you have a `grass_block_top.png` and `grass_block_side.png` and want to create a beautifully tinted isometric render:

1. **Tint the Top Face:**
   ```powershell
   python texture_tinter.py --input grass_block_top.png --out tinted_top.png --color 99C867
   ```

2. **Tint the Side Overlay (Grass Fringe):**
   ```powershell
   python texture_tinter.py --input grass_block_side.png --out tinted_side.png --color 99C867
   ```

3. **Generate the Final Isometric Block:**
   ```powershell
   python isometric_block_generator.py --top tinted_top.png --side tinted_side.png --out MY_LUSH_GRASS_BLOCK.png
   ```
