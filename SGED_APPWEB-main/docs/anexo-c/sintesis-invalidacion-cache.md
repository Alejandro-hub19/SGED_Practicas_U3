# Anexo C — Pregunta de Síntesis

## Estrategia de invalidación de caché y mitigación de cache stampede

### Estrategia de invalidación implementada

El sistema emplea el patrón **cache-aside** con Redis sobre el listado de
estudiantes. La coherencia entre la caché y la base de datos se mantiene mediante
invalidación explícita: toda operación de escritura (crear, actualizar, eliminar)
está anotada con `@CacheEvict(value = "estudiantes", allEntries = true)`, que
elimina todas las entradas del namespace `estudiantes` en Redis.

Se optó por `allEntries = true` en lugar de invalidar claves individuales porque
la clave de caché incorpora la combinación de filtros, paginación y ordenamiento
(`nombre-apellido-categoria-activo-page-size-sort`). Una sola escritura puede
afectar el resultado de múltiples combinaciones de consulta, por lo que invalidar
solo la clave "afectada" dejaría entradas obsoletas en otras páginas o filtros.
Vaciar el namespace completo garantiza coherencia a costa de un mayor número de
cache miss tras cada escritura, un compromiso razonable en un dominio donde las
lecturas superan ampliamente a las escrituras (Privalov & Stupina, 2024).

Como defensa adicional frente a fallos de invalidación, cada entrada tiene un
**TTL** configurado. Si por algún motivo un `@CacheEvict` no se ejecutara, la
entrada obsoleta no persistiría indefinidamente: expiraría automáticamente,
limitando la ventana de inconsistencia.

### El problema del cache stampede

El *cache stampede* (o *thundering herd*) ocurre cuando una clave muy solicitada
expira y, simultáneamente, numerosas peticiones concurrentes encuentran el cache
miss y disparan la misma consulta costosa contra la base de datos. En lugar de
aliviar la carga, la caché produce un pico abrupto que puede saturar el backend,
justo el escenario que se pretendía evitar.

### Estrategias de mitigación

**1. Bloqueo mutuo (mutex / lock).** Solo la primera petición que detecta el
miss adquiere un lock y recalcula el valor; las demás esperan o reciben el valor
anterior mientras tanto. Evita que N peticiones golpeen la base de datos, a costa
de introducir contención.

**2. Expiración temprana probabilística (*probabilistic early expiration*).**
Cada petición decide, con una probabilidad creciente a medida que se acerca el
TTL, refrescar la entrada *antes* de que expire. Así el recálculo se distribuye
en el tiempo y no coincide con la expiración exacta, evitando el pico
sincronizado.

**3. TTL con jitter.** Añadir una variación aleatoria al TTL de cada entrada
evita que múltiples claves creadas al mismo tiempo expiren simultáneamente,
dispersando los recálculos.

### Aplicación al SGED

En el estado actual, con un único cliente y sin concurrencia significativa, el
riesgo de stampede es bajo. No obstante, bajo la carga concurrente prevista en el
análisis de escalabilidad (miles de usuarios), la mitigación más adecuada sería
combinar **TTL con jitter** —de implementación trivial— con un **mutex** sobre la
consulta principal de listado, que es la clave más solicitada y la más costosa de
recalcular. La literatura reciente confirma que una gestión adecuada del TTL y de
la estrategia de invalidación es determinante para mantener una alta tasa de
aciertos de caché sin servir datos obsoletos (Privalov & Stupina, 2024).

## Referencias

- Privalov, M., & Stupina, A. (2024). Improving web-oriented information systems efficiency using Redis caching mechanisms. *Indonesian Journal of Electrical Engineering and Computer Science, 33*(3), 1667–1675. https://doi.org/10.11591/ijeecs.v33.i3.pp1667-1675