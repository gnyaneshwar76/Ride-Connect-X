"""
Pull the vector path data out of Android's compiled binary XML.

The maneuver icons in the Suzuki APK are vector drawables, so the arrow shape
lives in `pathData` strings inside a compiled resource. Those strings sit in the
file's string pool, which is a simple enough structure to read directly — no
need for a full AXML parser, and no external tooling.
"""
import re
import struct
import sys
import zipfile
from pathlib import Path

STRING_POOL = 0x0001
UTF8_FLAG = 1 << 8


def string_pool(data: bytes):
    """Return every string in the first string pool chunk of an AXML file."""
    # Header: magic(4) filesize(4), then chunks.
    pos = 8
    while pos + 8 <= len(data):
        chunk_type, header_size, chunk_size = struct.unpack_from("<HHI", data, pos)
        if chunk_type == STRING_POOL:
            break
        if chunk_size <= 0:
            return []
        pos += chunk_size
    else:
        return []

    count, _style_count, flags, strings_start, _styles_start = struct.unpack_from(
        "<IIIII", data, pos + 8
    )
    offsets = struct.unpack_from(f"<{count}I", data, pos + 28)
    base = pos + strings_start
    utf8 = bool(flags & UTF8_FLAG)

    out = []
    for off in offsets:
        p = base + off
        if p >= len(data):
            continue
        try:
            if utf8:
                # Two length fields (chars, then bytes), each 1-2 bytes.
                p += 2 if data[p] & 0x80 else 1
                if data[p] & 0x80:
                    n = ((data[p] & 0x7F) << 8) | data[p + 1]
                    p += 2
                else:
                    n = data[p]
                    p += 1
                out.append(data[p:p + n].decode("utf-8", "replace"))
            else:
                n = struct.unpack_from("<H", data, p)[0]
                if n & 0x8000:
                    n = ((n & 0x7FFF) << 16) | struct.unpack_from("<H", data, p + 2)[0]
                    p += 4
                else:
                    p += 2
                out.append(data[p:p + n * 2].decode("utf-16-le", "replace"))
        except Exception:
            continue
    return out


PATH_RE = re.compile(r"^[Mm][-\d.]")


def paths_in(data: bytes):
    return [s for s in string_pool(data) if PATH_RE.match(s.strip())]


def main(apk_path: str):
    icons = {}
    with zipfile.ZipFile(apk_path) as z:
        for name in z.namelist():
            m = re.search(r"ic_step_(\d+)\.xml$", name)
            if not m:
                continue
            icons[int(m.group(1))] = paths_in(z.read(name))

    for mappls_id in sorted(icons):
        print(f"=== ic_step_{mappls_id} ===")
        for p in icons[mappls_id]:
            print(f"  {p}")


if __name__ == "__main__":
    main(sys.argv[1])
