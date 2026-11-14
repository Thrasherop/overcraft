# #128: a hero's ART / ART_ENEMY lists: what its first uses made (the client logs) and what its Fx class names (fx128_scan).
# py -I fx128_gen.py <HERO> <Fx.java> <log>...   (prints Java)
import re, subprocess, sys
from collections import defaultdict

hero, fx, logs = sys.argv[1], sys.argv[2], sys.argv[3:]
here = sys.argv[0].rsplit('\\', 1)[0]
LINE = re.compile(r'Effect art made on first use, hero (\S+): \d+ in \d+ ms, frame -?\d+ ms: (.*)$')
seen = set()
for path in logs:
    for line in open(path, encoding='utf8', errors='replace'):
        m = LINE.search(line.rstrip())
        if m and m.group(1) in (hero, 'null' if hero == 'JETPACK_CAT' else '-'):
            for k in m.group(2).split():
                b = k.split(',')
                seen.add((b[0], b[1], len(b) > 2))
scanned = set()
for l in subprocess.run(['py', '-I', here + '\\fx128_scan.py', fx], capture_output=True, text=True).stdout.split():
    b = l.split(',')
    mask = b[0]
    if '#' in mask:
        base, n = mask.split('#')
        for i in range(int(n)):
            scanned.add((base + '_' + str(i), b[1], len(b) > 2))
    else:
        scanned.add((mask, b[1], len(b) > 2))
pairs = {(m, p) for m, p, s in seen}
allk = set(seen) | {k for k in scanned if (k[0], k[1]) not in pairs}


def collapse(ks):
    groups = defaultdict(set)
    out = []
    for name, pal, soft in ks:
        m = re.match(r'(.+)_(\d+)$', name)
        if m:
            groups[(m.group(1), pal, soft)].add(int(m.group(2)))
        else:
            out.append((name, pal, soft))
    for (base, pal, soft), idx in groups.items():
        if len(idx) > 1 and idx == set(range(max(idx) + 1)):
            out.append(('%s#%d' % (base, max(idx) + 1), pal, soft))
        else:
            out += [('%s_%d' % (base, i), pal, soft) for i in sorted(idx)]
    return sorted(out)


def java(name, ks):
    lines = ['\t\tnew EffectArt.Art("%s", Palette.%s, %s)' % (m, p, 'true' if s else 'false') for m, p, s in collapse(ks)]
    return '\tpublic static final java.util.List<EffectArt.Art> %s = java.util.List.of(\n%s);' % (name, ',\n'.join(lines))


ally = {k for k in allk if 'ENEMY' not in k[1]}
enemy = {k for k in allk if 'ENEMY' in k[1]}
print('\t/** Its art, made ahead as the hero is picked (HeroFxArt, #128): what its first uses made (the client log) and what this class names. */')
print(java('ART', ally))
if enemy:
    print('\t/** And as an enemy sees it, for a client seeing another player play the hero. */')
    print(java('ART_ENEMY', enemy))
print('// %s: %d recorded, %d ally, %d enemy' % (hero, len(seen), len(ally), len(enemy)), file=sys.stderr)
