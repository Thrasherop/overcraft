exec(open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_dev2.py", encoding="utf8").read().split('sub(C + "dev/DevBot.java"')[0])
# TEMPORARY, not to be committed: read the experiment's numbers from a file on each hit: lift mobTime mcTime heroTime
sub(M + "brigitte/BrigitteStats.java", "	public static final double WHIP_PUSH_TIME = 0.25;", '''	public static double[] TEMP() { try { String[] a = java.nio.file.Files.readString(java.nio.file.Path.of("E:/claude/mods/oc_whip_test.txt")).trim().split(" "); return new double[]{Double.parseDouble(a[0]), Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3])}; } catch (Exception e) { throw new RuntimeException(e); } }
	public static final double WHIP_PUSH_TIME = 0.25;''')
B_ = M + "brigitte/Brigitte.java"
sub(B_, ".add(0, Ow.speed(WHIP_LIFT), 0);", ".add(0, Ow.speed(BrigitteStats.TEMP()[0]), 0);")
sub(B_, "PUSHED.add(new Push(hit, push.scale(share), Ow.ticks(WHIP_PUSH_TIME),", "PUSHED.add(new Push(hit, push.scale(share), Math.max(1, Ow.ticks(BrigitteStats.TEMP()[1])),")
sub(B_, "boolean first = p.left == Ow.ticks(WHIP_PUSH_TIME) - 1;", "boolean first = p.first; p.first = false;")
sub(B_, "		int whiplashLeft = Ow.ticks(WHIPLASH_TIME);\n", "		int whiplashLeft = Ow.ticks(WHIPLASH_TIME);\n		boolean first = true;\n")
CB = C + "ClientBrigitte.java"
sub(CB, "whipUntil = player.tickCount + Ow.ticks(dev.overcraft.hero.OwMovement.handles(player) ? BrigitteStats.WHIP_HERO_PUSH_TIME : BrigitteStats.WHIP_PUSH_TIME);", "whipUntil = player.tickCount + Ow.ticks(dev.overcraft.hero.OwMovement.handles(player) ? BrigitteStats.TEMP()[3] : BrigitteStats.TEMP()[2]);")
print("ok")
