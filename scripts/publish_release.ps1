param(
    [Parameter(Mandatory=$true)]
    [string]$Version,          # e.g. "v1.0.2"
    [string]$Title = "",
    [string]$Notes = "Оновлення стабільності та оптимізація",
    [string]$Token = $env:GITHUB_TOKEN,
    [switch]$Forced = $false,
    [switch]$SkipBuild = $false
)

$ErrorActionPreference = "Stop"
$Owner = "lonelycattools-cloud"
$Repo = "ProzorroRadar"
$ApkPath = "$PSScriptRoot\..\ProzorroRadar.apk"

# Check token
if (-not $Token) {
    $propsPath = "$PSScriptRoot\..\local.properties"
    if (Test-Path $propsPath) {
        $line = Get-Content $propsPath | Where-Object { $_ -match "^github\.token=(.+)" } | Select-Object -First 1
        if ($line) {
            $Token = $line -replace "^github\.token=", ""
        }
    }
}

if (-not $Token) {
    Write-Error "GitHub Token не знайдено. Вкажіть параметр -Token або додайте github.token=... в local.properties"
    exit 1
}

if (-not $Title) {
    $Title = "Prozorro Radar $Version"
}

if ($Forced) {
    $Title = "$Title [FORCED]"
    $Notes = "[FORCED]`n$Notes"
}

if (-not $SkipBuild) {
    Write-Host "1. Компіляція свіжого APK..." -ForegroundColor Cyan
    Set-Location "$PSScriptRoot\.."
    cmd.exe /c "gradlew.bat assembleDebug" | Out-String | Out-Null
}

if (-not (Test-Path $ApkPath)) {
    Write-Error "Файл $ApkPath не знайдено після збірки"
    exit 1
}

Write-Host "2. Створення релізу $Version на GitHub..." -ForegroundColor Cyan
$headers = @{
    "Authorization" = "Bearer $Token"
    "Accept"        = "application/vnd.github.v3+json"
    "User-Agent"    = "ProzorroRadar-ReleaseScript"
}

$releaseBody = @{
    tag_name         = $Version
    target_commitish = "main"
    name             = $Title
    body             = $Notes
    draft            = $false
    prerelease       = $false
} | ConvertTo-Json

$releaseResp = Invoke-RestMethod -Uri "https://api.github.com/repos/$Owner/$Repo/releases" `
    -Method Post `
    -Headers $headers `
    -Body $releaseBody `
    -ContentType "application/json; charset=utf-8"

$releaseId = $releaseResp.id
$uploadUrl = $releaseResp.upload_url -replace '\{\?name,label\}', "?name=ProzorroRadar.apk"

Write-Host "3. Завантаження APK (розмір: $([math]::Round((Get-Item $ApkPath).Length / 1MB, 2)) МБ)..." -ForegroundColor Cyan
$apkBytes = [System.IO.File]::ReadAllBytes($ApkPath)

$uploadHeaders = @{
    "Authorization" = "Bearer $Token"
    "Content-Type"  = "application/vnd.android.package-archive"
    "User-Agent"    = "ProzorroRadar-ReleaseScript"
}

$assetResp = Invoke-RestMethod -Uri $uploadUrl `
    -Method Post `
    -Headers $uploadHeaders `
    -Body $apkBytes

Write-Host "Реліз $Version успішно опубліковано!" -ForegroundColor Green
Write-Host "Посилання: $($releaseResp.html_url)" -ForegroundColor Yellow
