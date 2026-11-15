#!/usr/bin/env bash
# Deep checks, too slow for every run: a soak leak check and startup timing over several launches.
#
#   scripts/perf-deep.sh [launches] [soakMinutes] [extra gradle -D args...]
#
#   launches     startup launches to take the median over (default 5)
#   soakMinutes  how long the soak repeats the scene list (default 10)
#
#   Soak: one world run with -Dalumite.perf.deep=true, repeating the default scene list and printing retained heap
#   after each cycle; it fails when the heap grows more than 5 MB per cycle.
#   Startup: launches the client <launches> times (joining the perf world, one short idle scene) and prints the
#   median, min and max of each one-off timing (mod_init, resource_reload, client_ready, world_join).
#   Logs go to ${TMPDIR:-/tmp}/alumite-deep-*.log.
#
#   exit 0  both checks passed
#   exit 1  the soak reported heap growth (FAILED soak)
#   exit 3  a launch failed or printed no results
set -uo pipefail

launches=${1:-5}
soak_minutes=${2:-10}
shift $(( $# > 2 ? 2 : $# ))

repo_dir=$(cd "$(dirname "$0")/.." && pwd)
cd "$repo_dir" || exit 3
log_dir=${TMPDIR:-/tmp}
status=0

soak_log="$log_dir/alumite-deep-soak.log"
echo "soak: ${soak_minutes} minutes"
timeout $(( soak_minutes * 60 + 1200 )) sh ./gradlew runClient --offline -Dalumite.perf.scenario=quick \
    -Dalumite.perf.world=deep-soak -Dalumite.perf.deep=true -Dalumite.perf.soakMinutes="$soak_minutes" \
    -Dalumite.perf.exit=true "$@" > "$soak_log" 2>&1
grep -a 'ALUMITE_PERF soak\|ALUMITE_PERF FAILED soak' "$soak_log" | sed 's/.*ALUMITE_PERF //'
if grep -aq 'ALUMITE_PERF FAILED soak' "$soak_log"; then
    status=1
elif ! grep -aq 'ALUMITE_PERF soak cycles=' "$soak_log"; then
    echo "soak: no summary (see $soak_log)" >&2
    status=3
fi

values_file=$(mktemp "$log_dir/alumite-deep-startup.XXXXXX")
trap 'rm -f "$values_file"' EXIT
for launch in $(seq 1 "$launches"); do
    log="$log_dir/alumite-deep-startup-$launch.log"
    timeout 900 sh ./gradlew runClient --offline -Dalumite.perf.scenario=quick -Dalumite.perf.world="deep-startup-$launch" \
        -Dalumite.perf.scenes=idle -Dalumite.perf.settleSeconds=1 -Dalumite.perf.measureSeconds=1 \
        -Dalumite.perf.exit=true "$@" > "$log" 2>&1
    found=$(grep -a 'ALUMITE_PERF oneoff [a-z_]* ms=' "$log" | sed 's/.*ALUMITE_PERF oneoff \([a-z_]*\) ms=\([0-9.]*\).*/\1 \2/')
    if [[ -z $found ]]; then
        echo "startup launch $launch: no one-off timings (see $log)" >&2
        status=3
        continue
    fi
    echo "$found" >> "$values_file"
done

awk '{ values[$1] = values[$1] " " $2 }
END {
    for (name in values) {
        count = split(substr(values[name], 2), sorted, " ")
        for (outer = 1; outer <= count; outer++) {
            for (inner = outer + 1; inner <= count; inner++) {
                if (sorted[inner] + 0 < sorted[outer] + 0) { swap = sorted[outer]; sorted[outer] = sorted[inner]; sorted[inner] = swap }
            }
        }
        median = count % 2 ? sorted[(count + 1) / 2] : (sorted[count / 2] + sorted[count / 2 + 1]) / 2
        printf "startup %s medianMs=%.1f minMs=%.1f maxMs=%.1f launches=%d\n", name, median, sorted[1], sorted[count], count
    }
}' "$values_file" | sort
exit $status
