#!/usr/bin/env bash
# Archive the ride log (and logcat) to a timestamped copy, then optionally clear.
#
#   tools/pull-ride-log.sh          # archive only
#   tools/pull-ride-log.sh --clear  # archive, then clear for a fresh run
#
# Two bugs this script exists to prevent, both hit on 19 August:
#
# 1. 349 lines of ride log were destroyed by clearing before copying. The device
#    file is the only copy and `adb shell rm` is one-way. Always archive first.
#
# 2. Git Bash rewrites a leading /sdcard/... into E:/Git/sdcard/... before adb
#    ever sees it, so `adb pull` fails with "failed to stat remote object". That
#    is what MSYS_NO_PATHCONV below disables. Without it this script silently
#    archives nothing while appearing to work.
set -u
export MSYS_NO_PATHCONV=1
export MSYS2_ARG_CONV_EXCL="*"

ADB="${LOCALAPPDATA}/Android/Sdk/platform-tools/adb.exe"
SERIAL="${SERIAL:-fe7d8c39}"
REMOTE="/sdcard/Android/data/com.gnyaneshwar.rideconnectx/files/ride-log.txt"
DIR="tools/ride-logs"
STAMP="$(date +%Y%m%d-%H%M%S)"

mkdir -p "$DIR"

if ! "$ADB" -s "$SERIAL" shell "test -f $REMOTE" 2>/dev/null; then
  echo "no ride log on the device yet"
else
  OUT="$DIR/ride-log-$STAMP.txt"
  if "$ADB" -s "$SERIAL" pull "$REMOTE" "$OUT" >/dev/null 2>&1 && [ -s "$OUT" ]; then
    echo "archived -> $OUT  ($(wc -l < "$OUT") lines)"
  else
    echo "PULL FAILED - not clearing anything"
    exit 1
  fi
fi

"$ADB" -s "$SERIAL" logcat -d > "$DIR/logcat-$STAMP.txt" 2>/dev/null \
  && echo "logcat   -> $DIR/logcat-$STAMP.txt"

if [ "${1:-}" = "--clear" ]; then
  "$ADB" -s "$SERIAL" shell "rm -f $REMOTE"
  "$ADB" -s "$SERIAL" logcat -c
  echo "cleared (archives above are kept)"
fi
