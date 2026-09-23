# Defense API

> 语言：**简体中文** | [English](defense-api.md)

`MaplesDefenseApi.query(entity)` 返回可选、脱离内部状态的 `DefenseView`，包含九通道 `defense` 与小数形式的 `absorption`、七种状态（实际免疫和修正阈值）、非玩家请求的 Profile ID、未知 Profile 标记与当前缓解压力配置。

`channel(entity, MaplesDamageChannel.FIRE)` 查询单一通道；`status(entity, MaplesStatusType.MADNESS)` 查询与 `MaplesStatusApi` 相同的权威状态视图。不会暴露可变 Profile Map、玩家 Attachment 或内部缓存。

`assignProfile(nonPlayer, profileId)` 验证已加载数据包 Profile 并写入现有持久引用。未知 ID、玩家、无效实体、客户端/错误线程或只读回调重入返回 false。`clearProfileOverride(entity)` 恢复普通数据包规则解析。保留的 `maplesadventure:none` 是单位值视图。

玩家使用现有玩家防御计算器和服务端开关，不接受 Entity Defense Profile 分配。这里的 Defense 不是 Vanilla 护甲；`absorption` 是伤害比例而非吸收心。查询与分配都不调用 `hurt`。

Profile 引用会随实体保存；Reload 按最新数据解析。已删除的 ID 回退到 NONE 并报告未知，不会静默替换为别的 Profile。静态 Boss 配置见[数据包 Schema](datapack-api.zh-CN.md)。
