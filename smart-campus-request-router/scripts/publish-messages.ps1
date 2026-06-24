$ErrorActionPreference = "Stop"

$RabbitMqBaseUrl = "http://localhost:15672/api"
$Credential = New-Object System.Management.Automation.PSCredential(
    "guest",
    (ConvertTo-SecureString "guest" -AsPlainText -Force)
)

$ExchangeName = "campus.exchange"
$ExchangeEncoded = [System.Uri]::EscapeDataString($ExchangeName)
$VHostEncoded = [System.Uri]::EscapeDataString("/")
$RoutingKey = "campus.requests.in"

$Messages = @(
    @{
        request_id = "REQ-1001"
        student_name = "Ana Perez"
        student_document = "1712345678"
        request_type = "ADMISSION"
        channel = "web"
        created_at = "2026-06-10T10:30:00"
    },
    @{
        request_id = "REQ-1002"
        student_name = "Luis Morales"
        student_document = "1723456789"
        request_type = "PAYMENT"
        channel = "mobile"
        created_at = "2026-06-10T11:00:00"
    },
    @{
        request_id = "REQ-1003"
        student_name = "Carla Gomez"
        student_document = "1734567890"
        request_type = "SUPPORT"
        channel = "web"
        created_at = "2026-06-10T11:30:00"
    },
    @{
        request_id = "REQ-1004"
        student_name = "Diego Ruiz"
        student_document = "1745678901"
        request_type = "ACADEMIC"
        channel = "kiosk"
        created_at = "2026-06-10T12:00:00"
    },
    @{
        request_id = "REQ-1005"
        student_name = "Maria Torres"
        student_document = "1756789012"
        request_type = "LIBRARY"
        channel = "web"
        created_at = "2026-06-10T12:30:00"
    },
    @{
        request_id = "REQ-1006"
        student_name = ""
        request_type = "ADMISSION"
    }
)

foreach ($Message in $Messages) {
    $Payload = $Message | ConvertTo-Json -Depth 10 -Compress
    $PublishBody = @{
        properties = @{}
        routing_key = $RoutingKey
        payload = $Payload
        payload_encoding = "string"
    } | ConvertTo-Json -Depth 10

    Invoke-RestMethod `
        -Method Post `
        -Uri "$RabbitMqBaseUrl/exchanges/$VHostEncoded/$ExchangeEncoded/publish" `
        -Credential $Credential `
        -DisableKeepAlive `
        -ContentType "application/json" `
        -Body $PublishBody | Out-Null

    Write-Host "Mensaje publicado: $Payload"
}

Write-Host "Mensajes de prueba publicados correctamente."
