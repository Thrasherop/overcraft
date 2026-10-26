#!/usr/bin/env bash
# Helpers for driving the dev client from a session (bash). Source it: . tools/devplay.sh
#   dev_wait_title      wait until the game reaches the title screen (a fresh client)
#   dev_open <world>    open a world and wait for the hero select screen
#   dev <commands...>   py tools/dev.py <commands...>
DEVPLAY_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

dev() {
	py -I "$DEVPLAY_ROOT/tools/dev.py" "$@"
}

dev_wait_title() {
	until dev state 2>/dev/null | grep -qE 'TitleScreen|AccessibilityOnboarding'; do sleep 3; done
}

dev_open() {
	dev "openworld $1" >/dev/null 2>&1
	until dev state 2>/dev/null | grep -q 'HeroSelect'; do sleep 2; done
}
