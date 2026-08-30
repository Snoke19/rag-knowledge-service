package com.example.ragknowledgeservice.exception;

import com.example.ragknowledgeservice.common.error.ErrorType;
import com.example.ragknowledgeservice.common.error.ValidationReason;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception exception, HttpServletRequest request) {
        log.error(
            "Unexpected exception. method={}, uri={}",
            request.getMethod(),
            request.getRequestURI(),
            exception
        );

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);

        problem.setType(URI.create(ErrorType.INTERNAL_ERROR.getUri()));
        problem.setTitle("Internal server error");
        problem.setDetail("An unexpected error occurred.");
        problem.setInstance(buildInstance());

        return problem;
    }

    @ExceptionHandler(StorageException.class)
    public ProblemDetail handleStorageException(StorageException exception, HttpServletRequest request) {
        log.error(
            "Handling StorageException. method={}, uri={}",
            request.getMethod(),
            request.getRequestURI(),
            exception
        );

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setType(URI.create(ErrorType.STORAGE_ERROR.getUri()));
        problem.setTitle("Storage operation failed");
        problem.setDetail("The document could not be stored.");
        problem.setInstance(buildInstance());

        return problem;
    }

    @ExceptionHandler(MultipartException.class)
    public ProblemDetail handleMultipartException(MultipartException exception, HttpServletRequest request) {
        log.error("Handling MultipartException. method={}, uri={}, message={}",
            request.getMethod(),
            request.getRequestURI(),
            exception.getMessage(),
            exception
        );

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create(ErrorType.INVALID_MULTIPART.getUri()));
        problem.setTitle("Invalid multipart request");
        problem.setDetail("The multipart request could not be processed.");
        problem.setInstance(buildInstance());

        return problem;
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ProblemDetail handleMissingServletRequestPart(MissingServletRequestPartException exception, HttpServletRequest request) {
        log.warn("Handling MissingServletRequestPartException. method={}, uri={}, part={}",
            request.getMethod(),
            request.getRequestURI(),
            exception.getRequestPartName(),
            exception
        );

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create(ErrorType.MISSING_REQUEST_PART.getUri()));
        problem.setTitle("Missing multipart part");
        problem.setDetail("Required multipart part '%s' is missing.".formatted(exception.getRequestPartName()));
        problem.setInstance(buildInstance());

        return problem;
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleValidation(HandlerMethodValidationException exception, HttpServletRequest request) {
        log.warn("Handling HandlerMethodValidationException. method={}, uri={}",
            request.getMethod(),
            request.getRequestURI(),
            exception
        );

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create(ErrorType.VALIDATION_ERROR.getUri()));
        problem.setTitle("Validation failed");
        problem.setDetail("One or more request fields are invalid.");
        problem.setInstance(buildInstance());

        List<FieldError> errors = createErrors(exception);
        problem.setProperty("errors", errors);
        return problem;
    }

    private List<FieldError> createErrors(HandlerMethodValidationException exception) {
        List<FieldError> errors = new ArrayList<>();

        for (ParameterValidationResult result : exception.getParameterValidationResults()) {
            String field = result.getMethodParameter().getParameterName();

            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                String reason = error.getDefaultMessage();
                if (reason != null) {
                    ValidationReason validationReason = resolveReason(error.getDefaultMessage());

                    errors.add(new FieldError(field, validationReason.name(), validationReason.getMessage()));
                }
            }
        }
        return errors;
    }

    private ValidationReason resolveReason(String code) {
        try {
            return ValidationReason.valueOf(code);
        } catch (IllegalArgumentException exception) {
            return ValidationReason.UNKNOWN;
        }
    }

    private URI buildInstance() {
        return URI.create("urn:problem:" + UUID.randomUUID());
    }

    public record FieldError(
        String field,
        String reason,
        String detail
    ) {
    }
}