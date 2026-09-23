# Status API

> 语言：**简体中文** | [English](status-api.md)

`MaplesStatusApi` 委托现有状态管线，不建立平行状态。`MaplesStatusType` 定义 BLEED、POISON、SCARLET_ROT、FROSTBITE、SLEEP、MADNESS、DEATH_BLIGHT；ID 为 `maplesadventure:<lowercase_name>`，未知 ID 的 `find(id)` 返回空值。

## 操作

- `apply(target, type, amount, source)`：提交有限且为正的积累量，最高 100000。
- `requestProc(target, type, source)`：通过相同检查请求足以触发一次 proc 的积累。这是有权限的服务端集成逻辑，不是无条件击杀或绕过效果检查。
- `query(target, type)`：返回可选不可变 `StatusView`，不创建状态 Attachment。
- `clear(target, type)`：治愈状态，包括控制锁，保留修正值。
- `clearBuildup(target, type)`：仅清理待积累量，不清计时效果、控制或修正。

以上操作需要服务端线程上的有效存活实体。死亡、移除、客户端或错误线程目标的查询返回空值。成功 proc 期间目标可能死亡，返回快照仍可描述该目标。视图包括当前积累、实际修正阈值、免疫、计时状态、修正 Profile/偏移/次数、proc 伤害倍率及 Sleep/Madness 剩余控制锁。Buildup 是最近一次权威 Tick 值。

来源工厂：`StatusSource.fromEntity(entity)`、`fromProjectile(projectile)`、`environment()`、`integration(integrationId, entity)`。应传入真实来源，不得以 `environment()` 绕过有归属攻击的 Phase 限制。来源是短生命周期对象，不要缓存或持久化实体引用。投射物/Owner Phase 在应用时验证；此 API 不重建或覆盖武器发射快照。

## 返回结果

`StatusApplyResult` 含 `outcome`、可选 `before` / `after` 和 `procced()`。结果枚举：APPLIED、PROCCED、IMMUNE、ALREADY_ACTIVE、PHASE_DENIED、INVALID_TARGET、INVALID_AMOUNT、UNSUPPORTED_STATUS、INVALID_SOURCE、NOT_SERVER、WRONG_THREAD、REENTRANT、CLEARED。部分验证失败没有快照。CLEARED 是幂等成功请求；无变化的 clear 不触发事件。每次显式请求是独立操作，集成不可重复提交同一次玩法命中。

## 通知

监听 `NeoForge.EVENT_BUS`：`StatusBuildupEvent` 在接受积累后触发一次（包括 proc 命中）；`StatusProcEvent` 在 proc 效果提交后触发一次，**不要再次应用 proc 伤害**；`StatusClearEvent` 对每个被清除状态触发一次并带稳定 Reason。它们都是不可取消、只读、脱离 Attachment 的快照通知。普通武器、环境和命令来源统一使用这些事件。致死 proc 的死亡清理可能在最终 Buildup/Proc 通知前产生 Clear 事件；最终快照反映真实死亡/幻影返回结果。

不要在回调里调用 `hurt` 或修改玩法状态。回调中的公开 API 写入（包括 Profile 写入）返回 REENTRANT/false。延迟操作也应单独验证并去重，不可把事件重新转为同一次 proc。Sleep/control 与 Poison/Rot/Frost 的计时激活不同；本 API 不改变控制时长、免疫、积累曲线、伤害或抗性平衡。
