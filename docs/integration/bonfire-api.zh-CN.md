# 内置篝火 API v1

> 语言：**简体中文** | [English](bonfire-api.md)

仅面向 `maplesadventure:bonfire`。外部 Legacy Bonfires 继续保留原有升级/休息 Hook，不写入 Maples 玩家篝火进度。公开签名不包含 Epic Fight、Iron's、Bonfires 或 Shoulder Surfing 类型。

## 权威与查询

在逻辑服务端线程调用 `dev.maplesadventure.api.bonfire.MaplesBonfireApi`：

- `query(ServerLevel, BlockPos)`：只查询已加载方块，返回脱离内部状态的 `Optional<MaplesBonfireView>`。
- `isActivated(ServerPlayer, MaplesBonfireRef)`：查询逐玩家激活记录。
- `lastRested(ServerPlayer)`：最后成功休息点，不是最后激活点；存在引用不代表方块仍有效。
- `isResting(ServerPlayer)`：仅有效 RESTING 会话。
- `isFeatureRegistered(ResourceLocation)`：注册状态，不等于当前授权。
- `availableFeatures(ServerPlayer)`：当前合法休息会话可用的功能 ID，按菜单顺序排列。

`MaplesBonfireRef` 保存维度 ID、不可变 BlockPos 与放置 generation UUID。同坐标替换篝火会使旧引用失效。`MaplesBonfireView` 复制名称和配置功能集，不暴露可变 Attachment、BlockEntity、内部 Session 或缓存。刻意不提供绕过验证的 `rest(player)`。

## 功能扩展

在 Mod 初始化/common setup、**首次服务器启动之前**，通过 `registerFeature` 注册 `MaplesBonfireFeatureHandler`。ServerAboutToStart 后注册冻结，单人服务器重启也不重复注册。重复 ID、迟到注册均抛出异常。

Handler 提供稳定 `id`、有长度限制的 `translationKey`、`order`、无副作用的 `isAvailable(context)` 和服务端 `execute(context)`。翻译由扩展 Mod 的语言文件提供。order 小的在前，相同则按 ID 字典序；内置升级 order=100，自定义一般使用 1000 以上。离开始终最后一项，是会话操作，不需要配置。

只有 **已配置 AND 已注册 AND 当前可用** 才显示。服务端下发列表，客户端不从方块实体推导权限。点击只提交 nonce 和功能 ID；服务端重查会话所有者、RESTING、生存/角色、维度、距离、放置代数、配置与可用性。功能随后打开的独立 UI 必须使用自己的服务器验证协议。

`MaplesBonfireContext` 含实时 ServerPlayer、脱离内部状态的 View 和捕获的 phase UUID。不要缓存 Context。回调异常按注册 ID 记录并隔离；可用性查询异常会隐藏该项。这是集成契约，不是阻止其他已安装 Mod 直接修改 Minecraft 的沙箱。

### 内置 ID 与存储

`MaplesBonfireFeatures` 提供 `LEVEL_UP`、`FLASK_ALLOCATION`、`SPELL_MEMORY`、`REINFORCE`、`WARP`，均为 `maplesadventure:` 命名空间下对应的小写 ID。

本轮仅升级有内置 Handler，继续调用现有 UpgradeAccessService 和按来源分发的 BONFIRE validator。退出升级返回坐姿菜单，不重复休息/Reset。Flask、法术记忆、强化、传送只是预留 ID，**尚无玩法实现**。以后注册 Handler 后，已配置的功能自动出现，无需再改 BonfireScreen 或新增包类型。

方块配置使用 `FeatureDataVersion=2`，最多 64 个完整资源 ID，每个最多 128 字符。旧无命名空间值迁移为 `maplesadventure:<id>`。合法但未安装的 addon ID 在读写时保留；非法/超限输入丢弃并警告。

`/ma bonfire feature <x> <y> <z> <id> <true|false>` 支持内置简写和完整 addon ID；`info` 分别显示配置 ID 与注册状态。

## 休息与重置生命周期

合法坐下过渡先取得一次性提交许可，再执行回调。BonfireRestService 提交 lastRested、恢复运行时资源、调用 BonfirePhaseResetService、发布完成通知并同步进度。打开/选择功能、离开都不重复休息。

通过 `registerRestResetParticipant` 注册 `MaplesBonfireRestResetParticipant`。每次合法休息调用一次，按 priority、ID 字典序排列。保留的 `maplesadventure:encounters` 首先执行，priority=Integer.MIN_VALUE；扩展 priority 必须更大。只重置 **context.phaseId() 中明确归属自己的状态**，不能扫描附近共享怪。扩展异常单独记录，不中断后续 participant，也不会重跑核心 Reset。

内置 participant 复用 EncounterResetService：重置当前 Phase 的 Encounter 敌人与普通 Phase 战利品，旧 generation 失效且不强制加载 Chunk，Boss/Fog 生命周期继续交给原服务。其他 Phase、共享 Mob、Debug prototype zombie 均不属于此次 Reset；已击败 Boss 尊重 `respawnDefeatedBosses` 配置。

HP 使用真实 `getMaxHealth()`，可选魔力/精力使用包含外部 modifier 的运行时最大值；未安装集成不制造替代资源池。可选恢复失败只警告，不回滚复活点或相位 Reset。

Iron's 3.16.3 的自然恢复将 MAX_MANA 截为整数。可选 `IronsFractionalManaRegenMixin` 仅在魔力已等于有限、正数、带小数的运行时上限时跳过该次自然恢复，防止满值 `137.5` 被削为 `137`。耗蓝、部分恢复和整数上限继续使用 Iron's 原行为，不修改第三方 JAR。

在 `NeoForge.EVENT_BUS` 监听不可取消的 `MaplesBonfireRestCompletedEvent`，不可变 context 表示休息已提交，不能否决或重复提交。可选资源仍可能恢复失败。每次 event post 外层隔离异常；NeoForge 本身不保证某监听器抛错后继续其余监听器。需要逐回调隔离的 Reset 工作应注册 participant。以后 Flask refill 可监听完成事件，本轮不实现 refill。

## 复活与网络边界

玩家 Attachment 仍为版本 1，死亡复制，激活与 lastRested 分离。复活重新验证代数并搜索安全站立空间。确认删除/替换时清除 lastRested；暂时堵住或维度不可用则保留记录并回退 Vanilla。不创建永久 Chunk ticket。

协议 **25**：canLevelUp 替换为最多 64 条菜单项，客户端发送 SELECT_FEATURE/LEAVE 意图。4096 个激活引用正文在 minecraft:overworld 时为 180226 字节，维度 ID 达 128 字符上限时为 630786 字节，连同少量包 ID 开销仍低于客户端接收的 1 MiB 上限。不新增逐 Tick 菜单或进度广播。

参见[可编译示例](examples/BonfireIntegrationExample.java)。`compileIntegrationExamplesJava` 已属于 `check`。

## 可复现开发检查

在**隔离测试世界**运行 `runServer -PweaponRegression=true`。管理员命令 `bonfirefunctional core` 用合成服务器参与者验证 Phase Reset 归属、COMMON 部分击杀、已击败 Boss 策略、资源恢复及安全/失效/堵塞复活；不等同于多客户端测试。夹具会搭建并还原测试平台。

对于真实连接的测试玩家，`bonfirefunctional resources <player>` 将 HP 设为 3、已安装的可选资源设为 10%。通过 F 休息，再执行 `bonfirefunctional check <player>` 比较原始数值与运行时上限。可选测试夹具记录每次公共完成事件及 Phase generation，可检查菜单/升级是否重复 Reset。按本地依赖说明添加 `-PwithEpicFight=true` 和/或 `-PwithIronsSpells=true`；生产环境不依赖这些开发参数。
