# 第三方声明

> 语言：**简体中文** | [English](THIRD_PARTY_NOTICES.md)。涉及权利判断时请阅读英文原文及相应上游许可。

根目录 MIT 授权适用于项目的 MIT 源代码，不自动涵盖项目自有模型、贴图、动画、图标、UI 等艺术资产；后者见 [ASSET_LICENSE.zh-CN.md](ASSET_LICENSE.zh-CN.md)。Minecraft、NeoForge、依赖 JAR、外部素材及其他第三方材料继续受各自权利人和许可约束。构建不把依赖 Mod 打包进发行物；本地检查或测试不等于再分发许可。

## 项目视觉贡献者

**Ai_myh — 美术与模型**。此为项目贡献署名，不表示贡献作品按 MIT 授权。各作品权利仍以适用的创作者协议和资产政策为准。

## 当前集成

| 项目 | 当前源代码中的关系 |
|---|---|
| Minecraft / NeoForge | 平台和工具链依赖；使用标准事件、注册、Attachments 及少量定向 Vanilla Mixin。 |
| Epic Fight | Compile-only API；运行时可选的精力、闪避技能、武器事实、状态动作、残影动画和感知过滤集成，含定向可选 Mixin。 |
| Iron's Spells 'n Spellbooks | Compile-only API；运行时可选魔力、学派补正/亲和集成及学派能力 Accessor Mixin。 |
| EpicFight-Nightfall | 通过 Epic Fight 注册 ID 与 `efn` mod ID 可选接入，不硬依赖 Nightfall 类。 |
| Bonfires | 仅作旧版可选兼容：反射/Accessor 与定向 Mixin 处理真实成功休息、点燃和授权菜单。内置 `maplesadventure:bonfire` 不使用 Bonfires 的模型、贴图或 Spiral Sword 激活素材。 |
| Presence Footsteps | 定向可选 Mixin，感知声音来源。 |
| Subtle Effects | 定向可选 Mixin，感知实体/数据包特效来源。 |
| Better Lock On | 独立可选兼容测试运行环境；无复制的目标管理系统。 |
| Shoulder Surfing Reloaded | 独立可选兼容测试运行环境；当前无专用 API 依赖或 Mixin。 |
| Create | 独立可选兼容测试环境及交互方块标签；无打包代码。 |

Epic Fight 与 Iron's 类型不出现在公开 API 签名中。可选运行时集成不意味着编译无需 API，详见 README。本仓库的兼容 Hook 并非第三方 Mod 完整实现的复制品；不修改或随发行物附带第三方 Mod JAR。其他可选测试运行时依赖包括 Iron's Lib、Curios、GeckoLib、Player Animator、Fzzy Config 和 Kotlin for Forge。

## 已审阅参考

曾参考 Seramicx 项目 `epic-fight-better-lockon-movement-camera-fix` 研究较早的锁定原型；当前源码没有纳入其实现，此处也不表示该项目许可覆盖其他依赖。上游位置：[Epic Fight](https://github.com/Antikythera-Studios/epicfight)、[Shoulder Surfing Reloaded](https://github.com/Exopandora/ShoulderSurfing)、[Create](https://github.com/Creators-of-Create/Create)、[Bonfires](https://github.com/Wehavecookies56/Bonfires)、[相机修复参考](https://github.com/Seramicx/epic-fight-better-lockon-movement-camera-fix)。

## 复制代码与素材

源码审计发现 API 引用、独立兼容代码和测试依赖，未发现被打包的第三方 Mod 实现或依赖 JAR。运行时引用 Minecraft 贴图、玩家皮肤或其他 Mod 资源不会转移版权。不能仅凭文件名推断外部美术所有权；保留文件级声明，并在来源不明时取得许可。Gradle wrapper 是构建工具，不是玩法 Mod。通用上游许可模板不能证明某实现已被复制。
