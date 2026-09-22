package cl.iplacex.tareas.controller;

import cl.iplacex.tareas.model.Tarea;
import cl.iplacex.tareas.service.TareaNoEncontradaException;
import cl.iplacex.tareas.service.TareaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * API REST del gestor de tareas.
 *
 *  GET    /api/tareas                 lista todas las tareas
 *  GET    /api/tareas/resumen         estadísticas (pendientes y % de avance)
 *  GET    /api/tareas/{id}            obtiene una tarea
 *  POST   /api/tareas                 crea una tarea
 *  PUT    /api/tareas/{id}/completar  marca como completada
 *  DELETE /api/tareas/{id}            elimina una tarea
 */
@RestController
@RequestMapping("/api/tareas")
public class TareaController {

    private final TareaService service;

    public TareaController(TareaService service) {
        this.service = service;
    }

    @GetMapping
    public List<Tarea> listar() {
        return service.listar();
    }

    @GetMapping("/resumen")
    public Map<String, Object> resumen() {
        return Map.of(
                "total", service.listar().size(),
                "pendientes", service.contarPendientes(),
                "porcentajeAvance", service.porcentajeAvance());
    }

    @GetMapping("/{id}")
    public Tarea obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Tarea crear(@Valid @RequestBody Tarea tarea) {
        return service.crear(tarea.getTitulo(), tarea.getDescripcion());
    }

    @PutMapping("/{id}/completar")
    public Tarea completar(@PathVariable Long id) {
        return service.completar(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }

    // ----- Manejo de errores -----

    @ExceptionHandler(TareaNoEncontradaException.class)
    public ResponseEntity<Map<String, String>> noEncontrada(TareaNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<Map<String, String>> datosInvalidos(Exception ex) {
        String mensaje = ex instanceof MethodArgumentNotValidException manve
                ? manve.getBindingResult().getFieldErrors().get(0).getDefaultMessage()
                : ex.getMessage();
        return ResponseEntity.badRequest().body(Map.of("error", mensaje));
    }
}
