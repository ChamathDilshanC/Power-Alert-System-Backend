package lk.ijse.poweralert.advice;

import lk.ijse.poweralert.dto.ResponseDTO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handle bean validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDTO> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        ResponseDTO responseDTO = new ResponseDTO(400, "Validation Error", errors);
        return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
    }

    // Handle entity not found exceptions
    @ExceptionHandler({EntityNotFoundException.class, NoSuchElementException.class})
    public ResponseEntity<ResponseDTO> handleEntityNotFoundException(Exception ex) {
        ResponseDTO responseDTO = new ResponseDTO(404, ex.getMessage(), null);
        return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
    }

    // Handle authentication exceptions
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ResponseDTO> handleAuthenticationException(Exception ex) {
        ResponseDTO responseDTO = new ResponseDTO(401, "Authentication failed: " + ex.getMessage(), null);
        return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
    }

    // Handle access denied exceptions
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseDTO> handleAccessDeniedException(AccessDeniedException ex) {
        ResponseDTO responseDTO = new ResponseDTO(403, "Access denied: " + ex.getMessage(), null);
        return new ResponseEntity<>(responseDTO, HttpStatus.FORBIDDEN);
    }

    // Handle constraint violations (database level)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResponseDTO> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                errors.put(violation.getPropertyPath().toString(), violation.getMessage())
        );

        ResponseDTO responseDTO = new ResponseDTO(400, "Constraint Violation", errors);
        return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
    }

    // Handle data integrity violations (unique constraints, etc.)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ResponseDTO> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        ResponseDTO responseDTO = new ResponseDTO(409, "Data integrity violation: " + message, null);
        return new ResponseEntity<>(responseDTO, HttpStatus.CONFLICT);
    }

    // Handle missing request parameters
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ResponseDTO> handleMissingParams(MissingServletRequestParameterException ex) {
        ResponseDTO responseDTO = new ResponseDTO(400, "Missing parameter: " + ex.getParameterName(), null);
        return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
    }

    // Handle type mismatch exceptions (e.g., trying to convert string to number)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ResponseDTO> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("The parameter '%s' should be of type '%s'",
                ex.getName(), ex.getRequiredType().getSimpleName());
        ResponseDTO responseDTO = new ResponseDTO(400, message, null);
        return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
    }

    // Fallback for all other exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDTO> handleGenericException(Exception ex) {
        ResponseDTO responseDTO = new ResponseDTO(500, "An unexpected error occurred: " + ex.getMessage(), null);
        return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}