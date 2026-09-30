# CommonUtils 当前进度

## 本轮范围与决定

- MainActivity 改为工具首页，密码列表迁移至 PasswordActivity；首页提供密码管理器、喝水提醒与记账入口。记账按用户选择仅提示「功能开发中」。首页、密码列表、新增/编辑页采用 Material 3 浅紫色主题，饮水提醒功能保持现状。
- 按用户最新要求移除全部主密码设置和解锁弹窗，默认通过 Android Keystore 自动打开本机加密 vault-device.dat；当前不要求身份验证。主密码 v1 仅保留兼容代码，旧 vault.dat 不覆盖、不回退到旧明文。
- 用户选择保留 need.json：迁移后不修改、不删除，残余明文 P1 保留。
- 实机只读检查确认原 need.json 为 4375 字节、20 条完整记录，未发现 vault.dat。此前空白列表来自未解锁时隐藏数据，没有证据表明记录被清空。
- plan.md 的 Argon2id、生物识别、恢复密钥和导入导出仍属于后续方案。本机 Keystore 加密文件依赖当前安装的密钥，卸载后不可恢复。

## 待验证

- 工具首页导航、密码列表及新增/编辑页的 Material 3 真机渲染、长文本/大字体和键盘避让尚未验证；本轮仅构建与静态检查，不安装或替换真实应用。
- 设备端验证待解除安装限制：已连接 Android 13 手机拒绝独立测试包安装，原应用没有被替换，本轮对原文件仅进行只读检查，未改写。
- 剪贴板敏感预览、系统复制时间匹配和焦点恢复后的实际清理行为待设备验证；PasswordClipTest 已编译。进程终止或后台无法读取时，不能保证严格 30 秒清理。

## 最近完成

- 2026-09-30 23:15：完成工具首页与独立密码列表、新增/编辑页的 Material 3 界面改造：三个图标入口、圆角搜索与密码卡片、复制图标、记录数量、新增悬浮按钮、轮廓输入框、随机/复制及保存按钮。保留明文显示、稳定 ID 编辑删除、自动读取、草稿和敏感剪贴板逻辑。同步 AGENTS.md、README.md；修正 README 的实际 APK 路径。
- 2026-09-30 22:40：按用户要求恢复主页密码明文显示，移除列表固定掩码；点击复制、搜索及编辑删除保持现有行为。AGENTS.md 和 README.md 同步显示规则。

- 2026-09-30 22:32：移除全部主密码弹窗及其输入逻辑；自动打开本机 Keystore 加密库并迁移旧记录，新增读取状态和失败提示。保留全部原文件，已有旧主密码库及密钥丢失时拒绝覆盖。AGENTS.md、README.md、REVIEW.md 同步当前决定。
- 2026-09-30 22:12：两项 P2 的代码与 JVM 回归完成：SecureRandom、唯一字符集、默认 16 位密码；敏感剪贴板、30 秒条件清理、复制身份及时间核对、焦点恢复重试；敏感标记兼容 Android 11/12。新增 9 项 JVM 测试全部通过，Android 集成仍待实机验证。
- 2026-09-30 21:37：完成 P1 实现与二次审核；最终 JVM 回归确认稳定 ID、原子写入、加密及迁移失败保护。按用户决定保留 need.json；解锁 UI、后台锁定和 Android 文件系统行为仍待设备验证。复查报告见 REVIEW.md。
- 2026-09-30 21:37：补充零字节旧文件拒绝、严格 UTF-8 与尾随内容检查、异步加载版本检查及新增草稿重复提交幂等保护；新增独立包名设备测试入口 gradle/isolated-test.init.gradle。
- 2026-09-30 21:25：JVM 回归确认加密、错误密码、篡改、稳定 ID CRUD、完整旧库迁移、坏记录拒绝、失败写入保留原文件及旧文件保留；具体实现见 crypto/、data/。
- 2026-09-30 21:18：规范统一至 AGENTS.md，CLAUDE.md 改为兼容入口；README.md 记录实际主密码解锁和迁移方式。

## 最近验证

- 2026-09-30 23:15：最终 assembleDebug、testDebugUnitTest、lintDebug 通过；30 项 JVM 测试零失败、零错误，Lint 零错误、15 项警告，改动页面无 Lint 警告；Manifest 与布局 XML 解析、git diff --check 通过。生成 APK 包名 com.luxu.commonutils，路径 app/build/intermediates/apk/debug/app-debug.apk。设备端 UI 和交互未验证，未安装真实应用。
- 2026-09-30 22:40：assembleDebug、git diff --check 通过；静态核对列表绑定真实 password，布局未设置密码变换。实机显示未验证。

- 2026-09-30 22:36：最终 assembleDebug、30 项 JVM 测试、lintDebug 通过；新增 Android Keystore 测试编译通过但未执行。APK 包名为 com.luxu.commonutils。再次只读核对 need.json 的 SHA-256 与排查初始值一致，未改写原文件。

- 2026-09-30 22:32：assembleDebug、testDebugUnitTest、lintDebug、assembleDebugAndroidTest 通过；新增 4 项本机格式 JVM 回归通过，覆盖随机 IV、篡改、自动迁移及 ID 编辑删除、旧文件保留、旧主密码库阻断、缺失或错误密钥不覆盖。静态搜索未发现主密码弹窗文案或 showUnlock。Android Keystore 实机测试尚未运行。
- 2026-09-30 22:32：ADB run-as 只读检查真实应用，need.json 严格 JSON 解析成功，20 条记录均包含必需字段；未输出凭据内容，未写入真实应用数据。

- 2026-09-30 22:17：assembleDebug、testDebugUnitTest、lintDebug 均通过，git diff --check 通过；静态引用确认 showUnlock 只从新增按钮触发。实机弹窗行为未验证，设备安装限制仍存在。
- 2026-09-30 22:12：最终 assembleDebug、testDebugUnitTest、lintDebug、assembleDebugAndroidTest 全部成功；新增 9 项 JVM 测试通过，全套 26 项测试零失败；Lint 零错误、33 项既有警告，新增 InlinedApi 警告已修正；git diff --check 通过。Android 测试仅编译，未执行，未重复尝试手机安装。
- 2026-09-30 21:37：最终 assembleDebug、testDebugUnitTest、lintDebug、assembleDebugAndroidTest 均成功；17 项 JVM 测试零失败、零错误（16 项业务回归及 1 项原样例）；Lint 零错误、33 项警告；Android 测试仅编译，未执行；输出 APK 包名核对为 com.luxu.commonutils，git diff --check 通过。
- 2026-09-30 21:25：阶段版本 assembleDebug、testDebugUnitTest、lintDebug 全部通过。
- 2026-09-30 21:20：独立包名 APK 经 metadata 核对；ADB 安装返回 INSTALL_FAILED_USER_RESTRICTED，设备测试未执行，没有重试或绕过。
- 2026-09-30 21:15：connectedDebugAndroidTest 离线执行器依赖缺失，测试 APK 编译成功；JVM 可执行完整迁移测试，Android 测试尚未执行。
- 2026-09-30 21:09：7 项新 JVM 加密与原子写入测试通过，原样例测试通过。

## 后续事项

- P1 残余：need.json 保留明文，仅在用户改变决定后处理。
- P2：通知权限、唯一周期任务、稍后提醒、通知导航清空任务栈、加密导入导出与恢复。
- P3：无障碍描述、字符串资源化、Manifest 顺序和有依据的列表刷新优化。
- 未确认：Android Keystore 自动打开及实际迁移、后台会话关闭、旋转草稿和进程重建行为。
