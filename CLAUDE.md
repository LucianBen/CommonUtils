# CommonUtils 项目规范

## 项目结构

- `app/`：Android 应用模块。
- `app/src/main/`：应用源码与资源。
- `app/src/test/`：本地单元测试。
- `app/src/androidTest/`：设备端测试。
- `gradle/wrapper/`：Gradle Wrapper 配置。

## 修改约定

- 仅修改与当前需求直接相关的文件，不顺带重构或格式化。
- Android 构建版本统一在根目录和 `app/build.gradle` 中维护。
- `local.properties` 仅保存本机 Android SDK 路径，不提交版本控制。
- 不在源码、构建脚本或日志中写入密钥、Token、密码。

## 验证命令

- 编译 Debug APK：`.\gradlew.bat assembleDebug`
- 运行本地单元测试：`.\gradlew.bat testDebugUnitTest`

完成构建配置或源码修改后，至少运行与改动相关的上述命令。
