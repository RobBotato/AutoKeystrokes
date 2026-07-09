import argparse
from PIL import Image, ImageDraw, ImageEnhance

def pad_image(img):
    """Pads the image by 1 pixel repeating the edges, used to eliminate anti-aliasing seams."""
    w, h = img.size
    pad = 1
    new_img = Image.new(img.mode, (w + 2*pad, h + 2*pad))
    new_img.paste(img, (pad, pad))
    new_img.paste(img.crop((0, 0, w, 1)), (pad, 0))
    new_img.paste(img.crop((0, h-1, w, h)), (pad, h+pad))
    new_img.paste(img.crop((0, 0, 1, h)), (0, pad))
    new_img.paste(img.crop((w-1, 0, w, h)), (w+pad, pad))
    new_img.paste(img.crop((0, 0, 1, 1)), (0, 0))
    new_img.paste(img.crop((w-1, 0, w, 1)), (w+pad, 0))
    new_img.paste(img.crop((0, h-1, 1, h)), (0, h+pad))
    new_img.paste(img.crop((w-1, h-1, w, h)), (w+pad, h+pad))
    return new_img

def create_block(top_tex_path, side_tex_path, bottom_tex_path, out_path):
    # Exact dimensions from a true 30-degree orthographic projection isometric block
    Xc = 249
    Yc = 284
    Wx = 247
    Wy = 141
    Hside = 287

    # Load and pad textures to prevent seams, then scale up heavily to preserve crisp pixel look
    S = 1024
    scale_factor = S // 16
    pad_px = 1 * scale_factor

    top_tex_raw = Image.open(top_tex_path).convert("RGBA")
    side_tex_raw = Image.open(side_tex_path).convert("RGBA")

    top_tex = pad_image(top_tex_raw).resize((S + 2*pad_px, S + 2*pad_px), Image.NEAREST)
    side_tex = pad_image(side_tex_raw).resize((S + 2*pad_px, S + 2*pad_px), Image.NEAREST)

    out_w, out_h = 499, 574
    out_img = Image.new("RGBA", (out_w, out_h), (0,0,0,0))

    def get_face(tex, a, b, c, d, e, f, polygon, brightness):
        # Shift mapping by pad_px because texture origin moved due to padding
        c_new = c - a*Xc - b*Yc + pad_px
        f_new = f - d*Xc - e*Yc + pad_px
        
        face = tex.transform((out_w, out_h), Image.AFFINE, (a, b, c_new, d, e, f_new), resample=Image.BICUBIC)
        
        if brightness < 1.0:
            enhancer = ImageEnhance.Brightness(face)
            face = enhancer.enhance(brightness)
            
        mask = Image.new("L", (out_w, out_h), 0)
        draw = ImageDraw.Draw(mask)
        poly_abs = [(x + Xc, y + Yc) for x, y in polygon]
        draw.polygon(poly_abs, fill=255)
        
        face.putalpha(mask)
        return face

    # Right Face
    a = S / Wx; b = 0; c_val = 0
    d = (Wy * S) / (Wx * Hside); e = S / Hside; f_val = 0
    poly_right_mask = [(0, 0), (Wx+2, -Wy-2), (Wx+2, -Wy + Hside + 2), (0, Hside+2)]
    right_face = get_face(side_tex, a, b, c_val, d, e, f_val, poly_right_mask, 0.6)

    # Left Face
    a = S / Wx; b = 0; c_val = S
    d = -(Wy * S) / (Wx * Hside); e = S / Hside; f_val = 0
    poly_left_mask = [(0, 0), (-Wx-2, -Wy-2), (-Wx-2, -Wy + Hside + 2), (0, Hside+2)]
    left_face = get_face(side_tex, a, b, c_val, d, e, f_val, poly_left_mask, 0.8)

    # Top Face
    a = -S / (2 * Wx); b = S / (2 * Wy); c_val = S
    d = S / (2 * Wx); e = S / (2 * Wy); f_val = S
    poly_top_mask = [(0, 2), (Wx+2, -Wy+1), (0, -2*Wy-2), (-Wx-2, -Wy+1)]
    top_face = get_face(top_tex, a, b, c_val, d, e, f_val, poly_top_mask, 1.0)

    # Paste faces back to front
    out_img.paste(left_face, (0, 0), left_face)
    out_img.paste(right_face, (0, 0), right_face)
    out_img.paste(top_face, (0, 0), top_face)

    out_img.save(out_path)
    print(f"Isometric block successfully generated: {out_path}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Generate a high-res isometric block from 2D textures.")
    parser.add_argument("--top", required=True, help="Path to top texture (e.g., grass_block_top.png)")
    parser.add_argument("--side", required=True, help="Path to side texture (e.g., grass_block_side.png)")
    parser.add_argument("--bottom", default="", help="Path to bottom texture (Optional, not currently visible in this projection)")
    parser.add_argument("--out", required=True, help="Output path (e.g., GRASS_BLOCK.png)")
    args = parser.parse_args()

    create_block(args.top, args.side, args.bottom, args.out)
