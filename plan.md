# CommonUtils 安全与可靠性改造计划

## 1. 改造目标

将当前明文 JSON 密码管理器改造成具备以下能力的本地密码库：

- 密码数据落盘后始终为密文。
- 当前手机可使用生物识别快速解锁。
- 更换手机后可通过“加密备份文件 + 主密码”恢复。
- 主密码遗忘时可通过离线恢复密钥恢复。
- 搜索、编辑、删除不会因列表位置变化而操作错记录。
- 数据损坏、迁移失败和写入失败不会静默清空密码库。
- 通知提醒不会重复创建，并正确处理通知权限。

## 2. 范围与非目标

### 本次范围

- 密码记录稳定 ID 与 CRUD 正确性。
- AES-256-GCM 密码库加密。
- Android Keystore 本机密钥保护。
- 主密码派生密钥与跨设备恢复。
- 离线恢复密钥。
- 旧版 `need.json` 明文数据迁移。
- 加密密码库导出和导入。
- 生物识别解锁、截图保护和安全剪贴板。
- WorkManager 提醒任务修复。
- 与上述改造直接相关的测试。

### 暂不包含

- 账号系统。
- 服务器端存储。
- 多设备实时同步和冲突合并。
- 浏览器插件、自动填充服务。
- 与需求无关的 UI 重做或全项目架构重构。

后续如果增加云同步，服务端只能保存端到端加密后的密码库，不能接触主密码、恢复密钥或明文数据。

## 3. 目标密钥结构

密码库不直接使用 Android Keystore 密钥加密，否则换机后无法恢复。采用三层结构：

```text
主密码 --Argon2id--> MasterKey ----\
                                   \
恢复密钥 ----------> RecoveryKey ----> 加密包装 VaultKey
                                   /
本机 Keystore -----> DeviceKey ----/

VaultKey --AES-256-GCM--> 加密整个密码库
```

### 密钥职责

- `VaultKey`
  - 使用安全随机源生成的 256 位密钥。
  - 直接负责加密和解密密码库。
  - 不以明文形式写入磁盘、日志或导出文件。
- `MasterKey`
  - 由主密码通过 Argon2id 派生。
  - 用于包装 `VaultKey`，支持跨设备恢复。
  - 文件中只保存随机 Salt 和 KDF 参数，不保存主密码。
- `RecoveryKey`
  - 首次初始化时随机生成，不从主密码推导。
  - 同样用于包装 `VaultKey`。
  - 只向用户展示一次，要求离线保存。
- `DeviceKey`
  - 由 Android Keystore 生成且不可导出。
  - 用于本机生物识别或设备凭据快速解锁。
  - 换机后重新生成，不参与跨设备传输。

### 设计收益

- 修改主密码时只需重新包装 `VaultKey`，不用重新加密全部密码。
- 新手机使用主密码或恢复密钥解开 `VaultKey`。
- 旧手机 Keystore 密钥丢失不会导致加密备份永久不可读。
- 导出文件泄漏后，攻击者仍需破解主密码或恢复密钥。

## 4. 加密文件格式

新增带版本号的密码库格式，建议文件名为 `vault.dat`。

```json
{
  "formatVersion": 1,
  "kdf": {
    "algorithm": "Argon2id",
    "salt": "Base64",
    "memoryKiB": 65536,
    "iterations": 3,
    "parallelism": 4
  },
  "masterWrappedVaultKey": {
    "iv": "Base64",
    "ciphertext": "Base64"
  },
  "recoveryWrappedVaultKey": {
    "iv": "Base64",
    "ciphertext": "Base64"
  },
  "deviceWrappedVaultKey": {
    "iv": "Base64",
    "ciphertext": "Base64"
  },
  "vault": {
    "iv": "Base64",
    "ciphertext": "Base64"
  }
}
```

约束：

- 所有加密操作使用 `AES/GCM/NoPadding`。
- 每次加密必须生成新的 12 字节随机 IV，禁止复用。
- AES-GCM 认证标签随密文保存。
- `formatVersion`、应用 ID 和加密用途作为 Associated Data，防止字段替换。
- 导出文件不包含 `deviceWrappedVaultKey`，避免携带无用的设备绑定数据。
- KDF 参数写入文件，未来升级参数时仍可读取旧数据。
- Argon2id 参数在实现时以 64 MiB、3 次迭代为基线，并在最低配置目标设备上校准解锁耗时。

## 5. 目录和文件调整

实施前先更新 `CLAUDE.md`，补充新目录职责、密钥处理规范和验证命令。

### 计划新增

```text
app/src/main/java/com/luxu/commonutils/
├── crypto/
│   ├── VaultCrypto.kt
│   ├── MasterKeyDeriver.kt
│   ├── DeviceKeyStore.kt
│   └── RecoveryKeyManager.kt
├── data/
│   ├── VaultRepository.kt
│   ├── VaultFileCodec.kt
│   ├── VaultModels.kt
│   └── LegacyVaultMigrator.kt
└── transfer/
    └── VaultTransferManager.kt
```

职责：

- `VaultCrypto.kt`
  - AES-GCM 加解密和密钥包装。
  - 不负责文件、UI 或业务数据操作。
- `MasterKeyDeriver.kt`
  - Argon2id 参数管理和主密码密钥派生。
  - 使用可审计且持续维护的 Argon2id 实现，版本固定在 Gradle 配置中。
- `DeviceKeyStore.kt`
  - 创建、读取和失效 Android Keystore 密钥。
  - 处理生物识别或锁屏变更导致的密钥失效。
- `RecoveryKeyManager.kt`
  - 生成高熵恢复密钥。
  - 负责显示格式、校验和恢复流程。
- `VaultRepository.kt`
  - 提供 `load/add/update/delete/search`。
  - 以稳定 ID 操作记录，不接受列表 position。
  - 所有磁盘操作在 IO 线程执行。
- `VaultFileCodec.kt`
  - 负责版本化加密文件的序列化、解析和格式升级。
- `LegacyVaultMigrator.kt`
  - 识别旧版明文 `need.json`。
  - 负责安全迁移和迁移结果验证。
- `VaultTransferManager.kt`
  - 通过 Storage Access Framework 导出和导入加密文件。
  - 不申请广泛存储权限。

### 计划修改

- `CLAUDE.md`
  - 先增加目录规范、安全约束和测试命令。
- `app/build.gradle`
  - 添加经过评估并固定版本的 Argon2id 依赖。
  - 添加测试所需依赖。
- `PasswordBean.kt`
  - `id: Int` 改为不可变的 UUID 字符串。
  - 字段尽量改为不可变 `val`。
- `ReadWriteFile.kt`
  - 由 `VaultRepository` 替代。
  - 移除全局 `lateinit filePath/jsonArray`。
  - 在迁移完成并验证后再移除旧实现。
- `MainActivity.kt`
  - 未解锁时不读取、不创建 Adapter、不展示密码。
  - 使用稳定 ID 编辑和删除。
  - 修复搜索后操作错记录的问题。
  - 删除后同步完整数据集。
- `AddPsdActivity.kt`
  - 使用记录 ID，而不是列表 position。
  - 增加网站、账户和密码的必要校验。
  - 保存失败时保留页面内容并显示明确错误。
- `PassListAdapter.kt`
  - 改为 `ListAdapter + DiffUtil`。
  - 监听器通过构造函数传入。
  - 点击时检查 `bindingAdapterPosition != NO_POSITION`。
  - 默认遮挡密码，仅按用户操作临时显示。
- `CommonUtils.kt`
  - 使用 `SecureRandom` 生成密码。
  - 使用无重复字符集，默认长度至少 16 位。
  - 剪贴板内容设置 `EXTRA_IS_SENSITIVE`。
  - 定时清除剪贴板时，先确认内容仍是本应用复制的密码。
- `BaseActivity.kt`
  - 对密码相关页面启用 `FLAG_SECURE`。
  - 修复可空事件强制解包。
- `AndroidManifest.xml`
  - 明确排除密码库文件备份，或只允许满足加密能力时备份加密文件。
  - 将所有 `uses-permission` 放在 `application` 前。
  - 明确声明 Receiver 和 Service 的 `exported`。
- `backup_rules.xml`
  - 排除旧明文文件和本机设备包装数据。
- `data_extraction_rules.xml`
  - 排除旧明文文件和不可跨设备恢复的本机数据。
- `ClockActivity.kt`
  - 调度前申请并检查通知权限。
  - 使用 `enqueueUniquePeriodicWork`。
  - 提供关闭提醒入口。
- `WaterWorker.kt`
  - 删除 `SuppressLint("MissingPermission")`。
  - 无权限时返回明确结果，不尝试发送通知。
- `AlarmReceiver.java`
  - 如果提醒和稍后提醒全部迁移到 WorkManager，则移除该组件。
- `LongRunningService.java`
  - 使用 WorkManager 替代旧式 Service + AlarmManager 链路后移除。
- 布局和字符串资源
  - 修复 `activity_main.xml` 的纵向约束闭环。
  - 增加无障碍描述。
  - 将硬编码文本移入 `strings.xml`。

## 6. 旧数据迁移流程

迁移必须保证失败时原数据仍可恢复：

1. 检查是否存在新版 `vault.dat`。
2. 如果只有旧版 `need.json`：
   - 要求用户创建主密码。
   - 读取旧 JSON，并对每一条记录做完整字段校验。
   - 将旧 `Int/Long` ID 转换为 UUID。
   - 生成 `VaultKey` 和恢复密钥。
   - 加密后写入同目录临时文件。
   - 重新读取临时文件并完整解密。
   - 逐条比较迁移前后记录数量和字段值。
   - 通过后原子替换为 `vault.dat`。
3. 迁移失败：
   - 不覆盖旧文件。
   - 不显示为空密码库。
   - 向用户显示可重试的明确错误。
4. 迁移成功：
   - 进入新密码库。
   - 提示用户验证并离线保存恢复密钥。

删除旧明文 `need.json` 属于不可逆数据操作。实施该步骤前需要主人单独确认；未获确认时，只能在迁移成功后保留旧文件并明确提示其安全风险。

## 7. 换机流程

### 旧手机导出

1. 要求主密码或生物识别重新认证。
2. 生成只包含密文、主密码包装密钥、恢复密钥包装密钥和格式参数的导出文件。
3. 使用系统文件选择器保存。
4. 明确提示：
   - 导出文件仍应妥善保管。
   - 不要把恢复密钥和导出文件存放在同一位置。

### 新手机导入

1. 使用系统文件选择器选择导出文件。
2. 校验文件格式、版本、长度和 AES-GCM 认证标签。
3. 用户输入主密码，或输入恢复密钥。
4. 解开 `VaultKey` 并解密密码库。
5. 完整验证所有记录后写入本机 `vault.dat`。
6. 在新手机生成新的 Android Keystore `DeviceKey`。
7. 用新 `DeviceKey` 重新包装 `VaultKey`，启用本机生物识别解锁。

### 主密码遗忘

- 有恢复密钥：允许恢复并设置新主密码。
- 没有恢复密钥：无法恢复，不提供绕过或后门。

## 8. 生物识别和页面保护

- 应用启动后先进入锁定状态。
- 认证成功前：
  - 不读取密码库。
  - 不创建包含密码的 RecyclerView Adapter。
  - 不在后台任务、日志或异常信息中暴露密码。
- 用户取消认证后停留在锁定页，不自动循环弹出认证。
- 应用进入后台一定时间后自动重新锁定。
- 密码页面启用 `FLAG_SECURE`，阻止普通截图、录屏和非安全屏幕投射。
- 密码字段默认遮挡，按住或主动点击后临时显示。

## 9. 数据正确性修复

- 所有记录使用 UUID，不再使用毫秒时间戳或 Adapter position。
- Repository API 示例：

```kotlin
suspend fun add(entry: PasswordEntry): Result<Unit>
suspend fun update(entry: PasswordEntry): Result<Unit>
suspend fun delete(id: String): Result<Unit>
suspend fun load(): Result<List<PasswordEntry>>
```

- 搜索仅产生显示列表，不改变持久化索引。
- 编辑和删除始终按 UUID 定位。
- 文件更新采用：
  - 读取并验证现有文件。
  - 在内存中生成新内容。
  - 写入临时文件。
  - 刷盘。
  - 重新读取并验证。
  - 原子替换正式文件。
- 不允许使用空列表掩盖解析或解密失败。

## 10. 通知提醒修复

- Android 13 及以上在用户主动开启提醒时申请 `POST_NOTIFICATIONS`。
- 用户拒绝权限时不创建提醒任务，并给出设置入口。
- 使用固定任务名和 `enqueueUniquePeriodicWork`，保证只有一个周期提醒。
- 支持更新提醒周期和关闭提醒。
- “稍后提醒”使用唯一一次性 WorkRequest，不再通过 Receiver 启动后台 Service。
- 删除不再使用的 Receiver、Service 和 Manifest 声明需要主人在实施阶段单独确认。

## 11. 测试计划

### 单元测试

- AES-GCM 正常加解密。
- 密文、IV、Associated Data 任一字节被修改时解密失败。
- 同一明文重复加密产生不同密文。
- 主密码正确时可以解开 `VaultKey`。
- 错误主密码不能解开 `VaultKey`。
- 恢复密钥可以恢复 `VaultKey`。
- 修改主密码后旧密码失效、新密码有效。
- UUID CRUD。
- 搜索后编辑和删除仍操作正确记录。
- JSON 损坏、密文损坏、版本未知时返回明确错误。
- 随机密码长度、字符范围和基本唯一性。

### 迁移测试

- 空旧密码库迁移。
- 正常旧密码库迁移。
- 包含中文、特殊字符和长备注的数据迁移。
- 旧时间戳 ID 溢出数据迁移。
- 单条记录损坏时迁移停止且不覆盖原文件。
- 写入中断时原文件仍可读取。
- 迁移完成后逐字段一致。

### Android 测试

- 首次启动创建主密码。
- 生物识别成功、失败和取消。
- 应用进后台后重新锁定。
- 新安装 Android 13+ 的通知权限授权与拒绝。
- 周期任务重复点击后仍只有一个实例。
- 导出、卸载模拟、新安装导入恢复。
- 主密码恢复和恢复密钥恢复。
- 截图保护和剪贴板敏感标记。

### 回归验证

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat lintDebug
```

## 12. 分阶段实施

### 阶段 1：数据正确性

- 更新 `CLAUDE.md` 目录规范。
- 引入 UUID。
- 新建 `VaultRepository`。
- 修复搜索后编辑和删除错记录。
- 增加 Repository 单元测试。

验收标准：

- 任意搜索状态下编辑和删除都命中正确 UUID。
- 文件错误不再表现为空密码库。

### 阶段 2：加密密码库

- 实现 AES-GCM、Argon2id 和密钥包装。
- 实现版本化 `vault.dat`。
- 实现原子写入和损坏检测。
- 完成密码学单元测试。

验收标准：

- 磁盘上不存在密码、账户或备注明文。
- 任意密文篡改都能被检测。

### 阶段 3：旧数据迁移

- 实现旧 JSON 识别和迁移。
- 增加迁移前后逐字段校验。
- 展示并确认保存恢复密钥。

验收标准：

- 原密码完整可读。
- 模拟迁移失败不会覆盖旧文件。
- 删除旧明文文件前单独取得主人确认。

### 阶段 4：解锁和界面保护

- 增加主密码解锁。
- 增加 Keystore 生物识别快速解锁。
- 增加自动锁定、密码遮挡、`FLAG_SECURE` 和安全剪贴板。

验收标准：

- 未认证时内存和界面中不加载密码列表。
- 取消认证不会循环弹窗。
- 密码复制预览被标记为敏感。

### 阶段 5：换机导入导出

- 实现加密文件导出。
- 实现主密码和恢复密钥导入。
- 在新设备重新创建 Keystore 包装。

验收标准：

- 导出文件不包含明文或设备密钥。
- 在没有旧手机 Keystore 的环境中仍能通过主密码恢复。

### 阶段 6：提醒与工程质量

- 修复通知权限和重复 WorkManager。
- 移除确认不再使用的 Service/Receiver。
- 修复 Adapter、布局、字符串、无障碍和 Lint 警告。
- 补齐端到端回归测试。

验收标准：

- 重复点击只存在一个提醒任务。
- `assembleDebug`、单元测试、Android 测试和 Lint 全部通过。

## 13. 实施前确认项

以下选择会影响最终行为，开始编码前需要确认：

1. 换机第一版采用“手动加密文件导出/导入”，暂不做云同步。
2. 是否接受每次首次初始化必须创建主密码。
3. 恢复密钥采用可复制文本、二维码，还是两者都提供。
4. 应用进入后台后多久自动锁定。
5. 是否允许截图；本计划默认所有密码页面禁止截图。
6. 迁移验证成功后是否删除旧明文 `need.json`。
7. 是否移除旧 `AlarmReceiver` 和 `LongRunningService`。

## 14. 完成定义

只有同时满足以下条件才视为改造完成：

- 旧密码数据迁移后逐字段一致。
- 磁盘、导出文件、日志和异常中没有密码明文。
- 搜索状态下编辑和删除不会操作错记录。
- 主密码和恢复密钥均可在新设备恢复密码库。
- Keystore 密钥丢失不会影响主密码恢复。
- 错误主密码、损坏密文和未知格式不会显示为空密码库。
- 通知权限和唯一周期任务行为正确。
- 所有新增核心逻辑有自动化测试。
- 编译、测试和 Lint 验证通过。
