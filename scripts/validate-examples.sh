#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

mvn -q -DskipTests package dependency:build-classpath -Dmdep.outputFile=target/classpath.txt

CP="target/classes:$(cat target/classpath.txt)"
rm -rf target/example-classes
mkdir -p target/example-classes

javac --release 21 \
  -cp "$CP" \
  -d target/example-classes \
  $(find examples -name '*.java' | sort)

echo "Compiled $(find examples -name '*.java' | wc -l | tr -d ' ') examples."
