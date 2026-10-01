# Maker-first editor

> Language: **English** | [简体中文](editor-maker.zh-CN.md)

The experimental editor authors existing scenes and real Event/Condition/Action rules.
It introduces no scripts, node graphs, quests, Prefabs or gameplay rollback.

## First scene

Press rebindable F8 with OP level 2 or as the integrated world owner. Create a scene using
only its display name; the server generates a stable ID. Add → Trigger area, edit width,
height and length, then add a rule: **When** a player enters, **If** conditions hold,
**Then** show a message. Apply the draft before Playtest.

Movement works without RMB and has no coasting; hold RMB only to look. Text fields and
internal overlays capture movement. More → Advanced information is a local display preference,
not extra permission. Ordinary display hides UUIDs, revision and full type IDs.
Trigger dimensions/rules come first; position and organization sections collapse.
Diagnostics select the affected object. “Autosaved ✓” means the server accepted the operation
into normal world saving, not an immediate forced disk flush.

## Drafts and shared Undo

Ctrl+Z / Ctrl+Y (or Ctrl+Shift+Z) first undo/redo local drafts. Typing the same field within
500ms merges; structural changes remain separate. Apply is one atomic server history step.
Switching object/scene, exiting or playtesting with drafts offers Apply and continue,
Discard and continue, or Cancel. Failed submissions never execute the continuation.
Conflicts stop dragging and discard invalid drafts; no automatic merging occurs.

When local history is exhausted and no draft remains, Undo/Redo requests the shared scene
timeline. All editors see the same next step and actor (names survive actor logout).
Undo can therefore reverse another author's edit. Tooltips identify the next shared step.
Restored UUIDs remain stable, but scene and affected object/group revisions always rise.
Old requests cannot become valid again; a new edit clears redo.

History holds at most 50 steps, 64 MiB per scene and 128 MiB per server. Oldest entries are
evicted under budget pressure. Only changed objects/groups/catalog entries and scene metadata
are stored, not complete scene copies. History is memory-only and clears on restart.
Runtime flags, items and other gameplay results are never undone.

## Story flags

Pickers combine named catalog entries with IDs referenced by existing rules. Enter a readable
name (Chinese/English Unicode supported); the server creates maplesadventure:flags/<uuid>.
Rename preserves ID; legacy unnamed flags show their path. New UI flag conditions/actions
default to **This player**; **Whole world** is explicit. Existing WORLD/PLAYER scopes stay intact.
The catalog never initializes or clears runtime values.

Scene v2 adds the catalog (1024 entries, 128-character names); v1 migrates with an empty catalog.
The SavedData ID stays maplesadventure_authoring. Future/damaged data retains its original
source read-only.

## Playtest and F8 return

Playtest uses real LogicRuntime and never reverts gameplay outcomes. The server restores original
mode/position before acknowledging the close. A veto keeps the recovery marker and reports failure.
An original Spectator cannot perform an area-trigger playtest.

F8 re-entry first records the new gameplay return point, then restores the server-trusted last
editor pose if its dimension, scene, bounds and already-loaded chunk remain valid. Otherwise
it stays at current position, without force-loading. Selection/tool/scroll return from local
UI context; deleted selections clear safely. Death, disconnect, dimension change and permission
revocation invalidate the context. No client teleport coordinates are accepted.

## Addon presentation (experimental)

Existing Descriptor constructors and public gameplay API v1 are unchanged. Client setup may call
MaplesEditorClientApi.registerPresentation(Target, id, EditorPresentation) before freeze.
Metadata includes category/description translation keys, optional icon resource ID, advanced-only
display and optional natural sentence key. Templates substitute named {field} placeholders for
display only. Categories/descriptions/templates are consumed by schema-backed pickers and rules;
icon IDs are available to client presentation providers, not downloaded or automatically added
to an atlas. No client renderer enters common code. Addons without metadata keep schema forms.
Duplicate/late registration rejects. See the compile-checked
[example](integration/examples/EditorGizmoExample.java).

## Protocol and validation

Protocol 30 adds typed catalog/history/playtest intents only to Editor payloads; other gameplay
formats are unchanged. History requests carry nonce and expected revision, never restore snapshots.
Flag operations carry names or stable IDs, never runtime values. Permission, dimension, readonly,
bounds, rate, size, schema and atomic-commit checks still apply. Snapshots remain paged; delta gaps
request resync. Normal players receive no authoring data.

Run gradlew.bat clean test and gradlew.bat clean build. In an isolated opt-in fixture run
/ma editorregression and /ma logicregression, save/restart, then their persisted variants.
Synthetic participants validate actual server dispatch/recovery, not visual two-client UX.
Manual acceptance: Hello World, per-player first visit, shared Undo with two OPs,
language/GUI scale, camera return and ordinary inputs after exit.
