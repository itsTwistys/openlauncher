param([string]$BackupDirectory = $PSScriptRoot)
$ErrorActionPreference = 'Stop'
$repo = 'itsTwistys/openlauncher'
$keyPath = Join-Path $BackupDirectory 'openlauncher-preview.p12'
$passwordPath = Join-Path $BackupDirectory 'keystore-password.txt'
if (!(Test-Path $keyPath) -or !(Test-Path $passwordPath)) {
    throw 'Extract the private signing backup and pass its folder as -BackupDirectory.'
}
if (!(Get-Command gh -ErrorAction SilentlyContinue)) {
    throw 'Install GitHub CLI from https://cli.github.com, reopen PowerShell, then run this script again.'
}
& gh auth status --hostname github.com
if ($LASTEXITCODE -ne 0) {
    & gh auth login --hostname github.com --git-protocol https --web
    if ($LASTEXITCODE -ne 0) { throw 'GitHub sign-in failed.' }
}
function Set-RepositorySecret([string]$Name, [string]$Value) {
    $start = New-Object System.Diagnostics.ProcessStartInfo
    $start.FileName = (Get-Command gh).Source
    $start.Arguments = "secret set $Name --repo $repo"
    $start.UseShellExecute = $false
    $start.RedirectStandardInput = $true
    $process = [System.Diagnostics.Process]::Start($start)
    try {
        $process.StandardInput.Write($Value)
        $process.StandardInput.Close()
        $process.WaitForExit()
        if ($process.ExitCode -ne 0) { throw "Could not save $Name. No key was regenerated." }
    } finally { $process.Dispose() }
}
$password = [System.IO.File]::ReadAllText($passwordPath).Trim()
if ($password.Length -lt 16) { throw 'Signing password is missing or invalid.' }
$keystore = [Convert]::ToBase64String([System.IO.File]::ReadAllBytes($keyPath))
Set-RepositorySecret 'OPENLAUNCHER_PREVIEW_KEYSTORE_PASSWORD' $password
Set-RepositorySecret 'OPENLAUNCHER_PREVIEW_KEYSTORE_BASE64' $keystore
$password = $null
$keystore = $null
Write-Host 'Permanent signing key saved in GitHub Actions secrets. Keep your private backup.'
Write-Host 'Run Android preview manually on main to build and publish with this key.'
