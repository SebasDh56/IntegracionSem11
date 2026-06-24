$ErrorActionPreference = "Stop"

$RabbitMqBaseUrl = "http://localhost:15672/api"
$Credential = New-Object System.Management.Automation.PSCredential(
    "guest",
    (ConvertTo-SecureString "guest" -AsPlainText -Force)
)

$ExchangeName = "campus.exchange"
$ExchangeEncoded = [System.Uri]::EscapeDataString($ExchangeName)
$VHostEncoded = [System.Uri]::EscapeDataString("/")

$Queues = @(
    "campus.requests.in",
    "campus.admissions.queue",
    "campus.payments.queue",
    "campus.support.queue",
    "campus.academic.queue",
    "campus.manual-review.queue"
)

Write-Host "Esperando a que RabbitMQ Management este disponible..."
$Ready = $false
for ($Attempt = 1; $Attempt -le 30; $Attempt++) {
    try {
        Invoke-RestMethod `
            -Method Get `
            -Uri "$RabbitMqBaseUrl/overview" `
            -Credential $Credential `
            -DisableKeepAlive | Out-Null

        $Ready = $true
        break
    } catch {
        Start-Sleep -Seconds 2
    }
}

if (-not $Ready) {
    throw "RabbitMQ Management no esta disponible en $RabbitMqBaseUrl despues de 60 segundos."
}

Write-Host "Creando exchange direct: $ExchangeName"
$ExchangeBody = @{
    type = "direct"
    durable = $true
    auto_delete = $false
    internal = $false
    arguments = @{}
} | ConvertTo-Json -Depth 5

Invoke-RestMethod `
    -Method Put `
    -Uri "$RabbitMqBaseUrl/exchanges/$VHostEncoded/$ExchangeEncoded" `
    -Credential $Credential `
    -DisableKeepAlive `
    -ContentType "application/json" `
    -Body $ExchangeBody | Out-Null

foreach ($Queue in $Queues) {
    Write-Host "Creando cola: $Queue"
    $QueueEncoded = [System.Uri]::EscapeDataString($Queue)
    $QueueBody = @{
        durable = $true
        auto_delete = $false
        arguments = @{}
    } | ConvertTo-Json -Depth 5

    Invoke-RestMethod `
        -Method Put `
        -Uri "$RabbitMqBaseUrl/queues/$VHostEncoded/$QueueEncoded" `
        -Credential $Credential `
        -DisableKeepAlive `
        -ContentType "application/json" `
        -Body $QueueBody | Out-Null

    Write-Host "Creando binding: $ExchangeName -> $Queue con routing key $Queue"
    $BindingBody = @{
        routing_key = $Queue
        arguments = @{}
    } | ConvertTo-Json -Depth 5

    Invoke-RestMethod `
        -Method Post `
        -Uri "$RabbitMqBaseUrl/bindings/$VHostEncoded/e/$ExchangeEncoded/q/$QueueEncoded" `
        -Credential $Credential `
        -DisableKeepAlive `
        -ContentType "application/json" `
        -Body $BindingBody | Out-Null
}

Write-Host "RabbitMQ configurado correctamente."
