#!/bin/bash
# filepath: /home/padi/projects/movies-track-app/build-and-push-multiarch.sh
# build-and-push-multiarch.sh
# Build and push multi-arch images for all microservices to ghcr.io

set -e

SERVICE_NAME="${1:-}"

declare -a services=(
    "auth-service:auth-service"
    "eureka:eureka"
    "gateway:gateway"
    "movie-service:movie-service"
    "tmdb-service:tmdb-service"
    "notification-service:notification-service"
    "recommendation-service:recommendation-service"
)

# Define Java services that need Maven build
declare -a java_services=(
    "auth-service"
    "eureka"
    "gateway"
    "movie-service"
    "tmdb-service"
    "notification-service"
)

# Function to check if a service is a Java service
is_java_service() {
    local service_name="$1"
    for java_svc in "${java_services[@]}"; do
        if [[ "$java_svc" == "$service_name" ]]; then
            return 0
        fi
    done
    return 1
}

# Filter services based on parameter
if [[ -n "$SERVICE_NAME" ]]; then
    found=false
    for service in "${services[@]}"; do
        name="${service%%:*}"
        if [[ "$name" == "$SERVICE_NAME" ]]; then
            services_to_build=("$service")
            found=true
            break
        fi
    done

    if [[ "$found" == false ]]; then
        echo "Error: Service '$SERVICE_NAME' not found. Available services:"
        for service in "${services[@]}"; do
            echo "  ${service%%:*}"
        done
        exit 1
    fi
    services_to_build=("$service")
    echo "=== Building specific service: $SERVICE_NAME ==="
else
    services_to_build=("${services[@]}")
    echo "=== Building all services ==="
fi

echo "=== Building Maven modules ==="
if [[ -n "$SERVICE_NAME" ]]; then
    if is_java_service "$SERVICE_NAME"; then
        echo "Building Java service: $SERVICE_NAME"
        mvn clean package -pl "$SERVICE_NAME" -am -DskipTests
    else
        echo "Skipping Maven build for non-Java service: $SERVICE_NAME"
    fi
else
    echo "Building all Java services"
    mvn clean package -DskipTests
fi

ghcr_user='panosdim'

# Create a buildx builder if it doesn't exist
docker buildx create --name multiarch-builder --use 2>/dev/null || docker buildx use multiarch-builder

for service in "${services_to_build[@]}"; do
    name="${service%%:*}"
    path="${service##*:}"
    img_base="ghcr.io/$ghcr_user/$name"

    echo "=== Building and pushing multi-arch image for $name ==="
    docker buildx build \
        --platform linux/amd64,linux/arm64 \
        --builder multiarch-builder \
        -t "${img_base}:latest" \
        -f "$path/Dockerfile" \
        --push \
        --provenance=false \
        --sbom=false \
        "$path"
done

if [[ -n "$SERVICE_NAME" ]]; then
    echo "Service $SERVICE_NAME built and pushed as multi-arch image to ghcr.io/$ghcr_user/"
else
    echo "All microservices built and pushed as multi-arch images to ghcr.io/$ghcr_user/"
fi
