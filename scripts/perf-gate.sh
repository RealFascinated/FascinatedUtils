#!/usr/bin/env bash
# Regression gate: runs perf-compare.sh on two refs and fails when a significant result got slower.
#
#   scripts/perf-gate.sh <before-ref> <after-ref> [launches] [--world [scenario]] [--low-end] [--turbo-check]
#
#   All arguments after the two refs are passed to perf-compare.sh unchanged (see its usage), except --turbo-check.
#   Prints every compare row with significant=true and deltaPercent > 0, excluding calibration.* ids.
#   --turbo-check compares the AFTER frame median of each Turbo scene with its <id>_turbo_off scene for entities_x9,
#   turbo_nametags, turbo_displays, turbo_rain, particles and worst_case. It fails when the Turbo-on median is not at
#   least 2% lower. Pairs missing from the output are skipped. Scenes only run in world runs, so pass --world too.
#
#   exit 0  no regression
#   exit 1  at least one significant regression, or a Turbo check failure
#   exit 2  the comparison was REFUSED (the two sides ran different scenarios)
#   exit 3  the comparison could not run (build or launch failure, see its output)
set -euo pipefail

usage() {
    sed -n '2,15p' "$0" | sed 's/^# \{0,1\}//'
    exit "${1:-1}"
}

case ${1:-} in
    -h | --help) usage 0 ;;
esac
turbo_check=0
arguments=()
for argument in "$@"; do
    if [[ $argument == --turbo-check ]]; then
        turbo_check=1
    else
        arguments+=("$argument")
    fi
done
[[ ${#arguments[@]} -ge 2 ]] || usage 1

script_dir=$(cd "$(dirname "$0")" && pwd)
compare="$script_dir/perf-compare.sh"

output_file=$(mktemp "${TMPDIR:-/tmp}/alumite-perf-gate.XXXXXX")
trap 'rm -f "$output_file"' EXIT

status=0
"$compare" "${arguments[@]}" > "$output_file" 2>&1 || status=$?
cat "$output_file"

if [[ $status -eq 2 ]]; then
    echo "GATE: comparison REFUSED" >&2
    exit 2
fi
if [[ $status -ne 0 ]]; then
    echo "GATE: comparison failed (perf-compare.sh exit $status)" >&2
    exit 3
fi

regressions=$(grep -a '^compare ' "$output_file" | awk '
    {
        id = $3
        if (id ~ /^calibration\./) next
        significant = ""; delta = ""
        for (i = 4; i <= NF; i++) {
            if ($i ~ /^significant=/) significant = substr($i, 13)
            if ($i ~ /^deltaPercent=/) delta = substr($i, 14)
        }
        if (significant == "true" && delta != "" && delta + 0 > 0) print
    }') || true

after_frame_median() {
    grep -a '^compare ' "$output_file" | awk -v id="$1" '
        $3 == id {
            for (i = 4; i <= NF; i++) {
                if ($i ~ /^afterMedianMs=/) { print substr($i, 15); exit }
            }
        }' || true
}

turbo_failed=0
if [[ $turbo_check -eq 1 ]]; then
    echo
    echo "GATE: turbo check (AFTER frame median, Turbo on vs off)"
    for scene_id in entities_x9 turbo_nametags turbo_displays turbo_rain particles worst_case; do
        on_ms=$(after_frame_median "scene.$scene_id/frame")
        off_ms=$(after_frame_median "scene.${scene_id}_turbo_off/frame")
        if [[ -z $on_ms || -z $off_ms ]]; then
            echo "GATE: turbo check skipped for $scene_id (scene missing from the after run)"
            continue
        fi
        if awk -v turbo_on="$on_ms" -v turbo_off="$off_ms" 'BEGIN { exit !(turbo_on + 0 > turbo_off * 0.98) }'; then
            echo "GATE: turbo slower than off: $scene_id on=$on_ms off=$off_ms"
            turbo_failed=1
        fi
    done
fi

if [[ -n $regressions ]]; then
    echo
    echo "GATE: significant regressions:"
    echo "$regressions"
    exit 1
fi

if [[ $turbo_failed -eq 1 ]]; then
    echo "GATE: turbo check failed"
    exit 1
fi

echo
echo "GATE: no significant regressions"
exit 0
