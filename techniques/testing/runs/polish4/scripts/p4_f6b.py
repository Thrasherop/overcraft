import re, sys, glob, os, collections
sys.stdout.reconfigure(encoding="utf8")
R = r"E:/Overwatch Archive/research/"
files = {}
for root in ("brigitte", "ana", "mercy", "sombra", "sigma", "jetpack_cat", "juno", "lifeweaver", "sojourn", "dva", "lucio", "kiriko-anim", "kiriko-effects", "movement"):
    for f in glob.glob(R + root + "/**/decomp_*.txt", recursive=True):
        b = os.path.basename(f)[7:-4]
        files.setdefault(b, f)
extra = r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_data/"
for f in glob.glob(extra + "**/decomp_*.txt", recursive=True):
    files.setdefault(os.path.basename(f)[7:-4], f)
print(len(files))
# 1. which of each value carries an "!= self" condition
cnt = collections.Counter(); ex = collections.defaultdict(list)
for b, f in files.items():
    node = None
    for l in open(f, encoding="utf8", errors="replace"):
        m = re.match(r"#(\d+) (\S+)", l)
        if m: node, typ = m[1], m[2]; continue
        m = re.search(r"m_89831602=(\d+)", l)
        if m and "/m_filter" in l:
            v = int(m[1]); cnt[v] += 1
            if re.search(r"ctx0 != <?(STU_00707E1E\{m_00903303=self\}|self|STU_0AC15F78\{m_entity=self\})", l):
                ex[v].append(b + "#" + node + ":" + typ);
print(cnt); print({k: v[:14] for k, v in ex.items()})
# 2. the known ones
for g, what in (("12D8", "Inspire: W heals Brigitte too"), ("0B97", "Biotic Grenade"), ("5341", "grenade"), ("5791", "grenade"), ("0EDD", "Repair Pack: never herself"), ("006B", "Mercy beam: never herself"), ("21EF", "Life Grip: never himself"), ("0B9E", "Nano: not herself alone"), ("0C7A", "Inspire?"), ("11B9", "Inspire on each ally"), ("1DD5", "?"), ("1248", "flail")):
    f = files.get(g)
    print("==", g, what, f and f.replace(R, ""))
    if not f: continue
    node = None
    for l in open(f, encoding="utf8", errors="replace"):
        m = re.match(r"#(\d+) (\S+)", l)
        if m: node, typ = m[1], m[2]; continue
        if "/m_filter = " in l and ("TrackTargets" in typ or "Shock" in typ or "0A6BE10E" in typ):
            print("  #" + node, typ, l.strip()[:150])
