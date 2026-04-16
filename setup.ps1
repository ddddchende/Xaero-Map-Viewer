$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

function Invoke-NpmCommand {
    param([string]$Arguments)
    $stdoutFile = "$env:TEMP\npm-stdout.txt"
    $stderrFile = "$env:TEMP\npm-stderr.txt"
    
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = "cmd.exe"
    $psi.Arguments = "/c npm $Arguments 2>`"$stderrFile`" >`"$stdoutFile`""
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    
    $process = [System.Diagnostics.Process]::Start($psi)
    $process.WaitForExit()
    $exitCode = $process.ExitCode
    
    if (Test-Path $stdoutFile) {
        Get-Content $stdoutFile | ForEach-Object { Write-Host "  $_" -ForegroundColor DarkGray }
        Remove-Item $stdoutFile -Force -ErrorAction SilentlyContinue
    }
    if (Test-Path $stderrFile) {
        $stderr = Get-Content $stderrFile -Raw
        if ($stderr -match "warn|deprecated") {
            Write-Host "  [!] npm 警告: 部分依赖已弃用，不影响使用" -ForegroundColor Yellow
        } elseif ($stderr -match "error|ERR!") {
            Write-Host "  $stderr" -ForegroundColor Red
        }
        Remove-Item $stderrFile -Force -ErrorAction SilentlyContinue
    }
    return $exitCode
}

function Write-Title($text) {
    Write-Host ""
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host "  $text" -ForegroundColor Cyan
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host ""
}

function Write-Step($num, $text) {
    Write-Host ""
    Write-Host "[$num] $text" -ForegroundColor Yellow
    Write-Host ("-" * 50) -ForegroundColor DarkGray
}

function Write-Ok($text) {
    Write-Host "  [OK] $text" -ForegroundColor Green
}

function Write-Warn($text) {
    Write-Host "  [!] $text" -ForegroundColor Yellow
}

function Write-Fail($text) {
    Write-Host "  [X] $text" -ForegroundColor Red
}

function Read-Default($prompt, $default) {
    $val = Read-Host "  $prompt (默认: $default)"
    if ([string]::IsNullOrWhiteSpace($val)) {
        return $default
    } else {
        return $val
    }
}

function Read-Required($prompt) {
    while ($true) {
        $val = Read-Host "  $prompt (必填)"
        if (-not [string]::IsNullOrWhiteSpace($val)) {
            return $val
        }
        Write-Fail "此项为必填项，请输入有效值"
    }
}

function Get-DefaultVal($value, $default) {
    if ($null -eq $value -or [string]::IsNullOrWhiteSpace($value)) {
        return $default
    } else {
        return $value
    }
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

Write-Title "Xaro 地图预览器 - 一键安装配置"

# Step 1: 检查 Node.js
Write-Step 1 "检查运行环境"

$nodeOk = $false
try {
    $nodeVer = node --version 2>&1
    $npmVer = npm --version 2>&1
    if ($nodeVer -and $npmVer) {
        Write-Ok "Node.js $nodeVer"
        Write-Ok "npm $npmVer"
        $nodeOk = $true
    }
} catch {
    $nodeOk = $false
}

if (-not $nodeOk) {
    Write-Fail "未检测到 Node.js，请先安装 Node.js 18+"
    Write-Host "  下载地址: https://nodejs.org/" -ForegroundColor White
    Read-Host "按回车退出"
    exit 1
}

$verNum = ($nodeVer -replace 'v', '') -split '\.'
if ([int]$verNum[0] -lt 18) {
    Write-Fail "Node.js 版本过低 (需要 18+)，当前: $nodeVer"
    Read-Host "按回车退出"
    exit 1
}
Write-Ok "Node.js 版本满足要求"

# Step 2: 安装依赖
Write-Step 2 "安装项目依赖"

if (Test-Path "node_modules") {
    Write-Warn "检测到已有 node_modules 目录"
    $reinstall = Read-Default "是否重新安装? (y/n)" "n"
    if ($reinstall -ne "y") {
        Write-Ok "跳过依赖安装"
    } else {
        Write-Host "  正在安装依赖，请稍候..." -ForegroundColor White
        $exitCode = Invoke-NpmCommand "install"
        if ($exitCode -ne 0) {
            Write-Fail "依赖安装失败，请检查网络连接"
            Read-Host "按回车退出"
            exit 1
        }
        Write-Ok "依赖安装完成"
    }
} else {
    Write-Host "  正在安装依赖，请稍候..." -ForegroundColor White
    $exitCode = Invoke-NpmCommand "install"
    if ($exitCode -ne 0) {
        Write-Fail "依赖安装失败，请检查网络连接"
        Read-Host "按回车退出"
        exit 1
    }
    Write-Ok "依赖安装完成"
}

# Step 3: 配置服务器
Write-Step 3 "配置服务器参数"

$configPath = Join-Path $ScriptDir "server\server_config.json"
$config = $null
$configLoaded = $false

if (Test-Path $configPath) {
    try {
        $config = Get-Content $configPath -Raw -Encoding UTF8 | ConvertFrom-Json
        $hasMapDir = -not [string]::IsNullOrWhiteSpace($config.mapDirectory)
        $hasCacheDir = -not [string]::IsNullOrWhiteSpace($config.cacheDirectory)
        $hasPort = $null -ne $config.port

        if ($hasMapDir -and $hasCacheDir -and $hasPort) {
            $configLoaded = $true
            Write-Ok "检测到已有配置"
            Write-Host "    地图目录:   $($config.mapDirectory)" -ForegroundColor DarkGray
            Write-Host "    缓存目录:   $($config.cacheDirectory)" -ForegroundColor DarkGray
            Write-Host "    端口:       $($config.port)" -ForegroundColor DarkGray
            Write-Host "    内存限制:   $($config.maxMemoryMB) MB" -ForegroundColor DarkGray
            Write-Host "    缓存条目:   $($config.maxCacheEntries)" -ForegroundColor DarkGray
            Write-Host "    并发加载:   $($config.maxConcurrentLoads)" -ForegroundColor DarkGray
            Write-Host "    批量区域:   $($config.maxBatchRegions)" -ForegroundColor DarkGray
            Write-Host ""

            $skipConfig = Read-Default "配置已存在，是否跳过? (y=跳过 / n=重新配置)" "y"
            if ($skipConfig -eq "y") {
                Write-Ok "保留现有配置"
            } else {
                $configLoaded = $false
            }
        } else {
            Write-Warn "配置文件不完整 (缺少地图目录/缓存目录/端口)，需要重新配置"
            $configLoaded = $false
        }
    } catch {
        Write-Warn "无法解析现有配置，将重新配置"
        $configLoaded = $false
    }
}

if (-not $configLoaded) {
    Write-Host ""
    Write-Host "  请输入 Xaero 地图数据所在目录" -ForegroundColor White
    Write-Host "  (通常是 .minecraft/versions/xxx/xaero 目录)" -ForegroundColor DarkGray
    if ($null -ne $config -and -not [string]::IsNullOrWhiteSpace($config.mapDirectory)) {
        $mapDir = Read-Default "地图目录" $config.mapDirectory
    } else {
        $mapDir = Read-Required "地图目录"
    }

    Write-Host ""
    Write-Host "  请输入缓存存储目录" -ForegroundColor White
    Write-Host "  (用于存储渲染后的地图像素数据)" -ForegroundColor DarkGray
    if ($null -ne $config -and -not [string]::IsNullOrWhiteSpace($config.cacheDirectory)) {
        $cacheDir = Read-Default "缓存目录" $config.cacheDirectory
    } else {
        $cacheDir = Read-Required "缓存目录"
    }

    $defPort = Get-DefaultVal $config.port 3001
    $port = Read-Default "服务器端口" $defPort

    $defMem = Get-DefaultVal $config.maxMemoryMB 4096
    $maxMem = Read-Default "Node.js 内存限制 (MB)" $defMem

    $defCache = Get-DefaultVal $config.maxCacheEntries 40000
    $maxCache = Read-Default "最大缓存条目数" $defCache

    $defConcurrent = Get-DefaultVal $config.maxConcurrentLoads 512
    $maxConcurrent = Read-Default "最大并发加载数" $defConcurrent

    $defBatch = Get-DefaultVal $config.maxBatchRegions 512
    $maxBatch = Read-Default "最大批量区域数" $defBatch

    $newConfig = @{
        port               = [int]$port
        maxMemoryMB        = [int]$maxMem
        mapDirectory       = $mapDir
        cacheDirectory     = $cacheDir
        maxCacheEntries    = [int]$maxCache
        maxConcurrentLoads = [int]$maxConcurrent
        maxBatchRegions    = [int]$maxBatch
    }

    $newConfig | ConvertTo-Json -Depth 10 | Set-Content $configPath -Encoding UTF8
    Write-Ok "配置已保存到 $configPath"
}

$finalConfig = Get-Content $configPath -Raw -Encoding UTF8 | ConvertFrom-Json
$port = $finalConfig.port

# Step 4: 构建项目
Write-Step 4 "构建前端项目"

if (Test-Path "dist\index.html") {
    Write-Warn "检测到已有构建产物 (dist/)"
    $rebuild = Read-Default "是否重新构建? (y/n)" "n"
    if ($rebuild -eq "y") {
        Write-Host "  正在构建，请稍候..." -ForegroundColor White
        $exitCode = Invoke-NpmCommand "run build"
        if ($exitCode -ne 0) {
            Write-Fail "构建失败，请检查 TypeScript 错误"
        } else {
            Write-Ok "构建完成"
        }
    } else {
        Write-Ok "跳过构建"
    }
} else {
    $build = Read-Default "是否立即构建前端? (y/n)" "y"
    if ($build -eq "y") {
        Write-Host "  正在构建，请稍候..." -ForegroundColor White
        $exitCode = Invoke-NpmCommand "run build"
        if ($exitCode -ne 0) {
            Write-Fail "构建失败，请检查 TypeScript 错误"
        } else {
            Write-Ok "构建完成"
        }
    } else {
        Write-Warn "跳过构建，可稍后运行 npm run build"
    }
}

# Step 5: 创建启动脚本
Write-Step 5 "创建启动脚本"

$prodBatPath = Join-Path $ScriptDir "启动.bat"

$prodBatContent = @"
@echo off
echo ============================================
echo   Xaro Map Viewer
echo ============================================
echo.
echo   http://localhost:$port
echo.
echo   Press Ctrl+C to stop
echo.
cd /d "%~dp0"
start http://localhost:$port
call npm start
pause
"@

if (-not (Test-Path $prodBatPath)) {
    [System.IO.File]::WriteAllText($prodBatPath, $prodBatContent, (New-Object System.Text.UTF8Encoding $false))
    Write-Ok "已创建 启动.bat"
} else {
    Write-Warn "启动.bat 已存在，跳过"
}

# Step 6: 完成
Write-Title "安装配置完成"

Write-Host "  启动方式:" -ForegroundColor White
Write-Host ""
Write-Host "    双击 启动.bat" -ForegroundColor Green -NoNewline
Write-Host "  启动服务器 (需先 build)"
Write-Host ""
Write-Host "  命令行命令:" -ForegroundColor White
Write-Host ""
Write-Host "    npm run dev:all    " -ForegroundColor Green -NoNewline
Write-Host "开发模式 (前后端同时启动)"
Write-Host "    npm run build      " -ForegroundColor Green -NoNewline
Write-Host "构建前端"
Write-Host "    npm start          " -ForegroundColor Green -NoNewline
Write-Host "生产模式 (端口 $port)"
Write-Host ""

$startNow = Read-Default "是否立即启动开发模式? (y/n)" "n"
if ($startNow -eq "y") {
    Write-Host ""
    Write-Host "  启动中... 按 Ctrl+C 停止" -ForegroundColor White
    npm run dev:all
}
