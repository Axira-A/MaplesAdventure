# MaplesAdventure

> 语言：**简体中文** | [English](README.en.md)

MaplesAdventure 是面向 Minecraft 共享世界的类魂冒险与 RPG 战斗框架。地图作者布置固定 Encounter 与 Boss；玩家培养属性、管理装备，并通过世界目标交互。服务端权威的相位系统支持独行、协作、决斗与主动入侵。

## 运行要求

- Minecraft **1.21.1**、NeoForge **21.1.219 或兼容的 21.1.x**、Java **21**。
- 服务端与客户端安装相同版本和协议。第三方整合均为可选，不随本 Mod 打包。

## 战斗、属性与防御

八项 RPG 属性、派生资源、精确 XP 升级、装备负重与自动闪避。武器需求、补正、多通道攻击力、质变、敌人与玩家防御及异常状态都由服务端裁定。投射物发射时冻结武器快照；多通道攻击仍只触发一次 Minecraft 命中。伤害通道包括标准物理、斩击、打击、突刺、魔法、火焰、雷电、冰霜和神圣。

Epic Fight 可提供精力、技能、动画与闪避整合；安装 Nightfall 时可选择相应闪避技能。Iron's Spells 可提供魔力和法术学派整合。这些都是可选依赖，详见[第三方声明](THIRD_PARTY_NOTICES.zh-CN.md)。

## 异常状态

出血、中毒、猩红腐败、冻伤、睡眠、癫狂与死亡枯萎使用服务端持有的积累、抗性、资格判定与重复触发修正。免疫、健壮、理智与活力构成防御视图。感应属性对异常积累的补正独立于武器攻击补正。HUD 和触发提示显示服务端状态，客户端输入不能强制触发异常。

## 多人相位与固定 Encounter

独立相位、金色协作、红灵入侵/决斗、安全返回、相位敌人和掉落、历史玩家残影及感知隔离都运行在同一共享世界。固定 Encounter 支持逐相位 Boss Attempt、实体血缘、锁定人数倍率、相位 BossBar 和雾门。Boss 房间几何封闭仍由地图作者负责；复杂第三方 Boss 需要明确适配，不能假定自动兼容。

## 篝火与交互

MaplesAdventure 已拥有**内置 Bonfire Core**。管理员可命名和配置篝火；激活记录逐玩家独立。第一次交互仅激活，之后才打开休息菜单。成功休息会设置该玩家的最后休息点、恢复可用 HP/魔力/精力，并对所属相位执行一次 Encounter Reset。仅管理员开启 `LEVEL_UP` 后，玩家休息时才出现升级入口。死亡时优先在有效的最后休息篝火附近复活；失效则回退至 Vanilla 规则。

篝火方块当前仍引用 Vanilla 占位模型，未使用 Bonfires 美术。三段 Epic Fight 坐下动画尚未交付。外部 Bonfires Mod 仅保留为**旧版可选兼容**，不是内置篝火的前置。详见[篝火说明](docs/bonfire.zh-CN.md)。

默认可重新绑定的按键：**L** 打开联机菜单，**F** 与当前世界目标交互，**Y** 切换附近目标。F 可能与 Vanilla 副手切换冲突，请按需要在控制设置中调整。结构化留言、召唤符、雾门、容器等复用世界交互流程。Lost Soul 仍保存和回收未消费 XP。

## 公共 API 与数据包

第三方整合应使用 **`dev.maplesadventure.api.*`**，不要依赖内部 Attachment、网络或缓存。公共 API v1 是兼容性契约；调用在服务端线程执行，Provider 与通知为只读。

- [整合入门](docs/integration/README.zh-CN.md)
- [异常状态与事件](docs/integration/status-api.zh-CN.md)
- [防御快照](docs/integration/defense-api.zh-CN.md)
- [类型伤害](docs/integration/typed-damage-api.zh-CN.md)
- [武器 API](docs/integration/weapon-api.zh-CN.md)
- [护甲 API](docs/integration/armor-api.zh-CN.md)
- [数据包格式](docs/integration/datapack-api.zh-CN.md)
- [可编译示例](docs/integration/examples/)

## 构建与开发

源码包含 Epic Fight 21.17.3.1 与 Iron's Spells 3.16.3 的仅编译期适配。请从合法发行渠道取得本地 JAR，通过 Gradle 属性 `epicFightJar`、`ironsSpellsJar` 或被忽略的 `local-development.properties` 指定路径。不要提交第三方二进制或凭据；核心 Mod 运行时不强制依赖它们。

```sh
./gradlew clean test
./gradlew clean build
./gradlew javadoc
```

Windows 请使用 `gradlew.bat`。构建结果位于 `build/libs`，包括 Mod、源码与 API Javadoc JAR。开发任务包括 `runClient`、`runServer`；可选运行配置见 `build.gradle`。整合示例编译已纳入 `check`。

## 开发人员

- **Axira** — 程序与策划
- **Ai_myh** — 美术与模型

## 许可

源代码遵循 [MIT License](LICENSE)，版权 (c) 2026 Axira。项目自有的视觉与美术资产另行保护，**不适用 MIT**，详见[美术资产许可](ASSET_LICENSE.zh-CN.md)。第三方材料遵循各自条款，详见[第三方声明](THIRD_PARTY_NOTICES.zh-CN.md)。
