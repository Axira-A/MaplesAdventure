# Typed damage API

Register `MaplesTypedDamageApi.register(id, priority, provider)` during common setup's
`enqueueWork`. The callback accepts the existing DamageSource and returns
`Optional<TypedDamage>`. Return empty for a source you do not own.

TypedDamage copies its channel map. Entries use MaplesDamageChannel: PHYSICAL, SLASH, STRIKE,
PIERCE, MAGIC, FIRE, LIGHTNING, ICE, HOLY. PHYSICAL is standard physical, not an aggregate.
One to nine entries; finite nonnegative absolute pressure, positive total at most 1000000.

**A provider describes pressure inside one existing attack. It must never call hurt, modify HP,
post another damage event, generate weapon hits, or mutate state.** The registry does not initiate
damage. Original Minecraft/NeoForge and Phase checks still run before final mitigation.

Example: MAGIC=20 and FIRE=10 describe 2/3 magic and 1/3 fire pressure. They do not request two
hits or 30 extra HP loss. Existing weapon/projectile snapshots take precedence over providers.
Providers apply to otherwise unclassified sources before exact vanilla fallbacks; higher
priority wins, then lexical ID. Re-registering an ID replaces it. Registration persists across
world reloads; no automatic per-world deregistration is performed. At most 64 providers.

Important existing scope: generic non-weapon typed mitigation currently applies to player
targets. This facade does not enable new generic enemy mitigation or change weapon balance.
Do not claim custom spells now have weapon scaling/status buildup automatically.

Invalid/throwing provider results are isolated by the internal registry and warned. Public
status/profile mutation in a provider callback is rejected. This contract cannot sandbox a
hostile mod directly invoking vanilla health methods; providers must be pure.

See the [compiled example](examples/BossIntegrationExample.java) for MAGIC + FIRE registration.
