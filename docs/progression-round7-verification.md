# RPG Round 7 — Weapon Scaling / Attack Rating 验证报告

实现与主要专服验证：2026-09-06；无可选模组客户端/专服及最终构建复核：2026-09-13。

状态：核心实现完成。下述 PASS 区分纯数学、专服测试夹具和真实客户端 UI；没有把尚未完成的手动连招、双客户端会话回归列为通过。

## 1. WeaponScalingProfile

独立不可变 record，保存 STR/DEX/INT/FTH/ARC 五项 double coefficient、maxBonus、source、weaponClass、debugReason；不保存玩家属性。每项系数有限且在 0..1.5；maxBonus 在 0..2，默认 1.15。source/class/reason 分别限制 32/64/512 字符。NONE 与 DISABLED 不参与补正。

## 2. ScalingGrade

NONE <0.10；E [0.10,0.25)；D [0.25,0.40)；C [0.40,0.55)；B [0.55,0.70)；A [0.70,0.85)；S >=0.85。字母仅显示，计算仍使用精确系数：例如 0.05 显示 NONE，但仍有数值贡献。边界与这一差异有单元测试。

## 3. 自动 Archetype

复用实际 WeaponClassifier；材料不改变系数。所有自动项 INT/FTH/ARC=0，不根据名称、颜色、School Power 或 modid 猜物理补正。

| Archetype | STR | DEX |
|---|---:|---:|
| TOOL | .15 | .05 |
| DAGGER | .10 | .65 |
| SWORD / GENERIC | .35 | .35 |
| LONGSWORD | .50 | .30 |
| GREATSWORD | .75 | .15 |
| AXE | .65 | .10 |
| GREATAXE | .80 | .10 |
| SPEAR | .25 | .55 |
| SCYTHE | .25 | .60 |
| TRIDENT | .30 | .55 |
| UCHIGATANA | .15 | .70 |
| TACHI | .25 | .65 |
| FIST | .15 | .60 |
| BOW | .10 | .70 |
| CROSSBOW | .25 | .45 |
| MACE | .80 | 0 |

## 4. AR 公式

唯一共享数学实现 WeaponAttackRatingCalculator：

```text
factor = Σ OffensiveScalingCurve.evaluate(attribute) × coefficient
effectiveScaling = clamp(factor, 0, maxBonus)
bonusAttack = baseAttack × effectiveScaling
AR = baseAttack + bonusAttack
```

沿用原成长曲线，5/20/40/60/99 对应约 0/.3015/.6030/.8040/1。SWORD 同时 STR/DEX 99 = +70%，不是因字母 D 计算某个固定值。

UI 低频读取当前 ItemStack 的主手 ATTACK_DAMAGE modifier（含三种操作）；不是永远使用默认物品。标准基础 1；没有伪造弓箭伤害，弓/弩物品 AR 基线 1 只是其标准物品攻击基线，蓄力、箭速及真正投射伤害仍由原版负责。AR 不是目标最终扣血预测。

## 5. 安全边界

拒绝 NaN、Infinity、负数、越界系数；baseAttack 限制 0..10000；补正倍率为 1..3。叠加需求惩罚后的总倍率可以低于 1（如 0.35），不能为了倍率下限抹掉惩罚。自动 maxBonus=1.15，datapack 最大 2.0；五属性高系数总和被统一裁剪。单元测试覆盖。

## 6. JSON 与缓存

路径：`data/<namespace>/maplesadventure/weapon_scaling/*.json`。

```json
{"item":"minecraft:diamond_sword","scaling":{"strength":0.75,"dexterity":0.15},"max_bonus":1.15}
```

也支持 `tag`、整数 `priority`、`disabled:true`。item/tag 必须恰好一个；非法字段/数值按文件拒绝并记录，不导致整个服务器加载失败。最多 4096 条合法规则。

优先级 Exact > Tag > 显式注册 Integration > Archetype / Generic > NONE；同一层按 priority 降序和资源 ID 稳定排序。Item 到 Profile 为不可变编译缓存，攻击时不扫描 datapack、材质、属性集合或 EF capability。

专服 PASS：Exact 胜过更高 priority 的 Tag、Tag 正确覆盖、disabled 归零、cap 生效、1e309 拒绝；停用测试包恢复自动 Profile。

## 7. 与 Requirement 共存 / 同步

需求和补正各有独立 Profile/规则/缓存，但复用 WeaponRequirementService 编译生命周期与原有 weapon_requirements 分批包，不建立第二套网络框架。禁用需求不会阻止识别和补正。

同一 revision 同步 Item ID、需求、补正、基础攻击和 weapon 标志；客户端收完再原子替换。每批 32 项、最大 128 KiB；最多 1024 批/32768 物品。新增 WeaponAttackSnapshot 统一 AR、系数、需求与效率；WeaponLoadoutSnapshot 扩展手持数据。协议版本 15，服务端和客户端须一起更新。

## 8. 唯一 Damage Pipeline

保留 WeaponRequirementEvents.damage 的 LOWEST LivingDamageEvent.Pre；只在这一处应用合成倍率：`incoming weapon damage × (1+effectiveScaling) × requirementMultiplier`。没有第二个 Scaling Damage Event，也没有本轮新生产 Mixin。

已经合并进本次标准武器伤害的附魔/动作数值一同受倍率影响；独立 MAGIC/INDIRECT_MAGIC、火、摔落、爆炸先排除。无武器来源或未携带武器发射快照的其他伤害不推断主手武器。特殊模组若伪装为同一种武器 DamageSource，仍需有明确来源的后续适配，未宣称能识别任意第三方自定义 DoT。

## 9. Vanilla / Epic Fight UsedItem

Vanilla PLAYER_ATTACK 取真实主手；EF 复用 getUsedItem，显式 EMPTY 不回退主手。主/副手各计算自己的 Profile。

专服夹具以真实 LivingEntity.hurt 经过整个伤害事件链：STR40 DEX20，Diamond Greatsword 原始 100 → 149.74878（预期 149.74874）；Trident →134.67334（预期 134.67337）。magic/fire/fall 均保持100。EF getUsedItem 主手 Greatsword=1.497487437、副手 Iron Dagger=1.256281407、EMPTY=1，全部匹配。

注意：EF UsedItem 是实际 EF DamageSource 的策略回归，未把它冒充真实玩家完整双持连招命中；100 点夹具也不等于物理鼠标 Player.attack 动作。

## 10. Projectile Snapshot / 兼容

继续原 Attachment ID `maplesadventure:projectile_weapon_requirement` 和 ProjectileRequirementPenalty 类；NBT version=2 新增 scalingMultiplier，保留 owner/qualified/multiplier。旧 multiplier 仍表示需求倍率，旧数据缺少 scalingMultiplier 时默认1。非法加载值回安全默认。

只在可靠 shooter+weapon 发射上下文捕获；已保存实体加载不重算，换手、升级、重新登录均不改变旧箭。普通 Projectile 没有已知武器来源时不猜。

## 11. Bow / Crossbow / Trident 验证

Round6 遗留先验证：真实测试玩家执行原版 releaseUsing（30 tick 充能参数），弩 performShooting；不是直接 new Projectile 模拟快照。属性不足产生 .35 快照，切装备/升级后保持；跨已加载区域、保存重启后原 UUID 和 .35 保留。三叉戟 melee 与 throwing 都使用同一 Profile。

Round7 STR40 DEX20 发射，之后修改 DEX/主手，再保存和真正重启专服；2026-09-06 重连后结果：

| 武器 | 原 UUID | scalingMultiplier / 重启后实际 Policy |
|---|---|---:|
| Bow | 06e3deac-90e0-4350-856d-9311cfec8f79 | 1.271356783919598 |
| Crossbow | fcb9095b-6d37-4b0c-8d4d-161dc1608b5b | 1.2864321608040201 |
| Trident | 2bf085c6-ecc5-43a3-91cd-648f89cc47ae | 1.3467336683417086 |

同一个箭实体作为 independentMagic 的 direct entity，Policy=1，不套快照倍率。新/旧 NBT 还有纯序列化单元测试。尚未补齐物理鼠标长按松弓、所有附魔/多重射击逐颗命中人工组合。

## 12. 属性不足

名义 AR 正常计算，最终需求效率另乘35%。Greatsword STR40 DEX5 原始100 →50.82916；magic/fire/fall仍100。无 EF 环境 Diamond Sword STR40 DEX5 原始100→42.38696，与 `(1+.35×curve(40))×.35×100` 相同。

达到需求立即用当前状态100%效率，不缓存玩家资格；空手没有自动武器补正。无 EF 环境 STR40空手原始100→100，实测 PASS。

## 13. Tooltip

复用 ItemTooltipEvent：物理 AR `base + bonus = total`、本地化五属性字母、当前补正百分比、AR不是最终伤害提示，然后独立需求/35%效率；Shift 查看各属性贡献（cap前贡献明确区分）。中英文语言键齐全。

2026-09-13 真实无 EF/Iron's 客户端 PASS：Diamond Sword STR/DEX40 显示 `7.0 + 3.0 = 10.0`、D/D、+42.2%；保持鼠标悬停，/reload exact override 后变为 `7.0 + 3.8 = 10.8`、A/E、+54.3%，无需重登。禁用测试包后回 D/D；DEX5 时 AR `7.0 + 1.5 = 8.5`，另列红色未满足 DEX7、35%效率。未声称中文和所有 GUI scale 都经过实机目测。

## 14. Character Stats

MAIN/OFF_HAND_ATTACK 从 UNAVAILABLE 改为有武器时 ACTIVE，StatBreakdown 保留 base 与 bonus。非武器显示不可用，不伪造拳头 AR。武器区显示名称、基础/补正/总值、等级、需求与效率；长文字现在逐行 wrap 成滚动列表项，不只保留第一行。

实际 EF 客户端已看到 `15.0 + 6.9 = 21.9`、STR A / DEX E、+46.0%，不足需求仍独立显示。最后的多行 wrap 修正已编译，未复测全部语言/GUI scale。

## 15. Level-Up Preview

对最终 Draft PlayerAttributeState 完整调用共享公式，不累加估算各属性增量。Compact Preview 同时显示需求变化、名义 AR 变化、效率变化；Preview 不写真实玩家数据或 Skill。

实机 STR40→41 Draft：21.8→21.9；期间服务端 STR仍40、实际夹具伤害仍50.829，未提前生效。多属性草稿、需求惩罚独立于名义AR、ACTIVE/UNAVAILABLE由单元测试覆盖；未把完整确认升级+双持战斗组合列为人工通过。

## 16. 50 件实际武器校准

见 [progression-round7-calibration.md](progression-round7-calibration.md)。使用 Round6 同50件实际注册物品，包含实际类别、基础攻击、五需求、STR/DEX系数/字母及5/20/40/60/99样本AR。没有创建50个Exact平衡补丁。自动样本初始无bonus、没有普通Starter自动S。

## 17. Round6 Innate / Arts 审计

实际版本：Epic Fight21.17.3.1、Nightfall(efn)3.4.0、Invincible21.15.8.2、Iron's3.16.3。

标准 SkillContainer.requestCasting：属性不足时 epicfight:steel_whirlwind rejected，resource60→60、stamina26→26、stack/duration/cooldown不变；Nightfall efn:ruinsgreatsword 同样 rejected、不消费（其该容器测试 resource0）。不重复拦其他slot，不改第三方JAR。

专属 Arts：Nightfall JudgmentCutEndSkill/StompSkill 是 PassiveSkill，经 handleKeyInput→CPSkillRequest→自定义 executeOnServer；Invincible ComboBasicAttack 的 executeNodeOnServer/executeOnServer 包含普通 combo 节点。未找到可无差别封锁且不会误伤普通连招的通用语义入口，因此只记录，未按动画ID或modid加伤害Hack。这部分仍是明确兼容缺口。

发现外部 Nightfall 专服异常：流浪商人饮药触发 AddTriggerEvent.onEffectAdded→EffekUnits.VFXENABLE，在配置未加载时抛 IllegalStateException。保留 `run/round6-weapons-server/crash-reports/crash-2026-09-06_13.02.36-server.txt` / `15.10.26`；随后使用独立 round7-world 禁用该测试世界流浪商人刷新继续回归，没有改其JAR，也没有宣称该整合组合整体稳定。

## 18. 无 Epic Fight / 无 Iron's、Phase 回归边界

2026-09-13：两个可选模组都未安装的专服及真实客户端正常启动并连接，34项武器配置编译成功，无可选类链接错误。Diamond Sword STR/DEX40 原始100→142.21106；magic/fire/fall=100，裸手=100；Tooltip与在线reload实测通过。使用单客户端，不冒称双客户端Coop/Invasion完整回归。

Scaling直接读取玩家自己的AttributeState，没有添加Phase属性副本/角色转换modifier，因此不修改Phase/Coop/Invasion生命周期；专服两/三客户端的切Phase、红灵/黄灵实战仍待人工回归。

## 19. 构建、变更与测试工具

2026-09-06 与最终 2026-09-13 `gradlew clean build` 均成功。最终 BUILD SUCCESSFUL in 15s，72 tests / 0 failures / 0 errors / 0 skipped；两个既有 deprecated 警告（EncounterConfig / EncounterSpawnService），没有新增编译错误。

新增生产文件（`src/main/java/dev/maplesadventure/progression/weapon/`）：ScalingGrade、WeaponScalingProfile、WeaponScalingRules、WeaponScalingService、WeaponAttackRatingCalculator、WeaponAttackSnapshot、client/WeaponAttackText。

修改：同目录 WeaponRequirementService、WeaponClassifier、WeaponRequirementEvents、WeaponDamagePolicy、ProjectileRequirementPenalty、WeaponLoadoutSnapshot、WeaponRequirementNetwork、WeaponRequirementCommands、client/ClientWeaponRequirements、client/WeaponRequirementText；progression/stats/CharacterStatsSnapshot；progression/client/CharacterStatsScreen、LevelUpScreen；network/MessageNetwork；en_us/zh_cn语言JSON。

测试：WeaponScalingTest新增8项；build.gradle增加仅显式 `-PweaponRegression=true` 启用的独立sourceSet；src/weaponRegression 下 WeaponRegressionMod / EpicRegression / mods.toml。该夹具提供launch/probes/damage/useditem/innate管理员测试，**不进入正式JAR**。隔离世界的测试datapack已停用，专服和客户端已关闭；无生产服务器部署。

## 20. 交付与剩余验收

JAR：`E:\RPG Menu Framework\MaplesAdventure\build\libs\maplesadventure-0.2.0.jar`。

最终产物：1,919,759 bytes；SHA-256 `ACB7DB21B29F51CECF1FA4C930A4E15F12AE268077EA4CD2C26B878DFE069151`。已打开 JAR 检查 Scaling/Profile/AR/Tooltip 类与中英 JSON；新增补正语言键双语齐全；没有 dev regression 夹具或测试 datapack 打入产物。

构建产物与 dev runtime 同源；上述人工/夹具测试通过 Gradle dev runtime 运行，未声称全部都是最终成品JAR重新部署后的验证。服务端和客户端须一起更新（协议15）。

剩余验收：物理长按Bow完整命中、实际EF双持/完整连招、特殊Arts、全部附魔/自定义独立DoT、Coop/Invasion双客户端、全部中文/GUI比例。核心数值、单次事件倍率、数据包覆盖、冻结Projectile重启、真实UI主要显示及无可选模组启动均有上述证据；不能把全部TEST1–24标记人工验收完成。
