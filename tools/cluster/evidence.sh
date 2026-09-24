#!/usr/bin/env bash
# Capture evidence for one test, as text.
#
#   tools/cluster/evidence.sh call        # ring the phone while this runs
#   tools/cluster/evidence.sh message     # send yourself a WhatsApp / SMS
#   tools/cluster/evidence.sh navend      # start navigation, then end it
#   tools/cluster/evidence.sh heartbeat   # just leave it connected
#   tools/cluster/evidence.sh pairing     # force-stop and reopen the app
#   tools/cluster/evidence.sh telemetry   # capture a live frame
#
# Why this exists: photographs live in a temp folder and vanish. The rider
# needs to be able to say "this works, here is the proof" months later, and a
# deleted screenshot proves nothing. Everything here lands in a dated text file
# under tools/live/ — paste the decisive lines into
# docs/testing/Hardware-Evidence-Log.md.
#
# Each mode clears the log, watches for a fixed window, then prints only the
# lines that decide the question. If nothing matches, that IS the answer: the
# event never reached the app.

set -u
MODE="${1:?usage: evidence.sh call|message|navend|heartbeat|pairing|telemetry [seconds]}"
WATCH="${2:-45}"
ADB="${LOCALAPPDATA}/Android/Sdk/platform-tools/adb.exe"
OUT_DIR="$(dirname "$0")/live"
OUT="$OUT_DIR/evidence-$MODE-$(date +%Y%m%d-%H%M%S).txt"
mkdir -p "$OUT_DIR"

case "$MODE" in
  call)
    # Both outcomes matter. "flagging cluster" means it reached the cluster.
    # "Ignoring call-category" means the new filter rejected it — a regression,
    # since that filter was rewritten 19 Sep and has never seen a real call.
    PATTERN="Call from|flagging cluster|Ignoring call-category|alert\? pkg=|Alert pushed"
    SAY="RING THE PHONE NOW (then try a WhatsApp call)" ;;
  message)
    PATTERN="Message from|flagging cluster|alert\? pkg=|Alert pushed"
    SAY="SEND YOURSELF A WHATSAPP MESSAGE, THEN AN SMS" ;;
  navend)
    # The question is whether the whole nav block clears, not just the arrow.
    PATTERN="Navigation ended|cluster blanked|notification removed|stopping relay|Relay .* code="
    SAY="START NAVIGATION, LET ONE TURN SHOW, THEN END IT" ;;
  heartbeat)
    # Interval between a533 packets is the answer; expect one per beat.
    PATTERN="a533"
    SAY="NOTHING TO DO - just stay connected" ;;
  pairing)
    PATTERN="saveSession|GATT connected|Write characteristic|servicesReady|reconnect|No .* write characteristic"
    SAY="FORCE-STOP THE APP, THEN REOPEN IT" ;;
  telemetry)
    PATTERN="RX UUID|a537"
    SAY="NOTHING TO DO - just stay connected" ;;
  *) echo "unknown mode: $MODE"; exit 1 ;;
esac

echo "=== $MODE ==="
echo ">>> $SAY"
echo ">>> watching for ${WATCH}s..."
"$ADB" logcat -c
sleep "$WATCH"

{
  echo "# evidence: $MODE"
  echo "# captured: $(date '+%Y-%m-%d %H:%M:%S')"
  echo "# device: $("$ADB" shell getprop ro.product.model 2>/dev/null | tr -d '\r')"
  echo
  "$ADB" logcat -d 2>&1 | grep -iE "$PATTERN" | grep -viE "adbd|ActivityManager"
} > "$OUT"

LINES=$(grep -cve '^#' -e '^$' "$OUT" 2>/dev/null || echo 0)
echo
if [ "$LINES" -eq 0 ]; then
  echo "NOTHING MATCHED — the event never reached the app. That is a result, not a failed capture."
else
  echo "$LINES matching lines:"
  grep -ve '^#' -e '^$' "$OUT" | tail -20
fi
echo
echo "saved: $OUT"
echo "Paste the decisive lines into docs/testing/Hardware-Evidence-Log.md"
