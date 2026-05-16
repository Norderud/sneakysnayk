param(
    [int]$Games = 10,
    [string]$UrlA = "http://localhost:8080",
    [string]$UrlB = "http://localhost:8081",
    [string]$NameA = "TerritorialBully",
    [string]$NameB = "OldVersion"
)

$winsA = 0
$winsB = 0
$draws = 0
$totalTurns = 0
$startTime = Get-Date

Write-Host "Benchmark started at $startTime" -ForegroundColor Cyan
Write-Host "Running $Games games between $NameA ($UrlA) and $NameB ($UrlB)" -ForegroundColor Cyan

for ($i = 1; $i -le $Games; $i++) {
    # Run game and capture output. 2>&1 to catch all info logs.
    $output = .\battlesnake.exe play -u $UrlA -n $NameA -u $UrlB -n $NameB 2>&1 | Out-String
    
    if ($output -match "Game completed after (\d+) turns\. (.*) was the winner\.") {
        $turns = [int]$Matches[1]
        $winner = $Matches[2]
        $totalTurns += $turns
        
        if ($winner -eq $NameA) { $winsA++ }
        elseif ($winner -eq $NameB) { $winsB++ }
        else { Write-Warning "Unknown winner: $winner" }
    } elseif ($output -match "Game completed after (\d+) turns\. It was a draw\.") {
        $turns = [int]$Matches[1]
        $totalTurns += $turns
        $draws++
    }

    if ($i % 10 -eq 0 -or $i -eq $Games) {
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
    $winRateA = ($winsA / $Games) * 100
    $winRateB = ($winsB / $Games) * 100
    $drawRate = ($draws / $Games) * 100
    $avgTurns = $totalTurns / $Games

    Write-Host "`n--- Benchmark Results ---" -ForegroundColor Green
    Write-Host "Total Games: $Games"
    Write-Host "Total Duration: $([math]::Round($duration.TotalSeconds, 2))s"
    Write-Host "Average Turns: $([math]::Round($avgTurns, 1))"
    Write-Host "`n$NameA ($UrlA):"
    Write-Host "  Wins: $winsA ($([math]::Round($winRateA, 2))%)"
    Write-Host "$NameB ($UrlB):"
    Write-Host "  Wins: $winsB ($([math]::Round($winRateB, 2))%)"
    Write-Host "Draws: $draws ($([math]::Round($drawRate, 2))%)"
    
    # Output simple CSV-like format for easy parsing if needed
    # Results|$Games|$winsA|$winsB|$draws|$avgTurns
}
