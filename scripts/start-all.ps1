param(
    [switch]$SkipBuild,
    [switch]$StopExisting
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$logsDir = Join-Path $root ".logs"
$pidFile = Join-Path $logsDir "service-pids.json"

$services = @(
    @{ Name = "scf-isc"; Port = 8081 },
    @{ Name = "scf-wms"; Port = 8082 },
    @{ Name = "scf-lgs"; Port = 8083 },
    @{ Name = "scf-oms"; Port = 8084 },
    @{ Name = "scf-gateway"; Port = 8090 }
)

function Ensure-Directory([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        New-Item -ItemType Directory -Path $Path | Out-Null
    }
}

function Stop-TrackedServices {
    if (-not (Test-Path -LiteralPath $pidFile)) {
        return
    }

    $tracked = Get-Content -LiteralPath $pidFile | ConvertFrom-Json
    foreach ($item in $tracked) {
        try {
            $proc = Get-Process -Id $item.pid -ErrorAction Stop
            Write-Host "Stopping $($item.name) (PID $($item.pid))"
            Stop-Process -Id $proc.Id -Force
        } catch {
            Write-Host "$($item.name) already stopped"
        }
    }

    Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
}

function Stop-PortListeners([int[]]$Ports) {
    foreach ($port in $Ports) {
        $listeners = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
            Select-Object -ExpandProperty OwningProcess -Unique
        foreach ($processId in $listeners) {
            try {
                $proc = Get-Process -Id $processId -ErrorAction Stop
                Write-Host "Stopping process on port ${port}: $($proc.ProcessName) (PID $processId)"
                Stop-Process -Id $processId -Force
            } catch {
            }
        }
    }
}

function Test-PortAvailable([int]$Port) {
    $conn = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue
    return $null -eq $conn
}

function Wait-PortOpen([int]$Port, [System.Diagnostics.Process]$Process, [int]$TimeoutSeconds = 120) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if ($Process.HasExited) {
            return $false
        }
        if (-not (Test-PortAvailable -Port $Port)) {
            return $true
        }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Resolve-JarPath([string]$ModuleName) {
    $targetDir = Join-Path $root $ModuleName
    $targetDir = Join-Path $targetDir "target"
    $jar = Get-ChildItem -Path $targetDir -Filter "*.jar" -File |
        Where-Object { $_.Name -notlike "original-*" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if ($null -eq $jar) {
        throw "Jar not found for $ModuleName under $targetDir"
    }

    return $jar.FullName
}

function Write-JarInfo([string]$ModuleName) {
    $jarPath = Resolve-JarPath -ModuleName $ModuleName
    $jar = Get-Item -LiteralPath $jarPath
    $message = "Using jar for ${ModuleName}: $($jar.FullName) (LastWriteTime: $($jar.LastWriteTime.ToString('yyyy-MM-dd HH:mm:ss')))"
    Write-Host $message
    Add-Content -LiteralPath (Join-Path $logsDir "startup-manifest.log") -Value $message
    return $jar.FullName
}

function Test-CommandExists([string]$CommandName) {
    return $null -ne (Get-Command $CommandName -ErrorAction SilentlyContinue)
}

function Test-DatabaseSchema {
    if (-not (Test-CommandExists -CommandName "mysql")) {
        $message = "mysql client not found; skipping preflight schema validation."
        Write-Host $message
        Add-Content -LiteralPath (Join-Path $logsDir "startup-manifest.log") -Value $message
        return
    }

    $dbUrl = if ($env:SCF_DB_URL) { $env:SCF_DB_URL } else { "jdbc:mysql://localhost:3306/scf_oms?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai" }
    $dbUser = if ($env:SCF_DB_USERNAME) { $env:SCF_DB_USERNAME } else { "root" }
    $dbPassword = if ($env:SCF_DB_PASSWORD) { $env:SCF_DB_PASSWORD } else { "112233zhan" }

    if ($dbUrl -notmatch 'jdbc:mysql://(?<host>[^:/?]+)(:(?<port>\d+))?/(?<database>[^?]+)') {
        throw "Unable to parse SCF_DB_URL: $dbUrl"
    }

    $dbHost = $Matches.host
    $dbPort = if ($Matches.port) { $Matches.port } else { "3306" }
    $database = $Matches.database
    $requiredTables = @("lgs_provider", "wms_wave", "wms_picking_task", "wms_packing_order", "wms_shipment_record")
    $sql = @"
SELECT table_name
FROM information_schema.tables
WHERE table_schema = '$database'
  AND table_name IN ('lgs_provider','wms_wave','wms_picking_task','wms_packing_order','wms_shipment_record');
"@

    $previousPassword = $env:MYSQL_PWD
    $env:MYSQL_PWD = $dbPassword
    try {
        $output = & mysql --host=$dbHost --port=$dbPort --user=$dbUser --batch --skip-column-names --execute=$sql 2>$null
        if ($LASTEXITCODE -ne 0) {
            throw "mysql exited with code $LASTEXITCODE"
        }
    } catch {
        throw "Database schema validation failed: $($_.Exception.Message)"
    } finally {
        if ($null -eq $previousPassword) {
            Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
        } else {
            $env:MYSQL_PWD = $previousPassword
        }
    }

    $existing = @($output | Where-Object { $_ -and $_.Trim() -ne "" })
    $missing = $requiredTables | Where-Object { $_ -notin $existing }
    if ($missing.Count -gt 0) {
        throw "Database '$database' is missing required tables: $($missing -join ', '). Re-run db/init/scf_seed.sql before starting services."
    }

    $message = "Database schema validation passed for $database"
    Write-Host $message
    Add-Content -LiteralPath (Join-Path $logsDir "startup-manifest.log") -Value $message
}

Ensure-Directory -Path $logsDir
Set-Content -LiteralPath (Join-Path $logsDir "startup-manifest.log") -Value ("Startup at " + (Get-Date -Format "yyyy-MM-dd HH:mm:ss"))

if ($StopExisting) {
    Stop-TrackedServices
    Stop-PortListeners -Ports ($services.Port)
    Start-Sleep -Seconds 2
}

foreach ($service in $services) {
    if (-not (Test-PortAvailable -Port $service.Port)) {
        throw "Port $($service.Port) is already in use. Free it or run with -StopExisting if the process was started by this script."
    }
}

if (-not $SkipBuild) {
    Write-Host "Building all modules..."
    & "$root\mvnw.cmd" -q -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        throw "Build failed."
    }
} else {
    $message = "SkipBuild enabled; reusing existing jars. Use this only when you are sure the target jars are already current."
    Write-Host $message
    Add-Content -LiteralPath (Join-Path $logsDir "startup-manifest.log") -Value $message
}

Test-DatabaseSchema

foreach ($service in $services) {
    Set-Content -LiteralPath (Join-Path $logsDir "$($service.Name).out.log") -Value ""
    Set-Content -LiteralPath (Join-Path $logsDir "$($service.Name).err.log") -Value ""
}

$started = @()

foreach ($service in $services) {
    $jarPath = Write-JarInfo -ModuleName $service.Name
    $outLogPath = Join-Path $logsDir "$($service.Name).out.log"
    $errLogPath = Join-Path $logsDir "$($service.Name).err.log"

    Write-Host "Starting $($service.Name) on port $($service.Port)..."
    $proc = Start-Process -FilePath "java" `
        -ArgumentList @("-jar", $jarPath) `
        -WorkingDirectory $root `
        -RedirectStandardOutput $outLogPath `
        -RedirectStandardError $errLogPath `
        -PassThru

    if (-not (Wait-PortOpen -Port $service.Port -Process $proc -TimeoutSeconds 120)) {
        try {
            Stop-Process -Id $proc.Id -Force
        } catch {
        }
        throw "$($service.Name) failed to open port $($service.Port). Check $outLogPath and $errLogPath"
    }

    $started += [pscustomobject]@{
        name = $service.Name
        pid = $proc.Id
        port = $service.Port
        outLog = $outLogPath
        errLog = $errLogPath
    }
}

$started | ConvertTo-Json | Set-Content -LiteralPath $pidFile -Encoding UTF8

Write-Host ""
Write-Host "All services started."
Write-Host "PID file: $pidFile"
Write-Host "Logs: $logsDir"
