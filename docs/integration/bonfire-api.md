# Built-in Bonfire API v1

> Language: [简体中文](bonfire-api.zh-CN.md) | **English**

Only `maplesadventure:bonfire` is covered. Legacy Bonfires keeps its existing upgrade/rest hooks and does not write Maples player bonfire progress. No Epic Fight, Iron's Spells, Bonfires or Shoulder Surfing type is part of this API.

## Authority and queries

Use `dev.maplesadventure.api.bonfire.MaplesBonfireApi` on the logical server thread:

- `query(ServerLevel, BlockPos)`: loaded-only, detached `Optional<MaplesBonfireView>`.
- `isActivated(ServerPlayer, MaplesBonfireRef)`: player-local progress.
- `lastRested(ServerPlayer)`: last successful rest, not the last activation; a ref is not a guarantee the placement still exists.
- `isResting(ServerPlayer)`: valid RESTING session only.
- `isFeatureRegistered(ResourceLocation)`: registry membership, not authorization.
- `availableFeatures(ServerPlayer)`: authorized current-session feature IDs in menu order.

`MaplesBonfireRef` contains dimension ID, immutable BlockPos and placement generation UUID. Replacing a bonfire invalidates old references. `MaplesBonfireView` copies the display name and configured feature set. No mutable attachment, block entity, internal session or cache is exposed. There is deliberately no public `rest(player)` bypass.

## Feature extensions

Register `MaplesBonfireFeatureHandler` with `registerFeature` during mod initialization/common setup, **before the first server starts**. Registration freezes at ServerAboutToStart and remains frozen across integrated-server restarts. Duplicate IDs and late registration throw exceptions.

A handler declares stable `id`, bounded `translationKey`, `order`, side-effect-free `isAvailable(context)`, and server-side `execute(context)`. Put translations in your mod's client language files. Smaller order is first; ties sort lexically by ID. Level Up is order 100; custom features normally use 1000 or above. Leave is always the final session action, not a configurable feature.

A row appears only when **configured AND registered AND available**. The server sends this list; the client never infers authority from a block entity. Selection submits only session nonce and feature ID. Before execution the server validates ownership, RESTING state, life/role, dimension, distance, exact placement generation, configured ID and availability again. The feature's subsequent UI must have its own authoritative protocol.

`MaplesBonfireContext` contains a live ServerPlayer, detached view and captured phase UUID. Do not retain contexts. Callback exceptions are logged with registration ID and isolated; failed availability hides the row. This is an integration contract, not a sandbox preventing other installed mods from directly mutating Minecraft.

### Built-in IDs and persistence

`MaplesBonfireFeatures` exposes `LEVEL_UP`, `FLASK_ALLOCATION`, `SPELL_MEMORY`, `REINFORCE`, `WARP`, respectively `maplesadventure:level_up`, `flask_allocation`, `spell_memory`, `reinforce`, `warp`.

Only Level Up has a built-in handler now; it delegates to the existing UpgradeAccessService and source-dispatched BONFIRE validator. Closing Level Up resumes the seated menu without another rest/reset. Flask, spell memory, reinforcement and warp are reserved IDs, **not implemented gameplay**. Registering a future handler exposes an already-configured ID without changing BonfireScreen or adding a packet type.

Block configuration stores `FeatureDataVersion=2` and up to 64 complete resource IDs (128 characters each). Legacy unnamespaced entries migrate to `maplesadventure:<id>`. Valid unknown addon IDs survive load/save even while the addon is absent. Invalid/oversized input is discarded with a warning.

`/ma bonfire feature <x> <y> <z> <id> <true|false>` accepts built-in short aliases or full addon IDs. `info` reports configured IDs separately from registration status.

## Rest/reset lifecycle

The validated sitting transition claims an exactly-once permit before callbacks. BonfireRestService commits lastRested, restores runtime resources, invokes BonfirePhaseResetService, posts completed notifications and syncs progress. Opening/selecting a feature or leaving never repeats rest.

Register a `MaplesBonfireRestResetParticipant` with `registerRestResetParticipant`. Callbacks run once per committed rest, sorted by priority then lexical ID. The reserved `maplesadventure:encounters` participant runs first (priority Integer.MIN_VALUE). Addon priority must be greater than this. Reset **only explicitly owned state in context.phaseId()**, never nearby shared mobs. A failed addon logs its ID without suppressing remaining participants or repeating core reset.

Core resets current-phase Encounter-managed enemies and common phase loot, invalidates old generations without force-loading chunks, and delegates boss/fog lifecycle to the existing EncounterResetService. Other phases, shared mobs and debug prototype zombies are outside its ownership. Defeated bosses respect `respawnDefeatedBosses`.

HP uses actual `getMaxHealth()`. Optional mana/stamina adapters use their runtime maximums (including external modifiers); unavailable integrations do not create substitute pools. A failed optional restore warns but does not undo respawn or phase reset.

Iron's 3.16.3 truncates MAX_MANA to an integer in natural regeneration. The optional `IronsFractionalManaRegenMixin` only skips that regeneration call when mana already equals its finite, positive, fractional runtime maximum. It prevents a full `137.5` pool being reduced to `137`; expenditure, partial regeneration and integer pools retain Iron's behavior. It does not patch the third-party JAR.

Listen for non-cancellable `MaplesBonfireRestCompletedEvent` on `NeoForge.EVENT_BUS`. Its immutable context describes a committed rest; it cannot veto or repeat it. Optional resource failures are possible. Listener failures are caught at each event-post boundary; NeoForge dispatch itself does not promise subsequent listeners run after a throwing listener. Use individually isolated reset participants for reset work. Future flask refill can subscribe here; no flask refill exists yet.

## Respawn and wire bounds

Player attachment format remains version 1, copy-on-death, with activation and lastRested separate. Respawn revalidates exact generation and searches safe standing space. Confirmed removed/replaced placements clear lastRested; temporarily blocked space or unavailable dimensions preserve it and fall back to Vanilla. No permanent chunk ticket is installed.

Protocol **25** replaces canLevelUp with at most 64 menu entries and SELECT_FEATURE/LEAVE intent. One 4096-reference progress body is 180226 bytes for minecraft:overworld, 630786 bytes at the 128-character dimension-ID bound, below the 1 MiB clientbound limit including modest packet-ID overhead. No per-tick menu/progress broadcast is introduced.

See [compilable example](examples/BonfireIntegrationExample.java). `compileIntegrationExamplesJava` is part of `check`.

## Reproducible development checks

In an **isolated test world**, launch `runServer -PweaponRegression=true`. Operator command `bonfirefunctional core` verifies phase reset ownership, partial COMMON clear, defeated-boss policy, resource restoration and safe/stale/blocked respawn using synthetic server participants (not a multiplayer-client test). The fixture constructs and then restores its test platform.

For a real connected test player, `bonfirefunctional resources <player>` sets HP to 3 and installed optional resources to 10%. Rest through F, then run `bonfirefunctional check <player>` to compare raw values against runtime maximums. The opt-in fixture logs each public completed event and its phase generation, allowing menu/upgrade actions to be checked for duplicate reset. Enable `-PwithEpicFight=true` and/or `-PwithIronsSpells=true` with the documented local dependencies; these flags are not production requirements.
