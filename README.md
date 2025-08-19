# Movies Track App

This project is a comprehensive, microservices-based application designed for tracking movies and receiving personalized recommendations. It leverages a variety of technologies, including Java with Spring Boot for backend services and Python for the machine learning-based recommendation engine. The entire application is containerized using Docker for easy deployment and scalability.

## Architecture

The application follows a standard microservices architecture pattern, including:

-   **API Gateway**: A single entry point for all client requests, routing traffic to the appropriate downstream service.
-   **Service Discovery**: A Eureka server that allows services to register and discover each other dynamically.
-   **Apache Kafka**: Used for message communication between the microservices.
-   **Containerization**: All services are containerized with Docker and orchestrated using `docker-compose.yml`.
-   **Monitoring & Logging**: The project is set up with Zipkin for distributed tracing, OpenSearch for indexing and Fluent Bit for log aggregation.

## Microservices

The project is composed of the following microservices:

| Service                    | Technology   | Description                                                                                                          |
| -------------------------- | ------------ | -------------------------------------------------------------------------------------------------------------------- |
| **Gateway**                | Java/Spring  | The API Gateway that acts as the single entry point for all incoming client traffic, handling routing and filtering. |
| **Eureka**                 | Java/Spring  | The service discovery server where all other microservices register themselves.                                      |
| **Auth Service**           | Java/Spring  | Manages user authentication, registration, and issues JWT tokens for securing the application.                       |
| **Movie Service**          | Java/Spring  | Handles core business logic related to movies, such as user watchlists, ratings, and movie details.                  |
| **TMDB Service**           | Java/Spring  | Acts as a proxy to the external The Movie Database (TMDB) API, fetching movie data to populate the system.           |
| **Notification Service**   | Java/Spring  | Responsible for sending notifications to users (e.g., email, push notifications).                                    |
| **Recommendation Service** | Python/Keras | A Python-based service that uses a trained Keras model to generate personalized movie recommendations for users.     |
| **Common**                 | Java/Maven   | A shared library containing common DTOs, utility classes, and configurations used across the Java-based services.    |

## Technologies Used

-   **Backend**: Java 17+, Spring Boot, Spring Cloud
-   **Recommendation Engine**: Python, Keras (TensorFlow backend)
-   **Database**: Multiple SQL databases (initialized via `init-multi-db.sh`)
-   **Build Tool**: Maven
-   **Containerization**: Docker, Docker Compose
-   **Monitoring & Logging**: Zipkin, OpenSearch, Fluent Bit

## How to Run

1.  **Build and Run the Application**:
    The entire application stack can be built and started using Docker Compose.

    ```bash
    docker-compose up --build -d
    ```

2.  **Build and Push Multi-Arch Images (Optional)**:
    A helper script is provided to build and push multi-architecture Docker images to a registry.
    ```bash
    sh ./build-and-push-multiarch.sh
    ```
