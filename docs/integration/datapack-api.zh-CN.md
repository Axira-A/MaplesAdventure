# 数据包集成 Schema

> 语言：**简体中文** | [English](datapack-api.md)

以下字段对应当前发行的解析器。未知字段和超出边界的数值会拒绝对应定义，并在服务端记录日志。Reload 使用 Minecraft 数据包和编译后的查询表；每次命中不会解析 JSON。不要直接修改内部 Attachments/NBT。

## Entity defense profiles

路径：`data/<namespace>/maplesadventure/entity_defense_profiles/<path>.json`；ID：`<namespace>:<path>`。顶层可选 `channels`、`status_resistances`、`status_traits`。空 Profile 表示单位防御和默认状态规则；`maplesadventure:none` 为保留 ID。

`channels` 的键：physical、slash、strike、pierce、magic、fire、lightning、ice、holy。

| 字段 | 默认值 | 范围／意义 |
|---|---:|---|
| `defense` | 0 | 有限值 0–10000 |
| `absorption` | 0 | 有限值 -.50–.80；小数比例 |

`status_resistances` 的键：bleed、poison、scarlet_rot、frostbite、sleep、madness、death_blight。

| 字段 | 默认值 | 范围／意义 |
|---|---|---|
| `threshold` | 160 | 有限值 .001–100000 |
| `immune` | false | 布尔值 |
| `proc_damage_multiplier` | 1 | 有限值 0–10 |
| `correction` | maplesadventure:none | 命名空间 ID，最多 256 字符 |
| `response` | stagger_only | `stagger_only` / `deep_sleep` / `immune`，Sleep 行为 |

`status_traits` 仅接受布尔字段，默认均为 false：`tarnished_like`、`madness_immune`、`death_blight_immune`、`allow_death_blight`。实际免疫还取决于资格：普通非玩家 Mob 无 `tarnished_like` 时不能承受 Madness；Death Blight 需要 `tarnished_like` 或 `allow_death_blight`，被管理的 Boss PRIMARY 必须有 `allow_death_blight`。显式免疫 Trait 仍优先。玩家使用自己的抗性模型，而非这里的实体 Profile 覆盖。

内置修正 ID（均属 `maplesadventure`）：none、standard、resistant、boss。Standard 偏移为 21/49/91/189/479；resistant/boss 为 28/70/168/458/915。按 proc 次数**替换**偏移，而不是累加。未解析到的 correction ID 当前按 NONE 处理；拼写必须正确。

自定义修正路径：`data/<namespace>/maplesadventure/status_resistance_corrections/<path>.json`。

```json
{"stages":[21,49,91,189,479]}
```

只接受 `stages`：0–5 个有限、非负、非递减且不超过 10000 的值。NONE 为保留 ID。修正次数独立于可见状态条持久化，不要通过手改 Attachment 清零。

## Entity defense rules

路径：`data/<namespace>/maplesadventure/entity_defense_rules/<path>.json`。

```json
{"entity":"minecraft:zombie","profile":"example:boss","priority":0}
```

`entity` 与 `tag` 必须恰选一个；`profile` 必填；整数 `priority` 默认 0，范围 -1000000–1000000。ID 字符串最长 256。精确实体规则优先于标签规则；组内按 priority 降序、文件 ID 字典序。明确的逐实体 API 分配优先于这些规则。最多保留 4096 条 Profile/规则；未知 Profile 安全回退 NONE。

[示例数据包](examples/datapack/)有九通道／七状态 Profile 和一条 Vanilla Zombie 规则，不依赖演示 Mod 即可加载。它会有意修改所有被选中的 Zombie，**仅在测试世界安装**；正式使用应替换为自己的实体 ID/标签。

## Reload 与持久化边界

成功 Reload 在标签可用后更新编译后的防御和武器 Registry。新攻击使用当前定义；已发射武器投射物保留发射快照。实体 Profile 引用在存档与 Reload 后保留稳定 ID。不可把 enum ordinal 序列化为公开 ID。

## 武器与盔甲规则

以下均为公开 Schema v1，路径格式是 `data/<namespace>/maplesadventure/<directory>/<path>.json`。每个文件对未知字段和越界值单独拒绝。除 `weapon_infusions` 外，每条规则恰选 `item` 或 `tag`，可选整数 `priority`（默认 0），再加该领域字段。精确物品优先于标签；组内按 priority 降序，再按文件 ID 字典序。每目录最多 4096 条规则。成功 Reload 在标签可用后编译物品查询；命中时不解析 JSON 或扫描标签。新攻击和当前盔甲防御立即使用新规则；已飞投射物保留发射快照。

| 目录 | 字段与验证 |
|---|---|
| `weapon_requirements` | `requirements` 对象，strength/dexterity/intelligence/faith/arcane 为 0–99 整数；`priority` -10000..10000；`disabled` 布尔值阻止较低优先级、标签或集成 fallback。 |
| `weapon_scaling` | 同五键的 `scaling` 对象，有限系数 0–1.5；可选 `max_bonus` 有限值 0–2（默认 1.15）、`priority` -10000..10000、`disabled`。系数是数值，不是字母等级。 |
| `weapon_damage_profiles` | `components` 数组 1–8 个，同一 `channel` 唯一，`base_ratio` 为有限值 0–2，总和位于 (0,2]。Component 可用 `scaling` 对象覆盖继承的武器补正；`{}` 表示无补正。`max_bonus` 需要显式 `scaling`。`priority` -10000..10000；`disabled` 回退到自动原型伤害。 |
| `weapon_status_buildup` | 可选 `statuses` 对象，最多七状态键；可选 `weight_class` = throwing/normal/great/colossal；`priority` -1000000..1000000。每状态支持 `base_buildup` 0–1000、`arcane_scaling` 0–2、`arcane_policy` none/explicit/follow_weapon_arcane。Frostbite、Scarlet Rot、Death Blight 禁止 ARC 补正。此 Schema 没有 `disabled`；空 `statuses` 会覆盖标签规则。 |
| `weapon_infusion_eligibility` | 可选 `infusible` 布尔值（默认 true）、最多 32 个唯一质变 ID 的 `allowed` 数组、`priority` -10000..10000。`infusible:false` 只留下 normal；infusible 规则中的空 `allowed` 使用当前允许的非特殊定义。特殊 frenzied/rot/blight 需显式资格。未知 ID 在编译时丢弃。 |
| `armor_profiles` | `channels` 对象含九个有限非负 0–1000 值，和／或 `resistances` 对象含 immunity/robustness/focus/vitality 的 0–1000 值。`priority` -10000..10000；精确 `disabled:true` 阻止标签继承。四个装备槽每字段合计最多 4000；空且未禁用的 Profile 被拒绝。 |

伤害通道为 physical、slash、strike、pierce、magic、fire、lightning、ice、holy。`physical` 是标准物理，不等于 slash+strike+pierce。状态键为 bleed、poison、scarlet_rot、frostbite、sleep、madness、death_blight。同一次武器命中各通道分别计算防御压力，但游戏只执行一次最终伤害事件。

`weapon_infusions` 也属于公开 Schema v1。其**文件 ID 即质变 ID**，没有 item/tag selector 或 priority，最多 32 个定义。字段：`display` 翻译键（最多 128 字符）、`icon` 资源 ID（最多 256）、有限值 (0,2] 的 `base_multiplier`、由五项武器属性作为键的 `physical_scaling`（可选 `multiply` 0–2、`minimum`/`maximum` 0–1.5）、`element`（null 或包含 `channel`、`physical_ratio`、`element_ratio`、`scaling`、可选 `max_bonus` 的对象）以及采用上述状态 Component 语法的 `statuses`。旧包的 `future_buildup` 仍可读取但已弃用，新包应定义 `statuses`。自定义质变 ID 只有在定义成功加载且物品具有资格后才存在，不会自动应用到所有武器。

官方 [Holy Blade / Holy Knight 示例](examples/datapack/data/example/maplesadventure/) 使用上述解析器。示例 ID 需要 `examplemod` 注册物品才能在运行时解析。Spell-school 与 status-effect 定义仍是内部 Schema。
