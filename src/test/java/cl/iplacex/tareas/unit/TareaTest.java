package cl.iplacex.tareas.unit;

import cl.iplacex.tareas.model.Tarea;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PRUEBAS UNITARIAS del modelo Tarea: validaciones de Bean Validation.
 */
@DisplayName("Tarea - validaciones del modelo")
class TareaTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("Una tarea válida no tiene violaciones")
    void tareaValida() {
        Tarea tarea = new Tarea(null, "Título correcto", "Descripción", false);

        Set<ConstraintViolation<Tarea>> violaciones = validator.validate(tarea);

        assertThat(violaciones).isEmpty();
    }

    @Test
    @DisplayName("El título en blanco es inválido")
    void tituloEnBlancoEsInvalido() {
        Tarea tarea = new Tarea(null, "  ", "", false);

        Set<ConstraintViolation<Tarea>> violaciones = validator.validate(tarea);

        assertThat(violaciones).hasSize(1);
        assertThat(violaciones.iterator().next().getMessage()).isEqualTo("El título es obligatorio");
    }

    @Test
    @DisplayName("Un título de más de 100 caracteres es inválido")
    void tituloMuyLargoEsInvalido() {
        Tarea tarea = new Tarea(null, "x".repeat(101), "", false);

        Set<ConstraintViolation<Tarea>> violaciones = validator.validate(tarea);

        assertThat(violaciones).extracting(ConstraintViolation::getMessage)
                .containsExactly("El título no puede superar los 100 caracteres");
    }
}
