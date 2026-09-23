# Weapon Integration API v1

Content mods register their own Minecraft `Item`, attack damage/speed modifiers, models, textures and animations. MaplesAdventure reads the stack's MAINHAND attack-damage modifiers as base attack; there is no second base-attack registry.

Five weapon attributes (`STRENGTH`, `DEXTERITY`, `INTELLIGENCE`, `FAITH`, `ARCANE`) drive requirements and numerical scaling. Use the [public datapack schemas](datapack-api.md) for static definitions. `MaplesWeaponApi.query(stack)` gives a detached, resolved view after the current infusion. `evaluate(player, stack)` uses current server attributes and configuration for requirement deficits, per-channel nominal attack rating, and status buildup at motion value 1. Both calls require the logical server thread after registry compilation. An empty result means invalid context or no resolved weapon; it does not mean zero attack power. Neither method grants client authority.

The nine channels are standard `PHYSICAL`, `SLASH`, `STRIKE`, `PIERCE`, `MAGIC`, `FIRE`, `LIGHTNING`, `ICE`, `HOLY`. Standard physical is separate from slash, strike and pierce. A split weapon contributes to several channels within one Minecraft damage event and one `hurt` call. Do not implement an element by calling `hurt` twice. Nominal attack rating excludes the unmet-requirement penalty and target mitigation.

Each damage component has a `base_ratio` of the Minecraft base attack and its own resolved scaling coefficients. A ratio such as `.65` physical plus `.35` holy allocates base attack; actual final damage proportions may differ after attribute scaling and target defense. Status buildup is separate from attack rating; Arcane policies are `NONE`, `EXPLICIT`, and `FOLLOW_WEAPON_ARCANE`. Query exposes the resolved profile, including infusion ID, allowed infusions, and optional status weight class. Infusion changes require the existing authorized game workflow, not a public mutation call.

Projectile weapon composition is frozen at launch. Attribute upgrades, equipment changes, infusions and datapack reloads do not retroactively change an in-flight projectile. New attacks use the latest compiled definitions.

The [Holy Blade example datapack](examples/datapack/data/example/maplesadventure/) defines `examplemod:holy_blade` with STR 12, FTH 24, physical `.65` and holy `.35` components. The item itself must be registered by `examplemod`. [WeaponIntegrationExample.java](examples/WeaponIntegrationExample.java) is compiled with this repository's integration examples task and uses public types only.
