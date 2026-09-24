# Superseded — do not use

Folded into **`RideConnectX-Knowledge-Base.md`**, which is the single source of
truth for the BLE protocol.

This file was written before three facts were corrected on real hardware, and its
contents are now **wrong** in ways that each cost a debugging session:

- It named `00000003` as the write characteristic. It is **`00000001`**
  (WRITE_NO_RESPONSE). Writes to `00000003` are accepted by the BLE stack and
  silently ignored by the cluster.
- It placed the maneuver code as two ASCII digits at bytes 23-24. It is a **raw
  byte at index 2**; bytes 23-24 are GPS / airplane status characters.
- It gave the checksum as one rule. There are **two branches**, selected by
  vehicle model from the advertised BLE name.

See `RideConnectX-Knowledge-Base.md` and `PROJECT-STATUS.md`.
