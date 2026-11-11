# The polish method: how polish 3 and 4 ran

Up: [Techniques](../README.md) · the tools: [harness](harness.md) · playing: [hand_play](hand_play.md) · looking: [visual_review](visual_review.md) · the records: [polish3](polish3.md), [polish4](polish4.md).

The owner, 2026-11-06, after playing the result: "they actually worked pretty well", and asked for exactly how to be written down. Each step below says what it caught, from the records, so nobody drops it as ceremony. The owner's earlier verdict is why it exists: "the QA is just sub-par in general ... it needs polish" (2026-11-02), with misses a player sees at once.

## The loop
Three roles, kept apart. The person who built a thing reviews it too kindly.

- **Builder** (an agent, or me): reproduces, fixes, replays, writes the record. Owns the branch and its worktree.
- **Critic** (a fresh agent every round): reads the code and tries to break it. Never the builder, never resumed for a second round.
- **Coordinator** (me): holds the lock rules, checks the raw output and the build personally, starts each critic, plays with the real keys, decides the merge, talks to the owner.

One round:

1. **Builder reproduces first, on the tree before the fix.** Every ranked claim gets a before row from the game, or the record says it couldn't be shown and why.
2. **Fix, then the same steps again.** Before and after on stamped trees, side by side in one table: claim, reproduced, fix, after.
3. **Mutation pass** on any rule that changed.
4. **Coordinator verifies:** opens the raw rows behind the headline claims, reads the build log, checks nothing under `src` moved since the commit called "last code".
5. **Fresh critic on the new code.** Findings go back to the builder.
6. **Repeat until a critic comes back with nothing that reproduces.** Polish 4 took six rounds. Don't plan on a number; stop on the result.
7. **Real-key play by the coordinator on the final commit**, then merge. If play finds a bug, that's a new round.

## What makes the critic work
- **Give it the code, not the conclusions.** The brief names the branch and the areas, and tells it not to open the record or the raw runs until it has its findings. Then it may check what was already covered. A critic handed a hypothesis tends to return it confirmed.
- **New agent each round**, so it hasn't absorbed the last round's reasoning.
- **Ask for** file:line, the in-game steps that would show it, and read-in-code against suspected. Ask what came out clean and what wasn't checked.
- **Read-only, no client** by default: it doesn't contend for the lock, and it can't blur into building.
- **The builder answers each claim, and may refuse it:** "if the critic is wrong, show it." Critics were wrong and that went in the record: `Holds.tells` on a waking sleeper was kept, on the data. Whiplash's lost tick went away by another change and is marked so.
- **Fixes need critics too.** The fourth critique's fix for soft landings ended them on any touch of water or a ladder. The next critic found the regression (Lifeweaver's dash off a ladder 20 -> 11) and it reproduced. Without a critic on the fix it would have shipped.
- **Critics also check the record.** They caught rows that the trace didn't support ("a 'night fight' was four daylight shots"; [hand_play](hand_play.md)).

## Evidence rules
- **Raw output is kept and committed** with the scripts (`runs/polish4/`, `scripts/`): anyone can open the row. Screenshots aren't committed when they show Overwatch models.
- **Every run says its tree.** A client can't report its commit, so the launch script writes `git rev-parse` and a count of dirty `src` files, and each script logs that line (`TREE`). Labels typed by hand are marked "typed": one was wrong on a detached worktree.
- **A table says which tree ran what** ([polish4](polish4.md#which-tree-ran-what)), and what later code changed. Superseded files stay, marked.
- **Void runs are marked void, not deleted.** A hero menu left open that took every key (the scripts now refuse to go on with one), a creeper at 3 m that never swells, a vine in open air that breaks itself, a read before arrival. The trap is a void run reading as a pass.
- **Say how many.** "2 of 2", "1 run each". Say when a result is neither shown nor excluded: Ana's fights, 9/37 against 18/42, stayed that, and more rounds wouldn't settle a script with chance in it. Compare fights like for like: fresh launch and world, same script and seed. A fight table from late in a long session was withdrawn.
- **Marks on every number:** D data, BZ patch notes, W wiki, R recording; H/M/L; or "ours". Blizzard's page gets read before it's cited: #125 was reversed when BZ 2023-04-11 was finally read instead of argued around from the data.
- **Every record has a Not-tried list and a "What a critic should check".** It says what the input was: `KeyMapping.set` isn't a mouse, a bot isn't a second player, nobody has played it. Hiding these is how QA looked better than it was.
- **Suspect the test first.** Lúcio's old rows misled because he rides round a free wall's end while Space is held, so he often never fell. The clean ride needed a script that hears the ride start and reads no ground from top to bottom.

## Mutation pass
Polish 4's first tests were three pure helpers: the critic removed the fixes and they still passed. So:

- Decisions live in small rule methods (`Holds`, `SoftLandingTest` and friends), testable offline.
- A script changes each rule's logic in a throwaway worktree and runs the tests. Every rule change must fail a test; polish 4 ended at 28 of 28 (`mutations_final3`).
- **Include a few call-site mutations expected to pass**, labelled as such. They show the gap: those lines have no offline cover and the game runs are their only evidence. The record lists what's in that gap.

## Real keys find what scripts don't
The harness measures well, and it's blind to what it wasn't asked. The real-key pass found Lúcio's fall damage after scripted rounds had passed: the song's healing hid it from health, and the pain line gave it away. The owner: "Real play is genuinely really important. Trust my human judgement on that."

- Limits of real keys through computer-use: no turning the view with the mouse, no two keys at once, no holding the right button. Write down what that left unplayed.
- Screenshots mask the Claude window, which still takes clicks: move the game clear of it first. See [harness](harness.md).
- The owner's own play outranks everything: the railgun's crispness (#126) came from their session on zumwalt, not from any of this.

## Running it on one machine
- **One client at a time, one lock:** `E:\claude\mods\.oc_client_lock`, taken with `New-Item -ItemType Directory ... -ErrorAction Stop` in its own command, removed only by its holder, only after that client exits. The PC hard-crashed on 2026-11-04. Never kill another agent's client. A lock you didn't take goes to the owner: agents can't approve each other's removals.
- **Overwatch and Battle.net checked closed before each launch.** The watcher (`tools/ow_watch.ps1`) covers what opens later. A run it cuts short is void.
- **A worktree per branch; old builds in detached throwaway worktrees** for the before rows; remove them after.
- **No `git stash`** in a shared repo, per-agent prefixes on scratch scripts, fresh launch and fresh world for anything compared.
- **Don't chain a command after one that can fail.** A failed `worktree add` once sent a merge into the wrong worktree.

## Reports aren't proof
Agents' reports, mine included, are claims. The ones that were wrong: a commit overstating a search's scope (3aca759); my telling the owner a Translocator teleported at once when it measured 0.25-0.38 s; `Fixes #N` trailers on things the owner hasn't seen. So: the coordinator opens the raw file, corrections go in the record and to the owner, and issues close at release after the owner can see the result.

## What it doesn't cover
None of this replaces the Overwatch side by side: [visual_review](visual_review.md) and the owner's clips. A clean critic and clean numbers say a thing works, not that it looks like Overwatch. A second client is its own pass ([hand_play](hand_play.md#a-second-player)).
