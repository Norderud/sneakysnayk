param (
    [int]$Width = 11,
    [int]$Height = 11,
    [switch]$Browser
)

# Access all arguments passed to the script via $args
$Name = @()
$Port = @()

for ($i = 0; $i -lt $args.Count; $i++) {
    $arg = $args[$i]
    if ($arg -eq "-n" -or $arg -eq "-Name") {
        $Name += $args[++$i]
    } elseif ($arg -eq "-p" -or $arg -eq "-Port") {
        $Port += $args[++$i]
    }
}

# If no names/ports provided, use defaults
if ($Name.Count -eq 0) {
    $Name = @("Bully", "Midas", "Duelist", "Parasite")
    $Port = @("8080", "8081", "8082", "8083")
}

if ($Name.Count -ne $Port.Count) {
    Write-Error "Number of names must match number of ports."
    return
}

# Find battlesnake executable: check rules engine dir, then project root, then PATH
$cliPath = "C:\Users\e117387\workspaces\random\rules\battlesnake.exe"
if (!(Test-Path $cliPath)) {
    $cliPath = Join-Path $PSScriptRoot "..\battlesnake.exe"
}
if (!(Test-Path $cliPath)) {
    $cli = Get-Command battlesnake -ErrorAction SilentlyContinue
    if ($cli) {
        $cliPath = $cli.Source
    } else {
        Write-Host "--- BattleSnake CLI not found ---" -ForegroundColor Yellow
        Write-Host "To run games locally, you need the 'battlesnake' CLI."
        Write-Host "Expected at: C:\Users\e117387\workspaces\random\rules\battlesnake.exe"
        Write-Host "Or place 'battlesnake.exe' in the project root or your PATH."
        return
    }
}

$playArgs = @("play")
for ($i = 0; $i -lt $Name.Count; $i++) {
    $playArgs += "--name"
    $playArgs += $Name[$i]
    $playArgs += "--url"
    $playArgs += "http://localhost:$($Port[$i])"
}
$playArgs += "--width"
$playArgs += $Width
$playArgs += "--height"
$playArgs += $Height

if ($Browser) {
    $playArgs += "--browser"
}

Write-Host "Running local game: $($Name -join ' vs ') ($Width x $Height)" -ForegroundColor Cyan
& $cliPath $playArgs
