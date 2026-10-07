"""Draws the simulated AYN Thor GIFs in docs/images/sim-*.gif.

These are SIMULATIONS of Mjolnir 0.2.7b behaviour, not device captures.
Run: python3 -m venv v && v/bin/pip install pillow && v/bin/python docs/tools/make_gifs.py
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

OUT = Path(__file__).resolve().parents[1] / "images"
W, H = 640, 600
TOP = (120, 40, 520, 265)      # 16:9 top screen
BOT = (200, 330, 440, 538)     # ~1.15:1 bottom screen

APPS = {  # name -> colour
    "Game": (90, 90, 110), "Browser": (70, 120, 90), "ES-DE": (232, 163, 61),
    "Companion": (61, 139, 232), "Launcher": (150, 90, 200), "Recents": (60, 60, 60),
}


def font(size, bold=False):
    name = "NotoSans-Bold.ttf" if bold else "NotoSans-Regular.ttf"
    try:
        return ImageFont.truetype(f"/usr/share/fonts/truetype/noto/{name}", size)
    except OSError:
        return ImageFont.load_default(size)


def screen(d, box, app, focus):
    x0, y0, x1, y1 = box
    if focus:
        d.rounded_rectangle((x0 - 7, y0 - 7, x1 + 7, y1 + 7), 14, outline=(255, 214, 0), width=5)
    d.rounded_rectangle(box, 8, fill=APPS[app])
    d.text(((x0 + x1) / 2, (y0 + y1) / 2), app, font=font(30, True), fill="white", anchor="mm")


def frame(title, top, bottom, focus_top, caption, press=0):
    im = Image.new("RGB", (W, H), (24, 24, 28))
    d = ImageDraw.Draw(im)
    d.rounded_rectangle((100, 22, 540, 283), 22, fill=(45, 45, 52))    # top shell
    d.rounded_rectangle((110, 300, 530, 552), 22, fill=(45, 45, 52))   # bottom shell
    screen(d, TOP, top, focus_top)
    screen(d, BOT, bottom, not focus_top)
    hx, hy = 480, 440                                                 # Home button
    d.ellipse((hx - 18, hy - 18, hx + 18, hy + 18), fill=(255, 214, 0) if press else (90, 90, 100))
    d.text((hx, hy + 32), "HOME", font=font(14, True), fill=(200, 200, 200), anchor="mm")
    if press:
        d.text((hx, hy - 34), "x" + str(press) if press < 9 else "hold", font=font(16, True), fill=(255, 214, 0), anchor="mm")
    d.text((W / 2, 2), title, font=font(18, True), fill="white", anchor="ma")
    d.rectangle((0, H - 40, W, H), fill=(14, 14, 16))
    d.text((12, H - 20), caption, font=font(17), fill=(230, 230, 230), anchor="lm")
    d.text((W - 10, H - 20), "SIMULATION", font=font(12), fill=(130, 130, 130), anchor="rm")
    return im


def gif(name, title, after_top, after_bottom, after_focus_top, note, press=1,
        before=("Game", "Browser", True)):
    bt, bb, bf = before
    frames = [
        (frame(title, bt, bb, bf, "Before: you use other apps."), 1400),
        (frame(title, bt, bb, bf, "You push Home.", press), 700),
        (frame(title, after_top, after_bottom, after_focus_top, "After: " + note), 2600),
    ]
    ims, durs = zip(*frames)
    ims[0].save(OUT / f"sim-{name}.gif", save_all=True, append_images=list(ims[1:]),
                duration=list(durs), loop=0, optimize=True)
    return frames


SETUP = "Top app = ES-DE, Bottom app = Companion, default home = Launcher. Yellow = focus."
CASES = [
    ("top-app", "TOP: ES-DE", "ES-DE", "Browser", True, "Top screen changes. Bottom screen stays."),
    ("bottom-app", "BOTTOM: Companion", "Game", "Companion", False, "Bottom screen changes. Top screen stays."),
    ("both-auto", "BOTH: Auto (Mjolnir icon), Main Screen = Top", "ES-DE", "Companion", True, "Both screens change. Top has focus."),
    ("both-auto-main-bottom", "BOTH: Auto, Main Screen = Bottom", "ES-DE", "Companion", False, "Both screens change. Bottom has focus."),
    ("focus-auto", "FOCUS: Auto (focus on top)", "ES-DE", "Browser", True, "Only the screen with focus changes."),
    ("top-home", "TOP: Home", "Launcher", "Browser", True, "Default home on top. Bottom screen stays."),
    ("bottom-home", "BOTTOM: Home", "Game", "Launcher", False, "Default home on bottom. Top screen stays."),
    ("both-home", "BOTH: Home", "Launcher", "Launcher", True, "Both screens go to the default home."),
    ("bottom-home-027a", "BOTTOM: Home in 0.2.7a (bug #34/#35)", "Launcher", "Launcher", True, "Bug: both screens went home."),
]

if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    tour = []
    for case in CASES:
        tour += gif(*case)
    ims, durs = zip(*tour)
    ims[0].save(OUT / "sim-tour.gif", save_all=True, append_images=list(ims[1:]),
                duration=list(durs), loop=0, optimize=True)
    print("setup:", SETUP)
    print("wrote", len(CASES) + 1, "gifs to", OUT)
