# 5.4 — Caché: Patrones y Medición de Rendimiento

## Niveles de caché

Una aplicación web moderna puede cachear datos en varios niveles, cada uno con su propio TTL y estrategia de invalidación. En el **navegador**, las cabeceras `Cache-Control` y `ETag` permiten que el cliente reutilice respuestas sin volver a pedirlas al servidor; el TTL se define explícitamente (`max-age`) y la invalidación ocurre por expiración o por cambio de `ETag`. Un **CDN** cachea contenido estático (imágenes, JS, CSS) cerca geográficamente del usuario, con TTLs de horas o días y purga manual o por versión de archivo. A nivel de **aplicación**, el propio backend puede mantener una caché en memoria (menos común hoy, por no escalar horizontalmente). Y a nivel de **base de datos**, Redis actúa como una capa intermedia entre la aplicación y PostgreSQL, con TTLs cortos (minutos) e invalidación activa mediante `@CacheEvict`, como se implementó en `EstudianteService`.

## Patrones de caché

- **Cache-aside** (el usado en este proyecto): la aplicación consulta primero Redis; si hay *hit*, devuelve el valor sin tocar la base de datos; si hay *miss*, consulta PostgreSQL, guarda el resultado en Redis y lo devuelve.
  Secuencia: `Cliente → Service → Redis (miss) → Repository → PostgreSQL → Redis (guardar) → Cliente`.

- **Read-through**: la aplicación nunca habla directo con la base; siempre pasa por una capa de caché que internamente decide si consulta la fuente.
  Secuencia: `Cliente → Cache → (miss) → PostgreSQL → Cache → Cliente`.
  La diferencia con cache-aside es que la lógica de "ir a buscar a la base" vive en la capa de caché, no en el Service.

- **Write-through**: cada escritura se guarda simultáneamente en caché y en base de datos, de forma síncrona. Garantiza consistencia inmediata, pero añade latencia a cada escritura.

- **Write-behind** (o *write-back*): la escritura se guarda primero en caché y se persiste a la base de datos de forma asíncrona, en lotes o tras un delay. Mejora el rendimiento de escritura, pero arriesga pérdida de datos si Redis falla antes de persistir.

## Cache stampede y su mitigación

El *cache stampede* ocurre cuando una clave popular expira y, en el mismo instante, múltiples peticiones concurrentes detectan el *miss* y golpean todas a la base de datos a la vez, anulando el beneficio de la caché y pudiendo saturar PostgreSQL.

Se mitiga con:

1. Un **mutex/lock** que permite que solo una petición regenere el valor mientras las demás esperan o reciben el valor viejo.
2. **Expiración probabilística temprana** (*probabilistic early expiration*), donde se recalcula el valor un poco antes de que expire realmente, distribuyendo la carga de regeneración en el tiempo en vez de concentrarla en un instante.

## Estructuras de datos de Redis y su uso en el proyecto

Redis no es solo un almacén clave-valor simple; ofrece varias estructuras:

- **Strings**: usada aquí para guardar el JSON serializado del `PageResponse` (cache-aside de estudiantes).
- **Hashes**: útil para representar objetos con campos, como un perfil de usuario.
- **Lists**: colas simples, por ejemplo notificaciones pendientes.
- **Sets**: colecciones sin duplicados, como IDs de estudiantes en una categoría.
- **Sorted sets**: rankings, como tabla de posiciones por rendimiento.
- **Streams**: para eventos tipo log, como auditoría de accesos.

En este proyecto, el cache-aside de estudiantes usa **strings** (vía `RedisCacheManager`), mientras que la blacklist de JWT (implementada por Alejandro) usa claves individuales con TTL igual a la vida restante del token.

## Metodología de benchmarking

Para medir el impacto real de la caché se requiere una metodología rigurosa:

1. Un tamaño de muestra **n ≥ 10** corridas por escenario, para evitar que un solo dato atípico distorsione la conclusión.
2. Reportar **media y desviación estándar**, no solo el promedio, porque una alta dispersión indica inconsistencia (por ejemplo, por variabilidad de red o carga del sistema).
3. **Aislar la variable**: correr exactamente la misma consulta, con los mismos datos, cambiando únicamente si la caché está activa o no (`SPRING_CACHE_TYPE=redis` vs `none`), sin modificar el código entre corridas.

El resultado se resume como *speedup* `S = T_sin / T_con`, donde un valor `S > 1` indica mejora y `S > 2` es el umbral que la rúbrica considera suficiente para justificar la complejidad operacional añadida.
