import re, sys, glob, os, collections
sys.stdout.reconfigure(encoding="utf8")
R = r"E:/Overwatch Archive/research/"
seen = {}
for f in glob.glob(R + "**/decomp_*.txt", recursive=True):
    b = os.path.basename(f)
    if b in seen or b.endswith("_f.txt"): continue
    seen[b] = f
cnt = collections.Counter(); rows = collections.defaultdict(list)
for b, f in sorted(seen.items()):
    node = None; typ = None
    for l in open(f, encoding="utf8", errors="replace"):
        m = re.match(r"#(\d+) (\S+)", l)
        if m: node, typ = m[1], m[2]; continue
        for mm in re.finditer(r"m_89831602=(\d+)", l):
            v = int(mm[1]); cnt[v] += 1
            if len(rows[v]) < 400: rows[v].append((b[7:11], node, typ, f.replace(R, "").split("/")[0], l.strip()[:150]))
print(cnt)
want = sys.argv[1:]
for v in sorted(rows):
    gr = collections.Counter(r[0] + ":" + r[3] for r in rows[v])
    print("VALUE", v, "graphs", list(gr.items())[:45])
for g in want:
    for v in rows:
        for r in rows[v]:
            if r[0] == g: print("  ", g, "v=", v, r[1], r[2], r[4])
