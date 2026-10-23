# Contributing

欢迎提交 Issue 或 Pull Request。

## 开发环境

- JDK 17
- Android SDK Platform 35
- Android Build-Tools（`aapt2`、`d8`、`zipalign`、`apksigner`）

项目不依赖 Gradle，Windows 下运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

也可以指定工具位置：

```powershell
.\build.ps1 -Jdk "C:\path\to\jdk-17" `
  -Sdk "C:\path\to\platforms\android-35" `
  -BuildTools "C:\path\to\build-tools\35.0.0"
```

构建脚本会先把工程复制到 `%TEMP%\zhiwu-build`，因为 AAPT2 对含中文的 Windows 工程路径支持不稳定。

## 签名

不要提交 `keystore/`、`*.jks`、`*.keystore`。本地构建会自动生成测试密钥；发布版本请通过 `-Keystore` 和 `-StorePass` 使用离线密钥。

## 历法口径

修改历法/节气相关代码时，请保持：

- 年柱、月柱按立春/节气换柱。
- 日柱按公历日 00:00 换日。
- 节气时刻按北京时间（UTC+8）显示。
- 十二时辰经络为十二经纳子法，不是纳甲开穴。

## 提交前

- 确认 `dist/` 中 APK 版本号与 `app/src/main/AndroidManifest.xml` 一致。
- 更新 `CHANGELOG.md`。
- 如果改了表盘布局，更新 `screenshots/preview.png`。
