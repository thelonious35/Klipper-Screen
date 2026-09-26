$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$tools = Join-Path $root '.android-build'
$sdk = Join-Path $tools 'sdk'
$gradleDir = Join-Path $tools 'gradle-8.9'
$jdkDir = Join-Path $tools 'jdk17'
$cmdZip = Join-Path $tools 'cmdline-tools.zip'
$gradleZip = Join-Path $tools 'gradle.zip'
$jdkZip = Join-Path $tools 'jdk17.zip'
New-Item -ItemType Directory -Force -Path $tools,$sdk | Out-Null

Write-Host '=== Klipper Screen APK Builder ===' -ForegroundColor Cyan

# Portable JDK 17 - do not depend on system Java
$javaExe = Get-ChildItem -Path $jdkDir -Filter java.exe -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
if (!$javaExe) {
  Write-Host 'JDK 17 indiriliyor...'
  $api = 'https://api.adoptium.net/v3/assets/latest/17/hotspot?architecture=x64&heap_size=normal&image_type=jdk&jvm_impl=hotspot&os=windows&vendor=eclipse'
  $resp = Invoke-RestMethod -Uri $api
  $link = $resp[0].binary.package.link
  Invoke-WebRequest -Uri $link -OutFile $jdkZip
  Remove-Item $jdkDir -Recurse -Force -ErrorAction SilentlyContinue
  New-Item -ItemType Directory -Force -Path $jdkDir | Out-Null
  Expand-Archive $jdkZip -DestinationPath $jdkDir -Force
  $javaExe = Get-ChildItem -Path $jdkDir -Filter java.exe -Recurse | Select-Object -First 1
}
$javaHome = Split-Path -Parent (Split-Path -Parent $javaExe.FullName)
$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$env:Path"
Write-Host "Java kullaniliyor: $(& $javaExe.FullName -version 2>&1 | Select-Object -First 1)" -ForegroundColor Green

if (!(Test-Path "$sdk\cmdline-tools\latest\bin\sdkmanager.bat")) {
  Write-Host 'Android command-line tools indiriliyor...'
  Invoke-WebRequest -Uri 'https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip' -OutFile $cmdZip
  $tmp = Join-Path $tools 'cmdtmp'
  Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue
  Expand-Archive $cmdZip -DestinationPath $tmp -Force
  New-Item -ItemType Directory -Force -Path "$sdk\cmdline-tools\latest" | Out-Null
  Copy-Item "$tmp\cmdline-tools\*" "$sdk\cmdline-tools\latest" -Recurse -Force
}

$sdkmanager = "$sdk\cmdline-tools\latest\bin\sdkmanager.bat"
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk

Write-Host 'Android SDK 35 ve build-tools kuruluyor...'
cmd /c "echo y|`"$sdkmanager`" --sdk_root=`"$sdk`" platform-tools platforms;android-35 build-tools;35.0.0"
cmd /c "for /l %i in (1,1,20) do @echo y" | & $sdkmanager --sdk_root=$sdk --licenses | Out-Null

if (!(Test-Path "$gradleDir\bin\gradle.bat")) {
  Write-Host 'Gradle 8.9 indiriliyor...'
  Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.9-bin.zip' -OutFile $gradleZip
  Expand-Archive $gradleZip -DestinationPath $tools -Force
}

Write-Host 'APK derleniyor...' -ForegroundColor Yellow
Push-Location $root
& "$gradleDir\bin\gradle.bat" --no-daemon clean assembleDebug
$exit = $LASTEXITCODE
Pop-Location
if ($exit -ne 0) { throw "Gradle build failed with exit code $exit" }

$src = Join-Path $root 'app\build\outputs\apk\debug\app-debug.apk'
if (!(Test-Path $src)) { throw "APK bulunamadi: $src" }
$dst = Join-Path $root 'Klipper-Screen.apk'
Copy-Item $src $dst -Force
Write-Host "`nTAMAMLANDI: $dst" -ForegroundColor Green
Start-Process explorer.exe "/select,`"$dst`""
