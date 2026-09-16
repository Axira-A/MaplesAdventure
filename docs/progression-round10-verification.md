# Round 10 — Enemy Defense / Resistance Core

验收记录：2026-09-15～16。工作目录：`E:\RPG Menu Framework\maplesadv\MaplesAdventure`，main 分支，Round 9 基线 `d7ac92e`。续做时已有 Round 10 提交 `d0a39cd`，未回退或覆盖该提交。

## 1. 真实伤害顺序

审计本地 NeoForge **21.1.219 / Minecraft 1.21.1** sources 中 `LivingEntity.actuallyHurt`：

1. Vanilla Armor / Toughness：`getDamageAfterArmorAbsorb`。
2. Resistance / Enchantment：`getDamageAfterMagicAbsorb`。
3. `CommonHooks.onLivingDamagePre` → `LivingDamageEvent.Pre`。
4. Absorption hearts（黄色吸收生命），随后扣血、Post。

Epic Fight **21.17.3.1** 的 `NeoForgeEntityEvent.epicfight$livingDamagePre` 调用 `VanillaEntityEventHooks.onCalculateDamagePre`；本地字节码显示订阅为默认 NORMAL。MaplesAdventure 沿用 LOWEST，因此在上述普通处理之后计算额外 Channel Defense、在黄色吸收生命之前返回一次结果。这里的 Profile absorption 是伤害抗性比例，不是黄色吸收生命。

没有改变原版护甲属性，也没有把 Armor 再换算为 Profile。不能据此宣称任意第三方 LOWEST 监听器都具有固定相对顺序。

## 2. 通道与 Profile

唯一战斗枚举仍是 `WeaponDamageChannel` 的九项。`DamageChannelMapping` 将现有 `DamageDefenseType` 一一映射，不增加第三套类型。PHYSICAL 不是 SLASH/STRIKE/PIERCE 的父级。

`EntityDefenseProfile` 保存 ID、不可变 Channel→`ChannelDefense` 映射、source。未声明项为 D=0、absorption=0；NONE 为 identity。D 必须有限且在 0～10000，absorption 必须有限且在 -0.50～0.80；非法文件整体拒绝并记录原因。

## 3. 数学与单次结算

`DefenseMitigationCurve` 集中实现：

```text
penetration = clamp(A / (A + D × pressure), 0.10, 1.00)
channelMultiplier = clamp(penetration × (1 - absorption), 0.05, 1.50)
```

D=0 时 penetration=1（含 A=D=0），避免 NaN。默认 pressure=.35，SERVER 配置 `maplesadventure-enemy-defense-server.toml` 可在 .05～2.0 调整。

`WeaponCombatResolutionService`：

```text
nominal = originalDamage × bundle.nominalMultiplier
share[channel] = channel.finalAR / totalAR
incoming[channel] = nominal × share[channel]
qualified = Σ(incoming[channel] × channelMultiplier)
final = qualified × requirementMultiplier
```

`WeaponDamageResolution` 保存 original、nominal、每通道 AR/share/incoming/defense/absorption/multiplier/final、qualified、requirement、final。没有增加 EnemyDefenseEvents，也没有新增 hurt 调用；仍由 `WeaponRequirementEvents.damage` 唯一武器数学入口最多调用一次 `setNewDamage`。

Requirement 的 .35 在非中性减伤汇总后只乘一次。测试验证相同通道、相同属性、相同目标下 unmet=qualified×.35。所有有效通道 multiplier=1 时使用原 Round 9 表达式 `originalDamage * bundle.effectiveMultiplier()`，保留乘法结合顺序，避免中性拆分重合导致末位浮点差异。

## 4. 数据包

Profile 路径：`data/<namespace>/maplesadventure/entity_defense_profiles/<id>.json`。

```json
{
  "channels": {
    "slash": {"defense": 45, "absorption": 0.15},
    "strike": {"defense": 20, "absorption": -0.20},
    "fire": {"defense": 10, "absorption": -0.25}
  }
}
```

绑定路径：`data/<namespace>/maplesadventure/entity_defense_rules/<id>.json`。

```json
{"entity":"minecraft:zombie","profile":"mypack:undead"}
```

或：

```json
{"tag":"mypack:stone_creatures","priority":100,"profile":"mypack:stone"}
```

entity/tag 必须二选一。优先级：个体 explicit → Exact EntityType → Tag → NONE。Tag 按 priority 降序、文件 ID 稳定排序；Exact 永远优先 Tag。最多 4096 profiles、4096 rules，引用 ID 最多 256 字符，严格拒绝未知字段/通道和非法数值。

**生产 JAR 不包含任何 Enemy Defense 平衡数据**；stone、undead 仅在 opt-in `weaponRegression` 测试 source set 中。安装本轮本身不会让某类原版生物突然获得弱点。

## 5. 个体 Override 与缓存

Attachment 注册 ID：`maplesadventure:entity_defense_profile`。`EntityDefenseProfileRef` 仅持久化 `profile` ID 与 `dataVersion=1`，不保存完整数值、不同步客户端。

`EntityDefenseService.assign(nonPlayerLivingEntity, profileId)` 是服务器入口；clearOverride 恢复类型规则。合法但已删除的 ID 保留在实体上，解析为 NONE 并显示 UNKNOWN_PROFILE，不偷偷回退到另一条类型规则；未知版本同样安全 fallback。

启动、完成 datapack/tag reload 后编译 EntityType→Binding，再原子替换不可变缓存。命中只做 attachment/type/profile 查询与最多九项的小循环，不扫 JSON/tag，不每 tick 更新、不加载 Chunk。已飞 projectile 仍使用旧 Bundle，但命中读取新目标 Profile。

## 6. 当前范围边界

- 所有 Player 目标均返回 NONE；玩家 DefenseCalculator 仍 PREVIEW_ONLY，异常抗性仍 UNAVAILABLE。
- Only `WeaponHitContext` 进入本层。直接魔法、Fall、Fire DoT 不因 DamageType 名称被猜成某个 Channel。
- `TypedDamageChannelProvider` 仅预留 API，没有注册 Iron’s spell 路由。
- Boss Primary/Add/Child 各自按自己的 EntityType/explicit ref 查询；不自动复制 Primary 防御给 Child，不改 Boss lifecycle、FogGate、Coop、Invasion 或 Loot。
- 没有新增 Mixin、没有新增客户端 payload；协议仍 **17**，不向普通客户端同步 Defense Registry。

## 7. 管理员命令

权限均 >=2；`/maplesadventure` 与 `/ma` 两个根均支持：

```text
/ma defense info
/ma defense setprofile namespace:id
/ma defense clearprofile
/ma defense simulate strike 40 20
/ma defense lasthit
```

前四项使用玩家准星射线，最大 32 格、方块 LOS、Phase 可见性检查。simulate 不产生伤害。lasthit 显示每通道及汇总，缓存最多 256 名攻击者、60 秒有效，服务器停止清理，无持久化/普通客户端同步。

## 8. 自动回归

`EnemyDefenseTest` 新增 11 个 JUnit 测试，覆盖：

- canonical 映射，SLASH/STRIKE/PIERCE 不额外吃 PHYSICAL。
- 曲线示例、纯 absorption/weakness、安全下限、零 AR、非有限值/越界/非法文件。
- 全部自动 archetype × 八种 infusion × 属性 5/20/39/40/60/99 × 需求 1/.35 × 多种 float 原始伤害，与 Round 9 **double bits 和最终 float bits 均严格相等**。
- 150 Slash +50 Fire 按 75%/25% 最终 AR 拆分，而不是原 baseRatio。
- Requirement 最后乘一次、Sacred 神圣弱点、自然 split damage tax，无人工 split penalty。
- Exact/Tag/priority/文件顺序，explicit/ref NBT、未来版本和删除 ID fallback。
- Projectile Bundle NBT 往返不变、目标防御变化影响结果。

最终完整测试数量及 clean build 结果见下方构建记录。

## 9. 专服真实 Damage 管线回归

隔离测试目录：`run/round10-defense-server`。开发 fixture 以服务端 FakePlayer 作为受控攻击者，对真实 Mob 调用实际 hurt，经历 NeoForge 与可选 Epic Fight 的真实事件链；**不是双真人客户端操作，也不是只调用纯数学函数**。

已通过：

- 无 Epic Fight / Iron’s 的专服正常启动，`defenseregression run` PASS（2026-09-16 重复通过）。可选 Mixin 缺目标类有已有警告，但不阻止启动。
- Epic Fight 21.17.3.1 + Iron’s 3.16.3 及其依赖的专服正常启动；实际 Epic Fight 武器 DamageSource 测试 one Pre event、single resolution/HP 一致。
- 八种 infusion 的中性真实伤害保持 Round 9；0 与 12 Armor 的目标显示原版护甲先处理，Profile 不修改 Armor。
- requirement=.35 最后应用；真实 arrow source 使用发射时 Bundle，发射后重新质变不改变攻击快照，给同一目标换 Profile 会改变结果。
- 玩家显式挂 Profile 仍 NONE；跨 Phase hurt 在本层之前拒绝（HP 不变，没有 LastHit）。
- `indirectMagic`、onFire、fall 实际伤害不进入武器 Profile 管线。
- 管理员准星 set/info/clear/simulate 实际执行；确认选中测试实体且 simulate 不伤血。
- 实际 EntityType Exact 胜过高 priority Tag，等 priority Tag 按文件稳定排序。

Epic Fight 样本记录：raw=20，nominal=27.44321608040201，D=40，qualified/final=11.166877153927476，实际 float HP loss=11.16687；无重复乘算。

## 10. 持久化、重载与区块测试

- 完整 Entity NBT save/load 保持 explicit ref。
- 测试实体 UUID `9d3e2b1f-a047-41a9-8754-1621e416bf43` 经完整专服停止/重启仍为 `weaponregression:stone`。
- 热重载该测试 Profile 为 PHYSICAL D=123 / absorption=.25 后，同一存活实体解析到新值；删除定义再 reload 后保留同 ID，返回 EXPLICIT UNKNOWN_PROFILE / NONE；没有崩溃。随后已恢复 fixture 定义。
- 2026-09-16 区块探针 `10000000-0000-0000-0000-000000000110`：seed loaded=true → 释放票据后 check loaded=false → load 后等待异步 Entity IO → check loaded=true；ref 验证通过。
- 临时 Chunk ticket 只存在开发测试命令；check 后释放。生产服务没有强制加载逻辑。

可复现开发命令：

```text
gradlew runServer -PweaponRegression=true -PrunDirectory=run/round10-defense-server
defenseregression run
defenseregression persist
# 正常 stop/restart 后
defenseregression checkpersist
defenseregression chunkseed
# 等待正常卸载
defenseregression chunkcheck
defenseregression chunkload
# 等待异步实体读取，然后释放测试票据
defenseregression chunkcheck
```

## 11. 未完成的人工验收边界

本轮没有启动两个真人客户端，未逐项人工重测 BossBar/FogGate/Coop/Invasion 完整生命周期，也没有人工操作 Epic Fight 动画与所有 modded weapon。当前通过的是服务端 Phase 拒绝、真实 EF source 伤害链、单位/持久化/命令回归；不能等同于完整双客户端视觉验收。弩/三叉戟继续走既有冻结 Bundle 路径，本轮实际 projectile source 样本为 Arrow，没有另做完整弩/三叉戟飞行人工录像。

## 12. 文件范围

新增生产文件：`config/EnemyDefenseConfig.java`；`progression/defense/` 下 ChannelDefense、DamageChannelMapping、DefenseMitigationCurve、EntityDefenseProfile、EntityDefenseProfileRef、EntityDefenseRegistry、EntityDefenseService、EntityDefenseCommands、LastWeaponDamageResolution、TypedDamageChannelProvider；`progression/weapon/WeaponCombatResolutionService.java`、`WeaponDamageResolution.java`。

修改注册/入口：MaplesAdventure、ProgressionAttachments、ProgressionEvents、DamageDefenseType、WeaponRequirementEvents。没有更换旧 projectile attachment ID、没有改 Infusion 数据。

测试：EnemyDefenseTest；opt-in DefenseRegression、EpicRegression、WeaponRegressionMod；测试专用两个 Profile、三个 rules、一个 entity_type tag。本次续做还修正了 chunkload 测试票据的异步读取生命周期。新增本报告。

## 13. 最终构建

2026-09-16 执行默认生产构建 `gradlew clean build`：**BUILD SUCCESSFUL in 15s**。

JUnit XML 汇总：**103 tests，0 failures，0 errors，0 skipped**，其中本轮新增 11 tests。存在既有 NeoForge deprecated API 编译警告，不影响构建。`git diff --check` 无空白错误。

最终文件：`E:\RPG Menu Framework\maplesadv\MaplesAdventure\build\libs\maplesadventure-0.2.0.jar`。

SHA-256：`CBC82D9EFCE64DD5D929BF3D2DDD1469AE7E6D103BA96EA320093A50052FCEAE`。

已检查 JAR 包含 EntityDefenseService / WeaponCombatResolutionService，且不含 `weaponregression`、DefenseRegression、`entity_defense_profiles` 或 `entity_defense_rules` 测试资源。专服回归使用相同生产源代码加 opt-in fixture 的开发运行环境；本次未把最终 JAR 部署到真人客户端整合包。

测试专服已正常停止，临时 RCON 已关闭、密码清空，临时 RCON 脚本已移除。测试世界/专服日志保留在隔离 run 目录，未修改正式世界或第三方 JAR。
