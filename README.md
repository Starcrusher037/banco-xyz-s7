# Banco XYZ - Arquitectura de Microservicios Distribuida con Spring Cloud, Tolerancia a Fallos (Resilience4j) y Arquitectura de Eventos con JMS y Patrón SAGA
### Asignatura: Desarrollo Backend III (PBY2203) - Experiencia 3 / Semanas 6 y 7
**Grupo:** Grupo 3  
**Integrantes:** Leonardo Bustamante - Carolina Delgado Sapunar  
**Repositorio GitHub:** [https://github.com/Lybern/Exp3_S6_Grupo3](https://github.com/Lybern/Exp3_S6_Grupo3)

---

## 1. Descripción del proyecto

Se diseñó e implementó una arquitectura distribuida de microservicios autónomos, tolerantes a fallos y orientada a eventos basada en **Spring Cloud 2025.1.2**, **Spring Boot 4.x**, **Apache ActiveMQ Artemis JMS** y **Java 21** para el **Banco XYZ**.

El sistema desacopla la lógica de negocio central en un **Core Service** y los canales de interacción mediante el patrón **Backend for Frontend (BFF)** (`bff-movil`, `bff-web`, `bff-cajero`), orquestados por un **Servidor de Configuración Centralizada (Spring Cloud Config Server)** y un **Servidor de Descubrimiento (Netflix Eureka Server)** con soporte de:
* **Balanceo de Carga del Lado del Cliente** (@LoadBalanced RestTemplate + Eureka).
* **Tolerancia a Fallos y Resiliencia:** Circuit Breakers, Retry con retroceso y Fallbacks mediante **Resilience4j**.
* **Arquitectura Orientada a Eventos (EDA) y Patrón SAGA:** Procesamiento asíncrono y desacoplado de transferencias interbancarias mediante **JMS (ActiveMQ Artemis)** con transacciones compensatorias automáticas (Rollback semántico) y bitácora inmutable de auditoría.

```
                              +-----------------------+
                              |     CONFIG SERVER     |
                              |    (Puerto: 8888)     |
                              | * Repositorio Nativo  |
                              | * config-repo/*.props |
                              +-----------+-----------+
                                          |
               +--------------------------+--------------------------+
               |                          |                          |
               v                          v                          v
    +--------------------+     +--------------------+     +--------------------+
    |  DISCOVERY SERVER  |     |    CORE SERVICE    |     |   BFFs (MOVIL/WEB/ |
    |  (Eureka: 8761)    |<----+   (Puerto: 8080)   |     |    CAJERO ATM)     |
    | * Registro Activo  |     | * Cuentas y Saldos |     | * Canales HTTPS    |
    | * Heartbeat / Info |     | * Persistencia CSV |     | * JWT + Delegated  |
    +---------^----------+     +----------^---------+     +---------+----------+
              |                           |                         |
              +---------------------------+-------------------------+
                                          |
                                          | @LoadBalanced RestTemplate + Eureka
                                          | Circuit Breaker, Retry & Fallback (Resilience4j)
                                          +-------------------------+

                     === ARQUITECTURA DE EVENTOS Y SAGA (JMS) ===
                     
       [BFF Móvil / API] ----(POST /transferencia)----> [Saga Orchestrator]
                                                                |
                                             Paso 1: Débito atómico en Origen
                                                                |
                          +-------------------------------------+
                          |
                          v (Mensajería Asíncrona JMS)
          +-------------------------------+
          |  ActiveMQ Artemis JMS Broker  |
          |  (Embebido en Core Service)   |
          +---------------+---------------+
                          |
     +--------------------+--------------------+
     |                                         |
     v cola: banco.saga.transferencias         v cola: banco.saga.compensaciones
[Saga Consumer: Abono Destino]           [Saga Consumer: Compensación/Rollback]
     |                                         |
     +-- Éxito: Acredita saldo y completa      +-- Fallo en destino: Reversa débito
     |                                             y restituye saldo a origen
     v cola: banco.eventos.auditoria
[Bitácora de Auditoría Inmutable]
```

---

## 2. Arquitectura de Eventos: Patrón SAGA y Mensajería JMS 

### 2.1 Justificación del Patrón SAGA frente a 2PC (Two-Phase Commit)
En arquitecturas distribuidas de microservicios en la nube, el uso de transacciones ACID distribuidas clásicas (2PC / XA) introduce bloqueos pesados y cuellos de botella que destruyen la escalabilidad y disponibilidad del sistema (Teorema CAP). 

Por ello, se implementó el **Patrón SAGA** mediante mensajería asíncrona JMS:
1. **Desacoplamiento Temporal:** La solicitud de transferencia no mantiene bloqueada la conexión HTTP del cliente mientras se procesa la acreditación en la cuenta destino.
2. **Consistencia Eventual:** Cada paso de la transacción realiza un cambio local en su estado y emite un evento al broker de mensajería.
3. **Transacciones Compensatorias (Rollback Semántico):** Si el abono en la cuenta de destino falla (por cuenta inexistente, estado bloqueado o fallo de sistema), el consumidor detecta la anomalía y despacha un evento compensatorio (`SagaCompensacionEvent`) que revierte automáticamente los fondos al titular de la cuenta origen.

### 2.2 Tópicos / Colas de Mensajería JMS Implementadas

| Nombre de la Cola JMS | Tipo | Mensaje / Evento | Propósito |
| :--- | :---: | :--- | :--- |
| **`banco.saga.transferencias`** | Point-to-Point (Queue) | `SagaTransferenciaEvent` | Despacho asíncrono para ejecutar el Paso 2 (Abono en cuenta destino) de forma desacoplada. |
| **`banco.saga.compensaciones`** | Point-to-Point (Queue) | `SagaCompensacionEvent` | Disparo de la transacción compensatoria ante excepciones en destino (restitución íntegra del saldo original). |
| **`banco.eventos.auditoria`** | Point-to-Point (Queue) | `AuditoriaEventoDto` | Bitácora inmutable de eventos transaccionales (`TRANSFERENCIA_INICIADA`, `TRANSFERENCIA_EXITOSA`, `COMPENSACION_SOLICITADA`, `COMPENSACION_FINALIZADA`). |

### 2.3 Máquina de Estados de la SAGA

* **`INICIADA`:** Solicitud recibida y registrada por el orquestador.
* **`DEBITO_COMPLETADO`:** Paso 1 finalizado (fondos debitados en origen; evento emitido al broker JMS).
* **`COMPLETADA`:** Paso 2 ejecutado con éxito por el consumidor JMS (fondos acreditados en destino).
* **`COMPENSADA`:** Ocurrió un error en el destino y la transacción compensatoria revirtió exitosamente el débito, devolviendo el saldo al origen.
* **`FALLIDA`:** La operación fue rechazada en el paso 1 (por ejemplo, fondos insuficientes).

---

## 3. Tolerancia a Fallos con Resilience4j (Semana 6 y 7)

La resiliencia se aplica en dos niveles críticos del ecosistema:

### A. Nivel Canales / Clientes BFF (`bff-movil`, `bff-web`, `bff-cajero`):
* **Circuit Breaker (`coreServiceCB`):**
  * `slidingWindowSize: 10`
  * `failureRateThreshold: 50%`
  * `waitDurationInOpenState: 10.000 ms`
  * `permittedNumberOfCallsInHalfOpenState: 3`
* **Retry:** 3 intentos automáticos con intervalo de 1.000 ms ante fallas de red.
* **Fallback:** Respuestas contingentes en modo degradado (`DEGRADADO_FALLBACK`).

### B. Nivel Mensajería y Core Service (`core-service`):
* **Circuit Breaker & Retry en Publicador JMS (`sagaJmsCB`):**
  * Protege las operaciones de publicación hacia el broker ActiveMQ Artemis. Si el broker no está disponible, el fallback intercepta la falla antes de corromper la transacción local.
* **Circuit Breaker & Retry en Consumidor JMS (`sagaConsumerCB`):**
  * Protege la ejecución del Paso 2 (abono en destino). Si se agotan los reintentos o el circuito se abre, se activa de forma garantizada el fallback de compensación enviando el mensaje a `banco.saga.compensaciones`.

---

## 4. Arquitectura de Microservicios y Componentes

### 4.1 Componentes Troncales Evaluados (Alcance Principal)
Los siguientes componentes constituyen el núcleo del sistema evaluado en la Experiencia 3 (Semana 7). El sistema es 100% funcional y evaluable únicamente con estos tres microservicios:

| Microservicio / Servidor | Puerto | Protocolo / Seguridad | Responsabilidad Principal | Estado en Evaluación |
| :--- | :---: | :--- | :--- | :---: |
| **`config-server`** | `8888` | HTTP (Basic Auth: `admin`/`gato`) | Servidor Spring Cloud Config con perfil nativo. Expone las propiedades centralizadas de todos los microservicios desde `config-repo/`. | **Obligatorio (Core)** |
| **`discovery-server`** | `8761` | HTTP (Basic Auth: `eureka`/`eureka2026`) | Servidor Netflix Eureka para registro y descubrimiento dinámico de instancias en tiempo de ejecución. | **Obligatorio (Core)** |
| **`core-service`** | `8080` | HTTP (Service Token JWT / Endpoints SAGA) | Core Bancario. Persistencia CSV, motor SAGA orquestado, broker JMS ActiveMQ Artemis embebido, colas transaccionales y Actuator. | **Obligatorio (Core)** |

### 4.2 Componentes Cliente / Arnés de Pruebas E2E (Desacoplados y Opcionales)
Los siguientes módulos actúan como clientes perimetrales especializados por canal. Están desacoplados del Core y no son requeridos para evaluar la lógica bancaria:

| Módulo Cliente | Puerto | Protocolo / Seguridad | Propósito como Cliente / Arnés de Prueba | Estado en Evaluación |
| :--- | :---: | :--- | :--- | :---: |
| **`bff-movil`** | `8443` | HTTPS / PKCS12 + JWT (`aud: MOVIL`) | Cliente móvil: valida payloads livianos, invoca transferencias SAGA y prueba Circuit Breakers en el consumidor. | **Opcional (Cliente)** |
| **`bff-web`** | `8444` | HTTPS / PKCS12 + JWT (`aud: WEB`) | Cliente web: consume vistas consolidadas anuales y prueba cabeceras seguras OWASP. | **Opcional (Cliente)** |
| **`bff-cajero`** | `8445` | HTTPS / PKCS12 + JWT (`aud: ATM`) | Terminal ATM: prueba límites de giro (\$200.000) y múltiplos de dispensación (\$5.000). | **Opcional (Cliente)** |

---

### 4.3 Nota de Alcance Arquitectónico: Justificación Técnica de los BFFs
En consideración directa a la retroalimentación docente sobre mantener el foco en los componentes troncales solicitados:

1. **Autonomía Total del Core Bancario (Foco Evaluado):**  
   El `core-service` es completamente autosuficiente e independiente. Contiene la orquestación SAGA, la mensajería asíncrona JMS con ActiveMQ Artemis, la persistencia, las reglas transaccionales y su propia interfaz interactiva OpenAPI/Swagger (`http://localhost:8080/swagger-ui/index.html`). No requiere en lo absoluto de los BFF para su despliegue, ejecución ni evaluación.

2. **Rol de los BFFs como Arnés de Pruebas de Resiliencia en el Consumidor (Resilience4j + Eureka):**  
   Patrones fundamentales como `@CircuitBreaker`, `@Retry` y el balanceo `@LoadBalanced` de Spring Cloud actúan formalmente en el **lado del cliente/consumidor** de una llamada de red remota. Para validar empíricamente que una caída o saturación del Core dispara el Circuit Breaker y activa respuestas contingentes (`DEGRADADO_FALLBACK`), se mantuvieron los BFFs como clientes reales de prueba, permitiendo observar el comportamiento distribuido en vivo sin limitarse a pruebas unitarias con mocks.

3. **Seguridad Perimetral y Separación de Canales (Modelo Zero Trust):**  
   En la arquitectura bancaria, el Core nunca se expone directamente a los dispositivos finales. Los BFFs se diseñaron como la frontera perimetral (Edge) que absorbe la autenticación de usuarios finales (Móvil, Web, ATM) y se comunica con el Core exclusivamente mediante *Service Tokens* internos, protegiendo las entidades de dominio y aplicando reglas específicas de cada canal (múltiplos de giro en cajero, resúmenes optimizados en móvil).

> **Para la evaluación de esta entrega:** El docente puede interactuar y validar el 100% de los criterios exigidos (SAGA, JMS, Config Server, Eureka y Resiliencia) directamente sobre el `core-service`, considerando los BFFs como artefactos complementarios para demostración de llamadas distribuidas.

---

## 5. Matriz de Credenciales y Autenticación

| Componente | Usuario | Contraseña | Rol / Audiencia | Tipo de Autenticación |
| :--- | :---: | :---: | :---: | :--- |
| **Config Server** | `admin` | `gato` | `ROLE_ADMIN` | HTTP Basic Auth |
| **Discovery Server** | `eureka` | `eureka2026` | `ROLE_ADMIN` | HTTP Basic Auth |
| **BFF Móvil** | `usuario_movil` | `movil123` | `ROLE_MOVIL` / `aud: MOVIL` | JWT Bearer Token |
| **BFF Web** | `usuario_web` | `web123` | `ROLE_WEB` / `aud: WEB` | JWT Bearer Token |
| **BFF Cajero ATM** | `operador_atm` | `atm123` | `ROLE_ATM` / `aud: ATM` | JWT Bearer Token |
| **Core Bancario (Interno)**| `bff-*-client` | *Delegated Token* | `SERVICE_TOKEN` / `aud: core-bancario` | Service JWT firmado compartido |

---

## 6. Instrucciones de Compilación y Puesta en Marcha

### Prerrequisitos:
* **Java:** OpenJDK 21 (LTS) o superior.
* **Maven:** Incluido a través del wrapper `./mvnw` / `mvnw.cmd`.

### Compilación y Ejecución de Pruebas Automatizadas de SAGA y Resiliencia
Para compilar y correr las pruebas del ecosistema, ejecutar:
```bash
./mvnw -f core-service/pom.xml test
```
*Resultado:* Se ejecutan 6 pruebas automatizadas que validan:
1. Flujo completo SAGA exitoso con acreditación asíncrona JMS y actualización a estado `COMPLETADA`.
2. Flujo de compensación SAGA automático (Rollback semántico al fallar la cuenta de destino, restituyendo el 100% de los fondos a origen y pasando a estado `COMPENSADA`).
3. Rechazo atómico de operaciones sin saldo suficiente.
4. Carga y consistencia de datos contables legacy.

### Orden de Inicio de los Microservicios

#### A. Inicio Troncal Obligatorio (Evaluación del Ecosistema Core):
Solo se requieren estas 3 terminales para evaluar la totalidad de los criterios académicos (SAGA, JMS, Eureka, Config Server y Resiliencia):

1. **Terminal 1 - Iniciar Config Server (Puerto 8888):**
   ```bash
   ./mvnw -f config-server/pom.xml spring-boot:run
   ```

2. **Terminal 2 - Iniciar Discovery Server Eureka (Puerto 8761):**
   ```bash
   ./mvnw -f discovery-server/pom.xml spring-boot:run
   ```
   *Dashboard Eureka:* [http://localhost:8761](http://localhost:8761) (`eureka` / `eureka2026`)

3. **Terminal 3 - Iniciar Core Service con Broker JMS (Puerto 8080):**
   ```bash
   ./mvnw -f core-service/pom.xml spring-boot:run
   ```
   *Swagger UI directo del Core:* [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

#### B. Inicio de Clientes BFF (Opcional - Arnés de Pruebas de Canal):
4. **Terminal 4 - Iniciar BFF Móvil / Web / Cajero (Solo si se desean probar canales cliente externos):**
   ```bash
   ./mvnw -f ../bff/bff-movil/pom.xml spring-boot:run
   ```

---

## 7. Matriz de Endpoints y Verificación en Postman

### A. Endpoints del Core Bancario (Evaluación Directa y Principal)
| Servicio | Método | Endpoint | Descripción | Autenticación en Postman |
| :--- | :---: | :--- | :--- | :--- |
| **Config Server** | `GET` | `/core-service/default` | Obtener propiedades centralizadas del microservicio | Basic Auth (`admin`/`gato`) |
| **Discovery Server** | `GET` | `/eureka/apps` | Consultar microservicios registrados en Eureka | Basic Auth (`eureka`/`eureka2026`) |
| **Core Service** | `POST` | `/api/core/operaciones/transferencia` | Orquestar transferencia SAGA asíncrona JMS directa | Service Token (`aud: core-bancario`) |
| **Core Service** | `GET` | `/api/core/saga/transacciones` | Listar todas las transacciones SAGA y sus estados | Service Token (`aud: core-bancario`) |
| **Core Service** | `GET` | `/api/core/saga/transacciones/{sagaId}` | Consultar detalle de una transacción SAGA por ID | Service Token (`aud: core-bancario`) |
| **Core Service** | `GET` | `/api/core/saga/auditoria` | Listar eventos de auditoría emitidos por JMS | Service Token (`aud: core-bancario`) |
| **Core Actuator** | `GET` | `/actuator/health` | Estado de salud, Circuit Breakers y Broker JMS embebido | Sin autenticación |

### B. Endpoints de Canales BFF (Opcional - Consumo Perimetral)
| Canal | Método | Endpoint | Descripción | Autenticación en Postman |
| :--- | :---: | :--- | :--- | :--- |
| **BFF Móvil** | `POST` | `/api/auth/login` | Login y emisión de JWT para canal móvil | Body JSON (`usuario_movil` / `movil123`) |
| **BFF Móvil** | `POST` | `/api/v1/movil/cuentas/{id}/transferencia` | Iniciar transferencia electrónica delegada a SAGA | Bearer Token (`aud: MOVIL`) |

---

## 8. Verificación Paso a Paso del Patrón SAGA en Postman

### Prueba 1: Transferencia Exitosa (SAGA Happy Path)
1. **Petición:** `POST http://localhost:8080/api/core/operaciones/transferencia` (Directo al Core Service con Service Token) o vía `POST https://localhost:8443/api/v1/movil/cuentas/106/transferencia` (BFF Móvil).
2. **Body:**
   ```json
   {
     "cuentaDestinoId": 107,
     "monto": 10000,
     "comentario": "Transferencia SAGA exitosa"
   }
   ```
3. **Respuesta Esperada (`200 OK`):**
   ```json
   {
     "mensaje": "Transferencia iniciada exitosamente bajo arquitectura SAGA asíncrona JMS",
     "sagaId": "uuid-generado",
     "estado": "DEBITO_COMPLETADO",
     "cuentaOrigenId": 106,
     "cuentaDestinoId": 107,
     "monto": 10000
   }
   ```
4. **Verificación de Consistencia:** Consultar `GET /api/core/saga/transacciones/{sagaId}`. El estado habrá transitado asíncronamente a **`COMPLETADA`** tras ser procesado por el broker JMS.

### Prueba 2: Fallo en Destino y Compensación Automática (Rollback)
1. **Petición:** Transferir fondos hacia una cuenta destino no existente (ej: `999999`).
2. **Resultado:** 
   * Paso 1 debita transitoriamente la cuenta 106.
   * El consumidor JMS detecta el error en destino (`Cuenta destino no existe`).
   * Se dispara el evento en `banco.saga.compensaciones`.
   * El consumidor de compensación devuelve los fondos a la cuenta 106.
   * La transacción finaliza en estado **`COMPENSADA`**, manteniendo intacto el balance contable inicial del usuario.
3. **Verificación en Auditoría:** Consultar `GET /api/core/saga/auditoria` para inspeccionar la bitácora con `COMPENSACION_SOLICITADA` y `COMPENSACION_FINALIZADA`.

---

## 9. Documentación Swagger / OpenAPI y Monitoreo Actuator

* **Core Service (SAGA & Swagger UI):** `http://localhost:8080/swagger-ui/index.html`
* **BFF Móvil:** `https://localhost:8443/swagger-ui/index.html`
* **Actuator Health & Metrics:** `http://localhost:8080/actuator/health`, `https://localhost:8443/actuator/health`
