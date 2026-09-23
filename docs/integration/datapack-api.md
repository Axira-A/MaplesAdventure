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

## Weapon and armor rules

The following are public schema v1. All paths have the form
`data/<namespace>/maplesadventure/<directory>/<path>.json`. Rules reject unknown fields and
bad bounds per file. Except `weapon_infusions`, a rule selects exactly one `item` or `tag`, an
optional integer `priority` (default 0), and domain-specific fields. Exact item beats tag;
within a selector group priority descends, then file ID sorts lexically. At most 4096 rules per
directory load. Successful reload recompiles item lookups after tags are available; no JSON
parse or tag scan occurs per hit. Compiled rules change new weapon attacks and current armor
defense immediately. Already launched projectiles keep their launch snapshot.

| Directory | Fields and validation |
|---|---|
| `weapon_requirements` | `requirements` object with strength/dexterity/intelligence/faith/arcane integer 0–99; `priority` -10000..10000; `disabled` boolean suppresses lower-priority/tag/integration fallback for this rule. |
| `weapon_scaling` | `scaling` object with the same five keys, finite coefficients 0–1.5; optional `max_bonus` finite 0–2 (default 1.15), `priority` -10000..10000, `disabled`. Coefficients are numbers, not letter grades. |
| `weapon_damage_profiles` | `components` array 1–8, unique `channel` per component, finite `base_ratio` 0–2, sum (0,2]. A component's optional `scaling` object overrides inherited weapon scaling; `{}` means no scaling. `max_bonus` requires explicit `scaling`. `priority` -10000..10000; `disabled` reverts to automatic archetype damage. |
| `weapon_status_buildup` | Optional `statuses` object, up to seven status keys; optional `weight_class` = throwing/normal/great/colossal; `priority` integer -1000000..1000000. Each status accepts `base_buildup` 0–1000, `arcane_scaling` 0–2, `arcane_policy` none/explicit/follow_weapon_arcane. Frostbite, Scarlet Rot and Death Blight forbid ARC scaling. No `disabled` field in this schema; an empty `statuses` rule shadows a tag rule. |
| `weapon_infusion_eligibility` | Optional `infusible` boolean (default true), optional `allowed` array of at most 32 unique infusion IDs, `priority` -10000..10000. `infusible:false` leaves only normal. Empty `allowed` on an infusible rule uses all currently allowed non-special definitions. Special frenzied/rot/blight require explicit eligibility. Unknown IDs are discarded at compile time. |
| `armor_profiles` | `channels` object with nine finite nonnegative values 0–1000 and/or `resistances` object with immunity/robustness/focus/vitality values 0–1000. `priority` integer -10000..10000; `disabled:true` exact rule blocks tag inheritance. Four equipped slots sum to at most 4000 per field. Empty non-disabled profile is rejected. |

Damage channels are physical, slash, strike, pierce, magic, fire, lightning, ice, holy.
`physical` is standard physical and does not mean slash+strike+pierce. Status keys are bleed,
poison, scarlet_rot, frostbite, sleep, madness, death_blight. Each channel of one weapon hit
passes its own defense pressure calculation, but the game performs one final damage event.

`weapon_infusions` is also public schema v1. Its **file ID is the infusion ID**, with no
item/tag selector or priority. Up to 32 definitions. Fields: `display` translation key (at
most 128 characters), `icon` resource ID (at most 256), `base_multiplier` finite (0,2],
`physical_scaling` keyed by the five weapon attributes with optional `multiply` 0–2,
`minimum`/`maximum` 0–1.5, `element` (null or object with `channel`, `physical_ratio`,
`element_ratio`, `scaling`, optional `max_bonus`), and `statuses` using the status component
grammar above. `future_buildup` remains readable for old packs but is deprecated; new packs
should define `statuses`. Custom infusion IDs exist only after a definition loads and an item
is eligible; a custom ID is not automatically applied to all weapons.

The official [Holy Blade and Holy Knight examples](examples/datapack/data/example/maplesadventure/)
use these exact parsers. The example IDs require `examplemod` items to be present for runtime
resolution. Spell-school and status-effect definitions remain internal schemas.
