"""
Prozorro Radar - Play Store Asset Generator (Vector-Icon Edition)
Generates pixel-perfect Google Play Store marketing assets with native vector icons:
1. icon_512x512.png (512x512, 32-bit opaque PNG)
2. feature_graphic_1024x500.png (1024x500 PNG, no tofu square glyphs)
3. screenshot_1_dashboard.png (1080x1920 PNG)
4. screenshot_2_wizard.png (1080x1920 PNG)
5. screenshot_3_analytics.png (1080x1920 PNG)
6. screenshot_4_archive.png (1080x1920 PNG)
"""

import os
import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter

OUTPUT_DIR = r"c:\WORK\ProzoroAndroid\play_store_assets"
os.makedirs(OUTPUT_DIR, exist_ok=True)

# Color Palette (matching Color.kt & Material 3 Dark theme)
DARK_BG_TOP = (7, 11, 18)
DARK_BG_BOTTOM = (19, 30, 51)
SLATE_BG = (11, 17, 26)
SLATE_SURFACE = (19, 28, 40)
SLATE_SURFACE_VARIANT = (27, 38, 54)
SLATE_BORDER = (38, 53, 74)
SLATE_TEXT_PRIMARY = (241, 245, 249)
SLATE_TEXT_SECONDARY = (148, 163, 184)
SLATE_TEXT_MUTED = (100, 116, 139)

EMERALD_PRIMARY = (0, 200, 83)
EMERALD_LIGHT = (0, 230, 118)
EMERALD_DARK = (0, 150, 36)
EMERALD_CONTAINER = (10, 61, 34)
ON_EMERALD_CONTAINER = (167, 243, 208)

STATUS_ACTIVE = (16, 185, 129)
STATUS_WARNING = (245, 158, 11)
STATUS_URGENT = (239, 68, 68)
STATUS_INFO = (56, 189, 248)
STATUS_COMPLETE = (139, 92, 246)
GOLD = (251, 191, 36)
WHITE = (255, 255, 255)
BLACK = (0, 0, 0)

# Fonts
FONT_BOLD_PATH = "C:/Windows/Fonts/segoeuib.ttf"
FONT_REG_PATH = "C:/Windows/Fonts/segoeui.ttf"
FONT_SEMI_PATH = "C:/Windows/Fonts/segoeuisl.ttf"

def get_font(size, bold=False, light=False):
    path = FONT_BOLD_PATH if bold else (FONT_SEMI_PATH if light else FONT_REG_PATH)
    try:
        return ImageFont.truetype(path, int(size))
    except Exception:
        return ImageFont.truetype("C:/Windows/Fonts/arial.ttf", int(size))

def draw_vertical_gradient(img, start_color, end_color):
    draw = ImageDraw.Draw(img)
    w, h = img.size
    for y in range(h):
        factor = y / float(h - 1)
        r = int(start_color[0] + (end_color[0] - start_color[0]) * factor)
        g = int(start_color[1] + (end_color[1] - start_color[1]) * factor)
        b = int(start_color[2] + (end_color[2] - start_color[2]) * factor)
        draw.line([(0, y), (w, y)], fill=(r, g, b))

# ==============================================================================
# VECTOR ICON DRAWING ENGINE (Prevents any missing font square glyphs)
# ==============================================================================
def draw_vector_icon(draw, icon_name, x, y, size, color):
    """Renders clean, high-resolution vector glyphs directly with PIL ImageDraw."""
    s = size
    if icon_name == 'radar':
        cx, cy = x + s // 2, y + s // 2
        draw.ellipse([cx - s//2, cy - s//2, cx + s//2, cy + s//2], outline=color, width=2)
        draw.ellipse([cx - s//4, cy - s//4, cx + s//4, cy + s//4], outline=color, width=1)
        draw.ellipse([cx - 2, cy - 2, cx + 2, cy + 2], fill=color)
        draw.line([(cx - s//2, cy), (cx + s//2, cy)], fill=color, width=1)
        draw.line([(cx, cy - s//2), (cx, cy + s//2)], fill=color, width=1)

    elif icon_name == 'chart':
        bw = max(2, s // 4)
        draw.rounded_rectangle([x, y + s * 0.55, x + bw, y + s], radius=1, fill=color)
        draw.rounded_rectangle([x + bw + 2, y + s * 0.2, x + 2*bw + 2, y + s], radius=1, fill=color)
        draw.rounded_rectangle([x + 2*bw + 4, y + s * 0.38, x + 3*bw + 4, y + s], radius=1, fill=color)

    elif icon_name == 'mail':
        draw.rounded_rectangle([x, y + 2, x + s, y + s - 2], radius=2, outline=color, width=2)
        draw.line([(x + 2, y + 4), (x + s // 2, y + s // 2 + 1), (x + s - 2, y + 4)], fill=color, width=2)

    elif icon_name == 'shield':
        draw.polygon([(x + s//2, y), (x + s, y + s//4), (x + s, y + s*5//8),
                      (x + s//2, y + s), (x, y + s*5//8), (x, y + s//4)], outline=color, width=2)
        draw.line([(x + s*0.32, y + s*0.52), (x + s*0.48, y + s*0.68), (x + s*0.72, y + s*0.35)],
                  fill=color, width=2)

    elif icon_name == 'search':
        r = int(s * 0.35)
        cx, cy = x + r + 1, y + r + 1
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=color, width=2)
        draw.line([(cx + int(r * 0.7), cy + int(r * 0.7)), (x + s - 1, y + s - 1)], fill=color, width=2)

    elif icon_name == 'refresh':
        cx, cy = x + s // 2, y + s // 2
        r = s // 2 - 2
        draw.arc([cx - r, cy - r, cx + r, cy + r], start=25, end=290, fill=color, width=2)
        draw.polygon([(cx + r - 3, cy - 2), (cx + r + 4, cy - 2), (cx + r, cy + 4)], fill=color)

    elif icon_name == 'folder':
        draw.polygon([(x, y + 3), (x + s*0.4, y + 3), (x + s*0.52, y + 6),
                      (x + s, y + 6), (x + s, y + s), (x, y + s)], outline=color, width=2)

    elif icon_name == 'gear':
        cx, cy = x + s // 2, y + s // 2
        draw.ellipse([cx - s//3, cy - s//3, cx + s//3, cy + s//3], outline=color, width=2)
        draw.ellipse([cx - 2, cy - 2, cx + 2, cy + 2], fill=color)
        for ang in range(0, 360, 45):
            rad = math.radians(ang)
            x1 = cx + int((s//3) * math.cos(rad))
            y1 = cy + int((s//3) * math.sin(rad))
            x2 = cx + int((s//2) * math.cos(rad))
            y2 = cy + int((s//2) * math.sin(rad))
            draw.line([(x1, y1), (x2, y2)], fill=color, width=2)

    elif icon_name == 'grid':
        gw = s // 2 - 2
        draw.rounded_rectangle([x, y, x + gw, y + gw], radius=1, fill=color)
        draw.rounded_rectangle([x + gw + 3, y, x + s, y + gw], radius=1, fill=color)
        draw.rounded_rectangle([x, y + gw + 3, x + gw, y + s], radius=1, fill=color)
        draw.rounded_rectangle([x + gw + 3, y + gw + 3, x + s, y + s], radius=1, fill=color)

    elif icon_name == 'pulse_dot':
        cx, cy = x + s // 2, y + s // 2
        r_outer = s // 2
        draw.ellipse([cx - r_outer, cy - r_outer, cx + r_outer, cy + r_outer],
                     outline=color, width=1)
        r_inner = max(2, s // 3)
        draw.ellipse([cx - r_inner, cy - r_inner, cx + r_inner, cy + r_inner], fill=color)

    elif icon_name == 'building':
        draw.rectangle([x + 2, y + 2, x + s - 2, y + s], outline=color, width=1)
        for wx in [x + 4, x + s - 7]:
            for wy in [y + 4, y + 8, y + 12]:
                draw.rectangle([wx, wy, wx + 2, wy + 2], fill=color)

    elif icon_name == 'star':
        cx, cy = x + s // 2, y + s // 2
        r_out = s // 2
        r_in = r_out * 0.42
        pts = []
        for i in range(10):
            r = r_out if i % 2 == 0 else r_in
            ang = math.radians(i * 36 - 90)
            pts.append((cx + r * math.cos(ang), cy + r * math.sin(ang)))
        draw.polygon(pts, fill=color)

    elif icon_name == 'play':
        draw.polygon([(x + 2, y + 1), (x + s, y + s // 2), (x + 2, y + s - 1)], fill=color)

    elif icon_name == 'arrow_right':
        cy = y + s // 2
        draw.line([(x + 2, cy), (x + s - 2, cy)], fill=color, width=2)
        draw.line([(x + s - 6, cy - 4), (x + s - 2, cy), (x + s - 6, cy + 4)], fill=color, width=2)

    elif icon_name == 'arrow_down':
        cx = x + s // 2
        draw.line([(cx, y + 2), (cx, y + s - 2)], fill=color, width=3)
        draw.line([(cx - 5, y + s - 7), (cx, y + s - 2), (cx + 5, y + s - 7)], fill=color, width=3)


def draw_pill(draw, x, y, text, font, bg_color, text_color, padding_x=14, padding_y=6,
              outline=None, icon=None, icon_color=None, icon_size=14):
    bbox = font.getbbox(text)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    extra_icon_w = (icon_size + 8) if icon else 0
    w = tw + padding_x * 2 + extra_icon_w
    h = th + padding_y * 2
    radius = h // 2
    draw.rounded_rectangle([x, y, x + w, y + h], radius=radius, fill=bg_color, outline=outline, width=1 if outline else 0)

    cur_x = x + padding_x
    if icon:
        draw_vector_icon(draw, icon, cur_x, y + (h - icon_size) // 2, icon_size, icon_color or text_color)
        cur_x += extra_icon_w

    draw.text((cur_x - bbox[0], y + padding_y - bbox[1]), text, font=font, fill=text_color)
    return w, h


# ==============================================================================
# 1. APP ICON (512 x 512 px)
# ==============================================================================
def generate_icon():
    print("Generating icon_512x512.png...")
    size = (512, 512)
    img = Image.new("RGBA", size, DARK_BG_TOP)
    draw_vertical_gradient(img, (7, 11, 20), (14, 25, 45))

    # Ambient background glow in center
    glow = Image.new("RGBA", size, (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    for r in range(250, 40, -10):
        alpha = int(35 * (1.0 - r / 250.0))
        gdraw.ellipse([256 - r, 256 - r, 256 + r, 256 + r], fill=(0, 200, 83, alpha))
    img = Image.alpha_composite(img, glow)

    # Inset subtle outer border
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle([6, 6, 505, 505], radius=90, outline=(38, 53, 74), width=3)
    draw.rounded_rectangle([9, 9, 502, 502], radius=88, outline=(0, 200, 83, 40), width=1)

    # Concentric Radar Rings
    cx, cy = 256, 256
    rings = [
        (210, (0, 200, 83, 60), 2),
        (160, (0, 200, 83, 90), 2),
        (110, (0, 200, 83, 130), 2),
        (60,  (0, 230, 118, 180), 2),
        (20,  (0, 230, 118, 220), 2),
    ]

    ring_layer = Image.new("RGBA", size, (0, 0, 0, 0))
    rdraw = ImageDraw.Draw(ring_layer)

    for radius, color, width in rings:
        rdraw.ellipse([cx - radius, cy - radius, cx + radius, cy + radius], outline=color, width=width)

    # Crosshairs with tick marks
    rdraw.line([(cx - 215, cy), (cx + 215, cy)], fill=(0, 200, 83, 75), width=2)
    rdraw.line([(cx, cy - 215), (cx, cy + 215)], fill=(0, 200, 83, 75), width=2)

    # Diagonal guidelines
    d_len = 150
    for ang in [45, 135, 225, 315]:
        rad = math.radians(ang)
        x1 = cx + int(40 * math.cos(rad))
        y1 = cy + int(40 * math.sin(rad))
        x2 = cx + int(d_len * math.cos(rad))
        y2 = cy + int(d_len * math.sin(rad))
        rdraw.line([(x1, y1), (x2, y2)], fill=(0, 200, 83, 45), width=1)

    # Tick marks on horizontal and vertical axes
    for r in range(40, 220, 25):
        rdraw.line([(cx - 4, cy - r), (cx + 4, cy - r)], fill=(0, 230, 118, 120), width=1)
        rdraw.line([(cx - 4, cy + r), (cx + 4, cy + r)], fill=(0, 230, 118, 120), width=1)
        rdraw.line([(cx - r, cy - 4), (cx - r, cy + 4)], fill=(0, 230, 118, 120), width=1)
        rdraw.line([(cx + r, cy - 4), (cx + r, cy + 4)], fill=(0, 230, 118, 120), width=1)

    img = Image.alpha_composite(img, ring_layer)

    # Radar Sweep Sector
    sweep_layer = Image.new("RGBA", size, (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(sweep_layer)
    sweep_radius = 210
    start_deg = -35
    end_deg = 35
    total_steps = 70
    for i in range(total_steps):
        deg = start_deg + (end_deg - start_deg) * (i / float(total_steps))
        rad = math.radians(deg)
        x_edge = cx + sweep_radius * math.cos(rad)
        y_edge = cy + sweep_radius * math.sin(rad)
        intensity = (i / float(total_steps)) ** 1.8
        alpha = int(140 * intensity)
        sdraw.line([(cx, cy), (x_edge, y_edge)], fill=(0, 230, 118, alpha), width=3)
    lead_rad = math.radians(end_deg)
    lead_x = cx + sweep_radius * math.cos(lead_rad)
    lead_y = cy + sweep_radius * math.sin(lead_rad)
    sdraw.line([(cx, cy), (lead_x, lead_y)], fill=(255, 255, 255, 220), width=2)
    img = Image.alpha_composite(img, sweep_layer)

    # Glowing Radar Blips
    blip_layer = Image.new("RGBA", size, (0, 0, 0, 0))
    bdraw = ImageDraw.Draw(blip_layer)

    blips = [
        (330, 190, EMERALD_LIGHT, 7),
        (200, 310, STATUS_WARNING, 6),
        (360, 320, STATUS_INFO, 5),
        (170, 170, EMERALD_PRIMARY, 5),
        (290, 280, STATUS_ACTIVE, 4),
    ]
    for bx, by, col, rad in blips:
        bdraw.ellipse([bx - rad * 2.5, by - rad * 2.5, bx + rad * 2.5, by + rad * 2.5],
                      outline=(col[0], col[1], col[2], 90), width=1)
        bdraw.ellipse([bx - rad, by - rad, bx + rad, by + rad], fill=col)
        bdraw.ellipse([bx - rad // 2, by - rad // 2, bx + rad // 2, by + rad // 2], fill=WHITE)

    img = Image.alpha_composite(img, blip_layer)

    # Stylized Center Symbol
    center_layer = Image.new("RGBA", size, (0, 0, 0, 0))
    cdraw = ImageDraw.Draw(center_layer)
    cdraw.ellipse([cx - 24, cy - 24, cx + 24, cy + 24], fill=(10, 61, 34, 230), outline=EMERALD_LIGHT, width=2)
    cdraw.ellipse([cx - 10, cy - 10, cx + 10, cy + 10], fill=EMERALD_LIGHT)
    cdraw.ellipse([cx - 4, cy - 4, cx + 4, cy + 4], fill=WHITE)

    for r in [38, 48]:
        cdraw.arc([cx - r, cy - r, cx + r, cy + r], start=210, end=330, fill=EMERALD_LIGHT, width=2)
        cdraw.arc([cx - r, cy - r, cx + r, cy + r], start=30, end=150, fill=EMERALD_LIGHT, width=2)

    img = Image.alpha_composite(img, center_layer)

    # Bottom Branding Pill: "PROZORRO RADAR"
    brand_layer = Image.new("RGBA", size, (0, 0, 0, 0))
    br_draw = ImageDraw.Draw(brand_layer)
    font_brand = get_font(20, bold=True)
    brand_text = "PROZORRO RADAR"
    bbox = font_brand.getbbox(brand_text)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    px = (512 - tw) // 2
    py = 430
    br_draw.rounded_rectangle([px - 18, py - 6, px + tw + 18, py + th + 8], radius=14,
                             fill=(19, 28, 40, 220), outline=(0, 200, 83, 180), width=1)
    br_draw.text((px - bbox[0], py - bbox[1]), brand_text, font=font_brand, fill=WHITE)
    img = Image.alpha_composite(img, brand_layer)

    final_img = img.convert("RGBA")
    final_path = os.path.join(OUTPUT_DIR, "icon_512x512.png")
    final_img.save(final_path, "PNG")
    print(f"Saved: {final_path}")


# ==============================================================================
# 2. FEATURE GRAPHIC (1024 x 500 px) - FIXED VECTOR ICONS (NO SQUARES)
# ==============================================================================
def generate_feature_graphic():
    print("Generating feature_graphic_1024x500.png (Clean Vector Icons)...")
    size = (1024, 500)
    img = Image.new("RGBA", size, DARK_BG_TOP)
    draw_vertical_gradient(img, (9, 14, 24), (19, 30, 51))

    # Tech grid background
    grid_layer = Image.new("RGBA", size, (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(grid_layer)
    for x in range(0, 1024, 40):
        gdraw.line([(x, 0), (x, 500)], fill=(38, 53, 74, 30), width=1)
    for y in range(0, 500, 40):
        gdraw.line([(0, y), (1024, y)], fill=(38, 53, 74, 30), width=1)
    img = Image.alpha_composite(img, grid_layer)

    # Ambient Right Glow (Radar center)
    radar_cx, radar_cy = 780, 250
    glow_layer = Image.new("RGBA", size, (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow_layer)
    for r in range(260, 30, -10):
        alpha = int(45 * (1.0 - r / 260.0))
        glow_draw.ellipse([radar_cx - r, radar_cy - r, radar_cx + r, radar_cy + r],
                          fill=(0, 200, 83, alpha))
    img = Image.alpha_composite(img, glow_layer)

    # Right Side Radar HUD
    hud_layer = Image.new("RGBA", size, (0, 0, 0, 0))
    hdraw = ImageDraw.Draw(hud_layer)

    # Radar rings
    for radius, alpha in [(210, 50), (160, 75), (110, 110), (60, 150)]:
        hdraw.ellipse([radar_cx - radius, radar_cy - radius, radar_cx + radius, radar_cy + radius],
                      outline=(0, 200, 83, alpha), width=2)
    # Crosshairs
    hdraw.line([(radar_cx - 215, radar_cy), (radar_cx + 215, radar_cy)], fill=(0, 200, 83, 60), width=1)
    hdraw.line([(radar_cx, radar_cy - 215), (radar_cx, radar_cy + 215)], fill=(0, 200, 83, 60), width=1)

    # Radar Sweep Sector
    sweep_rad = 210
    for i in range(50):
        deg = -20 + i * 1.2
        rad = math.radians(deg)
        x_e = radar_cx + sweep_rad * math.cos(rad)
        y_e = radar_cy + sweep_rad * math.sin(rad)
        alpha = int(120 * (i / 50.0) ** 1.8)
        hdraw.line([(radar_cx, radar_cy), (x_e, y_e)], fill=(0, 230, 118, alpha), width=2)

    # Radar Blips with tags
    blips = [
        (radar_cx + 110, radar_cy - 70, STATUS_ACTIVE, "14.5M ₴ • Будівництво"),
        (radar_cx - 90, radar_cy + 80, STATUS_WARNING, "2.8M ₴ • Сервери"),
        (radar_cx + 60, radar_cy + 110, STATUS_INFO, "850K ₴ • Медицина"),
    ]
    font_mini = get_font(12, bold=True)
    for bx, by, col, label in blips:
        hdraw.ellipse([bx - 12, by - 12, bx + 12, by + 12], outline=(col[0], col[1], col[2], 90), width=1)
        hdraw.ellipse([bx - 6, by - 6, bx + 6, by + 6], fill=col)
        hdraw.ellipse([bx - 2, by - 2, bx + 2, by + 2], fill=WHITE)

        bbox = font_mini.getbbox(label)
        lw = bbox[2] - bbox[0] + 12
        lh = bbox[3] - bbox[1] + 8
        hdraw.rounded_rectangle([bx + 12, by - lh // 2, bx + 12 + lw, by + lh // 2], radius=6,
                                fill=(19, 28, 40, 220), outline=(38, 53, 74), width=1)
        hdraw.text((bx + 18 - bbox[0], by - (bbox[3] - bbox[1]) // 2 - bbox[1]), label, font=font_mini, fill=WHITE)

    # Central Radar Beacon
    hdraw.ellipse([radar_cx - 16, radar_cy - 16, radar_cx + 16, radar_cy + 16],
                  fill=(10, 61, 34, 240), outline=EMERALD_LIGHT, width=2)
    hdraw.ellipse([radar_cx - 6, radar_cy - 6, radar_cx + 6, radar_cy + 6], fill=EMERALD_LIGHT)

    # Floating KPI cards
    kpi_w, kpi_h = 190, 62
    k1_x, k1_y = 570, 70
    hdraw.rounded_rectangle([k1_x, k1_y, k1_x + kpi_w, k1_y + kpi_h], radius=12,
                            fill=(19, 28, 40, 230), outline=SLATE_BORDER, width=1)
    font_kpi_lbl = get_font(12, light=True)
    font_kpi_val = get_font(20, bold=True)
    hdraw.text((k1_x + 16, k1_y + 10), "Всього в базі:", font=font_kpi_lbl, fill=SLATE_TEXT_SECONDARY)
    hdraw.text((k1_x + 16, k1_y + 28), "1 428 тендерів", font=font_kpi_val, fill=WHITE)

    k2_x, k2_y = 590, 360
    hdraw.rounded_rectangle([k2_x, k2_y, k2_x + 220, k2_y + kpi_h], radius=12,
                            fill=(19, 28, 40, 230), outline=(0, 200, 83, 160), width=1)
    hdraw.text((k2_x + 16, k2_y + 10), "Нових за 24 години:", font=font_kpi_lbl, fill=SLATE_TEXT_SECONDARY)
    hdraw.text((k2_x + 16, k2_y + 28), "+34 активних торгів", font=font_kpi_val, fill=EMERALD_LIGHT)

    img = Image.alpha_composite(img, hud_layer)

    # Left Side: Typography & Badges
    draw = ImageDraw.Draw(img)

    # Top Tag: Pulsing Dot + Text (NO EMOJI)
    font_tag = get_font(14, bold=True)
    draw_pill(draw, 60, 50, "ВІДКРИТІ ТОРГИ В РЕАЛЬНОМУ ЧАСІ", font_tag,
              EMERALD_CONTAINER, ON_EMERALD_CONTAINER, padding_x=14, padding_y=6,
              outline=(0, 200, 83, 160), icon='pulse_dot', icon_color=EMERALD_LIGHT, icon_size=12)

    # App Title
    font_title = get_font(52, bold=True)
    draw.text((60, 95), "Prozorro ", font=font_title, fill=WHITE)
    bbox_p = font_title.getbbox("Prozorro ")
    pw = bbox_p[2] - bbox_p[0]
    draw.text((60 + pw, 95), "Радар", font=font_title, fill=EMERALD_LIGHT)

    # Subtitle
    font_sub = get_font(21, light=True)
    draw.text((60, 168), "Автономний моніторинг тендерів та держзакупівель", font=font_sub, fill=SLATE_TEXT_SECONDARY)

    # 4 Feature Badges with DEDICATED VECTOR ICONS (No Unicode emojis)
    font_badge = get_font(15, bold=True)
    font_desc = get_font(13)

    badges = [
        ("Live Radar", "Автосканування Prozorro 24/7", 60, 225, 'radar', EMERALD_LIGHT),
        ("Excel Експорт", "Готові звіти та таблиці .xlsx", 300, 225, 'chart', STATUS_INFO),
        ("Email Дайджест", "Сповіщення про нові торги", 60, 310, 'mail', STATUS_WARNING),
        ("100% Автономно", "Локальна база SQLite на пристрої", 300, 310, 'shield', STATUS_COMPLETE),
    ]

    for title, desc, bx, by, icon_nm, icon_col in badges:
        bw, bh = 225, 68
        draw.rounded_rectangle([bx, by, bx + bw, by + bh], radius=10,
                               fill=SLATE_SURFACE, outline=SLATE_BORDER, width=1)
        # Vector icon
        draw_vector_icon(draw, icon_nm, bx + 14, by + 13, 18, icon_col)
        # Title beside icon
        draw.text((bx + 38, by + 12), title, font=font_badge, fill=WHITE)
        # Description below
        draw.text((bx + 14, by + 38), desc, font=font_desc, fill=SLATE_TEXT_MUTED)

    # Trust / Disclaimer footer
    font_footer = get_font(13)
    draw.text((60, 420), "• Відкриті дані OpenProcurement • Пошук за кодами ДК 021 (CPV)",
              font=font_footer, fill=(100, 160, 220))
    draw.text((60, 442), "• Повна конфіденційність: дані не передаються стороннім серверам",
              font=font_footer, fill=SLATE_TEXT_MUTED)

    final_img = img.convert("RGB")
    final_path = os.path.join(OUTPUT_DIR, "feature_graphic_1024x500.png")
    final_img.save(final_path, "PNG")
    print(f"Saved: {final_path} (size: {final_img.size})")


# ==============================================================================
# SCREENSHOT HELPERS (Phone Frame & Top Header)
# ==============================================================================
PHONE_X = 70
PHONE_Y = 320
PHONE_W = 940
PHONE_H = 1540
PHONE_R = 48

def create_base_screenshot_canvas(headline_pill, headline_main, headline_sub, pill_icon='pulse_dot'):
    size = (1080, 1920)
    img = Image.new("RGBA", size, DARK_BG_TOP)
    draw_vertical_gradient(img, (9, 13, 22), (18, 28, 48))

    # Tech grid pattern
    grid = Image.new("RGBA", size, (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(grid)
    for x in range(0, 1080, 54):
        gdraw.line([(x, 0), (x, 1920)], fill=(38, 53, 74, 25), width=1)
    for y in range(0, 1920, 54):
        gdraw.line([(0, y), (1080, y)], fill=(38, 53, 74, 25), width=1)
    img = Image.alpha_composite(img, grid)

    draw = ImageDraw.Draw(img)

    # Marketing Header Top
    font_pill = get_font(18, bold=True)
    draw_pill(draw, 70, 50, headline_pill, font_pill,
              EMERALD_CONTAINER, ON_EMERALD_CONTAINER, padding_x=16, padding_y=8,
              outline=(0, 200, 83, 180), icon=pill_icon, icon_color=EMERALD_LIGHT, icon_size=16)

    font_h1 = get_font(46, bold=True)
    draw.text((70, 115), headline_main, font=font_h1, fill=WHITE)

    font_sub = get_font(24)
    draw.text((70, 185), headline_sub, font=font_sub, fill=SLATE_TEXT_SECONDARY)

    # Phone Shadow
    shadow = Image.new("RGBA", size, (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(shadow)
    sdraw.rounded_rectangle([PHONE_X - 6, PHONE_Y + 12, PHONE_X + PHONE_W + 6, PHONE_Y + PHONE_H + 24],
                            radius=PHONE_R + 6, fill=(0, 0, 0, 140))
    shadow = shadow.filter(ImageFilter.GaussianBlur(16))
    img = Image.alpha_composite(img, shadow)

    # Phone Frame Outer Border
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle([PHONE_X, PHONE_Y, PHONE_X + PHONE_W, PHONE_Y + PHONE_H],
                           radius=PHONE_R, fill=SLATE_BG, outline=(50, 70, 95), width=5)

    # Phone Screen Glass Inset
    scr_inset = 8
    draw.rounded_rectangle([PHONE_X + scr_inset, PHONE_Y + scr_inset,
                            PHONE_X + PHONE_W - scr_inset, PHONE_Y + PHONE_H - scr_inset],
                           radius=PHONE_R - 4, fill=SLATE_BG)

    # Android Status Bar
    draw_android_status_bar(draw, PHONE_X + 24, PHONE_Y + 16, PHONE_W - 48)

    # Android App Top Bar
    draw_app_top_bar(draw, PHONE_X + 24, PHONE_Y + 60, PHONE_W - 48)

    # Android Navigation Bar
    draw_android_bottom_nav(draw, PHONE_X + 24, PHONE_Y + PHONE_H - 120, PHONE_W - 48)

    return img

def draw_android_status_bar(draw, x, y, width):
    font_time = get_font(18, bold=True)
    draw.text((x + 16, y + 4), "12:00", font=font_time, fill=WHITE)

    cx = x + width // 2
    draw.ellipse([cx - 10, y + 4, cx + 10, y + 24], fill=(5, 8, 14), outline=(30, 40, 55), width=1)

    rx = x + width - 130
    font_stat = get_font(16, bold=True)
    draw.text((rx, y + 4), "5G  Wi-Fi  100%", font=font_stat, fill=SLATE_TEXT_SECONDARY)

def draw_app_top_bar(draw, x, y, width):
    font_top = get_font(26, bold=True)
    # App logo dot
    draw.ellipse([x + 16, y + 10, x + 38, y + 32], fill=EMERALD_PRIMARY)
    draw.text((x + 48, y + 6), "Prozorro Радар", font=font_top, fill=WHITE)

    # Action button on right: Vector Refresh Icon + text "Оновити"
    font_actions = get_font(16, bold=True)
    btn_w = 120
    bx = x + width - btn_w - 16
    by = y + 4
    draw.rounded_rectangle([bx, by, bx + btn_w, by + 36], radius=8,
                           fill=SLATE_SURFACE, outline=SLATE_BORDER, width=1)
    draw_vector_icon(draw, 'refresh', bx + 12, by + 10, 16, EMERALD_LIGHT)
    draw.text((bx + 36, by + 8), "Оновити", font=font_actions, fill=EMERALD_LIGHT)
    draw.line([(x, y + 54), (x + width, y + 54)], fill=SLATE_BORDER, width=1)

def draw_android_bottom_nav(draw, x, y, width, active_index=0):
    draw.line([(x, y), (x + width, y)], fill=SLATE_BORDER, width=1)
    tabs = [
        ("grid", "Дашборд"),
        ("folder", "Архів"),
        ("chart", "Звіти"),
        ("gear", "Налаштування")
    ]
    tab_w = width // len(tabs)
    font_lbl = get_font(14, bold=True)

    for i, (ico_nm, name) in enumerate(tabs):
        tx = x + i * tab_w
        is_active = (i == active_index)
        col = EMERALD_LIGHT if is_active else SLATE_TEXT_MUTED
        # Vector tab icon
        draw_vector_icon(draw, ico_nm, tx + tab_w // 2 - 11, y + 10, 22, col)
        # Tab label
        bbox = font_lbl.getbbox(name)
        lw = bbox[2] - bbox[0]
        draw.text((tx + (tab_w - lw) // 2, y + 38), name, font=font_lbl, fill=col)
        if is_active:
            draw.line([(tx + tab_w // 4, y + 62), (tx + tab_w * 3 // 4, y + 62)], fill=EMERALD_LIGHT, width=3)


# ==============================================================================
# 3. SCREENSHOT 1: DASHBOARD (1080 x 1920 px)
# ==============================================================================
def generate_screenshot_1():
    print("Generating screenshot_1_dashboard.png (Vector Edition)...")
    img = create_base_screenshot_canvas(
        headline_pill="АВТОНОМНИЙ РАДАР",
        headline_main="МОНІТОРИНГ У РЕАЛЬНОМУ ЧАСІ",
        headline_sub="Автоматичне сканування тендерів та оновлення бази 24/7",
        pill_icon='pulse_dot'
    )
    draw = ImageDraw.Draw(img)

    content_x = PHONE_X + 44
    content_y = PHONE_Y + 130
    content_w = PHONE_W - 88

    # 4 Metric Cards (2x2 Grid)
    metrics = [
        ("Всього в базі", "1 428", "за весь час", STATUS_INFO),
        ("Нових за 24г", "+34", "останнє оновлення", STATUS_ACTIVE),
        ("Бюджет пулу", "842.5 млн ₴", "активні пропозиції", STATUS_WARNING),
        ("Термінові (<48г)", "12 торгів", "потребують уваги", STATUS_URGENT)
    ]
    card_w = (content_w - 18) // 2
    card_h = 100

    font_m_lbl = get_font(15)
    font_m_val = get_font(25, bold=True)
    font_m_sub = get_font(13)

    for idx, (lbl, val, sub, col) in enumerate(metrics):
        col_idx = idx % 2
        row_idx = idx // 2
        cx = content_x + col_idx * (card_w + 18)
        cy = content_y + row_idx * (card_h + 14)

        draw.rounded_rectangle([cx, cy, cx + card_w, cy + card_h], radius=14,
                               fill=SLATE_SURFACE, outline=SLATE_BORDER, width=1)
        draw.rounded_rectangle([cx, cy, cx + 5, cy + card_h], radius=2, fill=col)

        draw.text((cx + 18, cy + 12), lbl, font=font_m_lbl, fill=SLATE_TEXT_SECONDARY)
        draw.text((cx + 18, cy + 34), val, font=font_m_val, fill=WHITE)
        draw.text((cx + 18, cy + 68), sub, font=font_m_sub, fill=SLATE_TEXT_MUTED)

    # Scanner Control Card
    scan_y = content_y + 2 * (card_h + 14) + 10
    scan_h = 160
    draw.rounded_rectangle([content_x, scan_y, content_x + content_w, scan_y + scan_h], radius=16,
                           fill=SLATE_SURFACE_VARIANT, outline=(0, 200, 83, 140), width=1)

    # Pulsing live indicator dot
    draw_vector_icon(draw, 'pulse_dot', content_x + 22, scan_y + 22, 16, EMERALD_LIGHT)
    font_scan_title = get_font(20, bold=True)
    draw.text((content_x + 48, scan_y + 18), "Сканер активний: перевірка Prozorro", font=font_scan_title, fill=WHITE)

    font_scan_sub = get_font(15)
    draw.text((content_x + 22, scan_y + 55), "Фоновий моніторинг кожні 4 год • Наступний запуск о 14:00",
              font=font_scan_sub, fill=SLATE_TEXT_SECONDARY)
    draw.text((content_x + 22, scan_y + 78), "Активні CPV фільтри: 4500 (Будівництво), 3023 (IT), 0913 (Паливо)",
              font=font_scan_sub, fill=ON_EMERALD_CONTAINER)

    # Buttons inside Scanner Card
    btn_w = (content_w - 60) // 2
    btn_y = scan_y + 105
    btn_h = 42

    # Button 1: Scan now (Vector Play Icon)
    draw.rounded_rectangle([content_x + 20, btn_y, content_x + 20 + btn_w, btn_y + btn_h], radius=10,
                           fill=EMERALD_PRIMARY)
    draw_vector_icon(draw, 'play', content_x + 36, btn_y + 14, 14, WHITE)
    font_btn = get_font(16, bold=True)
    draw.text((content_x + 58, btn_y + 11), "Сканувати зараз", font=font_btn, fill=WHITE)

    # Button 2: Export Excel (Vector Chart Icon)
    draw.rounded_rectangle([content_x + 30 + btn_w, btn_y, content_x + 30 + 2 * btn_w, btn_y + btn_h], radius=10,
                           fill=SLATE_SURFACE, outline=SLATE_BORDER, width=1)
    draw_vector_icon(draw, 'chart', content_x + 46 + btn_w, btn_y + 13, 16, EMERALD_LIGHT)
    draw.text((content_x + 70 + btn_w, btn_y + 11), "Експорт в Excel", font=font_btn, fill=SLATE_TEXT_PRIMARY)

    # Section Header
    sec_y = scan_y + scan_h + 20
    font_sec = get_font(20, bold=True)
    draw.text((content_x, sec_y), "Останні відібрані закупівлі", font=font_sec, fill=WHITE)

    font_all = get_font(15, bold=True)
    draw.text((content_x + content_w - 120, sec_y + 3), "Всі (1 428) →", font=font_all, fill=EMERALD_LIGHT)

    # 3 Tender Cards
    tenders = [
        {
            "cpv": "45230000-8 • Будівництво доріг",
            "title": "Капітальний ремонт магістрального шляхопроводу та дорожнього полотна",
            "customer": "КП «Київавтодор» • Київ",
            "price": "14 850 000 ₴",
            "deadline": "Термін: 4 дні (до 24 лист.)",
            "urgent": False,
            "fav": True
        },
        {
            "cpv": "30230000-0 • Комп'ютерне обладнання",
            "title": "Постачання серверів високої щільності, мережевих СЗД та ІБЖ для ЦОД",
            "customer": "ДП «Державний центр кіберзахисту»",
            "price": "3 420 000 ₴",
            "deadline": "Термін: 28 год (до завтра!)",
            "urgent": True,
            "fav": True
        },
        {
            "cpv": "09130000-9 • Нафтопродукти та паливо",
            "title": "Закупівля дизельного пального Євро-5 та бензину А-95 у талонах",
            "customer": "АТ «Укрпошта» • Київська філія",
            "price": "8 200 000 ₴",
            "deadline": "Термін: 7 днів (до 28 лист.)",
            "urgent": False,
            "fav": False
        }
    ]

    card_start_y = sec_y + 36
    t_card_h = 175
    font_cpv = get_font(13, bold=True)
    font_t_title = get_font(17, bold=True)
    font_cust = get_font(14)
    font_price = get_font(21, bold=True)
    font_dead = get_font(14, bold=True)

    for idx, t in enumerate(tenders):
        ty = card_start_y + idx * (t_card_h + 14)
        if ty + t_card_h > PHONE_Y + PHONE_H - 130:
            break

        draw.rounded_rectangle([content_x, ty, content_x + content_w, ty + t_card_h], radius=14,
                               fill=SLATE_SURFACE, outline=SLATE_BORDER, width=1)

        # CPV Badge
        draw_pill(draw, content_x + 16, ty + 12, t["cpv"], font_cpv,
                  (20, 35, 55), (140, 190, 240), padding_x=10, padding_y=4)

        # Favorite star (vector)
        star_col = GOLD if t["fav"] else SLATE_TEXT_MUTED
        draw_vector_icon(draw, 'star', content_x + content_w - 38, ty + 12, 18, star_col)

        # Title
        draw.text((content_x + 16, ty + 42), t["title"], font=font_t_title, fill=WHITE)

        # Customer with vector building icon
        draw_vector_icon(draw, 'building', content_x + 16, ty + 74, 14, SLATE_TEXT_SECONDARY)
        draw.text((content_x + 36, ty + 72), t["customer"], font=font_cust, fill=SLATE_TEXT_SECONDARY)

        # Divider
        draw.line([(content_x + 16, ty + 105), (content_x + content_w - 16, ty + 105)],
                  fill=SLATE_BORDER, width=1)

        # Price & Deadline
        draw.text((content_x + 16, ty + 120), t["price"], font=font_price, fill=EMERALD_LIGHT)
        dead_col = STATUS_URGENT if t["urgent"] else SLATE_TEXT_MUTED
        draw.text((content_x + content_w - 240, ty + 125), t["deadline"], font=font_dead, fill=dead_col)

    final_img = img.convert("RGB")
    final_path = os.path.join(OUTPUT_DIR, "screenshot_1_dashboard.png")
    final_img.save(final_path, "PNG")
    print(f"Saved: {final_path}")


# ==============================================================================
# 4. SCREENSHOT 2: WIZARD (1080 x 1920 px)
# ==============================================================================
def generate_screenshot_2():
    print("Generating screenshot_2_wizard.png (Vector Edition)...")
    img = create_base_screenshot_canvas(
        headline_pill="ЛЕГКИЙ СТАРТ",
        headline_main="ШВИДКИЙ МАЙСТЕР НАЛАШТУВАННЯ",
        headline_sub="Готові галузеві пресети та запуск моніторингу в 1 клік",
        pill_icon='radar'
    )
    draw = ImageDraw.Draw(img)

    content_x = PHONE_X + 36
    content_y = PHONE_Y + 125
    content_w = PHONE_W - 72
    content_h = PHONE_H - 260

    draw.rounded_rectangle([content_x, content_y, content_x + content_w, content_y + content_h], radius=20,
                           fill=SLATE_SURFACE, outline=(0, 200, 83, 140), width=2)

    # Step Progress Indicator
    pw_y = content_y + 24
    font_wiz_title = get_font(23, bold=True)
    draw.text((content_x + 24, pw_y), "Майстер швидкого налаштування", font=font_wiz_title, fill=WHITE)

    font_step = get_font(16, bold=True)
    draw.text((content_x + 24, pw_y + 36), "Крок 1 з 4: Оберіть категорії закупівель (CPV)", font=font_step, fill=EMERALD_LIGHT)

    # 4-Step Progress Bar
    bar_y = pw_y + 68
    bar_w = content_w - 48
    draw.rounded_rectangle([content_x + 24, bar_y, content_x + 24 + bar_w, bar_y + 8], radius=4,
                           fill=SLATE_SURFACE_VARIANT)
    draw.rounded_rectangle([content_x + 24, bar_y, content_x + 24 + bar_w // 4, bar_y + 8], radius=4,
                           fill=EMERALD_PRIMARY)

    # Search Bar inside Wizard (Vector Search Icon)
    srch_y = bar_y + 26
    draw.rounded_rectangle([content_x + 24, srch_y, content_x + 24 + bar_w, srch_y + 50], radius=12,
                           fill=SLATE_SURFACE_VARIANT, outline=SLATE_BORDER, width=1)
    draw_vector_icon(draw, 'search', content_x + 40, srch_y + 16, 18, SLATE_TEXT_MUTED)
    font_srch = get_font(16)
    draw.text((content_x + 68, srch_y + 14), "Пошук за назвою або кодом (напр. 4500, IT...)", font=font_srch, fill=SLATE_TEXT_MUTED)

    # Category Presets Header
    pres_y = srch_y + 68
    font_pres_lbl = get_font(18, bold=True)
    draw.text((content_x + 24, pres_y), "Популярні галузеві напрямки:", font=font_pres_lbl, fill=WHITE)

    presets = [
        ("• Будівництво (4500)", "Споруди, дороги, капремонт", True),
        ("• IT та комп'ютери (3023)", "Сервери, софт, оргтехніка", True),
        ("  Медицина (3360)", "Ліки, медтехніка, препарати", False),
        ("• Паливо та ПММ (0913)", "Бензин, дизель, автогаз", True),
        ("  Одяг та форма (1810)", "Спецодяг, текстиль, захист", False),
        ("  Транспорт (6000)", "Вантажні та пасажирські рейси", False),
        ("  Послуги охорони (7971)", "Охоронні системи, патрулі", False),
        ("  Продукти харчування (1580)", "Крупи, м'ясо, консерви", False),
    ]

    c_w = (bar_w - 16) // 2
    c_h = 75
    font_chip_title = get_font(16, bold=True)
    font_chip_sub = get_font(13)

    for idx, (title, desc, is_sel) in enumerate(presets):
        col_i = idx % 2
        row_i = idx // 2
        chip_x = content_x + 24 + col_i * (c_w + 16)
        chip_y = pres_y + 34 + row_i * (c_h + 12)

        bg_col = EMERALD_CONTAINER if is_sel else SLATE_SURFACE_VARIANT
        border_col = EMERALD_PRIMARY if is_sel else SLATE_BORDER
        text_col = WHITE if is_sel else SLATE_TEXT_PRIMARY

        draw.rounded_rectangle([chip_x, chip_y, chip_x + c_w, chip_y + c_h], radius=12,
                               fill=bg_col, outline=border_col, width=2 if is_sel else 1)

        draw.text((chip_x + 14, chip_y + 12), title, font=font_chip_title, fill=text_col)
        draw.text((chip_x + 14, chip_y + 40), desc, font=font_chip_sub,
                  fill=ON_EMERALD_CONTAINER if is_sel else SLATE_TEXT_MUTED)

    # Next Steps Preview Card
    next_y = pres_y + 34 + 4 * (c_h + 12) + 15
    draw.rounded_rectangle([content_x + 24, next_y, content_x + 24 + bar_w, next_y + 160], radius=14,
                           fill=SLATE_BG, outline=SLATE_BORDER, width=1)

    font_n_title = get_font(16, bold=True)
    draw.text((content_x + 40, next_y + 14), "Наступні кроки майстра:", font=font_n_title, fill=SLATE_TEXT_SECONDARY)

    steps_preview = [
        "2. Email для регулярних дайджестів (lonelycattools@gmail.com)",
        "3. Інтервал фонової перевірки (кожні 4 години)",
        "4. Глибина первинного сканування (за останні 7 днів)"
    ]
    font_steps = get_font(14)
    for s_idx, s_text in enumerate(steps_preview):
        draw.text((content_x + 40, next_y + 45 + s_idx * 32), "• " + s_text, font=font_steps, fill=SLATE_TEXT_PRIMARY)

    # Wizard Footer Buttons
    btn_area_y = content_y + content_h - 75
    b_w = (bar_w - 20) // 2

    # Left Button: Skip
    draw.rounded_rectangle([content_x + 24, btn_area_y, content_x + 24 + b_w, btn_area_y + 52], radius=12,
                           fill=SLATE_SURFACE_VARIANT, outline=SLATE_BORDER, width=1)
    font_wbtn = get_font(17, bold=True)
    draw.text((content_x + 80, btn_area_y + 14), "Пропустити", font=font_wbtn, fill=SLATE_TEXT_SECONDARY)

    # Right Button: Next (Vector Arrow)
    draw.rounded_rectangle([content_x + 34 + b_w, btn_area_y, content_x + 24 + bar_w, btn_area_y + 52], radius=12,
                           fill=EMERALD_PRIMARY)
    draw.text((content_x + 70 + b_w, btn_area_y + 14), "Продовжити", font=font_wbtn, fill=WHITE)
    draw_vector_icon(draw, 'arrow_right', content_x + 195 + b_w, btn_area_y + 18, 16, WHITE)

    final_img = img.convert("RGB")
    final_path = os.path.join(OUTPUT_DIR, "screenshot_2_wizard.png")
    final_img.save(final_path, "PNG")
    print(f"Saved: {final_path}")


# ==============================================================================
# 5. SCREENSHOT 3: ANALYTICS & EXCEL (1080 x 1920 px)
# ==============================================================================
def generate_screenshot_3():
    print("Generating screenshot_3_analytics.png (Vector Edition)...")
    img = create_base_screenshot_canvas(
        headline_pill="ЗВІТНІСТЬ ТА АНАЛІЗ",
        headline_main="АНАЛІТИКА ТА ЕКСПОРТ В EXCEL",
        headline_sub="Топ замовників, фінансовий зріз ринку та таблиці .xlsx",
        pill_icon='chart'
    )
    draw = ImageDraw.Draw(img)

    content_x = PHONE_X + 44
    content_y = PHONE_Y + 130
    content_w = PHONE_W - 88

    # Summary KPI Card
    sum_h = 135
    draw.rounded_rectangle([content_x, content_y, content_x + content_w, content_y + sum_h], radius=16,
                           fill=SLATE_SURFACE, outline=SLATE_BORDER, width=1)

    font_s_title = get_font(16)
    draw.text((content_x + 24, content_y + 16), "Фінансовий огляд активного пулу закупівель", font=font_s_title, fill=SLATE_TEXT_SECONDARY)

    font_s_big = get_font(34, bold=True)
    draw.text((content_x + 24, content_y + 44), "1 245 800 000 ₴", font=font_s_big, fill=WHITE)

    font_s_sub = get_font(15)
    draw.text((content_x + 24, content_y + 92), "428 тендерів у 6 ключових напрямках • Середній чек: 2.91M ₴",
              font=font_s_sub, fill=ON_EMERALD_CONTAINER)

    # Big Prominent Excel Export Button with Vector Chart Icon
    excel_y = content_y + sum_h + 16
    excel_h = 75
    draw.rounded_rectangle([content_x, excel_y, content_x + content_w, excel_y + excel_h], radius=14,
                           fill=EMERALD_PRIMARY)

    draw_vector_icon(draw, 'chart', content_x + 28, excel_y + 18, 22, WHITE)
    font_ex_title = get_font(21, bold=True)
    font_ex_sub = get_font(14)
    draw.text((content_x + 58, excel_y + 14), "Експорт аналітики в Excel (.xlsx)", font=font_ex_title, fill=WHITE)
    draw.text((content_x + 28, excel_y + 44), "Завантажити повну таблицю з контактами та тендерною документацією",
              font=font_ex_sub, fill=WHITE)

    # Top Customers Card
    top_y = excel_y + excel_h + 20
    top_h = 490
    draw.rounded_rectangle([content_x, top_y, content_x + content_w, top_y + top_h], radius=16,
                           fill=SLATE_SURFACE, outline=SLATE_BORDER, width=1)

    font_top_lbl = get_font(20, bold=True)
    draw.text((content_x + 24, top_y + 18), "Топ найбільших замовників", font=font_top_lbl, fill=WHITE)

    font_top_sub = get_font(14)
    draw.text((content_x + 24, top_y + 46), "Рейтинг за загальним обсягом оголошених закупівель",
              font=font_top_sub, fill=SLATE_TEXT_MUTED)

    customers = [
        ("КП «Київпастранс»", "184.2 млн ₴", "14 тендерів", 0.90, EMERALD_PRIMARY),
        ("АТ «Укрзалізниця»", "142.5 млн ₴", "9 тендерів", 0.72, STATUS_INFO),
        ("ДП «Медичні закупівлі України»", "98.0 млн ₴", "21 тендер", 0.52, STATUS_WARNING),
        ("Львівська міська рада", "65.4 млн ₴", "8 тендерів", 0.36, STATUS_COMPLETE),
        ("АТ «Укрпошта»", "48.9 млн ₴", "12 тендерів", 0.28, STATUS_ACTIVE),
    ]

    chart_y = top_y + 80
    row_h = 75
    font_c_name = get_font(16, bold=True)
    font_c_val = get_font(16, bold=True)
    font_c_tenders = get_font(13)

    for idx, (name, val, count, pct, col) in enumerate(customers):
        ry = chart_y + idx * row_h
        draw.text((content_x + 24, ry), f"{idx + 1}. {name}", font=font_c_name, fill=WHITE)
        draw.text((content_x + content_w - 180, ry), val, font=font_c_val, fill=col)

        # Bar track
        bar_w = content_w - 48
        draw.rounded_rectangle([content_x + 24, ry + 28, content_x + 24 + bar_w, ry + 38], radius=5,
                               fill=SLATE_SURFACE_VARIANT)
        fill_w = int(bar_w * pct)
        draw.rounded_rectangle([content_x + 24, ry + 28, content_x + 24 + fill_w, ry + 38], radius=5,
                               fill=col)

        draw.text((content_x + 24, ry + 44), count, font=font_c_tenders, fill=SLATE_TEXT_MUTED)

    # Category Breakdown Card
    cat_y = top_y + top_h + 18
    cat_h = 160
    draw.rounded_rectangle([content_x, cat_y, content_x + content_w, cat_y + cat_h], radius=16,
                           fill=SLATE_SURFACE_VARIANT, outline=SLATE_BORDER, width=1)

    font_cat_title = get_font(17, bold=True)
    draw.text((content_x + 24, cat_y + 14), "Структура бюджету за категоріями ДК 021", font=font_cat_title, fill=WHITE)

    cat_chips = [
        ("4500 Будівництво: 48%", EMERALD_PRIMARY),
        ("3023 IT та техніка: 24%", STATUS_INFO),
        ("0913 Паливо: 16%", STATUS_WARNING),
        ("Інші сфери: 12%", STATUS_COMPLETE)
    ]
    font_cc = get_font(14, bold=True)
    for c_i, (c_label, c_col) in enumerate(cat_chips):
        cx_pos = content_x + 24 + (c_i % 2) * ((content_w - 36) // 2)
        cy_pos = cat_y + 48 + (c_i // 2) * 44
        draw.rounded_rectangle([cx_pos, cy_pos, cx_pos + (content_w - 48) // 2, cy_pos + 36], radius=8,
                               fill=SLATE_SURFACE, outline=c_col, width=1)
        draw.ellipse([cx_pos + 12, cy_pos + 13, cx_pos + 22, cy_pos + 23], fill=c_col)
        draw.text((cx_pos + 30, cy_pos + 9), c_label, font=font_cc, fill=SLATE_TEXT_PRIMARY)

    final_img = img.convert("RGB")
    final_path = os.path.join(OUTPUT_DIR, "screenshot_3_analytics.png")
    final_img.save(final_path, "PNG")
    print(f"Saved: {final_path}")


# ==============================================================================
# 6. SCREENSHOT 4: ARCHIVE & FILTERS (1080 x 1920 px)
# ==============================================================================
def generate_screenshot_4():
    print("Generating screenshot_4_archive.png (Vector Edition)...")
    img = create_base_screenshot_canvas(
        headline_pill="ТОЧНИЙ ПОШУК",
        headline_main="АРХІВ ТА ФІЛЬТРАЦІЯ ТЕНДЕРІВ",
        headline_sub="Миттєвий пошук за CPV, ЄДРПОУ замовника, сумою та термінами",
        pill_icon='search'
    )
    draw = ImageDraw.Draw(img)

    content_x = PHONE_X + 44
    content_y = PHONE_Y + 130
    content_w = PHONE_W - 88

    # Search Bar with Vector Search Icon
    srch_h = 58
    draw.rounded_rectangle([content_x, content_y, content_x + content_w, content_y + srch_h], radius=14,
                           fill=SLATE_SURFACE, outline=SLATE_BORDER, width=1)
    draw_vector_icon(draw, 'search', content_x + 18, content_y + 19, 20, SLATE_TEXT_MUTED)
    font_s_input = get_font(18)
    draw.text((content_x + 48, content_y + 16), "4523, ремонт водопроводу, Київ", font=font_s_input, fill=WHITE)

    # Clear button "x"
    font_x = get_font(18, bold=True)
    draw.text((content_x + content_w - 38, content_y + 16), "×", font=font_x, fill=SLATE_TEXT_MUTED)

    # Filter Chips Row
    chips_y = content_y + srch_h + 14
    chips = [
        ("Всі (1 428)", False),
        ("Активні (312)", True),
        ("Обрані (18)", True),
        ("Бюджет > 1M ₴", False),
    ]
    font_fchip = get_font(14, bold=True)
    ch_x = content_x
    for text, active in chips:
        bg = EMERALD_CONTAINER if active else SLATE_SURFACE
        border = EMERALD_PRIMARY if active else SLATE_BORDER
        txt_col = ON_EMERALD_CONTAINER if active else SLATE_TEXT_SECONDARY
        w, _ = draw_pill(draw, ch_x, chips_y, text, font_fchip, bg, txt_col, padding_x=14, padding_y=8, outline=border)
        ch_x += w + 10

    # 4 Detailed Tender Results Cards
    results = [
        {
            "fav": True,
            "status": "Прийом пропозицій",
            "status_col": STATUS_ACTIVE,
            "cpv": "45231300-8 Будівельні роботи: трубопроводи",
            "title": "Реконструкція водопровідної мережі та насосної станції м. Київ",
            "customer": "ПрАТ «АК «Київводоканал» • ЄДРПОУ 03327664",
            "price": "28 400 000 ₴",
            "deadline": "Термін: 4 дні (до 24 лист.)",
            "urgent": False
        },
        {
            "fav": True,
            "status": "Терміново: < 48 годин",
            "status_col": STATUS_WARNING,
            "cpv": "30230000-0 Комп'ютерне обладнання",
            "title": "Постачання серверів та ліцензійного програмного забезпечення",
            "customer": "АТ «Укрпошта» • ЄДРПОУ 20306396",
            "price": "4 150 000 ₴",
            "deadline": "Термін: 29 годин (до 21 лист.)",
            "urgent": True
        },
        {
            "fav": False,
            "status": "Прийом пропозицій",
            "status_col": STATUS_ACTIVE,
            "cpv": "09132000-3 Бензин та дизельне паливо",
            "title": "Закупівля пального дизельного Євро-5 та бензину А-95 у талонах",
            "customer": "КП «Київпастранс» • ЄДРПОУ 31725604",
            "price": "9 850 000 ₴",
            "deadline": "Термін: 8 днів (до 28 лист.)",
            "urgent": False
        },
        {
            "fav": False,
            "status": "Прекваліфікація",
            "status_col": STATUS_COMPLETE,
            "cpv": "45233142-6 Ремонт доріг",
            "title": "Поточний середній ремонт автомобільної дороги загального користування",
            "customer": "Служба відновлення та розвитку інфраструктури",
            "price": "42 000 000 ₴",
            "deadline": "Термін: 12 днів",
            "urgent": False
        }
    ]

    t_start_y = chips_y + 54
    t_card_h = 195
    font_res_cpv = get_font(13, bold=True)
    font_res_title = get_font(17, bold=True)
    font_res_cust = get_font(14)
    font_res_price = get_font(22, bold=True)
    font_res_dead = get_font(14, bold=True)

    for idx, t in enumerate(results):
        ry = t_start_y + idx * (t_card_h + 14)
        if ry + t_card_h > PHONE_Y + PHONE_H - 130:
            break

        draw.rounded_rectangle([content_x, ry, content_x + content_w, ry + t_card_h], radius=14,
                               fill=SLATE_SURFACE, outline=SLATE_BORDER, width=1)

        # Status Pill with pulsing color dot (NO EMOJI)
        draw_pill(draw, content_x + 16, ry + 12, t["status"], font_res_cpv,
                  (20, 35, 55), t["status_col"], padding_x=10, padding_y=4,
                  icon='pulse_dot', icon_color=t["status_col"], icon_size=10)

        # Favorite Star (Vector)
        star_col = GOLD if t["fav"] else SLATE_TEXT_MUTED
        draw_vector_icon(draw, 'star', content_x + content_w - 38, ry + 12, 18, star_col)

        # Title
        draw.text((content_x + 16, ry + 44), t["title"], font=font_res_title, fill=WHITE)

        # CPV & Customer with vector icons
        draw_vector_icon(draw, 'folder', content_x + 16, ry + 74, 14, ON_EMERALD_CONTAINER)
        draw.text((content_x + 36, ry + 72), t["cpv"], font=font_res_cust, fill=ON_EMERALD_CONTAINER)

        draw_vector_icon(draw, 'building', content_x + 16, ry + 96, 14, SLATE_TEXT_SECONDARY)
        draw.text((content_x + 36, ry + 94), t["customer"], font=font_res_cust, fill=SLATE_TEXT_SECONDARY)

        # Divider
        draw.line([(content_x + 16, ry + 125), (content_x + content_w - 16, ry + 125)],
                  fill=SLATE_BORDER, width=1)

        # Price & Deadline
        draw.text((content_x + 16, ry + 142), t["price"], font=font_res_price, fill=EMERALD_LIGHT)
        dead_col = STATUS_URGENT if t["urgent"] else SLATE_TEXT_MUTED
        draw.text((content_x + content_w - 260, ry + 147), t["deadline"], font=font_res_dead, fill=dead_col)

    # Floating Action Button with Vector Arrow Down
    fab_x = content_x + content_w - 80
    fab_y = PHONE_Y + PHONE_H - 220
    draw.ellipse([fab_x, fab_y, fab_x + 64, fab_y + 64], fill=EMERALD_PRIMARY)
    draw_vector_icon(draw, 'arrow_down', fab_x + 22, fab_y + 20, 20, WHITE)

    final_img = img.convert("RGB")
    final_path = os.path.join(OUTPUT_DIR, "screenshot_4_archive.png")
    final_img.save(final_path, "PNG")
    print(f"Saved: {final_path}")


def main():
    print("=== STARTING PLAY STORE ASSET GENERATION (VECTOR EDITION) ===")
    generate_icon()
    generate_feature_graphic()
    generate_screenshot_1()
    generate_screenshot_2()
    generate_screenshot_3()
    generate_screenshot_4()
    print("=== ALL ASSETS GENERATED CLEANLY WITHOUT MISSING GLYPHS ===")

if __name__ == "__main__":
    main()
