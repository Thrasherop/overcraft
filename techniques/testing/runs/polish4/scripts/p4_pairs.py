import re, sys, glob, os
sys.stdout.reconfigure(encoding="utf8")
R = r"E:/Overwatch Archive/research/"
seen = set()
for pat in ("brigitte/work_kit", "sigma/work_kit", "jetpack_cat/assets2", "sombra/work_kit", "sojourn/work_kit", "mercy/work_kit", "ana/work_voice", "lucio/work_kit", "lucio", "juno/work_kit", "lifeweaver/work_kit", "dva/work_kit", "kiriko-anim", "ana/work_kit/dart_grenade"):
    for f in glob.glob(R + pat + "/decomp_*.txt"):
        b = os.path.basename(f)
        if b in seen or b.endswith("_f.txt"): continue
        seen.add(b)
        node = None; rows = {}
        for l in open(f, encoding="utf8", errors="replace"):
            m = re.match(r"#(\d+) (\S+)", l)
            if m: node = m[1]; continue
            for k in ("m_E33EFEE1", "m_583C208B", "m_1376B4DF"):
                if k in l and "m_modifyHealth" in l or (k in l and "/m_" in l):
                    if k in l:
                        rows.setdefault(node, {})[k] = l.split("=", 1)[1].strip()[:150]
        for n, r in rows.items():
            if "m_583C208B" in r:
                print(pat.split('/')[0], b, "#" + n, "H", r.get("m_E33EFEE1", "-"), "| V", r.get("m_583C208B"), "| vec", r.get("m_1376B4DF", "-"))
