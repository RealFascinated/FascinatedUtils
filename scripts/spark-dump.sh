#!/usr/bin/env sh
# Prints the hot methods of a Spark profile saved with --save-to-file.
# Usage: scripts/spark-dump.sh <file.sparkprofile> [topN] [callerFilter]
# callerFilter lists who calls methods whose class.method contains that text.
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repo_root=$(CDPATH= cd -- "$script_dir/.." && pwd)
spark_jar=$(ls "$repo_root"/run/mods/spark-*-fabric.jar 2>/dev/null | head -n 1)

if [ -z "$spark_jar" ]; then
    echo "spark-dump: no spark-*-fabric.jar under $repo_root/run/mods" >&2
    exit 2
fi

if [ "$#" -lt 1 ] || [ "$#" -gt 3 ]; then
    echo "usage: scripts/spark-dump.sh <file.sparkprofile> [topN] [callerFilter]" >&2
    exit 2
fi

exec java ${SPARKDUMP_JAVA_OPTS:-} -cp "$spark_jar" "$script_dir/spark/SparkDump.java" "$@"
