"""Offline skins: editor + resource-pack injection (works on 1.8.9 .. 1.21.x)."""
import base64, io, json, re, threading, zipfile
from pathlib import Path
import tkinter as tk
from tkinter import colorchooser, filedialog, messagebox, ttk

import requests
from PIL import Image, ImageDraw, ImageTk

SKIN_DIR = Path(".")  # set by launcher
PACK_NAME = "LuminaSkin.zip"
BG, PANEL, FIELD, ACC, FG, DIM = "#12121a", "#1b1b28", "#25253a", "#7c5cff", "#ffffff", "#9a9ab0"

# ---------------------------------------------------------------- skin files
def skin_file(name):
    return SKIN_DIR / f"{name.lower()}.png"


def meta_file(name):
    return SKIN_DIR / f"{name.lower()}.json"


def default_slim(uuid_str):
    """Model the game picks for this offline UUID (exact for <= 1.19.2)."""
    h = uuid_str.replace("-", "")
    msb, lsb = int(h[:16], 16), int(h[16:], 16)
    hilo = msb ^ lsb
    return ((hilo >> 32) ^ hilo) & 1 == 1


def load_skin(name):
    f = skin_file(name)
    return Image.open(f).convert("RGBA") if f.exists() else None


def load_model(name, default):
    try:
        return bool(json.loads(meta_file(name).read_text())["slim"])
    except Exception:
        return default


def save_skin(name, img, slim):
    SKIN_DIR.mkdir(parents=True, exist_ok=True)
    img.save(skin_file(name))
    meta_file(name).write_text(json.dumps({"slim": slim}))


def delete_skin(name):
    for f in (skin_file(name), meta_file(name)):
        f.unlink(missing_ok=True)


def _flip(img, box, dst):
    img.paste(img.crop(box).transpose(Image.FLIP_LEFT_RIGHT), dst)


def _mirror_limb(img, su, sv, du, dv):
    _flip(img, (su + 4, sv, su + 8, sv + 4), (du + 4, dv))
    _flip(img, (su + 8, sv, su + 12, sv + 4), (du + 8, dv))
    _flip(img, (su, sv + 4, su + 4, sv + 16), (du + 8, dv + 4))
    _flip(img, (su + 4, sv + 4, su + 8, sv + 16), (du + 4, dv + 4))
    _flip(img, (su + 8, sv + 4, su + 12, sv + 16), (du, dv + 4))
    _flip(img, (su + 12, sv + 4, su + 16, sv + 16), (du + 12, dv + 4))


def normalize(img):
    """Any valid skin -> 64x64 RGBA."""
    img = img.convert("RGBA")
    w, h = img.size
    if w > 64 and w % 64 == 0 and w == h:
        img = img.resize((64, 64), Image.NEAREST)
    elif w > 64 and w % 64 == 0 and w == 2 * h:
        img = img.resize((64, 32), Image.NEAREST)
    if img.size == (64, 32):
        out = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        out.paste(img, (0, 0))
        _mirror_limb(out, 0, 16, 16, 48)
        _mirror_limb(out, 40, 16, 32, 48)
        img = out
    if img.size != (64, 64):
        raise ValueError("Нужен PNG 64x64 (или старый 64x32)")
    return img


def guess_slim(img):
    px = img.load()
    return all(px[x, y][3] == 0 for x in (54, 55) for y in range(20, 32))


def fetch_by_name(nick):
    r = requests.get(f"https://api.mojang.com/users/profiles/minecraft/{nick}", timeout=15)
    if r.status_code != 200:
        raise RuntimeError("Игрок не найден")
    pid = r.json()["id"]
    prof = requests.get(f"https://sessionserver.mojang.com/session/minecraft/profile/{pid}", timeout=15).json()
    for p in prof.get("properties", []):
        if p["name"] == "textures":
            tex = json.loads(base64.b64decode(p["value"]))["textures"].get("SKIN")
            if not tex:
                break
            slim = tex.get("metadata", {}).get("model") == "slim"
            data = requests.get(tex["url"], timeout=15).content
            return normalize(Image.open(io.BytesIO(data))), slim
    raise RuntimeError("У игрока нет скина")


# ---------------------------------------------------------------- preview
def preview(img, slim, back=False):
    """16x32 flat render of the character (front or back)."""
    out = Image.new("RGBA", (16, 32), (0, 0, 0, 0))

    def put(box, dst):
        out.alpha_composite(img.crop(box), dst)

    a = 3 if slim else 4
    ax = 4 - a  # dest x of the arm on viewer's left
    if not back:
        for dy in (0, 16):  # base, then overlay
            o = 1 if dy else 0
            put((8 + 32 * o, 8, 16 + 32 * o, 16), (4, 0))                      # head / hat
            put((20, 20 + 16 * o, 28, 32 + 16 * o), (4, 8))                     # body / jacket
            put((44, 20 + 16 * o, 44 + a, 32 + 16 * o), (ax, 8))                # right arm
            put((36 + 16 * o, 52, 36 + 16 * o + a, 64), (12, 8))                # left arm
            put((4, 20 + 16 * o, 8, 32 + 16 * o), (4, 20))                      # right leg
            put((20 - 16 * o, 52, 24 - 16 * o, 64), (8, 20))                    # left leg
    else:
        for o in (0, 1):
            put((24 + 32 * o, 8, 32 + 32 * o, 16), (4, 0))
            put((32, 20 + 16 * o, 40, 32 + 16 * o), (4, 8))
            la = 44 + 16 * o - (1 if slim else 0)
            ra = 52 - (1 if slim else 0)
            put((la, 52, la + a, 64), (ax, 8))                                  # left arm (viewer left)
            put((ra, 20 + 16 * o, ra + a, 32 + 16 * o), (12, 8))                # right arm
            put((28 - 16 * o, 52, 32 - 16 * o, 64), (4, 20))                    # left leg
            put((12, 20 + 16 * o, 16, 32 + 16 * o), (8, 20))                    # right leg
    return out


# ---------------------------------------------------------------- resource pack
FORMATS = [((1, 21, 11), 75), ((1, 21, 9), 69), ((1, 21, 7), 64), ((1, 21, 6), 63), ((1, 21, 5), 55),
           ((1, 21, 4), 46), ((1, 21, 2), 42), ((1, 21, 0), 34), ((1, 20, 5), 32), ((1, 20, 3), 22),
           ((1, 20, 2), 18), ((1, 20, 0), 15), ((1, 19, 4), 13), ((1, 19, 3), 12), ((1, 19, 0), 9),
           ((1, 18, 0), 8), ((1, 17, 0), 7), ((1, 16, 2), 6), ((1, 15, 0), 5), ((1, 13, 0), 4),
           ((1, 11, 0), 3), ((1, 9, 0), 2), ((1, 0, 0), 1)]
NAMES9 = ["alex", "ari", "efe", "kai", "makena", "noor", "steve", "sunny", "zuri"]


def _vt(mc):
    m = re.fullmatch(r"1\.(\d+)(?:\.(\d+))?", mc)
    return (1, int(m[1]), int(m[2] or 0)) if m else None


def pack_format(mc):
    t = _vt(mc)
    if t is None:
        return 75
    return next(f for v, f in FORMATS if t >= v)


def skin_paths(mc):
    t = _vt(mc)
    if t is None or t >= (1, 19, 3):
        return [f"assets/minecraft/textures/entity/player/{k}/{n}.png" for k in ("wide", "slim") for n in NAMES9]
    return ["assets/minecraft/textures/entity/steve.png", "assets/minecraft/textures/entity/alex.png",
            "assets/minecraft/textures/mob/char.png"]


def _set_option(opts, fmt, enable):
    old = ("LuminaSkin.zip", "file/LuminaSkin.zip")
    new = "file/" + PACK_NAME if fmt >= 4 else PACK_NAME
    lines = opts.read_text(encoding="utf-8").splitlines() if opts.exists() else []
    idx = next((i for i, l in enumerate(lines) if l.startswith("resourcePacks:")), None)
    if idx is None and not enable:
        return
    cur = []
    if idx is not None:
        raw = lines[idx].split(":", 1)[1].strip()
        try:
            cur = json.loads(raw)
        except Exception:
            cur = [x.strip().strip('"') for x in raw.strip("[]").split(",") if x.strip()]
    cur = [c for c in cur if c not in old]
    if enable:
        if not cur and fmt >= 4:
            cur = ["vanilla"]
        cur.append(new)
    line = "resourcePacks:" + json.dumps(cur, separators=(",", ":"))
    if idx is None:
        lines.append(line)
    else:
        lines[idx] = line
    opts.parent.mkdir(parents=True, exist_ok=True)
    opts.write_text("\n".join(lines) + "\n", encoding="utf-8")


def apply(name, mc, gamedir):
    """Called by the launcher before start: injects (or removes) the skin pack."""
    gamedir = Path(gamedir)
    pack = gamedir / "resourcepacks" / PACK_NAME
    fmt = pack_format(mc)
    img = load_skin(name)
    if img is None:
        pack.unlink(missing_ok=True)
        _set_option(gamedir / "options.txt", fmt, False)
        return False
    buf = io.BytesIO()
    img.save(buf, "PNG")
    png = buf.getvalue()
    pack.parent.mkdir(parents=True, exist_ok=True)
    meta = {"pack": {"pack_format": fmt, "description": f"Lumina skin: {name}",
                     "supported_formats": {"min_inclusive": 1, "max_inclusive": 999}}}
    with zipfile.ZipFile(pack, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("pack.mcmeta", json.dumps(meta))
        for p in skin_paths(mc):
            z.writestr(p, png)
    _set_option(gamedir / "options.txt", fmt, True)
    return True


# ---------------------------------------------------------------- editor UI
REGIONS = [  # name, x, y, w, h, overlay?
    ("голова", 0, 0, 32, 16, False), ("шлем", 32, 0, 32, 16, True),
    ("п.нога", 0, 16, 16, 16, False), ("тело", 16, 16, 24, 16, False), ("п.рука", 40, 16, 16, 16, False),
    ("п.штаны", 0, 32, 16, 16, True), ("куртка", 16, 32, 24, 16, True), ("п.рукав", 40, 32, 16, 16, True),
    ("л.нога", 16, 48, 16, 16, False), ("л.рука", 32, 48, 16, 16, False),
    ("л.штаны", 0, 48, 16, 16, True), ("л.рукав", 48, 48, 16, 16, True),
]


class SkinEditor(tk.Toplevel):
    SC = 8

    def __init__(self, parent, name, uuid_str, on_saved=None):
        super().__init__(parent)
        self.title(f"Скин: {name}")
        self.configure(bg=BG)
        self.resizable(False, False)
        self.name, self.on_saved = name, on_saved
        self.img = load_skin(name) or Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.slim = tk.BooleanVar(value=load_model(name, default_slim(uuid_str)))
        self.tool = tk.StringVar(value="pencil")
        self.guides = tk.BooleanVar(value=True)
        self.color = (124, 92, 255, 255)
        self.undo, self.last = [], None
        self.nick = tk.StringVar()
        S = self.SC
        self.checker = Image.new("RGBA", (64 * S, 64 * S))
        d = ImageDraw.Draw(self.checker)
        for y in range(64):
            for x in range(64):
                c = (58, 58, 74, 255) if (x + y) % 2 else (46, 46, 60, 255)
                d.rectangle((x * S, y * S, x * S + S - 1, y * S + S - 1), fill=c)

        left = ttk.Frame(self, padding=10)
        left.pack(side="left")
        self.canvas = tk.Canvas(left, width=64 * S, height=64 * S, bg=BG, highlightthickness=0, cursor="crosshair")
        self.canvas.pack()
        self.cimg = self.canvas.create_image(0, 0, anchor="nw")
        self.canvas.bind("<Button-1>", lambda e: self.press(e, False))
        self.canvas.bind("<B1-Motion>", lambda e: self.drag(e, False))
        self.canvas.bind("<Button-3>", lambda e: self.press(e, True))
        self.canvas.bind("<B3-Motion>", lambda e: self.drag(e, True))
        self.canvas.bind("<ButtonRelease>", lambda e: setattr(self, "last", None))
        self.bind("<Control-z>", lambda e: self.undo_step())

        right = ttk.Frame(self, style="Card.TFrame", padding=12)
        right.pack(side="left", fill="y", padx=(0, 10), pady=10)
        pv = ttk.Frame(right, style="Card.TFrame")
        pv.pack()
        self.pv = [tk.Canvas(pv, width=112, height=224, bg=PANEL, highlightthickness=0) for _ in range(2)]
        for c in self.pv:
            c.pack(side="left", padx=4)
        self.pv_img = [None, None]

        ttk.Label(right, text="Инструмент (ПКМ — ластик)", style="Dim.TLabel").pack(anchor="w", pady=(10, 2))
        tl = ttk.Frame(right, style="Card.TFrame")
        tl.pack(fill="x")
        for v, t in (("pencil", "Кисть"), ("eraser", "Ластик"), ("fill", "Заливка"), ("picker", "Пипетка")):
            ttk.Radiobutton(tl, text=t, value=v, variable=self.tool).pack(side="left", padx=(0, 6))
        self.swatch = tk.Button(right, text="Цвет", command=self.pick_color, relief="flat", bd=0)
        self.swatch.pack(fill="x", pady=6)
        self.upd_swatch()

        ttk.Label(right, text="Модель", style="Dim.TLabel").pack(anchor="w")
        ml = ttk.Frame(right, style="Card.TFrame")
        ml.pack(fill="x")
        ttk.Radiobutton(ml, text="Steve (руки 4px)", value=False, variable=self.slim, command=self.redraw).pack(anchor="w")
        ttk.Radiobutton(ml, text="Alex (руки 3px)", value=True, variable=self.slim, command=self.redraw).pack(anchor="w")
        ttk.Checkbutton(right, text="Показать зоны", variable=self.guides, command=self.redraw).pack(anchor="w", pady=(6, 0))

        row = ttk.Frame(right, style="Card.TFrame")
        row.pack(fill="x", pady=(10, 0))
        ttk.Button(row, text="Импорт PNG", command=self.import_png).pack(side="left", expand=True, fill="x", padx=(0, 3))
        ttk.Button(row, text="Экспорт", command=self.export_png).pack(side="left", expand=True, fill="x")
        row = ttk.Frame(right, style="Card.TFrame")
        row.pack(fill="x", pady=(6, 0))
        ttk.Entry(row, textvariable=self.nick, width=12).pack(side="left", padx=(0, 3))
        ttk.Button(row, text="Взять скин по нику", command=self.by_name).pack(side="left", expand=True, fill="x")
        row = ttk.Frame(right, style="Card.TFrame")
        row.pack(fill="x", pady=(6, 0))
        ttk.Button(row, text="Отменить (Ctrl+Z)", command=self.undo_step).pack(side="left", expand=True, fill="x", padx=(0, 3))
        ttk.Button(row, text="Очистить", command=self.clear).pack(side="left", expand=True, fill="x")
        ttk.Button(right, text="Сохранить", style="Play.TButton", command=self.save).pack(fill="x", pady=(12, 4))
        ttk.Button(right, text="Убрать скин (по умолчанию)", command=self.remove).pack(fill="x")
        ttk.Label(right, text="Скин виден только тебе (offline).\nПрименится при следующем запуске.",
                  style="Dim.TLabel", justify="left").pack(anchor="w", pady=(8, 0))
        self.redraw()

    # --- drawing
    def redraw(self):
        S = self.SC
        base = self.checker.copy()
        base.alpha_composite(self.img.resize((64 * S, 64 * S), Image.NEAREST))
        if self.guides.get():
            d = ImageDraw.Draw(base)
            for n, x, y, w, h, ov in REGIONS:
                col = (255, 190, 0, 255) if ov else (255, 255, 255, 255)
                d.rectangle((x * S, y * S, (x + w) * S - 1, (y + h) * S - 1), outline=col)
                d.text((x * S + 3, y * S + 2), n, fill=col)
        self.tkimg = ImageTk.PhotoImage(base)
        self.canvas.itemconfig(self.cimg, image=self.tkimg)
        for i, back in enumerate((False, True)):
            bg = Image.new("RGBA", (112, 224), (27, 27, 40, 255))
            bg.alpha_composite(preview(self.img, self.slim.get(), back).resize((112, 224), Image.NEAREST))
            self.pv_img[i] = ImageTk.PhotoImage(bg)
            self.pv[i].delete("all")
            self.pv[i].create_image(0, 0, anchor="nw", image=self.pv_img[i])

    def upd_swatch(self):
        r, g, b, a = self.color
        self.swatch.configure(bg="#%02x%02x%02x" % (r, g, b), fg="#000" if r + g + b > 380 else "#fff")

    def pick_color(self):
        c = colorchooser.askcolor(color="#%02x%02x%02x" % self.color[:3], parent=self)[0]
        if c:
            self.color = (int(c[0]), int(c[1]), int(c[2]), 255)
            self.upd_swatch()

    def push(self):
        self.undo.append(self.img.copy())
        del self.undo[:-40]

    def undo_step(self):
        if self.undo:
            self.img = self.undo.pop()
            self.redraw()

    def cell(self, e):
        x, y = e.x // self.SC, e.y // self.SC
        return (x, y) if 0 <= x < 64 and 0 <= y < 64 else None

    def press(self, e, erase):
        p = self.cell(e)
        if not p:
            return
        self.push()
        t = "eraser" if erase else self.tool.get()
        if t == "fill":
            self.flood(p)
        elif t == "picker":
            c = self.img.getpixel(p)
            if c[3]:
                self.color = c
                self.upd_swatch()
        else:
            self.dot(p, t)
            self.last = p
        self.redraw()

    def drag(self, e, erase):
        p = self.cell(e)
        t = "eraser" if erase else self.tool.get()
        if not p or t not in ("pencil", "eraser"):
            return
        if self.last and self.last != p:
            (x0, y0), (x1, y1) = self.last, p
            n = max(abs(x1 - x0), abs(y1 - y0))
            for i in range(1, n + 1):
                self.dot((round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n)), t)
        else:
            self.dot(p, t)
        self.last = p
        self.redraw()

    def dot(self, p, tool):
        self.img.putpixel(p, (0, 0, 0, 0) if tool == "eraser" else self.color)

    def flood(self, p):
        px = self.img.load()
        target = px[p]
        if target == self.color:
            return
        stack = [p]
        while stack:
            x, y = stack.pop()
            if 0 <= x < 64 and 0 <= y < 64 and px[x, y] == target:
                px[x, y] = self.color
                stack += [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]

    # --- actions
    def set_image(self, img, slim=None):
        self.push()
        self.img = img
        if slim is not None:
            self.slim.set(slim)
        self.redraw()

    def import_png(self):
        f = filedialog.askopenfilename(parent=self, filetypes=[("PNG", "*.png")])
        if not f:
            return
        try:
            img = normalize(Image.open(f))
            self.set_image(img, guess_slim(img))
        except Exception as e:
            messagebox.showerror("Импорт", str(e), parent=self)

    def export_png(self):
        f = filedialog.asksaveasfilename(parent=self, defaultextension=".png", initialfile=f"{self.name}.png",
                                         filetypes=[("PNG", "*.png")])
        if f:
            self.img.save(f)

    def by_name(self):
        n = self.nick.get().strip()
        if not n:
            return

        def work():
            try:
                img, slim = fetch_by_name(n)
                self.after(0, lambda: self.set_image(img, slim))
            except Exception as e:
                self.after(0, lambda: messagebox.showerror("Скин по нику", str(e), parent=self))
        threading.Thread(target=work, daemon=True).start()

    def clear(self):
        self.set_image(Image.new("RGBA", (64, 64), (0, 0, 0, 0)))

    def save(self):
        save_skin(self.name, self.img, self.slim.get())
        if self.on_saved:
            self.on_saved()
        self.destroy()

    def remove(self):
        delete_skin(self.name)
        if self.on_saved:
            self.on_saved()
        self.destroy()
