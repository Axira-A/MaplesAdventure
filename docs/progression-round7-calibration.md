# Round 7 — 50 件实际武器 Scaling / AR 校准

2026-09-06，隔离专服实际 `/ma weapon scaling <item>` 输出；同 Round 6 的 50 件物品。Epic Fight 21.17.3.1 / Nightfall 3.4.0 / Invincible 21.15.8.2 / Iron’s 3.16.3。

需求顺序 STR/DEX/INT/FTH/ARC。AR 列把五项进攻属性统一设置为列头值，不含需求惩罚。自动 Profile 的 INT/FTH/ARC=0、maxBonus=1.15，没有用于校准的 Exact override。

弓/弩 Base=1 是物品 ATTACK_DAMAGE 标准基线，不是箭的基础命中伤害；箭速、蓄力与实际投射伤害仍由原版决定。未虚构实际 GREATAXE/SCYTHE capability 样本；Iron’s scythe 的真实 Java 类型走 SWORD，不按名字改变分类。

| Item | Category | Base | Requirements | STR coeff/grade | DEX coeff/grade | AR5 | AR20 | AR40 | AR60 | AR99 |
|---|---|---:|---|---|---|---:|---:|---:|---:|---:|
| minecraft:wooden_sword | SWORD | 4.0 | 5/5/0/0/0 | 0.35/D | 0.35/D | 4.0000 | 4.8442 | 5.6884 | 6.2513 | 6.8000 |
| minecraft:stone_sword | SWORD | 5.0 | 5/5/0/0/0 | 0.35/D | 0.35/D | 5.0000 | 6.0553 | 7.1106 | 7.8141 | 8.5000 |
| minecraft:iron_sword | SWORD | 6.0 | 8/7/0/0/0 | 0.35/D | 0.35/D | 6.0000 | 7.2663 | 8.5327 | 9.3769 | 10.2000 |
| minecraft:diamond_sword | SWORD | 7.0 | 9/7/0/0/0 | 0.35/D | 0.35/D | 7.0000 | 8.4774 | 9.9548 | 10.9397 | 11.9000 |
| minecraft:netherite_sword | SWORD | 8.0 | 9/7/0/0/0 | 0.35/D | 0.35/D | 8.0000 | 9.6884 | 11.3769 | 12.5025 | 13.6000 |
| minecraft:wooden_axe | AXE | 7.0 | 14/7/0/0/0 | 0.65/B | 0.1/E | 7.0000 | 8.5829 | 10.1658 | 11.2211 | 12.2500 |
| minecraft:iron_axe | AXE | 9.0 | 14/8/0/0/0 | 0.65/B | 0.1/E | 9.0000 | 11.0352 | 13.0704 | 14.4271 | 15.7500 |
| minecraft:netherite_axe | AXE | 10.0 | 15/8/0/0/0 | 0.65/B | 0.1/E | 10.0000 | 12.2613 | 14.5226 | 16.0302 | 17.5000 |
| minecraft:bow | BOW | 1.0 | 5/12/0/0/0 | 0.1/E | 0.7/A | 1.0000 | 1.2412 | 1.4824 | 1.6432 | 1.8000 |
| minecraft:crossbow | CROSSBOW | 1.0 | 9/10/0/0/0 | 0.25/D | 0.45/C | 1.0000 | 1.2111 | 1.4221 | 1.5628 | 1.7000 |
| minecraft:trident | TRIDENT | 9.0 | 12/12/0/0/0 | 0.3/D | 0.55/B | 9.0000 | 11.3065 | 13.6131 | 15.1508 | 16.6500 |
| minecraft:mace | MACE | 6.0 | 18/7/0/0/0 | 0.8/A | 0.0/NONE | 6.0000 | 7.4472 | 8.8945 | 9.8593 | 10.8000 |
| minecraft:wooden_pickaxe | TOOL | 2.0 | 5/5/0/0/0 | 0.15/E | 0.05/NONE | 2.0000 | 2.1206 | 2.2412 | 2.3216 | 2.4000 |
| epicfight:bokken | SWORD | 4.0 | 5/5/0/0/0 | 0.35/D | 0.35/D | 4.0000 | 4.8442 | 5.6884 | 6.2513 | 6.8000 |
| epicfight:iron_dagger | DAGGER | 4.0 | 5/10/0/0/0 | 0.1/E | 0.65/B | 4.0000 | 4.9045 | 5.8090 | 6.4121 | 7.0000 |
| epicfight:diamond_dagger | DAGGER | 5.0 | 5/10/0/0/0 | 0.1/E | 0.65/B | 5.0000 | 6.1307 | 7.2613 | 8.0151 | 8.7500 |
| epicfight:netherite_dagger | DAGGER | 6.0 | 5/10/0/0/0 | 0.1/E | 0.65/B | 6.0000 | 7.3568 | 8.7136 | 9.6181 | 10.5000 |
| epicfight:wooden_greatsword | GREATSWORD | 12.0 | 21/10/0/0/0 | 0.75/A | 0.15/E | 12.0000 | 15.2563 | 18.5126 | 20.6834 | 22.8000 |
| epicfight:iron_greatsword | GREATSWORD | 14.0 | 23/11/0/0/0 | 0.75/A | 0.15/E | 14.0000 | 17.7990 | 21.5980 | 24.1307 | 26.6000 |
| epicfight:diamond_greatsword | GREATSWORD | 15.0 | 23/11/0/0/0 | 0.75/A | 0.15/E | 15.0000 | 19.0704 | 23.1407 | 25.8543 | 28.5000 |
| epicfight:netherite_greatsword | GREATSWORD | 16.0 | 24/11/0/0/0 | 0.75/A | 0.15/E | 16.0000 | 20.3417 | 24.6834 | 27.5779 | 30.4000 |
| epicfight:iron_longsword | LONGSWORD | 7.0 | 12/8/0/0/0 | 0.5/C | 0.3/D | 7.0000 | 8.6884 | 10.3769 | 11.5025 | 12.6000 |
| epicfight:diamond_longsword | LONGSWORD | 8.0 | 12/8/0/0/0 | 0.5/C | 0.3/D | 8.0000 | 9.9296 | 11.8593 | 13.1457 | 14.4000 |
| epicfight:netherite_longsword | LONGSWORD | 9.0 | 13/9/0/0/0 | 0.5/C | 0.3/D | 9.0000 | 11.1709 | 13.3417 | 14.7889 | 16.2000 |
| epicfight:iron_spear | SPEAR | 6.0 | 10/11/0/0/0 | 0.25/D | 0.55/B | 6.0000 | 7.4472 | 8.8945 | 9.8593 | 10.8000 |
| epicfight:diamond_spear | SPEAR | 7.0 | 11/11/0/0/0 | 0.25/D | 0.55/B | 7.0000 | 8.6884 | 10.3769 | 11.5025 | 12.6000 |
| epicfight:netherite_spear | SPEAR | 8.0 | 11/11/0/0/0 | 0.25/D | 0.55/B | 8.0000 | 9.9296 | 11.8593 | 13.1457 | 14.4000 |
| epicfight:iron_tachi | TACHI | 7.0 | 12/13/0/0/0 | 0.25/D | 0.65/B | 7.0000 | 8.8995 | 10.7990 | 12.0653 | 13.3000 |
| epicfight:diamond_tachi | TACHI | 8.0 | 12/13/0/0/0 | 0.25/D | 0.65/B | 8.0000 | 10.1709 | 12.3417 | 13.7889 | 15.2000 |
| epicfight:uchigatana | UCHIGATANA | 7.0 | 9/14/0/0/0 | 0.15/E | 0.7/A | 7.0000 | 8.7940 | 10.5879 | 11.7839 | 12.9500 |
| epicfight:glove | FIST | 3.0 | 5/11/0/0/0 | 0.15/E | 0.6/B | 3.0000 | 3.6784 | 4.3568 | 4.8090 | 5.2500 |
| efn:air_tachi | TACHI | 10.0 | 14/14/0/0/0 | 0.25/D | 0.65/B | 10.0000 | 12.7136 | 15.4271 | 17.2362 | 19.0000 |
| efn:broadblade | LONGSWORD | 10.0 | 14/9/0/0/0 | 0.5/C | 0.3/D | 10.0000 | 12.4121 | 14.8241 | 16.4322 | 18.0000 |
| efn:crescent_moon | LONGSWORD | 12.0 | 15/9/0/0/0 | 0.5/C | 0.3/D | 12.0000 | 14.8945 | 17.7889 | 19.7186 | 21.6000 |
| efn:excalibur | SWORD | 11.0 | 13/8/0/0/0 | 0.35/D | 0.35/D | 11.0000 | 13.3216 | 15.6432 | 17.1910 | 18.7000 |
| efn:hf_blade | TACHI | 14.0 | 17/15/0/0/0 | 0.25/D | 0.65/B | 14.0000 | 17.7990 | 21.5980 | 24.1307 | 26.6000 |
| efn:kusabimaru | UCHIGATANA | 10.0 | 12/14/0/0/0 | 0.15/E | 0.7/A | 10.0000 | 12.5628 | 15.1256 | 16.8342 | 18.5000 |
| efn:meen_spear | SPEAR | 11.0 | 14/12/0/0/0 | 0.25/D | 0.55/B | 11.0000 | 13.6533 | 16.3065 | 18.0754 | 19.8000 |
| efn:nf_claw | FIST | 9.0 | 8/9/0/0/0 | 0.15/E | 0.6/B | 9.0000 | 11.0352 | 13.0704 | 14.4271 | 15.7500 |
| efn:ruinsgreatsword | GREATSWORD | 16.0 | 24/11/0/0/0 | 0.75/A | 0.15/E | 16.0000 | 20.3417 | 24.6834 | 27.5779 | 30.4000 |
| efn:thornwheel | GREATSWORD | 14.0 | 23/11/0/0/0 | 0.75/A | 0.15/E | 14.0000 | 17.7990 | 21.5980 | 24.1307 | 26.6000 |
| irons_spellbooks:amethyst_rapier | SWORD | 8.0 | 9/8/0/0/0 | 0.35/D | 0.35/D | 8.0000 | 9.6884 | 11.3769 | 12.5025 | 13.6000 |
| irons_spellbooks:boreal_blade | SWORD | 16.0 | 16/9/0/0/0 | 0.35/D | 0.35/D | 16.0000 | 19.3769 | 22.7538 | 25.0050 | 27.2000 |
| irons_spellbooks:claymore | SWORD | 10.0 | 12/8/0/0/0 | 0.35/D | 0.35/D | 10.0000 | 12.1106 | 14.2211 | 15.6281 | 17.0000 |
| irons_spellbooks:decrepit_scythe | SWORD | 11.0 | 12/8/0/0/0 | 0.35/D | 0.35/D | 11.0000 | 13.3216 | 15.6432 | 17.1910 | 18.7000 |
| irons_spellbooks:hellrazor | SWORD | 13.0 | 13/9/0/0/0 | 0.35/D | 0.35/D | 13.0000 | 15.7437 | 18.4874 | 20.3166 | 22.1000 |
| irons_spellbooks:magehunter | SWORD | 7.0 | 9/7/0/0/0 | 0.35/D | 0.35/D | 7.0000 | 8.4774 | 9.9548 | 10.9397 | 11.9000 |
| irons_spellbooks:spellbreaker | SWORD | 10.0 | 11/8/0/0/0 | 0.35/D | 0.35/D | 10.0000 | 12.1106 | 14.2211 | 15.6281 | 17.0000 |
| irons_spellbooks:twilight_gale | SWORD | 13.0 | 13/9/0/0/0 | 0.35/D | 0.35/D | 13.0000 | 15.7437 | 18.4874 | 20.3166 | 22.1000 |
| irons_spellbooks:autoloader_crossbow | CROSSBOW | 1.0 | 9/10/0/0/0 | 0.25/D | 0.45/C | 1.0000 | 1.2111 | 1.4221 | 1.5628 | 1.7000 |

