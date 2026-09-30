# P1 修复后的代码复查

## 范围与判断边界

复查业务源码、密码库迁移、加密格式、会话生命周期、Manifest、资源、构建及测试。P0 为紧急阻断级问题，P1 为严重数据损坏或安全缺陷，P2 为功能和可靠性问题，P3 为低影响工程质量问题。测试状态以 ROADMAP.md 为准。

未发现有充分证据的新 P0。原 P1 中的错误记录定位、删除复活、非原子覆盖写入、系统备份明文均已有代码修复；身份验证按用户最新要求暂时取消。新密码库加密；用户明确选择暂时保留旧 need.json，所以旧文件明文风险仍存在，不能声称完全消除。

## 已修复的遗漏和关联问题

- 异步加载可能用删除前的旧结果覆盖新列表：MainActivity 使用请求版本检查，只接收最新结果。
- 解锁尚未完成时退出页面：取消任务，派生出的未接管密钥关闭并清除，不在后台开放密码库。
- 页面重建可能并发初始化不同主密码：初始化入口串行，第二次调用重新检查已提交密码库。
- 新增记录保存期间旋转、再次提交可能重复添加：草稿保留稳定 ID，仓储对同内容重复提交幂等处理，异内容重复提交拒绝覆盖。
- 损坏记录被跳过后产生数组错位：严格全量解析，任何记录异常都阻止迁移，不创建空库。
- 错误 UTF-8 字节可能被替换后迁移成错误密码：解码器拒绝无效编码。
- 零字节旧文件或 JSON 后附加垃圾内容不能被当作合法空库或忽略：迁移严格拒绝，保留原文件。
- 明文密码经 Intent 或保存状态传递：编辑页面只传稳定 ID，输入禁止保存到 Bundle；旋转草稿只保存在进程内 ViewModel。
- 密码页面截图、非法 Adapter position、主线程 IO、搜索状态丢失、主页约束闭环：关联修复已实现；设备 UI 行为仍待实机验证。

## 用户决定暂时取消身份验证

全部主密码设置及解锁弹窗已移除。默认使用 Android Keystore 自动打开本机加密库；这保护磁盘文件，但不能阻止能打开应用的人读取记录。旧主密码格式仅保留兼容代码，不自动创建或覆盖。平台依据：[Android Keystore](https://developer.android.com/privacy-and-security/keystore)。

## 仍存在的 P1

### P1：旧明文文件按用户选择保留

- 位置：VaultRepository.openDevice、MainActivity.loadData。
- 依据：迁移读取 need.json 后仅写入加密 vault-device.dat，原文件不修改、不删除。
- 影响：拥有旧文件的主体仍能读取迁移前的凭据；新编辑和新增内容只写入加密库。
- 当前处理：禁用备份、排除设备迁移，列表顶部显示明文残留提示。
- 后续：只有用户改变保留决定后，才能验证加密库再删除旧文件。
- 验证：迁移测试逐字段核对并断言旧文件字节未变。

## 本轮已修复的 P2

- 密码生成器改用 SecureRandom，有界抽样索引及无重复字符集，默认 16 位。PasswordGeneratorTest 验证字符集唯一性、长度和范围；随机统计不作为密码学安全证明。
- 密码复制增加 EXTRA_IS_SENSITIVE、随机归属 Token 和系统复制时间核对；30 秒后条件清理。ClipboardCleanupTest 覆盖未到期、到期、新内容保护、同标记不同复制时间、读取不可用、连续复制和失败重试。PasswordClipTest 检查实际 Android ClipData 元数据，仅编译，尚未执行。
- Android 无焦点时可能无法读取剪贴板，清理延后至重新获得焦点；进程被终止后不能保证定时任务执行。清理任务仅保留复制元数据，不持有密码。没有声称敏感标记能阻止读取或所有情况下都在 30 秒内清除。

## 仍存在的 P2

### P2：Android 13 及以上没有申请通知权限

- 位置：ClockActivity.kt:20、WaterWorker.kt:27。
- 依据：仅 Manifest 声明权限，代码没有运行时申请，Worker 抑制检查并返回成功。
- 影响：新安装时提醒可能一直不可见。
- 建议：申请并检查通知权限，再调度提醒。
- 验证：Android 13 新安装分别授权、拒绝，检查是否通知及任务反馈。

### P2：重复创建周期任务，没有关闭入口

- 位置：ClockActivity.kt:22。
- 依据：每次点击构造新的周期请求并 enqueue，Tag 不提供去重。
- 影响：多份长期任务消耗资源并重复提醒。
- 建议：唯一周期任务加取消入口。
- 验证：连续开启多次后查询任务，只有一个活动实例；关闭后无活动实例。

### P2：通知按钮没有实现稍后提醒

- 位置：WaterWorker.kt:47、AlarmReceiver.java:21、LongRunningService.java:19。
- 依据：按钮链路只再次调度 AlarmReceiver，没有发送通知；后台普通 Service 还有平台执行限制。
- 影响：用户点击按钮后不会收到预期的稍后提醒。
- 建议：唯一一次性 WorkRequest；旧组件仍被通知动态引用，不能当作孤立死代码删除。
- 验证：应用后台点击按钮，只产生一个稍后任务并发出通知。

### P2：点击通知会清空编辑任务栈（本次补充）

- 位置：WaterWorker.kt:30。
- 依据：Intent 同时设置 FLAG_ACTIVITY_NEW_TASK 和 FLAG_ACTIVITY_CLEAR_TASK。
- 影响：正在编辑尚未保存的密码时点通知，当前任务及编辑页面可能被移除。
- 建议：通知导航不清空密码编辑栈，必要时使用明确的返回栈。
- 验证：编辑草稿后点击通知，返回时草稿和编辑页面仍存在。

### P2：安全库没有导出、导入或主密码恢复入口

- 位置：MainActivity、VaultRepository 当前 API。
- 依据：本次只落实 P1 修复；系统备份已经禁用，加密库没有用户可用的导出与恢复界面。
- 影响：卸载、设备丢失会丢失本机库；忘记主密码后无法解开新库，旧明文也不包含迁移后的更新。
- 建议：按 plan.md 单独实施加密导入导出和恢复密钥；目前使用界面明确提示主密码须离线记住。
- 验证：后续在全新安装中恢复完整记录，而不是仅测试同设备读取。

### P2：设备端密码库及解锁交互尚未验证

- 位置：VaultRepositoryTest（androidTest）、MainActivity、VaultApplication。
- 依据：设备拒绝安装独立测试包；JVM 验证不能证明 Android 文件系统和生命周期行为完全一致。
- 影响：仍需验证后台锁定、旋转草稿、进程重建、取消解锁、实际原子替换及迁移耗时。
- 建议：在允许安装的独立测试设备或模拟器执行设备回归。
- 验证：独立包名执行 Android 测试，使用唯一 cache 子目录，不访问真实用户数据。

## 仍存在的 P3

- activity_main.xml 两个浮动按钮的 contentDescription 为 null；屏幕阅读器无法说明用途。添加语义描述并做无障碍检查。
- XML 和代码有硬编码中文文案；应迁移至 strings.xml 后检查本地化显示。
- Manifest 的 USE_BIOMETRIC 位于 application 后；Lint 报 ManifestOrder，整理声明顺序即可。没有据此认定权限失效。
- Adapter 仍使用 notifyDataSetChanged，未使用差分刷新；当前小库无已验证的性能事故，后续按测量结果决定优化。
- 依赖和 targetSdk 有更新提示；提示本身不等于安全漏洞或 P1，不在本轮升级技术栈。

## 平台依据

- [Android 系统备份](https://developer.android.com/identity/data/autobackup)
- [Android 通知权限](https://developer.android.com/develop/ui/compose/notifications/notification-permission)
- [Android 安全剪贴板](https://developer.android.com/privacy-and-security/risks/secure-clipboard-handling)
- [Android 剪贴板焦点与读取限制](https://developer.android.com/reference/android/content/ClipboardManager)
- [Android 加密 API](https://developer.android.com/privacy-and-security/cryptography)
- [OWASP 密码派生工作因子](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
