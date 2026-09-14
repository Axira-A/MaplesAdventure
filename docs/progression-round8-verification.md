# RPG Round 8 — Weapon Damage Profile Core

2026-09-13；Minecraft 1.21.1 / NeoForge 21.1.219 / Java 21。先审计 Round7 源码、实际 EF 21.17.3.1 JAR，再修改。下文区分单元测试、专服夹具和真实客户端 UI，不把夹具视作完整鼠标连招/实射验收。

## 1. WeaponDamageChannel

九种稳定语义：PHYSICAL、SLASH、STRIKE、PIERCE、MAGIC、FIRE、LIGHTNING、ICE、HOLY。PHYSICAL 只表示标准物理，不是前三种物理子类型的总和。NBT/JSON 使用名称；协议16用有界枚举编号；全部中英本地化。

当前元素仅描述这一次武器命中的组成，不额外点燃、不施加冰冻、不创建第二种 Minecraft DamageSource，也没有敌人抗性/弱点、质变、强化或异常积累。

## 2. Component / Profile

WeaponDamageComponent：channel、baseRatio、ScalingMode、可选 WeaponScalingProfile。

WeaponDamageProfile：不可变组件列表、source、weaponClass、debugReason。组件1..8，同一channel不得重复；每项ratio有限且0..2，总ratio在(0,2]。metadata长度32/64/512。零ratio组件合法，但整把武器不能全零。

## 3. 独立补正

INHERIT_WEAPON 取统一Resolver返回的武器ScalingProfile；OVERRIDE复用同一种Profile和原曲线，每通道独立STR/DEX/INT/FTH/ARC及maxBonus。

没有 `scaling` → 继承；显式 `scaling:{}` → 覆盖为零系数，不继承。max_bonus只允许跟随显式scaling，避免“继承又局部覆盖cap”的隐含第三种模式。

自动元素识别没有增加：Iron's School Power、武器名、材质、颜色、namespace均不会凭空生成元素。

## 4. 默认物理类型 / 实际抽样

| 类别 | 通道 |
|---|---|
| TOOL / SWORD / GENERIC | PHYSICAL |
| LONGSWORD / GREATSWORD / AXE / GREATAXE / UCHIGATANA / TACHI / SCYTHE | SLASH |
| DAGGER / SPEAR / TRIDENT / BOW / CROSSBOW | PIERCE |
| FIST / MACE | STRIKE |

每个默认项ratio1，INHERIT_WEAPON。实际 `/ma weapon damage <item>` 检查：minecraft:diamond_sword→PHYSICAL、epicfight:diamond_greatsword→SLASH、iron_dagger/iron_spear→PIERCE、uchigatana/iron_tachi→SLASH、minecraft:mace→STRIKE、bow/crossbow/trident→PIERCE。复用真实Category，没有按某一帧动画猜横斩/突刺。

## 5. 多通道 AR / Bundle

所有计算集中于现有 WeaponAttackRatingCalculator 新重载，内部继续复用 Round7 单通道计算和 OffensiveScalingCurve：

```text
componentBase = weaponBase × ratio
effectiveScaling = clamp(Σ curve(stat) × componentCoefficient, 0, componentMaxBonus)
componentBonus = componentBase × effectiveScaling
componentAR = componentBase + componentBonus
totalBase / totalBonus / totalAR = 对应组件求和
nominalMultiplier = Σ ratio × (1 + effectiveScaling)
effectiveMultiplier = nominalMultiplier × requirementMultiplier
```

base>0时nominal严格等价totalAR/base；base=0时使用相同权重式，避免0/0。ratio总上限2、补正倍率上限3，因此Round8名义倍率安全上限6，而不是强行套Round7的3并错误裁剪合法split武器。Bundle保留每通道base/bonus/AR，提供fraction(channel)、各total及两种倍率；不可变Map、有限数值及总量一致性验证。需求35%不进入任何名义AR。

伤害热路径使用已编译WeaponFacts基础值，不重新扫描装备modifier；基础值不影响通道比例或倍率。Tooltip/Loadout低频读取当前ItemStack实际基础modifier；默认武器数值一致。

## 6. Round7 等价测试

WeaponDamageProfileTest遍历所有WeaponRequirementArchetype、属性5..99、基础攻击 .01/1/7/15/500/10000，比较Round7倍率与Round8 totalAR/base、总AR、最终35%倍率，误差要求<1e-9，全部通过。混合属性、独立元素贡献、显式零补正、零基础值、最大值另有测试。

没有damage profile覆盖时，只有通道标签变化，默认伤害不变。

## 7. WeaponCombatProfileResolver

resolve(ItemStack) 一次组合 RequirementProfile、ScalingProfile、DamageProfile、weapon标记。Direct context、Projectile launch、Loadout与Registry sync走此入口；客户端用服务器下发的解析结果，不运行自己的profile规则。

未来质变只能在该Resolver加入明确transform，不需要复制伤害、UI或投射算法。本轮只组合静态Registry，没有任何质变实现。显式DamageProfile可声明通用物品为武器，空手始终不被推断成元素武器。

## 8. WeaponHitContext

包含usedWeapon资源ID、不可变Bundle、RequirementResult、projectileSnapshot标记、ownerUUID。context(DamageSource)返回Optional；不属于武器的来源返回空，旧multiplier入口委托context，不再重复计算伤害。

旧scalingMultiplier API仅作为deprecated兼容别名，返回Bundle名义倍率；生产消费者不再只读取Round7单通道公式。未来抗性应读取Bundle比例，在同一次事件内合并，不调用多个hurt。

## 9. 单一 DamageEvent

保留 WeaponRequirementEvents 的 LOWEST LivingDamageEvent.Pre，读取context.effectiveMultiplier后只setNewDamage一次。本轮没有新生产Mixin或第二个Damage监听。

EF getUsedItem继续区分主手/副手/显式EMPTY；Vanilla PLAYER_ATTACK用真实主手；Projectile用已冻结快照。独立MAGIC/INDIRECT_MAGIC、火、摔落、爆炸先排除，未声明武器来源的普通Projectile不会猜主手。

专服夹具的真实 EF DamageSource + SWORD_AUTO1 accessor + UsedItem 经LivingEntity.hurt完整伤害管线，观察到每次Pre恰好1次：Greatsword属性不足100→44.497498；副手Dagger100→122.61304；EMPTY100→100。Split Slash/Fire主手100→142.03516且events=1。不是完整客户端操作的双持连招，未据此声称所有EF/Nightfall战技已验收。

## 10. Datapack / Registry

路径 `data/<namespace>/maplesadventure/weapon_damage_profiles/*.json`。

```json
{
  "item":"minecraft:diamond_sword",
  "components":[
    {"channel":"slash","base_ratio":0.75,"scaling":{"strength":0.35,"dexterity":0.25}},
    {"channel":"fire","base_ratio":0.35,"scaling":{"faith":0.8,"intelligence":0.15},"max_bonus":1.15}
  ]
}
```

支持item/tag二选一、priority、disabled。Exact > Tag > 显式Integration > Archetype；同层priority降序、文件ID稳定排序。直接复用WeaponScalingRules解析selector/priority/boolean/系数，不另造JSON框架。每文件独立错误日志、最多4096条规则，Item→Profile编译缓存。

disabled表示停用该层自定义组成并恢复自动单物理Profile，同时阻止较低优先级覆盖；不是把武器伤害归零，也不删除需求/补正。未知channel、重复channel、负值、无穷、超ratio、空列表、9组件、非法枚举包均拒绝。

## 11. Projectile 快照迁移

选择保留Java类ProjectileRequirementPenalty和Attachment ID `maplesadventure:projectile_weapon_requirement`，降低第三方调用/旧档风险；其实际内容升级为version3武器战斗快照，而不是新建并行Attachment。

保存owner、qualified、requirement multiplier、nominal multiplier（延续旧字段scalingMultiplier）、weaponId、weaponBase、最多8个channel的base/bonus。由冻结AR精确恢复通道占比，不能在重载时再查玩家属性或当前datapack。

旧version1无scalingMultiplier→1；旧version2 multiplier×scalingMultiplier原样保持，构造合成基础1的单PHYSICAL Bundle，不能恢复的旧元素信息不猜。新NBT非法时记录警告并回到安全PHYSICAL倍率，避免加载崩溃。

## 12. 旧档 / 保存重启测试

单元测试覆盖version1、version2、新split往返、非法数值、低于1名义倍率、零基础及损坏新NBT。

实际专服发射后，提升INT/FTH到99、换武器、停用datapack：原快照不变。save-all flush→stop→重启同一测试世界→真实客户端重连，以下UUID、数值与通道全部保留（重启还加入EF/Iron's，未改变既有快照）：

| 武器 | UUID | frozen nominal | 通道 |
|---|---|---:|---|
| Bow | fbcf3d82-6e58-4b79-a721-f44e832bc9eb | 1.2412060301507537 | PHYSICAL + MAGIC |
| Crossbow | 3b45ce68-54f7-439e-8ca0-2b378d5a8ca4 | 1.2231155778894474 | PHYSICAL + MAGIC |
| Trident | d31e9248-bacb-4e5e-a817-7e44159f6e9f | 1.4824120603015074 | HOLY |

同投射物作为indirectMagic的directEntity时Policy=1，未错误消费武器Bundle。旧version1/2是反序列化测试，不冒称本轮又对旧世界完成全部实机重启。

## 13. Tooltip / Character Stats

Tooltip逐通道展示base+bonus=AR，紧随该通道自己的补正等级，再列总AR和“非最终扣血”说明；需求独立保留。Shift贡献按通道实际Profile生成，中英文键齐全。

真实英文客户端572×350：Split Sword显示Slash5.3+.9=6.2；Fire2.4+1.3=3.7，分别STR/DEX和INT/FTH等级，总9.9；没有把FTH显示为物理补正。

CharacterStat旧入口选择B：只初始化缺失武器为UNAVAILABLE，withWeapons把WeaponAttackSnapshot/Bundle总AR只读镜像到MAIN/OFF_HAND_ATTACK。没有第二套攻击公式。完整滚动页实测显示Slash、Fire、各自等级；一屏显示不下可滚动。

## 14. Level-Up Preview

草稿最终属性完整计算同一Bundle；Compact按绝对AR变化选择变化最大通道，再显示总AR与需求效率。完整页显示全部通道，不写真实属性/伤害或Skill。

实机FTH40→41预分配：Fire3.7→3.8，总AR9.9→10.0；服务端仍FTH40、XP100000、实际100基础武器伤害仍142.03516。已验证草稿不生效，未对本轮新增内容重复整个升级支付事务矩阵。

## 15. Split Fire / Magic / Holy

隔离世界测试包round8-channels，最终已停用，不打入JAR：

- Sword Slash.75 + Fire.35；STR/DEX/INT/FTH20时Slash6.1997487、Fire3.1517588、总9.3515075。只把FTH提高40，Slash不变，Fire3.7427136，总9.9424623。
- Bow/Crossbow使用Tag覆盖Physical.6继承 + Magic.4(INT.8)，真实原版发射入口捕获不同物理补正、相同魔法补正。
- Trident Pure Holy(FTH.8)：FTH40，base9 + bonus4.3417085 =13.3417085；近战倍率1.48241206，等于相同属性发射快照。
- `scaling:{}`无补正、maxBonus与有限数值约束、需求最后乘35%、恶意JSON由单元测试覆盖。

## 16. 实际伤害测试边界

Vanilla类PLAYER_ATTACK 100基础值经真实目标hurt事件：Split Sword133.59296→142.03516随FTH变化；PureHolyTrident148.24121；独立magic/fire/fall每次100。EF实际源伤害与一次事件计数见第9节。

Bow/Crossbow/Trident通过开发夹具调用真实玩家的原版releaseUsing（30tick充能参数）、弩performShooting，不直接new Projectile伪造快照。为持久化核验冻结其移动，未把这些结果说成物理鼠标长按实射或所有命中/附魔/多重射击组合。复杂模组自定义DoT和完整技能组合仍需后续人工覆盖。

## 17. 协议16

原武器Registry和Held snapshot扩展DamageProfile，未发送ItemStack或完整NBT；MessageNetwork协议15→16。每Item最多8组件，各channel/mode枚举边界、ratio、系数、metadata、总量均验证。

为容纳8组件各带最大长度多字节metadata的最坏情况，Registry由每批32项调整为4项，每批仍最大128KiB；最多8192批/32768物品。最大合法metadata包有单元测试。继续同revision暂存，全部到齐才原子替换。客户端与服务器必须同步更新。

## 18. Optional 启动

本轮真实无EF/无Iron's客户端与专服成功连接、34项profile编译；Tooltip、Draft和伤害验证通过。随后EF21.17.3.1+Iron's3.16.3成功启动/重连，79项profile编译；实际EF伤害测试通过。缺省可选Mixin目标会有原有跳过警告，没有阻塞启动的新链接错误。

本轮不修改任何第三方JAR，没有新增生产Mixin，没有新增多人玩法。测试仅一个真实客户端连接隔离专服，未声称双客户端Phase全矩阵重测或正式成品JAR全套部署验证。

## 19. 变更 / 构建

新增生产（progression/weapon）：WeaponDamageChannel、WeaponDamageComponent、WeaponDamageProfile、WeaponDamageProfileRules、WeaponDamageProfileService、WeaponDamageBundle、WeaponCombatProfileResolver、WeaponHitContext。

修改：WeaponAttackRatingCalculator、WeaponAttackSnapshot、WeaponLoadoutSnapshot、WeaponDamagePolicy、ProjectileRequirementPenalty、WeaponRequirementService/Events/Network/Commands、client/ClientWeaponRequirements、client/WeaponAttackText；progression/ProgressionAttachments（Supplier显式无参，Attachment ID不变）；progression/client/LevelUpScreen；progression/stats/WeaponAttackCalculator文档；network/MessageNetwork；en_us/zh_cn。

测试：新增WeaponDamageProfileTest；更新WeaponRequirementTest批次边界；已有opt-in src/weaponRegression/EpicRegression新增真实EF hurt及事件计数。测试夹具不打包。隔离测试包已停用，测试服与客户端已正常关闭。

最终执行 `gradlew clean build`：BUILD SUCCESSFUL，15秒，8项任务执行；83项测试，0失败、0错误、0跳过。仅有现有API弃用及测试使用RegistryFriendlyByteBuf构造器的弃用警告。

成品JAR核验：新核心类与中英文12项通道/AR键完整；未包含regression测试夹具或round8-channels测试数据包。隔离测试服RCON已恢复关闭，临时密码已清空。

## 20. JAR

`E:\RPG Menu Framework\MaplesAdventure\build\libs\maplesadventure-0.2.0.jar`

大小：1,951,683 bytes。SHA-256：`44470DEBDEF2641E1448C15D14904A0A16713A3BDC260F1DD62AFD2F985387A4`。

默认武器数学等价、split独立补正、单次伤害流程、快照迁移/重启、界面预览和可选依赖启动都有上述证据。未实现也未宣称敌人抗性/弱点已经参与伤害；这些通道是以后在同一次事件内计算抗性的正式输入。
