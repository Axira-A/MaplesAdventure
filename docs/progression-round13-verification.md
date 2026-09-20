# Round 13 — Status Authenticity / Extended Ailments

验证日期：2026-09-20。Minecraft 1.21.1、NeoForge 21.1.219、Java 21。

## 交付状态与证据边界

代码、单元测试及三种专服组合的启动/服务端回归已完成。真实客户端完成七种 HUD、GUI Scale 1/2/3/4 和睡眠触发提示检查。**尚未完成全部双客户端战斗人工验收，不把本报告视为完整玩法验收通过。**

最终 `gradlew clean build --offline`：BUILD SUCCESSFUL，19 秒。42 个测试类、171 项测试，0 failure / 0 error / 0 skipped。仅有 deprecated API 编译警告。

正式 JAR：`E:\RPG Menu Framework\maplesadv\MaplesAdventure\build\libs\maplesadventure-0.2.0.jar`

- 大小：2,242,793 bytes。
- SHA-256：`21D6AC311B1BB9C00A9BFBAA1A6ED16930B162D3F0582B4DC7DD085983B63B44`。
- 检查 JAR 内容，不含 `weaponregression` 资源或 regression 测试类。
- 协议 19，需要客户端和服务端一起更新。
- 未修改第三方 JAR；未提交或覆盖原有 Round 12 未提交改动。

## 1–7：原实现与防守侧

1. 旧实现把玩家阈值配置、简化武器积累、有限状态枚举作为主要来源，没有统一四项 Resistance/专用 ARC 曲线/独立重复触发历史。固定积累与默认动作倍率容易让不同武器、多段技能表现趋同。旧 HUD 的触发文字又与底部状态条布局耦合。本轮分离数据、动作、抗性与视觉，不重写 Phase。
2. 最终枚举：BLEED、POISON、SCARLET_ROT、FROSTBITE、SLEEP、MADNESS、DEATH_BLIGHT。
3. 映射：IMMUNITY(VIG) → Poison/Rot；ROBUSTNESS(END) → Bleed/Frost；FOCUS(MIND) → Sleep/Madness；VITALITY(ARC) → Death Blight。
4. 玩家阈值 = 160 + Level Contribution + 对应属性贡献 + equipment + effect。后两项本轮为 0，保留 Breakdown。旧 PLAYER_THRESHOLDS 不再作为玩家运行时阈值。
5. 等级贡献：Lv5–71 每级 .25；71–111 每级 .15；111–161 每级 .10；上限 27.5。这是 Maples 平衡公式，不声称是 Elden Ring 原公式。
6. VIG/END/MIND 贡献：5–30 为 0，30–40 每点 3，40–60 每点 .5，60–99 每点 .25。ARC Vitality：5–15 每点 1，15–40 每点 .6，40–60 每点 .5，60–99 每点 .25。
7. Character Stats 与升级预览调用 `PlayerStatusResistanceCalculator`；显示四项真实 Resistance。预览降低仅由 Level 变化导致的四项一起刷屏，保留对应属性重点变化。数学/映射已测试；完整 Stats/升级界面逐项人工截图仍待补。

## 8–17：攻击积累、动作与重复触发

8. `StatusArcaneScalingCurve` 独立于 OffensiveScalingCurve：ARC 5/25/45/60/99 → 0/.10/.75/.90/1，区间线性。
9. Policy 为 NONE、EXPLICIT、FOLLOW_WEAPON_ARCANE；后者读取已解析武器的 ARC 系数，不自动给没有来源的武器加状态。
10. 只有 Bleed、Poison、Sleep、Madness 允许 ARC 积累补正。Frost、Rot、Death 的非法 ARC 定义被拒绝；ARC 不改变伤害、持续时间或控制时间。
11. Weight Class 为 THROWING/NORMAL/GREAT/COLOSSAL。GREATSWORD/GREATAXE/MACE → COLOSSAL；LONGSWORD/TACHI/SCYTHE → GREAT；其余 NORMAL。THROWING 由明确数据指定，不通过名称猜。自定义数据可覆盖重量类别。重量变换的极端自定义 base 最多 1000，单次状态快照最多 3000，避免合法原始数据变换后抛异常。
12. 基础积累表：

| Status | THROWING | NORMAL | GREAT | COLOSSAL |
|---|---:|---:|---:|---:|
| Bleed | 12 | 28 | 34 | 41 |
| Poison / Frost | 14 | 30 | 36 | 43 |
| Sleep | 10 | 22 | 26 | 30 |
| Madness / Rot | 8 | 18 | 22 | 26 |
| Death Blight | 4 | 8 | 10 | 12 |

13. 无衰减、motion=1、初始 Resistance=160、ARC=5 时，NORMAL Blood 每击 28，需要 6 击；COLOSSAL 41，需要 4 击。NORMAL Blood、ARC99、系数 .30 时每击 36.4，需要 5 击。实际间隔衰减、目标 Correction 和技能段数会改变结果；这些是数学标定，不冒充六种武器的真人连续攻击结果。
14. 积累统一为 base × motion × (1 + StatusArcaneCurve × coefficient)。优先级：公开 Integration 值 → exact attack → skill → animation → projectile/archetype → fallback 1。数据包 motion 有限且 0–4，选择器必须是有界字符串；不从伤害数字、动画速度或事件时间猜动作。
15. Epic Fight 21.17.3.1 的 `AttackAnimation.getEpicFightDamageSource` 返回处关联实际 DamageSource、Animation ID、Phase index；支持显式 `STATUS_MOTION_VALUE` AttackPhaseProperty。最小 optional Mixin，不修改 EF JAR。生产数据为已审计的 rushing_tempo、blade_rush、eviscerate、relentless 动作提供倍率。未登记的第三方动作仍 fallback 1，需后续明确 Adapter/数据包，不能宣称所有 Mod 多段攻击已平衡。
16. Correction 是替换当前 offset，而非累加各阶段：STANDARD=21/49/91/189/479；RESISTANT=28/70/168/458/915；NONE=0；boss profile 别名使用 RESISTANT。玩家默认 NONE。Mob 通过 defense profile 明确选择，不对所有实体强行启用。
17. 独立 `procCounts` 按 Status 保存，最多 5；状态条归零/单项 CURE 不删除历史。不再 30 秒自动归零。死亡、重建 Encounter 实体、ADMIN reset 清理；单项 ADMIN reset 仅清该项。真实专服 Chunk 保存/重启已验证状态 Attachment、Phase 和 v2 procCounts 恢复。

## 18–28：七种效果与伤害管线

18. Bleed 默认 15% Max HP，flat=0。目标 `proc_damage_multiplier=.7` 时为 10.5%。Boss 倍率由显式 defense profile 定义，不用模组名猜 Boss。
19. Poison 默认 90 秒，每 20 ticks 造成 .45% Max HP，flat=0，可以致死。不用 Vanilla Poison，不绕过 hurt 用 setHealth 扣血。
20. Rot 默认同为 90 秒，每秒 .90% Max HP。运行时保证持续结束时最后一个计划脉冲不会被先过期逻辑吞掉；离线不集中补扣。
21. Frost 默认 10% Max HP，显式 .7 proc multiplier 时为 7%；持续 30 秒，直接受伤 ×1.20，精力恢复 ×.80。只有 active Frost 被正向火焰命中清除，未触发的积累不清。
22. Sleep 玩家 30 ticks 控制锁，扣 30 + 10% 实际最大魔力，无 HP 伤害。无 Iron's 时不创建虚构魔力池。
23. Mob 默认短暂 stagger；明确 deep_sleep profile 才进入最长 1200 ticks 睡眠。正向外部伤害唤醒；不永久设置 NoAI。睡眠状态条移除不再提前取消控制锁。
24. Madness 默认仅 Player / 显式 TarnishedLike；普通 Mob 免疫。15% Max HP、30 + 10% Max Mana、40 ticks 控制。真实 Iron's MagicData 精确扣量与归零钳制已验证。
25. Death Blight 默认 Player / 显式允许的类褪色者目标；普通 Mob 免疫，managed PRIMARY 即使 TarnishedLike 也需要 allow_death_blight。使用致命 hurt 走原死亡流程，不做永久减血上限。真实 Mob 死亡管线已验证；真实红灵/黄灵的 Death Blight 遣返尚待人工重测。
26. 自定义 Status DamageType 通过 tags 排除护甲、抗性、附魔、防盾、无敌帧及击退的重复干预。Death Blight 单独 bypasses_invulnerability。沿用 Phase 安全检查和一次实际 hurt，不创建新 Weapon DamageChannel。
27. Poison/Rot 使用 no_impact/no_knockback；另外 `StatusQuietDamageMixin` 只跳过这两个自定义 DOT 的 `LivingEntity.handleDamageEvent` 客户端受击视觉，因为 tags 本身不能阻止 hurtTime/walkAnimation 写入。实际伤害仍正常。源码与自动伤害检查通过，连续 DOT 的相机动态人工验收待补。
28. Frost 的直接伤害放大在现有最终结算位置排除所有 Status DamageSource；专服确认 Bleed 在 Frost 下不被重复放大，Poison/Rot 同走排除逻辑。

控制锁服务独立于动画：服务器拦截直接攻击/使用物品/交互和移动包，客户端提供输入提前反馈；EF 技能执行复用既有检查，Mob AI 用最小 Mixin 暂停。已经射出的 Projectile 不因为 owner 短暂被控而凭空失效。

## 29–38：质变与迁移

29. Cold：base .95，物理 .70 + Magic .30；INT .65、STR/DEX 保留倍率 .85；Frost 积累不吃 ARC/INT。
30. Occult：base .96，STR/DEX ×.25，ARC 至少 .75；无凭空添加的状态，只把允许 ARC 的原生积累转为 FOLLOW_WEAPON_ARCANE。
31. Slumber：base .95，物理 .75 + Magic .25；DEX 至少 .45，元素 INT .55 / ARC .20；Sleep 积累系数 .20。
32. Frenzied：base .95，物理 .65 + Fire .35；FTH .65 / ARC .25；Madness 积累系数 .25。
33. Rot：base .90，物理；Rot 无 ARC 积累补正。
34. Blight：base .90，物理 .70 + Holy .30；FTH .55 / ARC .25；Death 积累无 ARC。Frenzied、Rot、Blight 仅显式 eligibility 开放，不默认普及。
35. `future_buildup` 保留 legacy 读取桥；新 builtins 全部使用 canonical statuses。Blood/Poison 状态系数为 .30，原 AR ARC .55/.45 不因本轮改变。
36. Projectile Attachment Registry ID 不变，NBT 写 v5；旧版本安全读取，v4 状态快照兼容。发射时冻结积累/来源/动作结果，之后升级、换装、reload 不重算旧弹。NBT 单测通过，本轮未做实体 Projectile 的完整保存重启人工测试。
37. StatusRuntime 写 v2；读 v1 时从已有 procSerial 迁移历史；无效条目有界丢弃并记录警告。控制状态有上限，不 copyOnDeath；普通换维度/非死亡 clone 保留。
38. 协议为 19。最多七项状态 HUD 数据、受控 payload 大小、所有数值边界验证。只有自身完整状态，变化时同步，不逐 tick 广播完整状态。

## 39–40：HUD

39. Proc Banner 独立放在屏幕约 35% 高度，可配置 X/Y；不随状态条数量变化。状态条仍在下方；小 GUI 视口的七条自动分列，避免竖向溢出，保持整数像素资源。真实客户端 GUI Scale 1/2/3/4 已逐一查看；真实 Sleep 触发文字已查看。七条颜色/图标测试使用 owner-only HUD fixture，并不等于七种真实武器同时触发。
40. 颜色：Bleed #A32622、Poison #789B36、Rot #B94B32、Frost #69B8D4、Sleep #AD8BCF、Madness #E9AA24、Death #8E9490。新图标复用用户 `E:\RPG Menu Framework\图标` 资产；无生图、无替代图。新增六个 infusion 图标与三个状态图标。

## 41–48：实测结果

| 项目 | 结果与边界 |
|---|---|
| 41 Phase | 既有统一 Phase/伤害入口保留，服务端 fixture 覆盖相关拒绝与回归。未重新做完整双真人 Phase 战斗。 |
| 42 PvP | PlayerDefenseRegression 使用 synthetic ServerPlayer 验证，不是两个真人客户端 Duel。真实 Duel/Coop/Invasion 三种死亡与返回待补。 |
| 43 Player Defense | `playerdefenseregression`、`defenseregression run`、`statusregression run` 实际专服 PASS。 |
| 44 EF 多段 | 真正 AttackAnimation DamageSource factory 的 optional hook 验证 PASS；RUSHING_TEMPO1=.5、BLADE_RUSH_COMBO1=.5、EVISCERATE_FIRST=.6；RELENTLESS_COMBO 实际有 8 Phases，当前 .4×8=3.2，不能冒称四段总1.6。四段×.4=1.6 是单元数学示例。完整技能实际命中次数/节奏仍需真人标定。 |
| 45 Unit | 42 suites / 171 tests / 0 failures / 0 errors / 0 skipped。 |
| 46 Dedicated | 无EF无Iron's、EF only、EF+Iron's 三组合均启动并完成服务端状态回归。最后又重跑无模组分支及 Iron's 精确魔力断言。 |
| 47 Build | 最终 `gradlew clean build --offline` BUILD SUCCESSFUL，19s。 |
| 48 JAR | 本报告顶部给出绝对路径、大小、SHA-256；无测试 fixture 混入。 |

本次过程中修复过一次真实启动错误：NeoForge 不允许把 abstract PlayerInteractEvent 当直接订阅事件，已改为具体的 RightClick/EntityInteract/LeftClick 子事件。之后三环境启动正常，无本集成引起的 ClassNotFound / NoSuchMethodError / optional Mixin 崩溃。EF 对测试 wooden_sword 数据有既有反序列化警告；从装过模组的隔离测试世界移除模组启动时有 unknown attribute 警告，服务器仍成功启动，这不是“零警告”验收。

## 本轮新增 Mixin

| Mixin | 最小职责 |
|---|---|
| EpicFightStatusMotionMixin | optional @Pseudo / require=0，源 DamageSource 仍携带精确 Phase/Animation 上下文时关联 motion。 |
| StatusPlayerMovementMixin | 在服务端移动包主线程处理处拒绝控制锁期间位移，不信任客户端输入锁。 |
| StatusMobControlMixin | 暂停被控 Mob 的 serverAiStep，不修改持久 NoAI。 |
| StatusQuietDamageMixin | 仅自定义 Poison/Rot 的 hurt visual 路径，补足 vanilla tags 未覆盖的相机冲击。 |

## 数据包入口示例

沿用既有 weapon status 与 entity defense 框架；新增目录 `maplesadventure/status_motion_values`、`maplesadventure/status_resistance_corrections`。

动作规则示例（namespace 与路径使用实际 Animation ID）：

```json
{"animation":"epicfight:relentless_combo","motion":0.4,"priority":0}
```

Correction 资源示例：

```json
{"stages":[21,49,91,189,479]}
```

敌人 defense profile 的相关字段示例（其余选择器沿用当前防御数据格式）：

```json
{
  "status_traits":{"tarnished_like":false,"allow_death_blight":false},
  "status_resistances":{
    "bleed":{"threshold":220,"proc_damage_multiplier":0.7,"correction":"maplesadventure:standard"},
    "frostbite":{"threshold":260,"proc_damage_multiplier":0.7,"correction":"maplesadventure:standard"},
    "sleep":{"threshold":180,"response":"stagger_only","correction":"maplesadventure:resistant"}
  }
}
```

## 文件范围

- 核心新增：`progression/status/` 下 ResistanceType/曲线/Calculator/Snapshot、ArcaneScalingCurve/Policy、WeightClass、CorrectionProfile/Service/Reload、MotionResolver/Rules、ControlLockService、GuardPolicy、SleepResponse、TargetTraits。
- 核心修改：StatusEffectType/Definition/Definitions、BuildupComponent/Snapshot、WeaponStatusProfile/Rules、StatusResistance/Service、RuntimeState/Service、Events/Network/Commands、CombatDamageFinalizationService。
- 集成：`PlayerManaService`、DerivedStat adapter 接口/Registry、IronsManaAdapter、EpicFightStatusMotionAdapter；复用 EF 现有技能/负重入口做控制拒绝。
- 武器：WeaponCombatProfileResolver、WeaponDamagePolicy、WeaponLoadoutSnapshot、ProjectileRequirementPenalty、InfusionDefinition/Registry/Eligibility/EligibilityService、RequirementEvents/Commands 与客户端规则读取。
- UI：CharacterStat/Calculator、ResistanceCalculator、StatPreviewPriority、CharacterStatsScreen；StatusHud/Layout/VisualDefinition/ClientStatusState；StatusConfig/ClientConfig；zh_cn/en_us。
- 资源：六种 infusion JSON/图标、三种新 status 定义/图标、Motion 数据、Madness/Death damage types、DamageType tags、mixin 配置。
- 新增十类要求的单测：StatusResistanceCurveTest、StatusArcaneScalingCurveTest、StatusResistanceCorrectionTest、StatusMotionValueTest、ExtendedStatusRuntimeTest、StatusControlLockTest、StatusMigrationTest、StatusHudLayoutTest、StatusInfusionTest、StatusDamageSourceTest；更新旧 Status/Projectile/Infusion/PlayerDefense 单测。
- 测试 fixture：StatusRegression、EpicRegression、PlayerDefenseRegression、StatusManaRegression、status13 defense profile。只在显式 `-PweaponRegression=true` 运行，不进入正式包。
- 保留已有 Round 12 的 Coop/Invasion SESSION_RETURN 清理，不重构联机生命周期。

## 运行证据

仓库 `.research/` 本地日志（测试输出不打进正式包）：

- `round13-vanilla-initial.log`、`round13-vanilla-extended.log`：最早无可选模组回归。
- `round13-vanilla-final.log`：最新无模组启动、无虚构魔力、扩展状态及最后 DOT 脉冲 PASS。
- `round13-epic.log`：EF-only、状态持久化保存/重启读取。
- `round13-epic-irons.log`：EF+Iron's、实际 EF source factory、客户端 HUD 联机阶段。
- `round13-epic-irons-mana.log`：实际 MagicData 精确睡眠/发狂扣量、原 Status/Defense/PlayerDefense 回归。
- `round13-client-hud.log`：真实客户端启动和 HUD 阶段。

所有测试在既有隔离目录 `run/round11-status-server` / `run/round11-status-client` 进行；本轮结束测试服务器已正常 stop，客户端已关闭。没有部署或替换用户正式整合包。

## 尚需人工验收，不列为已通过

- Blood/Poison/Cold/Slumber/Frenzied/Blight 六种武器的真实连续攻击命中数、衰减和完整触发循环。
- 两个真实客户端的合法 Duel、HOST+COOPERATOR、INVADER Phantom Death、Lost Soul 与返回后控制解除。
- 控制期间真实移动/冲刺/闪避/蓄力及异常移动包；静默 DOT 的相机动态；所有真实死亡界面路径。
- EF 完整动画的命中节奏与多段总积累平衡，尤其实际八 Phase 的 RELENTLESS_COMBO。
- 全套 Character Stats/升级 Draft/武器 Tooltip 人工视觉回归。
- 无 EF/无 Iron's 的独立客户端启动（专服已验证）；真实 Projectile v5 Chunk 保存/重启（NBT 单测已验证）。

已完成的自动验证不能替代这些真人测试。当前构建可用于继续验收，但不宣称 Round 13 所有验收项已结束。
