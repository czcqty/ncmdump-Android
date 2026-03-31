# 开发环境配置指南

## 前提条件

### 系统要求
- Windows 10+ / macOS 11+ / Linux (Ubuntu 20.04+)
- 至少8GB RAM
- 至少20GB磁盘空间 (用于SDK和依赖)

### 必需软件

1. **Java Development Kit (JDK)**
   - 版本: JDK 8 或更高版本
   - 推荐使用 Android Studio 的内置JDK或 OpenJDK 11+
   - 设置 JAVA_HOME 环境变量

2. **Android SDK**
   - 推荐通过 Android Studio 安装
   - 所需SDK版本:
     - Compilé SDK: API 34 (Android 14)
     - Runtime: API 26+ (Android 8.0+)
   - 需要安装相应的NDK (如果项目使用native代码)

3. **Node.js 和 npm**
   - Node.js 14.0.0 或更高版本
   - 从 https://nodejs.org/ 下载安装
   - 验证: `node --version` 和 `npm --version`

4. **Git**
   - 从 https://git-scm.com/ 下载安装
   - 用于版本控制和项目管理

## 初始化步骤

### 1. 克隆项目
```bash
git clone https://github.com/czcqty/ncmdump-Android.git
cd ncmdump-android
```

### 2. 配置Android环境
#### Windows:
创建 `local.properties` 文件，添加SDK路径:
```properties
sdk.dir=C:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
```

#### macOS/Linux:
```properties
sdk.dir=/Users/YourUsername/Library/Android/sdk
```
或
```properties
sdk.dir=/home/username/Android/Sdk
```

### 3. 安装前端依赖
```bash
cd frontend
npm install
cd ..
```

### 4. 验证环境
```bash
./gradlew --version
npm --version
java -version
```

## 项目构建

### Android应用构建

**调试版本:**
```bash
./gradlew assembleDebug
# 输出文件: app/build/outputs/apk/debug/app-debug.apk
```

**发布版本:**
```bash
./gradlew assembleRelease
# 输出文件: app/build/outputs/apk/release/app-release-unsigned.apk
```

**清理构建:**
```bash
./gradlew clean
```

**完整构建流程:**
```bash
./gradlew clean build
```

### 前端构建

**开发服务器:**
```bash
cd frontend
npm run dev
# 访问: http://localhost:5173
```

**生产构建:**
```bash
cd frontend
npm run build
npm run preview  # 本地预览生产版本
```

## 开发工作流

### 使用IDE进行开发

#### 推荐: Android Studio
1. 打开 Android Studio
2. 选择 "Open an existing Android Studio project"
3. 选择项目根目录 `ncmdump-android`
4. 等待Gradle sync完成
5. 连接Android设备或启动模拟器
6. 点击 "Run > Run 'app'" 运行应用

#### VS Code (前端开发)
1. 在VS Code中打开 `frontend` 目录
2. 运行 `npm run dev`
3. 在浏览器中打开 http://localhost:5173

### 代码格式化和静态分析

**Kotlin代码检查:**
```bash
./gradlew detekt
./gradlew lintDebug
```

**前端代码检查:**
```bash
cd frontend
npm run build  # TypeScript检查
```

## 运行测试

### Android单元测试
```bash
./gradlew test
./gradlew testDebug
```

### Android仪器化测试 (需要设备/模拟器)
```bash
./gradlew connectedAndroidTest
```

## 调试

### Android应用调试
1. 在Android Studio中设置断点
2. 运行 Debug 配置 (Shift+F9)
3. 使用调试工具进行单步执行、变量查看等

### 前端调试
1. 在浏览器中打开开发者工具 (F12)
2. 使用Chrome DevTools调试React应用
3. 可使用React DevTools浏览器扩展

## 常见问题解决

### Gradle同步失败
- 检查网络连接
- 清理gradle缓存: `./gradlew clean`
- 重新同步: `./gradlew sync`

### SDK未找到
- 检查 `local.properties` 中的路径
- 使用 Android Studio 的 SDK Manager 安装缺失组件
- 設置 ANDROID_HOME 环境变量

### Node模块问题
```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
```

### 端口被占用
如果前端开发服务器端口 5173 被占用，可修改 `frontend/vite.config.ts` 的port配置。

## 发布应用

### 签署Release apk
```bash
./gradlew bundleRelease  # 生成App Bundle
# 或
./gradlew assembleRelease  # 生成签署的APK
```

详细的签署过程，参考Android官方文档: https://developer.android.com/studio/publish/app-signing

## 相关链接

- [Android Studio安装](https://developer.android.com/studio)
- [Gradle官方文档](https://gradle.org/releases/)
- [React官方文档](https://react.dev/)
- [Vite官方文档](https://vitejs.dev/)
- [Android开发文档](https://developer.android.com/docs)

## 获取帮助

遇到问题时:
1. 检查项目README.md
2. 查看此文档的相关部分
3. 搜索已知的GitHub issues
4. 提交新issue并详细描述问题
