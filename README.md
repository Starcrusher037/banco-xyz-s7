# Banco XYZ - Microservicios Resilientes y Seguridad en la Nube con Spring Cloud
### Asignatura: Desarrollo Backend III (PBY2203) - Experiencia 2 / Semana 6
**Integrantes:** Leonardo Bustamante - Carolina Delgado  
**Repositorio GitHub:** [https://github.com/Starcrusher037/Banco-xyz-BFF.git](https://github.com/Starcrusher037/Banco-xyz-BFF.git)

---

## 1. Arquitectura del Sistema (Semana 6)

El sistema evoluciona a una arquitectura de **microservicios distribuidos, resilientes e independientes** construidos con **Java 17**, **Spring Boot 3.2.5** y **Spring Cloud 2023.0.1**. Cada carpeta representa un proyecto **completamente autónomo e independiente** con su propio ciclo de vida Maven.

```mermaid
flowchart TD
    ConfigServer["config-server (Puerto 8888)<br/>Servidor Central de Configuración"] -.->|Provee propiedades| CoreService["core-service (Puerto 8080)<br/>Microservicio Central de Negocio"]
    ConfigServer -.->|Provee propiedades| BFFMovil["bff-movil (Puerto 8081)<br/>BFF Móvil + Circuit Breaker"]
    ConfigServer -.->|Provee propiedades| BFFWeb["bff-web (Puerto 8082)<br/>BFF Web"]
    ConfigServer -.->|Provee propiedades| BFFCajero["bff-cajero (Puerto 8083)<br/>BFF Cajero"]

    Eureka["discovery-server (Eureka - Puerto 8761)<br/>Service Discovery"] <-->|Registro y Latidos| CoreService
    Eureka <-->|Registro y Descubrimiento| BFFMovil
    Eureka <-->|Registro y Descubrimiento| BFFWeb
    Eureka <-->|Registro y Descubrimiento| BFFCajero

    Cliente["Cliente Móvil / Web / ATM"] -->|JWT Auth (Canal + Claims)| BFFMovil
    BFFMovil -->|Circuit Breaker (Resilience4j) + Fallback| CoreService
```

---

## 2. Requerimientos Implementados

### 1. Servidor Centralizado de Configuración (`config-server` - Puerto 8888)
* Microservicio con `@EnableConfigServer` utilizando el perfil `native`.
* Ubicación de propiedades: `src/main/resources/config/`.
* Provee configuraciones centralizadas (`core-service.properties`, `bff-movil.properties`, `bff-web.properties`, `bff-cajero.properties` y `application.properties` global con llaves JWT y métricas).
* Consumido por `core-service` y los microservicios BFF mediante `spring.config.import=optional:configserver:http://localhost:8888`.

### 2. Service Discovery (`discovery-server` - Puerto 8761)
* Servidor de registro y descubrimiento con **Netflix Eureka Server** (`@EnableEurekaServer`).
* Dashboard Eureka accesible en: `http://localhost:8761/`.
* Todos los microservicios (`core-service`, `bff-movil`, `bff-web`, `bff-cajero`) están anotados con `@EnableDiscoveryClient` y se auto-registran dinámicamente.

### 3. Tolerancia a Fallos (Resilience4j) y Seguridad
* **Tolerancia a Fallos / Circuit Breaker**:
  * Implementado en `bff-movil` (`MovilBffService`) utilizando `@CircuitBreaker(name = "coreService", fallbackMethod = "...")` y `@Retry(name = "coreService")`.
  * Si el `core-service` experimenta fallas o demoras, el circuito se abre y se ejecutan métodos de **Fallback** devolviendo respuestas en modo degradado controlado sin que el cliente reciba un error 500 no controlado.
* **Seguridad y Criptografía (Intacta)**:
  * **HTTPS / SSL (TLS)** con certificados digitales PKCS12 (`keystore.p12`) en todos los microservicios.
  * **Tokens JWT** firmados con HMAC-SHA256 y claims estrictos por canal (`MOVIL`, `WEB`, `CAJERO_ATM`).
  * **API-Key interna** (`X-Internal-Api-Key`) para proteger el perímetro entre BFFs y `core-service`.

---

## 3. Estructura de Proyectos Independientes

Cada directorio es un proyecto independiente con su propio `pom.xml` (`spring-boot-starter-parent`):

```text
Exp2_S4_Grupo3-main/
├── config-server/        # [Puerto 8888] Servidor Central de Configuración Spring Cloud
├── discovery-server/     # [Puerto 8761] Servidor Eureka de Registro y Descubrimiento
├── core-service/         # [Puerto 8080 - HTTPS] Microservicio Core de Negocio Bancario
├── bff-movil/            # [Puerto 8081 - HTTPS] Microservicio BFF Móvil con Resilience4j
├── bff-web/              # [Puerto 8082 - HTTPS] Microservicio BFF Web
├── bff-cajero/           # [Puerto 8083 - HTTPS] Microservicio BFF Cajero Automático
└── pom.xml               # POM raíz multi-proyecto opcional para compilación unificada
```

---

## 4. Orden de Ejecución

Para levantar el ecosistema completo:

1. **Servidor de Configuración (Primero)**:
   ```bash
   cd config-server
   mvn spring-boot:run
   ```
2. **Servidor de Descubrimiento Eureka (Segundo)**:
   ```bash
   cd discovery-server
   mvn spring-boot:run
   ```
   *Dashboard en:* `http://localhost:8761`
3. **Core Bancario**:
   ```bash
   cd core-service
   mvn spring-boot:run
   ```
4. **Microservicios BFFs**:
   ```bash
   cd bff-movil
   mvn spring-boot:run
   ```
