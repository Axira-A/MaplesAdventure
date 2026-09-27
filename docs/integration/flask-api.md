# Flask Core and API v1

> Language: **English** | [简体中文](flask-api.zh-CN.md)

Development feature inside MaplesAdventure, not a separate mod. Permanent player Attachment `maplesadventure:player_flasks` (data version 1, copy-on-death) owns all charges. ItemStacks are reusable handles: duplicating, trading or dropping a handle never transfers progression or charges. Missing handles are supplied on login/respawn when inventory space permits. There are no recipes, world-generation materials, empty bottles or durability charges.

## State and rules

`FlaskSnapshot` is immutable: total capacity 4–14, shared potency 0–12, crimson/ashen allocation and remaining charges. Allocation always sums to capacity; each remaining pool is bounded by its allocation. `FlaskState.normalize` is the sole repair policy: clamp capacity/potency/crimson allocation, derive ashen allocation, clamp remaining. Existing records never receive a new-player redistribution.

New player: 4/0 without Iron's, 3/1 with it. Removing Iron's preserves capacity/potency, merges allocated and **remaining** charges into crimson without granting extra charges. Reinstalling does not redistribute existing players.

| Potency | HP | Mana |
|---|---:|---:|
| +0 | 15 | 129 |
| +1 | 20.5 | 153 |
| +2 | 25.5 | 177 |
| +3 | 30 | 202 |
| +4 | 34 | 226 |
| +5 | 37.5 | 242 |
| +6 | 40 | 258 |
| +7 | 42 | 274 |
| +8 | 43.5 | 290 |
| +9 | 45 | 306 |
| +10 | 46.5 | 323 |
| +11 | 47.5 | 339 |
| +12 | 48.5 | 355 |

Fixed additions, capped at actual runtime maximum; not percentages. Drinking at full HP/mana still consumes a charge. Capacity upgrade costs from 4→14: `1,1,2,2,3,3,4,4,5,5` (30 shards total). Potency costs one Undead Bone Shard per level (12 total); its stable registry ID remains `maplesadventure:noble_ash`. Capacity adds its new charge to crimson. Both upgrades refill the current allocation.

## Bonfire integration

Refill listens to `MaplesBonfireRestCompletedEvent`; no second rest/reset transaction is created. `MaplesBonfireRespawnEvent` is an additive API notification after a validated built-in bonfire respawn actually completes. Clone only copies data, regardless of keepInventory; ordinary Vanilla fallback respawn does not refill.

Features: `maplesadventure:flask_allocation` and `maplesadventure:flask_upgrade`. New bonfires configure both. The rest menu exposes a single **Reinforce Flasks** entrance; its next page contains separate **Increase Charges** and **Improve Recovery** cards with current/next values, required/held materials, and independent purchase buttons. Both use the same authorized server transaction. Allocation remains separate and appears only with the mana adapter. The page uses existing Flask/material artwork, no screen blur, and scales to the available window; Bonfire animation/camera behavior is unchanged.

Feature data version 3 collapses legacy `flask_capacity` and `flask_potency` IDs into `flask_upgrade` on load. Either old ID remains an alias in admin configuration; enabling/disabling it now controls the combined entrance. Duplicate aliases produce only one entry. Other features, activation and player Flask progression are preserved. Previously unconfigured bonfires remain unconfigured; enable with `/ma bonfire feature <x> <y> <z> maplesadventure:flask_upgrade true`.

Menu requests carry a single-use server nonce and one bounded action: ALLOCATE, UPGRADE_CAPACITY, UPGRADE_POTENCY or CLOSE; only allocation uses the crimson/ashen counts. The server holds the page, bonfire reference and baseline; it rechecks current RESTING access, placement generation, dimension, feature availability and timeout. Allocation authorization cannot buy upgrades, and upgrade authorization cannot redistribute charges. No client-supplied capacity, potency, price or item counts are accepted. Successful or failed submissions rotate the nonce. Both upgrade buttons lock while pending. Replies identify the consumed nonce, so a late reply cannot reopen a closed screen. Upgrade commits snapshot affected inventory slots and old state, consume exact materials, then write new state; commit failures roll back both before synchronization. Extension events are after the commit.

## Action and compatibility

`FlaskUseController` tracks active actions (30 ticks, one effect commit at tick 14). `FlaskRecoveryService` delivers the committed total during the following six server ticks (15–20, 0.3 seconds). Each step adds to the current resource and clamps to its current maximum, preserving intervening damage instead of overwriting an interpolated target. Releasing the mouse does not cancel. Switching the held stack, damage or stagger cancels the animation but preserves committed recovery. Death, disconnect, dimension change, refill, invalid lifecycle state, SCRIPTED cancellation and shutdown clear pending recovery; it is never persisted or copied to a new player entity. Cancellation before effect changes neither resources nor charges; after effect it never refunds. Turning and slow movement remain possible; sprint, melee and competing use actions are blocked. Existing Epic Fight skill-cast interception rejects skills while drinking; Iron's public SpellPreCastEvent rejects new casts. Other combat addons should use the cancellation API for their own stagger/execution semantics.

Vanilla DRINK animation and entity-origin drink sound are placeholders behind Item/FlaskPresentation. No final VFX or Epic Fight-specific animation is supplied. The minimal `FlaskReleaseMixin` keeps vanilla release from cancelling the press-start action; cleanup uses `stopUsingItem`. It does not alter other items.

`FlaskManaBridge` reflectively loads `IronsFlaskAdapter` only for actual mod ID `irons_spellbooks`. It changes real MagicData and sends SyncManaPacket. Core/public signatures have no Iron's or Epic Fight types. Without Iron's, the stable ashen registry entry is inert and absent from the creative tab, HUD and allocation menu. Existing saved ashen handles may remain inert in inventories.

## Public interface

`dev.maplesadventure.api.flask.FlaskApi`: `getState`, `isUsingFlask`, `getTotalCapacity`, `getPotencyLevel`, crimson/ashen allocated/remaining getters, restoration amount getters, server-only `cancelUse(player, reason)` and `refill(player, reason)`. No public arbitrary progression setter. Snapshots are detached; client queries return synchronized local-player state, not remote-player inspection.

`FlaskEvents.Start` and `.Effect` are cancellable logical-server events. Effect allows a finite bounded nonnegative **total** amount override, sampled once before charge commit, not once per recovery increment; cancellation cancels the action without charge. `.Finish`, `.Cancel` (includes effectApplied), `.Refill` and `.Upgrade` are notifications. Event-post exceptions are logged; action callbacks fail closed. NeoForge does not guarantee later listeners execute after a throwing listener. An optional restoration adapter failure stops pending recovery without refund or retry, since restoration might already have occurred. Mana uses its existing resource synchronization for the six increments, not six full FlaskState packets.

Recharge reasons: BONFIRE, ENEMY_GROUP, BOSS_DEFEAT, SCRIPTED. Only bonfire gameplay is automatic; the others are extension intents. There is no enemy recharge, spell memory or unified bonfire menu.

Protocol **27** includes the combined upgrade page, bounded action IDs and correlated replies; client and server must update together. Only changes/login/respawn/dimension transitions synchronize; no per-tick full state broadcast. HUD displays allocated-pool counts for hotbar/offhand flasks.

## Flask presentation

The seven supplied `hpflask` / `mpflask` images are copied unchanged into `textures/gui/flask/icons`: **1 = full, 7 = empty**, selected by remaining / allocated charges (not total capacity). GUI-only direct rendering preserves aspect ratio and avoids stitching irregular-sized images into the block atlas, which would lower its mipmap level. Materials retain their existing artwork. Remote holders use a full-bottle presentation because their private charge pools are not synchronized; they never mirror the local player's charges.

Hand, ground and item-frame contexts use separate voxel models, with a crown/long neck for crimson and a jeweled stopper/metal straps for ashen. Body variants progressively lower the liquid surface. `models/item/flask/*_body_*.json` are editable Java Block/Item models in Blockbench. NeoForge face color, transparency and emissive data are authoritative in game; editors without those extensions may show opaque glass or untinted surfaces. No external model library is required.

Reinforcement shows the same 3D models; allocation shows two bottles, direction controls, numeric allocations and one small bottle marker per shared charge. Allocation preview refills its own visual bottles only and never changes gameplay state before confirmation. Eight supplied glow-mask frames are tinted translucent gold/blue at render time. All source PNG pixels remain untouched. These visual changes do not change protocol, gameplay balance, rest/reset transactions or the Bonfire camera.

Liquid model faces use alpha 120/255; metal stays opaque. Glow uses additive gold/blue blending, with its previous render state restored after drawing. Small allocation markers use the supplied silhouette's alpha mask (not its black RGB), tinted gold/blue for allocated charges and gray for the rest. Allocation confirmation returns to the Bonfire menu only after server success, even when unchanged; Escape discards the draft. Reinforcement stays open after purchase.

Bonfire-origin Level Up, Allocation and Reinforcement share `BonfireSubscreenTransition`: a transparent offscreen page composite slides left 24 GUI pixels while fading in, and right while fading out, over 320ms. Inputs are gated during transition; exit switches screens only once after completion. No world blur, rest/reset, camera or player-animation changes occur. Non-Bonfire upgrade entrances retain their own presentation. Future child pages can reuse the same transition without taking ownership of gameplay authorization.

## Optional Souls Combat HUD integration

The client adapter targets `souls_combat_hud` **1.3.1 NeoForge**. Its nine-slot consumable selector recognizes crimson and, only with mana support, ashen flasks. Explicit Souls HUD consumable/auto-use blacklists remain authoritative. Ordinary inventory slots are not searched. No third-party JAR, configuration, item registration, save format or network protocol is changed.

The standalone Flask HUD is suppressed only when the supported adapter is available. The selected consumable (not the held weapon) determines the counter. `remaining / allocated` sits 3 HUD-local pixels to the **left** of the main frame, with its bottom 1 pixel above the frame bottom. Names retain their normal location and fade; all preview rows remain in their original positions. Geometry inherits the actual HUD transform. An impossible custom layout suppresses the counter rather than painting over or moving another element; F1 or a disabled equipment/consumable HUD hides it. Missing/unsupported HUD integrations retain the standalone display and report incompatibility once.

Quick-use sends one ordinary item-use intent and waits for existing authoritative Flask snapshots, including an explicit rejection snapshot. Repeated use requests are suppressed while waiting/drinking. Finish, rejection and interruption restore the previous hotbar slot only while the adapter still owns the selected slot; manual slot changes, death, disconnect or dimension changes revoke ownership. The response-wait safety timeout is 100 client ticks. No charge or recovery is predicted, and the 14-tick commit / six-step recovery remains server-owned.

Key bindings remain owned by Souls Combat HUD. Its default `G` quick-use key can conflict with Curios' inventory key; rebind one in Minecraft Controls if both are installed. The adapter does not change either mod's bindings or bypass Souls HUD's Epic Fight held-item switching lock.

For isolated integration runs, use `-PwithSoulsCombatHud=true -PsoulsCombatHudJar=<local JAR>` and NeoForge `21.1.244` (the HUD's requirement). This does not raise MaplesAdventure's declared minimum. Optional mixins are client-only; neither dedicated servers nor installations without the HUD link its classes.

The three hooks have their own optional Mixin configuration. Its early gate checks the HUD version and all injection signatures from bytecode without loading its classes; missing or incompatible targets skip all three hooks. The client then verifies markers and reflective APIs before enabling the adapter, warning once and retaining the standalone display on failure. Unrelated Phase/Bonfire mixins are not gated by this compatibility check.

## Development checks

OP-only `/ma flask status [player]`, `refill`, `reset`, `set_capacity <4–14>`, `set_potency <0–12>`, `set_allocation <crimson> <ashen>`. Execute as a selected player for mutation commands. These are not survival upgrade entrances.

Unit tests cover frozen tables, bounds, every allocation, normalization, NBT copy, optional-mod migration, effect timing, icon mapping, codecs and API boundaries. In an isolated test world run `runServer -PweaponRegression=true`, then `flaskregression`. This synthetic server fixture exercises the real controller/resource mutation and material transactions, but is not a replacement for real-client animation/menu, restart and multiplayer acceptance tests.
