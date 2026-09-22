package cl.iplacex.tareas.service;

/**
 * Se lanza cuando se intenta operar sobre una tarea que no existe.
 */
public class TareaNoEncontradaException extends RuntimeException {

    public TareaNoEncontradaException(Long id) {
        super("No existe la tarea con id " + id);
    }
}
