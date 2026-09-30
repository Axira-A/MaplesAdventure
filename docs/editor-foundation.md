# Editor Foundation / UX 1.1

> Language: **English** | [简体中文](editor-foundation.zh-CN.md)

Experimental authoring with [Event/Condition/Action logic](logic-authoring.md). No Prefab, full Undo, runtime migration,
scene deletion or cross-dimension migration is implemented. Existing gameplay API v1 and resource
licenses remain unchanged; `api.editor` and `api.editor.client` are explicitly experimental.

## Using the editor

F8 is a normal rebindable KeyMapping, not raw key polling. OP level 2 or the integrated world's
actual owner may enter. Creative is not an authorization grant. Bonfire rest, Flask use and control
locks refuse entry. The server stores original mode/dimension/position/rotation in a small
`editor_recovery` Player Attachment before switching to Spectator. It does not change Phase or RPG
state. Death, disconnect, dimension change and permission revocation end the session and restore
the origin/mode. Login/respawn restores orphan markers; recovery shares the same player NBT save
as vanilla game mode. No inventory/XP snapshot is copied. Explicit external game-mode changes
end editing without overriding the administrator's chosen mode. Missing dimensions use the
overworld spawn with a warning. The temporary session, not Spectator alone, identifies an editor.

Create a Scene with a namespaced ID and display name in the current dimension. The Scene selector
searches that dimension's catalog. Objects have server-generated UUIDs: renaming/grouping
preserves identity, duplication creates a new UUID at the same position. Groups are organizational,
without inherited transforms. Deleting a group promotes direct members to its parent.

The left hierarchy and right Inspector scroll independently and resize with draggable splitters.
Defaults are 19% / 25%; min/max widths preserve a central viewport. Ratios persist only in local
`maplesadventure-editor-client.toml`, never in world data or packets. Search/collapse state uses
stable IDs. Bool/enum fields use toggles/pickers; Transform and components have collapsible sections.
Empty/Marker creates an object at
the center-view hit (or four blocks ahead). Select through the hierarchy or world bounds. Edit
fields locally and use Enter/Apply to commit; changing selection or exiting discards drafts.
Delete requires a second confirmation. Component IDs may appear once per object. Unknown or
invalid components retain data, but have no editable fields or component gizmo.

Movement keys navigate immediately without holding RMB; Space/Shift ascend/descend. RMB over the
center turns the view; F faces selection. Client-only spectator travel applies a normalized
0.35-block/tick step with no acceleration/coasting, using the real player and vanilla movement
packets, not teleporting or a detached camera. Text fields, pickers, lost focus and gizmo/splitter
dragging stop movement. A narrowly gated client Mixin bypasses vanilla inertial travel only for
the local spectator with an authorized Editor session. Editor clicks cannot attack/use/place or activate
F/Y/L interactions. World X/Y/Z handles move an object; the Yaw tool rotates about its center.
Pitch remains numeric. Dragging is preview-only until release; Esc cancels. Degenerate axis/plane
views refuse the drag; numeric editing remains available. Objects beyond 128 blocks are culled.
Normal gameplay does not run editor selection/render scans.

## Data and transactions

`authoring` contains immutable Scene/Object/Group/Transform/component values and pure operations.
`editor` owns server sessions, permission and operation dispatch. `client/editor` owns drafts,
selection and presentation. `api/editor` declares common descriptors; client providers are separate.

`maplesadventure_authoring` is global overworld SavedData, loaded at server startup. Scene v1 stores
ID, dimension, display name, revision, groups and objects. An object stores absolute position,
yaw/pitch, group UUID, components and revision. Serialization sorts UUIDs/type IDs deterministically.
No chunk is loaded to save, reset, validate or locate authoring objects.

The version-dispatch entry is `SceneSerialization.load`; current v1 tolerates safe missing
name/transform defaults with diagnostics. Unknown or invalid component payloads remain opaque.
Malformed/future scenes are read-only and their original serialized source is retained by
SavedData. Unsupported root versions preserve the complete root and block writes. Repair warnings
and validation diagnostics identify the Scene/object/component/field when available. Future
component versions are not silently downgraded.

One request performs one operation. Permission, nonce, dimension, selected Scene, rate, Scene
revision and target revision are verified before making a replacement value. Field accessors
return immutable replacements; validators run before one SavedData commit. Scene revision advances
for every success; changed objects/groups advance their own revisions. Group cycles/depth and
world bounds reject the entire edit. Repeated request IDs do not execute again. Stale edits return
the authoritative snapshot; no merging or last-writer-wins behavior is attempted.

## Protocol and limits

Protocol 29 extends bounded `editor_request`/`editor_page` with typed logic and atomic draft commits;
existing gameplay payload fields are unchanged. C2S contains intent, nonce/request IDs, expected revisions and registered scalar fields;
never a client-authored Scene, arbitrary class name or NBT patch. Resource IDs are syntax checked;
fields declaring a registry are also checked for server registry membership.

S2C includes session result, current-dimension catalog, schemas, paged initial snapshots, changed
objects/groups, removed UUIDs, validation and operation results. Only authorized subscribers
receive authoring data. Snapshots replace the cache atomically after assembly; deltas carry prior
and next revisions. A revision gap requests resynchronization. Closing clears drafts, selection
and pages; late responses cannot reopen a closed editor. No full Scene is sent every tick.

| Limit | Value |
| --- | --- |
| Scenes per world | 256 |
| Objects / groups per Scene | 4096 / 1024 |
| Components per object / group depth | 32 / 16 |
| Name / scalar text | 128 / 1024 characters |
| Component / object / Scene stored bytes | 16 KiB / 64 KiB / 16 MiB |
| Snapshot page | 256 KiB |
| Field patch | 64 fields |
| Request token bucket | 20/s, burst 40 |

Coordinates must be finite and inside current dimension height/world border. Radius is finite
and nonnegative; Box dimensions are finite and positive. Native numeric edit fields currently
use text entry with schema validation; they do not silently clamp malformed network values.

## Extension example

Register `ComponentDescriptor<T>` through `MaplesEditorApi.registerComponent` during common setup,
before the first server starts. It supplies a stable ResourceLocation, version, defaults, Codec,
translation key, typed fields and Validator. Registries reject duplicates and freeze before server
startup. Six scalar kinds are supported: boolean, integer, double, string, enum, ResourceLocation;
nullable and read-only metadata are explicit. Callbacks must be pure and bounded; failures do not
grant permission or commit a half-edited value.

The compile-checked [component example](integration/examples/EditorComponentExample.java) registers
an annotation without changing core code. The optional [client gizmo example](integration/examples/EditorGizmoExample.java)
uses `MaplesEditorClientApi` exclusively from client setup. Never reference a client renderer from a
common descriptor. Gizmo registration freezes at load completion; field constraints are available
as Inspector hover hints. A permission extension may grant a trusted server author role; it does not
trust any client flag. The default OP/owner policy is retained.

## Regression and boundaries

Run `gradlew.bat clean test`, `gradlew.bat clean build`; `check` compiles integration examples.
In an isolated `-PweaponRegression=true` server, use `/ma editorregression`, save and restart, then
`/ma editorregression persisted`. The fixture exercises real server request dispatch with synthetic
participants. `flaskdeathregression` uses lethal damage plus Clone/Respawn events for both
keepInventory values; `bonfirefunctional core` verifies existing phase-reset/resource behavior.
These are not substitutes for manual two-client rendering/input, integrated-owner and optional
camera checks. The fixture source is not part of the production JAR.

With the opt-in regression client connected, open an authorized workspace and press F9 to run
the real-client navigation fixture: forward movement without RMB, immediate stop on release,
and movement suppression while a text field has focus. It drives the Screen's input state over
actual client ticks; this development-only shortcut is not registered in the production mod.

Flask death loot excludes only reusable Crimson/Ashen handles, preserving missing-handle regrant,
charges, materials and deliberate Q drops. Encounter cleanup collects loaded targets before
discarding them so removing from the live entity lookup cannot corrupt its iteration.

No native window/focus crash workaround is embedded in gameplay. Native GLFW failures should be
reported separately from Java Editor exceptions. The first release intentionally has conservative
Scene-wide conflicts, no object locks and no full Undo. UI panels, buttons and splitter artwork
are original code-drawn assets; this pass adds no third-party icons, bitmap assets or licenses.
The provided reference informs layout only. See [Logic Authoring](logic-authoring.md) for runtime
limits, extension examples and `/ma logicregression` recovery/trigger checks.
