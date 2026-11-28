param(
    [Parameter(Mandatory)]
    [string]$OutputDirectory
)

$project = "AnaisPerron_SOEN343-Council"
$base = "https://sonarcloud.io"

# Request the token securely.
$secureToken = Read-Host "SonarQube token" -AsSecureString
$token = [System.Net.NetworkCredential]::new(
    "", $secureToken
).Password

$headers = @{
    Authorization = "Bearer $token"
}

New-Item -ItemType Directory -Force `
    -Path $OutputDirectory | Out-Null

# Export all file-level measurements.
$measures = @()
$page = 1

do {
    $query = @{
        component  = $project
        metricKeys = "ncloc,complexity,cognitive_complexity,code_smells"
        qualifiers = "FIL"
        ps         = 500
        p          = $page
    }

    $parameters = ($query.GetEnumerator() | ForEach-Object {
        "$([uri]::EscapeDataString($_.Key))=$([uri]::EscapeDataString([string]$_.Value))"
    }) -join "&"

    $response = Invoke-RestMethod `
        -Uri "$base/api/measures/component_tree?$parameters" `
        -Headers $headers

    $measures += @($response.components)
    $total = [int]$response.paging.total
    $page++
} while ($measures.Count -lt $total)

$measures | ConvertTo-Json -Depth 50 |
    Set-Content "$OutputDirectory/sonarqube-measures.json"

$csv = foreach ($file in $measures) {
    $m = @{}
    foreach ($measure in $file.measures) {
        $m[$measure.metric] = $measure.value
    }

    [PSCustomObject]@{
        File = $file.path
        NLOC = $m["ncloc"]
        CyclomaticComplexity = $m["complexity"]
        CognitiveComplexity = $m["cognitive_complexity"]
        CodeSmells = $m["code_smells"]
    }
}

$csv | Export-Csv `
    "$OutputDirectory/sonarqube-metrics.csv" `
    -NoTypeInformation -Encoding UTF8

# Export all SonarQube issues.
$issues = @()
$page = 1

do {
    $response = Invoke-RestMethod `
        -Uri "$base/api/issues/search?componentKeys=$project&ps=500&p=$page" `
        -Headers $headers

    $issues += @($response.issues)
    $total = [int]$response.paging.total
    $page++
} while ($issues.Count -lt $total)

$issues | ConvertTo-Json -Depth 50 |
    Set-Content "$OutputDirectory/sonarqube-all-issues.json"

$smells = @($issues | Where-Object {
    $_.type -eq "CODE_SMELL"
})

$smells | ConvertTo-Json -Depth 50 |
    Set-Content "$OutputDirectory/sonarqube-code-smells.json"

Write-Host "Export complete."
Write-Host "Files measured: $($measures.Count)"
Write-Host "Issues exported: $($issues.Count)"
Write-Host "Code smells exported: $($smells.Count)"
