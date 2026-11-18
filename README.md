# ☕ Café Aroma — Módulo de Pedidos de Mostrador

Aplicación web + API REST para registrar pedidos de mostrador, calcular su total y seguir su estado hasta la entrega. Reto integrador de aprendices (práctica Desarrollo, pista Java/Spring).

- Especificación: [`requirements.md`](requirements.md) · Diseño y diagramas: [`design.md`](design.md)
- Uso crítico de IA: [`docs/uso-critico-ia.md`](docs/uso-critico-ia.md) · Guion de la demo: [`docs/guion-demo.md`](docs/guion-demo.md)

## Tecnologías
Java 8 · Spring Boot 2.7.18 (Web MVC, Data JPA, Thymeleaf, Spring Security) · MySQL 8 · Maven · JUnit 5 + Spring Boot Test (H2 en memoria solo para pruebas) · Git.

## Requisitos previos
| Herramienta | Comprobar | Nota |
|---|---|---|
| JDK 8 (1.8) | `java -version` | Por ejemplo Eclipse Temurin 8 |
| Git | `git --version` | |
| MySQL Server 8.x | `mysql --version` o el menú Inicio | Ver instalación abajo |
| IntelliJ IDEA (gratuito) | — | Trae Maven integrado: no hace falta instalar Maven aparte |

## 1. Instalar MySQL en Windows
1. Descarga **MySQL Community Server 8.4 LTS** (instalador MSI para Windows) desde https://dev.mysql.com/downloads/mysql/.
2. En el asistente de configuración: tipo *Development Computer*, puerto **3306**, define la **contraseña de root** y deja MySQL como **servicio de Windows** que arranca solo.
3. (Opcional) Instala **MySQL Workbench** para ejecutar SQL con interfaz gráfica.
4. Si `mysql` no se reconoce en PowerShell, usa el acceso *MySQL 8.4 Command Line Client* del menú Inicio, o agrega `C:\Program Files\MySQL\MySQL Server 8.4\bin` al PATH.

## 2. Crear la base de datos
Abre `sql/01-crear-base-de-datos.sql`, reemplaza `<TU_CONTRASEÑA>` por una contraseña tuya y ejecútalo como root (Workbench o Command Line Client). Crea la base `cafe_aroma` y el usuario `cafe_app`.

> Las tablas y el menú los crea la aplicación al arrancar con `src/main/resources/schema.sql` y `data.sql`.

## 3. Variables de entorno
La contraseña **nunca** se escribe en el código ni en Git.

| Variable | Obligatoria | Valor por defecto |
|---|---|---|
| `DB_PASSWORD` | Sí | — (si falta, la app no arranca) |
| `CAJA_PASSWORD` | Sí | — contraseña del usuario `caja` |
| `COCINA_PASSWORD` | Sí | — contraseña del usuario `cocina` |
| `DB_USERNAME` | No | `cafe_app` |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | No | `localhost` / `3306` / `cafe_aroma` |

En IntelliJ: **Run → Edit Configurations… → PedidosApplication → Environment variables** → `DB_PASSWORD=tu_contraseña;CAJA_PASSWORD=...;COCINA_PASSWORD=...`.

## 4. Ejecutar
1. IntelliJ: **File → Open** → selecciona la carpeta del proyecto (la que tiene `pom.xml`) → *Open as Project*. Espera a que Maven descargue las dependencias.
2. Verifica **File → Project Structure → SDK = 1.8** (y **Language level = 8**).
3. Abre `PedidosApplication.java` y pulsa ▶ (Run).
4. Navega a **http://localhost:8080** e inicia sesión.

### Usuarios
| Usuario | Rol | Entra a | Ve en el menú ☰ |
|---|---|---|---|
| `caja` | CAJA | Tomar pedido | Tomar pedido · Pedidos (caja) · Historial |
| `cocina` | COCINA | Cocina | Cocina · Historial |

Las contraseñas son las de `CAJA_PASSWORD` y `COCINA_PASSWORD`. Se guardan cifradas con BCrypt y nunca van en el código.

## Ejecutar con Docker (alternativa a instalar MySQL y JDK)
Solo necesitas **Docker Desktop** abierto. En la carpeta del proyecto (la que tiene `docker-compose.yml`):
```powershell
copy .env.example .env      # luego edita .env y pon tus contraseñas
docker compose up -d --build
```
Abre **http://localhost:8080**. MySQL queda expuesto en el puerto **3307** del PC (para Workbench: usuario `cafe_app`).

| Acción | Comando |
|---|---|
| Ver logs de la app | `docker compose logs -f app` |
| Apagar | `docker compose down` |
| Apagar y borrar los datos | `docker compose down -v` |
| Recompilar tras cambiar código | `docker compose up -d --build` |

## 5. Pruebas
| Prueba | Tipo | Necesita MySQL |
|---|---|---|
| `OrderStatusTest` | Unitaria: transiciones de estado | No |
| `CustomerOrderTest` | Unitaria: reglas de caja y cocina que dependen del pago | No |
| `SecurityIntegrationTest` | Integración: login, páginas y API por rol (401 / 403) | No |
| `ItemPricingTest` | Unitaria: precios, tamaños y modificadores | No |
| `OrderServiceIntegrationTest` | Integración: Spring + JPA + H2 (pedido, estados, pago, consultas) | No |

Ejecutar: clic derecho en `src/test/java` → **Run 'All Tests'**, o en la ventana Maven: **Lifecycle → test**.

**Verificación manual:** con dos navegadores (o una ventana normal y una de incógnito), uno con `caja` en **Pedidos (caja)** y otro con `cocina` en **Cocina**, crear en **Tomar pedido** un pedido de varios ítems; cobrarlo en caja y ver que aparece solo en cocina (sin recargar); tomarlo y marcarlo listo en cocina; entregarlo en caja y verlo en **Historial**. Cancelar otro sin pagar e intentar registrar un pedido vacío (debe mostrar error).

## 6. Endpoints principales
| Método | Ruta | Respuestas |
|---|---|---|
| GET | `/api/products` | 200 |
| POST | `/api/orders` | 201 · 400 |
| GET | `/api/orders?status=RECIBIDO` | 200 · 400 |
| GET | `/api/orders/{id}` | 200 · 404 |
| GET | `/api/orders/active` | 200 |
| GET | `/api/orders/kitchen` | 200 |
| PATCH | `/api/orders/{id}/payment` | 200 · 400 · 404 |
| PATCH | `/api/orders/{id}/cancel` | 200 · 400 · 404 · 409 |
| PATCH | `/api/orders/{id}/take` | 200 · 400 · 404 · 409 |
| PATCH | `/api/orders/{id}/ready` | 200 · 404 · 409 |
| PATCH | `/api/orders/{id}/deliver` | 200 · 400 · 404 · 409 |

Todas exigen sesión. Desde PowerShell se usa `-u usuario:contraseña` (`-i` muestra el código HTTP). Cambia `CLAVE_CAJA` y `CLAVE_COCINA` por las de tu `.env`:
```powershell
curl.exe -i http://localhost:8080/api/orders/active                       # sin usuario → 401
curl.exe -i -u caja:CLAVE_CAJA http://localhost:8080/api/products
curl.exe -i -u caja:CLAVE_CAJA -X POST http://localhost:8080/api/orders -H "Content-Type: application/json" -d "@docs/api-ejemplos/pedido-valido.json"
curl.exe -i -u caja:CLAVE_CAJA -X POST http://localhost:8080/api/orders -H "Content-Type: application/json" -d "@docs/api-ejemplos/pedido-sin-items.json"
curl.exe -i -u caja:CLAVE_CAJA -X POST http://localhost:8080/api/orders -H "Content-Type: application/json" -d "@docs/api-ejemplos/pedido-modificador-invalido.json"
curl.exe -i -u cocina:CLAVE_COCINA -X PATCH http://localhost:8080/api/orders/1/take        # sin pagar → 400
curl.exe -i -u cocina:CLAVE_COCINA -X PATCH http://localhost:8080/api/orders/1/payment -H "Content-Type: application/json" -d "@docs/api-ejemplos/pago-pagado.json"   # cocina no cobra → 403
curl.exe -i -u caja:CLAVE_CAJA -X PATCH http://localhost:8080/api/orders/1/payment -H "Content-Type: application/json" -d "@docs/api-ejemplos/pago-pagado.json"
curl.exe -i -u cocina:CLAVE_COCINA http://localhost:8080/api/orders/kitchen
curl.exe -i -u cocina:CLAVE_COCINA -X PATCH http://localhost:8080/api/orders/1/take
curl.exe -i -u cocina:CLAVE_COCINA -X PATCH http://localhost:8080/api/orders/1/ready
curl.exe -i -u caja:CLAVE_CAJA -X PATCH http://localhost:8080/api/orders/1/cancel          # ya no está en Recibido → 409
curl.exe -i -u caja:CLAVE_CAJA -X PATCH http://localhost:8080/api/orders/1/deliver
curl.exe -i -u caja:CLAVE_CAJA "http://localhost:8080/api/orders?status=ENTREGADO"
```
Usa `curl.exe` (no `curl`): en PowerShell, `curl` es un alias de otro comando.

## 7. Estructura
```
cafe-aroma/
├── pom.xml
├── requirements.md · design.md · README.md
├── sql/                         creación de BD y consultas de la demo
├── docs/                        uso crítico de IA, guion de demo, ejemplos JSON
└── src/
    ├── main/java/com/cafearoma/pedidos/
    │   ├── model/  repository/  service/  dto/  web/
    ├── main/resources/
    │   ├── application.properties  schema.sql  data.sql
    │   ├── templates/  pedido · caja · cocina · historial  y  fragments/layout.html (encabezado y menú ☰)
    │   └── static/css/styles.css  static/js/  order-form.js · live-board.js · nav.js
    └── test/  (pruebas unitarias e integración + application-test.properties)
```

## Problemas comunes
| Síntoma | Causa probable |
|---|---|
| `Could not resolve placeholder 'DB_PASSWORD'` | Falta la variable de entorno en la configuración de ejecución |
| `Access denied for user 'cafe_app'` | Contraseña distinta a la del script SQL |
| `Communications link failure` | El servicio MySQL no está corriendo |
| `Unknown database 'cafe_aroma'` | No se ejecutó `sql/01-crear-base-de-datos.sql` |
| `Port 8080 was already in use` | Otra app usa el puerto: ciérrala o cambia `server.port` |
| `Could not resolve placeholder 'CAJA_PASSWORD'` | Faltan `CAJA_PASSWORD` / `COCINA_PASSWORD` en `.env` o en la configuración de ejecución |
