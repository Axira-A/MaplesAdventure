# Editor Foundation / UX 1.1

> 语言：[English](editor-foundation.md) | **简体中文**

本轮为实验性 Authoring，已支持 [Event/Condition/Action 逻辑](logic-authoring.zh-CN.md)。未实现 Prefab、完整 Undo、Runtime 迁移、Scene 删除或跨维度迁移。既有 gameplay API v1 与素材许可不变；`api.editor` / `api.editor.client` 明确标为实验性接口。

## 使用

F8 是可重绑定 KeyMapping，不是原始按键轮询。专服 OP 2 或单人世界真正主人可进入；Creative 不自动授权。篝火休息、喝瓶及控制锁定期间拒绝进入。服务端先用 `editor_recovery` Player Attachment 保存原模式/维度/坐标/朝向，再切到 Spectator；不改变 Phase 或 RPG 数据。死亡、退出、换维度、撤销权限结束会话并恢复。Login/Respawn 恢复孤立标记；标记与 Vanilla GameMode 保存在同一份玩家 NBT，不复制背包/XP。管理员外部修改模式会关闭编辑，但保留管理员指定的模式。原维度缺失时警告并回退主世界出生点。临时 Session 而非 Spectator 本身标识编辑者。

输入命名空间 ID 和显示名，在当前维度创建 Scene；左上选择器搜索当前维度目录。对象 UUID 由服务器产生，改名/改组不变，复制产生新 UUID 并保留位置。Group 只用于组织，不继承 Transform；删除组会将直接成员提升到父组。

左右 Hierarchy/Inspector 分别滚动，可拖分隔线调整宽度。默认 19%/25%，最小/最大宽度保护中央视口；比例仅保存于本地 `maplesadventure-editor-client.toml`，不进世界存档或网络。搜索/折叠依靠稳定 ID。布尔/枚举使用开关/选择器，Transform 和组件可折叠。空对象/标记在中央视线命中点创建，无命中则前方四格；可通过列表或世界 Bounds 选择。字段先写本地草稿，Enter/应用才提交；切换选择或退出丢弃草稿。删除需要二次确认。同类型组件每对象最多一个。未知/损坏组件保留数据，但没有可编辑字段或组件 Gizmo。

移动键不依赖右键，Space/Shift 上下；中央右键转动视角，F 朝向当前选择。仅对已授权会话中的本地 Spectator 使用客户端窄范围 Mixin，以归一化的 0.35 格/tick 位移替代原版加速/惯性；仍是玩家正常移动包，不瞬移、不增加独立镜头。文本输入、Picker、失焦、拖动 Gizmo/分隔线时停止移动。Editor 点击不会攻击、使用、放置或触发 F/Y/L。世界 X/Y/Z 轴用于移动；Yaw 工具绕对象中心旋转，Pitch 数值编辑。拖动只做预览，松开提交一次，Esc 取消；退化视角禁用对应拖动，仍可数值编辑。超过 128 格剔除。普通游戏不运行编辑器选择/渲染扫描。

## 数据与事务

`authoring` 保存不可变 Scene/Object/Group/Transform/组件值及纯操作；`editor` 管理服务端会话、权限与操作分发；`client/editor` 管理草稿、选择及显示；`api/editor` 提供通用 Descriptor，客户端表现独立注册。

世界级 `maplesadventure_authoring` SavedData 存在主世界，开服加载。Scene v1 保存 ID、维度、显示名、revision、组与对象。对象保存绝对坐标、yaw/pitch、组 UUID、组件和 revision。UUID/type ID 确定排序序列化；保存、校验、定位作者对象不加载区块。

版本分派入口为 `SceneSerialization.load`。v1 可修复的缺少名称/Transform 使用默认值并产生诊断。未知/解码失败组件原样保留。损坏/未来版本 Scene 只读，SavedData 保留原始序列化内容；未知根版本保留整个根数据并拒绝写入。修复/校验诊断尽可能定位 Scene、对象、组件、字段。未来组件版本不静默降级。

每请求一个操作，先验证权限、nonce/request ID、维度、订阅 Scene、限流、Scene 与目标 revision，再构造新值。类型化访问器返回不可变新值；验证成功后才一次提交 SavedData。每次成功增加 Scene revision，受影响对象/组也增加自己的 revision。组循环、层级深度及世界边界失败均拒绝整个操作。重复 request ID 不重复执行；过期编辑返回权威快照，不自动合并或覆盖别人修改。

## 协议与上限

协议 29 在有界 `editor_request` / `editor_page` 增加类型化逻辑及原子 Draft 提交；既有 gameplay Payload 字段不变。C2S 只包含意图、nonce/request ID、预期 revision 和已注册标量字段；不接受客户端 Scene、类名或 NBT Patch。ResourceLocation 校验格式，声明目标 Registry 的字段另外验证服务器注册项存在性。

S2C 包含会话结果、当前维度目录、Schema、分段初始快照、变化的对象/组、删除 UUID、校验与操作结果。只向授权订阅者发送数据。快照完整组装后原子替换；Delta 带前后 revision，断层请求重同步。退出清空草稿、选择和页面，迟到响应不能重新打开。不会每 tick 发完整 Scene。

| 限制 | 数值 |
| --- | --- |
| 每世界 Scene | 256 |
| 每 Scene 对象 / 组 | 4096 / 1024 |
| 每对象组件 / 组深度 | 32 / 16 |
| 名称 / 字段字符串 | 128 / 1024 字符 |
| 组件 / 对象 / Scene 存储数据 | 16 KiB / 64 KiB / 16 MiB |
| Snapshot 单段 | 256 KiB |
| Field Patch | 64 字段 |
| 请求令牌桶 | 每秒 20、突发 40 |

坐标必须有限且位于当前维度高度/世界边界内。Radius 有限非负，Box 三边有限且为正。首版数值字段使用文本输入配合 Schema 校验，不静默 clamp 非法网络输入。

## 扩展

首次开服前在 common setup 调用 `MaplesEditorApi.registerComponent` 注册 `ComponentDescriptor<T>`：稳定 ResourceLocation、版本、默认值、Codec、本地化键、类型化字段和 Validator。重复 ID 拒绝，开服前冻结。支持 boolean/integer/double/string/enum/ResourceLocation，以及 nullable/readOnly 元数据。回调必须纯函数且有界；异常不能授予权限或提交半成品数据。

可编译的[组件示例](integration/examples/EditorComponentExample.java)无需改核心即可注册注释组件；[客户端 Gizmo 示例](integration/examples/EditorGizmoExample.java)仅从 client setup 使用 `MaplesEditorClientApi`。Common Descriptor 不能引用客户端渲染器。Gizmo 注册在加载完成后冻结；Inspector 悬停提示提供字段约束。权限扩展可授予可信服务器作者角色，不信任客户端标志；保留默认 OP/世界主人策略。

## 回归与边界

执行 `gradlew.bat clean test`、`gradlew.bat clean build`，`check` 会编译扩展示例。在隔离的 `-PweaponRegression=true` 专服执行 `/ma editorregression`，保存重启后执行 `/ma editorregression persisted`。测试通过合成参与者调用真实服务端请求入口。`flaskdeathregression` 通过真实致命伤害及 Clone/Respawn 事件覆盖 keepInventory 两种值；`bonfirefunctional core` 验证现有 Phase Reset/资源行为。它们不替代双真实客户端视觉/输入、单人主人权限及可选镜头人工测试。Fixture 不打入正式 JAR。

使用启用 regression 的客户端连接并打开已授权工作台后，按 F9 可运行真实客户端导航测试：不按右键前进、松键立即停止、文本框聚焦时禁止移动。它在实际客户端 tick 上驱动 Screen 输入状态；正式模组不注册这个开发快捷键。

Flask 死亡掉落只过滤红/灰两种永久入口物品，保留缺失补发、次数、材料和主动 Q 丢弃。Encounter 清理先收集已加载目标再 discard，避免删除 live entity lookup 的当前元素破坏遍历。

没有将原生窗口/焦点崩溃绕过逻辑塞入 gameplay。GLFW 原生错误应与 Editor Java 异常分开报告。采用保守 Scene 级冲突，无对象锁和完整 Undo。本轮面板、按钮、分隔线均为原创代码绘制，没有新增第三方图标、位图或许可；参考图只用于布局方向。运行时限制、扩展示例和 `/ma logicregression` 恢复/触发测试见[逻辑创作](logic-authoring.zh-CN.md)。
