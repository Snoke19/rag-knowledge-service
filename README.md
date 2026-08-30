# RAG Knowledge Service

Java 21 / Spring Boot backend for building a document-based Retrieval-Augmented Generation (RAG) service.

The current implementation is focused on the **document management foundation**: accepting PDF uploads, validating them,
storing the binary content in MinIO, persisting document metadata in PostgreSQL, calculating a SHA-256 content hash,
retrieving metadata, and parsing stored PDFs into page-level text.

RAG search, embeddings, vector storage, and LLM-based chat are not implemented yet.

## Current capabilities

* PDF document upload through `multipart/form-data`
* Upload validation:

    * required file
    * non-empty file
    * `application/pdf` content type
    * PDF `%PDF-` file signature
    * maximum request/file size of 10 MB
* Safe extraction of the uploaded filename
* SHA-256 hashing of document content
* PostgreSQL document metadata persistence
* MinIO object storage for document binaries
* Compensation-based cleanup when storage and database operations do not complete together
* Document metadata retrieval
* PDF text extraction with Apache PDFBox, page by page
* Consistent HTTP error responses using Spring `ProblemDetail`
* Actuator health/info endpoints
* Test infrastructure for PostgreSQL and MinIO with Testcontainers

## Architecture

The current upload flow is:

```text
HTTP multipart upload
        |
        v
DocumentController
        |
        v
PDF validation + MultipartFile mapping
        |
        v
DocumentService
        |
        +--------------------+
        |                    |
        v                    v
    MinIO               PostgreSQL
 document binary       document metadata
        |                    |
        +---------+----------+
                  |
                  v
             SavedFile
```

Document parsing is currently separated behind a parser abstraction:

```text
DocumentService
      |
      v
stored PDF bytes
      |
      v
PdfDocumentParserImpl
      |
      v
PDFBox
      |
      v
List<DocumentPage>
```

## API

### Upload document

```http
POST /api/documents
Content-Type: multipart/form-data
```

Request part:

```text
file=<PDF file>
```

Successful response:

```http
HTTP/1.1 201 Created
Location: /api/documents/{documentId}
```

The response body contains the generated document ID and its initial status.

### Get document metadata

```http
GET /api/documents/{documentId}
```

Returns metadata including:

* document ID
* original title
* content type
* size
* document status
* SHA-256 content hash

### Health

```http
GET /actuator/health
```

### Current placeholder endpoints

The following endpoints exist in the current controller layer but are **not implemented as functional RAG features
yet**:

```http
POST /api/documents/{id}/ingest
POST /api/search
POST /api/chat
```

They currently return placeholder responses and should not be treated as completed ingestion, retrieval, or chat
functionality.

## Document lifecycle

A newly uploaded document is persisted with the following initial status:

```text
UPLOADED
```

The current implementation does not yet provide the complete ingestion lifecycle (chunking, embeddings, indexing, etc.).

## Storage model

### PostgreSQL

Document metadata is stored in the `documents` table.

Current fields include:

| Field            | Purpose                              |
|------------------|--------------------------------------|
| `id`             | Internal database identity           |
| `document_id`    | Public UUID identifier               |
| `title`          | Original document title              |
| `content_type`   | Uploaded MIME type                   |
| `size`           | Stored document size                 |
| `storage_key`    | MinIO object key                     |
| `status`         | Current document status              |
| `content_sha256` | SHA-256 hash of the document content |

The initial SQL definition is available in `script_sql/table_documents.sql`.

### MinIO

Document binaries are stored in the `rag-documents` bucket.

Objects use the following key format:

```text
documents/{documentId}/source.pdf
```

MinIO is accessed through the `FileStorage` abstraction, with `MinioFileStorage` providing the current implementation.

The upload operation uses a compensation mechanism: if the database operation fails after the object has been uploaded,
the stored object is deleted to avoid leaving an orphaned file.

## PDF processing

PDF validation is intentionally performed before the document enters the application service.

The validator checks:

1. The multipart file exists.
2. The file is not empty.
3. The declared content type is `application/pdf`.
4. The file starts with the PDF signature `%PDF-`.

After storage, `PdfDocumentParserImpl` loads the document with Apache PDFBox and extracts text separately for each page.
Each parsed page is represented by `DocumentPage` and retains the document ID, page number, source filename, and
extracted text.

Detailed parser limitations are documented in:

```text
doc/parsing/pdf-parser-limitations.md
```

## Error handling

The application uses a global `@RestControllerAdvice` and Spring `ProblemDetail` responses.

The current exception handling covers, among others:

* missing multipart parts → `400 Bad Request`
* malformed multipart requests → `400 Bad Request`
* PDF validation failures → `400 Bad Request`
* files larger than 10 MB → `400 Bad Request`
* missing documents → `404 Not Found`
* storage failures → `500 Internal Server Error`
* unexpected failures → `500 Internal Server Error`

Validation failures expose structured error information containing the affected field, validation reason, and
human-readable detail.

## Project structure

```text
src/main/java/com/example/ragknowledgeservice/
├── api/
│   ├── ChatController.java
│   └── DocumentController.java
├── command/
│   └── UploadDocumentCommand.java
├── common/
│   ├── DocumentStatus.java
│   ├── MultipartFileMapper.java
│   ├── error/
│   ├── hasher/
│   └── validation/
├── config/
│   ├── MinioConfiguration.java
│   └── MinioStorageProperties.java
├── dto/
│   ├── DocumentMetadataResponse.java
│   └── SavedFile.java
├── entities/
│   └── DocumentMetadata.java
├── exception/
│   ├── DocumentNotFoundException.java
│   ├── DocumentParsingException.java
│   ├── GlobalExceptionHandler.java
│   └── StorageException.java
├── repositories/
│   └── DocumentRepository.java
└── service/
    ├── DocumentService.java
    ├── parsing/
    │   ├── DocumentPage.java
    │   ├── DocumentParser.java
    │   └── PdfDocumentParserImpl.java
    └── storage/
        ├── CompensationAction.java
        ├── CompensationContext.java
        ├── StorageOperation.java
        ├── StorageTransactionManager.java
        └── file_storage/
            ├── FileStorage.java
            └── MinioFileStorage.java
```

## Technology stack

| Technology               | Usage                                                |
|--------------------------|------------------------------------------------------|
| Java 21                  | Application runtime                                  |
| Spring Boot 4.1.1        | Application framework                                |
| Spring Web MVC           | REST API                                             |
| Spring Validation        | Multipart/PDF validation                             |
| Spring Data JPA          | PostgreSQL persistence                               |
| Spring Actuator          | Health and application information                   |
| PostgreSQL 16            | Document metadata                                    |
| MinIO                    | Document object storage                              |
| Apache PDFBox 3.0.8      | PDF parsing and text extraction                      |
| Maven                    | Build and dependency management                      |
| JUnit / Spring Boot Test | Automated testing                                    |
| Testcontainers           | PostgreSQL and MinIO integration-test infrastructure |
| Lombok                   | Boilerplate reduction                                |

## Local development

### Prerequisites

* Java 21+
* Docker / Docker Compose
* Maven 3.9+ or the included Maven Wrapper

### 1. Configure environment variables

Copy the example environment file:

```bash
cp .env.example .env
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Configure the MinIO credentials in `.env`.

Do not commit `.env` or real credentials to the repository.

### 2. Start infrastructure

The repository provides Docker Compose configuration for PostgreSQL and MinIO:

```bash
docker compose up -d
```

The compose setup also creates the `rag-documents` MinIO bucket automatically.

Default local endpoints configured by the application are:

```text
PostgreSQL: localhost:5432
MinIO API:  http://localhost:9000
MinIO UI:   http://localhost:9001
```

### 3. Prepare the database

The current schema definition is:

```text
script_sql/table_documents.sql
```

The application expects the PostgreSQL database configured in `application.yaml`:

```text
Database: rag_knowledge
User:     rag
```

### 4. Start the application

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Alternatively:

```bash
./mvnw clean package
java -jar target/rag-knowledge-service-0.0.1-SNAPSHOT.jar
```

## Running tests

Run the complete test suite with:

```bash
./mvnw test
```

Windows:

```powershell
.\mvnw.cmd test
```

The project includes Testcontainers dependencies for PostgreSQL and MinIO, allowing integration tests to run against
containerized infrastructure.

## Configuration

Application configuration is located at:

```text
src/main/resources/application.yaml
```

Current defaults include:

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

MinIO credentials are supplied through environment variables:

```text
MINIO_ROOT_USER
MINIO_ROOT_PASSWORD
```

Actuator currently exposes:

```text
/actuator/health
/actuator/info
```

## Current implementation boundary

The repository currently covers the foundation required before implementing the retrieval part of RAG:

```text
                    CURRENT
                       |
                       v
HTTP upload → validation → metadata → object storage
                       |
                       v
                  PDF parsing
                       |
                       v
                 page-level text

                    NEXT
                       |
                       v
                   chunking
                       ↓
                  embeddings
                       ↓
                 vector store
                       ↓
                    search
                       ↓
                LLM generation
                       ↓
                     RAG
```

Not yet implemented as functional capabilities:

* document ingestion orchestration
* chunking
* embeddings
* vector database / vector search
* retrieval pipeline
* LLM integration
* grounded chat responses
* production authentication/authorization
* asynchronous ingestion processing

## Development philosophy

The project is intentionally implemented incrementally. Infrastructure and abstractions are introduced when they support
a concrete application requirement rather than being added as a complete RAG stack up front.

The current codebase therefore represents a working **document management and PDF processing foundation**, not a
finished RAG application.