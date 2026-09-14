# v0.1 verification record

Environment: Windows 11, Eclipse Temurin Java 21.0.9, Minecraft 1.21.1,
NeoForge 21.1.219.

## Automated

- `compileJava`: passed.
- `test`: target scoring tests passed.
- `build`: passed and produced `build/libs/maplesadventure-0.1.0.jar`.
- Static checks: no MaplesAdventure Mixin config, no `catch (Throwable)`, no
  reference to Shoulder Surfing `ClientEventHandler#updateRotation`, no direct
  copy of Better Lock On or the MIT movement-fix source.

## Client main-menu smoke tests

- MaplesAdventure only: passed; vanilla combat and camera adapters enabled.
- MaplesAdventure + Epic Fight 21.16.4: passed; public Epic Fight adapter enabled.
- MaplesAdventure + Shoulder Surfing Reloaded 1.21.1-5.0.11: passed; v5 plugin
  discovered and registered.
- MaplesAdventure + both optional mods: passed; both adapters enabled and all
  resource packs loaded without a MaplesAdventure Mixin injection error or broken
  mod state.

The normal development-environment refmap warnings emitted by Epic Fight and
Shoulder Surfing, and Epic Fight's missing-subtitle/develop-only resource warnings,
are upstream diagnostics and did not prevent reaching the main menu.

## Not automated in this workspace

No complete modpack instance or FTB Quests JAR/configuration was present, so FTB
Quests resource loading was not directly exercised. Entity interaction scenarios
(acquire/release/switch, target death/range/LOS, live Epic Fight attack/dodge and
live Shoulder Surfing offset behavior) require an in-world manual acceptance pass.
