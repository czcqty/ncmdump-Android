# ncmdump-Android

ncmdump-gui的Android版本，采用现代化的技术栈进行重构。

## 项目概述

这是一个基于 ncmdump-go 开发的Android应用程序。该项目采用混合开发模式：
- **后端**: Android (Kotlin) + Java
- **前端**: React + TypeScript + Vite
- **UI框架**: Fluent UI React Components

## 项目结构

```
ncmdump-android/
├── app/                    # Android应用主模块
│   ├── src/main/          # 应用源代码
│   │   ├── java/          # Java/Kotlin源码
│   │   ├── res/           # 资源文件
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts   # Android构建配置
│   └── proguard-rules.pro # 代码混淆规则
├── frontend/              # React Web用户界面
│   ├── src/               # React组件和样式
│   ├── package.json       # Node依赖配置
│   └── vite.config.ts     # Vite构建配置
├── gradle/                # Gradle包装器
├── build.gradle.kts       # 根项目构建配置
├── settings.gradle.kts    # Gradle设置
└── local.properties       # 本地配置(已忽略)
```

## 技术栈

### Android端
- **编译SDK**: API 34 (Android 14)
- **最小SDK**: API 26 (Android 8.0)
- **目标SDK**: API 34
- **编程语言**: Kotlin + Java
- **构建工具**: Gradle 8+

### 前端
- **框架**: React 18.2
- **语言**: TypeScript
- **构建工具**: Vite 4
- **UI组件库**: Fluent UI React Components 9.56
- **图标库**: Fluent UI Icons

## 版本信息

- **应用版本**: 1.2.1
- **包名**: com.ncmdump.android

## 开发环境要求

### 前置条件
- Java Development Kit (JDK) 8+
- Android SDK (推荐使用Android Studio)
- Node.js 14+ (用于前端开发)
- npm 或 yarn (前端包管理器)

### 构建指南

#### 构建整个项目
```bash
./gradlew build
```

#### 构建Debug版本
```bash
./gradlew assembleDebug
```

#### 构建Release版本
```bash
./gradlew assembleRelease
```

#### 前端开发
```bash
cd frontend
npm install
npm run dev      # 开发服务器
npm run build    # 构建生产版本
npm run preview  # 预览生产构建
```

## 功能特性

- 转换 ncm 格式为 mp3 或 flac 格式
- ncmdump-gui的用户界面
- 良好的兼容性 (Android 8.0+)

## 配置文件说明

- `build.gradle.kts` - 根项目gradle配置
- `app/build.gradle.kts` - Android应用模块配置
- `frontend/vite.config.ts` - Vite前端构建配置
- `frontend/tailwind.config.js` - Tailwind CSS配置
- `.gitignore` - Git忽略规则

## 许可证

请查看项目许可证信息

## 开发者说明

该项目是对原ncmdump-gui项目的移植，以提供Android平台的支持

---

更多信息和使用说明，请查看具体的模块文档。
