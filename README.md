<p align="center">
  <img src="docs/assets/fitzone.svg" alt="Icono de FitZone" width="112">
</p>

<h1 align="center">API de FitZone</h1>

<p align="center">Inscripción a clases grupales y avisos para socios.</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=flat" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring_Boot-4.1.1-6DB33F?style=flat" alt="Spring Boot 4.1.1">
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=flat" alt="MySQL">
  <img src="https://img.shields.io/badge/Postman-FF6C37?style=flat" alt="Postman">
</p>

## Resumen

La API registra socios, sucursales y clases grupales. Valida membresía vigente, cupo máximo de veinte personas e inscripciones duplicadas.

1. El socio elige una clase.
2. Se verifica la vigencia de su membresía.
3. Se consulta el cupo disponible.
4. Se registra la inscripción.
5. Se confirma el lugar y se envía un recordatorio dos horas antes.

La confirmación y el recordatorio se registran mediante logger. Si el socio se inscribe cuando faltan menos de dos horas, recibe el recordatorio al confirmar. La tarea revisa avisos pendientes cada cinco segundos.

## Estructura del proyecto

```text
src/main/java/com/universidad/fitzone/
├── FitzoneApplication.java            # Arranque de la aplicación
├── controllers/
│   ├── ClaseController.java           # Rutas de clase
│   ├── InscripcionController.java     # Rutas de inscripción
│   ├── SocioController.java           # Rutas de socio
│   └── SucursalController.java        # Rutas de sucursal
├── models/
│   ├── Clase.java                     # Datos de clase
│   ├── Inscripcion.java               # Datos de inscripción
│   ├── InscripcionRegistrada.java     # Evento de inscripción
│   ├── Socio.java                     # Datos de socio
│   └── Sucursal.java                  # Datos de sucursal
├── repositories/
│   ├── ClaseRepository.java           # Consultas de clase
│   ├── InscripcionRepository.java     # Consultas de inscripciones
│   ├── SocioRepository.java           # Consultas de socio
│   └── SucursalRepository.java        # Consultas de sucursal
└── services/
    ├── ClaseService.java              # Operaciones de clase
    ├── InscripcionListener.java       # Confirmación de inscripción
    ├── InscripcionService.java        # Registro y consulta de inscripciones
    ├── NotificacionService.java       # Avisos por consola
    ├── RecordatorioScheduler.java     # Revisión de recordatorios
    ├── SocioService.java              # Operaciones de socio
    └── SucursalService.java           # Operaciones de sucursal
```

Los controladores reciben las peticiones, los servicios aplican las reglas de negocio y los repositorios acceden a MySQL. Los modelos representan los datos y eventos de cada proceso.

## Instalación y ejecución

Requiere JDK 21 y MySQL en ejecución.

Crear la base desde MySQL Workbench y configurar las credenciales en un archivo `.env`, tomando como referencia `.env.example`.

```sql
CREATE DATABASE IF NOT EXISTS fitzone CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS fitzone_pruebas CHARACTER SET utf8mb4;
```

| Variable | Uso |
|---|---|
| `DB_URL` | Dirección de la base de datos. |
| `DB_USERNAME` | Usuario de MySQL. |
| `DB_PASSWORD` | Contraseña local de MySQL. |
| `TEST_DB_URL` | Base separada para las verificaciones de integración. |

Ejecutar `FitzoneApplication.java` con **Run Java**.

**URL base:** `http://localhost:8081/api/v1`.

## Endpoints

Las rutas de la tabla parten de `/api/v1`. Los registros reciben JSON.

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| GET | `/clases` | Listar clases | 200 OK |
| GET | `/clases/{id}` | Consultar clase | 200 OK |
| POST | `/clases` | Registrar clase | 201 Created |
| GET | `/clases/{id}/cupo` | Consultar cupo | 200 OK |
| GET | `/inscripciones` | Listar inscripciones | 200 OK |
| GET | `/inscripciones/{id}` | Consultar inscripción | 200 OK |
| POST | `/inscripciones` | Registrar inscripción | 201 Created |
| GET | `/socios` | Listar socios | 200 OK |
| GET | `/socios/{id}` | Consultar socio | 200 OK |
| POST | `/socios` | Registrar socio | 201 Created |
| GET | `/socios/{id}/membresia` | Consultar membresía | 200 OK |
| GET | `/sucursales` | Listar sucursales | 200 OK |
| GET | `/sucursales/{id}` | Consultar sucursal | 200 OK |
| POST | `/sucursales` | Registrar sucursal | 201 Created |

## Postman

Importar la [colección de Postman](postman/FitZone.postman_collection.json) y ejecutar las peticiones en orden. Las [instrucciones de uso](postman/README.md) explican cómo realizar las pruebas en Postman.

### Ejemplos de peticiones

Usar los IDs obtenidos en las respuestas. Para registrar una clase, indicar una fecha futura.

**POST `/sucursales`**

```json
{
  "nombre": "Centro",
  "direccion": "Veracruz Centro"
}
```

**POST `/socios`**

```json
{
  "nombre": "Emilio Alejandro Aguirre Tronco",
  "correo": "202360079@ucc.mx",
  "vigenciaMembresia": "2026-12-31"
}
```

**POST `/clases`**

```json
{
  "sucursal": {
    "id": 1
  },
  "tipo": "YOGA",
  "inicio": "2026-10-24T18:00:00",
  "cupo": 20
}
```

**POST `/inscripciones`**

```json
{
  "socio": {
    "id": 1
  },
  "clase": {
    "id": 1
  }
}
```

### Confirmación y recordatorio

Consultar `GET /inscripciones/{id}` para revisar `confirmacionEnviada` y `recordatorioEnviado`. Los avisos se registran en consola y se conservan sus indicadores en la inscripción.

## Manejo de errores

Los servicios validan los datos y el estado antes de guardar. Los rechazos incluyen el código HTTP y el motivo en `message`.

| Código HTTP | Motivo |
|---|---|
| `400 Bad Request` | Datos obligatorios ausentes o valores inválidos. |
| `403 Forbidden` | Membresía vencida. |
| `404 Not Found` | Socio, sucursal, clase o inscripción inexistentes. |
| `409 Conflict` | Inscripción duplicada, clase iniciada o sin cupo. |
