# Android 远程屏幕操作同步应用 - 需求分析与开发计划（修订版）

## 一、项目概述

### 1.1 项目名称
**RemoteTouch** - Android 设备间实时屏幕操作同步应用

### 1.2 核心功能
实现将一台 Android 设备（Client 端）的屏幕触摸操作（滑动、点击等）实时通过网络（WiFi）传输到另一台 Android 设备（Server 端）并执行。Client 端需要全屏显示操作窗口，并根据双方屏幕尺寸自动映射坐标。

### 1.3 技术特点
- 仅需传输操作事件数据，不传输屏幕画面（初期）
- 支持 WiFi 网络通信
- 低延迟、高实时性
- Server 端支持后台运行，Client 端前台运行
- **支持屏幕尺寸自适应坐标映射**
- **Client 端全屏窗口显示**
- **后续支持 Server 端截屏传输作为 Client 背景**

---

## 二、需求分析

### 2.1 用户场景
1. **场景 1**: 用户用手机远程控制平板玩游戏
   - 平板作为 Server 端，运行游戏
   - 手机作为 Client 端，显示全屏操作窗口
   - 用户在 Client 窗口上点击滑动，操作同步到 Server 执行
   - 坐标自动根据屏幕尺寸映射

2. **场景 2**: 首次使用配置
   - 用户选择设备模式（Server/Client）
   - Client 端绑定 Server 端 IP 地址
   - 建立连接，获取 Server 端屏幕尺寸
   - 开始同步操作

3. **场景 3**: 后续使用
   - 直接启动进入预设模式
   - 支持修改模式配置

### 2.2 功能需求

#### 2.2.1 模式选择模块
- **首次启动**: 显示模式选择界面（Server/Client）
- **后续启动**: 直接进入上次选择的模式
- **模式切换**: 提供设置入口，可随时修改模式
- **配置存储**: 使用 SharedPreferences 或 DataStore 存储用户选择

#### 2.2.2 Server 端功能
1. **网络服务**
   - 启动 TCP 服务器监听端口（默认 8899）
   - 显示当前设备 IP 地址（支持 WiFi IP 和热点 IP）
   - 监听并接收 Client 端连接
   - 发送 Server 端屏幕尺寸给 Client

2. **后台运行**
   - 使用 Foreground Service 保持服务运行
   - 通知栏显示服务状态
   - 防止被系统杀死

3. **事件执行**
   - 解析接收到的触摸事件数据包
   - 将 Client 坐标映射到 Server 屏幕坐标
   - 使用 AccessibilityService 模拟触摸操作
   - 支持事件类型：点击、长按、滑动、多点触控

4. **连接管理**
   - 显示已连接的 Client 信息
   - 支持断开/重连机制
   - 心跳检测保持连接

5. **截屏功能（后续）**
   - 定期截取屏幕
   - 压缩图片并发送给 Client
   - 作为 Client 端背景显示

#### 2.2.3 Client 端功能
1. **连接配置**
   - IP 地址输入界面（首次绑定）
   - 端口配置（默认端口）
   - 保存服务器配置信息
   - 快速重连功能

2. **全屏窗口显示**
   - **全屏沉浸式界面**
   - **可自定义背景颜色（初始随机颜色或图片）**
   - **后续支持显示 Server 端传输的截屏**
   - **显示连接状态**

3. **事件捕获**
   - **直接在全屏窗口上捕获触摸事件（无需 AccessibilityService）**
   - 使用 OnTouchListener 监听触摸
   - 捕获事件类型：ACTION_DOWN, ACTION_UP, ACTION_MOVE, ACTION_POINTER 等

4. **坐标映射**
   - **获取 Client 端屏幕尺寸**
   - **获取 Server 端屏幕尺寸（连接时获取）**
   - **计算坐标映射比例**
   - **将 Client 坐标转换为 Server 坐标**

5. **事件发送**
   - 将映射后的触摸事件序列化为数据包
   - 通过 Socket 实时发送到 Server 端
   - 支持事件队列和批量发送（优化性能）
   - 断线重连机制

6. **连接状态**
   - 显示连接状态（已连接/断开/连接中）
   - 连接失败提示
   - 自动重连功能

### 2.3 非功能需求

#### 2.3.1 性能要求
- **延迟**: 操作延迟 < 50ms（局域网内）
- **吞吐量**: 支持高频事件传输（游戏场景）
- **稳定性**: 长时间运行不掉线

#### 2.3.2 安全要求
- 支持连接密码/验证码（可选）
- 仅允许局域网内连接
- 数据传输加密（可选）

#### 2.3.3 兼容性要求
- Android 7.0+ (API 24+)
- 适配不同屏幕分辨率
- 支持平板和手机设备

---

## 三、技术架构设计

### 3.1 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                    应用架构分层                          │
├─────────────────────────────────────────────────────────┤
│  表现层 (UI Layer)                                      │
│  - ModeSelectActivity (模式选择)                        │
│  - ServerActivity (服务器界面)                          │
│  - ClientActivity (客户端全屏界面)                      │
│  - SettingsActivity (设置界面)                          │
├─────────────────────────────────────────────────────────┤
│  业务层 (Domain Layer)                                  │
│  - ModeManager (模式管理)                               │
│  - ConnectionManager (连接管理)                         │
│  - EventProcessor (事件处理)                            │
│  - CoordinateMapper (坐标映射)                          │
├─────────────────────────────────────────────────────────┤
│  数据层 (Data Layer)                                    │
│  - PreferencesRepository (配置存储)                     │
│  - NetworkRepository (网络通信)                         │
├─────────────────────────────────────────────────────────┤
│  服务层 (Service Layer)                                 │
│  - ServerService (服务器后台服务)                       │
│  - TouchAccessibilityService (Server 端辅助功能)        │
└─────────────────────────────────────────────────────────┘
```

### 3.2 网络通信设计

#### 3.2.1 通信协议
```
协议选择：TCP (保证可靠性)
端口：自定义端口（如 8899）

数据包格式：
┌─────────┬──────────┬──────────┬──────────┬──────────┬──────────┐
│ 消息头  │ 消息类型 │ 事件类型 │ 坐标 X   │ 坐标 Y   │ 附加数据 │
│ 4 bytes │ 1 byte   │ 1 byte   │ 4 bytes  │ 4 bytes  │ 可变     │
└─────────┴──────────┴──────────┴──────────┴──────────┴──────────┘

消息类型：
- 0x01: 连接请求
- 0x02: 连接响应
- 0x03: 触摸事件
- 0x04: 心跳包
- 0x05: 屏幕尺寸信息
- 0x06: 截屏数据（后续）

事件类型：
- 0x01: ACTION_DOWN
- 0x02: ACTION_UP
- 0x03: ACTION_MOVE
- 0x04: ACTION_POINTER_DOWN
- 0x05: ACTION_POINTER_UP
- 0x06: ACTION_CANCEL
```

#### 3.2.2 连接流程
```
Client:                        Server:
  |                              |
  |-- 输入 IP 地址 -------------> |
  |                              |
  |-- 发起连接请求 -------------> |
  |                          接收连接
  |                          发送屏幕尺寸
  |<-- 连接成功 + 屏幕尺寸 -----|
  |                              |
  |-- 心跳包 -----------------> |
  |<-- 心跳响应 ---------------|
  |                              |
  |-- 触摸事件包 -------------> |
  |                          坐标映射
  |                          执行操作
  |                              |
  |-- 截屏请求（后续）---------> |
  |<-- 截屏数据 ----------------|
  |                          截取屏幕
  |                              |
```

### 3.3 坐标映射算法

```kotlin
data class ScreenSize(val width: Int, val height: Int)

class CoordinateMapper(
    private val clientScreen: ScreenSize,
    private val serverScreen: ScreenSize
) {
    fun mapToServer(clientX: Float, clientY: Float): Pair<Float, Float> {
        val serverX = (clientX / clientScreen.width) * serverScreen.width
        val serverY = (clientY / clientScreen.height) * serverScreen.height
        return Pair(serverX, serverY)
    }
    
    fun mapToClient(serverX: Float, serverY: Float): Pair<Float, Float> {
        val clientX = (serverX / serverScreen.width) * clientScreen.width
        val clientY = (serverY / serverScreen.height) * clientScreen.height
        return Pair(clientX, clientY)
    }
}
```

### 3.4 触摸事件捕获方案

#### Client 端：OnTouchListener（推荐）
**优点**:
- 无需 root 权限
- 无需辅助功能
- 直接在全屏窗口上捕获
- 系统级支持

**实现**:
```kotlin
class ClientActivity : ComponentActivity() {
    private lateinit var touchOverlay: View
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        touchOverlay = View(this).apply {
            setOnTouchListener { v, event ->
                // 捕获触摸事件
                val clientX = event.x
                val clientY = event.y
                
                // 坐标映射
                val (serverX, serverY) = coordinateMapper.mapToServer(clientX, clientY)
                
                // 发送事件到 Server
                socketManager.sendEvent(event.action, serverX, serverY)
                
                true // 消费事件
            }
        }
        
        setContentView(touchOverlay)
    }
}
```

### 3.5 触摸事件执行方案（Server 端）

#### AccessibilityService（推荐）
```kotlin
class TouchAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 不需要捕获事件，只用于执行手势
    }
    
    fun executeTouch(x: Float, y: Float, action: Int) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 10))
            .build()
        
        dispatchGesture(gesture, null, null)
    }
}
```

---

## 四、模块设计

### 4.1 项目结构
```
app/
├── src/main/
│   ├── java/com/example/myapplication/
│   │   ├── MainActivity.kt                    # 主入口（模式选择）
│   │   ├── MyApplication.kt                   # Application 类
│   │   │
│   │   ├── ui/
│   │   │   ├── ModeSelectActivity.kt          # 模式选择界面
│   │   │   ├── ServerActivity.kt              # Server 端界面
│   │   │   ├── ClientActivity.kt              # Client 端全屏界面
│   │   │   └── SettingsActivity.kt            # 设置界面
│   │   │
│   │   ├── service/
│   │   │   ├── ServerService.kt               # Server 后台服务
│   │   │   ├── TouchAccessibilityService.kt   # Server 端辅助功能
│   │   │   └── BootReceiver.kt                # 开机自启动接收器
│   │   │
│   │   ├── network/
│   │   │   ├── SocketManager.kt               # Socket 连接管理
│   │   │   ├── PacketParser.kt                # 数据包解析
│   │   │   └── ConnectionListener.kt          # 连接状态监听
│   │   │
│   │   ├── model/
│   │   │   ├── TouchEvent.kt                  # 触摸事件数据类
│   │   │   ├── ScreenSize.kt                  # 屏幕尺寸数据类
│   │   │   └── ConnectionState.kt             # 连接状态枚举
│   │   │
│   │   ├── mapper/
│   │   │   └── CoordinateMapper.kt            # 坐标映射器
│   │   │
│   │   ├── repository/
│   │   │   └── PreferencesRepository.kt       # 配置存储
│   │   │
│   │   └── util/
│   │       ├── NetworkUtils.kt                # 网络工具类
│   │       ├── PermissionUtils.kt             # 权限工具类
│   │       ├── ScreenUtils.kt                 # 屏幕工具类
│   │       └── Constants.kt                   # 常量定义
│   │
│   ├── res/
│   │   ├── layout/                            # 布局文件
│   │   ├── values/                            # 资源值
│   │   └── drawable/                          # 图标资源
│   │
│   └── AndroidManifest.xml                    # 应用清单
│
└── build.gradle.kts                           # 构建配置
```

### 4.2 核心类设计

#### 4.2.1 TouchEvent 数据类
```kotlin
data class TouchEvent(
    val action: Int,          // 事件类型
    val x: Float,             // X 坐标（映射后的 Server 坐标）
    val y: Float,             // Y 坐标（映射后的 Server 坐标）
    val pointerId: Int = 0,   // 指针 ID
    val pressure: Float = 1f, // 压力值
    val timestamp: Long       // 时间戳
)
```

#### 4.2.2 ScreenSize 数据类
```kotlin
data class ScreenSize(
    val width: Int,
    val height: Int
) {
    fun aspectRatio(): Float = width.toFloat() / height.toFloat()
}
```

#### 4.2.3 SocketManager 网络管理
```kotlin
interface SocketManager {
    // Server 端接口
    fun startServer(port: Int)
    fun stopServer()
    fun getServerScreenSize(): ScreenSize
    fun sendScreenSize(size: ScreenSize)
    
    // Client 端接口
    fun connectToServer(ip: String, port: Int)
    fun disconnect()
    fun sendEvent(action: Int, x: Float, y: Float)
    fun requestScreenshot()
    
    // 通用接口
    fun isConnected(): Boolean
    fun setConnectionListener(listener: ConnectionListener)
}
```

#### 4.2.4 CoordinateMapper 坐标映射
```kotlin
class CoordinateMapper(
    private val clientScreen: ScreenSize,
    private var serverScreen: ScreenSize? = null
) {
    fun setServerScreenSize(size: ScreenSize) {
        serverScreen = size
    }
    
    fun mapToServer(clientX: Float, clientY: Float): Pair<Float, Float>? {
        val server = serverScreen ?: return null
        
        val serverX = (clientX / clientScreen.width) * server.width
        val serverY = (clientY / clientScreen.height) * server.height
        
        return Pair(serverX, serverY)
    }
}
```

---

## 五、权限需求

### 5.1 必需权限
```xml
<!-- 网络权限 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

<!-- 前台服务权限 -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

<!-- 辅助功能权限（Server 端） -->
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />

<!-- 开机自启动 -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- 防止休眠 -->
<uses-permission android:name="android.permission.WAKE_LOCK" />

<!-- 全屏显示 -->
<uses-permission android:name="android.permission.FULLSCREEN_INTENT" />
```

### 5.2 可选权限（后续截屏功能）
```xml
<!-- 截屏权限（需要特殊方式获取） -->
<uses-permission android:name="android.permission.MEDIA_PROJECTION" />
```

---

## 六、开发阶段规划

### 阶段 1: 基础框架搭建（预计 2-3 天）
- [x] 需求分析和架构设计
- [ ] 创建项目目录结构
- [ ] 配置依赖库（网络、存储等）
- [ ] 实现基础 UI 框架
- [ ] 实现模式选择逻辑
- [ ] 实现配置存储功能
- [ ] 实现屏幕尺寸获取工具类

### 阶段 2: 网络通信模块（预计 3-4 天）
- [ ] 实现 SocketManager 基础功能
- [ ] 实现 Server 端监听和连接
- [ ] 实现 Client 端连接功能
- [ ] 实现数据包编解码
- [ ] 实现连接状态管理
- [ ] 实现屏幕尺寸数据传输
- [ ] 实现心跳机制

### 阶段 3: Server 端功能（预计 3-4 天）
- [ ] 实现 Server 端 UI 界面
- [ ] 实现 IP 地址显示
- [ ] 实现后台服务（Foreground Service）
- [ ] 实现触摸事件接收和解析
- [ ] 实现坐标映射（Server 端）
- [ ] 实现触摸事件执行（AccessibilityService）
- [ ] 实现连接管理界面

### 阶段 4: Client 端功能（预计 3-4 天）
- [ ] 实现 Client 端全屏 UI 界面
- [ ] 实现 IP 地址配置界面
- [ ] 实现全屏窗口触摸事件捕获（OnTouchListener）
- [ ] 实现坐标映射（Client 端）
- [ ] 实现事件发送功能
- [ ] 实现连接状态显示
- [ ] 实现断线重连
- [ ] 实现背景颜色/图片设置

### 阶段 5: 优化和完善（预计 2-3 天）
- [ ] 性能优化（降低延迟）
- [ ] 增加多点触控支持
- [ ] 增加错误处理和日志
- [ ] UI/UX优化
- [ ] 测试和 Bug 修复
- [ ] 编写使用说明

### 阶段 6: 高级功能（可选）
- [ ] Server 端截屏功能
- [ ] 截屏图片传输和显示
- [ ] 数据加密传输
- [ ] 连接密码验证
- [ ] 多 Client 支持

---

## 七、技术难点和解决方案

### 7.1 坐标映射精度
**难点**: 不同屏幕分辨率和宽高比导致坐标映射不准确

**解决方案**:
1. 使用浮点数坐标，保持精度
2. 考虑宽高比差异，提供两种映射模式：
   - 拉伸模式（保持满屏）
   - 等比模式（保持比例，可能有黑边）

### 7.2 触摸事件同步
**难点**: Client 端直接捕获触摸，需要实时同步到 Server

**解决方案**:
1. 使用 OnTouchListener 直接捕获，无需辅助功能
2. 使用协程 + Channel 处理事件队列
3. 批量发送事件，减少网络请求

### 7.3 网络延迟优化
**难点**: 游戏场景对延迟要求高

**解决方案**:
1. 使用 TCP + 批量发送
2. 事件压缩（减少数据包大小）
3. 本地预测（可选）

### 7.4 后台保活（Server 端）
**难点**: Android 系统会杀死后台进程

**解决方案**:
1. Foreground Service + Notification
2. 忽略电池优化
3. 开机自启动

---

## 八、依赖库推荐

### 8.1 网络通信
```kotlin
// 使用 Java 原生 Socket API（推荐，更轻量）
```

### 8.2 数据存储
```kotlin
// DataStore - 官方推荐的存储方案
implementation("androidx.datastore:datastore-preferences:1.0.0")
```

### 8.3 协程和生命周期
```kotlin
// 已在项目中配置
implementation(libs.androidx.lifecycle.runtime.ktx)
implementation(libs.androidx.activity.compose)
```

### 8.4 UI 组件
```kotlin
// Jetpack Compose（已配置）
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.compose.ui)
implementation(libs.androidx.compose.material3)
```

---

## 九、测试计划

### 9.1 单元测试
- 网络模块测试
- 数据包编解码测试
- 坐标映射算法测试
- 配置存储测试

### 9.2 集成测试
- Server-Client 连接测试
- 触摸事件传输测试
- 坐标映射精度测试
- 断线重连测试

### 9.3 性能测试
- 延迟测试（不同网络环境）
- 稳定性测试（长时间运行）
- 压力测试（高频事件）
- 坐标映射精度测试

### 9.4 兼容性测试
- 不同 Android 版本测试
- 不同设备测试（手机、平板）
- 不同屏幕分辨率测试
- 不同网络环境测试（WiFi、热点）

---

## 十、UI/UX设计要点

### 10.1 模式选择界面
- 简洁的双按钮选择（Server / Client）
- 显示上次选择的模式
- 提供设置入口

### 10.2 Server 端界面
- 显示当前 IP 地址（大字体）
- 显示连接状态
- 显示已连接 Client 信息
- 提供复制 IP 功能
- 提供二维码分享 IP（可选）

### 10.3 Client 端界面
- **全屏沉浸式设计**
- **初始背景颜色（可配置）**
- **连接状态提示（小浮窗）**
- **IP 配置入口（可隐藏）**
- **后续显示 Server 截屏**

### 10.4 设置界面
- 修改模式
- 配置服务器 IP
- 配置端口
- 背景颜色设置
- 高级设置

---

## 十一、问题检查和修复

### 11.1 已识别的问题和修复

#### 问题 1: 数据包格式缺少指针 ID
**问题描述**: 多点触控场景下需要 pointerId 来区分不同的触摸点
**修复方案**: 
```
数据包格式（更新版）:
┌─────────┬──────────┬──────────┬──────────┬──────────┬──────────┬──────────┐
│ 消息头  │ 消息类型 │ 事件类型 │ 坐标 X   │ 坐标 Y   │ PointerID│ 压力值   │
│ 4 bytes │ 1 byte   │ 1 byte   │ 4 bytes  │ 4 bytes  │ 1 byte   │ 1 byte   │
└─────────┴──────────┴──────────┴──────────┴──────────┴──────────┴──────────┘
```

#### 问题 2: 缺少错误处理机制
**问题描述**: 网络异常、连接失败等情况没有明确的处理流程
**修复方案**: 增加错误码和重试机制
```kotlin
sealed class NetworkResult {
    object Success : NetworkResult()
    data class Error(val code: Int, val message: String) : NetworkResult()
    object Timeout : NetworkResult()
    object Disconnected : NetworkResult()
}
```

#### 问题 3: 缺少活动状态指示
**问题描述**: Server 端需要显示当前是否有 Client 连接，Client 端需要显示连接状态
**修复方案**: 增加状态指示 UI 和回调

#### 问题 4: 坐标映射需要考虑屏幕旋转
**问题描述**: 设备旋转后屏幕尺寸会交换，坐标映射会出错
**修复方案**: 
- 锁定屏幕方向（推荐）
- 或动态获取当前屏幕方向并调整映射算法

#### 问题 5: 缺少事件队列管理
**问题描述**: 高频触摸事件可能导致网络拥塞
**修复方案**: 
- 使用 Channel 缓冲事件
- 实现事件合并策略（连续的 MOVE 事件可以合并）
- 限流机制

### 11.2 补充的技术细节

#### 11.2.1 网络线程模型
```kotlin
// Server 端线程模型
class ServerSocketManager {
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val acceptScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val readScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Client 端类似
}
```

#### 11.2.2 辅助功能服务配置
```xml
<!-- res/xml/accessibility_service_config.xml -->
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeAllMask"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagRequestTouchExplorationMode"
    android:canPerformGestures="true"
    android:canRetrieveWindowContent="false"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100"
    android:settingsActivity="com.example.myapplication.ui.SettingsActivity" />
```

#### 11.2.3 前台服务通知
```kotlin
private fun createNotificationChannel() {
    val channel = NotificationChannel(
        CHANNEL_ID,
        "RemoteTouch Service",
        NotificationManager.IMPORTANCE_LOW
    ).apply {
        description = "RemoteTouch is running to receive touch events"
        setShowBadge(false)
    }
    notificationManager.createNotificationChannel(channel)
}
```

---

## 十二、总结

### 核心功能优先级
1. **P0（必须实现）**:
   - 模式选择和配置
   - Server 端网络监听和事件执行
   - Client 端全屏窗口和事件捕获
   - 坐标映射算法
   - 基础网络通信
   - 错误处理和重连机制

2. **P1（重要）**:
   - Server 后台服务保活
   - 连接状态管理
   - 断线重连
   - UI/UX优化
   - 事件队列管理

3. **P2（可选/后续）**:
   - Server 端截屏传输
   - 数据加密
   - 多 Client 支持
   - 屏幕旋转适配
   - 高级设置

### 开发建议
1. **先实现核心功能**: 先让 Server 和 Client 能够通信并执行简单点击
2. **坐标映射优先**: 确保不同屏幕尺寸下坐标准确映射
3. **逐步优化**: 再优化延迟、稳定性等
4. **充分测试**: 在真实设备上测试，特别是游戏场景
5. **用户友好**: 提供清晰的使用说明和配置引导

---

## 下一步行动

1. **确认需求**: 确认以上需求是否符合预期
2. **开始开发**: 按照开发阶段规划逐步实现
3. **持续迭代**: 根据测试反馈不断优化

**预计总开发周期**: 13-18 天（不含高级功能）
