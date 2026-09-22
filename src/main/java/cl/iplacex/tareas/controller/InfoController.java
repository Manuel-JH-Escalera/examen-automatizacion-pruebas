package cl.iplacex.tareas.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Expone información de la versión desplegada.
 * El deployment pipeline la usa para verificar qué versión y qué slot
 * (blue o green) está atendiendo el tráfico, y para evidenciar el rollback.
 */
@RestController
public class InfoController {

    @Value("${app.version:dev}")
    private String version;

    @Value("${app.commit:local}")
    private String commit;

    @Value("${app.slot:local}")
    private String slot;

    @GetMapping("/api/info")
    public Map<String, String> info() {
        return Map.of(
                "aplicacion", "gestor-tareas",
                "version", version,
                "commit", commit,
                "slot", slot);
    }
}
