# MaplesAdventure

MaplesAdventure is a Minecraft 1.21.1 / NeoForge 21.1.219 / Java 21 adventure
interaction and compatibility core. Better Lock On remains an independent combat
targeting mod; MaplesAdventure does not own or synchronize its combat target.

## Contextual interaction

- `F`: interact with the current contextual target.
- `Y`: cycle through nearby interaction targets.

Both controls are ordinary `KeyMapping` entries and can be rebound in Minecraft's
Controls screen. The known default conflict between `F` and Swap Item With Offhand
is intentionally left visible; MaplesAdventure does not rewrite vanilla bindings.

The client scans a small broad-phase around the player's bounding box every three
ticks by default. A formal target must be within the configured 1.25-block
body-to-shape distance, remain inside vanilla reach, and expose a visible surface
through Minecraft's collision-shape clipping. Candidate discovery is pure: it never
calls a block or entity interaction method. Targets are scored by distance, body
direction, camera alignment and provider priority, then retained with target
stickiness until invalid or manually cycled. Pressing interact revalidates immediately
and performs a bounded refresh when the cached target is absent or stale.

Actual interaction uses Minecraft's `MultiPlayerGameMode` block/entity paths. This
preserves NeoForge events, hand fallback, client prediction and standard server
checks for reach, world border and `ServerLevel#mayInteract` permissions.

## Extensibility

Providers can be registered through `InteractionRegistry`. Datapacks can opt blocks
or entity types in and out with:

- `maplesadventure:interaction_blocks`
- `maplesadventure:interaction_block_blacklist`
- `maplesadventure:interaction_entity_types`
- `maplesadventure:interaction_entity_type_blacklist`

Blacklist tags take precedence over explicit providers, opt-in tags and built-in
Vanilla recognition. Ordinary HUD text uses translated `Component` names and never
shows registry IDs; IDs are only visible when `interactionDebug` is enabled.

The bundled opt-in tag includes Create's `analog_lever` and Bonfires'
`ash_bone_pile`. Both use their own normal right-click logic through
`MultiPlayerGameMode`; complex controls that depend on a specialized click location
remain eligible for a dedicated provider instead of being guessed by the core scanner.

## Compatibility scope

The interaction system does not depend on Better Lock On, Shoulder Surfing or Epic
Fight and does not write camera rotation, player rotation, movement or combat target
state. No Mixin configuration is present. Optional runtime profiles remain available
for main-menu and in-world compatibility tests.

## Build

```text
./gradlew build
./gradlew runClient
./gradlew -PwithEpicFight=true runClient
./gradlew -PwithShoulderSurfing=true runClient
./gradlew -PwithShoulderSurfing=true -PwithShoulderSurfingLegacy=true runClient
./gradlew -PwithEpicFight=true -PwithShoulderSurfing=true runClient
./gradlew -PwithEpicFight=true -PwithShoulderSurfing=true -PwithBetterLockOn=true runClient
./gradlew -PwithCreate=true runClient
./gradlew -PwithBonfires=true runClient
```
