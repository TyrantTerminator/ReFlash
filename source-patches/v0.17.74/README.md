# ReFlash v0.17.74 staged patch helpers

**These are partial delta sources, NOT the complete ReFlash 0.17.74 source distribution.**

This folder stages three new helper Java classes extracted from the v0.17.74 patch ZIP:

- `ReFlashFps74.java`: validates and applies custom 1–240 FPS export selection.
- `ReFlashBeta74.java`: draws and dismisses the experimental beta notice.
- `ReFlashResponsive74.java`: guards against overlapping buttons on small windows.

The matching patcher, verification code and previous-release binary inputs are not yet present here. See `docs/source-publication/v0.17.74-status.md` for a full blocker audit. Do not claim this folder rebuilds v0.17.74.
