<#
.SYNOPSIS
  One-shot environment bootstrap for a fresh Windows machine
  (macOS / Linux: scripts/setup.sh).

.DESCRIPTION
  1. Checks Docker Desktop (prints install instructions when missing).
  2. Writes slotify-backend\.env from .env.example with the credentials below.
  3. Starts MySQL, Redis, MinIO (+ bucket), MailHog via docker compose.
  4. Writes slotify-admin\.env.local pointing at the API port.

  Credentials (override with -Param or by setting the environment variable):
    MySQL      root / $DbRootPassword            (API also connects as root)
    MinIO      $S3AccessKey / $S3SecretKey       (MinIO needs >= 8 characters)
    Admin web  $DemoAdminEmail / $DemoAdminPassword (login requires an email)

.EXAMPLE
  .\scripts\setup.ps1          # prepare .env files + start Docker stack
  .\scripts\setup.ps1 -Run     # ...and then start the API
#>
[CmdletBinding()]
param(
  [switch]$Run,
  [int]$ServerPort = $(if ($env:SERVER_PORT) { [int]$env:SERVER_PORT } else { 8081 }),
  [string]$DbRootPassword = $(if ($env:DB_ROOT_PASSWORD) { $env:DB_ROOT_PASSWORD } else { 'root' }),
  [string]$S3AccessKey = $(if ($env:S3_ACCESS_KEY) { $env:S3_ACCESS_KEY } else { 'admin' }),
  [string]$S3SecretKey = $(if ($env:S3_SECRET_KEY) { $env:S3_SECRET_KEY } else { 'admin1234' }),
  [string]$DemoAdminEmail = $(if ($env:DEMO_ADMIN_EMAIL) { $env:DEMO_ADMIN_EMAIL } else { 'admin@admin.com' }),
  [string]$DemoAdminPassword = $(if ($env:DEMO_ADMIN_PASSWORD) { $env:DEMO_ADMIN_PASSWORD } else { 'admin' })
)

$ErrorActionPreference = 'Stop'
$BackendDir = Split-Path -Parent $PSScriptRoot
$AdminDir = Join-Path (Split-Path -Parent $BackendDir) 'slotify-admin'

function Info($msg) { Write-Host "==> $msg" -ForegroundColor Cyan }
function Warn($msg) { Write-Host "WARN $msg" -ForegroundColor Yellow }
function Fail($msg) { Write-Host "ERROR $msg" -ForegroundColor Red; exit 1 }
function Random-Secret([int]$Length) {
  -join ((48..57) + (65..90) + (97..122) | Get-Random -Count $Length | ForEach-Object { [char]$_ })
}

# --- 1. Docker ---------------------------------------------------------------
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
  Fail 'Docker is not installed. Install Docker Desktop: winget install Docker.DockerDesktop  (or https://docs.docker.com/desktop/install/windows-install/), start it, then rerun.'
}
docker info *> $null
if ($LASTEXITCODE -ne 0) { Fail 'Docker daemon is not running. Start Docker Desktop and retry.' }
docker compose version *> $null
if ($LASTEXITCODE -ne 0) { Fail 'Docker Compose v2 is required (docker compose).' }
if ($S3SecretKey.Length -lt 8) { Fail "MinIO requires a secret key of at least 8 characters (got '$S3SecretKey')." }

# --- 2. Backend .env -----------------------------------------------------------
Set-Location $BackendDir
if (Test-Path .env) {
  Copy-Item .env (".env.bak." + (Get-Date -Format 'yyyyMMddHHmmss'))
  Warn 'Existing .env backed up.'
}
Copy-Item .env.example .env -Force

# Replace KEY=... in .env (or append when absent).
$envLines = [System.Collections.Generic.List[string]](Get-Content .env)
function Set-EnvValue([string]$Key, [string]$Value) {
  $index = $envLines.FindIndex({ param($l) $l -match "^$([regex]::Escape($Key))=" })
  if ($index -ge 0) { $envLines[$index] = "$Key=$Value" } else { $envLines.Add("$Key=$Value") }
}
Set-EnvValue 'SERVER_PORT' $ServerPort
Set-EnvValue 'SEED_DEMO_DATA' 'true'
Set-EnvValue 'APP_BASE_URL' "http://localhost:$ServerPort"
Set-EnvValue 'DB_USER' 'root'
Set-EnvValue 'DB_PASSWORD' $DbRootPassword
Set-EnvValue 'DB_ROOT_PASSWORD' $DbRootPassword
Set-EnvValue 'S3_ACCESS_KEY' $S3AccessKey
Set-EnvValue 'S3_SECRET_KEY' $S3SecretKey
Set-EnvValue 'DEMO_ADMIN_EMAIL' $DemoAdminEmail
Set-EnvValue 'DEMO_ADMIN_PASSWORD' $DemoAdminPassword
Set-EnvValue 'JWT_SECRET' (Random-Secret 80)
# LF line endings without BOM: the file is also read by docker compose.
[System.IO.File]::WriteAllText((Join-Path $BackendDir '.env'), ($envLines -join "`n") + "`n")
Info "Wrote $BackendDir\.env"

# --- 3. Docker stack -----------------------------------------------------------
Info 'Starting MySQL, Redis, MinIO, MailHog...'
docker compose up -d
if ($LASTEXITCODE -ne 0) { Fail 'docker compose up failed.' }
Info 'Waiting for MySQL to be healthy...'
$status = 'starting'
for ($i = 0; $i -lt 60 -and $status -ne 'healthy'; $i++) {
  Start-Sleep -Seconds 2
  $status = (docker inspect -f '{{.State.Health.Status}}' slotify-mysql 2>$null)
}
if ($status -ne 'healthy') { Fail 'MySQL did not become healthy; check: docker compose logs mysql' }
docker compose up -d minio-init *> $null
Info 'Docker stack is up.'

# --- 4. Admin web .env.local ---------------------------------------------------
if (Test-Path $AdminDir) {
  $adminEnv = @(
    "NEXT_PUBLIC_API_URL=http://localhost:$ServerPort",
    "API_URL=http://localhost:$ServerPort",
    "NEXT_PUBLIC_WS_URL=ws://localhost:$ServerPort/ws",
    "AUTH_SECRET=$(Random-Secret 48)",
    'AUTH_URL=http://localhost:3000'
  ) -join "`n"
  [System.IO.File]::WriteAllText((Join-Path $AdminDir '.env.local'), $adminEnv + "`n")
  Info "Wrote $AdminDir\.env.local"
} else {
  Warn 'slotify-admin not found next to slotify-backend; skipped its .env.local'
}

# --- Summary -------------------------------------------------------------------
Write-Host @"

Ready. Credentials:
  MySQL       localhost:3306  db=slotify  user=root  password=$DbRootPassword
  MinIO       http://localhost:9001 (console)  $S3AccessKey / $S3SecretKey
  Admin web   http://localhost:3000  $DemoAdminEmail / $DemoAdminPassword
  Other demo  owner@ / staff@ / customer@slotify.demo, password $DemoAdminPassword
  MailHog     http://localhost:8025

Start the API (reads .env, seeds demo data on first run):
  cd $BackendDir; .\mvnw.cmd spring-boot:run
Start the admin web:
  cd $AdminDir; pnpm install; pnpm dev
"@

# --- Optional: run the API now ------------------------------------------------
if ($Run) {
  Info "Starting the API on port $ServerPort..."
  & (Join-Path $BackendDir 'mvnw.cmd') spring-boot:run
}
