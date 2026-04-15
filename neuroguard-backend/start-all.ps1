param(
    [switch]$DryRun,
    [switch]$SkipPortCleanup
)

$ErrorActionPreference = 'Stop'

$serviceOrder = @(
    'eureka-server',
    'user-service',
    'careplan-service',
    'pharmacy-service',
    'prescription-service',
    'gateway'
)

$servicePorts = @{
    'eureka-server' = 8761
    'user-service' = 8081
    'careplan-service' = 8084
    'pharmacy-service' = 8085
    'prescription-service' = 8089
    'gateway' = 8083
}

$servicePortVars = @{
    'eureka-server' = 'EUREKA_PORT'
    'user-service' = 'USER_SERVICE_PORT'
    'careplan-service' = 'CAREPLAN_SERVICE_PORT'
    'pharmacy-service' = 'PHARMACY_SERVICE_PORT'
    'prescription-service' = 'PRESCRIPTION_SERVICE_PORT'
    'gateway' = 'GATEWAY_PORT'
}

function Get-ConfiguredPort {
    param(
        [string]$ServiceName
    )

    $portVar = $servicePortVars[$ServiceName]
    $rawValue = [Environment]::GetEnvironmentVariable($portVar, 'Process')

    if ([string]::IsNullOrWhiteSpace($rawValue)) {
        return $servicePorts[$ServiceName]
    }

    $parsed = 0
    if ([int]::TryParse($rawValue, [ref]$parsed)) {
        return $parsed
    }

    Write-Host "Invalid value '$rawValue' for $portVar. Using default $($servicePorts[$ServiceName])." -ForegroundColor Yellow
    return $servicePorts[$ServiceName]
}

function Stop-PortOwners {
    param(
        [int[]]$Ports
    )

    foreach ($port in $Ports) {
        $owners = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue |
            Select-Object -ExpandProperty OwningProcess -Unique

        foreach ($owner in $owners) {
            if ($owner) {
                Write-Host "Stopping PID $owner on port $port" -ForegroundColor Yellow
                if (-not $DryRun) {
                    Stop-Process -Id $owner -Force -ErrorAction SilentlyContinue
                }
            }
        }
    }
}

function Wait-ForPort {
    param(
        [int]$Port,
        [int]$TimeoutSeconds = 90
    )

    if ($DryRun) {
        return $true
    }

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $listening = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
        if ($listening) {
            return $true
        }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)

    return $false
}

function Start-ServiceInNewTerminal {
    param(
        [string]$ServiceName,
        [int]$Port
    )

    $portVar = $servicePortVars[$ServiceName]
    $runScript = Join-Path $PSScriptRoot 'run-service.ps1'
    $command = "`$env:$portVar=$Port; & '$runScript' -ServiceName '$ServiceName' -Port $Port"

    Write-Host "Starting $ServiceName on port $Port" -ForegroundColor Green

    if ($DryRun) {
        return
    }

    Start-Process -FilePath 'powershell.exe' -ArgumentList @(
        '-NoExit',
        '-ExecutionPolicy', 'Bypass',
        '-Command', $command
    ) | Out-Null
}

$configuredPorts = @{}
foreach ($service in $serviceOrder) {
    $configuredPorts[$service] = Get-ConfiguredPort -ServiceName $service
}

Write-Host "Planned startup:" -ForegroundColor Cyan
foreach ($service in $serviceOrder) {
    Write-Host " - $service : $($configuredPorts[$service])"
}

if (-not $SkipPortCleanup) {
    Write-Host "Cleaning service ports before startup..." -ForegroundColor Cyan
    Stop-PortOwners -Ports ($configuredPorts.Values | Select-Object -Unique)
}

# Warn if DB seems down, but do not block startup.
$dbCheck = Test-NetConnection -ComputerName 'localhost' -Port 3306 -WarningAction SilentlyContinue
if (-not $dbCheck.TcpTestSucceeded) {
    Write-Host "Warning: localhost:3306 is not reachable. Services using JPA may fail to start." -ForegroundColor Yellow
}

# Start Eureka first and wait for it.
Start-ServiceInNewTerminal -ServiceName 'eureka-server' -Port $configuredPorts['eureka-server']
if (-not (Wait-ForPort -Port $configuredPorts['eureka-server'] -TimeoutSeconds 120)) {
    Write-Host "Warning: Eureka did not open port $($configuredPorts['eureka-server']) in time. Continuing anyway." -ForegroundColor Yellow
}

# Start remaining services in order.
foreach ($service in $serviceOrder | Where-Object { $_ -ne 'eureka-server' }) {
    Start-ServiceInNewTerminal -ServiceName $service -Port $configuredPorts[$service]

    # Wait for each service port to reduce startup races.
    if (-not (Wait-ForPort -Port $configuredPorts[$service] -TimeoutSeconds 90)) {
        Write-Host "Warning: $service did not open port $($configuredPorts[$service]) in time." -ForegroundColor Yellow
    }
}

Write-Host ''
Write-Host 'Startup sequence completed.' -ForegroundColor Cyan
Write-Host 'If a service still fails, check the dedicated terminal window for its stack trace.' -ForegroundColor Cyan
