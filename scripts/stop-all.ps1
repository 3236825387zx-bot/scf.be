$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$pidFile = Join-Path (Join-Path $root ".logs") "service-pids.json"
$ports = @(8081, 8082, 8083, 8084, 8090)

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

if (-not (Test-Path -LiteralPath $pidFile)) {
    Stop-PortListeners -Ports $ports
    Write-Host "No tracked services were found. Port listeners on known service ports were stopped if present."
    exit 0
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
Stop-PortListeners -Ports $ports
Write-Host "All tracked services stopped."
