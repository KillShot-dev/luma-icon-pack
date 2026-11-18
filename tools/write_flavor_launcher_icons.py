#!/usr/bin/env python3
"""Per-flavor launcher icons for Luma (Play store-listing mismatch fix, Oct 2026).

Each flavor's on-device launcher icon is derived from that flavor's own
512px store icon (marketing/app-icons/luma-<flavor>-512.png), so the
installed icon matches the store listing for every flavor.

Flavor source sets override main resources with the same name/qualifier,
so these files win over app/src/main/res launcher art for that flavor.
The shared main icon (now full-bleed) remains as the fallback.

Backgrounds: Clear's and Glyph's store icons are transparent, so their
adaptive backgrounds are transparent (the mark floats, as in the
listing). All other store icons are opaque tiles, so the background
color never shows; they use the shared #141821 background.

Run: python3 tools/write_flavor_launcher_icons.py
Quality: LANCZOS resampling throughout.
"""
from pathlib import Path

from PIL import Image

GOAL = Path(__file__).resolve().parents[2]
APP = GOAL / "android-project" / "app"
ICONS = GOAL / "marketing" / "app-icons"
FLAVORS = ["regular", "dark", "gradient", "glyph", "clear", "prism", "midnight"]
DENSITIES = [("mipmap-mdpi", 48), ("mipmap-hdpi", 72), ("mipmap-xhdpi", 96),
              ("mipmap-xxhdpi", 144), ("mipmap-xxxhdpi", 192)]

ADAPTIVE_XML = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="{background}" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
"""


def main():
    for flavor in FLAVORS:
        src = Image.open(ICONS / f"luma-{flavor}-512.png")
        assert src.size == (512, 512), (flavor, src.size)
        base = APP / "src" / flavor / "res"
        # Adaptive foreground: store icon scaled to 432.
        fg = src.convert("RGBA").resize((432, 432), Image.LANCZOS)
        (base / "drawable").mkdir(parents=True, exist_ok=True)
        fg.save(base / "drawable" / "ic_launcher_foreground.png")
        # Legacy launcher + round icons per density.
        for qual, px in DENSITIES:
            d = base / qual
            d.mkdir(parents=True, exist_ok=True)
            small = src.convert("RGBA").resize((px, px), Image.LANCZOS)
            small.save(d / "ic_launcher.png")
            small.save(d / "ic_launcher_round.png")
        # Adaptive icon XMLs.
        bg = "@android:color/transparent" if flavor in ("clear", "glyph") \
            else "@color/ic_launcher_background"
        d = base / "mipmap-anydpi-v26"
        d.mkdir(parents=True, exist_ok=True)
        (d / "ic_launcher.xml").write_text(ADAPTIVE_XML.format(background=bg))
        (d / "ic_launcher_round.xml").write_text(ADAPTIVE_XML.format(background=bg))
        corner = src.convert("RGBA").getpixel((0, 0))
        print(f"{flavor}: store={src.mode} corner_alpha={corner[3]} "
              f"fg_bbox={fg.getbbox()} background={bg}")


if __name__ == "__main__":
    main()
