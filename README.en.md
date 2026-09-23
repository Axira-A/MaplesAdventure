# MaplesAdventure

> Language: [简体中文](README.md) | **English**

A Souls-inspired adventure and RPG combat framework for shared Minecraft worlds. Map makers
define encounters and bosses; players build attributes, manage equipment and interact through
contextual world targets. Server-authoritative phase relationships support solo exploration,
cooperation, duels and opt-in invasion without making the client a gameplay authority.

## Requirements

- Minecraft **1.21.1**
- NeoForge **21.1.219 or compatible 21.1.x**
- **Java 21**

Install the mod on both server and clients with matching versions/protocol. Optional integrations
are not bundled. Their absence must not prevent the core from loading.

## Combat, attributes and defense

Eight RPG attributes, derived capabilities, exact-XP upgrades, equipment load and automatic dodge
selection. Weapon requirements, scaling, multi-channel attack rating, infusions, enemy defense
profiles and player defense share a single damage-resolution pipeline. Projectile weapon data is
frozen at launch. Damage channels are standard physical, slash, strike, pierce, magic, fire,
lightning, ice and holy; a multi-channel hit is still one Minecraft hit.

Epic Fight can supply stamina, skills, animation and dodge integration. Nightfall dodge skills
are selected when available. Iron's Spells can supply mana and spell-school integration.
These remain optional; see [third-party notices](THIRD_PARTY_NOTICES.md).

## Status ailments

Bleed, Poison, Scarlet Rot, Frostbite, Sleep, Madness and Death Blight use server-owned buildup,
resistance, eligibility and repeat-proc correction. Immunity, Robustness, Focus and Vitality
provide the defensive views. ARC status scaling is independent of weapon attack scaling.
HUD bars and trigger notices display server state; client input cannot force a proc.

## Multiplayer phase and encounters

Solo phases, gold cooperation, red invasion/duel sessions, safe foreign-player returns,
phase-bound enemies and loot, historical player echoes and sensory isolation. Fixed encounters
support per-phase attempts, boss primary/child lineage, locked co-op scaling, phase boss bars
and fog gates. Map authors remain responsible for enclosed room geometry.

These are shared-world systems, not separate dimension copies. Complex third-party boss
replacement/effect semantics require explicit adapters; automatic support for every boss mod
is not claimed.

## Interaction

Rebindable defaults:

- **L**: Multiplayer Hub — messages, helper signs, duelist signs and invasion search.
- **F**: interact with the selected nearby world target.
- **Y**: cycle nearby targets.

F may conflict with vanilla offhand swap; configure controls as appropriate. Structured messages,
summon signs, fog gates, containers and supported blocks reuse contextual interaction.
Lost Souls preserve unspent XP through the established death/recovery flow. MaplesAdventure now has a built-in, player-local Bonfire Core: first interaction activates, later Rest sets a respawn point, restores available resources and resets encounters once. Level-up is available only where enabled. Bonfires remains legacy optional compatibility; see [bonfire documentation](docs/bonfire.md). The built-in block uses a Vanilla placeholder model; planned Epic Fight sit animations are not yet shipped.

## Public API and datapacks

Third-party integrations should use **`dev.maplesadventure.api.*`**, not internal progression,
Attachments, networking or caches.

- [Integration setup](docs/integration/README.md)
- [Status and events](docs/integration/status-api.md)
- [Defense snapshots/profile assignment](docs/integration/defense-api.md)
- [Typed damage descriptions](docs/integration/typed-damage-api.md)
- [Weapon Integration API](docs/integration/weapon-api.md)
- [Armor Integration API](docs/integration/armor-api.md)
- [Public datapack schemas](docs/integration/datapack-api.md)
- [Compilable Java and loadable datapack examples](docs/integration/examples/)

Public API v1 is a compatibility contract. Calls are server-thread operations; notifications and
providers are read-only. No hosted Maven repository is currently advertised.

## Build and development

The source includes compile-only adapters for **Epic Fight 21.17.3.1** and **Iron's Spells
3.16.3**. Obtain their Minecraft 1.21.1 NeoForge JARs from their legitimate distributions.
Provide `epicFightJar` and `ironsSpellsJar` through Gradle `-P` properties, or an ignored
`local-development.properties` file. Values are paths to locally obtained dependency files.
Do not commit these files or credentials. They are compile-only, not runtime requirements for
the base mod and not bundled into its distribution.

```sh
./gradlew clean test
./gradlew clean build
./gradlew publishToMavenLocal
```

Windows: use `gradlew.bat`. Build creates mod, sources and API Javadoc JARs in `build/libs`.
See [integration setup](docs/integration/README.md) for local Maven and composite dependencies.
The wrapper JAR is tracked; third-party binaries and generated output are not.

Development runs: `runClient`, `runServer`. Optional `-PwithEpicFight=true` and
`-PwithIronsSpells=true` enable runtime testing; Iron's also requires configured `ironsLibJar`,
`curiosJar`, `geckoLibJar`, and `playerAnimatorJar`. Other optional profiles and property names
are declared in `build.gradle`. A local repository cache may be supplied with
`localDependencyRepository`; no private cache location is assumed.

Reproducible server fixtures are source-only and opt-in:
`./gradlew runServer -PweaponRegression=true`. Use a disposable test world and operator commands
`apiregression`, `statusregression run`, `statusregression extended`,
`playerdefenseregression`, `defenseregression run`.
Fixtures never ship in the mod. The compiled integration example is included in `check`.

## Credits

- **Axira** — Programming & Game Design
- **Ai_myh** — Art & Models

## License

MaplesAdventure uses split licensing. Unless a file explicitly states otherwise, the **source
code** is licensed under the [MIT License](LICENSE), copyright (c) 2026 Axira.

Project-owned **models, textures, animations, icons, UI artwork and other visual/artistic assets
are not licensed under MIT**. They may not be extracted, copied, adapted, or reused in an
independent mod or other independent project without explicit permission from the applicable
rights holder. Complete official MaplesAdventure distributions may be redistributed under the
conditions in [ASSET_LICENSE.md](ASSET_LICENSE.md).

Third-party content retains its own terms: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
