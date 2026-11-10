"""One harness connection: py -I p3_oc.py <script> [out file]. c(cmd), mc(command), hp(sel), st(), sleep(s), log(text). Output goes to the screen and to the out file."""
import json, os, re, socket, sys, time
LOG = r"E:\claude\mods\overcraft_p4base\run\logs\latest.log"
sock = socket.create_connection(("127.0.0.1", 25599), timeout=60)
f = sock.makefile("r", encoding="utf8")
OUT = open(sys.argv[2], "a", encoding="utf8") if len(sys.argv) > 2 else None
def log(*a):
    s = " ".join(str(x) for x in a)
    print(s.encode("ascii", "replace").decode(), flush=True)
    if OUT: OUT.write(s + "\n"); OUT.flush()
def c(cmd):
    sock.sendall((cmd + "\n").encode("utf8"))
    return json.loads(f.readline())
def lines():
    with open(LOG, encoding="utf8", errors="replace") as fh:
        return fh.read().splitlines()
def mc(command, wait=0.3):
    n = len(lines()); c("cmd " + command); time.sleep(wait)
    return lines()[n:]
def hp(sel):
    for _ in range(3):
        for line in mc("/data get entity %s Health" % sel, 0.4):
            m = re.search(r"has the following entity data: ([\d.]+)f", line)
            if m: return float(m.group(1))
    return None
def st(): return c("state")
def sleep(s): time.sleep(s)
exec(open(sys.argv[1], encoding="utf8").read())
