#!/usr/bin/env python3
"""Lumina Launcher: all Minecraft versions, Fabric, Lumina Client, offline accounts."""
import hashlib, json, os, platform, re, shutil, subprocess, sys, tarfile, threading, uuid, zipfile
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import tkinter as tk
from tkinter import messagebox, simpledialog, ttk

import requests

import skins

# ---------- platform / paths ----------
SYS = platform.system()
OS = {"Windows": "windows", "Darwin": "osx"}.get(SYS, "linux")
MACH = platform.machine().lower()
ARM = MACH in ("arm64", "aarch64")
IS32 = (not ARM) and MACH in ("x86", "i386", "i686")
ARCH_RULE = "x86" if IS32 else ("arm64" if ARM else "x86_64")
if OS == "windows":
    BASE = Path(os.getenv("APPDATA", Path.home())) / "LuminaLauncher"
elif OS == "osx":
    BASE = Path.home() / "Library/Application Support/LuminaLauncher"
else:
    BASE = Path.home() / ".local/share/LuminaLauncher"
BASE.mkdir(parents=True, exist_ok=True)
skins.SKIN_DIR = BASE / "skins"
APP_DIR = Path(sys.executable).parent if getattr(sys, "frozen", False) else Path(__file__).resolve().parent
QUICK_VERSIONS = ["1.21.11", "1.8.9"]
MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

S = requests.Session()
S.headers["User-Agent"] = "LuminaLauncher/1.0"


# ---------- helpers ----------
def sha1_of(p):
    h = hashlib.sha1()
    with open(p, "rb") as f:
        for c in iter(lambda: f.read(1 << 20), b""):
            h.update(c)
    return h.hexdigest()


def fetch(url, dest, sha1=None):
    dest = Path(dest)
    if dest.exists() and (not sha1 or sha1_of(dest) == sha1):
        return
    dest.parent.mkdir(parents=True, exist_ok=True)
    err = None
    for _ in range(3):
        try:
            tmp = dest.with_name(dest.name + ".part")
            with S.get(url, stream=True, timeout=30) as r:
                r.raise_for_status()
                with open(tmp, "wb") as f:
                    for c in r.iter_content(1 << 16):
                        f.write(c)
            tmp.replace(dest)
            return
        except Exception as e:
            err = e
    raise err


def get_json(url):
    r = S.get(url, timeout=30)
    r.raise_for_status()
    return r.json()


def rules_ok(rules, feats=None):
    if not rules:
        return True
    allowed = False
    for r in rules:
        m = True
        o = r.get("os")
        if o:
            if "name" in o and o["name"] != OS:
                m = False
            if "arch" in o and o["arch"] != ARCH_RULE:
                m = False
            if "version" in o and not re.search(o["version"], platform.version()):
                m = False
        for k, v in (r.get("features") or {}).items():
            if (feats or {}).get(k, False) != v:
                m = False
        if m:
            allowed = r["action"] == "allow"
    return allowed


def offline_uuid(name):
    return str(uuid.UUID(bytes=hashlib.md5(f"OfflinePlayer:{name}".encode()).digest(), version=3))


# ---------- Java ----------
def java_major(path):
    try:
        out = subprocess.run([str(path), "-version"], capture_output=True, text=True, timeout=15).stderr
        m = re.search(r'version "(\d+)(?:\.(\d+))?', out)
        if not m:
            return 0
        return int(m.group(2)) if m.group(1) == "1" else int(m.group(1))
    except Exception:
        return 0


def find_java_in(d):
    exe = "java.exe" if OS == "windows" else "java"
    for p in Path(d).rglob(exe):
        if p.parent.name == "bin":
            return p
    return None


def ensure_java(major, custom, log):
    if custom and Path(custom).exists() and java_major(custom) >= major:
        return str(custom)
    rt = BASE / "runtime" / str(major)
    j = find_java_in(rt) if rt.exists() else None
    if j:
        return str(j)
    sysj = shutil.which("java")
    if sysj and java_major(sysj) == major:
        return sysj
    if IS32:
        raise RuntimeError(f"Нужна Java {major}, укажи путь к ней в настройках.")
    log(f"Скачиваю Java {major}...")
    ao = {"windows": "windows", "osx": "mac", "linux": "linux"}[OS]
    arch = "aarch64" if ARM else "x64"
    url = f"https://api.adoptium.net/v3/binary/latest/{major}/ga/{ao}/{arch}/jre/hotspot/normal/eclipse"
    arc = BASE / "runtime" / f"{major}.archive"
    arc.parent.mkdir(parents=True, exist_ok=True)
    fetch(url, arc)
    rt.mkdir(parents=True, exist_ok=True)
    if zipfile.is_zipfile(arc):
        zipfile.ZipFile(arc).extractall(rt)
    else:
        tarfile.open(arc).extractall(rt)
    arc.unlink()
    j = find_java_in(rt)
    if not j:
        raise RuntimeError("Не удалось распаковать Java")
    if OS != "windows":
        j.chmod(0o755)
    return str(j)


# ---------- versions ----------
def load_manifest():
    mf = BASE / "manifest.json"
    try:
        data = get_json(MANIFEST_URL)
        mf.write_text(json.dumps(data))
        return data
    except Exception:
        if mf.exists():
            return json.loads(mf.read_text())
        raise


def vjson_path(vid):
    return BASE / "versions" / vid / f"{vid}.json"


def ensure_vanilla_json(vid, manifest):
    p = vjson_path(vid)
    if p.exists():
        return
    ent = next((v for v in manifest["versions"] if v["id"] == vid), None)
    if not ent:
        raise RuntimeError(f"Версия {vid} не найдена")
    fetch(ent["url"], p)


def is_legacy(mc):
    m = re.fullmatch(r"1\.(\d+)(?:\.\d+)?", mc)
    return bool(m) and int(m.group(1)) < 14  # Legacy Fabric covers 1.8.9 .. 1.13.2


def fabric_meta(mc):
    return "https://meta.legacyfabric.net/v2" if is_legacy(mc) else "https://meta.fabricmc.net/v2"


def install_fabric(mc, manifest):
    ensure_vanilla_json(mc, manifest)
    meta = fabric_meta(mc)
    loaders = get_json(f"{meta}/versions/loader/{mc}")
    if not loaders:
        raise RuntimeError(f"Fabric пока не поддерживает {mc}")
    lv = next((l for l in loaders if l["loader"].get("stable")), loaders[0])["loader"]["version"]
    prof = get_json(f"{meta}/versions/loader/{mc}/{lv}/profile/json")
    vid = prof["id"]
    p = vjson_path(vid)
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(prof))
    return vid


def resolve(vid):
    j = json.loads(vjson_path(vid).read_text())
    if "inheritsFrom" not in j:
        j["jar"] = j.get("jar", vid)
        return j
    par = resolve(j["inheritsFrom"])
    m = dict(par)
    for k, v in j.items():
        if k == "libraries":
            m[k] = v + par.get("libraries", [])
        elif k == "arguments":
            pa = par.get("arguments", {})
            m[k] = {"game": pa.get("game", []) + v.get("game", []), "jvm": pa.get("jvm", []) + v.get("jvm", [])}
        else:
            m[k] = v
    m["jar"] = par["jar"]
    return m


def collect_libs(libs):
    """Returns list of (url, relpath, sha1, on_classpath, extract_natives)."""
    out, seen = [], set()
    for l in libs:
        if not rules_ok(l.get("rules")):
            continue
        parts = l["name"].split(":")
        key = ":".join(parts[:2]) + (":" + parts[3] if len(parts) > 3 else "")
        if key in seen:
            continue
        seen.add(key)
        dl = l.get("downloads")
        if dl is None:
            g, a, v = parts[:3]
            rel = f"{g.replace('.', '/')}/{a}/{v}/{a}-{v}.jar"
            out.append((l.get("url", "https://libraries.minecraft.net/") + rel, rel, None, True, False))
            continue
        art = dl.get("artifact")
        if art and art.get("url"):
            out.append((art["url"], art["path"], art.get("sha1"), True, ":natives-" in l["name"]))
        if "natives" in l and OS in l["natives"]:
            cl = l["natives"][OS].replace("${arch}", "32" if IS32 else "64")
            c = dl.get("classifiers", {}).get(cl)
            if c:
                out.append((c["url"], c["path"], c.get("sha1"), False, True))
    return out


def install_game(vid, mc_id_for_log, prog, log):
    """Download everything needed. Returns resolved json."""
    v = resolve(vid)
    jar_id = v["jar"]
    jar = BASE / "versions" / jar_id / f"{jar_id}.jar"
    log("Скачиваю клиент...")
    fetch(v["downloads"]["client"]["url"], jar, v["downloads"]["client"].get("sha1"))
    libs = collect_libs(v["libraries"])
    ai = v["assetIndex"]
    idx_path = BASE / "assets" / "indexes" / f"{ai['id']}.json"
    fetch(ai["url"], idx_path, ai.get("sha1"))
    idx = json.loads(idx_path.read_text())
    objs = list(idx["objects"].values())
    total = len(libs) + len(objs)
    done = [0]
    lock = threading.Lock()

    def tick():
        with lock:
            done[0] += 1
            if done[0] % 25 == 0 or done[0] == total:
                prog(done[0] / total, f"Файлы: {done[0]}/{total}")

    def dl_lib(x):
        fetch(x[0], BASE / "libraries" / x[1], x[2]); tick()

    def dl_obj(o):
        h = o["hash"]
        fetch(f"https://resources.download.minecraft.net/{h[:2]}/{h}", BASE / "assets" / "objects" / h[:2] / h); tick()

    log("Скачиваю библиотеки и ресурсы...")
    with ThreadPoolExecutor(16) as ex:
        list(ex.map(dl_lib, libs))
        list(ex.map(dl_obj, objs))
    # legacy assets layout
    if idx.get("virtual") or idx.get("map_to_resources"):
        root = BASE / "assets" / "virtual" / "legacy"
        for name, o in idx["objects"].items():
            src = BASE / "assets" / "objects" / o["hash"][:2] / o["hash"]
            dst = root / name
            if not dst.exists():
                dst.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(src, dst)
    v["_libs"] = libs
    v["_jar"] = jar
    v["_idx"] = idx
    return v


def extract_natives(v, vid):
    nd = BASE / "natives" / vid
    if nd.exists():
        shutil.rmtree(nd, ignore_errors=True)
    nd.mkdir(parents=True)
    for url, rel, sha, cp, nat in v["_libs"]:
        if not nat:
            continue
        with zipfile.ZipFile(BASE / "libraries" / rel) as z:
            for n in z.namelist():
                if n.lower().endswith((".dll", ".so", ".dylib", ".jnilib")) and not n.startswith("META-INF"):
                    (nd / Path(n).name).write_bytes(z.read(n))
    return nd


def build_command(v, vid, java, acc, gamedir, ram_mb, nd):
    sep = ";" if OS == "windows" else ":"
    cp = [str(BASE / "libraries" / rel) for _, rel, _, onc, _ in v["_libs"] if onc] + [str(v["_jar"])]
    asset_root = str(BASE / "assets")
    if v["_idx"].get("virtual"):
        game_assets = str(BASE / "assets" / "virtual" / "legacy")
    elif v["_idx"].get("map_to_resources"):
        game_assets = str(gamedir / "resources")
        shutil.copytree(BASE / "assets" / "virtual" / "legacy", game_assets, dirs_exist_ok=True)
    else:
        game_assets = asset_root
    vars_ = {
        "auth_player_name": acc["name"], "version_name": vid, "game_directory": str(gamedir),
        "assets_root": asset_root, "assets_index_name": v["assetIndex"]["id"],
        "auth_uuid": acc["uuid"], "auth_access_token": "0", "clientid": "0", "auth_xuid": "0",
        "user_type": "legacy", "version_type": v.get("type", "release"), "natives_directory": str(nd),
        "launcher_name": "LuminaLauncher", "launcher_version": "1.0", "classpath": sep.join(cp),
        "library_directory": str(BASE / "libraries"), "classpath_separator": sep,
        "user_properties": "{}", "game_assets": game_assets, "auth_session": "0",
    }

    def sub(s):
        return re.sub(r"\$\{(\w+)\}", lambda m: vars_.get(m.group(1), m.group(0)), s)

    def expand(args):
        res = []
        for a in args:
            if isinstance(a, str):
                res.append(sub(a))
            elif rules_ok(a.get("rules")):
                val = a["value"]
                res += [sub(x) for x in (val if isinstance(val, list) else [val])]
        return res

    if "arguments" in v:
        jvm = expand(v["arguments"].get("jvm", []))
        game = expand(v["arguments"].get("game", []))
    else:
        jvm = [f"-Djava.library.path={nd}", "-cp", vars_["classpath"]]
        game = [sub(x) for x in v["minecraftArguments"].split()]
    return [java, f"-Xmx{ram_mb}M", f"-Xms{min(512, ram_mb)}M"] + jvm + [v["mainClass"]] + game


def get_fabric_api(mc):
    slug = "legacy-fabric-api" if is_legacy(mc) else "fabric-api"
    vs = S.get(f"https://api.modrinth.com/v2/project/{slug}/version",
               params={"game_versions": json.dumps([mc]), "loaders": json.dumps(["fabric"])}, timeout=30).json()
    if not vs:
        raise RuntimeError(f"{slug} для {mc} не найден")
    files = vs[0]["files"]
    f = next((x for x in files if x.get("primary")), files[0])
    dst = BASE / "cache" / f["filename"]
    fetch(f["url"], dst, f["hashes"].get("sha1"))
    return dst


def vt(s):
    return tuple(int(x) for x in re.findall(r"\d+", s))


def find_lumina_jar(mc):
    """client/ jars are named like lumina-1.0.0+mc1.21.11.jar or lumina-1.0.0+mc1.14-1.21.11.jar"""
    if not re.fullmatch(r"1\.\d+(\.\d+)?", mc):
        return None
    cur = vt(mc)
    for d in (APP_DIR / "client", BASE / "client"):
        if not d.exists():
            continue
        for j in sorted(d.glob("lumina*.jar"), reverse=True):
            if "sources" in j.name or "-dev" in j.name:
                continue
            m = re.search(r"mc(\d+(?:\.\d+)*)(?:-(\d+(?:\.\d+)*))?", j.name)
            if m and vt(m.group(1)) <= cur <= vt(m.group(2) or m.group(1)):
                return j
    return None


# ---------- UI ----------
BG, PANEL, FIELD, ACC, FG, DIM = "#12121a", "#1b1b28", "#25253a", "#7c5cff", "#ffffff", "#9a9ab0"


class App:
    def __init__(self, root):
        self.root = root
        root.title("Lumina Launcher")
        root.geometry("780x520")
        root.configure(bg=BG)
        self.cfg_path = BASE / "settings.json"
        self.cfg = {"accounts": [], "selected": 0, "ram": 4096, "java": "", "version": "", "loader": "vanilla"}
        if self.cfg_path.exists():
            self.cfg.update(json.loads(self.cfg_path.read_text()))
        self.manifest = None
        self.busy = False
        self.style()
        self.build()
        threading.Thread(target=self.load_versions, daemon=True).start()

    def style(self):
        s = ttk.Style()
        s.theme_use("clam")
        s.configure(".", background=BG, foreground=FG, fieldbackground=FIELD, bordercolor=BG, lightcolor=BG, darkcolor=BG)
        s.configure("TFrame", background=BG)
        s.configure("Card.TFrame", background=PANEL)
        s.configure("TLabel", background=BG, foreground=FG)
        s.configure("Card.TLabel", background=PANEL)
        s.configure("Dim.TLabel", background=PANEL, foreground=DIM)
        s.configure("TCheckbutton", background=PANEL, foreground=FG)
        s.configure("TRadiobutton", background=PANEL, foreground=FG)
        s.map("TCheckbutton", background=[("active", PANEL)])
        s.map("TRadiobutton", background=[("active", PANEL)])
        s.configure("TButton", background=FIELD, foreground=FG, padding=6, borderwidth=0)
        s.map("TButton", background=[("active", "#33334d")])
        s.configure("Play.TButton", background=ACC, font=("Segoe UI", 13, "bold"), padding=10)
        s.map("Play.TButton", background=[("active", "#907aff"), ("disabled", "#3a3a55")])
        s.configure("TCombobox", fieldbackground=FIELD, background=FIELD, foreground=FG, arrowcolor=FG)
        s.configure("TEntry", fieldbackground=FIELD, foreground=FG)
        s.configure("TSpinbox", fieldbackground=FIELD, foreground=FG, arrowcolor=FG)
        s.configure("Horizontal.TProgressbar", background=ACC, troughcolor=FIELD, borderwidth=0)

    def build(self):
        r = self.root
        left = ttk.Frame(r, style="Card.TFrame", padding=12)
        left.pack(side="left", fill="y", padx=(12, 6), pady=12)
        ttk.Label(left, text="LUMINA", style="Card.TLabel", foreground=ACC, font=("Segoe UI", 16, "bold")).pack(anchor="w")
        ttk.Label(left, text="Аккаунты (offline)", style="Dim.TLabel").pack(anchor="w", pady=(10, 4))
        self.acc_list = tk.Listbox(left, bg=FIELD, fg=FG, selectbackground=ACC, bd=0, highlightthickness=0,
                                   exportselection=False, width=22, height=10, font=("Segoe UI", 10))
        self.acc_list.pack(fill="x")
        self.acc_list.bind("<<ListboxSelect>>", lambda e: self.save())
        self.nick = tk.StringVar()
        ttk.Entry(left, textvariable=self.nick).pack(fill="x", pady=(8, 4))
        row = ttk.Frame(left, style="Card.TFrame")
        row.pack(fill="x")
        ttk.Button(row, text="Добавить", command=self.add_acc).pack(side="left", expand=True, fill="x", padx=(0, 3))
        ttk.Button(row, text="Удалить", command=self.del_acc).pack(side="left", expand=True, fill="x")
        ttk.Button(left, text="Скин…", command=self.edit_skin).pack(fill="x", pady=(6, 0))
        self.refresh_accs()

        right = ttk.Frame(r, style="Card.TFrame", padding=14)
        right.pack(side="left", fill="both", expand=True, padx=(6, 12), pady=12)
        qp = ttk.Frame(right, style="Card.TFrame")
        qp.pack(fill="x", pady=(0, 10))
        self.quick_btns = {}
        for mc in QUICK_VERSIONS:
            b = ttk.Button(qp, text=f"Lumina {mc}", style="Play.TButton", command=lambda v=mc: self.quick(v))
            b.pack(side="left", expand=True, fill="x", padx=(0, 4))
            self.quick_btns[mc] = b
        self.refresh_quick()
        ttk.Label(right, text="Версия Minecraft", style="Card.TLabel").pack(anchor="w")
        self.ver = tk.StringVar(value=self.cfg["version"])
        self.combo = ttk.Combobox(right, textvariable=self.ver, state="readonly", height=15)
        self.combo.pack(fill="x", pady=(2, 6))
        self.combo.bind("<<ComboboxSelected>>", lambda e: self.on_ver())
        f = ttk.Frame(right, style="Card.TFrame")
        f.pack(fill="x")
        self.show = {k: tk.BooleanVar(value=(k == "release")) for k in ("release", "snapshot", "old_beta", "old_alpha")}
        for k, t in (("release", "Релизы"), ("snapshot", "Снапшоты"), ("old_beta", "Beta"), ("old_alpha", "Alpha")):
            ttk.Checkbutton(f, text=t, variable=self.show[k], command=self.fill_versions).pack(side="left", padx=(0, 8))

        ttk.Label(right, text="Загрузчик", style="Card.TLabel").pack(anchor="w", pady=(12, 2))
        self.loader = tk.StringVar(value=self.cfg["loader"])
        self.rb = {}
        for val, t in (("vanilla", "Vanilla"), ("fabric", "Fabric / Legacy Fabric (1.8.9–1.13.2)"), ("lumina", "Fabric + Lumina Client")):
            self.rb[val] = ttk.Radiobutton(right, text=t, value=val, variable=self.loader, command=self.save)
            self.rb[val].pack(anchor="w")

        g = ttk.Frame(right, style="Card.TFrame")
        g.pack(fill="x", pady=(12, 0))
        ttk.Label(g, text="Память (МБ)", style="Card.TLabel").grid(row=0, column=0, sticky="w")
        self.ram = tk.IntVar(value=self.cfg["ram"])
        ttk.Spinbox(g, from_=1024, to=32768, increment=512, textvariable=self.ram, width=8).grid(row=0, column=1, padx=8)
        ttk.Label(g, text="Java (пусто = авто)", style="Card.TLabel").grid(row=1, column=0, sticky="w", pady=(6, 0))
        self.java = tk.StringVar(value=self.cfg["java"])
        ttk.Entry(g, textvariable=self.java, width=34).grid(row=1, column=1, padx=8, pady=(6, 0))

        self.play = ttk.Button(right, text="ИГРАТЬ", style="Play.TButton", command=self.on_play)
        self.play.pack(fill="x", pady=(16, 6))
        self.bar = ttk.Progressbar(right, maximum=1.0)
        self.bar.pack(fill="x")
        self.status = tk.StringVar(value="Загружаю список версий...")
        ttk.Label(right, textvariable=self.status, style="Dim.TLabel").pack(anchor="w", pady=(4, 0))
        self.logbox = tk.Text(right, height=6, bg=FIELD, fg=DIM, bd=0, highlightthickness=0, font=("Consolas", 9))
        self.logbox.pack(fill="both", expand=True, pady=(6, 0))

    # --- state ---
    def save(self):
        sel = self.acc_list.curselection()
        if sel:
            self.cfg["selected"] = sel[0]
        try:
            self.cfg["ram"] = int(self.ram.get())
        except Exception:
            pass
        self.cfg.update(java=self.java.get().strip(), version=self.ver.get(), loader=self.loader.get())
        self.cfg_path.write_text(json.dumps(self.cfg, indent=2))

    def ui(self, fn):
        self.root.after(0, fn)

    def log(self, t):
        def f():
            self.logbox.insert("end", t + "\n"); self.logbox.see("end"); self.status.set(t)
        self.ui(f)

    def prog(self, frac, t):
        self.ui(lambda: (self.bar.configure(value=frac), self.status.set(t)))

    # --- accounts ---
    def refresh_accs(self):
        self.acc_list.delete(0, "end")
        for a in self.cfg["accounts"]:
            self.acc_list.insert("end", a["name"])
        if self.cfg["accounts"]:
            i = min(self.cfg["selected"], len(self.cfg["accounts"]) - 1)
            self.acc_list.selection_set(i)

    def add_acc(self):
        n = self.nick.get().strip()
        if not re.fullmatch(r"[A-Za-z0-9_]{3,16}", n):
            return messagebox.showwarning("Ник", "3–16 символов: латиница, цифры, _")
        if any(a["name"].lower() == n.lower() for a in self.cfg["accounts"]):
            return
        self.cfg["accounts"].append({"name": n, "uuid": offline_uuid(n)})
        self.cfg["selected"] = len(self.cfg["accounts"]) - 1
        self.nick.set("")
        self.refresh_accs(); self.save()

    def del_acc(self):
        sel = self.acc_list.curselection()
        if sel:
            del self.cfg["accounts"][sel[0]]
            self.cfg["selected"] = 0
            self.refresh_accs(); self.save()

    # --- versions ---
    def load_versions(self):
        try:
            self.manifest = load_manifest()
            self.ui(self.fill_versions)
            self.log("Готово")
        except Exception as e:
            self.log(f"Нет списка версий: {e}")

    def fill_versions(self):
        if not self.manifest:
            return
        types = {k for k, v in self.show.items() if v.get()}
        vs = [v["id"] for v in self.manifest["versions"] if v["type"] in types]
        self.combo["values"] = vs
        if self.ver.get() not in vs and vs:
            self.ver.set(vs[0])
        self.on_ver()

    def on_ver(self):
        ok = bool(self.ver.get()) and find_lumina_jar(self.ver.get()) is not None
        self.rb["lumina"].configure(state="normal" if ok else "disabled")
        if not ok and self.loader.get() == "lumina":
            self.loader.set("fabric")
        self.save()

    # --- skins ---
    def edit_skin(self):
        sel = self.acc_list.curselection()
        if not sel:
            return messagebox.showinfo("Скин", "Сначала выбери аккаунт.")
        a = self.cfg["accounts"][sel[0]]
        skins.SkinEditor(self.root, a["name"], a["uuid"])

    # --- quick play ---
    def refresh_quick(self):
        for mc, b in self.quick_btns.items():
            has = find_lumina_jar(mc) is not None
            b.configure(state="normal" if has else "disabled",
                        text=f"Lumina {mc}" if has else f"Lumina {mc} (нет jar)")

    def quick(self, mc):
        if self.busy or not self.manifest:
            return
        self.ver.set(mc)
        self.loader.set("lumina")
        self.on_ver()
        self.on_play()

    def ask_nick(self):
        while True:
            n = simpledialog.askstring("Добро пожаловать", "Введи ник для игры:", parent=self.root)
            if n is None:
                return False
            n = n.strip()
            if re.fullmatch(r"[A-Za-z0-9_]{3,16}", n):
                self.cfg["accounts"].append({"name": n, "uuid": offline_uuid(n)})
                self.cfg["selected"] = len(self.cfg["accounts"]) - 1
                self.refresh_accs(); self.save()
                return True
            messagebox.showwarning("Ник", "3–16 символов: латиница, цифры, _")

    # --- play ---
    def on_play(self):
        if self.busy:
            return
        if not self.cfg["accounts"] and not self.ask_nick():
            return
        if not self.ver.get() or not self.manifest:
            return
        self.save()
        self.busy = True
        self.play.configure(state="disabled")
        threading.Thread(target=self.run, daemon=True).start()

    def run(self):
        try:
            mc, loader = self.ver.get(), self.loader.get()
            acc = self.cfg["accounts"][min(self.cfg["selected"], len(self.cfg["accounts"]) - 1)]
            if loader == "vanilla":
                ensure_vanilla_json(mc, self.manifest)
                vid = mc
            else:
                self.log("Устанавливаю Fabric...")
                vid = install_fabric(mc, self.manifest)
            v = install_game(vid, mc, self.prog, self.log)
            major = v.get("javaVersion", {}).get("majorVersion", 8)
            java = ensure_java(major, self.cfg["java"], self.log)
            nd = extract_natives(v, vid)
            gd = BASE / "instances" / (mc if loader == "vanilla" else f"{mc}-fabric")
            (gd / "mods").mkdir(parents=True, exist_ok=True)
            if loader != "vanilla":
                self.log("Fabric API...")
                api = get_fabric_api(mc)
                for old in (gd / "mods").glob("fabric-api-*.jar"):
                    if old.name != api.name:
                        old.unlink()
                shutil.copy2(api, gd / "mods" / api.name)
            for old in (gd / "mods").glob("lumina*.jar"):
                old.unlink()
            if loader == "lumina":
                lj = find_lumina_jar(mc)
                if not lj:
                    raise RuntimeError(f"Нет jar Lumina для {mc} в папке client/")
                shutil.copy2(lj, gd / "mods" / lj.name)
            skins.apply(acc["name"], mc, gd)
            cmd = build_command(v, vid, java, acc, gd, int(self.cfg["ram"]), nd)
            self.log(f"Запуск {vid} как {acc['name']}")
            self.prog(1.0, "Игра запущена")
            p = subprocess.Popen(cmd, cwd=gd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, errors="replace")
            self.ui(lambda: self.play.configure(state="normal"))
            self.busy = False
            for line in p.stdout:
                self.ui(lambda l=line.rstrip(): (self.logbox.insert("end", l + "\n"), self.logbox.see("end")))
        except Exception as e:
            self.log(f"Ошибка: {e}")
            self.ui(lambda: messagebox.showerror("Ошибка", str(e)))
        finally:
            self.busy = False
            self.ui(lambda: self.play.configure(state="normal"))


if __name__ == "__main__":
    root = tk.Tk()
    App(root)
    root.mainloop()
