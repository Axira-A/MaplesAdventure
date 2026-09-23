# Defense API

> Language: [简体中文](defense-api.zh-CN.md) | **English**

`MaplesDefenseApi.query(entity)` returns an Optional detached DefenseView:

- all nine channels with `defense` and fractional `absorption`;
- seven actual status views, including immunity and corrected thresholds;
- requested profile ID for non-players (empty for players);
- unknown-profile indicator and current mitigation pressure configuration.

`channel(entity, MaplesDamageChannel.FIRE)` queries one channel.
`status(entity, MaplesStatusType.MADNESS)` queries the same authoritative status view used by
MaplesStatusApi. No mutable Profile Map, player Attachment or cached internal state is exposed.

`assignProfile(nonPlayer, profileId)` validates an already loaded datapack profile and writes
the existing persistent reference. It returns false on unknown IDs, players, invalid entities,
client/off-thread calls or read-only callback reentrancy. `clearProfileOverride(entity)` restores
normal datapack rule resolution. The reserved `maplesadventure:none` profile is the identity view.

Players use the existing player defense calculator and server enable setting, not Entity Defense
Profile assignment. Defense is not vanilla armor; absorption here is a damage fraction, not
absorption hearts. Neither query nor assignment calls hurt.

Profile references survive entity saves. Reload resolves them against current data; removed
profile IDs fall back to NONE and are reported as unknown, not silently replaced with another ID.
Use the [datapack schema](datapack-api.md) for stable static boss configuration.
