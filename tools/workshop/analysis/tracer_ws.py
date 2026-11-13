"""Tracer in the owner's Workshop recording (WS 2026-11-11, Log-2026-11-11-23-49-15.txt): Blink, Recall, run, jump.
An Ashe bot is also slot S0, so every tick is split by the hero its T line names, not by slot. Run with py -I.

    py -I tracer_ws.py <log>
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import parse_log  # noqa: E402


def hd(a, b):
    return math.hypot(b[0] - a[0], b[2] - a[2])


def main(path):
    ticks = [tk for kind, tk in parse_log.read(path) if kind == 'tick' and tk.get('hero') == 'Tracer' and 'pos' in tk]
    print(f'{len(ticks)} Tracer ticks, t {ticks[0]["t"]:.2f} - {ticks[-1]["t"]:.2f}')
    dts = [b['t'] - a['t'] for a, b in zip(ticks, ticks[1:])]
    print('median tick', sorted(dts)[len(dts) // 2])
    # run speed: ground ticks with input, horizontal speed
    run = [math.hypot(tk['vel'][0], tk['vel'][2]) for tk in ticks if tk['ground'] and tk.get('inp') and
           math.hypot(tk['inp'][0], tk['inp'][2]) > 0.5]
    run.sort()
    print('ground speed with input: n', len(run), 'p50 %.3f p90 %.3f max %.3f' % (run[len(run) // 2], run[int(len(run) * .9)], run[-1]))
    # by input direction (I is local: x side, z forward)
    for name, test in (('forward', lambda i: i[2] > 0.9 and abs(i[0]) < 0.1), ('back', lambda i: i[2] < -0.9 and abs(i[0]) < 0.1),
                       ('side', lambda i: abs(i[0]) > 0.9 and abs(i[2]) < 0.1), ('diag fwd', lambda i: i[2] > 0.5 and abs(i[0]) > 0.5),
                       ('diag back', lambda i: i[2] < -0.5 and abs(i[0]) > 0.5)):
        sp = sorted(math.hypot(tk['vel'][0], tk['vel'][2]) for tk in ticks if tk['ground'] and test(tk['inp']))
        if sp:
            print(f'  {name}: n {len(sp)} p50 {sp[len(sp) // 2]:.3f} max {sp[-1]:.3f}')
    # acceleration: from a standstill with input
    print('starts from standing (ground, speed < 0.2, then input):')
    for k in range(1, len(ticks) - 10):
        a, b = ticks[k - 1], ticks[k]
        if a['ground'] and b['ground'] and math.hypot(a['vel'][0], a['vel'][2]) < 0.2 and math.hypot(b['inp'][0], b['inp'][2]) > 0.5 \
                and math.hypot(a['inp'][0], a['inp'][2]) < 0.5:
            row = [f'{math.hypot(t["vel"][0], t["vel"][2]):.2f}' for t in ticks[k:k + 6]]
            print(f'  t {b["t"]:.2f}: {" ".join(row)}')
    # jumps
    vy = [tk['vel'][1] for tk in ticks]
    print('max vy', max(vy))
    # gravity: airborne, not blinking, consecutive vy differences
    g = []
    for a, b in zip(ticks, ticks[1:]):
        if a['air'] and b['air'] and 0 < b['t'] - a['t'] < 0.03 and a['vel'][1] < 5 and b['vel'][1] < a['vel'][1]:
            g.append((a['vel'][1] - b['vel'][1]) / (b['t'] - a['t']))
    g.sort()
    if g:
        print('gravity p50 %.2f' % g[len(g) // 2])
    # Blink: ab1 flag (using ability 1) per tick, charges
    print('\nBlinks (ticks while ab1 is in use):')
    k = 0
    while k < len(ticks):
        if ticks[k].get('ab1'):
            j = k
            while j < len(ticks) and ticks[j].get('ab1'):
                j += 1
            pre, post = ticks[k - 1], ticks[min(j, len(ticks) - 1)]
            seg = ticks[k - 1:j + 1]
            steps = ' '.join(f'{hd(a["pos"], b["pos"]):.2f}' for a, b in zip(seg, seg[1:]))
            print(f'  t {ticks[k]["t"]:.2f} ticks {j - k} lasted {post["t"] - pre["t"]:.3f} horiz {hd(pre["pos"], post["pos"]):.2f} '
                  f'dy {post["pos"][1] - pre["pos"][1]:+.2f} charges {pre.get("charge_ab1")} -> {post.get("charge_ab1")} '
                  f'inp {pre["inp"]} vel before {[round(v, 2) for v in pre["vel"]]} after {[round(v, 2) for v in post["vel"]]} '
                  f'face {[round(v, 2) for v in pre["face"]]}')
            print('     per-tick metres:', steps)
            print('     vy:', ' '.join(f'{t["vel"][1]:.2f}' for t in seg))
            k = j
        k += 1
    # charges over time and recharge
    print('\nBlink charges (changes), cd_ab1:')
    last = None
    for tk in ticks:
        c = tk.get('charge_ab1')
        if c != last:
            print(f'  t {tk["t"]:.2f} charges {c} cd {tk.get("cd_ab1")}')
            last = c
    # Recall
    print('\nRecall:')
    k = 0
    while k < len(ticks):
        if ticks[k].get('ab2'):
            j = k
            while j < len(ticks) and ticks[j].get('ab2'):
                j += 1
            t0, t1 = ticks[k]['t'], ticks[j - 1]['t']
            print(f'  in use t {t0:.2f} - {t1:.2f} ({t1 - t0 + 0.016:.3f} s), ticks {j - k}')
            end = ticks[min(j, len(ticks) - 1)]
            # where she was 3 s before
            back = min(ticks, key=lambda t: abs(t['t'] - (t0 - 3)))
            print(f'  start pos {ticks[k - 1]["pos"]}, end pos {end["pos"]}, pos 3 s before start {back["pos"]} (t {back["t"]:.2f})')
            print(f'  end vel {end["vel"]}, health {ticks[k - 1].get("health")} -> {end.get("health")}, phased during: '
                  f'{sum(1 for t in ticks[k:j] if t.get("phased"))}/{j - k}')
            hist = [t for t in ticks if t0 - 3.05 <= t['t'] <= t0]
            print('  health over the last 3 s: max', max(t.get('health', 0) for t in hist))
            # the path: each recall tick's position against the history (nearest past position)
            print('  per 0.1 s: t, pos, the history tick it matches (nearest point in the 3 s), its time')
            for t in ticks[k:j:6]:
                near = min(hist, key=lambda h: (h['pos'][0] - t['pos'][0]) ** 2 + (h['pos'][1] - t['pos'][1]) ** 2 + (h['pos'][2] - t['pos'][2]) ** 2)
                dist = math.dist(near['pos'], t['pos'])
                print(f'    {t["t"] - t0:.2f}  {[round(v, 2) for v in t["pos"]]}  near t-{t0 - near["t"]:.2f} ({dist:.2f} m)')
            print('  cd after:', [(round(t['t'], 2), t.get('cd_ab2')) for t in ticks[j:j + 3]])
            k = j
        k += 1
    # ult
    print('\nult flag ticks:', [(round(t['t'], 2)) for t in ticks if t.get('ult')][:30])


if __name__ == '__main__':
    main(sys.argv[1])


def recall_ticks(path):
    """Every tick of the Recall, and the history it went back over (the 3 s before), as (t from the start, pos, vel)."""
    ticks = [tk for kind, tk in parse_log.read(path) if kind == 'tick' and tk.get('hero') == 'Tracer' and 'pos' in tk]
    k = next(i for i, t in enumerate(ticks) if t.get('ab2'))
    t0 = ticks[k]['t']
    for t in ticks[k - 2:k + 60]:
        print(f'  {t["t"] - t0:+.3f} pos {[round(v, 2) for v in t["pos"]]} vel {[round(v, 2) for v in t["vel"]]} '
              f'ab2 {t.get("ab2")} phased {t.get("phased")} hp {t.get("health")} ammo? ground {t["ground"]}')
    print('history at -3.0 .. -2.9 s:')
    for t in ticks:
        if -3.06 <= t['t'] - t0 <= -2.94:
            print(f'  {t["t"] - t0:+.3f} pos {[round(v, 2) for v in t["pos"]]} vel {[round(v, 2) for v in t["vel"]]} hp {t.get("health")}')


if __name__ == '__main__' and len(sys.argv) > 2 and sys.argv[2] == 'recall':
    recall_ticks(sys.argv[1])
