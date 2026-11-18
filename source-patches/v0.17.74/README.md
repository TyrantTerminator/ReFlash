# ReFlash v0.17.74 build recovery — NOT full GPL corresponding source

Status: the v0.17.74 binary patch is reproducible, but current complete preferred Java source remains missing.

## Rebuild this exact patch

Requirements: Python 3, JDK 21 and an unmodified ReFlash-v0.17.73.jar.

1. Place the old JAR here, or provide its absolute location with --base.
2. Run: `python3 build_74.py --base ReFlash-v0.17.73.jar --output rebuilt-v0.17.74.jar`
3. Compare to the reference: `python3 compare_jar_entries.py rebuilt-v0.17.74.jar ReFlash-v0.17.74.jar`

Locally verified: the original 434 entries and the rebuilt 434 entries match by name and decompressed content byte-for-byte. ZIP container metadata can differ.

## Contents

- patcher/Apply74.java — source of Java 21 ASM patcher
- patcher/Verify74.java — bytecode structural verifier
- src/dev/reflash/client/ — three Java helper source files
- build_74.py — patch-and-package recipe
- compare_jar_entries.py — entry-level binary comparison

**GPL source is still incomplete.** This patch needs an already-compiled v0.17.73 JAR; its internal preferred source is not included. Historic main sources are about v0.11.4. See docs/source-publication/v0.17.74-status.md.
