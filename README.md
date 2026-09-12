# Banco XYZ - Arquitectura Backend for Frontend (BFF) Distribuida y Segura
### Asignatura: Desarrollo Backend III (PBY2203) - Experiencia 2 / Semana 5
**Integrantes:** Leonardo Bustamante - Carolina Delgado  
**Repositorio GitHub:** [https://github.com/Starcrusher037/Banco-xyz-BFF.git](https://github.com/Starcrusher037/Banco-xyz-BFF.git)

---

## 1. Objetivo del Proyecto y Propuesta Técnica

### Objetivo del Proyecto
El objetivo principal es desacoplar el **Core Bancario Legacy** del Banco XYZ mediante la implementación del patrón de diseño arquitectónico **Backend for Frontend (BFF)**. Esta arquitectura resuelve la sobrecarga de transferencia de datos y la falta de especialización de un backend monolítico tradicional, proveyendo a cada canal de cliente un microservicio intermediario optimizado, altamente disponible, independiente y seguro.

### Propuesta Técnica
La solución distribuye la carga en **4 microservicios independientes** desarrollados con **Java 17** y **Spring Boot 3.2.5**, organizados bajo una estructura multi-módulo Maven:

1. **`core-service` (Puerto HTTPS `8080`):**
   - Microservicio central que gestiona el acceso a datos y lógica transaccional bancaria a partir de los datos históricos (`intereses.csv`, `transacciones.csv`, `cuentas_anuales.csv`).
   - Acceso privado perimetral protegido con cabecera interna `X-Internal-Api-Key`.
2. **`bff-movil` (Puerto HTTPS `8081`):**
   - Optimizado para redes móviles y baja latencia (Smartphones Android / iOS).
   - Provee respuestas ultraligeras (Top 3 transacciones, saldo express y transferencias simplificadas).
   - Autenticación JWT (`canal: MOVIL`) con autorización granular por cuenta (el usuario solo puede acceder a su propio `cuentaId`).
3. **`bff-web` (Puerto HTTPS `8082`):**
   - Diseñado para portales web bancarios y ejecutivos administrativos.
   - Provee agregación de datos complejos: cálculo de intereses proyectados, histórico completo y dashboards gerenciales consolidados.
   - Autenticación JWT (`canal: WEB`) con control de acceso basado en roles (RBAC: rol `ADMIN` para dashboards).
4. **`bff-cajero` (Puerto HTTPS `8083`):**
   - Especializado para la red de Cajeros Automáticos (ATM).
   - Aplica reglas de negocio críticas de hardware: giros en múltiplos de $5.000, tope máximo de $200.000 y cálculo de dispensación de billetes ($20k, $10k, $5k).
   - Autenticación de terminal mediante JWT (`canal: CAJERO_ATM`).

---

## 2. Estructura del Código Fuente

El proyecto utiliza una arquitectura multi-módulo Maven que garantiza el desacoplamiento, la modularidad y la escalabilidad del sistema:

```text
Exp2_S4_Grupo3/
├── pom.xml                                   # POM Padre Multi-Módulo (Gestión de dependencias y plugins)
├── core-service/                             # Microservicio Core Bancario Legacy (HTTPS 8080)
│   ├── src/main/resources/
│   │   ├── application.properties            # Configuración de puerto, SSL y API-Key
│   │   ├── keystore.p12                      # Certificado digital SSL/TLS PKCS12
│   │   └── data/ (*.csv)                     # Archivos de datos históricos legacy
│   └── src/main/java/cl/duoc/bancoxyz/core/
│       ├── controller/CoreController.java    # Endpoints privados protegidos con API-Key
│       ├── model/                            # Entidades de dominio (Cuenta, Transaccion, etc.)
│       ├── repository/                       # Acceso a datos y cargador de CSV
│       └── service/BancoService.java         # Lógica transaccional central
├── bff-movil/                                # Microservicio BFF Canal Móvil (HTTPS 8081)
│   ├── src/main/resources/keystore.p12       # Certificado digital SSL/TLS
│   └── src/main/java/cl/duoc/bancoxyz/bff/movil/
│       ├── client/CoreClient.java            # Consumo REST hacia Core con SSL
│       ├── controller/MovilController.java   # Endpoints ligeros con autorización por cuenta
│       ├── dto/                              # Payloads optimizados para apps móviles
│       ├── security/JwtUtil.java             # Generación y validación de tokens JWT
│       └── service/MovilBffService.java      # Lógica y filtros para móvil
├── bff-web/                                  # Microservicio BFF Canal Web (HTTPS 8082)
│   ├── src/main/resources/keystore.p12       # Certificado digital SSL/TLS
│   └── src/main/java/cl/duoc/bancoxyz/bff/web/
│       ├── client/CoreClient.java            # Consumo REST hacia Core con SSL
│       ├── controller/WebController.java     # Endpoints con RBAC (Roles ADMIN/EJECUTIVO)
│       ├── dto/                              # DTOs ricos y agregaciones para portal web
│       ├── security/JwtUtil.java             # Generación y validación de tokens JWT
│       └── service/WebBffService.java        # Cálculo de intereses y dashboards
└── bff-cajero/                               # Microservicio BFF Canal Cajero ATM (HTTPS 8083)
    ├── src/main/resources/keystore.p12       # Certificado digital SSL/TLS
    └── src/main/java/cl/duoc/bancoxyz/bff/cajero/
        ├── client/CoreClient.java            # Consumo REST hacia Core con SSL
        ├── controller/CajeroController.java  # Endpoints de dispensación y giros ATM
        ├── dto/                              # DTOs de saldo express y desglose de billetes
        ├── security/JwtUtil.java             # Generación y validación de tokens de terminal
        └── service/CajeroBffService.java     # Reglas de negocio de dispensación de efectivo
```

---

## 3. Instrucciones de Compilación y Ejecución

### Requisitos Previos
- **Java Development Kit (JDK):** Versión 17 o superior instalada.
- **Maven:** No requiere instalación global (incluye Maven Wrapper `./mvnw`).

### Paso 1: Compilar el Proyecto Multi-Módulo
En la raíz del proyecto, ejecuta:
```bash
./mvnw clean compile
```

### Paso 2: Ejecutar los 4 Microservicios
Abre **4 terminales independientes** para levantar cada servicio en su respectivo puerto HTTPS:

* **Terminal 1 - Core Bancario (HTTPS 8080):**
  ```bash
  ./mvnw spring-boot:run -pl core-service
  ```
* **Terminal 2 - BFF Móvil (HTTPS 8081):**
  ```bash
  ./mvnw spring-boot:run -pl bff-movil
  ```
* **Terminal 3 - BFF Web (HTTPS 8082):**
  ```bash
  ./mvnw spring-boot:run -pl bff-web
  ```
* **Terminal 4 - BFF Cajero ATM (HTTPS 8083):**
  ```bash
  ./mvnw spring-boot:run -pl bff-cajero
  ```

---

## 4. Medidas de Seguridad y Flujo de Autenticación / Autorización

### Seguridad Implementada
1. **Cifrado en Tránsito (HTTPS / TLS):** Todos los microservicios utilizan certificados digitales PKCS12 (`keystore.p12`) configurados en `server.ssl.*`.
2. **Autenticación Basada en Tokens JWT:** Criptográficamente firmados con HMAC-SHA256, con expiración configurada y claims personalizados.
3. **Autorización en 3 Niveles:**
   - **Nivel 1 (Aislamiento de Canal):** Los tokens generados para un canal no pueden ser utilizados en otro (Móvil rechaza tokens Web y viceversa).
   - **Nivel 2 (Autorización Granular):**
     - *Web:* Control RBAC (`ADMIN` requerido para dashboards globales, `EJECUTIVO` para consultas regulares).
     - *Móvil:* Validación de propiedad de cuenta (`cuentaId` en JWT debe ser idéntico al recurso consultado).
   - **Nivel 3 (Seguridad Servicio a Servicio):** El Core solo acepta peticiones autenticadas con la clave interna `X-Internal-Api-Key`.

---

## 5. Guía de Pruebas de Endpoints y Verificación

> **Nota:** Debido al uso de certificados autofirmados en entorno local, utiliza el parámetro `-k` (o `--insecure`) en `curl`.

### A. Canal Móvil (`https://localhost:8081`)

1. **Autenticación (Login):**
   ```bash
   curl -k -X POST "https://localhost:8081/api/v1/movil/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username": "carlos_movil", "cuentaId": 101}'
   ```
2. **Consulta Autorizada de Cuenta Propia (HTTP 200 OK):**
   ```bash
   curl -k -X GET "https://localhost:8081/api/v1/movil/cuentas/101" \
        -H "Authorization: Bearer <TOKEN_OBTENIDO>"
   ```
3. **Demostración de Autorización Denegada (Consulta de Cuenta Ajena - HTTP 403 Forbidden):**
   ```bash
   curl -k -X GET "https://localhost:8081/api/v1/movil/cuentas/102" \
        -H "Authorization: Bearer <TOKEN_OBTENIDO>"
   ```

---

### B. Canal Web (`https://localhost:8082`)

1. **Autenticación como Administrador:**
   ```bash
   curl -k -X POST "https://localhost:8082/api/v1/web/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username": "admin_web", "rol": "ADMIN"}'
   ```
2. **Consulta de Dashboard Global con Rol ADMIN (HTTP 200 OK):**
   ```bash
   curl -k -X GET "https://localhost:8082/api/v1/web/dashboard" \
        -H "Authorization: Bearer <TOKEN_ADMIN>"
   ```
3. **Demostración de Autorización Denegada (Dashboard con Rol EJECUTIVO - HTTP 403 Forbidden):**
   ```bash
   # Login como ejecutivo
   curl -k -X POST "https://localhost:8082/api/v1/web/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username": "ejecutivo_juan", "rol": "EJECUTIVO"}'
   
   # Intentar ingresar al dashboard administrativo
   curl -k -X GET "https://localhost:8082/api/v1/web/dashboard" \
        -H "Authorization: Bearer <TOKEN_EJECUTIVO>"
   ```

---

### C. Canal Cajero ATM (`https://localhost:8083`)

1. **Autenticar Terminal ATM:**
   ```bash
   curl -k -X POST "https://localhost:8083/api/v1/cajero/auth/token" \
        -H "Content-Type: application/json" \
        -d '{"terminalId": "ATM-SCL-01"}'
   ```
2. **Procesar Retiro de Dinero en Efectivo:**
   ```bash
   curl -k -X POST "https://localhost:8083/api/v1/cajero/cuentas/101/retiro" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer <TOKEN_TERMINAL>" \
        -d '{"monto": 35000, "pin": "1234", "terminalId": "ATM-SCL-01"}'
   ```

---

## 6. Documentación Interactiva Swagger UI (HTTPS)

Cada microservicio dispone de su propia consola OpenAPI Swagger interactiva:
* **Core Bancario API:** [https://localhost:8080/swagger-ui.html](https://localhost:8080/swagger-ui.html)
* **BFF Móvil:** [https://localhost:8081/swagger-ui.html](https://localhost:8081/swagger-ui.html)
* **BFF Web:** [https://localhost:8082/swagger-ui.html](https://localhost:8082/swagger-ui.html)
* **BFF Cajero ATM:** [https://localhost:8083/swagger-ui.html](https://localhost:8083/swagger-ui.html)
