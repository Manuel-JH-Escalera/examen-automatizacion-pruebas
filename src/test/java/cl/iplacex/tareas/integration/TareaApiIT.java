package cl.iplacex.tareas.integration;

import cl.iplacex.tareas.service.TareaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PRUEBAS DE INTEGRACIÓN.
 * Levantan el contexto completo de Spring Boot y prueban la API REST
 * de extremo a extremo (controlador + servicio + serialización JSON + validación).
 * Las ejecuta maven-failsafe-plugin en la fase "verify" (sufijo IT).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API /api/tareas - pruebas de integración")
class TareaApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TareaService service;

    @BeforeEach
    void limpiar() {
        service.limpiar();
    }

    @Test
    @DisplayName("GET /api/tareas devuelve lista vacía al inicio")
    void listarVacio() throws Exception {
        mockMvc.perform(get("/api/tareas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("POST /api/tareas crea una tarea y responde 201")
    void crearTarea() throws Exception {
        mockMvc.perform(post("/api/tareas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Integrar pipeline\",\"descripcion\":\"CI/CD\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.titulo").value("Integrar pipeline"))
                .andExpect(jsonPath("$.completada").value(false));

        mockMvc.perform(get("/api/tareas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("POST /api/tareas sin título responde 400 con mensaje de validación")
    void crearTareaInvalida() throws Exception {
        mockMvc.perform(post("/api/tareas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"\",\"descripcion\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El título es obligatorio"));
    }

    @Test
    @DisplayName("PUT /api/tareas/{id}/completar marca la tarea y actualiza el resumen")
    void completarTareaYResumen() throws Exception {
        var tarea = service.crear("Desplegar", "");

        mockMvc.perform(put("/api/tareas/{id}/completar", tarea.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completada").value(true));

        mockMvc.perform(get("/api/tareas/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.pendientes").value(0))
                .andExpect(jsonPath("$.porcentajeAvance").value(100));
    }

    @Test
    @DisplayName("DELETE /api/tareas/{id} elimina y luego responde 404")
    void eliminarTarea() throws Exception {
        var tarea = service.crear("Temporal", "");

        mockMvc.perform(delete("/api/tareas/{id}", tarea.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tareas/{id}", tarea.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No existe la tarea con id " + tarea.getId()));
    }

    @Test
    @DisplayName("GET /api/info expone versión y commit desplegados")
    void infoVersion() throws Exception {
        mockMvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aplicacion").value("gestor-tareas"))
                .andExpect(jsonPath("$.version").isString());
    }

    @Test
    @DisplayName("GET /actuator/health responde UP (usado por el health check del despliegue)")
    void healthUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
