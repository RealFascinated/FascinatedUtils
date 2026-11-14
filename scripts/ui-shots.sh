#!/bin/sh
# Takes one PNG per UI scene (mod screens, inventory, HUD overlays) from the real game, for UI/UX review.
#
# Usage: scripts/ui-shots.sh [--scenes a,b] [--gui-scale N] [--size WxH] [--out DIR]
#
# Opens a throwaway singleplayer world, so a display must be available. Writes DIR/<scene id>.png (default
# build/ui-shots) and prints each path. Scene ids: see `PerfSceneRegistry` (e.g. screen.settings, screen.hud_editor).
set -eu

cd "$(dirname "$0")/.."
OUT="$PWD/build/ui-shots"
SCENES=""
GUI_SCALE=""
SIZE="1920x1080"
while [ $# -gt 0 ]; do
    case "$1" in
        --scenes) SCENES="$2"; shift 2 ;;
        --gui-scale) GUI_SCALE="$2"; shift 2 ;;
        --size) SIZE="$2"; shift 2 ;;
        --out) OUT="$(realpath -m "$2")"; shift 2 ;;
        *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
done

rm -rf "$OUT"
mkdir -p "$OUT"
LOG="$OUT/run.log"

set -- -Dalumite.perf.scenario=shots -Dalumite.perf.shots="$OUT" -Dalumite.perf.exit=true -Dalumite.perf.shots.size="$SIZE"
[ -n "$SCENES" ] && set -- "$@" -Dalumite.perf.scenes="$SCENES"
[ -n "$GUI_SCALE" ] && set -- "$@" -Dalumite.perf.shots.guiScale="$GUI_SCALE"

sh ./gradlew runClient --offline "$@" >"$LOG" 2>&1 || true
grep -a "ALUMITE_PERF" "$LOG" | sed 's/\x1b\[[0-9;]*m//g' | grep -E "shot |FAILED" || true
if ! grep -aq "ALUMITE_PERF_DONE" "$LOG"; then
    echo "run did not finish, see $LOG" >&2
    exit 1
fi
