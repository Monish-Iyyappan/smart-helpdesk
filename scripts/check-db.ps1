# =============================================================================
# Smart Helpdesk - Database Connection and Health Audit Script
# ASCII only, read-only verification, never prints or stores passwords.
# =============================================================================

[CmdletBinding()]
param()

$ErrorActionPreference = "Continue"

Write-Host "===================================================="
Write-Host "          SMART HELPDESK - DATABASE AUDIT           "
Write-Host "===================================================="
Write-Host ""

# -----------------------------------------------------------------------------
# 1. Locate mysql.exe
# -----------------------------------------------------------------------------
Write-Host "Step 1: Locating mysql.exe client..."
$mysqlCmd = Get-Command mysql.exe -ErrorAction SilentlyContinue
$mysqlPath = $null

if ($mysqlCmd) {
    $mysqlPath = $mysqlCmd.Source
} else {
    $searchRoots = @(
        "C:\Program Files\MySQL",
        "C:\Program Files (x86)\MySQL",
        "C:\tools\mysql",
        "C:\xampp\mysql\bin"
    )
    foreach ($sr in $searchRoots) {
        if (Test-Path $sr) {
            $found = Get-ChildItem -Path $sr -Filter "mysql.exe" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($found) {
                $mysqlPath = $found.FullName
                break
            }
        }
    }
}

if ($mysqlPath) {
    Write-Host "[PASS] mysql.exe found at: $mysqlPath"
} else {
    Write-Host "[FAIL] mysql.exe could not be found in PATH or standard directories."
    Write-Host "       Fix: Install MySQL or add its bin directory to your PATH."
}

# -----------------------------------------------------------------------------
# 2. Check MySQL Windows Service Status
# -----------------------------------------------------------------------------
Write-Host "`nStep 2: Checking MySQL Windows Service..."
$services = Get-Service -Name *mysql* -ErrorAction SilentlyContinue

if ($services) {
    $running = $services | Where-Object { $_.Status -eq 'Running' }
    if ($running) {
        $svcNames = ($running | ForEach-Object { "$($_.Name) ($($_.Status))" }) -join ", "
        Write-Host "[PASS] MySQL Windows Service is active: $svcNames"
    } else {
        $svcNames = ($services | ForEach-Object { "$($_.Name) ($($_.Status))" }) -join ", "
        Write-Host "[FAIL] MySQL Windows Service is not running: $svcNames"
        Write-Host "       Fix: Run 'Start-Service $($services[0].Name)' in an administrator PowerShell."
    }
} else {
    Write-Host "[WARN] No Windows service matching '*mysql*' was found."
    Write-Host "       Note: If running MySQL via XAMPP, Docker, or ZIP archive, verify the process is active."
}

# -----------------------------------------------------------------------------
# 3. Test TCP Connection to localhost:3306
# -----------------------------------------------------------------------------
Write-Host "`nStep 3: Checking TCP connection to localhost:3306..."
try {
    $netTest = Test-NetConnection -ComputerName "localhost" -Port 3306 -WarningAction SilentlyContinue
    if ($netTest.TcpTestSucceeded) {
        Write-Host "[PASS] Successfully connected to localhost:3306 (TCP port is open and listening)."
    } else {
        Write-Host "[FAIL] Port 3306 is not listening on localhost."
        Write-Host "       Fix: Ensure MySQL server is running and configured on port 3306."
    }
} catch {
    Write-Host "[FAIL] Failed to execute Test-NetConnection to localhost:3306."
    Write-Host "       Fix: Verify network stack and firewall permissions."
}

# -----------------------------------------------------------------------------
# 4. Check Environment Variables (Password length only, never the value)
# -----------------------------------------------------------------------------
Write-Host "`nStep 4: Checking environment variables..."

if ([string]::IsNullOrWhiteSpace($env:DB_URL)) {
    Write-Host "[INFO] DB_URL is not set (Default: jdbc:mysql://localhost:3306/smart_helpdesk)"
    $dbName = "smart_helpdesk"
} else {
    Write-Host "[INFO] DB_URL is set: $env:DB_URL"
    if ($env:DB_URL -match "3306/([^?/\s]+)") {
        $dbName = $matches[1]
    } else {
        $dbName = "smart_helpdesk"
    }
}

if ([string]::IsNullOrWhiteSpace($env:DB_USERNAME)) {
    Write-Host "[INFO] DB_USERNAME is not set (Default: root)"
    $dbUser = "root"
} else {
    Write-Host "[INFO] DB_USERNAME is set: $env:DB_USERNAME"
    $dbUser = $env:DB_USERNAME
}

if ([string]::IsNullOrEmpty($env:DB_PASSWORD)) {
    Write-Host "[INFO] DB_PASSWORD is not set in this terminal"
} else {
    $passLength = $env:DB_PASSWORD.Length
    Write-Host "[INFO] DB_PASSWORD is set (length: $passLength characters)"
}

# -----------------------------------------------------------------------------
# 5. Database Login & Table Verification (Read-Only)
# -----------------------------------------------------------------------------
Write-Host "`nStep 5: Database Login & Structure Verification..."

if (-not $mysqlPath) {
    Write-Host "[SKIP] Cannot perform MySQL queries because mysql.exe was not found."
    exit 0
}

Write-Host "Please provide the MySQL password for user '$dbUser' to verify database health."
$secPass = Read-Host "Enter MySQL password for '$dbUser' (input hidden)" -AsSecureString
$bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secPass)
$plainPass = [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
[System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)

# Helper function to invoke mysql.exe with credentials safely without logging password
function Invoke-MySqlQuery {
    param([string]$Query)
    $output = & $mysqlPath -u $dbUser "--password=$plainPass" -h "localhost" -P 3306 -N -s -e "$Query" 2>&1
    return $output
}

# A. Test login and verify database exists
$dbCheckQuery = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '$dbName';"
$dbExists = Invoke-MySqlQuery -Query $dbCheckQuery

$dbError = $dbExists | Where-Object { $_ -match "ERROR" -or $_ -match "Access denied" }
if ($dbError) {
    Write-Host "[FAIL] Authentication or connection failed:"
    Write-Host "       $dbError"
    if ($dbError -match "Access denied") {
        Write-Host "       Fix: Verify username and password credentials."
    } elseif ($dbError -match "Can't connect") {
        Write-Host "       Fix: Ensure MySQL server is running on localhost:3306."
    }
    $plainPass = $null
    exit 1
}

if ($dbExists -eq $dbName) {
    Write-Host "[PASS] Database '$dbName' exists."
} else {
    Write-Host "[FAIL] Database '$dbName' does not exist."
    Write-Host "       Fix: Log in to MySQL and run: CREATE DATABASE $dbName; then import database\schema.sql."
    $plainPass = $null
    exit 1
}

# B. Check all six required tables exist
$expectedTables = @("users", "categories", "incidents", "tickets", "ticket_participants", "comments")
$tablesQuery = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '$dbName';"
$foundTables = Invoke-MySqlQuery -Query $tablesQuery

$missingTables = @()
foreach ($tbl in $expectedTables) {
    if ($foundTables -notcontains $tbl) {
        $missingTables += $tbl
    }
}

if ($missingTables.Count -eq 0) {
    Write-Host "[PASS] All 6 required tables exist ($($expectedTables -join ', '))."
} else {
    Write-Host "[FAIL] Missing tables in '$dbName': $($missingTables -join ', ')"
    Write-Host "       Fix: Import schema: Get-Content database\schema.sql | mysql -u $dbUser -p $dbName"
}

# C. Check categories count
$catCountQuery = "SELECT COUNT(*) FROM $dbName.categories;"
$catCountResult = Invoke-MySqlQuery -Query $catCountQuery
$catCount = 0
[int]::TryParse("$catCountResult", [ref]$catCount) | Out-Null

if ($catCount -gt 0) {
    Write-Host "[PASS] Categories table contains $catCount seeded categories."
} else {
    Write-Host "[FAIL] Categories table is empty (count: $catCount)."
    Write-Host "       Fix: Seed database: Get-Content database\seed_data.sql | mysql -u $dbUser -p $dbName"
}

# D. Compare MySQL NOW() with Windows system clock
$timeQuery = "SELECT UNIX_TIMESTAMP(NOW());"
$timeResult = Invoke-MySqlQuery -Query $timeQuery
$dbEpoch = 0
[long]::TryParse("$timeResult", [ref]$dbEpoch) | Out-Null

if ($dbEpoch -gt 0) {
    $winEpoch = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $diffSeconds = [Math]::Abs($dbEpoch - $winEpoch)

    if ($diffSeconds -le 120) {
        Write-Host "[PASS] Database clock matches system time (skew: $diffSeconds second(s))."
    } else {
        Write-Host "[WARN] Clock drift detected! Database time differs from Windows by $diffSeconds seconds (> 2 minutes)."
        Write-Host "       Fix: Ensure MySQL server time zone and system clocks are synchronized."
    }
} else {
    Write-Host "[WARN] Could not retrieve MySQL NOW() timestamp."
}

# Clear plain text password from memory
$plainPass = $null

Write-Host ""
Write-Host "===================================================="
Write-Host "Audit completed. No changes were made to database."
Write-Host "===================================================="
