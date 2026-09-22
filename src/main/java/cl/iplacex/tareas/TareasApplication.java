package cl.iplacex.tareas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicación "Gestor de Tareas".
 * Expone una API REST (/api/tareas) y una página web estática (/).
 */
@SpringBootApplication
public class TareasApplication {

    public static void main(String[] args) {
        SpringApplication.run(TareasApplication.class, args);
    }
}
