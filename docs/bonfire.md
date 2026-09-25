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

The first valid right-click or contextual **F** interaction activates only that player's ref and plays the optional `bonfire_activate` action; it never opens a menu. The prompt reads **Activate bonfire**, then **Rest at bonfire** after activation. A bounded, owner-only progress snapshot is sent at login, respawn, activation and admin reset; it includes placement generations, not a shared lit flag. Both F and right-click consume the action without a Vanilla hand swing.

Interacting with an activated bonfire begins `SITTING_DOWN` automatically—there is no Rest button. The animation starts before a delayed, client-only black fade. At tick 14 with Epic Fight (tick 10 without it), the server revalidates and commits Rest exactly once while fully black: `lastRested`, HP and available optional mana/stamina, then the phase's Encounter Reset. Full black is held until tick 20 (tick 16 without Epic Fight), 0.2 seconds longer than before. The world is clear again by tick 28 (tick 24 without Epic Fight), showing the end of sitting. At tick 43 (tick 24 without Epic Fight), the server enters `RESTING`, and the left-side menu fades in over 320 ms. Only a bonfire with `LEVEL_UP` enabled offers **Level Up**. **Leave** disables the menu immediately, fades it out over 320 ms while `STANDING_UP` plays, and retains the movement lock until the server completes the 33-tick transition (8 without Epic Fight). Animated transitions include the 0.12-second blend-in in addition to clip duration. No Flask, spell-memory, reinforcement or warp gameplay is implemented.

Session actions contain only a nonce and `LEVEL_UP` or `LEAVE`; the client never decides when Rest succeeds. Each action and server tick rechecks placement generation, dimension, reach, life/spectator state, role, active boss and hostile session. SOLO/HOST may use bonfires; COOPERATOR/INVADER may not activate, rest or level up in a foreign world. Animation and fade completion are not gameplay authorities.

Activation, sitting, resting and standing own the player's pose. Movement inputs and server movement packets are blocked; physical movement/push, damage and knockback are rejected, without changing `noPhysics` or permanent invulnerability flags. An explicit external teleport invalidates the session instead of dragging the player back. Leaving does not unlock the player early just because the visual menu has closed.

## Respawn and encounters

On normal death, NeoForge `PlayerRespawnPositionEvent` validates the stored dimension and exact generation, then searches safe nearby positions (radius at most three blocks, vertical offset at most two). It checks solid footing, collision, fluids and world border, and creates no persistent chunk ticket. If the bonfire was removed/replaced or no safe position exists, Vanilla's original respawn transition remains. Lost Soul and foreign-player phantom death are not modified. A successful Rest calls `EncounterResetService.resetForPlayer(..., BONFIRE)` once; opening, upgrading and leaving never reset encounters.

## Integrations and current visual limitation

The previous external Bonfires `setLastRested` hooks and upgrade source remain optional compatibility; they do not write `PlayerBonfireState`. Both access paths share one source-dispatched `BONFIRE` upgrade validator. Built-in rest works without Bonfires or Epic Fight. With Epic Fight installed, the optional adapter registers four original biped animations: `bonfire_activate` (1.8 s), `bonfire_sit_down` (2.0 s), `bonfire_sit_idle` (4.0 s loop), and `bonfire_stand_up` (1.5 s). They use Epic Fight's main-action/state API to prevent locomotion from replacing a rest clip, without applying root motion. All four clips use a separate copy of the user's existing 20-bone Epic Fight Blender player rig, with editable hand/foot IK controls baked into the original bones for export. Neither the original template nor the player mesh was replaced. Bind-preserving forward correction keeps the knees and forearms in front. Monotone cubic control interpolation avoids stopping at every intermediate waypoint; 60 Hz baking preserves rotations and clip joins. Automated checks cover sampling, root displacement, unit-scale transforms and abrupt IK flips; in-game armor, held-item and third-person pose validation remains necessary.

The resting menu overrides `renderBackground` with a no-op and draws its entries directly: Minecraft 1.21.1's normal `Screen.render` would otherwise invoke blur. The world stays clear; only a left-to-right, fully transparent-at-the-edge gradient shades the menu.

Optional Shoulder Surfing 5 integration activates only if shoulder perspective is already selected when sitting starts. Once per rest, the fully black interval selects a safe camera station at a two-block horizontal radius and 20-degree downward pitch, aimed at the bonfire center (0.65 blocks above its base). Relative to the bonfire-to-player bearing, only the 45–135° and 225–315° side sectors are eligible; the player-facing and opposite 90° wedges are excluded. Both mouse axes provide inverse screen-plane parallax. The camera and blackout use one frame clock: acquiring a shot during a visible fade is forbidden, including after a dropped frame. Leaving blends position and rotation back to the ordinary shoulder view over 650 ms (350 ms in the shorter no-Epic-Fight transition). No player position, look, perspective or saved configuration is changed. A bounded 32-candidate search and padded block rays protect against wall clipping; if neither side offers a safe shot, the normal shoulder view is retained. A narrow client-only `Camera.setup` return hook applies the registered optional provider after Shoulder Surfing: the earlier NeoForge angle event runs before Shoulder Surfing replaces the camera rotation. Without Shoulder Surfing the provider is a no-op. Manual perspective changes, world changes or session closure release it.
