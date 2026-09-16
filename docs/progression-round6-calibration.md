# Round 6 — 50 件实际武器校准

2026-09-06；最终 clean build 对应代码，隔离专服真实注册表 `/ma weapon audit`。环境：MC 1.21.1 / NeoForge 21.1.219 / Epic Fight 21.17.3.1 / Nightfall 3.4.0 (`efn`) / Invincible 21.15.8.2 / Iron’s 3.16.3。

需求顺序是 STR/DEX/INT/FTH/ARC。Weight 为默认主手 WEIGHT 修饰符的净贡献，不是玩家身体重量；这些样本都没有声明该修饰符，0 不代表按名称猜测的轻重。不同重量的同类武器由纯数学 7/13 单元测试验证。弓弩的 melee damage/speed 仍按原值列出，但不用于推导投射武器需求。

当前 JAR 中没有找到可可靠分类为 GREATAXE 的实际样本；不虚构测试物品。Nightfall 的未知 `EFN_YAMATO` 类别走 GENERIC 基线，建议地图包显式覆盖。Iron’s 这些武器没有明确的默认主手 School Power 属性，故不会因名称中 fire/blood/holy 自动添加魔法需求；School profile affinity 的正向路径通过构造明确属性语义的数学测试验证。

| Item ID | Category | Weight | Base damage | Attack speed | STR/DEX/INT/FTH/ARC | Source |
|---|---|---:|---:|---:|---|---|
| minecraft:wooden_sword | SWORD | 0.0 | 4.0 | 1.60 | 5/5/0/0/0 | EPIC_FIGHT |
| minecraft:stone_sword | SWORD | 0.0 | 5.0 | 1.60 | 5/5/0/0/0 | EPIC_FIGHT |
| minecraft:iron_sword | SWORD | 0.0 | 6.0 | 1.60 | 8/7/0/0/0 | EPIC_FIGHT |
| minecraft:diamond_sword | SWORD | 0.0 | 7.0 | 1.60 | 9/7/0/0/0 | EPIC_FIGHT |
| minecraft:netherite_sword | SWORD | 0.0 | 8.0 | 1.60 | 9/7/0/0/0 | EPIC_FIGHT |
| minecraft:wooden_axe | AXE | 0.0 | 7.0 | 0.80 | 14/7/0/0/0 | VANILLA |
| minecraft:iron_axe | AXE | 0.0 | 9.0 | 0.90 | 14/8/0/0/0 | VANILLA |
| minecraft:netherite_axe | AXE | 0.0 | 10.0 | 1.00 | 15/8/0/0/0 | VANILLA |
| minecraft:bow | BOW | 0.0 | 1.0 | 4.00 | 5/12/0/0/0 | VANILLA |
| minecraft:crossbow | CROSSBOW | 0.0 | 1.0 | 4.00 | 9/10/0/0/0 | VANILLA |
| minecraft:trident | TRIDENT | 0.0 | 9.0 | 1.10 | 12/12/0/0/0 | EPIC_FIGHT |
| minecraft:mace | MACE | 0.0 | 6.0 | 0.60 | 18/7/0/0/0 | VANILLA |
| minecraft:wooden_pickaxe | TOOL | 0.0 | 2.0 | 1.20 | 5/5/0/0/0 | VANILLA |
| epicfight:bokken | SWORD | 0.0 | 4.0 | 1.60 | 5/5/0/0/0 | EPIC_FIGHT |
| epicfight:iron_dagger | DAGGER | 0.0 | 4.0 | 2.40 | 5/10/0/0/0 | EPIC_FIGHT |
| epicfight:diamond_dagger | DAGGER | 0.0 | 5.0 | 2.40 | 5/10/0/0/0 | EPIC_FIGHT |
| epicfight:netherite_dagger | DAGGER | 0.0 | 6.0 | 2.40 | 5/10/0/0/0 | EPIC_FIGHT |
| epicfight:wooden_greatsword | GREATSWORD | 0.0 | 12.0 | 1.15 | 21/10/0/0/0 | EPIC_FIGHT |
| epicfight:iron_greatsword | GREATSWORD | 0.0 | 14.0 | 1.05 | 23/11/0/0/0 | EPIC_FIGHT |
| epicfight:diamond_greatsword | GREATSWORD | 0.0 | 15.0 | 1.00 | 23/11/0/0/0 | EPIC_FIGHT |
| epicfight:netherite_greatsword | GREATSWORD | 0.0 | 16.0 | 0.95 | 24/11/0/0/0 | EPIC_FIGHT |
| epicfight:iron_longsword | LONGSWORD | 0.0 | 7.0 | 1.20 | 12/8/0/0/0 | EPIC_FIGHT |
| epicfight:diamond_longsword | LONGSWORD | 0.0 | 8.0 | 1.20 | 12/8/0/0/0 | EPIC_FIGHT |
| epicfight:netherite_longsword | LONGSWORD | 0.0 | 9.0 | 1.20 | 13/9/0/0/0 | EPIC_FIGHT |
| epicfight:iron_spear | SPEAR | 0.0 | 6.0 | 1.20 | 10/11/0/0/0 | EPIC_FIGHT |
| epicfight:diamond_spear | SPEAR | 0.0 | 7.0 | 1.20 | 11/11/0/0/0 | EPIC_FIGHT |
| epicfight:netherite_spear | SPEAR | 0.0 | 8.0 | 1.20 | 11/11/0/0/0 | EPIC_FIGHT |
| epicfight:iron_tachi | TACHI | 0.0 | 7.0 | 1.20 | 12/13/0/0/0 | EPIC_FIGHT |
| epicfight:diamond_tachi | TACHI | 0.0 | 8.0 | 1.20 | 12/13/0/0/0 | EPIC_FIGHT |
| epicfight:uchigatana | UCHIGATANA | 0.0 | 7.0 | 2.00 | 9/14/0/0/0 | EPIC_FIGHT |
| epicfight:glove | FIST | 0.0 | 3.0 | 4.00 | 5/11/0/0/0 | EPIC_FIGHT |
| efn:air_tachi | TACHI | 0.0 | 10.0 | 1.20 | 14/14/0/0/0 | EPIC_FIGHT |
| efn:broadblade | LONGSWORD | 0.0 | 10.0 | 1.20 | 14/9/0/0/0 | EPIC_FIGHT |
| efn:crescent_moon | LONGSWORD | 0.0 | 12.0 | 1.00 | 15/9/0/0/0 | EPIC_FIGHT |
| efn:excalibur | SWORD | 0.0 | 11.0 | 1.20 | 13/8/0/0/0 | EPIC_FIGHT |
| efn:hf_blade | TACHI | 0.0 | 14.0 | 1.20 | 17/15/0/0/0 | EPIC_FIGHT |
| efn:kusabimaru | UCHIGATANA | 0.0 | 10.0 | 1.20 | 12/14/0/0/0 | EPIC_FIGHT |
| efn:meen_spear | SPEAR | 0.0 | 11.0 | 1.00 | 14/12/0/0/0 | EPIC_FIGHT |
| efn:nf_claw | FIST | 0.0 | 9.0 | 1.00 | 8/9/0/0/0 | EPIC_FIGHT |
| efn:ruinsgreatsword | GREATSWORD | 0.0 | 16.0 | 1.00 | 24/11/0/0/0 | EPIC_FIGHT |
| efn:thornwheel | GREATSWORD | 0.0 | 14.0 | 1.00 | 23/11/0/0/0 | EPIC_FIGHT |
| irons_spellbooks:amethyst_rapier | SWORD | 0.0 | 8.0 | 2.30 | 9/8/0/0/0 | VANILLA |
| irons_spellbooks:boreal_blade | SWORD | 0.0 | 16.0 | 0.90 | 16/9/0/0/0 | VANILLA |
| irons_spellbooks:claymore | SWORD | 0.0 | 10.0 | 1.30 | 12/8/0/0/0 | VANILLA |
| irons_spellbooks:decrepit_scythe | SWORD | 0.0 | 11.0 | 1.40 | 12/8/0/0/0 | VANILLA |
| irons_spellbooks:hellrazor | SWORD | 0.0 | 13.0 | 1.40 | 13/9/0/0/0 | VANILLA |
| irons_spellbooks:magehunter | SWORD | 0.0 | 7.0 | 1.60 | 9/7/0/0/0 | VANILLA |
| irons_spellbooks:spellbreaker | SWORD | 0.0 | 10.0 | 1.80 | 11/8/0/0/0 | VANILLA |
| irons_spellbooks:twilight_gale | SWORD | 0.0 | 13.0 | 1.40 | 13/9/0/0/0 | VANILLA |
| irons_spellbooks:autoloader_crossbow | CROSSBOW | 0.0 | 1.0 | 4.00 | 9/10/0/0/0 | VANILLA |

校准修复：

- Epic Fight `CapabilityItem` 未声明 category 时默认 FIST，不能据此把盔甲当拳套；现在要求 WeaponCapability，排除盔甲等非武器，标准武器的默认 FIST 回退 Java 类型。
- 未声明 WEIGHT 时贡献为 0，不再无根据地减 2 STR。
- 弓弩不使用徒手默认 1 damage / 4 speed 推导速度加成。
- Mace 正确回退 MACE，18 STR / 7 DEX，不再误归 FIST。
- 木剑、石剑、木镐、部分基础工具及 bokken 可由初始 5/5 属性使用。高级大剑约 23–24 STR，未出现自动 40+。

