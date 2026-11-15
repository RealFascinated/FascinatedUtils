#!/usr/bin/env bash
# Measures the current HEAD commit and saves its merged perf report into the history folder.
#
#   scripts/perf-history.sh [launches] [--world [scenario]] [--low-end]
#
#   launches     game launches (default 2); the reports of all launches are merged into one
#   --world      run the in-world scenario (default preset "default") instead of the micro-benchmarks
#   --low-end    pass -Palumite.perf.lowEnd=true to every launch (2 CPUs, 2 GB heap; see build.gradle)
#
# HEAD is checked out into a throwaway git worktree, so the working tree is never touched. The merged report is
# copied to ${PERF_HISTORY_DIR:-run/config/alumite/perf/history}/<short-sha>.json, and
# "<short-sha>\t<ISO date>\t<subject>" is appended to index.tsv in that folder.
set -euo pipefail

usage() {
    sed -n '2,12p' "$0" | sed 's/^# \{0,1\}//'
    exit "${1:-1}"
}

launches=2
world_scenario=""
low_end=0
case ${1:-} in
    -h | --help) usage 0 ;;
esac
if [[ $# -gt 0 && $1 =~ ^[0-9]+$ ]]; then
    launches=$1
    shift
fi
while [[ $# -gt 0 ]]; do
    case $1 in
        --world)
            world_scenario=default
            if [[ $# -gt 1 && $2 != --* ]]; then
                world_scenario=$2
                shift
            fi
            ;;
        --low-end) low_end=1 ;;
        *) usage 1 ;;
    esac
    shift
done
[[ $launches -ge 1 ]] || usage 1

repo=$(git rev-parse --show-toplevel)
short_sha=$(git -C "$repo" rev-parse --short HEAD)
commit=$(git -C "$repo" rev-parse HEAD)
subject=$(git -C "$repo" log -1 --format=%s HEAD)
iso_date=$(date -Iseconds)
history_dir=${PERF_HISTORY_DIR:-$repo/run/config/alumite/perf/history}

work=$(mktemp -d "${TMPDIR:-/tmp}/alumite-perf-history.XXXXXX")
reports="$work/reports"
worktree="$work/head"
mkdir -p "$reports"

cleanup() {
    git -C "$repo" worktree remove --force "$worktree" >/dev/null 2>&1 || true
    git -C "$repo" worktree prune >/dev/null 2>&1 || true
}
trap cleanup EXIT

git -C "$repo" worktree add --detach "$worktree" "$commit" >/dev/null
if [[ ! -d "$worktree/src/main/java/cc/fascinated/fascinatedutils/debug/perf" ]]; then
    echo "HEAD ($commit) has no perf suite" >&2
    exit 1
fi
mkdir -p "$worktree/run"
if [[ -d "$repo/run/mods" ]]; then
    cp -r "$repo/run/mods" "$worktree/run/mods"
fi
# Without options.txt a fresh run dir opens the accessibility onboarding screen instead of the title screen.
if [[ -f "$repo/run/options.txt" ]]; then
    cp "$repo/run/options.txt" "$worktree/run/options.txt"
else
    echo "onboardAccessibility:false" > "$worktree/run/options.txt"
fi

echo "measuring $short_sha ($subject) launches=$launches mode=${world_scenario:+world:$world_scenario}${world_scenario:-micro}"
echo "work dir $work"
(cd "$worktree" && sh ./gradlew compileJava --offline -q) > "$work/build.log" 2>&1 || {
    echo "build failed, see $work/build.log" >&2
    exit 1
}

properties_common=()
if [[ $low_end -eq 1 ]]; then
    properties_common+=("-Palumite.perf.lowEnd=true")
fi

for ((run = 1; run <= launches; run++)); do
    label="$short_sha-run$run"
    log="$work/$label.log"
    echo "launch $run/$launches"
    properties=("-Dalumite.perf.reportDir=$reports" "-Dalumite.perf.exit=true")
    if [[ -n $world_scenario ]]; then
        properties+=("-Dalumite.perf.scenario=$world_scenario" "-Dalumite.perf.world=$label")
    else
        properties+=("-Dalumite.perf.run=$label")
    fi
    (cd "$worktree" && sh ./gradlew runClient --offline "${properties[@]}" "${properties_common[@]}") > "$log" 2>&1 || true
    if ! grep -aq ALUMITE_PERF_DONE "$log"; then
        echo "launch did not finish, see $log" >&2
        exit 1
    fi
    if grep -aq "ALUMITE_PERF FAILED" "$log"; then
        grep -a "ALUMITE_PERF FAILED" "$log" | sed 's/.*ALUMITE_PERF /  /' >&2
    fi
done

# Merge the launches: per test id, the median of the per-launch medians (and of the other per-call figures), the
# minimum of minMs, the maximum of maxMs, and the launch count as samples.
merged="$work/$short_sha.json"
python3 -I - "$reports" "$short_sha" "$merged" <<'PY'
import glob
import json
import os
import statistics
import sys

reports_dir, short_sha, merged_path = sys.argv[1], sys.argv[2], sys.argv[3]
files = sorted(glob.glob(os.path.join(reports_dir, f"{short_sha}-run*.json")))
if not files:
    sys.exit("no launch reports were written")
runs = []
for path in files:
    with open(path, encoding="utf-8") as handle:
        runs.append(json.load(handle))

by_id = {}
for run in runs:
    for result in run.get("results", []):
        by_id.setdefault(result["id"], []).append(result)

def median_of(results, key):
    values = [result[key] for result in results if key in result]
    return statistics.median(values) if values else 0

merged_results = []
for test_id, results in by_id.items():
    first = dict(results[0])
    for key in ("calls", "totalMs", "perCallMs", "medianMs", "p95Ms", "p99Ms", "p999Ms", "cvPercent", "allocBytesPerCall", "stutters"):
        if any(key in result for result in results):
            first[key] = median_of(results, key)
    first["minMs"] = min(result.get("minMs", 0) for result in results)
    first["maxMs"] = max(result.get("maxMs", 0) for result in results)
    first["samples"] = len(results)
    merged_results.append(first)

report = dict(runs[0])
report["label"] = short_sha
report["results"] = merged_results
report["meta"] = dict(report.get("meta", {}), launches=str(len(runs)))
with open(merged_path, "w", encoding="utf-8") as handle:
    json.dump(report, handle, indent=2)
PY

mkdir -p "$history_dir"
cp "$merged" "$history_dir/$short_sha.json"
index="$history_dir/index.tsv"
printf '%s\t%s\t%s\n' "$short_sha" "$iso_date" "$subject" >> "$index"

echo "saved $history_dir/$short_sha.json"
echo "index $index"
