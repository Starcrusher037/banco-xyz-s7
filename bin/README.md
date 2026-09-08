# Banco XYZ - Arquitectura Backend for Frontend (BFF)
### Asignatura: Desarrollo Backend III (PBY2203) - Experiencia 2 / Semana 4
**Integrantes:** Leonardo Bustamante - Carolina Delgado  
**Repositorio GitHub:** [https://github.com/Lybern/Exp2_S4_Grupo3](https://github.com/Lybern/Exp2_S4_Grupo3)

---

## 1. Descripción del Proyecto
Este proyecto implementa el patrón arquitectónico **Backend for Frontend (BFF)** para modernizar y optimizar la interacción entre los diferentes canales de atención del **Banco XYZ** y sus sistemas centrales de datos legacy (basado en el repositorio [bank_legacy_data](https://github.com/KariVillagran/bank_legacy_data)).

El objetivo principal es eliminar el problema del *backend monolítico generalizado* (que enviaba la misma respuesta pesada e indiferenciada a todos los clientes), creando interfaces y contratos de datos personalizados para cada tipo de cliente:
1. **BFF Móvil (`/api/v1/movil`):** Optimizado para smartphones con respuestas compactas (*payload* ligero) para minimizar el consumo de datos y mejorar la velocidad de carga.
2. **BFF Web (`/api/v1/web`):** Proporciona información exhaustiva, dashboards administrativos, cálculo de intereses y desgloses anuales para pantallas de escritorio.
3. **BFF Cajeros Automáticos (`/api/v1/cajero`):** Interfaz sumamente segura y orientada a operaciones transaccionales críticas (consulta express, retiros en múltiplos de $5.000 y validación de hardware/terminal).

---

## 2. Análisis y Fundamentación de la Estrategia BFF Elegida

### Estrategia Implementada: **Monolito Modular con Capa BFF Desacoplada (Endpoints y Controladores Especializados)**

En concordancia con lo analizado en la Guía de Aprendizaje y las instrucciones docentes:
* **¿Por qué esta estrategia?**
  1. **Aislamiento de DTOs y Contratos:** Cada cliente (`movil`, `web`, `cajero`) dispone de sus propios Data Transfer Objects (DTOs), servicios y controladores, garantizando que los cambios en la UI de una app móvil no afecten a la banca web ni a los cajeros.
  2. **Eficiencia de Recursos y Ejecución:** Permite unificar el procesamiento de los archivos legacy (`intereses.csv`, `transacciones.csv`, `cuentas_anuales.csv`) en un único motor concurrente en memoria, reduciendo la sobrecarga de múltiples servidores independientes.
  3. **Seguridad y Auditoría por Canal:** Permite aplicar políticas de seguridad diferenciadas mediante interceptores HTTP (`X-Channel-Security`), validando tokens móviles, sesiones web y terminales ATM de forma especializada.

---

## 3. Matriz Comparativa de Canales BFF

| Característica | 📱 BFF Móvil | 🌐 BFF Web | 🏧 BFF Cajero Automático |
| :--- | :--- | :--- | :--- |
| **Ruta Base** | `/api/v1/movil` | `/api/v1/web` | `/api/v1/cajero` |
| **Audiencia** | Smartphones (iOS / Android) | Navegadores PC / Portal Ejecutivo | Terminales Físicos ATM / Tótems |
| **Tamaño de Payload** | **Ultraligero** (Top 3 movimientos) | **Completo** (Historial + Dashboard) | **Mínimo Transaccional** |
| **Operaciones Clave** | Resumen, Saldo rápido, Transferencia | Auditoría, Desglose anual, Métricas | Giro en efectivo, Consulta, Depósito |

---

## 4. Estructura del Código Fuente

```text
src/main/java/cl/duoc/bancoxyz/
├── BancoBffApplication.java              # Clase principal Spring Boot
├── config/
│   ├── OpenApiConfig.java               # Configuración Swagger / OpenAPI 3
│   ├── SeguridadCanalesInterceptor.java # Interceptor de seguridad por canal
│   └── WebMvcConfig.java                # Registro de interceptores y CORS
├── model/                               # Entidades de dominio bancario
│   ├── Cuenta.java
│   ├── Transaccion.java
│   └── MovimientoAnual.java
├── repository/                          # Capa de persistencia y carga legacy
│   ├── BancoRepository.java             # Almacén concurrente thread-safe
│   └── CargadorDatosLegacy.java         # Lector de intereses, transacciones y cuentas anuales CSV
├── service/
│   └── BancoService.java                # Lógica de negocio bancaria central (retiros, depósitos)
├── exception/                           # Manejador global de excepciones
│   ├── ErrorRespuestaDto.java
│   └── ManejadorExcepcionesGlobal.java
└── bff/                                 # Capas Backend for Frontend especializadas
    ├── movil/                           # 📱 MÓDULO MÓVIL
    │   ├── controller/MovilController.java
    │   ├── service/MovilBffService.java
    │   └── dto/ (ResumenCuentaMovilDto, TransaccionMovilDto, SolicitudTransferenciaMovilDto)
    ├── web/                             # 🌐 MÓDULO WEB
    │   ├── controller/WebController.java
    │   ├── service/WebBffService.java
    │   └── dto/ (DetalleCuentaWebDto, TransaccionWebDto, DashboardWebDto)
    └── cajero/                          # 🏧 MÓDULO CAJERO ATM
        ├── controller/CajeroController.java
        ├── service/CajeroBffService.java
        └── dto/ (ConsultaSaldoCajeroDto, SolicitudRetiroCajeroDto, RespuestaRetiroDto)
```

---

## 5. Instrucciones de Compilación y Ejecución

### Requisitos Previos:
* Java JDK 17 o 21+ instalado.
* Git.

### Pasos para Ejecutar:
1. Clonar el repositorio:
   ```bash
   git clone https://github.com/Lybern/Exp2_S4_Grupo3.git
   cd Exp2_S4_Grupo3
   ```
2. Compilar el proyecto con el Maven Wrapper incluido:
   ```bash
   ./mvnw clean compile
   ```
3. Iniciar la aplicación:
   ```bash
   ./mvnw spring-boot:run
   ```
4. El servidor iniciará en el puerto **`8080`**.

---

## 6. Documentación Interactiva de la API (Swagger UI)

Una vez iniciado el servidor, accede a la interfaz gráfica interactiva de Swagger UI en tu navegador:

🔗 **URL Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)  
🔗 **OpenAPI Docs (JSON):** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 7. Ejemplos de Pruebas y Consumo de Endpoints

### A. Canal Móvil (BFF Móvil)
* **Obtener Resumen Ligero de Cuenta:**
  ```bash
  curl -X GET "http://localhost:8080/api/v1/movil/cuentas/101"
  ```
* **Consulta Rápida de Saldo:**
  ```bash
  curl -X GET "http://localhost:8080/api/v1/movil/cuentas/101/saldo"
  ```
* **Transferencia Rápida:**
  ```bash
  curl -X POST "http://localhost:8080/api/v1/movil/cuentas/101/transferencia" \
       -H "Content-Type: application/json" \
       -d '{"cuentaDestinoId": 102, "monto": 25000, "comentario": "Pago almuerzo"}'
  ```

### B. Canal Web (BFF Web)
* **Obtener Detalle Completo con Intereses e Historial Anual:**
  ```bash
  curl -X GET "http://localhost:8080/api/v1/web/cuentas/101"
  ```
* **Listado de Todas las Transacciones Históricas:**
  ```bash
  curl -X GET "http://localhost:8080/api/v1/web/cuentas/101/transacciones"
  ```
* **Dashboard Global de Administración:**
  ```bash
  curl -X GET "http://localhost:8080/api/v1/web/dashboard"
  ```

### C. Canal Cajero Automático (BFF ATM)
* **Consulta de Saldo en Cajero:**
  ```bash
  curl -X GET "http://localhost:8080/api/v1/cajero/cuentas/101/saldo?terminalId=ATM-SCL-01"
  ```
* **Retiro de Efectivo (Giro en múltiplos de $5.000 con PIN de 4 dígitos):**
  ```bash
  curl -X POST "http://localhost:8080/api/v1/cajero/cuentas/101/retiro" \
       -H "Content-Type: application/json" \
       -d '{"monto": 40000, "pin": "1234", "terminalId": "ATM-SCL-01"}'
  ```
