# Integration API v1

Third-party integrations MUST prefer `dev.maplesadventure.api.*` and documented datapack interfaces.

- [Status API and events](status-api.md)
- [Defense API](defense-api.md)
- [Typed damage providers](typed-damage-api.md)
- [Datapack schema](datapack-api.md)
- [Compilable integration example](examples/BossIntegrationExample.java)
- [Example datapack](examples/datapack/)

## Dependency setup

Minecraft 1.21.1, NeoForge 21.1.x, Java 21. The complete mod JAR contains the API; there is no
separate API artifact or hosted Maven repository advertised at present.

Build this repository with the compile-only dependency properties described in its README, then:

```sh
./gradlew publishToMavenLocal
```

A dependent NeoForge project can use:

```groovy
repositories { mavenLocal() }
dependencies {
    compileOnly 'dev.maplesadventure:maplesadventure:0.2.0'
    localRuntime 'dev.maplesadventure:maplesadventure:0.2.0'
}
```

Version shown must match this checkout's `mod_version`. Declare a required MaplesAdventure mod
dependency in your own metadata if your mod uses it unconditionally. For optional integration,
isolate adapter loading behind ModList and keep API references out of classes loaded without it.

Alternatively use a locally built JAR with `compileOnly files(...)` / `localRuntime files(...)`,
or Gradle `includeBuild('../MaplesAdventure')` with dependency substitution for the same module.
Source/composite builds still need MaplesAdventure's documented compile-only dependencies.
No credentials, binary copies or private repository URLs belong in source control.

`sourcesJar` contains the project sources. `javadocJar` documents only the supported API.
The example Java source is compiled by `compileIntegrationExamplesJava`, included in `check`.

## Contract

Server calls run on the logical server thread. Views are detached immutable values; empty
Optional means unavailable/invalid context, not zero resistance. Invalid mutation requests return
a reason (or false for profile assignment). No method grants client authority.

API IDs and method contracts are stable within v1; internal package layouts, enum ordinals,
Attachments and wire formats are not. Do not reflect into internals, mutate their NBT, depend on
HUD caches, or install Mixins to call these interfaces. No optional combat mod types appear in
the public signatures. Requests do not bypass PhaseRelations.

Events/providers are read-only callbacks. The API rejects callback-reentrant mutations. This is
a guard against accidental recursion, not a sandbox against another installed mod deliberately
calling Minecraft methods or falsifying an environmental source.
