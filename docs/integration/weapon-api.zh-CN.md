# Weapon Integration API v1

> 语言：**简体中文** | [English](weapon-api.md)

内容 Mod 自行注册 Minecraft `Item`、攻击伤害/速度 Modifier、模型、贴图与动画。MaplesAdventure 读取物品堆栈 MAINHAND 攻击伤害 Modifier 作为基础攻击；没有第二套基础攻击 Registry。

五项武器属性（`STRENGTH`、`DEXTERITY`、`INTELLIGENCE`、`FAITH`、`ARCANE`）驱动需求和数值补正。静态定义见[公开数据包 Schema](datapack-api.zh-CN.md)。`MaplesWeaponApi.query(stack)` 返回经过当前质变解析的脱离状态视图；`evaluate(player, stack)` 根据当前服务端属性与配置求出需求缺口、逐通道名义攻击力以及 motion value 为 1 的状态积累。两者均须在 Registry 编译后的逻辑服务端线程调用。空结果表示上下文无效或没有可解析武器，不表示攻击力为零。客户端不因此获得权威。

九个通道是 PHYSICAL、SLASH、STRIKE、PIERCE、MAGIC、FIRE、LIGHTNING、ICE、HOLY；标准物理与斩击、打击、突刺分开。多通道武器仍在一次 Minecraft 伤害事件和一次 `hurt` 中结算；不得为元素再调用一次 `hurt`。名义攻击力不包含未达需求的惩罚或目标减伤。

各伤害 Component 的 `base_ratio` 以 Minecraft 基础攻击为基准，拥有独立解析后的补正系数。`.65` 物理加 `.35` 神圣分配的是基础攻击；属性补正和敌人防御可能使最终比例不同。状态积累与攻击力分离；Arcane 策略为 `NONE`、`EXPLICIT`、`FOLLOW_WEAPON_ARCANE`。查询视图暴露包括质变 ID、允许质变和可选状态重量类别在内的解析后 Profile。质变更改必须走已有授权玩法流程，不提供公开修改入口。

投射物组成在发射时冻结。属性升级、换装备、质变和数据包 Reload 不会追溯修改已飞出的投射物；新攻击使用最新定义。

[Holy Blade 示例数据包](examples/datapack/data/example/maplesadventure/) 将 `examplemod:holy_blade` 定义为 STR 12、FTH 24，物理 `.65` 与神圣 `.35`；物品本身仍须由 `examplemod` 注册。[WeaponIntegrationExample.java](examples/WeaponIntegrationExample.java) 使用公开类型并纳入示例编译任务。
