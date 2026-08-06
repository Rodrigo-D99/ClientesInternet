# Copilot Instructions

- Responder en español.
- Priorizar respuestas cortas y directas.
- No extenderse con explicaciones largas.
- Cuando se pida cambiar código existente, modificar solo las líneas necesarias.
- No reescribir archivos completos salvo que se pida explícitamente.
- Mantener la solución lo más pequeña posible.
- Si hay varias opciones, elegir la más simple y compatible con el código actual.
- En frontend (JS/HTML), devolver solo el fragmento afectado.
- En backend Spring Boot, tocar solo el método, clase o bloque necesario.
- No agregar librerías nuevas salvo que sean imprescindibles.
- Si falta contexto, pedir solo la mínima información necesaria.

---
 "src/main/resources/static/**,**/*.js,**/*.html"
---

- Cambiar solo el bloque necesario.
- No reescribir el archivo completo.
- Mantener HTML simple y JS mínimo.
- Responder con el fragmento exacto a reemplazar.
- Recordar que el JS está modulado: *-core.js maneja datos/fetch y *-ui.js manejan el DOM/renderizado.
- Al usar paginación de Spring Boot, recordar que el array está en `data.content` y el control en `data.totalPages`/`data.number`.

---
 "src/main/java/**"
---

- Modificar solo el método/clase afectado.
- No recrear controladores o servicios enteros si alcanza con un cambio puntual.
- Mantener compatibilidad con el estilo existente del proyecto.
- Responder con el diff lógico más pequeño posible.
- Siempre usar comentarios breves en español si el código requiere explicación.
- Respetar las anotaciones de Lombok existentes (como @Data, @NoArgsConstructor) en las entidades de Java.
- Optimizar las respuestas para los modelos Gemini 3.1 Pro y Gemini 3 Flash instalados.