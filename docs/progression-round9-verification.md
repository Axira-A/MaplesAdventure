# RPG Progression Round 9 — Weapon Infusion Core

## Outcome

Round 9 adds a server-authoritative, per-`ItemStack` infusion layer on top of the Round 8 weapon profile pipeline. An item stores only an infusion identifier and schema version; requirement, scaling and damage profiles remain registry/datapack data and are resolved at use time.

The final pipeline is:

```text
base Requirement + base Scaling + base Damage Profile
                         ↓
              WeaponInfusionState on stack
                         ↓
              WeaponInfusionDefinition
                         ↓
          WeaponCombatProfileResolver.resolve
                         ↓
 WeaponAttackRatingCalculator / WeaponDamageBundle
                         ↓
       existing single LivingDamageEvent.Pre path
```

## 1. Data component and persisted state

- `ProgressionDataComponents` registers `maplesadventure:weapon_infusion` with NeoForge's `DeferredRegister.DataComponents`.
- The component uses both `DataComponentType.Builder#persistent(Codec)` and `#networkSynchronized(StreamCodec)`.
- `WeaponInfusionState` contains only:
  - `ResourceLocation infusionId`
  - `int dataVersion` (current version `1`)
- Codec/wire limits: identifier at most 256 characters; version range 1–16.
- Absence of the component is implicit `NORMAL`.
- Unknown/deleted identifiers and unsupported versions remain stored on the stack, resolve safely to the base profile, and produce `UNKNOWN` UI state instead of mutating the item.
- Custom item names are never modified.

## 2. Definition model and exact built-in transforms

`WeaponInfusionDefinition` contains bounded display/icon metadata, base-ratio multiplier, five offensive-stat transforms, optional elemental split and future buildup metadata. All built-ins are also present as datapack JSON and may be overridden by `/reload`.

| Infusion | Base ratio | Physical scaling transform | Added/split channel | Future buildup |
|---|---:|---|---|---|
| NORMAL | ×1.00 | identity | none | none |
| HEAVY | ×0.95 | STR `clamp(base, .80, 1.50)`; DEX `base × .25`; INT/FTH/ARC 0 | none | none |
| KEEN | ×0.95 | DEX `clamp(base, .80, 1.50)`; STR `base × .25`; INT/FTH/ARC 0 | none | none |
| QUALITY | ×0.94 | STR and DEX each `clamp(base, .55, .65)`; INT/FTH/ARC 0 | none | none |
| MAGIC | total ×1.00 | physical STR/DEX ×.40; others 0 | 65% physical + 35% MAGIC, MAGIC INT .80 | none |
| SACRED | total ×1.00 | physical STR/DEX ×.40; others 0 | 65% physical + 35% HOLY, HOLY FTH .80 | none |
| BLOOD | ×0.90 | STR/DEX ×.60; ARC .55 | retains physical channels | BLEED (metadata only) |
| POISON | ×0.90 | STR/DEX ×.60; ARC .45 | retains physical channels | POISON (metadata only) |

Transforms apply to every physical component of an explicitly eligible profile. Existing elemental components are preserved for explicitly allowed complex weapons. Elemental infusion rejects a duplicate target channel rather than silently merging ambiguous definitions.

## 3. Eligibility

`WeaponInfusionEligibilityService` compiles a bounded per-item allow-list:

- Default infusible: exactly one PHYSICAL/SLASH/STRIKE/PIERCE component.
- Default non-infusible: multiple components or any elemental component; only NORMAL is allowed.
- Datapack rules support exact `item`, `tag`, `priority`, `infusible`, and bounded `allowed` lists.
- Exact rules take precedence over tag rules; compiled rules remain server authoritative.
- A weapon may be recognized by the base scaling registry or an explicit damage profile.
- Admin `set` validates that the held stack is a weapon, the definition exists, eligibility allows it, and the transform produces a valid profile before writing the component.

## 4. Requirement and damage invariants

- `WeaponCombatProfileResolver.apply` passes `base.requirements()` through unchanged for all eight built-ins.
- Requirement failure is still applied once, last, via the existing configured `0.35` multiplier.
- Infusions do not calculate damage themselves. They only transform the profile consumed by the existing `WeaponAttackRatingCalculator` and `WeaponDamageBundle`.
- Direct hits still enter one `LivingDamageEvent.Pre`; there is no infusion event, extra `hurt`, or altered `DamageSource`.
- MAGIC/SACRED channels are semantic components in the one hit context, not separate magic/holy damage invocations.
- Spell, damage-over-time, fall and other environmental sources remain outside `WeaponDamagePolicy`'s weapon path.

## 5. Projectile freezing and compatibility

- Launch-time code resolves the infused stack through the same resolver and writes the resulting frozen `WeaponDamageBundle` into `ProjectileRequirementPenalty`.
- The projectile does not save or re-resolve an infusion ID; later stat changes, weapon replacement, reinfusion or `/reload` cannot change an in-flight projectile.
- The existing attachment registry identifier remains `maplesadventure:projectile_weapon_requirement`; no world-compatibility-breaking registry rename was made.
- Existing legacy snapshot decoding remains in place.

## 6. Client/UI integration

- Protocol version is now `17`.
- Batch zero contains at most 16 bounded infusion definitions; each per-item entry includes a bounded eligibility allow-list.
- Client cache rebuild is atomic and uses the same pure base-profile → infusion transform as the server.
- Tooltip shows infusion, final per-channel AR/scaling and the explicit “not yet integrated” notice for BLEED/POISON buildup.
- `WeaponLoadoutSnapshot.Held` carries the resolved `WeaponInfusionView`; Character Stats and Level-Up Preview therefore consume final infused channels/AR instead of reconstructing infusion math in UI code.
- The live client Level-Up panel was checked with an INT 40 MAGIC diamond sword and displayed separate PHYSICAL and MAGIC channels from the protocol-17 snapshot.

## 7. Datapack and reload behavior

Definitions:

```text
data/<namespace>/maplesadventure/weapon_infusions/*.json
```

Eligibility:

```text
data/<namespace>/maplesadventure/weapon_infusion_eligibility/*.json
```

Runtime verification used a temporary `round9test:test_focus` definition and exact diamond-sword eligibility rule:

1. `/reload` exposed the new definition and allowed it on the existing stack.
2. Runtime profile changed to base ratio `.91` and STR `.90`, proving current registry data is resolved rather than copied into the item.
3. The definition was removed and `/reload` run again.
4. The stack retained `round9test:test_focus`, reported unknown, and safely used the original base profile.
5. The temporary pack was formally disabled in the test world's enabled-pack metadata and removed from the final artifact inputs.

## 8. Persistence and real stack lifecycle

Dedicated-server/client checks used real diamond-sword stacks:

- Three identical item IDs simultaneously carried MAGIC, KEEN and implicit NORMAL states.
- Server entity data showed `maplesadventure:weapon_infusion` with only `infusion` and `data_version`.
- Container insertion/extraction retained KEEN.
- Breaking the container produced an `ItemEntity` retaining KEEN.
- Picking that item up retained KEEN.
- Overworld → Nether → Overworld retained MAGIC/KEEN.
- Survival death with `keepInventory=false` produced item entities retaining MAGIC and both KEEN copies.
- Logout/reconnect and full dedicated-server restart retained the component.
- Saved player data contained the synchronized `weapon_infusion`, `magic`, and `keen` values.
- `PatchedDataComponentMap`/`ItemStack` copy semantics and codec/stream round trips are covered by automated tests.

## 9. Commands

Permission level 2:

```text
/ma weapon infusion info
/ma weapon infusion set <namespace:id>
/ma weapon infusion clear
```

These mutate only the authoritative server-held main-hand stack. There is no player C2S component-writing payload and no Bonfires dependency.

## 10. Automated and runtime verification

- Clean test result: **92 tests, 0 failures, 0 errors, 0 skipped** across 23 suites.
- `WeaponInfusionTest` covers implicit/explicit NORMAL Round 8 identity, all physical transforms, MAGIC/SACRED stat isolation, BLOOD/POISON metadata boundary, unknown/ineligible fallback, complex-profile policy, codecs, network bounds, datapack override and frozen projectile bundles.
- Round 8 single-component AR equivalence is checked from offensive stats 5 through 99.
- Dedicated server without optional mods: successful startup, 8 definitions loaded, 1,333 eligibility entries compiled, protocol-17 client connected.
- Dedicated server with Epic Fight 21.17.3.1 + Iron's Spells 3.16.3 and required libraries: successful startup, 79 weapon profiles and 1,604 eligibility entries compiled; no optional-class linkage failure.
- The damage integration remains the already shared Vanilla/Epic Fight `LivingDamageEvent.Pre` path, with no new mixin or second multiplier hook in Round 9. A fresh automated physical mouse-attack comparison inside Epic Fight was not performed in this closeout; the optional stack was boot-tested and the unchanged one-hit policy is covered structurally/unit-wise.

## 11. Files

Key new code:

- `progression/weapon/WeaponInfusionState.java`
- `progression/weapon/ProgressionDataComponents.java`
- `progression/weapon/WeaponInfusionDefinition.java`
- `progression/weapon/WeaponInfusionRegistry.java`
- `progression/weapon/WeaponInfusionEligibility.java`
- `progression/weapon/WeaponInfusionEligibilityRules.java`
- `progression/weapon/WeaponInfusionEligibilityService.java`
- `progression/weapon/WeaponInfusionBuildup.java`
- `progression/weapon/WeaponInfusionView.java`
- `progression/weapon/WeaponInfusionService.java`
- `progression/weapon/client/ClientWeaponInfusions.java`
- `progression/WeaponInfusionTest.java`

Key modified integration code:

- `MaplesAdventure.java`
- `network/MessageNetwork.java`
- `progression/ProgressionAttachments.java` (registry ID intentionally unchanged)
- `progression/weapon/WeaponCombatProfileResolver.java`
- `progression/weapon/WeaponDamageChannel.java`
- `progression/weapon/WeaponDamagePolicy.java`
- `progression/weapon/WeaponLoadoutSnapshot.java`
- `progression/weapon/WeaponRequirementCommands.java`
- `progression/weapon/WeaponRequirementEvents.java`
- `progression/weapon/WeaponRequirementNetwork.java`
- `progression/weapon/WeaponRequirementService.java`
- `progression/weapon/WeaponRequirementText.java`
- `progression/weapon/client/ClientWeaponRequirements.java`
- `assets/maplesadventure/lang/en_us.json`
- `assets/maplesadventure/lang/zh_cn.json`

Assets/data:

- 8 built-in JSON definitions under `data/maplesadventure/maplesadventure/weapon_infusions/`
- 8 supplied 16×16 icons under `assets/maplesadventure/textures/gui/infusion/`

## 12. Build artifact

```text
gradlew clean build: BUILD SUCCESSFUL
tests: 92 / 92 passed
JAR: build/libs/maplesadventure-0.2.0.jar
size: 2,010,938 bytes
SHA-256: 8E8BF845CC74F7C302DDC4F0D51EBCA48B7F9D51801E3979E92472416833C089
```

The build emitted only existing API deprecation warnings (`defineListAllowEmpty`, `Mob.finalizeSpawn`, and test-only `RegistryFriendlyByteBuf` constructors).
