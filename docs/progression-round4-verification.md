# RPG Round 4：负重与自动闪避实现、验证记录

日期：2026-09-05。Minecraft 1.21.1 / NeoForge 21.1.219 / Java 21。

## 交付状态

实现及 `gradlew clean build` 已完成。48 项自动化测试通过；Epic Fight-only 专服与真实客户端完成负重、技能槽、持久化与升级预览检查。**尚未完成全部战斗实测，不能据此宣布 Round 4 全部验收通过。**

Nightfall 完整组合的专服启动成功，但客户端未完成登录，出现 GLFW 原生崩溃；因此 Nightfall 技能实机授予、动作距离和实际消费测量仍待验证。Coop/Invasion 本轮未进行多人实机回归。

运行测试使用 Gradle 开发启动目录中的当前类与资源；不是已部署最终打包 JAR 的生产整合包验收。最后一次小改为远端玩家位移保护，随后再次 clean build；该保护尚未多人实测。

## 1. Epic Fight Slot/API 与真实技能 ID

直接审计工作区 `epic-fight-21.17.3.1-mc1.21.1-neoforge.jar`。

- 唯一受管理槽位：`SkillSlots.DODGE`。
- 原版 Roll：`epicfight:roll`；Step：`epicfight:step`。
- `SkillContainer.setSkill` 更换已装备技能；不重建 PlayerSkills 或其他槽位。
- 新学技能通过 `SPAddLearnedSkill` 同步，槽位通过 `SPChangeSkill` 同步。
- 只管理 DODGE，不更换 Guard、Passive、Mover、Weapon Innate 等槽位。

## 2. Nightfall 实际信息

工作区 `NightFall-1.21.1-Neoforge-3.4.0.jar`：modid **efn**，版本 **3.4.0**。

- Souls Roll：`efn:efn_dodge`。
- Souls Step：`efn:efn_step`。
- `NightfallDodgeAdapter` 同时检查 ModList 和真实技能 Registry；没有 Nightfall 类的直接类型依赖。
- Nightfall `EFNDodgeSkill` 直接继承 Skill，不能按 `DodgeSkill instanceof` 判断。
- 其 `EFNDodgeAnimation_ROLL/STEP` 继承 Epic Fight `DodgeAnimation`，因此采用动画语义而非动画资源名判断。

测试缺失的 Invincible Lib 从[官方版本页](https://modrinth.com/mod/epic-fight-invincible-lib/version/21.15.8.2-mc1.21.1-neoforge)取得，验证 SHA-512 后只放在隔离测试 mods 目录。未加入 MaplesAdventure JAR，也未修改任何第三方 JAR。

## 3. 初始化与旧玩家迁移

`CombatSkillInitializationState` 是带序列化与 copyOnDeath 的玩家 Attachment：

- DataVersion=1。
- VanillaDodgeVersion。
- NightfallDodgeVersion。

旧玩家没有标记时会安全补学原版 Roll/Step；Nightfall 安装且注册成功时单独补学其两项技能。已学习技能不重复 Learn，不删除旧技能，不每次登录重复初始化。以后安装 Nightfall 不需要删除玩家数据。

卸载 Nightfall 后使用原版 ID 回退。Epic Fight 的技能 NBT 读取会忽略不存在的技能；MaplesAdventure 再按负重恢复有效槽位。本轮使用含 Nightfall ID 的**复制测试存档**验证了缺模组恢复，不等于已完成真实安装→游玩→卸载的完整循环。

## 4. 唯一负重 Profile

`EquipLoadTier` 使用 LIGHT / NORMAL / HEAVY / OVERLOADED；`EncumbranceProfile` 集中保存全部效果。

| Tier | 默认比例 | 移动 | 恢复 | 消费 | 闪避 | 位移 |
|---|---|---:|---:|---:|---|---:|
| LIGHT | <30% | 1.03 | 1.15 | 0.85 | STEP | 1.00 |
| NORMAL | 30%–<70% | 1.00 | 1.00 | 1.00 | ROLL | 1.00 |
| HEAVY | 70%–100% | 0.92 | 0.80 | 1.20 | ROLL | 0.75 |
| OVERLOADED | >100% | 0.80 | 0.60 | 1.40 | NONE | 0.00 |

`EquipLoadConfig` 为 SERVER 配置；轻装移速 bonus 限定 0–0.08。NORMAL 的四个倍率精确为 1，不保留 MaplesAdventure 移速/恢复 Modifier。

装备重量读取 Epic Fight WEIGHT 属性的 value−base，排除普通玩家身体基础重量。当前最大负重仍复用 END 公式；本轮没有另建物品重量表。没有 Epic Fight 时负重来源不可用，采用中性 NORMAL/NONE，不伪造可用的战斗接口。

## 5. 自动选择、同步与延迟切换

`EncumbranceRuntimeService` 在登录、装备事件、属性真正提交、重生等节点刷新。装备事件排队到 ServerTick.Post，使装备属性 Modifier 已落定后再读取重量。

LIGHT 选择 Step；NORMAL/HEAVY 选择 Roll；OVERLOADED 清空 DODGE。Nightfall 可用时优先 Nightfall ID，否则原版回退。

服务器当前动画由 `getPlayerFor(null).getRealAnimation()` 取得；DodgeAnimation 未结束时只记录 pending，动画结束后换槽。移动/精力倍率不必等动画结束。没有反复 Learn/Forget。

AttributeSnapshot 新增服务器权威的当前重量、Tier、实际 DodgeMode，以及阈值与四套 Profile。客户端预览同样使用服务器策略；不上传 Tier。协议版本为 **12**，客户端/服务端必须一起更新。

## 6. 防止手动覆盖与新增 Mixin

优先使用 Epic Fight 公开事件；没有对应公开入口的部分使用小型 optional Mixin：

- `EpicFightDodgeSkillChangeMixin`：CPChangeSkill 服务端入口，只拒绝 DODGE 请求并回同步当前槽位。目标实际为 interface，Mixin 也为 interface。
- `EpicFightDodgeSlotOwnershipMixin`：初始化之后守卫 `SkillContainer.setSkill(Skill,boolean)`，覆盖技能书/add-on 绕过普通换技能包的路径；内部自动换槽使用作用域标记。初始 NBT 加载不被拦截。
- `EpicFightDodgeSlotButtonMixin`：Epic SkillEditScreen 的 DODGE 按钮禁止手动编辑，提供负重决定闪避的 Tooltip；不隐藏整个界面。
- `EpicFightOverloadedDodgeMixin`：本地预测拒绝超重 DODGE 并提示；服务端 CAST_SKILL 再验证。
- `EpicFightDodgeMovementMixin`：ActionAnimation.move 的根位移向量缩放；不修改动画速度。
- `EncumbranceSprintMixin`：Vanilla LivingEntity.setSprinting 的玩家局部守卫。没有足够的可取消 Sprint start 公共事件；服务端还清除已存在的超重 sprint flag。

Epic Fight 目标均 optional（Pseudo/require=0），未安装 Epic Fight 的真实专服和客户端已启动并连接。技能 GUI 的最终按钮禁用交互尚未实机验证。

## 7. Stamina Regen 真正入口

复用 Epic Fight 原生 `STAMINA_REGEN` 属性，稳定 ID：`maplesadventure:encumbrance_stamina_regen`，ADD_MULTIPLIED_TOTAL。移除旧同 ID 后按 Profile 添加；不通过每 tick 额外 setStamina 模拟恢复。

不替换 Epic Fight 原有恢复条件、技能或药水效果。NORMAL 移除本模组的恢复修正。

## 8. Stamina Cost 唯一入口

只注册一次 `EpicFightEventHooks.Player.CONSUME_SKILL`，仅处理 STAMINA：

`finalAmount = event.originalAmount × profile.staminaCostMultiplier`。

服务器真正支付与本地玩家可支付预测使用同一乘数；不是在 Skill 计算和最终 setter 各乘一次。DEBUG 日志包括 original、multiplier、final。

审计 `consumeForSkill` 和持有技能消费路径；普通通过此 API 的攻击、技能及防御消费获得倍率。第三方/特殊技能若直接减 `setStamina` 而绕过标准消费事件，暂不覆盖；例如审计中发现的 MeteorSlam/ForbiddenStrength 路径。不能通过全局 setter 拦截补洞，否则会误改恢复和返还。

Epic Fight 自带重量消费算法仍保留，MaplesAdventure 乘在其结果上。NORMAL 表示本模组不加减，不意味着删除 Epic Fight/Nightfall 自有机制。

## 9. 移动与短翻滚

`maplesadventure:encumbrance_movement` 使用稳定 ADD_MULTIPLIED_TOTAL Modifier，不 setBaseValue、不覆盖其他来源。

HEAVY 在 `ActionAnimation.move` 的实际 Vec3 根位移上乘 0.75；基于调用实例是否 DodgeAnimation，覆盖其 link 路径以及 Nightfall 子类。动画时间不变。

最后补充：客户端只处理自己的预测位移；远端 Player 不套用本地负重，继续由权威实体同步移动。

代码入口与继承链已核对，但实际距离比、执行期间跨 Tier 的连续性尚未通过操作测量。Nightfall Roll 自身增加的 `efn:roll_skill_stamina_bonus` 等原生机制没有被静默删除。

## 10. UI 与 END 预览

Character Stats 显示中文轻装/正常/重装/超重和实际效果。升级预览优先显示 Tier、移动、恢复、消费、Dodge 变化，复用共享计算器与同步的 Profile。

真实 UI 测试发现窄窗口右栏重叠，已修复：密集布局让出可用高度，Tier/Dodge 分行，数值按宽度适配。使用 **computer-use skill** 检查了实际 Minecraft 窗口，而不是仅看静态布局代码。

实测 END45、装备51，Draft END+1：Heavy→Normal，移动92%→100%、恢复80%→100%、消费120%→100%、Short roll→Roll。查询服务器仍是 END45/HEAVY，取消后同样未改变。真实 END46 提交/管理员设置后立即 NORMAL/ROLL。

## 11. 已完成验证

隔离 Epic Fight-only 专服与真实客户端：`run/round4-nightfall-removal-server` / `run/round4-nightfall-removal-client`，角色 EncTest。

- 裸装0/40：LIGHT、STEP、1.03/1.15/0.85。
- 钻石胸甲24/40：NORMAL、ROLL，移动/恢复没有本模组 Modifier。
- 加靴33/40：HEAVY、ROLL、0.92/0.8/1.2、距离配置0.75。
- 加护腿51/40：OVERLOADED、NONE、0.8/0.6/1.4。
- 100 次穿/脱胸甲（各步跨服务端 tick）：最终移速/恢复各只有一个 Modifier，Learned Roll/Step 无重复。
- END20 后死亡/自动 Respawn，属性与 STEP 恢复；此测试 XP 为0，不能代替完整 Lost Soul 剩余 XP 回归。
- 关闭/重启同测试世界，END20、初始化标记、LIGHT/STEP 保持。
- 含缺失 Nightfall 技能 ID 的复制存档可在 Epic Fight-only 环境登录并回退 STEP。
- END45→46、预览取消与小窗口布局检查通过。

无 Epic Fight：`run/round4-no-epic-final` / `run/round4-no-epic-client`，专服与客户端成功连接；报告 NORMAL/NONE，战斗资源为 PREVIEW_ONLY，无缺类启动失败。

自动测试48项/18 suites：0 failures、0 errors。其中 EncumbranceMathTest 覆盖边界、NORMAL精确中性、END跨Tier预览不改原值、实际重量计算与服务器自定义策略。

## 12. Nightfall 测试结果与未通过项

组合：Epic Fight21.17.3.1 + Nightfall3.4.0 + Invincible21.15.8.2，另含 Nightfall 内嵌 Avalon/Geckolib/VIX。

`run/round4-nightfall-final` 专服于13:43:32到达 Done；可用 RCON。

`run/round4-nightfall-client` 客户端未完成登录，13:48:23发生 `EXCEPTION_ACCESS_VIOLATION`，native frame `glfw.dll+0x101ab`，Java 调用栈含 RenderSystem.limitDisplayFPS。保存 `hs_err_pid6120.log`。此外日志有 efn 动画缺 constructor 信息、Avalon JSON/资源加载问题。**尚未证实这些资源错误与原生崩溃的因果关系，不能直接归因某一个模组，也不能据此宣布已修好。**

尚待真人战斗验收：

- Nightfall 首次/旧玩家补学与四档实际动作。
- 完整真实 Nightfall 安装后再卸载的同存档循环（当前只做合成缺失 ID 测试）。
- 精力消费与恢复的实际时间/数值对照、无重复倍率。
- 短滚实际位移75%、闪避期间变装不截断动画、超重持续 sprint/dodge 输入拒绝。
- Skill GUI/技能书手动覆盖的实际点击测试。
- Coop/Invasion 角色切换与远端动画多人回归。

上述未完成项不能列为通过。没有改 Phase、Session、Loot 或第三方 JAR 来规避测试。

## 13. 主要新增/修改文件

以下路径以项目根 `E:/RPG Menu Framework/MaplesAdventure` 为基准：

- 新增 `src/main/java/dev/maplesadventure/progression/encumbrance/`：EquipLoadTier、DodgeMode、EncumbranceProfile、EncumbrancePolicySnapshot、EquipLoadRuntimeSnapshot、CombatSkillInitializationState、EncumbranceCombatAdapter、EncumbranceRuntimeService。
- 新增 `config/EquipLoadConfig.java`。
- 新增 `integration/epicfight/progression/EpicFightEncumbranceAdapter.java`、`EpicFightEncumbranceHooks.java`、`NightfallDodgeAdapter.java`。
- 新增上述6个 Mixin 与 common/client mixin JSON 注册。
- 修改 MaplesAdventure 初始化、ProgressionAttachments、ProgressionEvents、ProgressionCommands、PlayerAttributeService。
- 修改 AttributeSnapshot、ClientAttributeState、AttributePayloads、MessageNetwork 协议版本及属性同步/预览计算链。
- 修改 CharacterStatCalculator、CharacterStatsService、EquipLoadSnapshot 等负重描述层。
- 修改 CharacterStatsScreen、LevelUpScreen、LevelUpLayout、中英文本地化。
- 新增 EncumbranceMathTest；更新 DerivedRuntimeMathTest 等 Snapshot 构造测试。
- 测试启动支持 build.gradle 的隔离 runDirectory/用户名参数；`.research/Round4Rcon.ps1` 为本机隔离服检查工具。
- 新增本文档；保留测试目录与日志用于复查，没有删除用户存档。

## 14. 最终构建与产物

最后修改远端位移保护后执行：`gradlew.bat clean build --console=plain`。

结果：**BUILD SUCCESSFUL in 14s**，8 tasks executed，48 tests passed。仅两项原有 deprecation 警告（EncounterConfig/EncounterSpawnService）。

JAR：`E:/RPG Menu Framework/MaplesAdventure/build/libs/maplesadventure-0.2.0.jar`

大小：1,794,541 bytes。

SHA-256：`EA43FF01AF85AB42212164B73411D1CC53C977AB11A980A371A52E956AC30CD7`

已检查归档包含 Encumbrance、NightfallDodgeAdapter 和本轮 Mixin，未内嵌第三方 JAR。发布时客户端和服务器需同时更新。
