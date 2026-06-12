package com.auradev.erp.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralised exception-to-HTTP-response mapping following RFC 7807.
 *
 * <p>All responses use {@code Content-Type: application/problem+json}.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final MediaType PROBLEM_JSON =
            MediaType.valueOf("application/problem+json");

    // -------------------------------------------------------------------------
    // Validation — HTTP 422
    // -------------------------------------------------------------------------

    /**
     * Bean-validation failures from {@code @Valid} / {@code @Validated} on
     * request bodies or request parameters.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<ApiError.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ApiError.FieldError(
                        fe.getField(),
                        fe.getDefaultMessage()))
                .collect(Collectors.toList());

        ApiError body = ApiError.builder()
                .type("https://erp.auradev.com/problems/validation-error")
                .title("Validation Failed")
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .detail("One or more fields failed validation")
                .code("VALIDATION_ERROR")
                .instance(request.getRequestURI())
                .timestamp(Instant.now())
                .errors(fieldErrors)
                .build();

        return problem(HttpStatus.UNPROCESSABLE_ENTITY, body);
    }

    /**
     * Constraint-violation failures from {@code @Validated} on service/
     * repository method arguments.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        List<ApiError.FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(cv -> new ApiError.FieldError(
                        leafPath(cv),
                        cv.getMessage()))
                .collect(Collectors.toList());

        ApiError body = ApiError.builder()
                .type("https://erp.auradev.com/problems/validation-error")
                .title("Constraint Violation")
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .detail("One or more constraints were violated")
                .code("VALIDATION_ERROR")
                .instance(request.getRequestURI())
                .timestamp(Instant.now())
                .errors(fieldErrors)
                .build();

        return problem(HttpStatus.UNPROCESSABLE_ENTITY, body);
    }

    /**
     * Domain-level business-rule violations (e.g. oversell, invalid state
     * transitions).
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request) {

        ApiError body = ApiError.builder()
                .type("https://erp.auradev.com/problems/business-rule-violation")
                .title("Business Rule Violation")
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .detail(ex.getMessage())
                .code(ex.getCode())
                .instance(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return problem(HttpStatus.UNPROCESSABLE_ENTITY, body);
    }

    // -------------------------------------------------------------------------
    // Not Found — HTTP 404
    // -------------------------------------------------------------------------

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request) {

        ApiError body = ApiError.builder()
                .type("https://erp.auradev.com/problems/not-found")
                .title("Not Found")
                .status(HttpStatus.NOT_FOUND.value())
                .detail("API endpoint not found — restart the backend if you recently added new routes")
                .code("NOT_FOUND")
                .instance(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return problem(HttpStatus.NOT_FOUND, body);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleEntityNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request) {

        ApiError body = ApiError.builder()
                .type("https://erp.auradev.com/problems/not-found")
                .title("Not Found")
                .status(HttpStatus.NOT_FOUND.value())
                .detail(ex.getMessage())
                .code("NOT_FOUND")
                .instance(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return problem(HttpStatus.NOT_FOUND, body);
    }

    // -------------------------------------------------------------------------
    // Forbidden — HTTP 403
    // -------------------------------------------------------------------------

    @ExceptionHandler(TenantAccessException.class)
    public ResponseEntity<ApiError> handleTenantAccess(
            TenantAccessException ex,
            HttpServletRequest request) {

        ApiError body = ApiError.builder()
                .type("https://erp.auradev.com/problems/tenant-access-denied")
                .title("Tenant Access Denied")
                .status(HttpStatus.FORBIDDEN.value())
                .detail(ex.getMessage())
                .code("TENANT_ACCESS_DENIED")
                .instance(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return problem(HttpStatus.FORBIDDEN, body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        ApiError body = ApiError.builder()
                .type("https://erp.auradev.com/problems/access-denied")
                .title("Access Denied")
                .status(HttpStatus.FORBIDDEN.value())
                .detail("You do not have permission to perform this action")
                .code("ACCESS_DENIED")
                .instance(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return problem(HttpStatus.FORBIDDEN, body);
    }

    // -------------------------------------------------------------------------
    // Unauthorised — HTTP 401
    // -------------------------------------------------------------------------

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(
            ResponseStatusException ex,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String code = status == HttpStatus.UNAUTHORIZED ? "UNAUTHORIZED" : status.name();

        ApiError body = ApiError.builder()
                .type("https://erp.auradev.com/problems/" + code.toLowerCase())
                .title(status.getReasonPhrase())
                .status(status.value())
                .detail(ex.getReason() != null ? ex.getReason() : status.getReasonPhrase())
                .code(code)
                .instance(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return problem(status, body);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request) {

        ApiError body = ApiError.builder()
                .type("https://erp.auradev.com/problems/unauthorized")
                .title("Unauthorized")
                .status(HttpStatus.UNAUTHORIZED.value())
                .detail("Authentication is required to access this resource")
                .code("UNAUTHORIZED")
                .instance(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return problem(HttpStatus.UNAUTHORIZED, body);
    }

    // -------------------------------------------------------------------------
    // Conflict — HTTP 409
    // -------------------------------------------------------------------------

    /**
     * Database-level unique-constraint / foreign-key violations.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.warn("Data integrity violation at {}: {}", request.getRequestURI(),
                ex.getMostSpecificCause().getMessage());

        ApiError body = ApiError.builder()
                .type("https://erp.auradev.com/problems/conflict")
                .title("Conflict")
                .status(HttpStatus.CONFLICT.value())
                .detail("The request conflicts with the current state of the resource")
                .code("CONFLICT")
                .instance(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return problem(HttpStatus.CONFLICT, body);
    }

    /**
     * Optimistic-locking failure: a concurrent update modified the record
     * between read and write.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLocking(
            ObjectOptimisticLockingFailureException ex,
            HttpServletRequest request) {

        log.warn("Optimistic locking failure at {}: {}", request.getRequestURI(),
                ex.getMessage());

        ApiError body = ApiError.builder()
                .type("https://erp.auradev.com/problems/version-conflict")
                .title("Version Conflict")
                .status(HttpStatus.CONFLICT.value())
                .detail("The resource was modified by another request; please retry with the latest version")
                .code("VERSION_CONFLICT")
                .instance(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return problem(HttpStatus.CONFLICT, body);
    }

    // -------------------------------------------------------------------------
    // Catch-all — HTTP 500
    // -------------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAll(
            Exception ex,
            HttpServletRequest request) {

        // Log the full stack trace for internal diagnostics but never leak it
        // to the caller.
        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ApiError body = ApiError.builder()
                .type("https://erp.auradev.com/problems/internal-server-error")
                .title("Internal Server Error")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail("An unexpected error occurred; please contact support")
                .code("INTERNAL_ERROR")
                .instance(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return problem(HttpStatus.INTERNAL_SERVER_ERROR, body);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ResponseEntity<ApiError> problem(HttpStatus status, ApiError body) {
        return ResponseEntity
                .status(status)
                .contentType(PROBLEM_JSON)
                .body(body);
    }

    /** Extract the leaf property name from a {@link ConstraintViolation} path. */
    private String leafPath(ConstraintViolation<?> cv) {
        String path = cv.getPropertyPath().toString();
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot + 1) : path;
    }
}
