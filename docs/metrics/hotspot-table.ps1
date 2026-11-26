<#
.SYNOPSIS
    Prints the Phase IV hotspot comparison table from the committed SonarQube exports.

.DESCRIPTION
    Reads docs/metrics/<snapshot>/sonarqube-metrics.csv for every snapshot given, in order,
    and prints one Markdown table. Each cell is "NLOC / cyclomatic / cognitive / code smells"
    for that class at that snapshot, or "-" when the class does not exist there.

    Rows: the five Step 1 hotspots, the classes the refactorings added or changed, the classes
    of the RVW module, and the existing classes RVW had to modify. RVW classes are found
    automatically (every production file under rvw/).

    Class coupling is counted by hand (see the report); SonarQube does not export it.

.EXAMPLE
    .\docs\metrics\hotspot-table.ps1 -Snapshots phase3,phase4-refactored
    .\docs\metrics\hotspot-table.ps1 -Snapshots phase3,phase4-refactored,phase4 -OutFile docs/metrics/phase4/hotspot-table.md
#>
param(
    [Parameter(Mandatory)]
    [string[]]$Snapshots,

    # Optional: also write the table to this file (UTF-8).
    [string]$OutFile
)

$ErrorActionPreference = "Stop"
$metricsDir = $PSScriptRoot
$src = "tender-app/backend/src/main/java/com/council/tenderapp"

$groups = [ordered]@{
    "Step 1 hotspots" = @(
        "ord/services/OrderService.java",
        "ord/services/CustomerOrderService.java",
        "ord/entities/Order.java",
        "dlv/services/AssignmentService.java",
        "ord/services/CustomerOrderHistoryService.java"
    )
    "Added by the refactorings (R1-R4)" = @(
        "ord/services/OrderPreparation.java",
        "ord/services/OrderResponseAssembler.java",
        "dlv/services/AssignmentAdoption.java",
        "ord/services/OrderAccessPolicy.java"
    )
    "Changed by R4 (ownership checks)" = @(
        "ord/services/OrderCancellationService.java",
        "prc/service/OrderBelongingService.java"
    )
    "RVW module (added)" = @()   # filled from the exports below
    "Existing classes modified by RVW" = @(
        "ord/services/BrowseService.java",
        "ord/dtos/RestaurantResponseDTO.java",
        "reg/exceptions/GlobalExceptionHandler.java"
    )
}

# Load each snapshot: path -> row
$data = [ordered]@{}
foreach ($snapshot in $Snapshots) {
    $csv = Join-Path $metricsDir "$snapshot/sonarqube-metrics.csv"
    if (-not (Test-Path -LiteralPath $csv)) { throw "Missing export: $csv" }
    $rows = @{}
    foreach ($row in Import-Csv -LiteralPath $csv) { $rows[$row.File] = $row }
    $data[$snapshot] = $rows
}

# RVW production classes present in any snapshot
$rvw = foreach ($rows in $data.Values) {
    $rows.Keys | Where-Object { $_ -like "$src/rvw/*.java" } |
        ForEach-Object { $_.Substring($src.Length + 1) }
}
$groups["RVW module (added)"] = @($rvw | Sort-Object -Unique)

function Format-Cell($row) {
    if ($null -eq $row) { return "-" }
    return "$($row.NLOC) / $($row.CyclomaticComplexity) / $($row.CognitiveComplexity) / $($row.CodeSmells)"
}

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("Cells: NLOC / cyclomatic complexity / cognitive complexity / SonarQube code smells. Source: ``docs/metrics/<snapshot>/sonarqube-metrics.csv``.")
$lines.Add("")
$lines.Add("| Class | " + ($Snapshots -join " | ") + " |")
$lines.Add("| --- |" + (" --- |" * $Snapshots.Count))
foreach ($group in $groups.Keys) {
    if ($groups[$group].Count -eq 0) { continue }
    $lines.Add("| **$group** |" + (" |" * $Snapshots.Count))
    foreach ($relative in $groups[$group]) {
        $path = "$src/$relative"
        $name = [System.IO.Path]::GetFileNameWithoutExtension($relative)
        $cells = foreach ($snapshot in $Snapshots) { Format-Cell $data[$snapshot][$path] }
        $lines.Add("| ``$name`` | " + ($cells -join " | ") + " |")
    }
}

$lines | ForEach-Object { Write-Output $_ }
if ($OutFile) {
    [System.IO.File]::WriteAllLines(
        [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $OutFile)), $lines,
        (New-Object System.Text.UTF8Encoding($false)))
}
