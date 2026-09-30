# Logic Authoring 1.1（实验性）

> 语言：[English](logic-authoring.md) | **简体中文**

Authoring 定义与执行分离。`LogicComponent` 属于有版本的 Scene 数据；`LogicRuntime` 在加载或 revision 变化时编译已验证的绑定。客户端不执行玩法动作。这是 Event/Condition/Action 列表，不是脚本引擎或节点图。

## 定义与 Inspector

每个对象最多一个 `maplesadventure:logic`，包含最多 8 个 Binding。每个 Binding 有稳定 UUID、启用标志、类型化 Event、条件树、有序 Action。定义保存命名空间类型 ID、数据版本及有界的已注册标量字段；不能指定 Java 类、执行命令或提交任意 NBT 合并。

条件为 ALL/ANY/NOT/类型化 LEAF，短路求值。空 ALL=true，空 ANY=false；NOT 必须一个子节点。每 Binding 深度最多 8、节点最多 64、Action 最多 16；每定义最多 32 字段，每 Registry 最多 256 类型。仍受组件 16 KiB、对象 64 KiB、Scene、字符串和请求总限制约束。

Inspector 从服务器获取 Schema；可搜索选择器显示本地化名称及命名空间 ID。布尔开关、枚举选择与标量输入编辑本地 Draft；增删、重排同样不立即发包。Apply/Save 才提交。Save 将当前对象名称、Transform、组件字段与逻辑一起进行 revision 校验，一次提交并标脏，不强制保存整个世界。失败不产生半个作者数据修改。其他对象的 Delta 保留当前选择、面板宽度及草稿。

未知/被移除类型、未来版本、损坏数据保留诊断而不执行。Validation 定位对象/组件/Binding，覆盖缺少 Volume、上下文缺玩家、未知字段/类型、非法 Flag、Sound 注册项及结构上限。网络输入不静默转换或 clamp。单个绑定错误不会使整个 Scene 无法查看；需要显式修复或移除错误组件。

## 内置类型

以下 ID 均为 `maplesadventure` 命名空间：

| Registry | ID | 行为 |
| --- | --- | --- |
| Event | `player_enter_volume` | 外→内边沿，包含触发玩家 |
| Event | `player_exit_volume` | 内→外边沿，包含触发玩家 |
| Condition | `always` | 恒真 |
| Condition | `flag_equals` | scope=WORLD/PLAYER、flag=ResourceLocation、value=boolean |
| Action | `set_flag` | 同上字段，经服务端 Flag Service 写入 |
| Action | `show_message` | text 最长 512，translate 决定普通文本或本地化键 |
| Action | `play_sound` | sound 为注册的 SoundEvent，只发送给触发玩家 |

不含玩家上下文的事件不能绑定玩家专属动作或 PLAYER Flag。WORLD Flag 是当前存档全局数据，**并非逐 Phase 数据**；PLAYER Flag 属于玩家 UUID。未设置的 Flag 为 false。WORLD 使用 `maplesadventure_game_flags` SavedData；PLAYER 使用 `maplesadventure:game_flags` copyOnDeath Attachment。两者均有版本、只支持布尔、最多 4096 条；未知版本/损坏存档只读保留，不静默清空。

## Volume 与运行时

创建 Trigger Volume 原子添加 Box 和空 Logic，不塞入剧情。复用 `box_volume` / `radius`，Box 遵循对象旋转，Radius 为球体；两者都有时取并集。用玩家脚部位置判断。不会生成辅助 Entity/BlockEntity，也不 force-load 区块。

索引按维度和 16/64/256/1024/4096 格空间网格分层；每个区域选占用格数有界的层级。玩家最多查询五个索引格，再精确检查候选 Bounds，不进行全对象×全玩家扫描。Tick 路径不解码定义/Codec。每玩家保存临时边沿集合；停留不重复 Enter。Logout/换维度清边沿，不伪造 Exit；再次玩法进入可重新触发。不是全局排除 Spectator，而是**排除有效 Editor Session**；死亡/断线玩家也不求值。退出 Editor 先恢复原位置，再恢复玩法触发检测。

全服每 tick 4096 次预算涵盖 Binding、Condition、Action，第三方递归事件共享预算，递归深度最多 16。回调必须同步、有界、不阻塞。Action 按顺序执行，不是可回滚的玩法事务；后续扩展异常不会撤销已经完成的 Action。异常/预算耗尽停止该 Binding，记录来源，并禁用到 Scene revision 改变，避免静默重复副作用。编译时排除非法 Binding，其余合法绑定照常运行。

## 扩展 API

在 common setup、首次开服冻结前通过实验性 `MaplesAuthoringApi.registerEventType/registerConditionType/registerActionType` 注册。三个 Registry 独立，拒绝重复 ID。类型复用现有 `ComponentDescriptor<T>`：默认值、Codec、版本、显示键、类型化访问器及 Validator。建议使用不可变 Record；新类型无需专门 Screen。

参见构建时编译的 [LogicAuthoringExample](integration/examples/LogicAuthoringExample.java)。可信服务端 Hook 可在线程内调用 `MaplesAuthoringApi.emit`，传入 Scene/对象/事件 ID 及可选玩家。`LogicContext` 提供身份和运行时引用，不暴露可编辑 Scene/SavedData 集合；Flag 经明确 API 修改。扩展仍是可信 Java 代码，框架不能沙箱化无限执行的回调。Common 描述器不可导入客户端渲染器；客户端 Gizmo 仍独立注册。

## 可复现验收

1. 授权 Survival 玩家 F8，创建/选择 Scene，新增 Trigger Volume。
2. 增加 Enter Binding，条件 WORLD `example:triggered == false`。
3. 有序动作：设该 Flag=true、显示 `Trigger executed`、播放 `minecraft:block.note_block.pling`。
4. Apply/Save、Validate，退出回到原 Survival 位置，再进入区域。
5. 消息/声音一次；原地停留不再触发，离开重进被 Flag 条件阻止。
6. Editor 飞入不执行玩法绑定；保存重启后定义和 Flag 保留。

单元测试覆盖 Schema、条件树、边界、未知内容保留、revision、Codec、预算、布局及无惯性移动。隔离的 `-PweaponRegression=true` 专服执行 `/ma logicregression`；真实保存重启后执行 `/ma logicregression persisted`。Fixture 使用真实 ServerPlayer NBT/Clone 和 Runtime，但参与者是合成玩家，不能代替双客户端布局、鼠标或实际听音验收。

1.1 不提供 Encounter Action：自动激活不能绕过 FogGate、Phase、Attempt、Reset 权威流程，留给独立 Encounter Authoring 阶段。本轮没有 Prefab、时间轴、通用变量、任务、脚本或战斗系统迁移。
