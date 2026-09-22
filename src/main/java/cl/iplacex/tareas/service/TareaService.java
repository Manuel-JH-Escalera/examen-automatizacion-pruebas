package cl.iplacex.tareas.service;

import cl.iplacex.tareas.model.Tarea;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lógica de negocio del gestor de tareas.
 * Usa almacenamiento en memoria para mantener el proyecto simple y
 * fácilmente desplegable en cualquier ambiente (sin base de datos externa).
 */
@Service
public class TareaService {

    private final Map<Long, Tarea> tareas = new ConcurrentHashMap<>();
    private final AtomicLong secuencia = new AtomicLong(0);

    /** Devuelve todas las tareas ordenadas por id. */
    public List<Tarea> listar() {
        List<Tarea> lista = new ArrayList<>(tareas.values());
        lista.sort(Comparator.comparing(Tarea::getId));
        return lista;
    }

    /** Busca una tarea por id; lanza excepción si no existe. */
    public Tarea obtener(Long id) {
        Tarea tarea = tareas.get(id);
        if (tarea == null) {
            throw new TareaNoEncontradaException(id);
        }
        return tarea;
    }

    /** Crea una tarea nueva. El título se normaliza (sin espacios sobrantes). */
    public Tarea crear(String titulo, String descripcion) {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("El título es obligatorio");
        }
        Long id = secuencia.incrementAndGet();
        Tarea tarea = new Tarea(id, titulo.trim(), descripcion == null ? "" : descripcion.trim(), false);
        tareas.put(id, tarea);
        return tarea;
    }

    /** Marca una tarea como completada. */
    public Tarea completar(Long id) {
        Tarea tarea = obtener(id);
        tarea.setCompletada(true);
        return tarea;
    }

    /** Elimina una tarea existente. */
    public void eliminar(Long id) {
        obtener(id);
        tareas.remove(id);
    }

    /** Cantidad de tareas pendientes (no completadas). */
    public long contarPendientes() {
        return tareas.values().stream().filter(t -> !t.isCompletada()).count();
    }

    /** Porcentaje de avance: tareas completadas sobre el total (0 si no hay tareas). */
    public int porcentajeAvance() {
        int total = tareas.size();
        if (total == 0) {
            return 0;
        }
        long completadas = total - contarPendientes();
        return (int) Math.round(completadas * 100.0 / total);
    }

    /** Limpia todas las tareas (útil para pruebas). */
    public void limpiar() {
        tareas.clear();
    }
}
