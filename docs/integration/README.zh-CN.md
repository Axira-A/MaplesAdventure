# Integration API v1

> 语言：**简体中文** | [English](README.md)

第三方集成**必须优先**使用 `dev.maplesadventure.api.*` 和文档化的数据包接口。

- [Status API 与事件](status-api.zh-CN.md)
- [Defense API](defense-api.zh-CN.md)
- [Typed damage provider](typed-damage-api.zh-CN.md)
- [Weapon Integration API](weapon-api.zh-CN.md)
- [Armor Integration API](armor-api.zh-CN.md)
- [数据包 Schema](datapack-api.zh-CN.md)
- [可编译的 Boss 示例](examples/BossIntegrationExample.java)
- [武器示例](examples/WeaponIntegrationExample.java)
- [盔甲示例](examples/ArmorIntegrationExample.java)
- [示例数据包](examples/datapack/)

## 依赖设置

目标为 Minecraft 1.21.1、NeoForge 21.1.x、Java 21。完整 Mod JAR 包含 API，目前没有另行发布的 API artifact 或公开 Maven 仓库。依照 [README](../../README.md) 配置 compile-only 依赖后运行：

```sh
./gradlew publishToMavenLocal
```

下游 NeoForge 项目可以使用：

```groovy
repositories { mavenLocal() }
dependencies {
    compileOnly 'dev.maplesadventure:maplesadventure:0.2.0'
    localRuntime 'dev.maplesadventure:maplesadventure:0.2.0'
}
```

示例版本须与本仓库的 `mod_version` 一致。若下游 Mod 无条件使用 MaplesAdventure，应在自己的 metadata 中声明必需依赖；可选集成应通过 `ModList` 隔离 Adapter 加载，避免缺少目标 Mod 时加载其 API 类型。

也可以使用本地构建 JAR 的 `compileOnly files(...)` / `localRuntime files(...)`，或通过 `includeBuild('../MaplesAdventure')` 替换相同模块；源码/复合构建仍需配置 MaplesAdventure 的 compile-only 依赖。不要把凭据、二进制副本或私有仓库 URL 提交进源码。`sourcesJar` 包含项目源码，`javadocJar` 仅记录支持的 API。`compileIntegrationExamplesJava` 编译 Java 示例，并纳入 `check`。

## 契约

服务端 API 应在逻辑服务端线程调用。View 是脱离内部状态的不可变值；空 `Optional` 表示上下文无效或不可用，不代表“抗性为零”。无效修改请求返回原因（Profile 分配返回 false）。客户端没有权威修改能力。

API ID 和方法契约在 v1 内稳定；内部包结构、枚举 ordinal、Attachments 和网络格式不是公开稳定接口。不要反射内部类、修改内部 NBT、依赖 HUD 缓存或通过 Mixin 调用这些接口。公开签名不包含可选战斗 Mod 类型，请求也不会绕过 `PhaseRelations`。

Event/provider 回调是只读的。API 拒绝回调中重入修改，以避免意外递归；这不是防止其他已安装 Mod 直接调用 Minecraft 方法或伪造环境来源的沙箱。
