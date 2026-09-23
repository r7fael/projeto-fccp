<#
.SINOPSE
Sobe o TicketFlow nos 3 modos de sincronizacao (SEM_TRAVA, TRAVA_GLOBAL,
TRAVA_POR_ASSENTO), dispara reservas concorrentes pro mesmo assento em
cada um, e imprime uma tabela comparando os resultados.

.EXEMPLO
.\demo-concorrencia.ps1
.\demo-concorrencia.ps1 -Threads 50 -AssentoId 2
#>
param(
    [int]$AssentoId = 1,
    [int]$Threads = 30
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$root = Split-Path -Parent $scriptDir
$baseUrl = "http://localhost:8080"

function Esperar-Servidor {
    param([int]$TimeoutSegundos = 60)
    $deadline = (Get-Date).AddSeconds($TimeoutSegundos)
    while ((Get-Date) -lt $deadline) {
        try {
            $r = Invoke-WebRequest -Uri "$baseUrl/saude" -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
            if ($r.StatusCode -eq 200) { return $true }
        } catch {}
        Start-Sleep -Milliseconds 500
    }
    return $false
}

function Parar-ServidorNaPorta {
    param([int]$Porta = 8080)
    $linhas = netstat -ano | Select-String ":$Porta\s" | Select-String "LISTENING"
    $pids = foreach ($linha in $linhas) {
        $colunas = ($linha -split '\s+') | Where-Object { $_ -ne "" }
        $colunas[-1]
    }
    foreach ($procId in ($pids | Select-Object -Unique)) {
        try {
            taskkill /F /PID $procId | Out-Null
        } catch {}
    }
}

$modos = @("SEM_TRAVA", "TRAVA_GLOBAL", "TRAVA_POR_ASSENTO")
$resumo = @()

Parar-ServidorNaPorta -Porta 8080

foreach ($modo in $modos) {
    Write-Host ""
    Write-Host "=== Modo: $modo ===" -ForegroundColor Cyan

    $env:TICKETFLOW_MODO_SINCRONIZACAO = $modo
    Push-Location $root
    Start-Process -FilePath ".\mvnw.cmd" -ArgumentList "-q", "spring-boot:run" -WindowStyle Hidden
    Pop-Location

    if (-not (Esperar-Servidor -TimeoutSegundos 60)) {
        Write-Host "Servidor nao subiu a tempo no modo $modo" -ForegroundColor Red
        Parar-ServidorNaPorta -Porta 8080
        continue
    }

    $resultado = & "$scriptDir\teste-carga.ps1" -BaseUrl $baseUrl -AssentoId $AssentoId -Threads $Threads
    $resultado | Add-Member -NotePropertyName Modo -NotePropertyValue $modo
    $resumo += $resultado

    Write-Host "Encerrando servidor..."
    Parar-ServidorNaPorta -Porta 8080
    Start-Sleep -Seconds 2
}

Remove-Item Env:\TICKETFLOW_MODO_SINCRONIZACAO -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "=== Resumo comparativo (assento $AssentoId, $Threads threads) ===" -ForegroundColor Yellow
$resumo | Select-Object Modo, Sucesso, Conflito, Outros | Format-Table -AutoSize
