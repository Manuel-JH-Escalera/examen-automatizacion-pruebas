package cl.iplacex.tareas.acceptance;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PRUEBAS DE ACEPTACIÓN (end-to-end) con Selenium WebDriver.
 *
 * Se ejecutan contra la aplicación YA DESPLEGADA en el ambiente de pruebas,
 * navegando la interfaz web real con Chrome en modo headless.
 * Forman el stage "acceptance-tests" del deployment pipeline: si fallan,
 * el pipeline ejecuta el rollback automático a la versión anterior.
 *
 * Ejecución:  mvn verify -Pacceptance -Dapp.baseUrl=https://mi-app.ejemplo.cl
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Gestor de Tareas - pruebas de aceptación E2E")
class GestorTareasAcceptanceTest {

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static String baseUrl;

    @BeforeAll
    static void abrirNavegador() {
        baseUrl = System.getProperty("app.baseUrl",
                System.getenv().getOrDefault("APP_BASE_URL", "http://localhost:8080"));
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-gpu",
                "--disable-dev-shm-usage", "--window-size=1280,900");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @AfterAll
    static void cerrarNavegador() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @Order(1)
    @DisplayName("La página principal carga y muestra la versión desplegada")
    void paginaPrincipalCarga() {
        driver.get(baseUrl + "/");

        assertThat(driver.getTitle()).isEqualTo("Gestor de Tareas");
        WebElement version = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("version")));
        wait.until(d -> !version.getText().equals("..."));
        assertThat(version.getText()).isNotBlank();
    }

    @Test
    @Order(2)
    @DisplayName("El usuario puede agregar una tarea y verla en la lista")
    void agregarTarea() {
        driver.get(baseUrl + "/");
        String titulo = "Tarea E2E " + System.currentTimeMillis();

        driver.findElement(By.id("titulo")).sendKeys(titulo);
        driver.findElement(By.id("btn-agregar")).click();

        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("lista-tareas"), titulo));
        List<WebElement> tareas = driver.findElements(By.cssSelector("#lista-tareas li"));
        assertThat(tareas).extracting(WebElement::getText).anyMatch(t -> t.contains(titulo));
        assertThat(driver.findElement(By.id("error")).getText()).isEmpty();
    }

    @Test
    @Order(3)
    @DisplayName("Agregar una tarea sin título muestra un mensaje de error")
    void agregarTareaVaciaMuestraError() {
        driver.get(baseUrl + "/");

        driver.findElement(By.id("btn-agregar")).click();

        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("error"), "obligatorio"));
        assertThat(driver.findElement(By.id("error")).getText()).isEqualTo("El título es obligatorio");
    }

    @Test
    @Order(4)
    @DisplayName("El usuario puede completar una tarea y el avance se actualiza")
    void completarTarea() {
        driver.get(baseUrl + "/");
        String titulo = "Completar E2E " + System.currentTimeMillis();
        driver.findElement(By.id("titulo")).sendKeys(titulo);
        driver.findElement(By.id("btn-agregar")).click();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("lista-tareas"), titulo));

        WebElement fila = driver.findElements(By.cssSelector("#lista-tareas li")).stream()
                .filter(li -> li.getText().contains(titulo)).findFirst().orElseThrow();
        String id = fila.getAttribute("data-id");
        fila.findElement(By.cssSelector(".btn-completar")).click();

        By filaCompletada = By.cssSelector("#lista-tareas li.completada[data-id='" + id + "']");
        wait.until(ExpectedConditions.presenceOfElementLocated(filaCompletada));
        int avance = Integer.parseInt(driver.findElement(By.id("avance")).getText());
        assertThat(avance).isBetween(1, 100);
    }

    @Test
    @Order(5)
    @DisplayName("El usuario puede eliminar una tarea")
    void eliminarTarea() {
        driver.get(baseUrl + "/");
        String titulo = "Eliminar E2E " + System.currentTimeMillis();
        driver.findElement(By.id("titulo")).sendKeys(titulo);
        driver.findElement(By.id("btn-agregar")).click();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("lista-tareas"), titulo));

        WebElement fila = driver.findElements(By.cssSelector("#lista-tareas li")).stream()
                .filter(li -> li.getText().contains(titulo)).findFirst().orElseThrow();
        String id = fila.getAttribute("data-id");
        fila.findElement(By.cssSelector(".btn-eliminar")).click();

        wait.until(ExpectedConditions.numberOfElementsToBe(
                By.cssSelector("#lista-tareas li[data-id='" + id + "']"), 0));
        assertThat(driver.findElement(By.id("lista-tareas")).getText()).doesNotContain(titulo);
    }
}
