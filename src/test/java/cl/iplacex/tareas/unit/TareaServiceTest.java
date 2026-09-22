package cl.iplacex.tareas.unit;

import cl.iplacex.tareas.model.Tarea;
import cl.iplacex.tareas.service.TareaNoEncontradaException;
import cl.iplacex.tareas.service.TareaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PRUEBAS UNITARIAS (JUnit 5).
 * Prueban la lógica de negocio de TareaService de forma aislada,
 * sin levantar el contexto de Spring ni el servidor web.
 * Las ejecuta maven-surefire-plugin en la fase "test".
 */
@DisplayName("TareaService - pruebas unitarias")
class TareaServiceTest {

    private TareaService service;

    @BeforeEach
    void setUp() {
        service = new TareaService();
    }

    @Test
    @DisplayName("Crear una tarea asigna id, guarda el título y queda pendiente")
    void crearTareaAsignaIdYQuedaPendiente() {
        Tarea tarea = service.crear("Estudiar Maven", "Repasar el pom.xml");

        assertThat(tarea.getId()).isEqualTo(1L);
        assertThat(tarea.getTitulo()).isEqualTo("Estudiar Maven");
        assertThat(tarea.getDescripcion()).isEqualTo("Repasar el pom.xml");
        assertThat(tarea.isCompletada()).isFalse();
    }

    @Test
    @DisplayName("Crear una tarea normaliza los espacios del título")
    void crearTareaNormalizaTitulo() {
        Tarea tarea = service.crear("   Configurar CI   ", null);

        assertThat(tarea.getTitulo()).isEqualTo("Configurar CI");
        assertThat(tarea.getDescripcion()).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Crear una tarea sin título lanza IllegalArgumentException")
    void crearTareaSinTituloFalla(String titulo) {
        assertThatThrownBy(() -> service.crear(titulo, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("título");
    }

    @Test
    @DisplayName("Los ids son correlativos y el listado sale ordenado")
    void idsCorrelativosYListadoOrdenado() {
        service.crear("A", "");
        service.crear("B", "");
        service.crear("C", "");

        assertThat(service.listar())
                .extracting(Tarea::getId)
                .containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("Completar una tarea cambia su estado")
    void completarTarea() {
        Tarea tarea = service.crear("Escribir pruebas", "");

        Tarea completada = service.completar(tarea.getId());

        assertThat(completada.isCompletada()).isTrue();
        assertThat(service.obtener(tarea.getId()).isCompletada()).isTrue();
    }

    @Test
    @DisplayName("Eliminar una tarea la quita del listado")
    void eliminarTarea() {
        Tarea tarea = service.crear("Borrar", "");

        service.eliminar(tarea.getId());

        assertThat(service.listar()).isEmpty();
        assertThatThrownBy(() -> service.obtener(tarea.getId()))
                .isInstanceOf(TareaNoEncontradaException.class);
    }

    @Test
    @DisplayName("Operar sobre un id inexistente lanza TareaNoEncontradaException")
    void operarSobreIdInexistenteFalla() {
        assertThatThrownBy(() -> service.completar(99L))
                .isInstanceOf(TareaNoEncontradaException.class)
                .hasMessage("No existe la tarea con id 99");
        assertThatThrownBy(() -> service.eliminar(99L))
                .isInstanceOf(TareaNoEncontradaException.class);
    }

    @Test
    @DisplayName("El porcentaje de avance se calcula correctamente")
    void porcentajeAvance() {
        assertThat(service.porcentajeAvance()).isZero();

        service.crear("1", "");
        service.crear("2", "");
        service.crear("3", "");
        assertThat(service.porcentajeAvance()).isZero();
        assertThat(service.contarPendientes()).isEqualTo(3);

        service.completar(1L);
        assertThat(service.porcentajeAvance()).isEqualTo(33);
        assertThat(service.contarPendientes()).isEqualTo(2);

        service.completar(2L);
        service.completar(3L);
        assertThat(service.porcentajeAvance()).isEqualTo(100);
        assertThat(service.contarPendientes()).isZero();
    }
}
