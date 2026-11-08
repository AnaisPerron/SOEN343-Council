package com.council.tenderapp.reg.exceptions;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.council.tenderapp.ord.exceptions.InvalidOrderException;
import com.council.tenderapp.dlv.exceptions.InvalidCourierStateException;
import com.council.tenderapp.ord.exceptions.InvalidOrderStateException;
import com.council.tenderapp.ord.exceptions.MenuItemUnavailableException;
import com.council.tenderapp.ord.exceptions.OrderAccessDeniedException;
import com.council.tenderapp.ord.exceptions.OrderNotFoundException;
import com.council.tenderapp.ord.exceptions.RestaurantClosedException;
import com.council.tenderapp.ord.exceptions.RestaurantNotFoundException;
import com.council.tenderapp.ord.exceptions.UnauthorizedMenuAccessException;
import com.council.tenderapp.reg.dtos.ErrorResponse;
import com.council.tenderapp.technicalservices.payment.PaymentDeclinedException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        log.warn("Validation failed "+fieldErrors);

        return build(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request body or invalid role", Map.of());
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    @ExceptionHandler({InvalidCredentialsException.class, MissingSessionTokenException.class})
    public ResponseEntity<ErrorResponse> handleUnauthorized(RuntimeException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), Map.of());
    }


    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, Map<String, String> fieldErrors) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.value(), status.getReasonPhrase(), message, fieldErrors));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        String roles = "ANONYMOUS";
        if (auth != null && auth.isAuthenticated()) {
            roles = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(authority -> authority.startsWith("ROLE_") ? authority.substring(5) : authority)
                    .collect(Collectors.joining(", "));
        }

        String requestUri = request.getRequestURI();

        String customMessage = String.format("role:%s does not have permission for %s", roles, requestUri);

        log.warn("Security Alert: {}", customMessage);

        Map<String, Object> errorBody = Map.of(
            "timestamp", Instant.now().toString(),
            "status", HttpStatus.FORBIDDEN.value(),
            "error", "Forbidden",
            "message", customMessage,
            "path", requestUri
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody);
    }

    @ExceptionHandler (RestaurantNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRestaurantNotFound(RestaurantNotFoundException ex) {
       
        log.warn("Restaurant not found: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler (RestaurantClosedException.class)
    public ResponseEntity<ErrorResponse> handleRestaurantClosed(RestaurantClosedException ex) {
        
        log.warn("Access denied - Restaurant closed: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.FORBIDDEN.value(),
            "Forbidden",
            ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(UnauthorizedMenuAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedMenuAccess(UnauthorizedMenuAccessException ex)
    {
        log.warn("Access denied - Restaurant does not have permission over this menu: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.FORBIDDEN.value(),
            "Forbidden",
            ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrder(InvalidOrderException ex) {
        log.warn("Invalid order: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(MenuItemUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleMenuItemUnavailable(MenuItemUnavailableException ex) {
        log.warn("Menu item unavailable: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        log.warn("Order not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(OrderAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleOrderAccessDenied(OrderAccessDeniedException ex) {
        log.warn("Order access denied: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrderState(InvalidOrderStateException ex) {
        log.warn("Invalid order state: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    // Without this, Spring drops the reason and the app only sees "Request failed (400)"
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() == null ? status.getReasonPhrase() : ex.getReason();
        if (status.is5xxServerError()) log.error("Request failed: {}", message);
        else log.warn("Request refused: {}", message);
        return build(status, message, Map.of());
    }

    @ExceptionHandler(InvalidCourierStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCourierState(InvalidCourierStateException ex) {
        log.warn("Invalid courier state: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(PaymentDeclinedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentDeclined(PaymentDeclinedException ex) {
        log.warn("Payment declined: {}", ex.getMessage());
        return build(HttpStatus.PAYMENT_REQUIRED, ex.getMessage(), Map.of());
    }
}
