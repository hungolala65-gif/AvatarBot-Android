# Native libraries and Java ME core

- `bot2x-headless.jar`: extracted from the user-provided `Bot2X-SuperLite.jar`, retaining all 50 non-UI `farm2x` classes. Used directly by the Android Gradle build. The 10 MIDlet/Canvas UI classes are removed; no custom Java source for this JAR is provided.
- Optional `mobilecore.aar` from the earlier Go experiment may be placed here, but **is not required** for Java ME bot mode in v0.4.

See `../../../docs/JAR_ANALYSIS_AND_INTEGRATION.md` for manifest, protocol and security caveats.
