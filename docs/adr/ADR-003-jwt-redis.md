# ADR-003: Uso de Redis para Caché (Cache-Aside) y Blacklist de Tokens JWT

## Estado
Aceptado

## Contexto
En el desarrollo de SGED, nos enfrentamos a dos problemas distintos que pueden resolverse con una misma tecnología de almacenamiento en memoria:
1. **Invalidación de JWT:** JWT es por naturaleza stateless: el servidor no almacena el token. Si un usuario cierra sesión de forma explícita o si un token es comprometido, el servidor no tiene forma de invalidarlo sin mantener un registro o lista de revocación de los tokens comprometidos (blacklist).
2. **Rendimiento de lecturas repetitivas:** Determinadas consultas a la base de datos (por ejemplo, listado de estudiantes o datos maestros que no cambian en tiempo real) implican operaciones costosas de I/O y procesamiento repetitivo en la base de datos PostgreSQL, lo que afecta el tiempo de respuesta bajo carga.

## Decisión
Se decide implementar **Redis** como almacén centralizado de clave-valor en memoria para satisfacer ambos requerimientos:
1. **Blacklist de JWT:** Se almacenará el identificador único del JWT (JTI) en Redis cuando se solicite un "logout". La clave en Redis tendrá un tiempo de vida (TTL) exactamente igual al tiempo restante de validez del token original. Cada solicitud entrante deberá consultar esta blacklist a través del filtro de autenticación de Spring Security.
2. **Estrategia Cache-Aside:** Se empleará Redis como caché secundario para aliviar las consultas frecuentes de lectura a PostgreSQL. La aplicación verificará primero en Redis (mediante anotaciones de Spring Cache como `@Cacheable`). Si ocurre un *cache miss*, consultará a la base de datos y actualizará Redis. Cuando se realice una escritura (crear/actualizar/eliminar), se invalidará la caché correspondiente (`@CacheEvict`).

## Consecuencias

**Positivas:**
- **Seguridad (Logout real):** Permite un cierre de sesión inmediato sin esperar la expiración del token JWT. La asignación del TTL evita el crecimiento infinito de la memoria en Redis al limpiar los tokens ya expirados.
- **Rendimiento y Escalabilidad:** Reduce significativamente la latencia de las consultas frecuentes al servir respuestas desde memoria O(1), y disminuye la carga sobre PostgreSQL, incrementando la escalabilidad horizontal del backend.

**Negativas:**
- **Complejidad de Infraestructura:** Introduce un nuevo componente (Redis) que requiere monitoreo, gestión de despliegue y mantenimiento.
- **Acoplamiento temporal:** La aplicación depende de Redis para autenticar cualquier petición. Si Redis cae, la verificación de tokens revocados puede fallar o bloquearse.
- **Complejidad de Inconsistencia (Cache):** Con el patrón Cache-Aside, existe la posibilidad temporal de leer datos obsoletos (stale data) si las estrategias de invalidación fallan, lo que complica el proceso de debugging (los errores a menudo solo aparecen en una segunda solicitud). Además, se requiere serializar correctamente los objetos (como la clase `PageImpl` de Spring) para almacenarlos en Redis.

## Alternativas consideradas
- **Para Blacklist (Sin Blacklist):** Confiar solo en un tiempo corto de expiración del JWT y dejar que caduque solo. Inseguro, ya que no permite el *logout* real e incumple directrices estrictas de seguridad.
- **Para Blacklist (Almacenar en BD Relacional):** Usar PostgreSQL. Descartado porque añadiría latencia I/O de disco a *cada* solicitud que el usuario realice.
- **Para Caché (Caché local en memoria - Ej: ConcurrentHashMap / EhCache):** Descartado, ya que en un entorno escalado horizontalmente (múltiples instancias del backend), la caché sería inconsistente entre nodos (problemas de "sticky sessions" y *cache drift*). Redis nos da un estado distribuido centralizado.

## Referencias
- OWASP Top 10 2021 - A07 Identification and Authentication Failures
- Patrón de Diseño Cloud: Cache-Aside (Microsoft / AWS)
- RFC 7519 - JSON Web Token (JWT)