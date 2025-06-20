#!/usr/bin/env pwsh
# build-and-push-multiarch.ps1
# Build and push multi-arch images for all microservices to ghcr.io

param(
    [Parameter(Position = 0, Mandatory = $false)]
    [string]$ServiceName
)

$ErrorActionPreference = 'Stop'

$services = @(
    @{ name = 'auth-service'; path = 'auth-service' },
    @{ name = 'eureka'; path = 'eureka' },
    @{ name = 'gateway'; path = 'gateway' },
    @{ name = 'movie-service'; path = 'movie-service' },
    @{ name = 'tmdb-service'; path = 'tmdb-service' },
    @{ name = 'notification-service'; path = 'notification-service' },
    @{ name = 'recommendation-service'; path = 'recommendation-service' }
)

# Filter services based on parameter
if ($ServiceName)
{
    $filteredServices = $services | Where-Object { $_.name -eq $ServiceName }
    if ($filteredServices.Count -eq 0)
    {
        Write-Error "Service '$ServiceName' not found. Available services: $( $services.name -join ', ' )"
        exit 1
    }
    $servicesToBuild = $filteredServices
    Write-Host "=== Building specific service: $ServiceName ==="
}
else
{
    $servicesToBuild = $services
    Write-Host "=== Building all services ==="
}

Write-Host "=== Building Maven modules ==="
if ($ServiceName)
{
    mvn clean package -pl $ServiceName -am -DskipTests
}
else
{
    mvn clean package -DskipTests
}

$ghcrUser = 'panosdim'

foreach ($svc in $servicesToBuild)
{
    $imgBase = "ghcr.io/$ghcrUser/$( $svc.name )"
    Write-Host "=== Building $( $svc.name ) for amd64 ==="
    podman build --arch amd64 -t "${imgBase}:amd64" -f "$( $svc.path )/Dockerfile" $( $svc.path )

    Write-Host "=== Building $( $svc.name ) for arm64 ==="
    podman build --arch arm64 -t "${imgBase}:arm64" -f "$( $svc.path )/Dockerfile" $( $svc.path )

    Write-Host "=== Remove existing manifest or image for $( $svc.name ) ==="
    try
    {
        podman manifest rm "${imgBase}:latest" | Out-Null
    }
    catch
    {
        # Ignore errors if manifest does not exist
    }
    try
    {
        podman rmi "${imgBase}:latest" -f | Out-Null
    }
    catch
    {
        # Ignore errors if image does not exist
    }

    Write-Host "=== Creating manifest for $( $svc.name ) ==="
    podman manifest create "${imgBase}:latest"
    podman manifest add "${imgBase}:latest" "${imgBase}:amd64"
    podman manifest add "${imgBase}:latest" "${imgBase}:arm64"

    Write-Host "=== Pushing manifest and images for $( $svc.name ) ==="
    podman manifest push --all "${imgBase}:latest" "docker://${imgBase}:latest"
}

if ($ServiceName)
{
    Write-Host "Service $ServiceName built and pushed as multi-arch image to ghcr.io/$ghcrUser/"
}
else
{
    Write-Host "All microservices built and pushed as multi-arch images to ghcr.io/$ghcrUser/"
}
