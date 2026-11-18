# TP10/TP11 – API REST de Ventas (Spring Boot)

API RESTful para gestionar **clientes**, **productos** y **ventas** (contado / financiada con plan de cuotas),
consultar el **estado de cuenta** de un cliente y **cobrar cuotas**. Documentación interactiva con **Swagger**.

## Requisitos
- Java 17+, Maven 3.9+ y MySQL 8 (la base `tp10_ventas` se crea sola).
- Ajustar usuario/clave en `src/main/resources/application.properties`.

```bash
mvn spring-boot:run      # levanta en http://localhost:8080
```

## Documentación interactiva (Swagger)
Con la aplicación levantada:

| | URL |
|---|---|
| Swagger UI (navegar, inspeccionar y **probar** todos los endpoints) | http://localhost:8080/swagger-ui.html |
| Especificación OpenAPI (JSON) | http://localhost:8080/v3/api-docs |

Implementado con `springdoc-openapi-starter-webmvc-ui`; los controladores están anotados con `@Tag`, `@Operation` y `@ApiResponse`,
y la configuración general está en `config/OpenApiConfig` y en `application.properties` (`springdoc.*`).

> **Base de datos:** el TP11 reemplaza la columna booleana `cuotas.pagada` por `cuotas.estado` y agrega el estado `SALDADA` a las ventas.
> Con una base creada por el TP10 hay que borrarla (`DROP DATABASE tp10_ventas;`) para que se regenere el esquema.

## Arquitectura
```
controller ──► IClienteService / IProductoService / IVentaService / ICuotaService   (interfaces)
                      │ implementadas en service.impl
                      ▼
 VentaServiceImpl (orquestador del registro de ventas)
   ├─ IClienteValidador   → existe / está habilitado
   ├─ IStockService       → comprueba, descuenta y repone stock
   └─ IPoliticaPago       → Strategy por modalidad de pago
        ├─ PoliticaContado     → VentaContado    (descuento, sin cuotas)
        └─ PoliticaFinanciada  → VentaFinanciada (recargo + IPlanCuotasService)
repository ► model (herencia JPA: Venta ← VentaContado / VentaFinanciada)
dto.request / dto.response (record)   mapper   exception (+ GlobalExceptionHandler)
```
- **Interfaces:** todos los servicios y los módulos internos tienen interfaz; los controladores dependen solo de interfaces.
- **Dos tipos de venta:** `Venta` es abstracta y cada modalidad es una subclase con sus datos y reglas
  (`VentaContado`: porcentaje y monto de descuento; `VentaFinanciada`: tasa y monto de recargo, cantidad de cuotas y plan de cuotas).
  La respuesta también es distinta (`VentaContadoResponse` / `VentaFinanciadaResponse`).
- **Registro de ventas modularizado:** `VentaServiceImpl` solo coordina; stock, validación de cliente, política de pago y plan de cuotas son módulos separados e intercambiables.
  Para agregar una modalidad nueva basta una nueva `IPoliticaPago` (más su subclase de `Venta`).
- Los controladores **solo** reciben `*Request` y devuelven `*Response`; nunca entidades.
- `GlobalExceptionHandler` (`@RestControllerAdvice`) mapea las excepciones a HTTP con un JSON único:
  `{ timestamp, status, error, mensaje, ruta, detalles }`.

| Excepción | HTTP |
|---|---|
| Validación `@Valid`, JSON ilegible, parámetro inválido/faltante, `SolicitudInvalidaException` | 400 |
| `RecursoNoEncontradoException` | 404 |
| `StockInsuficienteException`, `DatoDuplicadoException`, `OperacionNoPermitidaException` | 409 |
| `ClienteInhabilitadoException`, `ImporteInvalidoException` | 422 |
| Cualquier otra | 500 |

## Estado de cuenta y cobro de cuotas (TP11)

**Estados de una cuota** (`EstadoCuota`):

| Estado | Significado |
|---|---|
| `ADEUDADA` | Pendiente. Puede abonarse hasta su fecha de vencimiento (inclusive) por su monto original. |
| `VENCIDA` | Pendiente y con la fecha de vencimiento ya superada. Pasa a este estado **automáticamente** cuando se consulta el estado de cuenta o cuando un cliente se presenta a pagar. |
| `PAGADA` | Ya fue abonada (se guarda la `fechaPago`). |

**Estado general de la venta** (`EstadoVenta`): `ACTIVA`, `SALDADA` (venta financiada sin cuotas pendientes) y `ANULADA`.

**Cobro** — `POST /api/ventas/{ventaId}/cobrar-cuota`:
- Recibe el identificador de la **venta** y cobra su cuota correlativa pendiente (la primera impaga, por número).
  Como siempre se cobra la más antigua, **no se pueden saltear cuotas ni cobrar vencimientos futuros** existiendo saldos impagos anteriores.
- Si la cuota ya había vencido, pasa a `VENCIDA` antes de cobrarse y la respuesta indica `pagoFueraDeTermino: true`. Se cobra por su monto original (el enunciado no define recargos por mora).
- Al cobrar la última cuota adeudada la venta verifica que no queden compromisos pendientes y pasa automáticamente a `SALDADA`.
- La venta se lee con bloqueo pesimista (`VentaRepository.buscarConBloqueo`): dos cobros simultáneos no pueden pagar la misma cuota.
- `409` si la venta es de contado, está anulada o ya está saldada; `404` si no existe.

**Estado de cuenta** — `GET /api/ventas/cliente/{clienteId}/estado-cuenta`: datos del cliente, historial de ventas
(contado y financiadas, con su estado), cuotas pendientes con su estado (`ADEUDADA`/`VENCIDA`), cantidad de cuotas vencidas, y totales pendiente y vencido.

El "hoy" lo da un `Clock` inyectable (`config/ClockConfig`), por eso los vencimientos se prueban fijando la fecha en los tests.

## Reglas de negocio de la venta
Configurables en `application.properties` (`ventas.contado.descuento`, `ventas.financiada.recargo`, `ventas.financiada.cuotas-por-defecto`).

| | CONTADO | FINANCIADA |
|---|---|---|
| Ajuste al subtotal | 10 % de descuento | 15 % de recargo |
| Cuotas | No admite `cantidadCuotas` (400 si se envía) | Plan mensual; `cantidadCuotas` opcional (6 por defecto, máx. 24); la última cuota absorbe el redondeo |
| Respuesta | `porcentajeDescuento`, `descuento` | `tasaRecargo`, `recargo`, `cantidadCuotas`, `cuotas` |

Comunes: cliente inexistente → 404, inhabilitado → 422; stock insuficiente → 409; importe total ≤ 0 → 422.
Si algo falla se revierte todo (`@Transactional`). Anular una venta repone el stock.

## Pruebas unitarias
```bash
mvn test
```
- **Servicios (JUnit 5 + Mockito):** `PoliticaContadoTest`, `PoliticaFinanciadaTest`, `PlanCuotasServiceImplTest`, `StockServiceImplTest`, `VentaServiceImplTest`, `CuotaServiceImplTest` (estado de cuenta, cobro correlativo, vencimientos y saldado de la venta), `ClienteServiceImplTest`, `ProductoServiceImplTest`.
- **Capa web (`@WebMvcTest` + MockMvc):** `VentaControllerTest`, `ClienteControllerTest`, `ProductoControllerTest` verifican códigos HTTP, JSON de respuesta y el mapeo de excepciones.
- Los tests no necesitan MySQL.

## Endpoints

### Clientes `/api/clientes`
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/clientes` | Crear |
| PUT | `/api/clientes/{id}` | Modificar |
| DELETE | `/api/clientes/{id}` | Baja lógica (inhabilita) |
| GET | `/api/clientes` , `/{id}` | Listar / obtener |
| GET | `/api/clientes/documento/{dniCuit}` | Buscar por DNI/CUIT |
| GET | `/api/clientes/buscar?nombre=` | Buscar por nombre o razón social |

### Productos `/api/productos`
| Método | Ruta | Descripción |
|---|---|---|
| POST / PUT / DELETE | `/api/productos[/{id}]` | Crear / modificar / baja lógica |
| GET | `/api/productos` , `/{id}` | Listar / obtener |
| GET | `/api/productos/filtrar?categoria=&marca=&precioMin=&precioMax=` | Filtros combinables |
| GET | `/api/productos/stock-critico[?umbral=5]` | Stock ≤ stock mínimo del producto, o ≤ umbral |

### Ventas `/api/ventas`
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/ventas` | Registrar venta (`modalidadPago`: CONTADO o FINANCIADA) |
| DELETE | `/api/ventas/{id}` | Anular venta (repone stock) |
| GET | `/api/ventas` , `/{id}` | Listar / obtener |
| GET | `/api/ventas/cliente/{clienteId}` | Historial del cliente |
| GET | `/api/ventas/cliente/{clienteId}/estado-cuenta` | Estado de cuenta: cliente, historial de ventas y cuotas pendientes/vencidas |
| GET | `/api/ventas/periodo?desde=2026-01-01&hasta=2026-12-31` | Ventas por rango de fechas |
| GET | `/api/ventas/modalidad/{CONTADO\|FINANCIADA}` | Filtrar por modalidad |
| POST | `/api/ventas/{ventaId}/cobrar-cuota` | Cobrar la próxima cuota pendiente de la venta (la pasa a `SALDADA` al cobrar la última) |

### Ejemplos
```bash
curl -X POST localhost:8080/api/clientes -H "Content-Type: application/json" \
  -d '{"dniCuit":"40123456","nombre":"Ana Pérez","email":"ana@mail.com","telefono":"3884000000"}'

curl -X POST localhost:8080/api/productos -H "Content-Type: application/json" \
  -d '{"nombre":"Notebook 15","categoria":"Notebooks","marca":"Lenovo","precio":850000,"stock":10,"stockMinimo":3}'

curl -X POST localhost:8080/api/ventas -H "Content-Type: application/json" \
  -d '{"clienteId":1,"modalidadPago":"FINANCIADA","cantidadCuotas":6,"items":[{"productoId":1,"cantidad":2}]}'

curl localhost:8080/api/ventas/cliente/1/estado-cuenta
curl -X POST localhost:8080/api/ventas/1/cobrar-cuota
```

## Flujo de trabajo con Git (obligatorio)
```bash
git init && git branch -M main
git remote add origin https://github.com/<usuario>/tp10-ventas-api.git
git add . && git commit -m "chore: proyecto base Spring Boot" && git push -u origin main

# cada integrante trabaja en su rama de característica
git checkout -b feature/clientes-controller
git commit -am "feat: endpoints CRUD de clientes"
git push -u origin feature/clientes-controller   # abrir Merge/Pull Request hacia main y pedir revisión
```
Ramas sugeridas: `feature/clientes`, `feature/productos`, `feature/ventas`, `feature/manejo-excepciones`;
para el TP11: `feature/estado-cuenta`, `feature/cobro-cuotas` y `feature/swagger`.
Convención de commits: `feat:`, `fix:`, `docs:`, `refactor:`.
