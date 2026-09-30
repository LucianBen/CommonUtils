# CommonUtils 项目约定

## 项目定位与目录

Android 本地密码管理器，另含饮水提醒。项目当前使用方式见 README.md，当前修复与验证状态见 ROADMAP.md；plan.md 是长期安全改造设计，不代表已经实现。

- `app/src/main/java/com/luxu/commonutils/data/`：严格解析、稳定 ID、密码库仓储和原子文件提交。
- `app/src/main/java/com/luxu/commonutils/crypto/`：主密码派生、版本化 AES-GCM 密码库格式。
- `activity/`、`base/`：MainActivity 是工具首页，PasswordActivity 是密码列表页，ClockActivity 是饮水提醒页；密码页面保护及 VaultApplication 管理进程内解锁会话。
- `app/src/main/res/`：布局、主题和系统备份排除规则。
- `app/src/test/`：JVM 加密与原子写入回归测试。
- `app/src/androidTest/`：Android 密码库迁移及 CRUD 回归测试。
- `gradle/wrapper/`、根目录及 app/build.gradle：构建配置。
- `gradle/isolated-test.init.gradle`：设备回归使用独立包名的构建入口，不改变生产配置。

## 安全与工程边界

- 用户要求暂时取消全部主密码弹窗：默认通过 Android Keystore 自动打开本机加密库 `vault-device.dat`，不要求主密码或身份验证；密码页面启用 FLAG_SECURE。
- 用户要求主页面直接显示明文密码，不使用掩码；编辑页密码输入与剪贴板保护遵循现有行为。
- 工具首页提供密码管理器、喝水提醒、记账入口；记账当前仅提示「功能开发中」。首页、密码列表及新增/编辑页使用独立的 Material 3 浅色主题，不影响饮水提醒页面主题。
- 本机格式使用不可导出的 256 位 AES 密钥及 AES-GCM 随机 IV，文件头纳入认证。已有主密码格式 `vault.dat` 保留兼容代码；没有本机库时发现该文件必须明确报错，不得回退到过期旧记录。已有本机库丢失密钥时禁止重建或覆盖。
- 所有磁盘操作和密钥派生在 IO 线程执行；Repository 操作按稳定 ID，禁止传 Adapter position。
- 写入先加密至同目录临时文件、刷盘、解密及逐字段验证，再原子替换；不得降级到非原子覆盖。
- 读取、解密、迁移失败必须显式报错，禁止跳过坏记录或回退为空库。
- 用户已明确要求暂时保留旧 `need.json`：不得删除或修改；迁移后界面提示残余明文风险。整个应用数据排除系统备份及设备迁移。
- 不在源码、构建脚本、日志或错误信息中写入密钥、Token、密码。
- 随机密码使用 SecureRandom 和无重复字符集，默认 16 位；复制密码设置敏感标记，30 秒到期仅清理归属标记及复制时间仍匹配的内容。后台无法核对时在重新获得焦点后重试，禁止盲目清空剪贴板。
- `local.properties` 只保存本机 SDK 路径，不提交。
- 仅修改当前任务相关文件；保留用户已有的 IDE 修改。CLAUDE.md 仅指向本文件。

## 验证入口

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat connectedDebugAndroidTest
```

密码库改动必须覆盖错误密码、篡改、迁移失败、按 ID 编辑/删除和写入失败。设备测试不得触碰用户密码库；使用独立测试构建的唯一 cache 子目录，不使用 filesDir。已安装真实应用的设备应使用独立 applicationId 的测试构建，避免替换用户应用。
