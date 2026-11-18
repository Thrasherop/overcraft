# #128: the (mask, palette) pairs an Fx class names literally, with soft guessed from the call (Fx.soft / a "true" last argument).
# py -I fx128_scan.py <Fx.java>
import re, sys
src = open(sys.argv[1], encoding='utf8').read()
CALL = re.compile(r'(\w+)\(\s*(?:[\w.]+\s*,\s*[\w.]+\s*,\s*)?"([a-z_0-9]+(?:#\d+)?)"\s*,\s*(?:\w+\()?\s*(?:[\w ]+\?\s*)?Palette\.([A-Z_0-9]+)(?:\s*:\s*Palette\.([A-Z_0-9]+))?([^;]*)')
out = set()
for m in CALL.finditer(src):
    fn, mask, pal, pal2, rest = m.groups()
    rest = rest.split('\n')[0]
    soft = fn == 'soft' or bool(re.search(r',\s*true\s*\)', rest))
    for p in (pal, pal2):
        if p:
            out.add((mask, p, soft))
for mask, p, soft in sorted(out):
    print('%s,%s%s' % (mask, p, ',soft' if soft else ''))
