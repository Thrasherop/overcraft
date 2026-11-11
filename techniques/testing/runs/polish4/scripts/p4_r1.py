exec(open(r"C:/Users/ultra/AppData/Local/Temp/claude/E--claude-mods-overcraft/6635df67-fe58-47ac-a0d2-4067e7d195cf/scratchpad/p4_lib.py").read())
log('=== #119 BEFORE. Tree: base e5e5f66 (fix/polish-3 tip), unmodified. Input: harness key mappings and bots, not a mouse; bots are server-side players, not second clients.')
sleep(6); log('screen',st().get('screen')); c('close'); sleep(1)
setup(); mc('/gamemode survival @s')
log('me', {k:st().get(k) for k in ('screen','hero','heroMode','health','maxHealth')})
def cl(*names):
    for n in names: bot('remove '+n)
def add(n,hero,team,x,z,yaw,pitch=0):
    r=bot('add %s %s %s %s -60 %s %s'%(n,hero,team,x,z,yaw)); mc('/gamemode survival '+n); bot('look %s %s %s'%(n,yaw,pitch)); return r
def timeline(label, read, dt=0.25, n=8):
    for i in range(n):
        sleep(dt); log('  %s +%.2f %s'%(label,dt*(i+1),read()))
# ---- A: bot ally slept, real Kiriko's Suzu
pick('kiriko'); mc('/gamemode survival @s'); mc('/overcraft reset'); tp(0.5,-60,0.5,0,60)
cl('anaE','allyB'); add('allyB','mercy','blue',1.5,3.5,0); add('anaE','ana','red',1.5,12.5,180); sleep(0.5)
log('--- A sleep on a bot ally'); log(bot('press anaE ability1')); sleep(0.8); log('after dart',bot('read allyB').get('asleep'),'all',bot('asleep'))
press('ability_2'); s=st(); log('suzu cd2',s.get('ability2Cooldown'))
timeline('A',lambda:(bot('read allyB').get('asleep'),bot('read allyB').get('health'),round(st().get('ability2Cooldown',0),1)))
c('screenshot p4_r1_a.png')
# ---- B: real client slept, a bot Kiriko's Suzu
pick('mercy'); mc('/gamemode survival @s'); tp(0.5,-60,0.5,0,0)
cl('anaE','allyB','kiriB'); add('kiriB','kiriko','blue',-2.5,0.5,-90,70); add('anaE','ana','red',0.5,8.5,180); sleep(0.5)
log('--- B sleep on the real client'); log(bot('press anaE ability1')); sleep(0.8); log('after dart',bot('asleep'),'clip',st().get('anim'))
log(bot('press kiriB ability2'))
timeline('B',lambda:(bot('asleep').get('asleep'),st().get('anim')),0.5,9)
c('screenshot p4_r1_b.png')
# ---- C: Rally's stun on a bot ally then real Suzu
sleep(2); pick('kiriko'); mc('/gamemode survival @s'); mc('/overcraft reset'); tp(0.5,-60,0.5,0,60)
cl('anaE','kiriB','allyB','brig'); add('allyB','mercy','blue',1.5,3.5,0); add('brig','brigitte','red',1.5,5.5,180)
log('--- C stun on a bot ally'); bot('charge brig 9999'); bot('press brig ultimate'); sleep(1.2); bot('secondary brig on'); sleep(0.6)
inp('brig',3,0); sleep(0.15); inp('brig',2,0); sleep(0.12); log('bash',bot('asleep'),bot('read allyB')['health']); press('ability_2',0.1)
timeline('C',lambda:(bot('asleep').get('asleep'),bot('read allyB')['health']),0.15,8)
inp('brig',0,0); bot('secondary brig off')
# ---- D: EMP on a real client, a bot Kiriko's Suzu: are the keys still locked?
sleep(2); pick('mercy'); mc('/gamemode survival @s'); mc('/overcraft reset'); tp(0.5,-60,0.5,0,0)
cl('allyB','brig','kiriB','som'); add('kiriB','kiriko','blue',-2.5,0.5,-90,70); add('som','sombra','red',0.5,6.5,180); sleep(0.4)
log('--- D Hack on the real client'); bot('charge som 9999'); log(bot('press som ultimate')); sleep(0.9)
log('glowing?',mc('/data get entity @s active_effects',0.4)[-1:]); log(bot('press kiriB ability2')); sleep(0.5)
log('after suzu effects',mc('/data get entity @s active_effects',0.4)[-1:])
b=st().get('ability1Cooldown'); press('ability_1'); sleep(0.2); log('Shift pressed while "cleansed": cd1',b,'->',st().get('ability1Cooldown'))
c('screenshot p4_r1_d.png'); sleep(3.5); b=st().get('ability1Cooldown'); log('after 3s')
cl('som','kiriB')
