# Typed damage API

> 语言：**简体中文** | [English](typed-damage-api.md)

在 common setup 的 `enqueueWork` 中调用 `MaplesTypedDamageApi.register(id, priority, provider)`。回调接收已有 `DamageSource` 并返回 `Optional<TypedDamage>`；不属于你的来源应返回空值。

`TypedDamage` 复制通道 Map。`MaplesDamageChannel` 包含 PHYSICAL、SLASH、STRIKE、PIERCE、MAGIC、FIRE、LIGHTNING、ICE、HOLY。PHYSICAL 是标准物理而非合计。允许 1–9 个条目，绝对压力必须有限且非负，总和为正且不超过 1000000。

**Provider 只描述一次已有攻击中的压力，绝不能调用 `hurt`、修改 HP、发布第二个伤害事件、生成武器命中或修改状态。**Registry 不发起伤害。Minecraft/NeoForge 原有逻辑和 Phase 检查仍在最终缓解前执行。

例如 MAGIC=20、FIRE=10 表示 2/3 魔法和 1/3 火焰压力，不是两次命中，也不是额外扣 30 HP。已有武器/投射物快照优先于 provider；provider 处理其余未分类来源，先于精确 Vanilla fallback。高 priority 优先，同级按 ID 字典序；相同 ID 重新注册会替换旧项。注册在世界 Reload 后仍存在，不自动按世界注销。最多 64 个 provider。

当前通用非武器 typed mitigation 适用于**玩家目标**；本 API 不会启用新的通用敌人减伤，也不改变武器平衡。自定义法术不会因此自动拥有武器补正或状态积累。

无效或抛异常的 provider 结果由内部 Registry 隔离并记录警告；回调中的公开状态/Profile 修改被拒绝。该契约不能沙箱化恶意 Mod 对 Vanilla 生命方法的直接调用，provider 应保持纯函数。MAGIC + FIRE 注册示例见[可编译代码](examples/BossIntegrationExample.java)。
