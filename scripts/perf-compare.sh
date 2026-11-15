#!/usr/bin/env bash
# Before/after perf comparison of two git refs, run in throwaway git worktrees so the working tree is never touched.
#
#   scripts/perf-compare.sh <before-ref> <after-ref> [launches] [--world [scenario]] [--filter <text>] [--no-mods]
#
#   <ref>        any commit-ish; WORKTREE means the current uncommitted state (tracked and staged files, taken with
#                `git stash create`, which changes nothing; untracked files are not included)
#   launches     game launches per side, run in ABAB order (default 2); the reports are merged per side
#   --world      run the in-world scenario (default preset "default") instead of the micro-benchmarks
#   --filter     only tests whose id contains the text
#   --no-mods    do not copy run/mods into the worktrees (by default both sides get the same copy)
#   --low-end    pass -Palumite.perf.lowEnd=true to every launch (2 CPUs, 2 GB heap; see build.gradle)
#
# Both refs must contain the perf suite (debug/perf). Reports go to a temp dir that is printed at the end.
set -euo pipefail

usage() {
    sed -n '2,14p' "$0" | sed 's/^# \{0,1\}//'
    exit 1
}

[[ $# -ge 2 ]] || usage
before_ref=$1
after_ref=$2
shift 2
launches=2
if [[ $# -gt 0 && $1 =~ ^[0-9]+$ ]]; then
    launches=$1
    shift
fi
world_scenario=""
filter=""
copy_mods=1
low_end=0
while [[ $# -gt 0 ]]; do
    case $1 in
        --world)
            world_scenario=default
            if [[ $# -gt 1 && $2 != --* ]]; then
                world_scenario=$2
                shift
            fi
            ;;
        --filter)
            [[ $# -gt 1 ]] || usage
            filter=$2
            shift
            ;;
        --no-mods) copy_mods=0 ;;
        --low-end) low_end=1 ;;
        *) usage ;;
    esac
    shift
done
[[ $launches -ge 1 ]] || usage

repo=$(git rev-parse --show-toplevel)
work=$(mktemp -d "${TMPDIR:-/tmp}/alumite-perf-compare.XXXXXX")
reports="$work/reports"
mkdir -p "$reports"

resolve_ref() {
    if [[ $1 == WORKTREE ]]; then
        # stash create builds a commit of the tracked changes without touching the working tree, index or stash list.
        local snapshot
        snapshot=$(git -C "$repo" stash create)
        echo "${snapshot:-$(git -C "$repo" rev-parse HEAD)}"
    else
        git -C "$repo" rev-parse --verify "$1^{commit}"
    fi
}

before_commit=$(resolve_ref "$before_ref")
after_commit=$(resolve_ref "$after_ref")

cleanup() {
    for side in before after; do
        if [[ -d "$work/$side" ]]; then
            git -C "$repo" worktree remove --force "$work/$side" >/dev/null 2>&1 || true
        fi
    done
    git -C "$repo" worktree prune >/dev/null 2>&1 || true
}
trap cleanup EXIT

for side in before after; do
    commit_var="${side}_commit"
    git -C "$repo" worktree add --detach "$work/$side" "${!commit_var}" >/dev/null
    if [[ ! -d "$work/$side/src/main/java/cc/fascinated/fascinatedutils/debug/perf" ]]; then
        echo "ref for $side (${!commit_var}) has no perf suite" >&2
        exit 1
    fi
    mkdir -p "$work/$side/run"
    if [[ $copy_mods -eq 1 && -d "$repo/run/mods" ]]; then
        cp -r "$repo/run/mods" "$work/$side/run/mods"
    fi
    # Without options.txt a fresh run dir opens the accessibility onboarding screen instead of the title screen.
    if [[ -f "$repo/run/options.txt" ]]; then
        cp "$repo/run/options.txt" "$work/$side/run/options.txt"
    else
        echo "onboardAccessibility:false" > "$work/$side/run/options.txt"
    fi
done

echo "before=$before_ref ($before_commit) after=$after_ref ($after_commit) launches=$launches mode=${world_scenario:+world:$world_scenario}${world_scenario:-micro}"
echo "work dir $work"

for side in before after; do
    echo "building $side"
    (cd "$work/$side" && sh ./gradlew compileJava --offline -q) > "$work/build-$side.log" 2>&1 || {
        echo "build failed for $side, see $work/build-$side.log" >&2
        exit 1
    }
done

launch() {
    local side=$1
    local log=$2
    shift 2
    (cd "$work/$side" && sh ./gradlew runClient --offline -Dalumite.perf.reportDir="$reports" -Dalumite.perf.exit=true "$@") > "$log" 2>&1 || true
    if ! grep -aq ALUMITE_PERF_DONE "$log"; then
        echo "launch did not finish, see $log" >&2
        exit 1
    fi
    if grep -aq "ALUMITE_PERF FAILED" "$log"; then
        grep -a "ALUMITE_PERF FAILED" "$log" | sed 's/.*ALUMITE_PERF /  /' >&2
    fi
}

for ((run = 1; run <= launches; run++)); do
    for side in before after; do
        label="$side-run$run"
        echo "launch $run/$launches $side"
        properties=()
        if [[ -n $world_scenario ]]; then
            properties+=("-Dalumite.perf.scenario=$world_scenario" "-Dalumite.perf.world=$label")
        else
            properties+=("-Dalumite.perf.run=$label")
        fi
        if [[ -n $filter ]]; then
            properties+=("-Dalumite.perf.filter=$filter")
        fi
        if [[ $low_end -eq 1 ]]; then
            properties+=("-Palumite.perf.lowEnd=true")
        fi
        launch "$side" "$work/$label.log" "${properties[@]}"
    done
done

echo "comparing"
launch after "$work/compare.log" "-Dalumite.perf.compareReports=before,after"
grep -a "ALUMITE_PERF compare" "$work/compare.log" | sed 's/\x1b\[[0-9;]*m//g; s/.*ALUMITE_PERF //'
if grep -aq "REFUSED" "$work/compare.log"; then
    echo "COMPARISON REFUSED: the two sides ran different scenarios" >&2
    exit 2
fi
echo "reports in $reports, logs in $work"
