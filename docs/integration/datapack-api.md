# Datapack integration schema

These fields match the shipped parsers. Unknown fields and invalid numeric bounds reject the
affected definition with a server log message. Reload uses Minecraft datapacks and compiled
lookup tables; no JSON parsing is performed per hit. Do not modify internal Attachments/NBT.

## Entity defense profiles

Path: `data/<namespace>/maplesadventure/entity_defense_profiles/<path>.json`.
ID: `<namespace>:<path>`. Top-level fields: `channels`, `status_resistances`, `status_traits`.
All three are optional; an empty profile is identity defense/default status rules.
`maplesadventure:none` is reserved.

`channels` keys: physical, slash, strike, pierce, magic, fire, lightning, ice, holy.
Each value accepts:

| Field | Default | Bounds / meaning |
|---|---:|---|
| defense | 0 | finite 0–10000 |
| absorption | 0 | finite -.50–.80, fractional |

`status_resistances` keys: bleed, poison, scarlet_rot, frostbite, sleep, madness, death_blight.
Each value accepts:

| Field | Default | Bounds / meaning |
|---|---|---|
| threshold | 160 | finite .001–100000 |
| immune | false | boolean |
| proc_damage_multiplier | 1 | finite 0–10 |
| correction | maplesadventure:none | namespaced ID, at most 256 characters |
| response | stagger_only | stagger_only / deep_sleep / immune; Sleep behavior |

`status_traits` accepts only boolean fields (all default false):
`tarnished_like`, `madness_immune`, `death_blight_immune`, `allow_death_blight`.

Effective immunity additionally respects eligibility: ordinary non-player mobs cannot receive
Madness without tarnished_like; death blight needs tarnished_like or allow_death_blight, and a
managed boss PRIMARY always needs allow_death_blight. Explicit immune traits still win.
Players use their own resistance model, not these entity profile overrides.

Built-in correction IDs: none, standard, resistant, boss (all in maplesadventure namespace).
Standard offsets: 21/49/91/189/479; resistant/boss: 28/70/168/458/915.
These are replacement offsets by proc count, not cumulative sums. Unresolved correction IDs
currently resolve to NONE; spelling matters.

Custom correction path:
`data/<namespace>/maplesadventure/status_resistance_corrections/<path>.json`.

```json
{"stages":[21,49,91,189,479]}
```

Only stages is accepted: zero to five finite, nonnegative, nondecreasing values at most 10000.
NONE is reserved. Correction counts persist independently of the visible bar. Do not reset them
by manually editing Attachments.

## Entity defense rules

Path: `data/<namespace>/maplesadventure/entity_defense_rules/<path>.json`.

```json
{"entity":"minecraft:zombie","profile":"example:boss","priority":0}
```

Exactly one of `entity` or `tag`; required `profile`; optional integer `priority` defaults 0,
range -1000000–1000000. IDs are strings of at most 256 characters.
Exact entity rules precede tag rules; within a group priority descends, then file ID sorts
lexically. Explicit per-entity API assignment precedes these rules.
At most 4096 profiles/rules are retained. Unknown profile references safely use NONE.

[The example datapack](examples/datapack/) has a full nine-channel/seven-status profile and one
vanilla Zombie rule so it loads without a separate demo mod. Install it only in a test world:
that example rule intentionally changes all selected Zombies. Replace the selector with your
own entity ID/tag for production.

## Reload / persistence boundaries

Successful reload updates compiled defense and weapon registry data after tags are available.
New attacks use current definitions. Already launched weapon projectiles retain their launch
snapshot; this API does not replace it. Entity profile references retain their stable IDs across
saves and reloads. Never serialize enum ordinals as a public ID.

Other shipped datapack systems (weapons, infusions, status definitions and spell schools) remain
implemented internally, but this document only promises the defense/correction schema above.
Do not assume an undocumented field or expose an internal parser as a new public contract.
