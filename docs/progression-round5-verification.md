# RPG Round 5 — Iron's School Scaling：实现与验证

交付核对：2026-09-06。实机测试：2026-09-05。

本轮实现 INT / FTH / ARC → Iron's 九大学派 School Power。没有修改最终伤害事件、施法速度、冷却、Mana 消耗、武器公式或第三方 JAR。MIND 的 Mana 职责保持不变。

## 1. 实际版本与 API 审计

审计对象是工作区 `.research/irons_spellbooks-1.21.1-3.16.3.jar`，而非按旧版教程猜测。

- Minecraft 1.21.1，NeoForge 21.1.219，Java 21。
- Iron's modid：`irons_spellbooks`，版本 `1.21.1-3.16.3`。
- 实机依赖：Iron's Lib 2.1.0、Curios 9.5.1、GeckoLib 4.9.2、Player Animator 2.0.4。
- Registry：`io.redspace.ironsspellbooks.api.registry.SchoolRegistry.REGISTRY`。
- School：`io.redspace.ironsspellbooks.api.spells.SchoolType`，`getId()`、`getDisplayName()`、`getPowerFor(LivingEntity)`。
- 属性：`io.redspace.ironsspellbooks.api.registry.AttributeRegistry`。
- `SchoolType.powerAttribute` 是非 public 字段，没有公开 Holder getter。因此增加一个只读、`@Pseudo`、`remap=false` 的 `IronsSchoolPowerAccessor`，让附属 School 也能取得其实际 Attribute，而不是硬编码九个字段。
- 没有新增伤害、Projectile、AOE、治疗 Mixin。

当前 School Power 为基值 1 的同步 RangedAttribute，范围 -100..100。`SchoolType.getPowerFor(entity)` 读取该 School 对应 Attribute；不存在该属性时返回 1。

`AbstractSpell.getSpellPower(level, entity)` 的原生语义为：

```text
(baseSpellPower + spellPowerPerLevel * (level - 1))
    * entity.globalSpellPower
    * school.getPowerFor(entity)
    * spellConfigPowerMultiplier
```

具体法术仍决定哪些伤害、治疗或其他效果使用该数值；没有把整个最终伤害再乘一次。

## 2. 九大学派真实 ID / 默认 Profile

以下 School 都位于 `irons_spellbooks` namespace。对应 Attribute 的完整 ID 是 `irons_spellbooks:<path>_spell_power`；全局属性为 `irons_spellbooks:spell_power`。

| 学派 | School ResourceLocation | INT | FTH | ARC | max_bonus |
|---|---|---:|---:|---:|---:|
| 自然 | irons_spellbooks:nature | .85 | .15 | 0 | .75 |
| 雷霆 | irons_spellbooks:lightning | .85 | .15 | 0 | .75 |
| 冰霜 | irons_spellbooks:ice | .90 | .10 | 0 | .75 |
| 末影 | irons_spellbooks:ender | .80 | 0 | .20 | .75 |
| 邪术 | irons_spellbooks:eldritch | .15 | 0 | .85 | .75 |
| 猩红 | irons_spellbooks:blood | 0 | .15 | .85 | .75 |
| 唤魔 | irons_spellbooks:evocation | .20 | .80 | 0 | .75 |
| 神圣 | irons_spellbooks:holy | 0 | 1 | 0 | .75 |
| 炽焰 | irons_spellbooks:fire | .15 | .85 | 0 | .75 |

## 3. 曲线与 Modifier

继续调用原有 `OffensiveScalingCurve.evaluate(stat)`：

| stat | rating（0..1） |
|---:|---:|
| 5 | 0 |
| 20 | .3015075377 |
| 40 | .6030150754 |
| 60 | .8040201005 |
| 99 | 1 |

```text
bonus = (curve(INT)*weightINT + curve(FTH)*weightFTH + curve(ARC)*weightARC) * max_bonus
```

每个学派只增加自己的稳定 `ADD_VALUE` Modifier：

```text
maplesadventure:progression_spell_power/<school namespace>/<school path>
```

选择 ADD_VALUE 是因为实际 School Attribute 基值为 1，`.362` 的贡献就是增加 `.362` 学派单位，即 36.2 个百分点。它不是对最终伤害进行额外百分比相乘。原有装备的 ADD_VALUE、ADD_MULTIPLIED_BASE、ADD_MULTIPLIED_TOTAL 和全局 Spell Power 均保留。

刷新先清理本模组该前缀的旧 Modifier，再按当前 Profile 添加。没有 `setBaseValue`，也不会清理其他模组的 Modifier。两份 Profile 若共享同一实际 Attribute，会拒绝第二次写入并警告，避免双加成。

## 4. 数据包与扩展

默认文件位于 `src/main/resources/data/maplesadventure/spell_school_scaling/*.json`。例：

```json
{
  "school": "irons_spellbooks:nature",
  "weights": { "intelligence": 0.85, "faith": 0.15, "arcane": 0.0 },
  "max_bonus": 0.75
}
```

`SpellSchoolScalingRegistry` 是服务端 JSON Reload Listener，以 ResourceLocation 为 Key；不是九个固定枚举分支。

- 最多 256 个 Profile；权重必须有限、非负，和为 1（容差 1e-6）；max_bonus 为 0..10。
- 未知字段、未知属性名、负数、非数值、非有限值、重复 school 等会明确拒绝该项并记录日志。
- 相同资源路径的数据包覆盖遵循 Minecraft ResourceManager 优先级；多个不同文件声明同一 school 时，按资源 ID 排序后拒绝重复项。
- 没有 Profile 的附属 School 不添加成长加成。
- Profile 指向尚未注册的 School：不崩溃，警告一次，快照标记 PREVIEW_ONLY，不假装已创建该 School。
- 已注册附属 School 若沿用 SchoolType 的线性 Attribute 语义，可通过 Profile 接入；覆盖了 `getPowerFor` 的自定义公式若与当前模型不匹配，预览降级，不伪称准确。
- 本轮没有安装一个真正新增 School 的附属模组；扩展数学/动态集合有自动测试，真实附属 Registry 接入尚未实测。

## 5. Optional Adapter / 生命周期

Core 的 `SpellScalingRuntimeService` 不 import Iron's 类。只有 ModList 检测到 `irons_spellbooks` 才加载 `IronsSpellScalingAdapter`。

Adapter 负责实际 Registry、Holder、AttributeInstance、Modifier；Core 负责 Profile、曲线、快照、预览与协议。

刷新纳入既有 `DerivedStatRuntimeService`：登录、重生、属性 set/add/reset/upgrade、显式 refresh 等路径；数据包 reload 后刷新在线玩家。不会每 Tick 重算学派或广播。

属性仍属于 Player，不存入 Phase、Coop 或 Invasion Session。没有修改 Foreign Return 数据模型。

## 6. 动态 Snapshot / UI / Preview

`AttributeSnapshot` / `CharacterStatsSnapshot` 增加 `SpellSchoolScalingSnapshot`，内部 Map 的每项包含：

- School ID、官方本地化显示名、Profile 权重。
- 成长贡献、Modifier 是否存在、ACTIVE / PREVIEW_ONLY。
- 排除本模组成长 Modifier 后的 Attribute 计算上下文、全局 Spell Power、实际观察值。

`SpellPowerContext` 保存未 clamp 的计算操作数，而非直接从已 clamp 的最终值减去成长。Draft 只替换成长贡献，保留所有其他 Attribute 操作及范围限制。

INT/FTH/ARC 显示为 0..100 的“补正评级”，不是额外伤害百分比。单一 `SPELL_POWER` 不再冒充九大学派总攻击力。

详细面板使用现有可滚动列表动态列出全部 School；Hover 显示成长、权重、状态、可可靠读取的最终 School / School×Global 倍率。未可靠接入时不显示虚假的 Runtime 数字。名称、说明、中英文语言文件均已更新。

升级紧凑预览优先显示改变的评级和成长变化最大的学派；完整面板显示全部。预览调用原有成长曲线，不更改真实 Modifier。

网络复用 AttributeSnapshot 通道，新增有界 School Codec。MessageNetwork 协议 **12 → 13**，客户端与服务端必须一起更新；不支持新旧版本混连。

### 实际 UI 验证

使用 computer-use 技能操作真正 Minecraft 客户端，检查了紧凑预览、确认、完整滚动面板和 Tooltip，文字清晰，背景 Blur 没有覆盖内容。

- INT39 → 临时 +1：服务端仍 INT39，取消不会改属性。
- 实际确认：Level39→40，XP100025→99136，扣费889；冰霜成长40.7035%，自然/雷霆38.4422%，与预览一致。
- 原需求 INT39 的示例评级57.3与现有曲线不一致；代码没有改曲线凑示例，实际58.79→60.30。
- 数据包重载期间存在 +1 草稿：客户端收到 STALE_STATE，草稿清零，提示重新分配，没有扣费。
- 早期截图右侧被 Minecraft 新手教程提示局部覆盖；不是本模组 Blur。最终无 Iron's 检查中该提示已消失。

## 7. 专服 + 真实客户端法术测试

使用独立 `run/round5-irons-server` 专服和真实 `SpellTest` 客户端。不是单人集成服务器，也没有改生产存档。

无装备 Spell Power Bonus；同一法术等级1、Normal difficulty。各样本采用相同参数的独立 Zombie（1000HP、armor0、NoAI、knockback resistance1）与隔离测试位置，防止上一轮毒云/燃烧污染下一轮。INT/FTH/ARC 在这张表中同时取5/40/60/99；主副属性分别变化另外用数学测试和 INT40 实机 Debug 核对。

数值为约4.5~5秒后目标实际损失 HP（治疗行为单独列出）：

| School / 真实 Spell ID path | 5 | 40 | 60 | 99 |
|---|---:|---:|---:|---:|
| fire / firebolt | 8.0000 | 10.7136 | 11.6181 | 12.5000 |
| ice / icicle | 6.0000 | 8.7136 | 9.6181 | 10.5000 |
| lightning / lightning_bolt | 10.0000 | 14.5226 | 16.0302 | 17.5000 |
| ender / magic_missile | 6.0000 | 8.7136 | 9.6181 | 10.5000 |
| blood / blood_slash | 10.0000 | 14.5226 | 16.0302 | 17.5000 |
| nature / poison_arrow | 10.5499 | 15.3212 | 16.9118 | 20.0814 |
| eldritch / eldritch_blast | 15.0000 | 21.7839 | 24.0452 | 26.2500 |
| evocation / fang_strike | 6.0000 | 8.7136 | 9.6181 | 10.5000 |
| holy / heal（实际恢复 HP） | 5.0000 | 7.2613 | 8.0151 | 8.7500 |

所有 Spell ID 均为 `irons_spellbooks:<path>`。

基准学派倍率为1；三属性40/60/99时为1.4522613 / 1.6030151 / 1.75。直接效果符合一次学派缩放。Firebolt 还有原生燃烧（该组约2HP）；Poison Arrow 存在原生持续效果与采样时序，因此不能把其最终总扣血量当精确的单次比例测试。没有添加额外 DoT/伤害事件倍率。

Eldritch 最初被原生知识前置拦截，客户端显示 “You can't understand this spell”。通过 Iron's 自带管理员命令仅给测试角色学习该法术后重测：

```text
execute as SpellTest run learnSpell learn irons_spellbooks:eldritch_blast
```

早期未学习、冷却/施法尚未完成以及落脚点未建好导致的未命中样本均不算通过结果。

### 神圣治疗

自然回血关闭，Survival 测试角色先恢复20HP，再受15点伤害至5HP，然后 `/cast SpellTest irons_spellbooks:heal 1`。治疗后10 / 12.261307 / 13.015076 / 13.75HP。

`HealSpell.onCast` 使用 `getSpellPower` 作为 healAmount；MaplesAdventure没有另写治疗规则，保留 Iron's 原生事件/治疗路径。

## 8. 装备、幂等、重载、持久化

- 实穿 `irons_spellbooks:pyromancer_chestplate`：全局 Power1.05、火焰装备 +.10 仍存在。INT40 的火焰 School Power 为1.167839195…，不是被成长公式覆盖。
- 连续10次 `/ma attribute refresh SpellTest`：不叠加。
- 所有三种 Modifier Operation 和 clamp 的保留由纯数学测试额外覆盖；未实穿测试所有饰品/药水组合。
- 数据包将 nature 改为 INT1 / max_bonus .60：在线三属性99玩家自然倍率立即1.60。
- 同包 blood 为负权重：仅该配置被拒绝，blood 原成长 Modifier 清除，实际属性1.0。
- 未注册 `test:unregistered_school`：警告、PREVIEW_ONLY、无崩溃。
- 禁用测试包后 nature/blood 恢复默认1.75，不需重登。
- 真正死亡并自动 Respawn 后：Health20、INT40仍在、学派 Modifier 数值保持、XP0。Lost Soul 保存文件存在；本轮没有单独解码该文件验证魂内精确XP，也没有完成回收魂实机测试。
- 保存、正常关服、重新启动并让同一真实客户端登录：INT40、自然1.384422…、冰霜1.407035…等保持，未重复叠加。
- Coop/Invasion 的实机加入/遣返本轮未重新执行；既有相关数据单元测试通过，代码未把 Spell Modifier 绑定 Session。这不等于完整多人玩法回归已通过。

## 9. 无 Iron's 环境

另启 `run/round5-no-irons-server`，Runtime mod list 仅 Minecraft、NeoForge、MaplesAdventure；客户端同样不加载 Iron's 和其依赖。

- 专服 Done，`NoSpellTest` 真实客户端成功连接。
- `/ma attribute spell NoSpellTest` 显示 adapter=false，九项 PREVIEW_ONLY / runtime=unavailable。
- 升级 Screen 与完整能力面板可打开、滚动，学派 Tooltip 显示“仅预览，尚未应用到游戏”。
- 未出现致命 NoClassDefFoundError / MixinApplyError。`@Pseudo` 缺省目标产生普通 “Error loading class ... ClassNotFoundException” WARN（既有其他 optional mixin 也如此），不导致启动失败；并非宣称日志中完全不存在缺类警告。
- 使用既有测试客户端目录但不同纯净服务端世界；没有把正式玩家存档拿来移除模组。
- 全部本轮测试客户端/服务器已关闭，两个测试世界和日志保留，未删除用户存档。

## 10. 自动测试与构建

`gradlew clean build --console=plain`：**BUILD SUCCESSFUL in 14s**，55项测试，0失败、0错误。

新增7项 `SpellSchoolScalingTest` 覆盖共享曲线、九Profile边界、主副权重、所有 Attribute 运算与 clamp、动态附属School模型、纯Draft、非法JSON、真实变化排序。其余48项现有测试通过。

构建有2项既有弃用警告：EncounterConfig 的 defineListAllowEmpty，以及 EncounterSpawnService.finalizeSpawn；不是本轮编译错误。

最终 JAR：

```text
E:\RPG Menu Framework\MaplesAdventure\build\libs\maplesadventure-0.2.0.jar
size: 1,827,668 bytes
SHA-256: 064E73362992445BB4B00DF69C6A5AB8E2974ABCBEC77958C3B71550349DA723
```

已检查 JAR 内有全部9个Profile、新Adapter与Accessor，无 `io/redspace/` 第三方实现类混入。

实机运行通过 Gradle NeoForge development launch，使用最终编译输出；未另外把该成品JAR部署到用户正式整合包验收。没有声称完整附属Spell、所有装备或三客户端Coop/Invasion均已覆盖。

## 11. 文件清单

新增：

```text
src/main/java/dev/maplesadventure/progression/spell/
  SpellSchoolScalingProfile.java
  SpellSchoolScalingRegistry.java
  SpellPowerContext.java
  SpellSchoolStat.java
  SpellSchoolScalingSnapshot.java
  SpellSchoolSnapshotCodec.java
  SpellScalingAdapter.java
  SpellScalingRuntimeService.java
src/main/java/dev/maplesadventure/integration/irons/progression/IronsSpellScalingAdapter.java
src/main/java/dev/maplesadventure/mixin/IronsSchoolPowerAccessor.java
src/main/resources/data/maplesadventure/spell_school_scaling/
  nature.json lightning.json ice.json ender.json eldritch.json
  blood.json evocation.json holy.json fire.json
src/test/java/dev/maplesadventure/progression/SpellSchoolScalingTest.java
.research/Round5SpellProbe.ps1
docs/progression-round5-verification.md
```

修改（相对 `src/main/java/dev/maplesadventure/`）：

```text
network/MessageNetwork.java
progression/AttributeSnapshot.java
progression/LevelUpPreviewCalculator.java
progression/OffensiveScalingCurve.java（注释，公式未改）
progression/ProgressionCommands.java
progression/ProgressionEvents.java
progression/client/CharacterStatsScreen.java
progression/client/LevelUpScreen.java
progression/network/AttributePayloads.java
progression/runtime/DerivedStatRuntimeService.java
progression/stats/CharacterStatCalculator.java
progression/stats/CharacterStatsService.java
progression/stats/CharacterStatsSnapshot.java
progression/stats/SpellScalingCalculator.java
progression/stats/SpellScalingSnapshot.java
progression/upgrade/UpgradeAccessService.java
```

另外更新 `src/main/resources/maplesadventure.mixins.json` 与 en_us/zh_cn 语言文件。测试配置、临时数据包及日志只在 `run/round5-*`，不进入发布JAR。工作区无Git元数据，此清单按本轮编辑记录核对，不覆盖或撤销先前修改。
