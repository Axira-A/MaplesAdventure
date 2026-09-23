# Third-party notices

MaplesAdventure's root MIT license applies to the project's MIT-licensed source code; it does not
automatically license project-owned visual/artistic assets. Models, textures, animations, icons,
UI artwork and other protected visual material are governed by
[ASSET_LICENSE.md](ASSET_LICENSE.md).

Minecraft, NeoForge, dependency JARs, externally sourced assets and other third-party material
remain under their own licenses and rights holders. Dependencies must be obtained separately under
their own licenses. The build does not shade or bundle these mods. Local inspection/testing is not
redistribution permission.

## Project visual contributors

**Ai_myh — Models & Visual Art / 模型及美术创作者**

Ai_myh is credited as a MaplesAdventure model and visual-art creator. This is project contributor
credit, not a claim that the contributor's work is MIT-licensed. Rights in individual contributed
or commissioned works remain subject to the applicable creator agreement and the asset policy
above.

## Current integrations

| Project | Relationship in current source |
|---|---|
| Minecraft / NeoForge | Platform/toolchain dependency; normal events, registries, Attachments and targeted vanilla Mixins. Not redistributed as MaplesAdventure source. |
| Epic Fight | Compile-only API; optional runtime integration for stamina, skill/dodge ownership, weapon facts, status motion, echo animation and sensory filtering. Includes optional Mixins targeting Epic Fight classes. |
| Iron's Spells 'n Spellbooks | Compile-only API; optional mana, school scaling/affinity integration and optional school-power accessor Mixin. |
| EpicFight-Nightfall | Optional runtime integration through Epic Fight registry IDs and mod ID efn; no hard Nightfall class dependency. |
| Bonfires | Optional runtime reflection/accessor and targeted Mixins for successful rest/light hooks and authorized menu access. Not merely a generic block tag; the integration uses its actual block/screen behavior. |
| Presence Footsteps | Optional runtime source-aware sound integration with a targeted optional Mixin. |
| Subtle Effects | Optional runtime source-aware entity/packet VFX integration with targeted optional Mixins. |
| Better Lock On | Independent optional compatibility-test runtime. No compile-only API or copied target-management system; Phase target rules remain MaplesAdventure's responsibility. |
| Shoulder Surfing Reloaded | Independent optional compatibility-test runtime; no direct API dependency or Shoulder Surfing-specific Mixin in current source. |
| Create | Independent optional compatibility-test runtime and interaction block tag; no compile-only API or bundled code. |

Epic Fight and Iron's references are isolated from the public API signatures. Optional
integration does not mean source builds need no compile-time APIs; consult the README.
Compatibility hooks written in this repository are not copies of a third-party mod's complete
implementation. No third-party mod JAR is patched or included in the release.

Supporting optional test runtimes include Iron's Lib, Curios, GeckoLib, Player Animator,
Fzzy Config and Kotlin for Forge. These are dependency/test-runtime roles, not bundled assets.

## Reviewed references

The Seramicx project `epic-fight-better-lockon-movement-camera-fix` was reviewed for an earlier
target-lock prototype. Current source does not incorporate that project's implementation.
This is a reference-only notice, not a claim that its license covers other dependencies.

Relevant upstream project locations:

- Epic Fight: https://github.com/Antikythera-Studios/epicfight
- Shoulder Surfing Reloaded: https://github.com/Exopandora/ShoulderSurfing
- Create: https://github.com/Creators-of-Create/Create
- Bonfires: https://github.com/Wehavecookies56/Bonfires
- Reviewed camera-fix reference: https://github.com/Seramicx/epic-fight-better-lockon-movement-camera-fix

## Copied code and assets

The source audit identifies API references, independent compatibility code and test dependencies;
it does not identify a bundled third-party mod implementation or dependency JAR. Referencing
Minecraft textures, player skins or another mod's resources at runtime does not transfer their
copyright to this project. Do not infer ownership of externally supplied artwork from its file
name. Preserve any applicable per-asset notice and obtain permission where provenance is unclear.

The Gradle wrapper bootstrap binary is build tooling, not a third-party gameplay mod.
No generic upstream license template is presented as evidence of a copied implementation.
