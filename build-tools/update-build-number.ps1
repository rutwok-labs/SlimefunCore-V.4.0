param(
    [Parameter(Mandatory = $true)]
    [string]$PropertiesPath,

    [Parameter(Mandatory = $true)]
    [string]$BuildSession
)

$ErrorActionPreference = 'Stop'

$propertiesDirectory = Split-Path -Parent $PropertiesPath
if (-not [string]::IsNullOrWhiteSpace($propertiesDirectory)) {
    New-Item -ItemType Directory -Force -Path $propertiesDirectory | Out-Null
}

$currentYear = (Get-Date).ToString('yy')
$currentCount = 0

if (Test-Path -LiteralPath $PropertiesPath) {
    $existing = @{}

    foreach ($line in Get-Content -LiteralPath $PropertiesPath) {
        if ($line -match '^\s*#' -or $line -notmatch '=') {
            continue
        }

        $key, $value = $line -split '=', 2
        $existing[$key.Trim()] = $value.Trim()
    }

    if ($existing['build.year'] -eq $currentYear -and $existing['build.session'] -eq $BuildSession) {
        return
    }

    if ($existing['build.year'] -eq $currentYear) {
        $parsedCount = 0
        if ([int]::TryParse($existing['build.count'], [ref]$parsedCount)) {
            $currentCount = $parsedCount
        }
    }
}

$nextCount = $currentCount + 1

if ($nextCount -gt 99) {
    throw "The 4-digit YYNN build number format only supports up to 99 builds per year. Reset or expand the format before building again."
}

$buildCount = '{0:D2}' -f $nextCount
$shortBuildNumber = "$currentYear$buildCount"

@(
    "# Auto-generated build number for SlimefunCore."
    "build.year=$currentYear"
    "build.count=$buildCount"
    "short.build.number=$shortBuildNumber"
    "generated.short.build.number=$shortBuildNumber"
    "build.session=$BuildSession"
) | Set-Content -LiteralPath $PropertiesPath -Encoding ASCII
