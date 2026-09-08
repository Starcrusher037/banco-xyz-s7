# Banco XYZ - Arquitectura Backend for Frontend (BFF) Distribuida
### Asignatura: Desarrollo Backend III (PBY2203) - Experiencia 2 / Semana 4
**Integrantes:** Leonardo Bustamante - Carolina Delgado  
**Repositorio GitHub:** [https://github.com/Lybern/Exp2_S4_Grupo3](https://github.com/Lybern/Exp2_S4_Grupo3)

---

## 1. Descripción de la Solución
Este proyecto implementa el patrón arquitectónico **Backend for Frontend (BFF)** desacoplando las responsabilidades en una arquitectura multi-servicio independiente, donde cada canal de cliente posee su propio backend especializado y se comunica con el **Core Bancario Legacy**.

### Arquitectura de Microservicios Implementada:
1. **`core-service` (Puerto `8080`):** 
   - Core central y carga de datos legacy (`intereses.csv`, `transacciones.csv`, `cuentas_anuales.csv`).
   - Protegido mediante clave interna `X-Internal-Api-Key` para llamadas privadas de servicio a servicio.
2. **`bff-movil` (Puerto `8081`):** 
   - Backend exclusivo para Smartphones (Android / iOS).
   - Respuestas ultraligeras (Top 3 transacciones, saldo rápido, transferencias).
   - Autenticación mediante **JWT** (`ROLE: MOVIL`).
3. **`bff-web` (Puerto `8082`):** 
   - Backend para navegadores web y portales administrativos.
   - Respuestas ricas: dashboards gerenciales, cálculo de intereses y desgloses anuales.
   - Autenticación mediante **JWT** (`ROLE: WEB`).
4. **`bff-cajero` (Puerto `8083`):** 
   - Backend para Cajeros Automáticos (ATM).
   - Operaciones críticas: giros en múltiplos de $5.000, validación de PIN y topes de dispensación ($200.000).
   - Autenticación de terminal mediante **JWT** (`ROLE: CAJERO_ATM`).

---

## 2. Estructura Multi-Módulo Maven

```text
Exp2_S4_Grupo3/
├── pom.xml                               # Parent POM Multi-Módulo
├── core-service/                         # Puerto 8080: Core Bancario & Datos Legacy
│   └── src/main/java/cl/duoc/bancoxyz/core/
│       ├── controller/CoreController.java
│       ├── repository/ (BancoRepository, CargadorDatosLegacy)
│       └── service/BancoService.java
├── bff-movil/                            # Puerto 8081: BFF Móvil
│   └── src/main/java/cl/duoc/bancoxyz/bff/movil/
│       ├── client/CoreClient.java
│       ├── controller/MovilController.java
│       └── security/JwtUtil.java
├── bff-web/                              # Puerto 8082: BFF Web
│   └── src/main/java/cl/duoc/bancoxyz/bff/web/
│       ├── client/CoreClient.java
│       ├── controller/WebController.java
│       └── security/JwtUtil.java
└── bff-cajero/                           # Puerto 8083: BFF Cajeros ATM
    └── src/main/java/cl/duoc/bancoxyz/bff/cajero/
        ├── client/CoreClient.java
        ├── controller/CajeroController.java
        └── security/JwtUtil.java
```

---

## 3. Instrucciones de Compilación y Ejecución

### Compilar Todo el Proyecto:
```bash
./mvnw clean compile
```

### Ejecución de los Servicios:
Abre 4 terminales independientes para levantar cada servicio:

* **Terminal 1 (Core Bancario):**
  ```bash
  ./mvnw spring-boot:run -pl core-service
  ```
* **Terminal 2 (BFF Móvil):**
  ```bash
  ./mvnw spring-boot:run -pl bff-movil
  ```
* **Terminal 3 (BFF Web):**
  ```bash
  ./mvnw spring-boot:run -pl bff-web
  ```
* **Terminal 4 (BFF Cajero ATM):**
  ```bash
  ./mvnw spring-boot:run -pl bff-cajero
  ```

---

## 4. Pruebas de Endpoints y Autenticación JWT

### A. Canal Móvil (`http://localhost:8081`)
1. **Obtener Token JWT:**
   ```bash
   curl -X POST "http://localhost:8081/api/v1/movil/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username": "carlos_movil", "cuentaId": 101}'
   ```
2. **Consultar Resumen de Cuenta con Token:**
   ```bash
   curl -X GET "http://localhost:8081/api/v1/movil/cuentas/101" \
        -H "Authorization: Bearer <TOKEN_OBTENIDO>"
   ```

### B. Canal Web (`http://localhost:8082`)
1. **Obtener Token JWT:**
   ```bash
   curl -X POST "http://localhost:8082/api/v1/web/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username": "admin_web", "rol": "ADMIN"}'
   ```
2. **Consultar Dashboard Global con Token:**
   ```bash
   curl -X GET "http://localhost:8082/api/v1/web/dashboard" \
        -H "Authorization: Bearer <TOKEN_OBTENIDO>"
   ```

### C. Canal Cajero ATM (`http://localhost:8083`)
1. **Obtener Token de Terminal:**
   ```bash
   curl -X POST "http://localhost:8083/api/v1/cajero/auth/token" \
        -H "Content-Type: application/json" \
        -d '{"terminalId": "ATM-SCL-01"}'
   ```
2. **Realizar Retiro de Efectivo:**
   ```bash
   curl -X POST "http://localhost:8083/api/v1/cajero/cuentas/101/retiro" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer <TOKEN_OBTENIDO>" \
        -d '{"monto": 40000, "pin": "1234", "terminalId": "ATM-SCL-01"}'
   ```

---

## 5. Documentación Swagger UI Interactiva
Cada microservicio cuenta con su propia documentación Swagger UI independiente:
* **Core API:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **BFF Móvil:** [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
* **BFF Web:** [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html)
* **BFF Cajero ATM:** [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html)
