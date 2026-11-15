"""Checks the language packs in src/main/java/i18n: every pack must have the same keys as the English master (en.txt).
Usage: python tools/check_packs.py [--write-master]  (--write-master rebuilds en.txt from the union of the keys of all packs)"""
import io, os, sys

BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "java", "i18n")
LANGS = ["it", "es", "fr", "de", "pt", "zh"]


def keys(path):
    result = {}
    for line in io.open(path, encoding="utf-8").read().split("\n"):
        if line and not line.startswith("#") and "\t" in line:
            k, v = line.split("\t", 1)
            result[k] = v
    return result


packs = {l: keys(os.path.join(BASE, l + ".txt")) for l in LANGS}
union = sorted(set(k for p in packs.values() for k in p))
master_path = os.path.join(BASE, "en.txt")
if "--write-master" in sys.argv:
    io.open(master_path, "w", encoding="utf-8", newline="\n").write(
        "# English master of the language packs: same keys as every pack, value = the English text. Not loaded at runtime.\n"
        + "\n".join(k + "\t" + (k[1:] if k.startswith("~") else k) for k in union) + "\n")
    print("master written:", len(union), "keys")
master = set(keys(master_path)) if os.path.exists(master_path) else set()
bad = 0
for l, p in packs.items():
    missing = sorted(master - set(p))
    extra = sorted(set(p) - master)
    if missing or extra:
        bad += 1
        print(l, "missing:", len(missing), "extra:", len(extra), missing[:5])
print("OK: all packs have the keys of the master" if not bad else "PROBLEMS in %d packs" % bad)
sys.exit(1 if bad else 0)
