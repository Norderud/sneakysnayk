param(
    [int]$Games = 10
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

$snakes = @()
for ($i = 0; $i -lt $Name.Count; $i++) {
    $snakes += [PSCustomObject]@{
        Name = $Name[$i]
        Url  = "http://localhost:$($Port[$i])"
    }
}

$wins = @{}
foreach ($s in $snakes) {
    $wins[$s.Name] = 0
}

$draws = 0
$totalTurns = 0
$startTime = Get-Date

Write-Host "Benchmark started at $startTime" -ForegroundColor Cyan
$snakeNames = $snakes | ForEach-Object { $_.Name }
Write-Host "Running $Games games between: $($snakeNames -join ', ')" -ForegroundColor Cyan

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
        Write-Error "BattleSnake CLI not found. Expected at: C:\Users\e117387\workspaces\random\rules\battlesnake.exe"
        return
    }
}

for ($i = 1; $i -le $Games; $i++) {
    # Build the command arguments
    $argList = @("play")
    foreach ($s in $snakes) {
        $argList += "-u"
        $argList += $s.Url
        $argList += "-n"
        $argList += $s.Name
    }

    # Run game and capture output.
    $output = & $cliPath $argList 2>&1 | Out-String
    
    if ($output -match "Game completed after (\d+) turns\. (.*) was the winner\.") {
        $turns = [int]$Matches[1]
        $winner = $Matches[2].Trim()
        $totalTurns += $turns
        
        if ($wins.ContainsKey($winner)) {
            $wins[$winner]++
        } else {
            Write-Warning "Unknown winner: $winner"
        }
    } elseif ($output -match "Game completed after (\d+) turns\. It was a draw\.") {
        $turns = [int]$Matches[1]
        $totalTurns += $turns
        $draws++
    }

    if ($i % 5 -eq 0 -or $i -eq $Games) {
        $elapsed = (Get-Date) - $startTime
        $avgTime = $elapsed.TotalSeconds / $i
        $remaining = ($Games - $i) * $avgTime
        $percent = [math]::Round(($i / $Games) * 100, 1)
        Write-Host "Progress: $i/$Games ($percent%) - Avg: $([math]::Round($avgTime, 3))s/game - Est. remaining: $([math]::Round($remaining))s" -ForegroundColor Gray
    }
}

$endTime = Get-Date
$duration = $endTime - $startTime

if ($Games -gt 0) {
    $avgTurns = $totalTurns / $Games

    Write-Host "`n--- Benchmark Results ---" -ForegroundColor Green
    Write-Host "Total Games: $Games"
    Write-Host "Total Duration: $([math]::Round($duration.TotalSeconds, 2))s"
    Write-Host "Average Turns: $([math]::Round($avgTurns, 1))"
    Write-Host "`nStandings:"
    
    $sortedWins = $wins.GetEnumerator() | Sort-Object Value -Descending
    foreach ($entry in $sortedWins) {
        $rate = ($entry.Value / $Games) * 100
        Write-Host "  $($entry.Key): $($entry.Value) wins ($([math]::Round($rate, 2))%)"
    }
    
    $drawRate = ($draws / $Games) * 100
    Write-Host "Draws: $draws ($([math]::Round($drawRate, 2))%)"
}
