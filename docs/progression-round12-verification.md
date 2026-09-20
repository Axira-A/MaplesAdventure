# Round 12 — Player Defense / Universal Typed Defense 验证报告

## 基线与范围

初始审计基线：本地与远端 main 均为 `70723b1468024889172e53922de6cde414`（Round 11）。续做期间用户已将主体改动提交为 `e143b40`；保留该提交，仅追加收尾修复与报告。未修改第三方 JAR、原版 Armor、法术分类、武器强化或 Phase 关系规则。本轮没有新增 Mixin。

## 1–8：架构、公式与 FIRE 修复

1. Round 11 的 Bleed、Poison、Scarlet Rot、Frostbite 与 Round 10 Enemy Defense 专服夹具均重跑。最初 DOT 夹具使用可坠落/晒伤的 Zombie，延迟生命断言失败；仅修复测试环境（禁止重力、装备头盔），随后通过，不改变生产 DOT 数学。
2. `CombatHitLifecycle` 在 Pre 保存原 `WeaponHitContext`，Post 按目标 UUID + DamageSource 对象身份消费，支持同源嵌套命中。空上下文也压栈，避免嵌套非武器攻击取走外层上下文。最多 1024 条，tick 结束及停服清空。Post 不再 resolve 当前手持武器。正生命伤害且 IS_FIRE，或原 Bundle 的 FIRE AR>0 且 share>0，才解除 ACTIVE Frost；未 Proc 积累不清除。
3. `TargetDefenseResolver`：ServerPlayer 使用 Player Build；其他 LivingEntity 继续 EntityDefenseService。没有给 Player 挂敌人 Profile，管理员敌人 Profile 指派仍拒绝 Player。
4. `ChannelDefenseView` 提供 `channel(type)`、`source()`；Enemy Profile 和 Player Snapshot 实现同一只读契约。
5. `PlayerDefenseSnapshot` 保存九通道不可变 StatBreakdown。Absorption 全部 0。通过现有 ChannelDefense 校验 finite / 非负 / 安全上限，而不是把 Defense 当百分比截到 100。
6. 保留唯一纯函数 `stats.DefenseCalculator`，runtime、Character Stats、Level-Up Preview 共用。以下变量均为 `max(0, attribute-5)`：

| Channel | Defense |
|---|---|
| PHYSICAL | 10 + .25 VIG + .15 STR + .10 END |
| STRIKE | 10 + .30 STR + .10 END + .05 VIG |
| SLASH | 10 + .25 DEX + .10 VIG |
| PIERCE | 10 + .30 DEX + .05 STR |
| MAGIC | 10 + .35 INT + .05 MND |
| FIRE | 10 + .25 FTH + .10 VIG |
| LIGHTNING | 10 + .25 INT + .10 DEX |
| ICE | 10 + .25 INT + .10 END |
| HOLY | 10 + .35 FTH |

7. 独立 SERVER 配置 `maplesadventure-player-defense-server.toml`：enabled=true，pressure=.10，范围 .01–1。关闭时 resolver 返回零防御；纯 Build 预览仍给出设计数值。按命中读取最新属性，无每 tick Defense 网络包。
8. Enemy 继续独立 .35；未将玩家较高 Build Defense 与低武器 AR 的量级直接套进 Enemy 平衡。共享 DefenseMitigationCurve，只扩展参数合法下界至 .01，不复制公式。

## 9–18：校准、伤害顺序、有限类型路由

9. [50 武器校准](progression-round12-calibration.md) 与 [750 行 CSV](progression-round12-calibration.csv) 复用 Round 7 已测 AR，明确不是重新实测所有物品。D10 对 AR4/7/10 额外减伤为 20%/12.5%/9.09%。D50 对 AR4 为 55.56%，对 AR30 为 14.29%。**Bow/Crossbow 的旧基准 AR≈1 是明确平衡缺口**：初始对 D10 已有 50% 额外减伤。没有为了掩盖它修改 Round 7 AR 或压力来源。
10. Generic Power 2/4/6/10/20/40 网格在同一报告。Power2、D50 为 71.43% 减伤，不宣称所有组合都达成 20–35% 的目标；这是低伤害对高 Build 的极端结果。保留用户指定 .10，并公开此边界。
11. 实际顺序：Vanilla Armor/Toughness → effects/enchantment → LivingDamageEvent.Pre → absorption hearts → HP。
12. Player Build Calculator 不读取装备/ARMOR/TOUGHNESS；真实专服 20 点木剑输入：无甲 Pre20→16，铁甲 Pre16→12.8，钻石甲 Pre8→6.4，三者 Build Defense 均为 10。
13. Pre 内只有一处 Maples setNewDamage：原输入 ×武器 nominal → 按 channel 分摊 → 对应目标防御 → 合并 → requirement → Frost vulnerability。只执行一次 hurt 管线，不拆成多次伤害。Armor 已在前面处理整次 DamageSource；本轮不是完整魂系 Armor 替代模型。
14. `TypedIncomingDamageContext` 保存不可变 slices(channel/share/attackPower)、总压力、来源与调试原因、可选原 WeaponHitContext。最多九通道；finite、范围、重复通道、share 总和与压力一致性均验证。Provider 注册最多64，显式 priority/id 排序，异常 provider 跳过并仅警告一次。
15. Weapon Context 优先，其次显式 Integration，最后 exact Vanilla。`MOB_ATTACK`/`MOB_ATTACK_NO_AGGRO` 且 direct LivingEntity==owner，读取主手 resolved damage profile 的 baseRatio，仅用于分类，不给 Mob 套玩家 scaling/requirement；无可识别武器为 PHYSICAL。
16. 无冻结 Bundle 的 AbstractArrow + ARROW、ThrownTrident + TRIDENT 为 PIERCE；Power 为当次 Pre damage，不称武器 AR。有 Launch Snapshot 时始终优先原 Bundle。
17. 精确 FIREBALL + Fireball + Living owner 为 FIRE，FREEZE 为 ICE；Lava/onFire/fall/未知 magic 保持 NONE。`SPELL_TYPED_DEFENSE_NOT_YET_CONNECTED`：Iron’s 法术未按名字/颜色/宽泛 MAGIC 标签猜类型。Generic 路由本轮只应用于玩家目标，非武器 Enemy 行为保持旧版。
18. 1000 组随机 Bundle/Profile/pressure 的旧 Round 10 参考算法与新共享算法 double bits 一致；中性 profile 保留原乘法结合顺序。专服 Enemy Defense 夹具通过。

## 19–28：UI、状态与真实客户端

19. 九系 Character Stats 使用 ACTIVE，保留 base/attribute/equipment/other 明细；装备部分为0。中英 Tooltip 明确说明 Build Defense 与原版护甲分层。
20. Level-Up Draft 仍为纯 Preview，复用同一 Calculator，不写真实属性；compact preview 跳过未变化防御，全详情保留全部通道。
21. ResistanceCalculator 直接消费服务器权威 threshold map，在 Snapshot 构造时产生 ACTIVE；删除 `withStatusThresholds` 的事后覆盖。没有权威数据时保留 unavailable，不编造阈值。
22. 专服合成 ServerPlayer 测试：跨 Phase hit 在前层拒绝，不进入 Defense/Status；普通合法 PvP 使用 PLAYER_BUILD。NeoForge FakePlayer 本身无敌且禁 PvP，因此夹具使用真实 ServerPlayer 类 + no-op 网络连接；这不冒充在线双客户端。
23. **2026-09-16 两个真实客户端 StatusTester / DefensePeer 同服**：正式 Coop 通过现有 debugstart 创建，双向 4 点 player_attack 都拒绝，HP均20；解散恢复异 Phase。Duel 由 DefensePeer 的 L 菜单创建红符，用户操作火主按 F 召唤，服务器确认 ACTIVE 正式会话。双向 4 点命中均为 PLAYER_BUILD / pressure .10 / Defense10 / final3.2，HP均20→16.8。伤害输入为服务器 `/damage ... by ...`，不是宣称已完成手动连招体验测试。
24. 专服测试验证 physical 不解冻、FIRE Weapon 解冻、Pre 后换物品仍消费原 FIRE Context、FIRE Launch Snapshot 发射后换手仍解冻、Frost vulnerability 只乘一次、FIRE 不清未 Proc buildup。
25. Status 自有 DamageTypes 在 typed resolver 前排除，Bleed 对 MaxHP1000 实测150且不生成武器 LastHit；Poison/Rot 延迟 DOT、去重、死亡及 Frost 原回归继续通过。新增 SESSION_RETURN 清理：Coop 与 Invasion/Duel 共用的现有返回入口清掉持续异常和积累，Pending Return 恢复夹具也验证属性不回滚、Pending 消费、SOLO恢复。**此最后清理修复通过专服夹具；未再次完成双真人异常 Proc→退出→下一 Session 的端到端人工复测。**
26. Wire 未变，Protocol 保持18；没有新增 Defense 网络字段。
27. 用户完成 F3+T，客户端日志 2026-09-16 17:29:25 显示 Reloaded resource packs / Reloading ResourceManager。随后检查四颜色、图标、50%填充，无黑纹理/Missing Texture。[重载后截图](assets/round12/status-hud-after-f3t.png)。代码审计确认 reload 先 release 旧动态纹理再替换四项；没有据此宣称做过 GPU 内存长期泄漏压力测试。
28. `.gitignore` 增加 logs/，四个已追踪日志仅 `git rm --cached`，未删除本地日志。历史 docs 与 round11 图片保留。

## 已知环境与人工验收限制

- 新客户端默认 L 与 Vanilla Advancements 冲突，用户已在测试客户端解除进度键绑定；未擅自修改本轮之外的 Hub 代码。另见菜单旧 `gui.close` 未翻译文本，未改动。
- 双客户端切窗过程中出现两次 `glfw.dll` 原生 EXCEPTION_ACCESS_VIOLATION；保存 hs_err 报告，重启后才继续。未证明由本轮 Java 代码造成，也未把崩溃写成通过。
- 正式随机 Invasion 三人匹配、鼠标实际连招、异常 HUD Proc 提示与完整返回后的第二次真人 Session，仍为发布回归项。已有两客户端正式 Duel 不等于全套随机入侵人工验收。
- 三环境专服原先均通过；收尾追加返回清理后再次运行三环境。详细日志在本地 `.research/round12-*.log`，不作为运行日志提交。

## 文件清单（相对 src/main/java/dev/maplesadventure）

新增：`config/PlayerDefenseConfig`；`progression/defense/{ChannelDefenseView,CombatHitLifecycle,PlayerDefenseSnapshot,PlayerDefenseService,TargetDefenseResolver,TypedIncomingDamageContext,TypedDamageProviderRegistry}`。

修改：`MaplesAdventure` 注册配置；`progression/defense/{EntityDefenseProfile,DefenseMitigationCurve,EntityDefenseCommands,LastWeaponDamageResolution,TypedDamageChannelProvider}`；`progression/weapon/WeaponCombatResolutionService`；`progression/status/{CombatDamageFinalizationService,StatusEvents,StatusRuntimeService}`；`progression/stats/{DefenseCalculator,CharacterStatCalculator,CharacterStatsSnapshot,CharacterStatsService,ResistanceCalculator,StatPreviewPriority}`；`progression/client/{CharacterStatsScreen,LevelUpScreen}`；`multiplayer/coop/CoopSessionManager` 和 `multiplayer/invasion/InvasionSessionManager` 各仅在现有返回入口增加状态清理。

另有 en_us/zh_cn Tooltip、四个 PlayerDefense/TypedIncoming 单测类、既有 CharacterStatsCalculatorTest 断言更新、开发专用 PlayerDefenseRegression 及其注册、StatusRegression 环境加固、测试用金剑 FIRE profile、校准与本报告/截图、.gitignore/日志索引清理。开发夹具与测试 profile 不进入生产 JAR。

## 29–32：最终构建记录

待最终 clean build 后补入测试总数、专服收尾结果与 JAR 校验信息。
