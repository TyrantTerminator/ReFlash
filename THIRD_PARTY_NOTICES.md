# Replay engine attribution — ReFlash 0.11.0

ReFlash integrates a modified **Momenton v1.0.0** replay engine by Relapzzz
(https://github.com/Relapzzz/momenton), originally licensed under GPL-3.0-only.
This is a GPL-derived source fork inside one Fabric mod, **not a clean-room engine**.
There is no separate Momenton mod, mod ID or nested Fabric mod required at runtime.

Changes: integrate the compiled replay engine into the top-level ReFlash JAR;
patch internal archive suffix to `.reflash`, store data under `reflash/`, and run
the engine's tick methods through ReFlash so its title screen, HUD and F9
recording hotkey aren't installed. The underlying binary chunk encoding is still
derived from Momenton; old `.momenton` archives may be converted non-destructively.

The exact upstream source is included in the build artifact as a downloadable
`Momenton-1.0.0-source.zip`. The modified source and the bytecode patch/merge
scripts are included in ReFlash's complete corresponding source. Distribution
of this combined work is under GPL-3.0-only and must preserve notices.
