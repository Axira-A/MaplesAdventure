# RPG Round 6 — Weapon Requirements Core

日期：2026-09-06。范围仅武器需求、未满足惩罚、武器战技入口和属性预览；未加入 Weapon Scaling、AR、质变、元素或异常系统。未修改第三方 JAR，也未修改物品 NBT。

## 1. 实际识别与服务入口

`WeaponRequirementProfile` 保存 STR/DEX/INT/FTH/ARC（0–99）以及 source、weaponClass、debugReason。`WeaponRequirementService` 统一提供 profile/evaluate/meetsRequirements/missingRequirements；Item → Profile 在启动、reload 时编译，攻击路径不再扫描属性或计算启发式。

严格顺序：Exact Item JSON → Tag JSON → 显式 integration rule → Epic Fight 分类 → Java 武器类型 → generic attack attributes → NONE。精确覆盖不依赖可选分类器成功。相同层内 priority 降序、文件 ID 排序消除不稳定性。

排除 ArmorItem、ShieldItem、BlockItem、Food；其余优先可靠 EF WeaponCapability。Java fallback 支持 Sword/Axe/Trident/Bow/Crossbow/Mace/Digger/ProjectileWeapon。Generic 要求主手同时有伤害和攻速、伤害至少 4、合理攻速且可损坏。工具使用低需求；盾牌留待装备需求系统。

## 2. Epic Fight 实际 API / 修复的分类陷阱

审计工作区 Epic Fight **21.17.3.1**：`EpicFightCapabilities.getItemStackCapability`、`CapabilityItem.getWeaponCategory/isWeaponCategory/getAllAttributeModifiers`、`WeaponCapability`、`Style`。实际基础类别：NOT_WEAPON、AXE、FIST、GREATSWORD、HOE、PICKAXE、SHOVEL、SWORD、UCHIGATANA、SPEAR、TACHI、TRIDENT、LONGSWORD、DAGGER、SHIELD、RANGED。

`CapabilityItem` 构造器在 builder.category 为空时默认 FIST，且 `isEmpty()` 只比较 EMPTY 单例。真实专服校准发现盔甲、弓和一些标准武器因此被误认成拳套；已增加 WeaponCapability 检查、非武器排除，以及标准 Java 武器的默认 FIST fallback。Mace 现在为 MACE，而非 FIST。

未知扩展类别保留原类别元数据、使用 GENERIC 基线，不依据物品名猜刀/枪。Nightfall (`efn`, 3.4.0) 的 EFN_YAMATO 没有公开父类别，因此暂走 GENERIC；需要整合包 override 精调。GREATAXE/SCYTHE 保留语义 archetype，但不声称工作区存在对应可靠 EF 原生 enum。

## 3. Archetype 与完整启发式

基线 STR/DEX：TOOL 5/5；DAGGER 5/9；SWORD 8/7；LONGSWORD 10/8；GREATSWORD 16/9；GREATAXE 18/7；AXE 11/7；SPEAR/TRIDENT 9/11；UCHIGATANA 8/13；TACHI 10/13；FIST 5/8；SCYTHE 10/12；BOW 5/12；CROSSBOW 9/10；MACE 16/7；GENERIC 8/7。

令 round 为 Java Math.round，clamp 为闭区间限制：

```
weightAdjustment = weight <= 0 ? 0 : round(clamp(3*(sqrt(weight)-sqrt(5)), -2, 8))
damageAdjustment = round(clamp((damage-6)*0.7, -3, 7))
slowAdjustment   = round(clamp((1.6-speed)*2, 0, 3))
fastAdjustment   = round(clamp((speed-1.6)*1.5, 0, 3))
STR = baselineSTR + weightAdjustment + damageAdjustment + slowAdjustment
DEX = baselineDEX + fastAdjustment + round(max(0,damageAdjustment)*0.3)
```

弓弩不以默认近战 1 damage / 4 speed 推导投射性能，以上 damage/slow/fast 贡献对它们为 0。低伤害（≤5）、低重量（≤5）的直剑/工具降至 5/5；工具 STR 最多 10、DEX 5。自动非零要求最终限制 5..autoCap，默认 autoCap=40；手工 JSON 仍允许 0..99。

## 4. Weight / Damage / Speed

实际审计未发现原有独立 `EquipmentWeightService`；原负重服务通过 Epic Fight 玩家 WEIGHT 当前值减 base 计算装备负重。新增同名小型服务复用同一 WEIGHT attribute，读取默认物品主手修饰符的净贡献，不读取/写入玩家身体重量，也不改变现有负重算法。

伤害/攻速从默认 ItemStack 主手修饰符读取，尊重 ADD_VALUE / ADD_MULTIPLIED_BASE / ADD_MULTIPLIED_TOTAL。这些值只在编译规则时读取。50 个实际样本 WEIGHT 均为 0（未声明）；没有伪造测试重量。7 与 13 重量差异另有数学测试。

## 5. Iron’s 魔法 Affinity

Iron’s 3.16.3 optional adapter 扫描默认主手正向属性，与真实 SchoolRegistry 的 school power attribute holder 比较，复用上一轮 `IronsSchoolPowerAccessor` 和 `SpellSchoolScalingProfile`。

每个可靠 school power amount：`r = 10 + min(10, amount*20)`；按该 school 已有 INT/FTH/ARC 权重求需求，每项取各 school 最大值，向上取整，非零限制 5..autoCap。无属性来源就保持魔法要求 0。不会因为武器名称含 fire/blood/holy 添加要求。也不修改上一轮纯法术伤害。

## 6. 实际 50 件校准

见 [progression-round6-calibration.md](progression-round6-calibration.md)。表来自最终代码运行的专服注册表，覆盖 Vanilla、Epic Fight、Nightfall、Iron’s。没有用 30 个精确 override 掩盖启发式问题。

## 7. Datapack

路径：`data/<namespace>/maplesadventure/weapon_requirements/<name>.json`。

```json
{"item":"minecraft:diamond_sword","requirements":{"strength":18,"dexterity":12}}
```

```json
{"tag":"mypack:great_weapons","priority":100,"requirements":{"strength":24,"dexterity":10}}
```

```json
{"item":"mypack:special_item","disabled":true}
```

item/tag 二选一；priority -10000..10000；要求整数 0..99；拒绝未知字段/不属于五项武器属性的字段/非法数值。最多 4096 条规则，坏文件单独记录并跳过。`/reload` 重新编译、同步在线客户端、刷新升级 policy view。

已实测：Exact 6/5/7 压过 priority=10000 的 Tag 50/20；另一个 Tag 物品正确 50/20；golden_sword disabled 后无需求；负数规则被拒绝但其他规则正常加载。隔离测试包最后已禁用，不打进 Mod JAR。

## 8. 网络与客户端真相

复用 MessageNetwork，协议 **13 → 14**。只新增 server→client registry batch，没有 C2S 改需求/改属性接口。

每包最多 32 条、131072 bytes、最多 1024 批/32768 items；item ID ≤256 字符，source ≤32、class ≤64、debugReason ≤512，属性 0..99，倍率 0.1..1。revision UUID、index、total 验证后原子替换客户端缓存，登录/reload 同步，不每 tick 广播。

本地 AttributeSnapshot 另携带主/副手规范 Item ID、需求 profile 和惩罚倍率，不发送整个装备 NBT。装备变更沿用已有延迟刷新机制。

## 9. Tooltip / Character Stats / Draft

Tooltip 只显示非零需求；不足红色，满足灰色；显示当前伤害效率和战技限制提示。en_us/zh_cn 各补 13 个语言 key。已在 Nightfall 客户端实际看到钻石剑红色 STR/DEX、35% 提示。

完整 Character Stats 增加当前主/副手需求、当前值、满足状态、效率和战技需求许可。实机检查过大剑主手红字与 35%。文案只说明“需求允许”，不承诺技能已学习或资源足够。

Level-Up 使用 baseline WeaponLoadoutSnapshot + draft attributes 调用同一个 evaluate；需求变化优先于一般派生数值。实测 STR20→21 草稿时服务端仍 STR20、XP10000。小窗口中状态转换文本曾被截断，最终已改多行 wrap；完整换行后的实机画面尚需复核。没有加入假 AR 或真正的武器补正。

## 10. Vanilla / Epic Fight Damage

唯一乘法点：`WeaponRequirementEvents.damage`，NeoForge `LivingDamageEvent.Pre` 的 LOWEST。在 Epic Fight 默认优先级的 `VanillaEntityEventHooks.onCalculateDamagePre` 完成 extra/modifier/source-specific 计算之后应用一次惩罚；没有在 AttackEvent/IncomingDamage 再乘一次。

Vanilla 仅 `PLAYER_ATTACK` 且 direct/source 是该 ServerPlayer，使用主手（原版 Player.attack 的手）。EF 使用 `EpicFightDamageSource.getUsedItem()`，支持实际副手来源；明确空手不退回不合格主手。法术、环境、火焰、摔落或其他实体来源不靠当前手持物猜测。

真实客户端连接隔离专服后，命令驱动的标准 damage 管线测试：1000 HP 测试牛受 100 player_attack 后变 965；满足要求后下一次变 865；重新不足并用 magic 来源后变 765。该测试证明单次服务器惩罚及魔法来源排除，**不等于完整人工 Epic Fight 连击验收**。

## 11. Weapon Innate / Nightfall 边界

`EpicFightWeaponRequirementMixin`：optional @Pseudo、require=0，在 `SkillContainer.canUse` HEAD 仅检查 WEAPON_INNATE + ServerPlayer。其所有权由当前版本主手 CapabilityItem 安装；普通武器伤害则使用上文实际 getUsedItem，而不是永远主手。

选这个入口的原因：requestCasting/requestHold 在执行/消费之前调用 canUse；其中已有蓄力释放的 early-return 能绕开普通 CAST_SKILL event，因此单靠公开 cast event 不完整。

未满足时返回 false 和本地化 actionbar，不调用执行/消费，不主动扣资源或开冷却。没有按动画 ID 判断，也不阻止装备或普通挥舞。

Nightfall 3.4.0 + Invincible 21.15.8.2 专服/客户端实际启动成功，标准 WEAPON_INNATE 容器路径复用此限制。**Invincible ComboBasicAttack 还公开直接 executeNodeOnServer / executeOnServer 及 Nightfall 专属 arts 等入口，不能声称它们全部经过 canUse。绕过标准容器的连段/专属技能是本轮明确兼容缺口，未加入模组名伤害 hack。**完整技能拒绝时零耐力/零冷却的手动对照仍待验证。

## 12. 投射物

`ProjectileRequirementPenalty` 是 NeoForge serializable entity attachment，仅玩家且已识别武器发射时创建：owner UUID、qualifiedAtLaunch、multiplier。不保存整套 profile，不每 tick 更新。NBT round-trip 及随后属性变更不影响快照已通过单元测试。

`ProjectileWeaponRequirementMixin` 在真实 `ProjectileWeaponItem.shoot` 的 `ServerLevel.addFreshEntity` 前记录实际 weapon/shooter，覆盖弓/弩，包括弩烟花路径。`EntityJoinLevelEvent` 对新 AbstractArrow 使用真实 `getWeaponItem()`；ThrownTrident 的该方法返回被投掷的三叉戟。重载实体沿用附件，不根据当前武器重算。

已实机右键发射预装箭的弩并命中牛，HP 765→761.5。另一轮落地箭因贴近玩家而被拾取，未成功实机读取其 NBT，**不把这次操作写成快照持久化通过**。

弓长按、三叉戟投掷、发射后换武器继续飞行，以及跨 chunk/restart 的完整实射回归仍待人工验证。无可靠发射武器的 modded projectile 不猜当前手持物；adapter 可调用 `WeaponDamagePolicy.recordLaunch`。

## 13. Optional / 生命周期回归

- 无 Epic Fight、无 Iron’s：专服 Done，编译 34 个 Vanilla profile；客户端到达可操作多人菜单，没有启动崩溃。@Pseudo 缺少目标的 WARN 存在，但不是 MixinApplyError。
- EF + Iron’s：真实专服、真实客户端连接成功；registry batches 和 AttributeSnapshot 未导致断连。
- EF + Iron’s + Nightfall + Invincible：真实启动/连接与 Tooltip/弩射击正常。首次 Nightfall 启动因缺少它自己的必需依赖 Invincible 失败，补齐工作区既有 JAR 后重跑通过。
- 无单独完成“只 EF、不 Iron’s”第三套实机组合；两者均无的 optional fallback 已运行。
- 本轮不改 Phase/Coop/Invasion/LostSoul/XP 数据；未执行完整多客户端 PvP/LostSoul 回归，不把代码不变当作人工通过。
- 缓存热路径已审计，没有实际完成 100 次攻击性能采样。

## 14. 管理员接口

`/ma weapon requirement` 检查当前主手；`/ma weapon requirement <item>` 指定物品；`/ma weapon audit` 导出已编译物品。兼容 `/maplesadventure` 根。权限≥2。输出识别、类别、需求、来源、启发式贡献；自动分类项同时有 weight/damage/speed；玩家执行时有 missing/satisfied/multiplier/innateAllowed。

## 15. 新增 / 修改文件

新增（相对 src/main/java/dev/maplesadventure）：

- `config/WeaponRequirementConfig.java`
- `progression/encumbrance/EquipmentWeightService.java`
- `progression/weapon/WeaponRequirementProfile.java`, `WeaponRequirementResult.java`, `WeaponRequirementArchetype.java`, `WeaponFacts.java`
- `progression/weapon/WeaponClassifier.java`, `WeaponRequirementHeuristic.java`, `WeaponRequirementRules.java`, `WeaponRequirementService.java`
- `progression/weapon/WeaponIntegration.java`, `WeaponIntegrations.java`, `WeaponDamagePolicy.java`, `ProjectileRequirementPenalty.java`
- `progression/weapon/WeaponRequirementEvents.java`, `WeaponRequirementCommands.java`, `WeaponRequirementNetwork.java`, `WeaponLoadoutSnapshot.java`
- `progression/weapon/client/ClientWeaponRequirements.java`, `WeaponRequirementText.java`
- `integration/epicfight/progression/EpicFightWeaponRequirements.java`, `integration/irons/progression/IronsWeaponAffinity.java`
- `mixin/EpicFightWeaponRequirementMixin.java`, `mixin/ProjectileWeaponRequirementMixin.java`

修改：`MaplesAdventure.java`、`client/MaplesAdventureClient.java`、`network/MessageNetwork.java`、`ProgressionAttachments.java`、`ProgressionEvents.java`、`AttributeSnapshot.java`、`LevelUpPreviewCalculator.java`、`progression/network/AttributePayloads.java`、`progression/stats/CharacterStatsSnapshot.java`、`progression/client/CharacterStatsScreen.java`、`LevelUpScreen.java`、mixins JSON、en_us/zh_cn JSON。

测试/文档：`src/test/java/dev/maplesadventure/progression/WeaponRequirementTest.java`（9 tests）、`.research/Round6Rcon.ps1`、两个 Round6 隔离专服目录、隔离 world datapack fixture、本文与校准表。所有运行测试只使用本机隔离世界，不部署生产服。

## 16. 最终构建与交付边界

`gradlew clean build`：**BUILD SUCCESSFUL，1m 2s，64 tests / 0 failures / 0 errors**。有两个原有 deprecated 警告（EncounterConfig/EncounterSpawnService），无新增编译错误。

JAR：`E:\RPG Menu Framework\MaplesAdventure\build\libs\maplesadventure-0.2.0.jar`

大小：1,892,762 bytes。

SHA-256：`8EB1280C079394A71AF11411B207DB1C5B896478F46E75754D9FF1CC856AAD32`

已实际检查 JAR 包含需求服务、两条 Mixin、Projectile attachment、loadout snapshot 和中英资源；未打入测试 datapack 或第三方 JAR。启动测试使用 Gradle dev runtime 的同版代码，未声称全部测试均为最终成品 JAR 部署验证。

结论：核心代码、缓存/同步、JSON 覆盖、UI 接入与构建已完成；上文列出的弓/三叉戟完整实射、完整 EF/Nightfall 战技零消费、双持与最终 wrap 实机复核尚未全部通过人工验收。本轮不能标记为所有 TEST 1–26 已验收。
