# Built-in Bonfire Core

> Language: [简体中文](bonfire.zh-CN.md) | **English**

MaplesAdventure's own `maplesadventure:bonfire` does not require the external Bonfires mod. Its current block/item model references Vanilla Minecraft's campfire model as a **temporary placeholder**. No Bonfires model, texture or Spiral Sword asset is used. Final visual art is deferred.

## Map-author setup

Give and place the block with `/give @s maplesadventure:bonfire`. With permission level 2, configure the loaded bonfire:

```text
/ma bonfire info <x> <y> <z>
/ma bonfire name <x> <y> <z> <name...>
/ma bonfire feature <x> <y> <z> level_up true
/ma bonfire feature <x> <y> <z> level_up false
/ma bonfire resetplayer <player>
```

`/maplesadventure` is an alias for `/ma`. Explicit coordinates allow functions and command blocks to change features without a player executor. Names are plain text bounded to 64 characters. A newly placed bonfire has **no enabled features**; resting is a core action, not a flag. Stable flags `LEVEL_UP`, `WARP`, `FLASK_ALLOCATION`, `SPELL_MEMORY`, and `REINFORCE` can be stored/configured, but only `LEVEL_UP` has an action today. Other flags are hidden in the player UI.

## Player-local state

Each placement has `BonfireRef = dimension + BlockPos + generation UUID`; the block entity stores only shared map-author name/features/generation. `PlayerBonfireState` is a versioned, copy-on-death player Attachment with up to 4096 activated refs and an optional last-rested ref/yaw. No global `LIT` bit exists. Replacing a block at the same location creates a new generation, so old activation/respawn progress does not silently bind to the replacement.

The first valid right-click or contextual **F** interaction only activates the player's ref. Later interactions open a server-authorized session in `OPEN_STANDING`. **Rest** succeeds once, sets `lastRested`, restores HP plus available optional mana/stamina, resets the player's phase encounters once, then advances `SITTING_DOWN → RESTING` by server timer. Only when resting at a bonfire with `LEVEL_UP` enabled is **Level Up** offered. It uses the existing exact-XP, server-authorized upgrade transaction. **Leave** advances through `STANDING_UP` before closing. No Flask, spell-memory, reinforcement or warp gameplay is implemented.

Session actions contain only a nonce and `REST`, `LEVEL_UP` or `LEAVE`. Every action rechecks placement generation, dimension, reach, life/spectator state, role, active boss and hostile session. SOLO/HOST may use bonfires; COOPERATOR/INVADER may not activate, rest or level up in a foreign world. Animation completion is not a gameplay authority.

## Respawn and encounters

On normal death, NeoForge `PlayerRespawnPositionEvent` validates the stored dimension and exact generation, then searches safe nearby positions (radius at most three blocks, vertical offset at most two). It checks solid footing, collision, fluids and world border, and creates no persistent chunk ticket. If the bonfire was removed/replaced or no safe position exists, Vanilla's original respawn transition remains. Lost Soul and foreign-player phantom death are not modified. A successful Rest calls `EncounterResetService.resetForPlayer(..., BONFIRE)` once; opening, upgrading and leaving never reset encounters.

## Integrations and current visual limitation

The previous external Bonfires `setLastRested` hooks and upgrade source remain optional compatibility; they do not write `PlayerBonfireState`. Both access paths share one source-dispatched `BONFIRE` upgrade validator. Built-in rest works without Bonfires or Epic Fight. With Epic Fight installed, an optional adapter registers three original biped animations (`bonfire_sit_down`, `bonfire_sit_idle`, `bonfire_stand_up`) and plays them on server-authoritative session transitions. Their 1.8/4.0-loop/1.5-second clips are presentation only; the server timers and validated rest transaction do not depend on animation loading or completion. Blockbench import and structural seam checks passed, but multi-angle in-game pose, armor and held-item QA remains necessary.
