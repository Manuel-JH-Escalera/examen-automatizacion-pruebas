# Gestor de Tareas – CI/CD con pruebas automatizadas

Proyecto del **Examen Final de Automatización de Pruebas** (Iplacex).
Autor: **Manuel Escalera**.

Aplicación web de gestión de tareas (Java 17 + Spring Boot 3) que sirve de base para
demostrar un flujo completo de entrega continua: control de versiones con **GitFlow**,
pipeline de **integración continua** con pruebas unitarias e integración, y un
**deployment pipeline** con pruebas de aceptación end-to-end, despliegue a un ambiente
de pruebas y **rollback automático**.

[![CI](https://github.com/Manuel-JH-Escalera/examen-automatizacion-pruebas/actions/workflows/ci.yml/badge.svg)](https://github.com/Manuel-JH-Escalera/examen-automatizacion-pruebas/actions/workflows/ci.yml)
[![CD](https://github.com/Manuel-JH-Escalera/examen-automatizacion-pruebas/actions/workflows/cd.yml/badge.svg)](https://github.com/Manuel-JH-Escalera/examen-automatizacion-pruebas/actions/workflows/cd.yml)

---

## 1. Descripción del proyecto

| Elemento | Tecnología |
|---|---|
| Lenguaje / framework | Java 17, Spring Boot 3.4 (API REST + página web estática) |
| Construcción | Apache Maven 3.9 |
| Pruebas unitarias | JUnit 5, AssertJ (plugin Surefire) |
| Pruebas de integración | Spring Boot Test + MockMvc (plugin Failsafe) |
| Pruebas de aceptación | Selenium WebDriver 4 + Chrome headless (perfil `acceptance`) |
| Cobertura | JaCoCo |
| Contenedores | Docker (imagen multi-stage) publicada en GitHub Container Registry |
| CI/CD | GitHub Actions |
| Ambiente de pruebas | Contenedores Docker en el runner: router nginx + slots Blue/Green |

### Funcionalidad de la aplicación

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/` | Interfaz web para gestionar tareas |
| `GET` | `/api/tareas` | Lista las tareas |
| `POST` | `/api/tareas` | Crea una tarea (`titulo` obligatorio) |
| `PUT` | `/api/tareas/{id}/completar` | Marca una tarea como completada |
| `DELETE` | `/api/tareas/{id}` | Elimina una tarea |
| `GET` | `/api/tareas/resumen` | Total, pendientes y % de avance |
| `GET` | `/api/info` | Versión y commit desplegados (usado por el pipeline) |
| `GET` | `/actuator/health` | Health check (usado por Docker y el pipeline Blue-Green) |

### Estructura del repositorio

```
.
├── pom.xml                         # Dependencias y plugins de pruebas
├── Dockerfile                      # Imagen multi-stage (build Maven + JRE Alpine)
├── .github/workflows/
│   ├── ci.yml                      # Pipeline CI: build → unitarias → integración → imagen
│   └── cd.yml                      # Deployment pipeline: imagen → deploy Blue-Green → acceptance → rollback/promote
├── scripts/
│   ├── blue-green.sh               # Router nginx + contenedores blue/green, switch y rollback
│   └── resumen-pruebas.sh          # Resumen Markdown de reportes Surefire/Failsafe
├── src/main/java/cl/iplacex/tareas # Aplicación (modelo, servicio, controladores)
├── src/main/resources/static       # Interfaz web (index.html)
├── src/test/java/cl/iplacex/tareas
│   ├── unit/                       # *Test.java      → pruebas unitarias
│   ├── integration/                # *IT.java        → pruebas de integración
│   └── acceptance/                 # *AcceptanceTest → pruebas E2E con Selenium
└── docs/capturas                   # Evidencias de ejecución
```

---

## 2. Estrategia de pruebas implementada

Se aplica la **pirámide de pruebas**: muchas pruebas unitarias rápidas, un número menor de
pruebas de integración y pocas pruebas de aceptación end-to-end, cada capa ejecutada en
un stage distinto del pipeline para obtener retroalimentación temprana.

| Capa | Qué valida | Herramientas | Convención | Fase Maven | Dónde corre |
|---|---|---|---|---|---|
| **Unitarias** (14 casos) | Lógica de negocio de `TareaService` y validaciones del modelo `Tarea`, sin Spring | JUnit 5, AssertJ, Bean Validation | `*Test.java` | `test` (Surefire) | CI, stage 2 |
| **Integración** (7 casos) | API REST completa con el contexto de Spring levantado: rutas, JSON, códigos HTTP, validación, health | `@SpringBootTest`, MockMvc | `*IT.java` | `verify` (Failsafe) | CI, stage 3 |
| **Aceptación / E2E** (5 casos) | Flujos de usuario reales en el navegador contra la aplicación desplegada: cargar página, agregar, validar error, completar y eliminar tareas | Selenium WebDriver, Chrome headless | `*AcceptanceTest.java` | `verify -Pacceptance` (Failsafe) | CD, stage 3 |

Otras verificaciones automatizadas:

- **Smoke test de la imagen Docker** (CI, stage 4): la imagen se construye, arranca y responde
  `/actuator/health`.
- **Smoke test de GREEN antes de recibir tráfico** (CD): el pipeline consulta `/api/info` del
  contenedor nuevo y confirma que reporta el commit esperado antes de enrutarle tráfico.
- **Puerta de calidad para el rollback**: si las pruebas de aceptación fallan, el paso de rollback
  devuelve el tráfico a BLUE de forma automática y verifica que la versión anterior atiende.

---

## 3. Flujo de ramas: GitFlow

| Rama | Propósito | Pipeline que dispara |
|---|---|---|
| `main` | Código en producción/ambiente de pruebas. Solo recibe merges desde `release/*` o `hotfix/*` | CI + **CD (deploy)** |
| `develop` | Rama de integración del desarrollo | CI |
| `feature/*` | Una rama por funcionalidad, nace y muere en `develop` | CI |
| `release/*` | Preparación de una versión; nace de `develop` y se integra en `main` y `develop` | CI |
| `hotfix/*` | Correcciones urgentes sobre `main` | CI |

Cada versión integrada en `main` se etiqueta (`v1.0.0`). Los merges se hacen con `--no-ff`
para conservar el historial de cada rama.

---

## 4. Pipelines

### 4.1 CI – `.github/workflows/ci.yml`

Se ejecuta en cada push o pull request a `main`, `develop`, `feature/**`, `release/**` y `hotfix/**`.

```
build ──► unit-tests ──► integration-tests ──► docker-image
```

1. **build**: `mvn package -DskipTests`; guarda el `.jar` como artefacto.
2. **unit-tests**: `mvn test`; publica reportes Surefire y cobertura JaCoCo y un resumen en el
   *Summary* de la ejecución.
3. **integration-tests**: `mvn verify -DskipUnitTests=true`; publica reportes Failsafe.
4. **docker-image**: construye la imagen, la levanta y comprueba el health check.

### 4.2 Deployment pipeline – `.github/workflows/cd.yml`

Se ejecuta al hacer push a `main` (o manualmente desde *Actions*).

```
build-image ──► deploy-test (Blue-Green + acceptance tests + rollback) ──► promote
```

**Ambiente de pruebas.** Se levanta con Docker en el runner mediante `scripts/blue-green.sh`:
un router **nginx** expone un único punto de entrada (`http://localhost:8080`) y enruta el tráfico
a uno de dos contenedores, **BLUE** (versión estable actual) o **GREEN** (versión nueva). Cambiar de
versión solo reescribe el upstream de nginx y lo recarga, así que el cambio es instantáneo y el
rollback consiste en volver a BLUE. Cada respuesta incluye la cabecera `X-Deploy-Slot` y
`/api/info` reporta `slot`, `commit` y `version`, lo que permite verificar en todo momento qué
versión atiende.

```
                 ┌──────────────┐
 http://localhost:8080 ─►│ router nginx │──► app-blue  (imagen :stable, versión anterior)
                 │  (switch)    │──► app-green (imagen :sha-xxxxxxx, versión nueva)
                 └──────────────┘
```

1. **build-image**: construye la imagen Docker con `APP_VERSION` y `APP_COMMIT` y la publica en
   GHCR con el tag inmutable `sha-<commit>` (y `latest`).
2. **deploy-test** (stages 2 y 3 en el mismo runner, porque comparten el ambiente):
   - *Deploy*: levanta el router; arranca **BLUE** con la imagen `stable` (o `latest` en el primer
     despliegue) y espera su health check; arranca **GREEN** con la imagen nueva y espera su health
     check; hace un *smoke test* de GREEN y recién entonces cambia el tráfico del router a GREEN.
   - *Acceptance*: ejecuta las pruebas Selenium contra el router (es decir, contra GREEN).
   - *Rollback* (solo si algo falló): devuelve el tráfico a BLUE, verifica por `X-Deploy-Slot` y
     `/api/info` que la versión anterior atiende y retira el contenedor GREEN.
   - Si todo pasó, retira BLUE y GREEN queda como versión activa.
3. **promote** (solo si todo pasó): etiqueta la imagen como `stable`, la última versión conocida
   como buena, que será el BLUE del próximo despliegue.

El pipeline manual admite el parámetro **`simular_falla`**, que hace fallar el stage de
aceptación a propósito para **demostrar el rollback automático** con evidencia real.

---

## 5. Cómo ejecutar

### Requisitos

- JDK 17, Maven 3.9, Google Chrome (solo para las pruebas de aceptación), Docker (opcional).

### Pruebas en local

```bash
# Pruebas unitarias
mvn test

# Unitarias + integración (build completo)
mvn verify

# Solo integración
mvn verify -DskipUnitTests=true

# Levantar la aplicación
mvn spring-boot:run          # http://localhost:8080

# Pruebas de aceptación contra una app en ejecución
mvn verify -Pacceptance -Dapp.baseUrl=http://localhost:8080
```

Reportes: `target/surefire-reports`, `target/failsafe-reports`, `target/site/jacoco/index.html`.

### Docker

```bash
docker build -t gestor-tareas .
docker run -p 8080:8080 gestor-tareas
```

### Ambiente Blue-Green en local (opcional, requiere Docker)

```bash
docker build --build-arg APP_COMMIT=v1 -t gestor-tareas:v1 .
docker build --build-arg APP_COMMIT=v2 -t gestor-tareas:v2 .
scripts/blue-green.sh up-router
scripts/blue-green.sh start blue  gestor-tareas:v1 v1 && scripts/blue-green.sh wait-healthy blue
scripts/blue-green.sh start green gestor-tareas:v2 v2 && scripts/blue-green.sh wait-healthy green
scripts/blue-green.sh switch green      # despliegue
curl -i http://localhost:8080/api/info  # X-Deploy-Slot: green
scripts/blue-green.sh switch blue       # rollback
scripts/blue-green.sh down
```

---

## 6. Evidencias

Las capturas de las ejecuciones se encuentran en `docs/capturas/`:

| Archivo | Evidencia |
|---|---|
| `01-repositorio-github.png` | Repositorio en GitHub con las ramas GitFlow |
| `02-ramas-gitflow.png` | Grafo de ramas (`feature`, `develop`, `release`, `main`) |
| `03-ci-ejecucion-exitosa.png` | Pipeline CI con los cuatro stages en verde |
| `04-ci-resumen-pruebas.png` | Resumen de pruebas unitarias e integración en el Summary |
| `05-cd-despliegue-exitoso.png` | Deployment pipeline exitoso: imagen, deploy Blue-Green, acceptance tests y promote |
| `06-cd-blue-green-switch.png` | Log del cambio de tráfico de BLUE a GREEN y versión servida por el router |
| `07-cd-rollback-automatico.png` | Ejecución con falla simulada: paso de rollback devolviendo el tráfico a BLUE |
| `08-cd-resumen-rollback.png` | Summary de la ejecución con el rollback y las versiones involucradas |
