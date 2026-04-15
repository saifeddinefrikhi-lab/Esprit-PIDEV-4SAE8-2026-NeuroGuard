param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('eureka-server','gateway','user-service','medical-history-service','careplan-service','pharmacy-service','prescription-service')]
    [string]$ServiceName,
    [int]$Port
)

$ErrorActionPreference = 'Stop'

$servicePortDefaults = @{
    'eureka-server' = 8761
    'gateway' = 8083
    'user-service' = 8081
    'medical-history-service' = 8082
    'careplan-service' = 8084
    'pharmacy-service' = 8085
    'prescription-service' = 8089
}

$servicePortVars = @{
    'eureka-server' = 'EUREKA_PORT'
    'gateway' = 'GATEWAY_PORT'
    'user-service' = 'USER_SERVICE_PORT'
    'medical-history-service' = 'MEDICAL_HISTORY_SERVICE_PORT'
    'careplan-service' = 'CAREPLAN_SERVICE_PORT'
    'pharmacy-service' = 'PHARMACY_SERVICE_PORT'
    'prescription-service' = 'PRESCRIPTION_SERVICE_PORT'
}

$targetPort = if ($PSBoundParameters.ContainsKey('Port')) { $Port } else { $servicePortDefaults[$ServiceName] }
$portEnvVar = $servicePortVars[$ServiceName]

# Free the target port if another process is already listening.
$owners = Get-NetTCPConnection -State Listen -LocalPort $targetPort -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty OwningProcess -Unique

foreach ($owner in $owners) {
    if ($owner) {
        Write-Host "Stopping PID $owner on port $targetPort..." -ForegroundColor Yellow
        Stop-Process -Id $owner -Force -ErrorAction SilentlyContinue
    }
}

Set-Item -Path "Env:$portEnvVar" -Value "$targetPort"

$servicePath = Join-Path $PSScriptRoot $ServiceName
if (-not (Test-Path $servicePath)) {
    throw "Service folder not found: $servicePath"
}

Push-Location $servicePath
try {
    Write-Host "Starting $ServiceName on port $targetPort..." -ForegroundColor Green
    mvn spring-boot:run
}
finally {
    Pop-Location
}
