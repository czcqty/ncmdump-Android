# ncmdump-Android 项目同步指南

## 前置要求
确保已安装 Git for Windows：https://git-scm.com/download/win

## 同步步骤

### 1. 安装 Git
从 https://git-scm.com/download/win 下载并安装 Git for Windows

### 2. 配置 Git 用户信息
打开 PowerShell，执行：
```powershell
git config --global user.name "czcqty"
git config --global user.email "1250320117@zust.edu.cn"
```

### 3. 初始化本地仓库
```powershell
cd d:\1pt5file\program\Android\ncmdump\ncmdump-android
git init
git add .
git commit -m "Initial commit: restructured ncmdump-Android project"
```

### 4. 配置远程仓库
```powershell
git remote add origin https://github.com/czcqty/ncmdump-Android.git
```

### 5. 强制推送到远程仓库（清空旧代码）
```powershell
git branch -M main
git push -u origin main --force
```

### 注意事项
- `--force` 标志会覆盖远程仓库的所有内容，请确保这是你想要的操作
- 在执行推送前，需要在GitHub上创建或清空该仓库
- 需要配置GitHub的SSH key或使用个人访问令牌进行身份验证

## 需要编写的文档
完成上述步骤后，建议添加以下文档：
- README.md - 项目说明
- .gitignore - Git忽略文件配置
- 开发指南
- 构建说明
