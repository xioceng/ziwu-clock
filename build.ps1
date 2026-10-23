param(
    [string]$Jdk = $env:JAVA_HOME,
    [string]$Sdk = $env:ANDROID_HOME,
    [string]$BuildTools = "",
    [string]$VersionName = "1.1.0",
    [int]$VersionCode = 2,
    [string]$Keystore = "",
    [string]$StorePass = ""
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Parent = Split-Path -Parent $Root
$Package = "com.zhiwu.wallpaper"

function Find-First($paths) {
    foreach ($p in $paths) {
        if ($p -and (Test-Path $p)) { return (Resolve-Path $p).Path }
    }
    return $null
}

if (-not $Jdk) {
    $Jdk = Find-First @(
        (Join-Path $Parent "tools\jdk-17.0.2"),
        "C:\Program Files\Java\jdk-17",
        "C:\Program Files\Java\jdk-21"
    )
}
if (-not $Sdk) {
    $Sdk = Find-First @(
        (Join-Path $Parent "tools\android-35"),
        (Join-Path $Parent "tools\android-sdk\platforms\android-35"),
        (Join-Path $Parent "tools\android-sdk")
    )
}
if (-not $BuildTools) {
    $BuildTools = Find-First @(
        (Join-Path $Parent "tools\android-15"),
        (Join-Path $Parent "tools\android-sdk\build-tools\35.0.0"),
        (Join-Path $env:LOCALAPPDATA "Android\Sdk\build-tools\35.0.0")
    )
}

$Javac = Join-Path $Jdk "bin\javac.exe"
$Jar = Join-Path $Jdk "bin\jar.exe"
$Keytool = Join-Path $Jdk "bin\keytool.exe"
if (-not (Test-Path $Javac)) { throw "找不到 JDK，请用 -Jdk 指定 JDK 17 目录。" }
$env:JAVA_HOME = $Jdk
$env:PATH = "$Jdk\bin;$env:PATH"

$AndroidJar = Find-First @(
    (Join-Path $Sdk "android.jar"),
    (Join-Path $Sdk "platforms\android-35\android.jar"),
    (Join-Path $Sdk "platforms\android-36\android.jar")
)
if (-not $AndroidJar) { throw "找不到 android.jar，请用 -Sdk 指定 Android 平台目录。" }

$Aapt2 = Find-First @(
    (Join-Path $BuildTools "aapt2.exe"),
    (Join-Path $BuildTools "aapt2")
)
$D8 = Find-First @(
    (Join-Path $BuildTools "d8.bat"),
    (Join-Path $BuildTools "d8")
)
$ZipAlign = Find-First @(
    (Join-Path $BuildTools "zipalign.exe"),
    (Join-Path $BuildTools "zipalign")
)
$ApkSigner = Find-First @(
    (Join-Path $BuildTools "apksigner.bat"),
    (Join-Path $BuildTools "apksigner")
)
if (-not $Aapt2 -or -not $D8 -or -not $ZipAlign -or -not $ApkSigner) {
    throw "找不到 aapt2 / d8 / zipalign / apksigner，请用 -BuildTools 指定 build-tools 目录。"
}

$Work = Join-Path $env:TEMP "zhiwu-build"
Remove-Item $Work -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force (Join-Path $Work "app"), (Join-Path $Work "lib"), (Join-Path $Work "build\gen"), (Join-Path $Work "build\classes"), (Join-Path $Work "build\dex"), (Join-Path $Root "dist") | Out-Null
Copy-Item (Join-Path $Root "app\src") (Join-Path $Work "app\src") -Recurse -Force
Copy-Item (Join-Path $Root "app\src\main\AndroidManifest.xml") (Join-Path $Work "app\src\main\AndroidManifest.xml") -Force
Copy-Item (Join-Path $Root "lib\lunar-1.7.7.jar") (Join-Path $Work "lib\lunar-1.7.7.jar") -Force
Copy-Item $AndroidJar (Join-Path $Work "android.jar") -Force

Write-Host "1/6  编译资源 ..."
& $Aapt2 compile --dir (Join-Path $Work "app\src\main\res") -o (Join-Path $Work "build\res.zip")
if ($LASTEXITCODE -ne 0) { throw "资源编译失败" }

& $Aapt2 link `
    -o (Join-Path $Work "build\unsigned.apk") `
    -I (Join-Path $Work "android.jar") `
    --manifest (Join-Path $Work "app\src\main\AndroidManifest.xml") `
    --java (Join-Path $Work "build\gen") `
    --min-sdk-version 23 --target-sdk-version 35 `
    --version-code $VersionCode --version-name $VersionName `
    (Join-Path $Work "build\res.zip")
if ($LASTEXITCODE -ne 0) { throw "资源链接失败" }

Write-Host "2/6  编译 Java ..."
$sources = Join-Path $Work "build\sources.txt"
Get-ChildItem (Join-Path $Work "app\src\main\java") -Recurse -Filter *.java | ForEach-Object { $_.FullName } | Set-Content $sources -Encoding ASCII
Get-ChildItem (Join-Path $Work "build\gen") -Recurse -Filter *.java | ForEach-Object { $_.FullName } | Add-Content $sources -Encoding ASCII

& $Javac -encoding UTF-8 --release 8 -classpath "$Work\android.jar;$Work\lib\lunar-1.7.7.jar" -d (Join-Path $Work "build\classes") "@$sources"
if ($LASTEXITCODE -ne 0) { throw "Java 编译失败" }

Write-Host "3/6  生成 classes.dex ..."
& $Jar cf (Join-Path $Work "build\app.jar") -C (Join-Path $Work "build\classes") .
if ($LASTEXITCODE -ne 0) { throw "jar 打包失败" }
& $D8 --release --min-api 23 --lib (Join-Path $Work "android.jar") --output (Join-Path $Work "build\dex") (Join-Path $Work "build\app.jar") (Join-Path $Work "lib\lunar-1.7.7.jar")
if ($LASTEXITCODE -ne 0) { throw "D8 失败" }

Write-Host "4/6  封装 APK ..."
$withDex = Join-Path $Work "build\withdex.apk"
Copy-Item (Join-Path $Work "build\unsigned.apk") $withDex -Force
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open($withDex, "Update")
try {
    $entry = $zip.GetEntry("classes.dex")
    if ($entry) { $entry.Delete() }
    [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
        $zip, (Join-Path $Work "build\dex\classes.dex"), "classes.dex",
        [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
} finally {
    $zip.Dispose()
}

Write-Host "5/6  对齐 ..."
& $ZipAlign -p -f 4 $withDex (Join-Path $Work "build\aligned.apk")
if ($LASTEXITCODE -ne 0) { throw "zipalign 失败" }

Write-Host "6/6  签名 ..."
$keystoreDir = Join-Path $Root "keystore"
if ($Keystore) {
    $keystore = $Keystore
} else {
    $keystore = Join-Path $keystoreDir "zhiwu-release.jks"
}
$storePass = $StorePass
$passFile = Join-Path (Split-Path -Parent $keystore) "local-test-password.txt"
if (-not $storePass -and (Test-Path $passFile)) {
    $storePass = (Get-Content $passFile -Raw).Trim()
}
if (-not (Test-Path $keystore)) {
    if (-not $storePass) {
        $storePass = [guid]::NewGuid().ToString("N")
    }
    New-Item -ItemType Directory -Force (Split-Path -Parent $keystore) | Out-Null
    & $Keytool -genkeypair -keystore $keystore -alias zhiwu -keyalg RSA -keysize 2048 `
        -validity 10000 -storepass $storePass -keypass $storePass `
        -dname "CN=Zhiwu, OU=Wallpaper, O=Zhiwu, L=Shanghai, ST=Shanghai, C=CN"
    Set-Content -Path $passFile -Value $storePass -Encoding ASCII
}
if (-not $storePass) {
    throw "签名密钥已存在，请用 -StorePass 提供口令（或把口令写入密钥同目录的 local-test-password.txt）。"
}
$out = Join-Path $Root "dist\zhiwu-$VersionName.apk"
& $ApkSigner sign --ks $keystore --ks-key-alias zhiwu --ks-pass "pass:$storePass" --key-pass "pass:$storePass" --out $out (Join-Path $Work "build\aligned.apk")
if ($LASTEXITCODE -ne 0) { throw "签名失败" }
Remove-Item "$out.idsig" -Force -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "完成: $out"
Write-Host "大小: $([math]::Round((Get-Item $out).Length / 1KB, 1)) KB"
