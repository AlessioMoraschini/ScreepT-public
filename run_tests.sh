#!/bin/sh
# ScreepT automatic tests, three tiers (see TESTING.md): quick | medium | full
# Needs JDK 8 and Maven 3.8.x in the PATH.
TIER="${1:-quick}"
case "$TIER" in
  quick)  PATTERN='*QuickTest' ;;
  medium) PATTERN='*QuickTest,*MediumTest' ;;
  full)   PATTERN='*QuickTest,*MediumTest,*FullTest' ;;
  *) echo "Unknown tier '$TIER': use quick, medium or full"; exit 2 ;;
esac
export MAVEN_OPTS=-Dfile.encoding=UTF-8
cd "$(dirname "$0")/ScreepT_Parent" || exit 1
echo "Running the $TIER tests ($PATTERN)..."
mvn -o test -Dtest="$PATTERN" -DfailIfNoTests=false -Dproject.build.sourceEncoding=UTF-8 || { echo "TESTS FAILED: $TIER"; exit 1; }
echo "Tests OK: $TIER"
