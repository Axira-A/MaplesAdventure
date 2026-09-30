# Logic Authoring 1.1 (experimental)

> Language: **English** | [简体中文](logic-authoring.zh-CN.md)

Authoring definitions and execution are separate. `LogicComponent` is versioned Scene data;
`LogicRuntime` compiles validated bindings on load/revision change. Editor clients never execute
gameplay actions. This is an Event/Condition/Action list, not a scripting engine or node graph.

## Definitions and Inspector

An object may have one `maplesadventure:logic` component with at most eight bindings. Each binding
has its own stable UUID, enabled flag, typed Event definition, condition tree and ordered Actions.
A definition stores a namespaced type ID, data version and bounded registered scalar fields.
It cannot name a Java class, execute a command, or supply arbitrary NBT for merging.

Conditions are `ALL`, `ANY`, `NOT` or a typed leaf. Empty ALL is true, empty ANY is false, and NOT
has exactly one child. Evaluation short-circuits. Limits are depth 8 and 64 nodes per binding,
16 actions per binding, 32 scalar fields per definition, and 256 types per registry. Existing
component (16 KiB), object (64 KiB), Scene, string and request limits also apply.

The Inspector discovers Event/Condition/Action schemas from the server. Searchable pickers show
translated labels and namespaced IDs; boolean toggles, enum pickers and scalar fields edit local
drafts. Add/remove/reorder operations stay local until Apply or Save. Save commits the current
object name, transform, component field changes and logic together under one revision check.
It marks SavedData dirty; it does not force a full world save. A rejected draft makes no partial
authoring change. Unrelated object deltas retain selection, local panel sizes and the current draft.

Unknown/removed types, future versions and damaged data are retained for diagnosis, not executed.
Validation identifies object/component/binding, including missing volume, incompatible player
context, unknown fields/type, invalid flags, invalid sound registry entry and structural limits.
Known type fields cannot be silently coerced or clamped by client submissions. The scene remains
inspectable when a binding is invalid; remove/repair the invalid component explicitly.

## Built-ins

| Registry | ID (namespace `maplesadventure`) | Behavior |
| --- | --- | --- |
| Event | `player_enter_volume` | Outside → inside edge, with triggering player |
| Event | `player_exit_volume` | Inside → outside edge, with triggering player |
| Condition | `always` | True |
| Condition | `flag_equals` | `scope`: WORLD/PLAYER, `flag`: ResourceLocation, `value`: boolean |
| Action | `set_flag` | Same fields; writes through the server flag service |
| Action | `show_message` | `text` (max 512), `translate`; literal text or translation key |
| Action | `play_sound` | `sound`: registered SoundEvent ID; sent only to the triggering player |

Events with no player context reject player-only actions and PLAYER-scoped flags. WORLD flags are
global to this save, **not per Phase**. PLAYER flags belong to the UUID and persist through the
copy-on-death Attachment. Unset flags are false. WORLD flags use `maplesadventure_game_flags`
SavedData; PLAYER flags use `maplesadventure:game_flags`. Both are versioned, boolean-only and
bounded to 4096 entries; unsupported/corrupt stored data remains read-only instead of being lost.

## Volumes and runtime

Trigger Volume creation atomically adds a Box and empty Logic component, without prewritten story
logic. Existing `box_volume` and `radius` components supply geometry. Box uses the object's rotation;
radius is a sphere. With both present, their union is the trigger. A player's feet position is tested.
No helper Entity, BlockEntity or chunk loading is needed.

The index partitions by dimension and spatial cells (16/64/256/1024/4096 block levels); each volume
uses a level with bounded occupied cells. A player queries at most five cells before precise bounds
tests, rather than scanning all Scene objects. Definitions/codecs are not decoded in the tick path.
Per-player edge sets emit one Enter/Exit per transition. Logout/dimension change clears edges
without a synthetic Exit; a later gameplay entry can emit Enter again. Normal spectators are not
globally excluded: **an active Editor session** is the exclusion criterion. Dead/disconnected players
are also skipped. Closing the Editor restores the origin before gameplay trigger evaluation resumes.

A server-wide 4096-operation/tick budget covers bindings, conditions and actions, including nested
addon emissions (depth at most 16). Callbacks must be synchronous, bounded and non-blocking.
Actions run in order, not as an all-or-nothing gameplay transaction: already executed actions cannot
be rolled back if an addon fails. Failure/budget exhaustion stops that binding, logs its source and
disables it until its Scene revision changes. This intentionally favors safety over silent retries.
Invalid bindings are excluded when compiling; other valid bindings remain usable.

## Extension API

Use experimental `MaplesAuthoringApi.registerEventType`, `registerConditionType` and
`registerActionType` during common setup before server startup/freeze. The three registries are
separate and reject duplicate IDs. Each type reuses the existing `ComponentDescriptor<T>` contract:
defaults, Codec, version, display key, typed field accessors and validation. A custom immutable
record is preferable to mutable maps. The shared Inspector needs no custom Screen for new types.

See the compile-checked [LogicAuthoringExample](integration/examples/LogicAuthoringExample.java).
An Event publisher calls `MaplesAuthoringApi.emit` on the server thread with Scene/object/event IDs
and optional player. `LogicContext` supplies identities and server runtime references, not editable
Scene/SavedData collections. Flags change only through explicit API methods. An addon remains
trusted server Java code: the framework cannot sandbox a callback that performs unbounded work.
Common descriptors must not import client renderers; existing client Gizmo registration stays separate.

## Reproducible acceptance

1. F8 as authorized Survival author; create/select a Scene and add Trigger Volume.
2. Add Enter binding, condition WORLD `example:triggered == false`.
3. Order actions: set that flag true, show `Trigger executed`, play `minecraft:block.note_block.pling`.
4. Apply/Save and Validate; exit to the original Survival location, then enter the volume.
5. The message/sound occur once. Staying inside emits no new edge; re-entering fails the flag condition.
6. In Editor, flying through does not execute gameplay bindings. Save/restart retains definition/flag.

Unit tests cover schemas, trees, bounds, unknown payload preservation, revisions, codecs, budget,
layout and non-inertial navigation. In an isolated `-PweaponRegression=true` server, run
`/ma logicregression`; after a real save/restart run `/ma logicregression persisted`.
The fixture uses actual ServerPlayer NBT/Clone and runtime evaluation with synthetic participants;
it is not a substitute for human observation of two-client layout, mouse behavior or audible output.

No Encounter Action is supplied in 1.1: automatic activation must not bypass Fog Gate, Phase,
attempt and reset authority. Encounter Authoring is a separate future stage. No Prefab, timeline,
arbitrary variables, quests, script execution or combat-system migration is included.
