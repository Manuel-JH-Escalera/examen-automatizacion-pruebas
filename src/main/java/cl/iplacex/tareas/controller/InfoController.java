package cl.iplacex.tareas.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Expone información de la versión desplegada.
 * El pipeline de despliegue la usa para verificar qué versión está activa
 * (y para evidenciar el rollback).
 */
@RestController
public class InfoController {

    @Value("${app.version:dev}")
    private String version;

    @Value("${app.commit:local}")
    private String commit;

    @GetMapping("/api/info")
    public Map<String, String> info() {
        return Map.of("aplicacion", "gestor-tareas", "version", version, "commit", commit);
    }
}
