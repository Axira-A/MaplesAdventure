# 内置篝火核心

> 语言：**简体中文** | [English](bonfire.md)

MaplesAdventure 自带 `maplesadventure:bonfire`，不依赖外部 Bonfires Mod。方块与物品模型暂时引用 Vanilla Minecraft 的营火模型，**仅作占位**；没有使用 Bonfires 的模型、贴图或 Spiral Sword 资产。正式美术另行制作。

## 地图作者配置

使用 `/give @s maplesadventure:bonfire` 取得方块并放置。权限等级 2 的管理员可配置已加载篝火：

```text
/ma bonfire info <x> <y> <z>
/ma bonfire name <x> <y> <z> <name...>
/ma bonfire feature <x> <y> <z> level_up true
/ma bonfire feature <x> <y> <z> level_up false
/ma bonfire resetplayer <player>
```

`/maplesadventure` 是 `/ma` 的别名。明确坐标使函数与命令方块无需玩家执行者也能切换功能。名称为最多 64 字符的纯文本。新放置篝火**默认无功能标记**；休息属于核心能力，不是标记。`LEVEL_UP`、`WARP`、`FLASK_ALLOCATION`、`SPELL_MEMORY`、`REINFORCE` 均有稳定存储 ID，但目前只有 `LEVEL_UP` 有实际动作；其他功能不会在玩家菜单显示。

## 逐玩家状态

每次放置拥有 `BonfireRef = dimension + BlockPos + generation UUID`。方块实体只保存地图作者共享的名称、功能和放置代数。`PlayerBonfireState` 是带版本号、死亡时复制的玩家 Attachment，最多保存 4096 个已激活引用及可选的最后休息点/朝向。不存在世界统一的 `LIT` 状态。在同坐标重新放置会产生新代数，旧激活和复活记录不会错误绑定新方块。

第一次合法右键或情境 **F** 交互只激活玩家自己的篝火引用，可选播放 `bonfire_activate`，不打开菜单。提示先显示**激活篝火**，激活后显示**在篝火处休息**。登录、重生、激活及管理员重置时仅向本人同步有界的进度快照，包含放置代数，不使用共享点亮标记。F 与右键都消耗交互，但不触发原版挥手。

再次使用已激活篝火直接进入 `SITTING_DOWN`，不再出现“休息”按钮。先开始坐下动作，稍后客户端才开始黑色淡出/淡入。安装 Epic Fight 时第 14 tick（未安装时第 10 tick），服务端重新验证会话并在全黑阶段只提交一次休息：更新 `lastRested`、恢复生命值及可用魔力/精力，并重置所属相位的 Encounter。全黑停留至第 20 tick（未安装时第 16 tick），比此前延长 0.2 秒；第 28 tick（未安装时第 24 tick）画面恢复清晰，可看到坐下的后半段；第 43 tick（未安装时第 24 tick）进入 `RESTING`，左侧菜单用 320 ms 淡入。只有该篝火启用 `LEVEL_UP` 才显示**升级**。选择**离开**立即禁用菜单，在起身同时用 320 ms 淡出；服务端完成 33 tick 起身过渡（未安装时 8 tick）才解除移动锁。动画过渡计时包含片段时长及额外的 0.12 秒混合时间。当前没有 Flask、法术记忆、强化或传送玩法。

客户端动作包仅携带 nonce 与 `SELECT_FEATURE + featureId` 或 `LEAVE`；客户端不能决定何时休息成功。每次动作和服务端计时均重查放置代数、维度、距离、生存/旁观、角色、Boss 战与敌对会话；执行功能还要重查注册、当前配置和可用性。SOLO/HOST 可使用篝火；COOPERATOR/INVADER 不得在外世界激活、休息或升级。动画及淡入淡出完成信号都不是 Gameplay 权威。

功能现以最多 64 个完整命名空间 ID 持久化，每个最多 128 字符。旧简写迁移至 `maplesadventure:`；暂时卸载的 addon 的合法 ID 仍保留。服务端按“已配置、已注册、可用”筛选菜单，以 order、ID 字典序排序。升级已有处理器；Flask/法术记忆/强化/传送仅预留，未实现时不显示。新增菜单行复用原有样式与淡入淡出，见[篝火 API](integration/bonfire-api.zh-CN.md)。

复活时确认篝火删除或被新代数替换才清除 lastRested；临时堵住安全位置则保留并仅本次回退 Vanilla，边界检查覆盖玩家站立包围盒。Rest 通过 BonfirePhaseResetService 的内置 encounters participant 调用原 EncounterResetService 一次，不影响其他 Phase、共享 Mob 或 Debug prototype。

激活、坐下、休息、起身期间由篝火持有玩家姿态。拦截移动输入、服务端移动包、实体位移/推动、伤害与击退，不修改 `noPhysics` 或永久无敌标记。外部明确传送会使会话失效，而不是把玩家强拉回去。离开时不能因视觉菜单提前关闭而提前解除移动锁。

## 复活与 Encounter

正常死亡时，NeoForge 的 `PlayerRespawnPositionEvent` 验证原维度及精确代数，在附近寻找安全落脚点（水平半径至多三格，垂直偏移至多两格）。检查坚固地面、碰撞、流体和世界边界，不创建永久区块加载票据。篝火被拆除、替换或无安全位置时保持 Vanilla 原复活流程。Lost Soul 与外世界玩家的 Phantom Death 未改变。成功休息只调用一次 `EncounterResetService.resetForPlayer(..., BONFIRE)`；打开菜单、升级、离开都不会刷新 Encounter。

## 兼容与当前视觉限制

外部 Bonfires 的 `setLastRested` Hook 和升级来源继续作为可选兼容，不写入 `PlayerBonfireState`。两条入口共享按来源分派的 `BONFIRE` 升级验证器。未安装 Bonfires 或 Epic Fight 时，内置篝火玩法仍可运行。安装 Epic Fight 时，可选适配器注册四段原创 biped 动画：`bonfire_activate`（1.8 秒）、`bonfire_sit_down`（2.0 秒）、`bonfire_sit_idle`（4.0 秒循环）、`bonfire_stand_up`（1.5 秒）。使用 Epic Fight 主动作与状态 API 阻止站姿/行走覆盖休息动作，不应用 Root Motion。四段动作均使用用户现有 20 骨 Epic Fight Blender 玩家骨架的独立副本制作；手脚 IK 控制器烘焙到原骨骼，没有替换原模板或玩家网格。保留绑定姿态的前后修正使膝盖与前臂朝前。控制点采用单调三次插值，不在每个中间姿势点停顿；以 60 Hz 烘焙，保留旋转及首尾衔接。自动检查覆盖采样、Root 位移、单位缩放与 IK 突变；游戏内盔甲、手持物与第三人称姿态仍需实机验收。

休息菜单将 `renderBackground` 覆写为空并直接绘制条目，避免 Minecraft 1.21.1 的普通 `Screen.render` 隐式触发模糊。世界保持清晰，左侧仅用从左向右渐变至完全透明的背景衬托文字。

Shoulder Surfing 5 可选扩展仅在坐下开始时已选中越肩视角才启用。每次休息在全黑期间选取一次安全机位：到篝火的水平投影距离 2 格，俯角 20°，初始对准篝火基座上方 0.65 格的中心。以篝火到玩家的方向为 0°，仅选 45–135° 和 225–315° 两侧扇区，排除玩家侧及正对面各 90°。鼠标两轴均使用反向屏幕平面视差。机位与黑屏共用逐帧时钟，不允许在可见淡入/淡出期间切换，包括掉帧后迟到的切换。离开时位置与朝向用 650 ms 平滑回归正常越肩视角（无 Epic Fight 的较短起身流程为 350 ms）。不修改玩家位置、朝向、视角模式或保存的配置。最多尝试 32 个侧面候选并用带边距的方块射线防穿墙；两侧都无安全机位则保持普通越肩视角。新增一个窄范围客户端 `Camera.setup` 返回 Hook，在 Shoulder Surfing 完成设置后应用可选 Provider；原 NeoForge 角度事件发生更早，会被其旋转设置覆盖。没有 Shoulder Surfing 时 Provider 不做任何操作。手动切换视角、换世界或关闭会话会释放效果。
