import argparse
from PIL import Image

def colorize(img_path, target_r, target_g, target_b, base_grey=144):
    img = Image.open(img_path).convert("RGBA")
    out = Image.new("RGBA", img.size)
    pixels = img.load()
    out_pixels = out.load()
    
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = pixels[x, y]
            if a > 0:
                # Check if it's greyscale (like a grass overlay)
                if abs(r-g) <= 15 and abs(g-b) <= 15:
                    # Apply tint using Minecraft's multiply logic
                    new_r = min(255, int(r * target_r / base_grey))
                    new_g = min(255, int(g * target_g / base_grey))
                    new_b = min(255, int(b * target_b / base_grey))
                    out_pixels[x, y] = (new_r, new_g, new_b, a)
                else:
                    out_pixels[x, y] = (r, g, b, a)
            else:
                out_pixels[x, y] = (0, 0, 0, 0)
    return out

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Tint greyscale areas of a texture to a specific RGB biome color.")
    parser.add_argument("--input", required=True, help="Input texture path")
    parser.add_argument("--out", required=True, help="Output texture path")
    parser.add_argument("--color", required=True, help="Target RGB color in hex (e.g., 99C867)")
    args = parser.parse_args()

    # Parse hex color
    hex_color = args.color.lstrip('#')
    if len(hex_color) != 6:
        raise ValueError("Color must be a 6-character hex code, e.g., 99C867")
    
    target_r = int(hex_color[0:2], 16)
    target_g = int(hex_color[2:4], 16)
    target_b = int(hex_color[4:6], 16)

    tinted = colorize(args.input, target_r, target_g, target_b)
    tinted.save(args.out)
    print(f"Tinted texture successfully saved to: {args.out}")
