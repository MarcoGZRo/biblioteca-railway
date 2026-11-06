package com.donquijote.biblioteca;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.*;
@RestControllerAdvice public class ApiErrors {
@ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> known(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"Solicitud inválida":e.getReason()));}
@ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> invalid(MethodArgumentNotValidException e){return ResponseEntity.badRequest().body(Map.of("message",e.getBindingResult().getFieldErrors().stream().findFirst().map(x->x.getField()+": "+x.getDefaultMessage()).orElse("Datos inválidos")));}
@ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> conflict(DataIntegrityViolationException e){return ResponseEntity.status(409).body(Map.of("message","Conflicto de datos; comprueba que el código y DNI sean únicos"));}
}
