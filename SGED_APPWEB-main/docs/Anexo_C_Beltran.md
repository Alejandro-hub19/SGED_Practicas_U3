# Anexo C — Preguntas de Beltrán

## Pregunta de Análisis

**¿El speedup obtenido justifica la complejidad operacional de añadir Redis?**

Con un speedup promedio de **2.07x** y **2.24x** en el percentil 95 (medido por Alejandro), el resultado supera el umbral de `S > 2` que la rúbrica establece como mínimo para justificar la complejidad de introducir una capa de caché. Esto significa que, en promedio, las peticiones con caché tardan menos de la mitad de tiempo que sin ella, y ese beneficio se mantiene incluso en el peor 5% de los casos (P95), lo cual es una señal saludable: la mejora no depende de condiciones ideales aisladas, sino que se sostiene bajo variabilidad real de carga.

Sin embargo, el análisis no puede limitarse al número de speedup; hay que sopesarlo contra tres costos concretos:

- **Costo de infraestructura**: Redis requiere un contenedor/servicio adicional corriendo, con su propio consumo de memoria y CPU. En este proyecto es marginal (un contenedor Docker local), pero en producción implica aprovisionar y monitorear un servicio más.

- **Complejidad de despliegue**: cada entorno (desarrollo, staging, producción) necesita ahora una instancia de Redis disponible y correctamente configurada (TTLs, política de desalojo de memoria, persistencia opcional), lo que añade una pieza más a la infraestructura que puede fallar.

- **¿Qué pasa si Redis se cae?**: con el patrón cache-aside implementado aquí, si Redis no responde, la aplicación simplemente vuelve a consultar PostgreSQL directamente en cada petición (no hay dependencia dura de Redis para que el sistema funcione) — el sistema se degrada a su rendimiento sin caché, pero no se cae. Este es precisamente uno de los motivos por los que se optó por cache-aside en vez de un patrón como read-through, donde la aplicación sí dependería de la capa de caché para servir cualquier dato.

**Conclusión**: con un speedup de 2.07x-2.24x, un costo de infraestructura bajo para esta escala de proyecto (un solo contenedor), y una degradación segura ante fallos de Redis, la complejidad añadida **sí se justifica** en este caso. La recomendación sería revisar esta decisión nuevamente si el proyecto creciera a un volumen de tráfico donde el costo de operar Redis en alta disponibilidad (réplicas, failover) empiece a acercarse al beneficio que aporta.

---

## Pregunta de Evaluación

**¿Qué casos de prueba adicionales aumentarían la confianza sin inflar el mantenimiento? ¿Qué tipo de bugs no captura una prueba unitaria del Repository?**

### Casos de prueba adicionales que aportarían valor real

- Pruebas de **integración** que levanten un contenedor Redis real (por ejemplo con Testcontainers) y verifiquen que `@Cacheable` efectivamente evita una segunda consulta a la base — las pruebas actuales validan el Repository de forma aislada con H2, pero no prueban la interacción real con Redis.

- Pruebas de **concurrencia**: múltiples hilos pidiendo el mismo filtro al mismo tiempo, para detectar problemas de *cache stampede* o condiciones de carrera al momento de invalidar (`@CacheEvict`) mientras otra petición está leyendo.

- Pruebas de **serialización**: confirmar explícitamente que un `PageResponse<EstudianteResponse>` se serializa y deserializa correctamente desde Redis, ya que este es justamente el punto donde apareció el bug de `PageImpl`/Jackson que motivó usar `Serializable` con `JdkSerializationRedisSerializer`.

- Un caso límite de **filtros vacíos combinados con datos vacíos** (por ejemplo, pedir una categoría que no existe) para confirmar que la metadata de paginación (`total: 0`, `last_page: 0` o `1`) no rompe el contrato de la API.

### Bugs que una prueba unitaria del Repository NO puede capturar

- **Bugs de infraestructura real**: el bug de serialización `Page`/Jackson en Redis (que motivó implementar `Serializable`) nunca habría aparecido en un test de Repository con H2, porque ese entorno nunca pasa por Redis — solo se detecta en una prueba de integración real o, como pasó en este proyecto, en producción/desarrollo manual.

- **Bugs de concurrencia**: un test unitario corre en un solo hilo, secuencial; nunca va a exponer una condición de carrera entre dos peticiones simultáneas invalidando y leyendo la misma clave de caché.

- **Bugs de configuración**: por ejemplo, un TTL mal configurado en `CacheConfig`, o una variable de entorno faltante (`JWT_SECRET`), no rompen ningún test unitario porque el contexto de test usa su propia configuración aislada (`application.properties` de test) — estos solo se detectan al levantar la aplicación completa.

- **Bugs de comportamiento con datos reales**: el bug que sí encontramos durante este trabajo (`@PrePersist` forzando `activo=true` siempre, ignorando el valor real asignado antes de guardar) es justamente un ejemplo de lo contrario — este tipo de bug de lógica de negocio **sí** lo captura una prueba unitaria bien diseñada, lo cual reafirma por qué vale la pena escribirlas a pesar del costo de mantenimiento que implican.
