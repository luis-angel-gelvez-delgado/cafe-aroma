# Guion de la demo (10–15 min)

**Antes de empezar:** MySQL corriendo, la app arrancada, navegador en http://localhost:8080, PowerShell en la carpeta del proyecto y Workbench abierto con `sql/02-consultas-demo.sql`.

## 1. Contexto y alcance (2 min)
- Café Aroma toma pedidos en un cuaderno; el objetivo es digitalizar el mostrador.
- Alcance elegido (pista Java/Spring, "elige 2-3"): crear pedido, listar por estado y actualizar estado, más la pantalla de mostrador. Extra pequeño: registro de pago.
- Fuera de alcance: pagos en línea, inventario, "agotado hoy", domicilios.
- Flujo SDD: `requirements.md` → `design.md` → código con IA → revisión humana.

## 2. Demo funcional (6–7 min)
1. **Menú:** cada producto solo muestra sus opciones válidas (el croissant no tiene tamaños; el frappé no tiene Pequeño).
2. **Crear pedido:** "Ana": Latte Grande + shot + deslactosada, y Croissant para llevar → total **$18.000**, calculado por el servidor.
3. **Validación en el backend:** registrar un pedido sin productos → mensaje de error. Marcar a la vez "Para llevar" y "Consumir en el local" → error.
4. **Cocina no toma pedidos sin pagar:** el pedido nuevo está en Recibido con pago Pendiente y no tiene botón para cocina. Por API, intentar tomarlo → **400**.
   `curl.exe -i -X PATCH http://localhost:8080/api/orders/1/take`
5. **Caja cobra:** al cobrar desaparece Cancelar (un pedido pagado no se cancela) y aparece el botón de cocina **Tomar**.
6. **Cocina:** Tomar → En preparación; Listo → Listo para recoger. Por API, intentar cancelar ese pedido → **409** con mensaje.
   `curl.exe -i -X PATCH http://localhost:8080/api/orders/1/cancel`
7. **Caja entrega:** el pedido pasa a Entregado y aparece en el historial.

## 3. SQL (1–2 min)
- Mostrar el diagrama ER de `design.md` y por qué `order_item.price` congela el precio cobrado.
- Ejecutar en Workbench: pedidos activos por estado, resumen del día y la comprobación del total (0 filas).

## 4. Uso crítico de la IA (2–3 min)
- Mostrar `docs/uso-critico-ia.md`: qué propuso la IA, qué revisé, qué cambié y cómo lo verifiqué.
- Mencionar que las pruebas automáticas (unitarias e integración) se ejecutaron y pasaron en mi equipo.

## 5. Reflexión (2 min)
- Qué aprendí del flujo SDD y de revisar código generado.
- Siguientes pasos posibles: pantalla "Listo para recoger" para TV, resumen del día en la interfaz.
