# RAG Knowledge Service

A Java/Spring Boot backend service for building a Retrieval-Augmented Generation (RAG) knowledge system.

The project is being developed incrementally, with each phase introducing a new responsibility of the final RAG system.

## Tech Stack

* Java 21
* Spring Boot
* Spring Web MVC
* Spring Validation
* Spring Actuator
* Maven
* JUnit / Spring Boot Test

## Project Structure

The application is organized around clear responsibilities:

```text
src/main/java/com/example/ragknowledgeservice/
├── api/        # HTTP controllers and API-level concerns
├── common/     # Shared components
├── dto/        # Request/response and application boundary objects
├── handler/    # Global HTTP exception handling
└── service/    # Application/service logic
```

The structure is intentionally kept small at the beginning. New boundaries will be introduced as the corresponding capabilities are implemented.

## Running the Application

### Prerequisites

* Java 21+
* Maven 3.9+ or use the included Maven Wrapper

### Start the application

Using Maven Wrapper:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Or build and run the generated JAR:

```bash
./mvnw clean package
java -jar target/*.jar
```

## Running Tests

Run the complete test suite:

```bash
./mvnw test
```

On Windows:

```powershell
.\mvnw.cmd test
```

## Health Check

The application exposes a Spring Boot Actuator health endpoint:

```http
GET /actuator/health
```

When the application is running, it can be used to verify that the service is healthy and available.

## API

The service is being developed around the following API:

| Method | Endpoint                     | Purpose                       |
| ------ | ---------------------------- | ----------------------------- |
| GET    | `/actuator/health`           | Application health            |
| POST   | `/api/documents`             | Upload a documentMetadata             |
| GET    | `/api/documents/{id}`        | Retrieve documentMetadata information |
| POST   | `/api/documents/{id}/ingest` | Start documentMetadata ingestion      |
| POST   | `/api/search`                | Search the knowledge base     |
| POST   | `/api/chat`                  | Ask a question using RAG      |

Some endpoints are introduced incrementally and their implementation is completed in the corresponding development phases.

## Development Approach

The project follows an incremental implementation approach.

The system is not built by introducing the entire RAG stack at once. Instead, each phase adds a concrete capability:

```text
Project setup
     ↓
Document management
     ↓
Persistence
     ↓
Document ingestion
     ↓
Text extraction
     ↓
Chunking
     ↓
Embeddings
     ↓
Vector search
     ↓
RAG generation
     ↓
Production hardening
```

This approach keeps each architectural decision connected to an actual requirement and avoids introducing infrastructure before it is needed.

## RAG Goal

The eventual goal is to build a production-oriented RAG backend capable of:

1. Accepting knowledge documents.
2. Processing their content.
3. Splitting content into retrieval-friendly chunks.
4. Generating embeddings.
5. Storing and retrieving relevant knowledge.
6. Supplying retrieved context to an LLM.
7. Returning answers grounded in the uploaded knowledge.

PDF is the first documentMetadata format targeted by the project.

## Configuration

Application configuration is located in:

```text
src/main/resources/application.yaml
```

Environment-specific configuration should be kept outside the source code whenever possible.

Secrets and credentials should not be committed to the repository.

## Project Status

The project is under active development and follows a phased learning and implementation roadmap.

The current codebase represents the early foundation of the service. RAG-specific capabilities are introduced progressively rather than being configured upfront.

## License

This project is currently intended as a personal learning and engineering project.
