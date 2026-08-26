#!/usr/bin/env python3
"""Generate high-resolution PNG logo, banner, and UI mockup screenshots for Mobdysseus v2."""

import math
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

PROJECT_ROOT = Path(__file__).resolve().parents[1]
ASSETS_DIR = PROJECT_ROOT / "assets"
SCREENSHOTS_DIR = ASSETS_DIR / "screenshots"
DOCS_SCREENSHOTS_DIR = PROJECT_ROOT / "docs" / "screenshots"

SCREENSHOTS_DIR.mkdir(parents=True, exist_ok=True)
DOCS_SCREENSHOTS_DIR.mkdir(parents=True, exist_ok=True)


def draw_neon_logo(size=512):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Background rounded rect
    bg_color = (17, 19, 24, 255)
    border_color = (224, 108, 117, 200)
    corner_radius = int(size * 0.22)
    draw.rounded_rectangle([0, 0, size - 1, size - 1], radius=corner_radius, fill=bg_color, outline=border_color, width=3)

    # Cyber circle accents
    center = size / 2
    r1 = size * 0.38
    draw.ellipse([center - r1, center - r1, center + r1, center + r1], outline=(52, 57, 70, 180), width=2)
    
    # Outer Spartan / Cyber Crest
    top_y = size * 0.16
    mid_y = size * 0.30
    bot_y = size * 0.60
    tip_y = size * 0.84
    left_x = size * 0.26
    right_x = size * 0.74

    crest_points = [
        (center, top_y),
        (right_x, mid_y),
        (right_x, bot_y),
        (center, tip_y),
        (left_x, bot_y),
        (left_x, mid_y),
    ]
    draw.polygon(crest_points, fill=(27, 30, 38, 255), outline=(77, 85, 105, 255))

    # Inner Shard Facets
    draw.polygon([(center, top_y), (center, center), (right_x, mid_y)], fill=(45, 52, 68, 255))
    draw.polygon([(center, top_y), (center, center), (left_x, mid_y)], fill=(28, 32, 42, 255))
    draw.polygon([(left_x, mid_y), (center, center), (left_x, bot_y)], fill=(22, 25, 33, 255))
    draw.polygon([(right_x, mid_y), (center, center), (right_x, bot_y)], fill=(38, 44, 58, 255))
    draw.polygon([(left_x, bot_y), (center, center), (center, tip_y)], fill=(25, 29, 38, 255))
    draw.polygon([(right_x, bot_y), (center, center), (center, tip_y)], fill=(33, 38, 50, 255))

    # Neon Coral Lambda Chevron
    chevron = [
        (center, size * 0.24),
        (size * 0.66, size * 0.54),
        (size * 0.59, size * 0.54),
        (center, size * 0.36),
        (size * 0.41, size * 0.54),
        (size * 0.34, size * 0.54),
    ]
    draw.polygon(chevron, fill=(224, 108, 117, 255))

    # Quantum Cyan Core
    core = [
        (center, size * 0.42),
        (size * 0.58, size * 0.50),
        (center, size * 0.58),
        (size * 0.42, size * 0.50),
    ]
    draw.polygon(core, fill=(0, 240, 255, 255), outline=(255, 255, 255, 255))
    draw.ellipse([center - 6, center - 6, center + 6, center + 6], fill=(255, 255, 255, 255))

    return img


def draw_hero_banner():
    width = 1200
    height = 420
    img = Image.new("RGB", (width, height), (17, 19, 24))
    draw = ImageDraw.Draw(img)

    # Grid background lines
    for x in range(0, width, 40):
        draw.line([(x, 0), (x, height)], fill=(27, 30, 37), width=1)
    for y in range(0, height, 40):
        draw.line([(0, y), (width, y)], fill=(27, 30, 37), width=1)

    # Ambient glow on left
    for r in range(250, 0, -10):
        alpha = int(25 * (1 - r / 250))
        draw.ellipse([140 - r, 210 - r, 140 + r, 210 + r], outline=(224, 108, 117), width=2)

    logo = draw_neon_logo(240)
    img.paste(logo, (80, 90), logo)

    # Typography & Hero copy
    draw.text((370, 110), "MOBDYSSEUS v2.0", fill=(224, 108, 117))
    draw.text((370, 155), "The Sovereign Edge AI Workspace for Android", fill=(231, 233, 240))
    draw.text((370, 205), "• 100% Native Jetpack Compose  • Zero-WebView  • Airplane-Mode First", fill=(171, 177, 192))
    draw.text((370, 235), "• Hardware Keystore + SQLCipher  • LiteRT Snapdragon NPU Runtime", fill=(171, 177, 192))
    draw.text((370, 265), "• 7 Custom Dynamic Themes  • Sandboxed Android Capability Broker", fill=(171, 177, 192))

    # Badge chips
    badges = [
        ("OFFLINE CORE", (123, 201, 154)),
        ("NO TELEMETRY", (77, 150, 255)),
        ("GALAXY S25 READY", (245, 166, 35)),
        ("AGPL-3.0", (224, 108, 117)),
    ]
    bx = 370
    for label, col in badges:
        draw.rounded_rectangle([bx, 315, bx + 160, 350], radius=8, fill=(27, 30, 37), outline=col, width=1)
        draw.text((bx + 16, 326), label, fill=col)
        bx += 175

    return img


def draw_mockup_screen(title, subtitle, elements, accent_color=(224, 108, 117), bg_color=(17, 19, 24)):
    w, h = 540, 960
    img = Image.new("RGB", (w, h), bg_color)
    draw = ImageDraw.Draw(img)

    # Status Bar
    draw.text((24, 14), "12:00", fill=(231, 233, 240))
    draw.text((w - 120, 14), "5G  100% [=]", fill=(171, 177, 192))

    # Top App Bar
    draw.rectangle([0, 48, w, 110], fill=bg_color)
    draw.text((24, 68), "◢  MOBDYSSEUS", fill=accent_color)
    draw.text((w - 90, 72), "LOCAL", fill=accent_color)
    draw.line([(0, 110), (w, 110)], fill=(52, 57, 70), width=1)

    # Subheading
    draw.text((24, 126), title, fill=(231, 233, 240))
    draw.text((24, 155), subtitle, fill=(171, 177, 192))

    # Cards / Elements
    cur_y = 195
    for item in elements:
        card_h = item.get("height", 90)
        draw.rounded_rectangle([20, cur_y, w - 20, cur_y + card_h], radius=16, fill=(27, 30, 37), outline=(52, 57, 70), width=1)
        
        tag = item.get("tag")
        if tag:
            draw.rounded_rectangle([36, cur_y + 16, 36 + len(tag)*9 + 12, cur_y + 36], radius=6, fill=(36, 40, 51), outline=accent_color, width=1)
            draw.text((42, cur_y + 19), tag, fill=accent_color)

        draw.text((36, cur_y + (44 if tag else 18)), item.get("heading", ""), fill=(231, 233, 240))
        draw.text((36, cur_y + (68 if tag else 44)), item.get("detail", ""), fill=(171, 177, 192))
        
        cur_y += card_h + 16

    # Bottom Nav Bar
    draw.rectangle([0, h - 80, w, h], fill=(27, 30, 37))
    draw.line([(0, h - 80), (w, h - 80)], fill=(52, 57, 70), width=1)
    nav_items = ["Chat", "Cookbook", "Brain", "Notes", "Tasks", "More"]
    step = w // len(nav_items)
    for i, name in enumerate(nav_items):
        col = accent_color if name in title or (i == 0 and "Chat" in title) else (171, 177, 192)
        draw.text((i * step + 18, h - 48), name, fill=col)

    return img


def main():
    print("Generating Mobdysseus v2 visual media assets...")

    # 1. Logo PNG
    logo = draw_neon_logo(512)
    logo.save(ASSETS_DIR / "logo.png")
    print("  -> Saved assets/logo.png")

    # 2. Hero Banner
    banner = draw_hero_banner()
    banner.save(ASSETS_DIR / "banner.png")
    print("  -> Saved assets/banner.png")

    # 3. Screenshot: Streaming Chat
    chat_mock = draw_mockup_screen(
        title="Local AI Chat",
        subtitle="Zero network egress. Token streaming on Snapdragon NPU.",
        elements=[
            {"tag": "LITERT 3B", "heading": "Local Assistant", "detail": "I'm running completely on your device. Zero bytes leave this phone.", "height": 105},
            {"tag": "RAG CITATION", "heading": "Personal Context", "detail": "Retrieved 2 notes with offline BM25 + LiteRT vector indexing.", "height": 105},
            {"tag": "PROMPT", "heading": "User", "detail": "Summarize my architectural notes on Mobdysseus v2 security.", "height": 90},
            {"tag": "BENCHMARK", "heading": "Hardware Telemetry", "detail": "First token: 340ms | 48.2 tok/s | RAM: 1.4GB | Temp: 32.5°C", "height": 95},
        ],
        accent_color=(224, 108, 117),
    )
    chat_mock.save(SCREENSHOTS_DIR / "01_chat_streaming.png")
    chat_mock.save(DOCS_SCREENSHOTS_DIR / "01_chat_streaming.png")

    # 4. Screenshot: Theme Engine
    themes_mock = draw_mockup_screen(
        title="Dynamic Theme Engine",
        subtitle="7 high-contrast OLED palettes with instant live switching.",
        elements=[
            {"tag": "ACTIVE", "heading": "Obsidian Coral", "detail": "Classic dark obsidian with vivid coral accents & monospace badges.", "height": 95},
            {"tag": "OLED PURE", "heading": "Cyberpunk Neon", "detail": "Pitch black backdrop with hyper-vibrant cyan & magenta highlights.", "height": 95},
            {"tag": "NAVY SKY", "heading": "Midnight Navy", "detail": "Deep indigo night sky with electric blue accents.", "height": 95},
            {"tag": "WARM DARK", "heading": "Solarized Amber", "detail": "Espresso dark palette with rich amber & gold typography.", "height": 95},
            {"tag": "TERMINAL", "heading": "Forest Matrix", "detail": "High-contrast terminal black with glowing emerald green.", "height": 95},
        ],
        accent_color=(0, 240, 255),
    )
    themes_mock.save(SCREENSHOTS_DIR / "02_theme_engine.png")
    themes_mock.save(DOCS_SCREENSHOTS_DIR / "02_theme_engine.png")

    # 5. Screenshot: Cookbook & Hardware Diagnostics
    cookbook_mock = draw_mockup_screen(
        title="Cookbook & Recipe Runs",
        subtitle="Automated workflows, hardware-fit check, and model manager.",
        elements=[
            {"tag": "RECIPE", "heading": "Private Quick Chat", "detail": "General-purpose conversation tuned for low memory footprint.", "height": 95},
            {"tag": "RECIPE", "heading": "Deep Document Analysis", "detail": "Full local RAG retrieval with citation verify and extraction.", "height": 95},
            {"tag": "HARDWARE FIT", "heading": "Galaxy S25 Snapdragon 8 Elite", "detail": "NPU backend active | 12GB RAM available | Fits up to 7B Q4_K_M", "height": 105},
            {"tag": "SECURITY", "heading": "Capability Allowlist", "detail": "Read: Notes, Documents | Network: Blocked | Subprocess: Denied", "height": 95},
        ],
        accent_color=(123, 201, 154),
    )
    cookbook_mock.save(SCREENSHOTS_DIR / "03_cookbook_recipes.png")
    cookbook_mock.save(DOCS_SCREENSHOTS_DIR / "03_cookbook_recipes.png")

    # 6. Screenshot: Brain & Governed Memory
    brain_mock = draw_mockup_screen(
        title="Mnemosyne Governed Memory",
        subtitle="Encrypted long-term memory with strict user approval gates.",
        elements=[
            {"tag": "MEM-01", "heading": "Project Architecture", "detail": "Mobdysseus strictly prohibits WebViews and enforces SQLCipher encryption.", "height": 95},
            {"tag": "MEM-02", "heading": "Device Preference", "detail": "User prefers Cyberpunk Neon theme with compact density enabled.", "height": 95},
            {"tag": "GOVERNANCE", "heading": "Proposed Memory Queue (0)", "detail": "Zero unapproved memories. Model extraction requires user tap.", "height": 95},
            {"tag": "BACKUP", "heading": "Keystore Encrypted Export", "detail": "Export full memory matrix to passphrase-protected .mobdbak archive.", "height": 95},
        ],
        accent_color=(245, 166, 35),
    )
    brain_mock.save(SCREENSHOTS_DIR / "04_brain_memory.png")
    brain_mock.save(DOCS_SCREENSHOTS_DIR / "04_brain_memory.png")

    print("All media assets generated successfully!")


if __name__ == "__main__":
    main()
