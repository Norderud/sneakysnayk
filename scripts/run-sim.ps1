Write-Host "Compiling and running in-memory simulation..." -ForegroundColor Cyan
mvn test-compile
if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed." -ForegroundColor Red
    exit $LASTEXITCODE
}

mvn exec:java -Dexec.mainClass="sneak.snaek.sim.LocalSimulationRunner" -Dexec.classpathScope="test"
