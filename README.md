# CommonUtils

Android 本地工具应用，包含密码管理器和独立的饮水提醒页面。最低支持 Android 11（API 30）。

首页提供「密码管理器」「喝水提醒」「记账」三个图标入口；记账点击后提示「功能开发中」。首页与密码列表使用 Material 3 浅紫色主题，密码页包含圆角搜索栏、账户卡片、复制按钮和「新增密码」悬浮按钮；返回按钮回到工具首页。

新增与编辑密码页采用同一主题，提供圆角轮廓输入框、随机密码与复制按钮，以及保存按钮；表单可滚动，适配键盘弹出后的可用空间。

## 密码库使用

进入或返回密码管理器页面时自动读取密码列表，不设置主密码，也不弹出主密码窗口。已有 need.json 在完整校验后迁移到本机加密 vault-device.dat，旧文件不修改、不删除；界面提示旧文件仍含明文。读取失败会明确提示，不回退为空库。

密码列表直接显示明文密码，点击记录复制密码，长按编辑或删除；搜索后的操作按记录 ID 定位。密码页面禁止普通截图及录屏，后台关闭会话，返回时自动打开。当前不要求身份验证，能打开应用的人可以访问记录。

随机密码默认 16 位，使用 SecureRandom 从无重复的大小写字母、数字和 `!` 中生成。复制密码会附加敏感标记，并在 30 秒后尝试清理；仅清除归属标记和复制时间仍匹配的内容，保留后来复制的数据。后台无法读取剪贴板时，重新获得焦点后再检查。进程被终止时无法保证应用的定时清理执行；敏感标记只用于隐藏预览，不限制获准应用读取。

本机加密使用 Android Keystore 不可导出的 AES-256 密钥和 AES-GCM。密钥绑定当前应用安装，卸载后无法恢复本机加密文件；应用禁用系统云备份和设备迁移。已有主密码格式 vault.dat 不覆盖、不自动回退到旧明文数据，需单独迁移。

## 构建和验证

安装适配当前 Gradle/AGP 的 JDK 和 Android SDK，在未提交的 `local.properties` 配置 `sdk.dir`，并设置 `JAVA_HOME`。

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

当前构建生成的 APK 位于 `app/build/intermediates/apk/debug/app-debug.apk`。Android 密码库回归测试使用独立包名 `com.luxu.commonutils.reviewtest` 和唯一 cache 子目录，不读写用户的密码文件：

```powershell
.\gradlew.bat -I gradle/isolated-test.init.gradle connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.luxu.commonutils.VaultRepositoryTest"
```

独立测试构建会临时改变本次输出 APK 的包名；交付正常 APK 前重新执行不带 `-I` 的 `assembleDebug`。

项目约定见 AGENTS.md，修复状态、待办及验证限制见 ROADMAP.md，长期设计见 plan.md。
