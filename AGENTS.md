# MaplesAdventure contributor constraints

> Language: [简体中文](AGENTS.zh-CN.md) | **English**

- Target Minecraft 1.21.1, NeoForge 21.1.x (currently 21.1.219), Java 21.
- Gameplay is server-authoritative. Client packets express intent, never trusted state.
- Third-party integrations MUST prefer `dev.maplesadventure.api.*` and documented datapack interfaces.
- Public API v1 is a compatibility contract. Preserve IDs and signatures; prefer additive changes
  and deprecation before removal. Update Javadoc, examples and contract tests together.
- `progression.*`, Attachments, networking, internal caches and client HUD classes are implementation
  details, not third-party interfaces. Never expose mutable Attachment/state objects through API.
- Do not bypass PhaseRelations. Use an actual entity/projectile source for owned status applications.
- Weapon/projectile calculations have one authority; projectiles retain launch snapshots.
  Typed providers describe an existing hit; never call hurt twice or directly subtract HP.
- Datapack parsers validate bounded data; compile lookups at reload/tag binding, not on hot paths.
  Update the documented JSON schema when changing a parser.
- Keep persistence IDs stable; version serialized formats and test migration of existing worlds.
- Optional integrations must classload safely on dedicated servers without the target mod.
  No third-party JAR modifications, copied internals or required dependency introduced accidentally.
- Configure compile-only dependency paths in ignored `local-development.properties`; see README.
- Build/test: `./gradlew clean test`, `./gradlew clean build`.
  Opt-in dedicated fixture: `./gradlew runServer -PweaponRegression=true`.
  In an isolated operator-controlled test world: `apiregression`, `statusregression run`,
  `statusregression extended`, `playerdefenseregression`, `defenseregression run`.
- Keep tests and reproducible fixture source. Do not commit logs, local dependency binaries,
  decompiled sources, test worlds, screenshots, calibration dumps or task-verification narratives.
- Retain MIT project licensing and third-party rights notices. Never infer third-party asset ownership.
- User-facing repository Markdown documentation is bilingual. When changing an English document,
  update its matching Simplified Chinese document in the same change, and vice versa.
  `README.md` is the Simplified Chinese landing page; `README.en.md` is its English counterpart.
