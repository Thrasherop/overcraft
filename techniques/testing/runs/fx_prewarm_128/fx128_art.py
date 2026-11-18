# #128: the client logs' "Effect art made on first use" lines -> per hero: the frames that paid, and the Art list for its Fx class.
# py -I fx128_art.py <log>...
import re, sys
from collections import defaultdict

LINE = re.compile(r'Effect art made on first use, hero (\S+): (\d+) in (\d+) ms, frame (-?\d+) ms: (.*)$')
keys = defaultdict(set)
for path in sys.argv[1:]:
    for line in open(path, encoding='utf8', errors='replace'):
        m = LINE.search(line.rstrip())
        if not m:
            continue
        hero, n, ms, frame, ks = m.groups()
        print('%s  %s: %s made, %s ms, frame %s ms' % (path.rsplit('\\', 1)[-1], hero, n, ms, frame))
        for k in ks.split():
            keys[hero].add(tuple(k.split(',')) if k.count(',') == 2 else tuple(k.split(',')) + ('',))
for hero, ks in keys.items():
    # frames: name_0..name_n with one palette -> name#n+1
    groups = defaultdict(set)
    single = []
    for name, pal, soft in ks:
        m = re.match(r'(.+)_(\d+)$', name)
        if m:
            groups[(m.group(1), pal, soft)].add(int(m.group(2)))
        else:
            single.append((name, pal, soft))
    out = list(single)
    for (base, pal, soft), idx in groups.items():
        if idx == set(range(max(idx) + 1)) and len(idx) > 1:
            out.append(('%s#%d' % (base, max(idx) + 1), pal, soft))
        else:
            out += [('%s_%d' % (base, i), pal, soft) for i in sorted(idx)]
    print('\n// %s: %d' % (hero, len(out)))
    for name, pal, soft in sorted(out):
        print('\t\tnew EffectArt.Art("%s", Palette.%s, %s),' % (name, pal, 'true' if soft else 'false'))
