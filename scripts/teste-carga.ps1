<#
.SINOPSE
Dispara N reservas concorrentes contra o mesmo assento, num servidor
TicketFlow ja rodando, e conta quantas tiveram sucesso (201) vs conflito (409).

Usa um runspace pool (varias execucoes dentro do mesmo processo) em vez de
Start-Job, entao aguenta algumas centenas de threads sem ficar pesado.

Com -AoVivo, imprime cada resultado assim que ele chega (na ordem em que as
threads terminam, nao na ordem em que foram disparadas) - bom pra ver a
concorrencia acontecendo em vez de so o resumo no final.

.EXEMPLO
.\teste-carga.ps1 -AssentoId 1 -Threads 30
.\teste-carga.ps1 -AssentoId 1 -Threads 10 -AoVivo
#>
param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$AssentoId = 1,
    [int]$Threads = 30,
    [switch]$AoVivo
)

$scriptBlock = {
    param($url, $assentoId, $usuario)
    $inicio = Get-Date
    try {
        $corpo = @{ usuario = $usuario; assentoId = $assentoId } | ConvertTo-Json
        $resposta = Invoke-WebRequest -Uri "$url/reservas" -Method Post -ContentType "application/json" -Body $corpo -UseBasicParsing -ErrorAction Stop
        $status = [int]$resposta.StatusCode
    } catch {
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
        } else {
            $status = -1
        }
    }
    [PSCustomObject]@{
        Usuario    = $usuario
        Status     = $status
        DuracaoMs  = [int]((Get-Date) - $inicio).TotalMilliseconds
    }
}

$pool = [runspacefactory]::CreateRunspacePool(1, [Math]::Max($Threads, 1))
$pool.Open()

$tarefas = @()
for ($i = 1; $i -le $Threads; $i++) {
    $usuario = "usuario-$i"
    $ps = [powershell]::Create()
    $ps.RunspacePool = $pool
    [void]$ps.AddScript($scriptBlock).AddArgument($BaseUrl).AddArgument($AssentoId).AddArgument($usuario)
    $tarefas += [PSCustomObject]@{ Usuario = $usuario; PS = $ps; Handle = $ps.BeginInvoke(); Coletado = $false }
}

if ($AoVivo) { Write-Host "Disparadas $Threads threads pro assento $AssentoId - coletando conforme cada uma termina:`n" }

$resultados = @()
$pendentes = $tarefas.Count
while ($pendentes -gt 0) {
    foreach ($tarefa in $tarefas) {
        if (-not $tarefa.Coletado -and $tarefa.Handle.IsCompleted) {
            $r = $tarefa.PS.EndInvoke($tarefa.Handle)
            $tarefa.PS.Dispose()
            $tarefa.Coletado = $true
            $pendentes--
            $resultados += $r

            if ($AoVivo) {
                $cor = switch ($r.Status) {
                    201 { "Green" }
                    409 { "Yellow" }
                    default { "Red" }
                }
                $rotulo = switch ($r.Status) {
                    201 { "CRIADA (sucesso)" }
                    409 { "CONFLITO (perdeu a corrida)" }
                    default { "ERRO/OUTRO" }
                }
                Write-Host ("{0,-14} -> {1} [{2}]  ({3}ms)" -f $r.Usuario, $r.Status, $rotulo, $r.DuracaoMs) -ForegroundColor $cor
            }
        }
    }
    if ($pendentes -gt 0) { Start-Sleep -Milliseconds 15 }
}

$pool.Close()
$pool.Dispose()

$sucesso = ($resultados | Where-Object { $_.Status -eq 201 }).Count
$conflito = ($resultados | Where-Object { $_.Status -eq 409 }).Count
$outros = $resultados.Count - $sucesso - $conflito

Write-Host ""
Write-Host "=== Resultado do teste de carga ==="
Write-Host "Assento: $AssentoId | Threads: $Threads"
Write-Host "201 (sucesso): $sucesso"
Write-Host "409 (conflito): $conflito"
Write-Host "Outros/erros: $outros"

[PSCustomObject]@{
    AssentoId = $AssentoId
    Threads   = $Threads
    Sucesso   = $sucesso
    Conflito  = $conflito
    Outros    = $outros
}
