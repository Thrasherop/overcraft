# Ana alone in the fights' script: base against final, the same rounds. Prefix: TAG, WORLD, TREE_LINE, ROUNDS.
D = r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/"
src = open(D + "p4_fight3_body.py", encoding="utf8").read()
old = "for hero,base,rounds in (('kiriko',1000,2),('brigitte',1100,2),('ana',1200,4),('sombra',1300,2)):"
assert old in src
exec(src.replace(old, "for hero,base,rounds in (('ana',1200,ROUNDS),):"))
