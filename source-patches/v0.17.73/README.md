# ReFlash v0.17.73 recovered source delta (verified)

**The three changed Java classes were recovered from earlier source bundles and rebuilt byte-for-byte identically to the supplied v0.17.73 binary** using JDK 21.

**Source inputs:** the `ReFlashAudioSource69.java` and `ReFlashAudioWarp69.java` files in the v0.17.69 source ZIP; `ReFlashFfmpegSafety68.java` from the v0.17.68 source ZIP. Apply `source73.patch` to their original `src/dev/reflash/client/` package paths. The complete reconstructed class sources are also provided separately in the assistant's v0.17.73 recovery archive.

**Rebuild:** `python3 build_73.py --base ReFlash-v0.17.72.jar --output rebuilt-73.jar`, from a directory containing those updated Java sources.

**Proof:** compiled classes matched all three v0.17.73 compiled classes exactly, and the resulting 431-entry JAR matched all 431 entries from the original v0.17.73 JAR by decompressed bytes. This is a *verified patch-source reconstruction*, not a full clean source build: `v0.17.72.jar` is still a compiled input and the original v0.11.4→v0.15.60 sources remain incomplete.

**Never represent this delta alone as complete GPL corresponding source.**
