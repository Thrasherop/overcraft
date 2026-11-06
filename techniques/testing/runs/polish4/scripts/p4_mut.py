# Each fix removed, in a throwaway worktree: does a test fail? Usage: py p4_mut.py <worktree> <out file>. Each mutation is restored after its run.
import subprocess, sys, os, time
W, OUT = sys.argv[1], sys.argv[2]
M = W + "/src/main/java/dev/overcraft/"
MUTS = [
 ("stack: a stun on a knock-down no longer stays one", "hero/Holds.java", "old.slides || fresh.slides, gravity", "fresh.slides, gravity"),
 ("stack: until's max gone", "hero/Holds.java", "long until = Math.max(old.until, fresh.until);", "long until = fresh.until;"),
 ("stack: a dart lands on a knock-down", "hero/Holds.java", "return old.slides ? null : fresh;", "return fresh;"),
 ("stack: a stun lands on a sleeper", "hero/Holds.java", "if (!old.stun) {\r\n\t\t\treturn null;", "if (false) {\r\n\t\t\treturn null;"),
 ("stack: a plain knock-down no longer ends a float", "hero/Holds.java", "double gravity = plainKnockDown ? 1 : fresh.gravity != 1 ? fresh.gravity : old.gravity;", "double gravity = fresh.gravity != 1 ? fresh.gravity : old.gravity;"),
 ("cleanse: a knock-down ends", "hero/Holds.java", "!hold.slides ? Cleansing.ENDS : Cleansing.STAYS", "Cleansing.ENDS"),
 ("cleanse: a stun stays", "hero/Holds.java", "!hold.slides ? Cleansing.ENDS : Cleansing.STAYS", "Cleansing.STAYS"),
 ("cleanse: a sleep doesn't wake", "hero/Holds.java", "case WAKES -> !hold.waking && hold.until - now > wakingTicks ? new Hold(hold.since, now + wakingTicks, true, false, false, 1) : hold;", "case WAKES -> hold;"),
 ("release: Flux's hold ignored", "hero/Holds.java", "return !heldHere && !fluxHolding;", "return !heldHere;"),
 ("release: this map's hold ignored", "hero/Holds.java", "return !heldHere && !fluxHolding;", "return !fluxHolding;"),
 ("phase skip gone from the hit rule", "hero/Allies.java", "return !phased && (here && !ally || friendlyFire && ally);", "return here && !ally || friendlyFire && ally;"),
 ("phase read on the client", "hero/Combat.java", "return !clientSide && !devOff && phased;", "return !devOff && phased;"),
 ("/kill refused again", "hero/Combat.java", "return phased && !bypassesInvulnerability;", "return phased;"),
 ("shockwave: the bashed one hit again", "brigitte/Brigitte.java", "if (devRallyTwice || !id.apply(e).equals(s.rallyBashed)) {", "if (true) {"),
 ("shockwave: the slots gone", "brigitte/Brigitte.java", "tracked.subList(0, Math.min(RALLY_SHOCKWAVE_TARGETS, tracked.size()))", "tracked"),
 ("anesthetic: stops others' sound", "ana/Anesthetic.java", "return !isTarget && toTarget <= EARSHOT && !anotherNumbedInEarshot;", "return !isTarget && toTarget <= EARSHOT;"),
 ("#121: a knock-down tells", "hero/Holds.java", "return cleansing(hold) != Cleansing.STAYS;", "return true;"),
 ("#121: a sleep or a stun doesn't tell", "hero/Holds.java", "return cleansing(hold) != Cleansing.STAYS;", "return false;"),
 ("shots: the client copy takes creatures", "hero/Allies.java", "return !clientSide && vanilla && (!phased || ownSide);", "return vanilla && (!phased || ownSide);"),
 ("shots: stops on the phased", "hero/Allies.java", "return !clientSide && vanilla && (!phased || ownSide);", "return !clientSide && vanilla;"),
 ("shots: a phased ally refused", "hero/Allies.java", "return !clientSide && vanilla && (!phased || ownSide);", "return !clientSide && vanilla && !phased;"),
 ("landing: not kept in the air", "hero/Holds.java", "return moving || was && (!onGround || grace);", "return moving;"),
 ("landing: the ground left counts", "hero/Holds.java", "return moving || was && (!onGround || grace);", "return moving || was && !onGround;"),
 ("whip: lift 2", "brigitte/BrigitteStats.java", "WHIP_KNOCKBACK = 25, WHIP_LIFT = 3;", "WHIP_KNOCKBACK = 25, WHIP_LIFT = 2;"),
 ("whip: not level", "brigitte/Brigitte.java", "Vec3 level = new Vec3(aim.x, 0, aim.z);", "Vec3 level = aim;"),
 ("call site #121: every Suzu tells (not offline-testable, expected to pass)", "entity/SuzuEntity.java", "told |= Combat.cleanse(entity);", "Combat.cleanse(entity); told = true;"),
 ("call site: Sleep.cleanse no longer asks Holds (not offline-testable, expected to pass)", "hero/Sleep.java", "Holds.Hold after = Holds.cleansed(s.hold(), entity.level().getGameTime(), Ow.ticks(AnaStats.SLEEP_WAKING));", "Holds.Hold after = null;"),
 ("call site: canHurt passes phased=false (not offline-testable, expected to pass)", "hero/Allies.java", "other instanceof LivingEntity living && Combat.phaseStops(living));", "false);"),
]
out = open(OUT, "a", encoding="utf8")
def log(*a):
    s = " ".join(str(x) for x in a); print(s[:200], flush=True); out.write(s + "\n"); out.flush()
def run():
    env = dict(os.environ, JAVA_TOOL_OPTIONS="-Xmx2G")
    r = subprocess.run(["cmd", "/c", W.replace("/", chr(92)) + chr(92) + "gradlew.bat", "-p", W, "test", "--console=plain", "--tests", "*HoldsTest", "--tests", "*PhaseRulesTest", "--tests", "*AnestheticRuleTest", "--tests", "*BrigitteRulesTest", "--tests", "*ShotRuleTest", "--tests", "*SoftLandingTest", "--continue"], cwd=W, env=env, capture_output=True, text=True, errors="replace")
    t = r.stdout + r.stderr
    failed = [l.strip() for l in t.splitlines() if " FAILED" in l and "Task" not in l]
    return ("BUILD SUCCESSFUL" in t, failed, [l for l in t.splitlines() if "tests completed" in l or "error:" in l][:3] or t[-300:] if "BUILD SUCCESSFUL" not in t and not failed else [l for l in t.splitlines() if "tests completed" in l])
log("base, no mutation:", run())
for name, f, old, new in MUTS:
    p = M + f
    s = open(p, encoding="utf8", newline="").read()
    o = old.replace("\r\n", "\n"); crlf = "\r\n" in s
    t = s.replace("\r\n", "\n")
    if t.count(o.replace("\r\n", "\n")) != 1:
        log("NO MATCH", name, t.count(o)); continue
    open(p, "w", encoding="utf8", newline="").write(t.replace(o, new.replace("\r\n", "\n")))
    ok, failed, notes = run()
    log("MUTATION:", name, "| tests", "PASS" if ok else "FAIL", "|", failed, notes)
    open(p, "w", encoding="utf8", newline="").write(s)
log("DONE")
