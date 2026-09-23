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

第一次合法右键或情境 **F** 交互只激活玩家自己的篝火引用。此后交互打开服务端授权的 `OPEN_STANDING` 会话。**休息**成功一次后写入 `lastRested`，恢复生命值及可用的可选魔力/精力，对玩家相位执行一次 Encounter Reset，再由服务端计时推进 `SITTING_DOWN → RESTING`。仅在已休息且该篝火启用 `LEVEL_UP` 时提供**升级**，沿用现有精确 XP 与服务端授权事务。**离开**经过 `STANDING_UP` 后关闭。当前没有 Flask、法术记忆、强化或传送玩法。

客户端动作包仅携带 nonce 与 `REST`、`LEVEL_UP`、`LEAVE`。服务端每次重查放置代数、维度、距离、生存/旁观、角色、Boss 战与敌对会话。SOLO/HOST 可使用篝火；COOPERATOR/INVADER 不得在外世界激活、休息或升级。动画完成信号不是 Gameplay 权威。

## 复活与 Encounter

正常死亡时，NeoForge 的 `PlayerRespawnPositionEvent` 验证原维度及精确代数，在附近寻找安全落脚点（水平半径至多三格，垂直偏移至多两格）。检查坚固地面、碰撞、流体和世界边界，不创建永久区块加载票据。篝火被拆除、替换或无安全位置时保持 Vanilla 原复活流程。Lost Soul 与外世界玩家的 Phantom Death 未改变。成功休息只调用一次 `EncounterResetService.resetForPlayer(..., BONFIRE)`；打开菜单、升级、离开都不会刷新 Encounter。

## 兼容与当前视觉限制

外部 Bonfires 的 `setLastRested` Hook 和升级来源继续作为可选兼容，不写入 `PlayerBonfireState`。两条入口共享按来源分派的 `BONFIRE` 升级验证器。未安装 Bonfires 或 Epic Fight 时，内置篝火玩法仍可运行。安装 Epic Fight 时，可选适配器注册三段原创 biped 动画（`bonfire_sit_down`、`bonfire_sit_idle`、`bonfire_stand_up`），并在服务端会话状态切换时播放。三段时长分别为 1.8 秒、4.0 秒循环、1.5 秒，纯属表现层；服务端计时与已验证的休息事务不依赖动画加载或播放完成。Blockbench 导入与端点结构检查已通过，但仍须在游戏内从多角度检查姿态、盔甲与手持物。
