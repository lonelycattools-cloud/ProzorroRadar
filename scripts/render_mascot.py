import os
import subprocess
from PIL import Image

EDGE = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
OUTPUT_DIR = r"c:\WORK\ProzoroAndroid\play_store_assets"
os.makedirs(OUTPUT_DIR, exist_ok=True)

# 1. Render official 512x512 icon
svg_icon = """<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  html, body { width: 512px; height: 512px; overflow: hidden; background: #0B111A; }
  svg { width: 512px; height: 512px; display: block; }
</style>
</head>
<body>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="512" height="512" shape-rendering="geometricPrecision">
  <defs>
    <radialGradient id="radarGlow" cx="50%" cy="50%" r="50%">
      <stop offset="0%" stop-color="#00E676" stop-opacity="0.18"/>
      <stop offset="60%" stop-color="#00E676" stop-opacity="0.06"/>
      <stop offset="100%" stop-color="#00E676" stop-opacity="0"/>
    </radialGradient>
    <linearGradient id="bgGrad" x1="0%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%" stop-color="#090E17"/>
      <stop offset="100%" stop-color="#121B2A"/>
    </linearGradient>
  </defs>

  <rect width="108" height="108" fill="url(#bgGrad)"/>
  <circle cx="54" cy="54" r="50" fill="url(#radarGlow)"/>

  <!-- Concentric Green Radar Rings -->
  <circle cx="54" cy="54" r="42" stroke="#00E676" stroke-opacity="0.12" stroke-width="1.2" fill="none"/>
  <circle cx="54" cy="54" r="28" stroke="#00E676" stroke-opacity="0.22" stroke-width="1.3" fill="none"/>
  <circle cx="54" cy="54" r="15" stroke="#00E676" stroke-opacity="0.32" stroke-width="1.4" fill="none"/>

  <!-- Crosshairs -->
  <line x1="12" y1="54" x2="96" y2="54" stroke="#00E676" stroke-opacity="0.10" stroke-width="1"/>
  <line x1="54" y1="12" x2="54" y2="96" stroke="#00E676" stroke-opacity="0.10" stroke-width="1"/>

  <!-- Top Antenna Ball and Stick -->
  <rect x="52" y="24" width="4" height="8" rx="1" fill="#00E676"/>
  <circle cx="54" cy="23" r="4" fill="#00E676"/>

  <!-- Wheels -->
  <circle cx="37" cy="76" r="7" fill="#1E293B"/>
  <circle cx="37" cy="76" r="3" fill="#FFFFFF"/>
  <circle cx="71" cy="76" r="7" fill="#1E293B"/>
  <circle cx="71" cy="76" r="3" fill="#FFFFFF"/>

  <!-- Main Body -->
  <path fill="#00C853" d="M32,44 C32,34 42,32 54,32 C66,32 76,34 76,44 C76,48 78,54 78,60 C78,70 70,74 54,74 C38,74 30,70 30,60 C30,54 32,48 32,44 Z"/>

  <!-- Highlight -->
  <path fill="#FFFFFF" fill-opacity="0.18" d="M35,44 C35,36 44,34 54,34 C64,34 73,36 73,44 C73,46 72,49 71,51 C66,45 58,42 54,42 C50,42 42,45 37,51 C36,49 35,46 35,44 Z"/>

  <!-- Eyes -->
  <circle cx="43" cy="52" r="6" fill="#FFFFFF"/>
  <circle cx="44" cy="52" r="3" fill="#0B111A"/>
  <circle cx="45" cy="51" r="1.1" fill="#FFFFFF"/>

  <circle cx="65" cy="52" r="6" fill="#FFFFFF"/>
  <circle cx="66" cy="52" r="3" fill="#0B111A"/>
  <circle cx="67" cy="51" r="1.1" fill="#FFFFFF"/>

  <!-- Smile -->
  <path d="M48,64 Q54,68 60,64" stroke="#003912" stroke-width="2.5" stroke-linecap="round" fill="none"/>

  <!-- Radar Waves -->
  <path d="M46,18 C51,15 57,15 62,18" stroke="#00E676" stroke-width="2" stroke-linecap="round" fill="none"/>
  <path d="M42,13 C50,9 58,9 66,13" stroke="#00E676" stroke-opacity="0.6" stroke-width="1.6" stroke-linecap="round" fill="none"/>
</svg>
</body>
</html>"""

icon_html = os.path.join(OUTPUT_DIR, "icon_render.html")
icon_png = os.path.join(OUTPUT_DIR, "icon_512x512.png")
with open(icon_html, "w", encoding="utf-8") as f:
    f.write(svg_icon)

subprocess.run([EDGE, "--headless", "--disable-gpu", f"--screenshot={icon_png}", "--window-size=512,512", f"file:///{icon_html}"], check=True)
print("Rendered icon_512x512.png")

# 2. Render transparent mascot
svg_mascot = """<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  html, body { width: 512px; height: 512px; overflow: hidden; background: transparent; }
  svg { width: 512px; height: 512px; display: block; }
</style>
</head>
<body>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="512" height="512" shape-rendering="geometricPrecision">
  <rect x="52" y="24" width="4" height="8" rx="1" fill="#00E676"/>
  <circle cx="54" cy="23" r="4" fill="#00E676"/>

  <circle cx="37" cy="76" r="7" fill="#1E293B"/>
  <circle cx="37" cy="76" r="3" fill="#FFFFFF"/>
  <circle cx="71" cy="76" r="7" fill="#1E293B"/>
  <circle cx="71" cy="76" r="3" fill="#FFFFFF"/>

  <path fill="#00C853" d="M32,44 C32,34 42,32 54,32 C66,32 76,34 76,44 C76,48 78,54 78,60 C78,70 70,74 54,74 C38,74 30,70 30,60 C30,54 32,48 32,44 Z"/>
  <path fill="#FFFFFF" fill-opacity="0.22" d="M35,44 C35,36 44,34 54,34 C64,34 73,36 73,44 C73,46 72,49 71,51 C66,45 58,42 54,42 C50,42 42,45 37,51 C36,49 35,46 35,44 Z"/>

  <circle cx="43" cy="52" r="6" fill="#FFFFFF"/>
  <circle cx="44" cy="52" r="3" fill="#0B111A"/>
  <circle cx="45" cy="51" r="1.1" fill="#FFFFFF"/>

  <circle cx="65" cy="52" r="6" fill="#FFFFFF"/>
  <circle cx="66" cy="52" r="3" fill="#0B111A"/>
  <circle cx="67" cy="51" r="1.1" fill="#FFFFFF"/>

  <path d="M48,64 Q54,68 60,64" stroke="#003912" stroke-width="2.5" stroke-linecap="round" fill="none"/>
  <path d="M46,18 C51,15 57,15 62,18" stroke="#00E676" stroke-width="2" stroke-linecap="round" fill="none"/>
  <path d="M42,13 C50,9 58,9 66,13" stroke="#00E676" stroke-opacity="0.7" stroke-width="1.6" stroke-linecap="round" fill="none"/>
</svg>
</body>
</html>"""

mascot_html = os.path.join(OUTPUT_DIR, "mascot_render.html")
mascot_png = os.path.join(OUTPUT_DIR, "mascot_512.png")
with open(mascot_html, "w", encoding="utf-8") as f:
    f.write(svg_mascot)

subprocess.run([EDGE, "--headless", "--disable-gpu", f"--screenshot={mascot_png}", "--default-background-color=00000000", "--window-size=512,512", f"file:///{mascot_html}"], check=True)
print("Rendered mascot_512.png")

# 3. Clean up HTML temp files
if os.path.exists(icon_html):
    os.remove(icon_html)
if os.path.exists(mascot_html):
    os.remove(mascot_html)

print("Mascot assets ready.")
