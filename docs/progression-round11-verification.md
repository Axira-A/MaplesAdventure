# Round 11 — Status Buildup / Ailment Core

验证日期：2026-09-16。本文区分实现、自动化验证、真实客户端观察与尚未完成的人工回归。

## 结果摘要

- 实现出血、中毒、猩红腐败、冻伤四种状态，服务端积累/衰减/触发/持续伤害与客户端状态条。
- 复用 Round 7–10 武器、质变、投射物快照、敌人防御及唯一伤害处理入口；没有另造 AR、Phase 或 Lost Soul 系统。
- `gradlew.bat clean build --offline`：**BUILD SUCCESSFUL**，16 秒，124 项测试、0 failure、0 error；其中本轮新增 21 项。
- 三种隔离专服环境均启动并完成服务端状态回归：无 Epic Fight / Iron's；Epic Fight；Epic Fight + Iron's。
- 实际客户端观察了状态条、四种颜色、图标、25/50/75/100% 填充、持续时间模式和 Proc 提示；留存截图见后文。
- 实际 Chunk 保存、关服、重启、重新加载状态验证通过。实际玩家死亡/重生清理通过。
- **没有完成双真实客户端 Coop/Invasion 全流程、所有模组技能、所有投射物物理飞行及资源重载人工验收。不能把这份报告解读为这些场景已全部通过。**

最终生产 JAR：`E:\RPG Menu Framework\maplesadv\MaplesAdventure\build\libs\maplesadventure-0.2.0.jar`

大小：2,148,032 bytes。

SHA-256：`284285909BC822DDD988B84F44CB5CD6761D6AFB3610AC12E55E6B7639B26A6A`。

## 1. 审计与原作规则映射

基于本地 main `d0a39cd` 的 Round 10 实现继续。已有 `DefenseRegression.java` 修改与 `docs/progression-round10-verification.md` 保留，未覆盖、未提交其他用户修改。远端检查因连接重置失败，不能确认远端 main 是否另有更新。

原作参考与本轮平衡分开：采用“积累达到抗性阈值后触发”“ARC 改变积累而非触发伤害/持续时间”“冻伤是爆发加持续减益、火焰解除已触发冻伤”的语义。可追溯的一般属性参考是 [Bandai Namco 官方属性介绍](https://www.bandainamcoent.com/es_mx/news/elden-ring-introduction-part-1-stats)。本报告不以这篇介绍作为精确异常伤害公式的证据，也不声称 Minecraft 默认数值完全复刻某一原作版本。

实现中百分比生命伤害与时长是集中、可数据包覆盖的 Minecraft 平衡值。Epic Fight 的精力恢复使用自身 `STAMINA_REGEN` 乘数，不把其他游戏的绝对恢复点数生硬移植进来。

## 2–6. 状态、持久化和抗性

`StatusEffectType`：`BLEED / POISON / SCARLET_ROT / FROSTBITE`，固定顺序、有界网络枚举。

`ProgressionAttachments.STATUS_RUNTIME` 是正规序列化 Attachment，`dataVersion=1`。每个类型的 Entry 保存：

- `current`、`lastBuildup`、`lastUpdate`；
- `activeStart`、`activeEnd`、`nextDot`；
- `procSerial` 与 `source`；容器另有 revision。

`StatusSourceContext` 保存 source UUID、来源类别、武器 ID、是否投射物、状态类型；NBT 不保存 Entity 引用。非法/越界条目隔离丢弃，旧无状态数据视为空容器。

状态随实体 NBT 保存；非死亡 Clone 拷贝；死亡不 copyOnDeath，且清除 canceled 的玩家 Phantom Death 状态。登出/卸载移除运行时跟踪引用，重载恢复。服务端时间使用 Overworld gameTime 作为跨维度时钟。离线不补算漏掉的 DOT；服务器时钟继续流逝时持续时间可以到期，停服期间不按真实墙钟补扣生命。

`StatusResistance`：`threshold / immune / procDamageMultiplier`。阈值是积累点数，不是伤害百分比；免疫是显式布尔值。

敌人复用 `EntityDefenseProfile.statusResistances` 与既有 Round 10 profile 选择，不建立第二套实体规则。缺失类型默认 `100 / false / 1`。玩家四个 threshold 来自 `StatusConfig` SERVER 配置，默认 100；没有把 ARC 或防具凭空当抗性来源。

## 7–9. 积累、ARC、衰减

每个武器贡献：

```text
buildup = baseBuildup × (1 + OffensiveScalingCurve.evaluate(ARC) × arcaneScaling)
```

基础物品与质变贡献相加，同类型最终上限 3000。单组件 base 范围 0..1000、ARC coefficient 0..2；最多 8 个组件（基础四种 + 质变四种）。复用原 `OffensiveScalingCurve`，没有复制曲线。

ARC 不改 threshold、Proc 最大生命比例、DOT、时长。积累不乘 AR、需求不足 0.35、物理防御或元素吸收。

默认最后一次积累后等待 60 ticks，然后每秒衰减 5 点。运行时仅遍历当前有状态的实体，每 5 ticks 维护一次；客户端按服务端时间和定义插值，不为动画每 tick 发包。达到 threshold 只触发一次，溢出丢弃；持续型状态激活时不重新积累/刷新。

武器接入点是 `LivingDamageEvent.Post` 且实际正生命伤害。盾牌/吸收完全挡掉、被取消或零伤害命中不积累。这是本轮明确采用的命中政策，不声称与所有原作盾牌机制一致。

## 10–17. Proc、DOT、冻伤与唯一伤害入口

| 类型 | 默认伤害 | 持续时间 | 其他效果 |
| --- | --- | --- | --- |
| BLEED | 最大生命 × 0.15，瞬时一次 | 无 | 清空积累，可再次积累 |
| POISON | 每 40 ticks：最大生命 × 0.0025 + 0.1 | 1200 ticks | 与腐败独立共存 |
| SCARLET_ROT | 每 40 ticks：最大生命 × 0.0075 + 0.2 | 1200 ticks | 比默认中毒更强 |
| FROSTBITE | 最大生命 × 0.11，瞬时一次 | 300 ticks | 受到伤害 ×1.07，精力恢复 ×0.75 |

伤害再乘对应 resistance 的 `procDamageMultiplier`。冻伤先 Burst、后启用减益，不放大自己的首次 Burst。默认值集中在 `StatusEffectDefinition` 与四个数据包 JSON；不是散落的 Event 常量。

Fire Reset：成功造成正生命伤害且 DamageType 有 `minecraft:is_fire` 时，解除 **ACTIVE** 冻伤；不抹掉尚未触发的积累，也不靠武器名称/颜色猜火焰。当前仅有武器 FIRE 通道而没有 fire DamageType 标记的命中，不会被假定为可解冻的燃烧命中。

精力：复用 `EpicFightEncumbranceAdapter` 中现有恢复 Modifier，把负重恢复系数再乘 frost coefficient。没有自行 tick/setStamina。实际 Epic Fight 21.17.3.1 `PlayerPatch` 使用 `STAMINA_REGEN` 计算恢复量和恢复等待；本轮保留其语义。实际玩家观测：轻装 1.15 → 冻伤 0.8624999999999999 → 清除后 1.15。

伤害：`CombatDamageFinalizationService` 先运行既有 `WeaponCombatResolutionService`，再乘目标冻伤 damage-taken，最终一次 `Pre.setNewDamage`。非武器伤害也能受已激活冻伤影响，但不进入 Weapon AR/Defense。`LastWeaponDamageResolution` 仍记录武器层结果，不包含后置 frost multiplier。

四个独立 DamageType：`maplesadventure:bleed / poison / scarlet_rot / frostbite`。加入六种 Vanilla tag：`bypasses_armor / bypasses_shield / bypasses_cooldown / bypasses_resistance / bypasses_enchantments / no_knockback`；没有加入 `bypasses_invulnerability`。Tags 使用 `replace=false`，仅添加本模组四个类型。所有伤害都走 `hurt`，不直接 setHealth；自身 Status Damage 被 WeaponDamagePolicy 与 Post 积累入口排除，防止循环。

## 18. Phase 与来源生命周期

初次积累若有可解析的在线来源，通过统一 `PhaseRelations.canDamage` 校验。武器正常伤害仍首先受现有 Phase 服务端保护。已合法触发的状态不因原攻击者换 Phase/离线而消失。

DOT 只有在来源仍能可靠取得、同维度且关系允许时携带 owner 用于归因；否则退为 source-less Status Damage，避免把新的非法关系当作一次跨 Phase 主动攻击。此时可能失去击杀归因，但不会为了归因保留强 Entity 引用或跨 Phase 伤害权限。

没有修改 Coop/Invasion、Boss Reset、Lost Soul 数据模型。实际跨 Phase 拒绝与来源换 Phase 后既有 DOT 继续通过专服 fixture 验证；多人 Phantom Return 只做了代码接入，未完成双客户端人工全流程。

## 19–22. 武器、质变、投射物、迁移

`WeaponStatusProfile` 是正式积累定义。`WeaponCombatProfileResolver` 返回基础物品 + 当前质变合并结果；`WeaponHitContext` 携带实际 UsedItem 计算的 Snapshot，UI 与直接命中同源。

Blood 默认 BLEED base=30、ARC=.55；Poison 默认 POISON base=30、ARC=.45。旧 `WeaponInfusionBuildup` 保留为 deprecated 兼容桥，不再只是“未来积累”文字。

`ProjectileRequirementPenalty` 沿用既有 Attachment Registry ID `projectile_weapon_requirement`，NBT 升为 version 4，增加有界状态快照。发射时从同一 Resolver 和当前 ARC 计算；飞出后换武器、改质变、升级 ARC 不重算。旧 NBT 3/更早数据缺失状态字段时安全迁移为空 Status Snapshot，保留原 Requirement/Scaling/Damage 信息。

单元测试包含 legacy NBT、保存往返与快照独立性。专服使用真实 Arrow DamageSource 验证发射后改为另一质变/ARC，命中仍使用原快照；不是声称已经人工射遍 Bow、Crossbow、Trident 和每种第三方投射物。

## 数据包示例

武器路径：`data/<namespace>/maplesadventure/weapon_status_buildup/<id>.json`。

```json
{
  "item": "minecraft:iron_sword",
  "priority": 10,
  "statuses": {
    "bleed": {"base_buildup": 30, "arcane_scaling": 0.55}
  }
}
```

`item` 或 `tag` 二选一；Exact 优于 Tag，同类 priority 降序、资源 ID 稳定排序。JSON 严格字段和有限数校验，最多 4096 条规则，reload 后编译 Item lookup；不是每次命中扫描全部 JSON。

现有敌人防御 Profile 可增加：

```json
{
  "channels": {},
  "status_resistances": {
    "bleed": {"threshold": 150, "immune": false, "proc_damage_multiplier": 0.8},
    "poison": {"threshold": 100, "immune": true}
  }
}
```

默认状态定义位于 `data/maplesadventure/maplesadventure/status_effects/{bleed,poison,scarlet_rot,frostbite}.json`。管理员可以用同 ID 数据包覆盖；本轮只接受四个 canonical 类型，不动态注册任意新状态。

## 23–28. HUD 与原图资产

两种 Mode：BUILDUP 用 current/threshold 填充；ACTIVE_DURATION 用 remaining/duration 逐渐缩短。归零短淡出。固定顺序从下到上 BLEED、POISON、SCARLET_ROT、FROSTBITE；仅显示有值/激活/正在淡出的条，不为零状态永久占位。

导入的是用户 `E:\RPG Menu Framework\图标` 的原始 PNG，没有重新生成、重画、缩放保存状态条：

- `assets/maplesadventure/textures/gui/status/buildup_empty.png`：128×32，原 `空状态条.png`。
- `assets/maplesadventure/textures/gui/status/buildup_full_red.png`：128×32，原 `满状态条.png`。

SHA-256 原文件与导入文件一致：

```text
EMPTY 84F04942774F59ED58D34B4BEA93C1BC2BE10D378F519433040EFD1603E583AF
FULL  952E34751EFECFA06D019F48430E27244753677001E86400A39411ACCD5B5682
```

实际填充区域 x=18、y=13、107×6；按宽度裁切 UV，不把红色原图压缩到进度宽度。左图标腔约 x=4..12、y=12..19，图标 nearest 绘制为 8×8。原边框单独保留，整数缩放。

`StatusHudTextureCache` 在加载/资源重载时由 FULL RED 生成四个动态填充纹理；保留 alpha 与明暗层次，替换调色板，不另存四套变色 PNG。缓存复用且重载释放旧纹理；**资源重载生命周期代码已实现，F3+T 人工重载这轮没有成功完成，不能报告为实测通过**。

| 类型 | Palette RGB | Icon ResourceLocation |
| --- | --- | --- |
| BLEED | `#A32622` | `maplesadventure:textures/gui/infusion/blood.png` |
| POISON | `#789B36` | `maplesadventure:textures/gui/infusion/poison.png` |
| SCARLET_ROT | `#B94B32` | `maplesadventure:textures/gui/status/scarlet_rot.png` |
| FROSTBITE | `#69B8D4` | `maplesadventure:textures/gui/status/frost.png` |

后两项使用用户 `猩红腐败.png`、`寒冷.png`；前两项复用已有图标。资源包可覆盖 `assets/maplesadventure/status_visuals.json`。

Proc 为本地化 `!!!出血!!! / !!!中毒!!! / !!!猩红腐败!!! / !!!冻伤!!!`，英文同样提供。队列最多 4 条、短时显示，非聊天刷屏。原条与默认布局避开底部快捷栏和 F 提示；`StatusClientConfig` 提供偏移、间距及整数 scale。第三方自定义 HUD 布局仍可能需要调偏移，不能保证任意组合绝对不重叠。

## 29–34. 同步、预览与命令

协议从 17 升至 **18**；客户端/服务端必须同时更新。扩展现有武器 Registry 与 Loadout 同步的 status profile。StatusNetwork 仅向实体为本地玩家时发送其自身 4 行有界 Snapshot 与 Proc，不广播敌人状态，不接收客户端上报积累值。

登录、重生、维度变化、状态改变/清除、reload 等同步。持续条由时间插值，无每 tick HUD Packet。Client 使用游戏 tick+partial 计算条进度，暂停单人游戏时不会仅因墙钟让条继续下降。

Tooltip 使用 `StatusText` 显示真实积累；武器详情共用 Loadout 的 statuses；ARC 升级预览用同一 profile evaluate(before/after)，不更改真实状态/武器。Character Stats 原来占位的四个 resistance 改为 Snapshot 中真实 threshold。上述预览/Tooltip 数值代码与公式测试通过，**未完成逐页面中英双语人工验收**。

管理员 permission >=2：

```text
/ma status info [self]
/ma status add <bleed|poison|scarlet_rot|frostbite> <amount> [self]
/ma status clear <type> [self]
/ma status clearall [self]
/ma status proc <type> [self]
```

也支持 `/maplesadventure status ...`。不写 self 时选准星前 32 格内、有 LOS 且当前 Phase 可见的 LivingEntity。输出包含 current、threshold、ratio、immune、mode、remaining、source、definition。测试 fixture 的 `statusregression` 命令仅在 `-PweaponRegression=true` 的独立开发 sourceSet 中，生产 JAR 不包含。

## 35–38. 验证记录

### 单元测试

新增 4 个测试类、21 项：`StatusBuildupTest` 6、`StatusRuntimeTest` 8、`StatusProjectileTest` 3、`StatusHudMathTest` 4。

覆盖 ARC 公式/与 Proc 分离、组件合并/边界、阈值/免疫/溢出、衰减、持续状态、NBT 往返与非法数据、旧投射物兼容与冻结、裁切边界/调色板 alpha。全项目 28 suites、124 tests，failure/error=0。

### 真实专服/伤害管线

| 环境 | 结果 | 日志 |
| --- | --- | --- |
| 无 Epic Fight、无 Iron's | 启动 + Status fixture COMPLETE | `.research/round11-server-none.log` |
| Epic Fight 21.17.3.1 | 启动 + Status fixture COMPLETE | `.research/round11-server-epic.log` |
| Epic Fight + Iron's 3.16.3 | 启动 + Status fixture COMPLETE | `.research/round11-server-epic-irons.log` |
| 上述组合最终补测 | Arrow 冻结、Poison 需求独立、DOT 延迟 COMPLETE | `.research/round11-server-final.log` |
| 实际保存/重启/重新加载 | PERSISTENCE COMPLETE | `.research/round11-server-restart.log` |

Fixture 使用 FakePlayer 驱动真实服务器 LivingEntity / Arrow / DamageSource 和正常 `hurt` / NeoForge Event，不是仅纯数学断言，也不等于两个真人客户端。验证：真实 Vanilla/Epic Fight 武器源每次命中只加一次积累；Magic 不误用手持武器；Bleed 越过盔甲/受伤冷却；Frost Burst 顺序、减益、火焰清除；正常死亡；跨 Phase 初次拒绝；Poison+Rot 并存；来源 Phase 改变后 DOT 继续且不递归积累。

Epic Fight 的真实伤害源由现有 `EpicRegression` fixture 构造，没有逐项人工执行每个武器技能。可选模组缺失环境中既有 optional Pseudo Mixin 会产生目标类不存在的 warning，但未出现致命 NoClassDefFoundError/MixinApplyError；不能把日志说成完全没有 ClassNotFound 字样。

Round 10 `defenseregression run` 在联合环境重新执行通过：单次防御、投射物冻结、目标当前防御、跨 Phase 拒绝、法术/火焰/摔落排除，以及 Epic Fight 实际 HP 损失与唯一 resolution 一致。保留用户已有该 fixture 修改。

### 实际客户端观察与截图

本轮使用 computer-use 技能操作隔离 Minecraft 客户端检查实际像素，不用替代 UI mock。原始游戏 F2 截图复制到 `docs/assets/round11/`：

- [25% / 854×480](assets/round11/hud-25.png)
- [75% / 854×480](assets/round11/hud-75.png)
- [50% / 1920×1080 / GUI Scale 3](assets/round11/hud-50-scale3.png)
- [100% / 1920×1080 / GUI Scale 4](assets/round11/hud-full-scale4.png)
- [真实腐败 Proc 与中毒/腐败持续条](assets/round11/hud-active-proc.png)

GUI Scale 1 也实际观察；低分辨率中请求更大 GUI Scale 会被 Minecraft 限制，不能把 854×480 的 scale3请求称为有效 scale3实测。截图填充测试使用仅限测试环境的服务器 Snapshot fixture 固定比例；实际 poison/rot duration 与 Proc 则来自真实状态命令。

四种颜色、左侧图标、多条堆叠、25/50/75/100% 端点可见。真实客户端看到了英文 POISON/SCARLET ROT 提示、持续条下降与消失；中文资源已加入，但未留存中文提示截图。零值不长期显示；未单独保留 0% 条截图。实际玩家先积累再 `/kill`，死亡前清理、重生后新容器为空均确认。

### 未完成与兼容边界

- 双真实客户端 Coop/Invasion/Phantom Death、Boss/FogGate/Lost Soul 全流程人工回归未完成；没有把 fixture Phase 校验当成多人验收。
- Mob/第三方技能主动施加状态应调用 `StatusBuildupService` 并提供可靠 source；自动武器上下文目前复用既有 ServerPlayer 近战/投射物入口，不根据附近实体/武器名字猜来源。
- Fire Reset 认真实 fire DamageType；纯描述性的 FIRE AR 通道不是火焰事件。完全被盾/吸收挡住的攻击不累积。
- 未对所有第三方 AOE/法术/投射物做兼容承诺；没有新增 Mixin 或改第三方 JAR。
- F3+T 手动重载、所有 GUI 尺寸/第三方 HUD 组合、Tooltip/升级面板逐项人工对照尚待完成。
- 原生 Client UI 测试为单个真实客户端连专服；玩家断线长时间状态保留没有独立充分证据，真实持久化证据是实体 Chunk 重启测试。

## 新增/修改文件

新增生产代码：`config/StatusConfig.java`、`StatusClientConfig.java` 与 `progression/status/` 下：

```text
StatusEffectType                 StatusEffectDefinition
StatusDefinitions                StatusResistance
StatusResistanceService          StatusSourceContext
StatusRuntimeState               StatusRuntimeService
StatusBuildupComponent           StatusBuildupSnapshot
StatusBuildupService              StatusDamageSources
StatusEvents                     StatusNetwork
StatusCommands                   CombatDamageFinalizationService
WeaponStatusProfile              WeaponStatusRules
client/ClientStatusState         client/StatusHud
client/StatusHudLayout           client/StatusHudMath
client/StatusHudTextureCache     client/StatusVisualDefinition
client/StatusText
```

修改既有 Java：

```text
MaplesAdventure / MaplesAdventureClient / MessageNetwork
ProgressionAttachments / ProgressionEvents
EpicFightEncumbranceAdapter
EntityDefenseProfile / EntityDefenseRegistry
CharacterStatsService / CharacterStatsSnapshot
CharacterStatsScreen / LevelUpScreen
ProjectileRequirementPenalty
WeaponCombatProfileResolver / WeaponDamagePolicy / WeaponHitContext
WeaponInfusionBuildup / WeaponInfusionDefinition / WeaponInfusionRegistry
WeaponLoadoutSnapshot / WeaponRequirementEvents / WeaponRequirementNetwork
ClientWeaponInfusions / ClientWeaponRequirements / WeaponRequirementText
```

资源：四个 Status 默认定义、四个 DamageType、六个 vanilla damage_type tags、`status_visuals.json`、四个用户 PNG 导入、zh_cn/en_us。测试：四个新 JUnit 类、新 `StatusRegression.java`、现有 `WeaponRegressionMod` 注册。本报告及五张实际截图。

## 39–40. 构建和收尾

生产构建不带 `-PweaponRegression=true`。`gradlew.bat clean build --offline` **BUILD SUCCESSFUL**；只有 deprecated API/旧 Infusion bridge 警告，无编译或测试失败。正式 JAR 不含测试 fixture。

JAR：`build/libs/maplesadventure-0.2.0.jar`，完整路径及 SHA-256 见顶部。没有自动替换用户整合包 JAR，也没有修改第三方 JAR。隔离测试服正常 stop，临时 RCON 已关闭并清空测试密码；隔离客户端已关闭。测试世界/日志/截图保留以便复核。
