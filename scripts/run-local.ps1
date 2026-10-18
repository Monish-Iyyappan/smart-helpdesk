# =============================================================================
# Smart Helpdesk - Local Execution Runner Script
# Sets environment variables in process scope only; never logs or stores passwords.
# =============================================================================

[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [string]$Database = "test",

    [switch]$Console
)

$ErrorActionPreference = "Stop"

# Map predefined database names or accept custom database name
$resolvedDb = switch ($Database.ToLower()) {
    "test"   { "smart_helpdesk_test" }
    "demo"   { "smart_helpdesk_demo" }
    "real"   { "smart_helpdesk" }
    default  { $Database }
}

Write-Host "===================================================="
Write-Host "        SMART HELPDESK - LOCAL LAUNCHER             "
Write-Host "===================================================="
Write-Host "Target Database : $resolvedDb"
Write-Host "JDBC URL        : jdbc:mysql://localhost:3306/$resolvedDb"
$user = if (-not [string]::IsNullOrWhiteSpace($env:DB_USERNAME)) { $env:DB_USERNAME } else { "root" }
Write-Host "Database User   : $user"
Write-Host "Console Mode    : $(if ($Console) { 'Enabled (Standalone Console)' } else { 'Disabled (Web Server)' })"
Write-Host "===================================================="

# Prompt for password securely
$secPass = Read-Host "Enter MySQL password for '$user' (input hidden)" -AsSecureString

# Safely extract plain text from SecureString supporting any special characters
$bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secPass)
$plainPass = [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
[System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)

# Set environment variables for this process only
$env:DB_URL = "jdbc:mysql://localhost:3306/$resolvedDb"
$env:DB_USERNAME = $user
$env:DB_PASSWORD = $plainPass

# Navigate to project root (directory containing pom.xml)
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

if ($Console) {
    Write-Host "`nBuilding application package (skipping tests)..."
    mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Maven package build failed."
        exit $LASTEXITCODE
    }

    $jarPath = Join-Path $projectRoot "target\smart-helpdesk-0.0.1-SNAPSHOT.jar"
    if (-not (Test-Path $jarPath)) {
        $jarPath = Get-ChildItem -Path (Join-Path $projectRoot "target") -Filter "*.jar" |
                   Where-Object { $_.Name -notmatch "original" } |
                   Select-Object -First 1 -ExpandProperty FullName
    }

    Write-Host "`nLaunching Smart Helpdesk Console UI..."
    java -jar $jarPath --app.console.enabled=true --server.port=0
} else {
    Write-Host "`nLaunching Spring Boot application via Maven..."
    mvn spring-boot:run
}
