# Armor Integration API v1

> 语言：**简体中文** | [English](armor-api.md)

MaplesAdventure 盔甲 Profile 向九个伤害防御通道和四类状态抗性提供固定装备值。Minecraft 原版护甲点数、韧性、耐久、附魔及减伤仍独立生效。Profile 不提供百分比吸收、状态免疫或属性补正。没有 Profile 的钻石／下界合金胸甲贡献**零 Maples 装备防御**，但保留原版护甲行为。

通道为 `physical`、`slash`、`strike`、`pierce`、`magic`、`fire`、`lightning`、`ice`、`holy`；标准物理不自动提高其他三种物理通道。抗性类为 `immunity`（Poison、Scarlet Rot）、`robustness`（Bleed、Frostbite）、`focus`（Sleep、Madness）、`vitality`（Death Blight）。这些数值加入玩家现有 breakdown 的 `equipment` 部分，表示防御压力和状态阈值，而非百分比；它们可与 Vanilla 护甲减伤共存。

静态物品／标签规则位于 `data/<namespace>/maplesadventure/armor_profiles/`。只有实际装备在 HEAD、CHEST、LEGS、FEET 的物品计入；自定义可穿戴物品不必继承 `ArmorItem`。主手、副手、背包和 Curios 不计入。精确物品规则优先于标签；组内先按 priority 降序，再按资源 ID 字典序。精确 `disabled:true` 可阻止标签 Profile 继承。Reload 会重编译查询并更新在线玩家快照；不会把盔甲数据写入玩家持久状态。

`MaplesArmorApi.query(stack)` 返回明确规则的脱离状态的可选 Profile；`MaplesArmorApi.equipped(player)` 返回四槽合计与带 Profile 的槽位。两者均为逻辑服务端线程只读调用。客户端 Character Stats 与 Level-Up Preview 消费服务端有界装备快照。`MaplesDefenseApi.query(player)` 自然包含这些装备值。没有公开的盔甲修改 API。

[Holy Knight chestplate 示例](examples/datapack/data/example/maplesadventure/armor_profiles/holy_knight_chestplate.json) 定义 holy +18、fire +9、robustness +12、vitality +10 等数值；`examplemod` 必须注册可穿戴物品。[ArmorIntegrationExample.java](examples/ArmorIntegrationExample.java) 演示公开查询并接受编译检查。
