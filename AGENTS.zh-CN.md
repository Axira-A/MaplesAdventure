# MaplesAdventure 贡献约束

> 语言：**简体中文** | [English](AGENTS.md)。英文 `AGENTS.md` 是权威贡献约束。

- 目标版本：Minecraft 1.21.1、NeoForge 21.1.x（当前 21.1.219）、Java 21。
- 游戏逻辑由服务端裁决；客户端数据包只表达意图，不作为可信状态。
- 第三方集成优先使用 `dev.maplesadventure.api.*` 和公开的数据包接口。
- Public API v1 是兼容性契约：保持 ID 与签名稳定；删除前优先扩展、弃用，并同步更新 Javadoc、示例和契约测试。
- `progression.*`、Attachments、网络、内部缓存与客户端 HUD 属于实现细节；不要通过 API 暴露可变状态对象。
- 不得绕开 `PhaseRelations`。有归属的状态效果必须传入真实实体或投射物来源。
- 武器/投射物计算保持唯一权威，投射物保留发射快照。Typed provider 只描述一次已有命中，不得再次调用 `hurt` 或直接扣血。
- 数据包解析必须限制输入边界，在 reload/tag 绑定时编译查询结构；修改解析器时同步更新 JSON 文档。
- 持久化 ID 保持稳定，序列化格式需版本化并测试旧世界迁移。
- 可选集成在缺少目标 Mod 的专服上也必须安全加载；不得修改第三方 JAR、复制内部实现或意外增加必需依赖。
- 本地 compile-only 依赖放在被忽略的 `local-development.properties` 中，见 README。
- 构建/测试：`./gradlew clean test`、`./gradlew clean build`。可选专服 fixture：`./gradlew runServer -PweaponRegression=true`；仅在隔离测试世界运行 `apiregression`、`statusregression run`、`statusregression extended`、`playerdefenseregression`、`defenseregression run`。
- 保留测试及可复现 fixture 源码，不提交日志、本地依赖 JAR、反编译源码、测试世界、截图、校准输出或任务验证流水账。
- 保持 MIT 源码授权和第三方权利声明；不要推断第三方美术归属。
- 面向用户的仓库 Markdown 文档应中英双语。修改英文文档时须同步更新对应简体中文版，反之亦然。`README.md` 为中文首页，`README.en.md` 为英文对应页。
