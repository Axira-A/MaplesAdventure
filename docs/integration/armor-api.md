# Armor Integration API v1

> Language: [简体中文](armor-api.zh-CN.md) | **English**

MaplesAdventure armor profiles add fixed equipment contributions to its nine damage-defense channels and four status-resistance families. Minecraft's armor points, armor toughness, durability, enchantments and native damage reduction remain separate. The profile does not grant percentage absorption or ailment immunity, and it has no attribute scaling. An unprofiled diamond or netherite chestplate contributes **zero Maples equipment defense** while retaining its normal Minecraft armor behavior.

Channels are `physical`, `slash`, `strike`, `pierce`, `magic`, `fire`, `lightning`, `ice`, `holy`. Standard physical does not raise the other three physical channels. Resistance families are `immunity` (Poison, Scarlet Rot), `robustness` (Bleed, Frostbite), `focus` (Sleep, Madness), and `vitality` (Death Blight). These values add to the `equipment` part of the existing player breakdown; they are defense pressure and status thresholds, not percentages. A player can have both Vanilla armor mitigation and Maples channel defense.

Define static per-item or tag rules under `data/<namespace>/maplesadventure/armor_profiles/`. Only items actually equipped in HEAD, CHEST, LEGS or FEET count. A custom equippable item need not subclass `ArmorItem`. Main hand, off hand, inventory and Curios do not count. Exact item rules precede tag rules; priority descends within each group, then resource ID sorts lexically. An exact `disabled:true` rule suppresses an inherited tag profile. Reload recompiles item lookups and updates online player snapshots; no armor data is persisted to the player.

`MaplesArmorApi.query(stack)` returns a detached optional profile for an explicit rule. `MaplesArmorApi.equipped(player)` returns four-slot totals and the slots with profiles. Both are read-only logical-server-thread calls. The client Character Stats and Level-Up Preview consume a bounded server-authored equipment snapshot. `MaplesDefenseApi.query(player)` naturally includes these contributions in effective defense. No public armor mutation API exists.

The [Holy Knight chestplate example](examples/datapack/data/example/maplesadventure/armor_profiles/holy_knight_chestplate.json) provides holy defense +18, fire +9, robustness +12 and vitality +10, alongside its other channels. `examplemod` must register a wearable item. [ArmorIntegrationExample.java](examples/ArmorIntegrationExample.java) shows public query calls and is compile checked.
