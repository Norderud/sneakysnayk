param (
    [string]$Url1 = "http://localhost:8080",
    [string]$Url2 = "http://localhost:8081",
    [int]$Width = 11,
    [int]$Height = 11,
    [switch]$Browser
)

# Find battlesnake executable: check project root, then PATH
$cliPath = Join-Path $PSScriptRoot "..\battlesnake.exe"
if (!(Test-Path $cliPath)) {
    $cli = Get-Command battlesnake -ErrorAction SilentlyContinue
    if ($cli) {
        $cliPath = $cli.Source
    } else {
        Write-Host "--- BattleSnake CLI not found ---" -ForegroundColor Yellow
        Write-Host "To run games locally, you need the 'battlesnake' CLI."
        Write-Host "We tried to build it from the 'rules' directory, but it might be missing."
        Write-Host "Place 'battlesnake.exe' in the project root or your PATH."
        return
    }
}

$args = @("play", "--name", "Snake-A", "--url", $Url1, "--name", "Snake-B", "--url", $Url2, "--width", $Width, "--height", $Height)
if ($Browser) {
    $args += "--browser"
}

Write-Host "Running local game: Snake-A vs Snake-B ($Width x $Height)" -ForegroundColor Cyan
& $cliPath @args
